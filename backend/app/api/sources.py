from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Source

router = APIRouter()


@router.post("/{source_id}/resolve")
async def resolve_source(source_id: int, db: AsyncSession = Depends(get_db)):
    query = select(Source).where(Source.id == source_id)
    result = await db.execute(query)
    source = result.scalar_one_or_none()
    if not source:
        raise HTTPException(status_code=404, detail="Source not found")

    # If URL is cached and not expired, return it (but still fetch subtitles for Dailymotion)
    if source.source_url:
        from datetime import datetime
        if not source.url_expires_at or source.url_expires_at > datetime.utcnow():
            subtitles = {}
            # Dailymotion sources: always fetch fresh subtitles since they have separate expiry
            if source.source_type == "hls" and (
                "dailymotion" in (source.source_name or "").lower()
                or source.source_key.startswith("dailymotion")
            ):
                subtitles = await _fetch_dailymotion_subtitles(source.raw_player_data)
            return {
                "id": source.id,
                "source_name": source.source_name,
                "source_key": source.source_key,
                "source_url": source.source_url,
                "source_type": source.source_type,
                "subtitles": subtitles,
            }

    # Resolve via source resolver
    from app.services.source_resolver import resolve_video_url
    resolved = await resolve_video_url(source)
    if not resolved:
        raise HTTPException(status_code=502, detail="Could not resolve video URL")

    source.source_url = resolved["url"]
    source.source_type = resolved["type"]
    if resolved.get("expires_at"):
        source.url_expires_at = resolved["expires_at"]
    await db.commit()
    await db.refresh(source)

    return {
        "id": source.id,
        "source_name": source.source_name,
        "source_key": source.source_key,
        "source_url": source.source_url,
        "source_type": source.source_type,
        "subtitles": resolved.get("subtitles", {}),
    }


async def _fetch_dailymotion_subtitles(raw_player_data: str) -> dict:
    """Fetch subtitle tracks from Dailymotion metadata API."""
    import re
    import httpx

    video_id = (raw_player_data or "").strip()
    match = re.search(r"video[=/]([a-zA-Z0-9]+)", video_id)
    if match:
        video_id = match.group(1)
    if not video_id:
        return {}

    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"https://www.dailymotion.com/player/metadata/video/{video_id}",
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"},
            )
            if resp.status_code == 200:
                data = resp.json()
                subtitles = {}
                subs_data = data.get("subtitles", {}).get("data", {})
                for lang, sub_info in subs_data.items():
                    urls = sub_info.get("urls", [])
                    if urls:
                        subtitles[lang] = {
                            "label": sub_info.get("label", lang),
                            "url": urls[0],
                        }
                return subtitles
    except Exception:
        pass
    return {}
