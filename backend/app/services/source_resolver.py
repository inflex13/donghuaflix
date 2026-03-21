import logging
import re
from datetime import datetime, timedelta

from app.models import Source

logger = logging.getLogger(__name__)


async def resolve_video_url(source: Source) -> dict | None:
    """Resolve a video source to a direct playable URL."""
    raw_data = source.raw_player_data
    if not raw_data:
        return None

    source_name = (source.source_name or "").lower()

    # Dailymotion sources — raw_data is just the video ID like "k1WMDu8XxGioSiFaV6O"
    if "dailymotion" in source_name or source.source_type == "dailymotion":
        return await _resolve_dailymotion(raw_data)

    # Embed sources (1080eng, 1080indo) — raw_data is a UUID
    # UUID pattern: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
    if re.match(r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$', raw_data):
        embed_url = f"https://play.donghuafun.com/embed/{raw_data}"
        return await _resolve_embed(embed_url)

    # Full URL
    if raw_data.startswith("http"):
        if "dailymotion" in raw_data:
            return await _resolve_dailymotion(raw_data)
        return await _resolve_embed(raw_data)

    return None


async def _resolve_dailymotion(raw_data: str) -> dict | None:
    """Extract stream URL and subtitles from Dailymotion."""
    video_id = raw_data.strip()

    match = re.search(r"video[=/]([a-zA-Z0-9]+)", video_id)
    if match:
        video_id = match.group(1)

    try:
        import httpx
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.get(
                f"https://www.dailymotion.com/player/metadata/video/{video_id}",
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"},
            )
            if resp.status_code == 200:
                data = resp.json()
                qualities = data.get("qualities", {}).get("auto", [])
                stream_url = None
                for q in qualities:
                    if q.get("type") == "application/x-mpegURL":
                        stream_url = q["url"]
                        break

                if not stream_url:
                    return None

                # Extract subtitle URLs
                subtitles = {}
                subs_data = data.get("subtitles", {}).get("data", {})
                for lang, sub_info in subs_data.items():
                    urls = sub_info.get("urls", [])
                    if urls:
                        subtitles[lang] = {
                            "label": sub_info.get("label", lang),
                            "url": urls[0],
                        }

                logger.info(f"Resolved Dailymotion {video_id} → HLS + {len(subtitles)} subtitle tracks")
                return {
                    "url": stream_url,
                    "type": "hls",
                    "subtitles": subtitles,
                    "expires_at": datetime.utcnow() + timedelta(hours=6),
                }
    except Exception as e:
        logger.error(f"Dailymotion resolve failed for {video_id}: {e}")

    return None


async def _resolve_embed(embed_url: str) -> dict | None:
    """Use Playwright to extract video URL from embed page."""
    try:
        from playwright.async_api import async_playwright

        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True, args=["--no-sandbox"])
            page = await browser.new_page()

            video_url = None
            video_type = None

            async def handle_response(response):
                nonlocal video_url, video_type
                url = response.url
                content_type = response.headers.get("content-type", "")

                if any(ext in url for ext in [".m3u8", ".mpd"]):
                    video_url = url
                    video_type = "hls" if ".m3u8" in url else "dash"
                elif "video" in content_type or any(ext in url for ext in [".mp4", ".webm"]):
                    if not video_url:
                        video_url = url
                        video_type = "mp4"

            page.on("response", handle_response)

            await page.goto(embed_url, wait_until="networkidle", timeout=30000)
            await page.wait_for_timeout(5000)

            await browser.close()

            if video_url:
                logger.info(f"Resolved embed → {video_type}: {video_url[:80]}...")
                return {
                    "url": video_url,
                    "type": video_type,
                    "expires_at": datetime.utcnow() + timedelta(hours=2),
                }
            else:
                logger.warning(f"No video URL found for {embed_url}")
    except Exception as e:
        logger.error(f"Embed resolve failed for {embed_url}: {e}")

    return None
