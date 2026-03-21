import asyncio
import logging
import re

import httpx
from bs4 import BeautifulSoup

from app.config import settings
from app.scraper.base import BaseScraper, RawEpisode, RawShow, RawSource, ShowDetail
from app.scraper.registry import register_scraper

logger = logging.getLogger(__name__)


@register_scraper("donghuafun")
class DonghuaFunScraper(BaseScraper):
    """Scraper for donghuafun.com (MacCMS-based)."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.headers = {
            "User-Agent": settings.SCRAPE_USER_AGENT,
            "Referer": self.base_url,
        }

    async def scrape_catalog(self, full: bool = False) -> list[RawShow]:
        """Scrape show catalog from donghuafun.com.

        Uses the MacCMS API endpoint for faster scraping.
        """
        shows = []
        max_pages = 100 if full else 1

        async with httpx.AsyncClient(timeout=30, headers=self.headers) as client:
            for page in range(1, max_pages + 1):
                try:
                    # Try MacCMS API first
                    resp = await client.get(
                        f"{self.base_url}/api.php/provide/vod/",
                        params={"ac": "detail", "pg": page},
                    )
                    if resp.status_code != 200:
                        # Fallback to HTML scraping
                        page_shows = await self._scrape_catalog_html(client, page)
                        shows.extend(page_shows)
                    else:
                        data = resp.json()
                        page_list = data.get("list", [])
                        if not page_list:
                            break

                        for item in page_list:
                            show = self._parse_api_show(item)
                            if show:
                                shows.append(show)

                        # Check if we've reached the last page
                        total_pages = data.get("pagecount", 1)
                        if page >= total_pages:
                            break

                except Exception as e:
                    logger.error(f"Error scraping catalog page {page}: {e}")
                    break

                await asyncio.sleep(settings.SCRAPE_REQUEST_DELAY)

        logger.info(f"Scraped {len(shows)} shows from donghuafun.com")
        return shows

    def _parse_api_show(self, item: dict) -> RawShow | None:
        """Parse a show from the MacCMS API response."""
        try:
            vod_id = str(item.get("vod_id", ""))
            title = item.get("vod_name", "").strip()
            if not title:
                return None

            # Parse genres from type/class
            genres = []
            vod_class = item.get("vod_class", "")
            if vod_class:
                genres = [g.strip() for g in vod_class.split(",") if g.strip()]

            # Determine status
            status_text = item.get("vod_remarks", "")
            status = "completed" if "END" in status_text.upper() or "完结" in status_text else "ongoing"

            # Parse episode count from remarks
            total_episodes = None
            ep_match = re.search(r"EP?(\d+)", status_text, re.IGNORECASE)
            if ep_match:
                total_episodes = int(ep_match.group(1))

            # Category
            type_name = item.get("type_name", "").lower()
            category = "movie" if "movie" in type_name or "电影" in type_name else "donghua"

            return RawShow(
                external_id=vod_id,
                title=title,
                title_chinese=item.get("vod_sub", ""),
                poster_url=item.get("vod_pic", ""),
                description=self._clean_html(item.get("vod_content", "")),
                rating=self._parse_float(item.get("vod_score")),
                rating_count=self._parse_int(item.get("vod_score_num")),
                year=self._parse_int(item.get("vod_year")),
                status=status,
                genres=genres,
                region=item.get("vod_area", ""),
                language=item.get("vod_lang", ""),
                total_episodes=total_episodes,
                category=category,
                external_url=f"{self.base_url}/index.php/vod/detail/id/{vod_id}.html",
            )
        except Exception as e:
            logger.error(f"Error parsing show: {e}")
            return None

    async def _scrape_catalog_html(self, client: httpx.AsyncClient, page: int) -> list[RawShow]:
        """Fallback: scrape catalog from HTML listing pages."""
        shows = []
        try:
            resp = await client.get(
                f"{self.base_url}/index.php/vod/type/id/1/page/{page}.html"
            )
            if resp.status_code != 200:
                return shows

            soup = BeautifulSoup(resp.text, "lxml")
            # Find show links - adjust selectors based on actual HTML
            for link in soup.select("a[href*='/vod/detail/id/']"):
                href = link.get("href", "")
                id_match = re.search(r"/id/(\d+)", href)
                if id_match:
                    title = link.get_text(strip=True)
                    if title:
                        shows.append(RawShow(
                            external_id=id_match.group(1),
                            title=title,
                            external_url=f"{self.base_url}{href}",
                        ))
        except Exception as e:
            logger.error(f"Error scraping HTML catalog page {page}: {e}")

        return shows

    async def scrape_show_detail(self, external_id: str) -> ShowDetail | None:
        """Scrape full details for a show from donghuafun.com."""
        async with httpx.AsyncClient(timeout=30, headers=self.headers) as client:
            try:
                # Try API first
                resp = await client.get(
                    f"{self.base_url}/api.php/provide/vod/",
                    params={"ac": "detail", "ids": external_id},
                )
                if resp.status_code == 200:
                    data = resp.json()
                    items = data.get("list", [])
                    if items:
                        item = items[0]
                        show = self._parse_api_show(item)
                        if not show:
                            return None

                        episodes = self._parse_episodes_from_api(item, external_id)
                        return ShowDetail(show=show, episodes=episodes)

                # Fallback to HTML
                return await self._scrape_detail_html(client, external_id)

            except Exception as e:
                logger.error(f"Error scraping show detail {external_id}: {e}")
                return None

    def _parse_episodes_from_api(self, item: dict, external_id: str) -> list[RawEpisode]:
        """Parse episodes from MacCMS API vod_play_url field."""
        episodes = []
        play_url = item.get("vod_play_url", "")
        play_from = item.get("vod_play_from", "")

        if not play_url:
            return episodes

        # vod_play_from: source1$$$source2$$$source3
        # vod_play_url: ep1$url1#ep2$url2$$$ep1$url1#ep2$url2$$$...
        source_names = play_from.split("$$$") if play_from else []
        source_groups = play_url.split("$$$")

        if not source_groups:
            return episodes

        # Parse each source group into a dict of {ep_num: (title, url)}
        parsed_groups = []
        for group in source_groups:
            ep_map = {}
            for entry in group.split("#"):
                parts = entry.split("$", 1)
                if len(parts) < 2:
                    continue
                ep_title = parts[0].strip()
                ep_url = parts[1].strip()
                num_match = re.search(r"(\d+)", ep_title)
                if num_match:
                    ep_num = int(num_match.group(1))
                    ep_map[ep_num] = (ep_title, ep_url)
            parsed_groups.append(ep_map)

        # Find ALL unique episode numbers across all source groups
        all_ep_nums = set()
        for group in parsed_groups:
            all_ep_nums.update(group.keys())

        # Build episodes with all available sources
        for ep_num in sorted(all_ep_nums):
            sources = []
            ep_title = f"EP{ep_num}"

            for si, group in enumerate(parsed_groups):
                if ep_num not in group:
                    continue

                title, source_url = group[ep_num]
                ep_title = title  # Use the title from any available source
                source_name = source_names[si] if si < len(source_names) else f"Source {si + 1}"

                # Determine source type
                source_type = "embed"
                if "dailymotion" in source_name.lower():
                    source_type = "dailymotion"
                elif source_url.endswith(".m3u8"):
                    source_type = "hls"
                elif source_url.endswith(".mp4"):
                    source_type = "mp4"

                sources.append(RawSource(
                    source_name=source_name,
                    source_key=f"{source_name}_{ep_num}",
                    raw_player_data=source_url,
                    source_type=source_type,
                ))

            episodes.append(RawEpisode(
                episode_number=ep_num,
                title=ep_title,
                external_url=f"{self.base_url}/index.php/vod/play/id/{external_id}/sid/1/nid/{ep_num}.html",
                sources=sources,
            ))

        return episodes

    async def _scrape_detail_html(self, client: httpx.AsyncClient, external_id: str) -> ShowDetail | None:
        """Fallback: scrape show detail from HTML page."""
        try:
            resp = await client.get(
                f"{self.base_url}/index.php/vod/detail/id/{external_id}.html"
            )
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "lxml")

            title = ""
            title_tag = soup.select_one("h1, .title, .video-title")
            if title_tag:
                title = title_tag.get_text(strip=True)

            if not title:
                return None

            return ShowDetail(
                show=RawShow(
                    external_id=external_id,
                    title=title,
                    external_url=f"{self.base_url}/index.php/vod/detail/id/{external_id}.html",
                ),
                episodes=[],
            )
        except Exception as e:
            logger.error(f"Error scraping HTML detail {external_id}: {e}")
            return None

    async def extract_video_url(self, raw_player_data: str) -> dict | None:
        """Extract video URL using source resolver."""
        from app.services.source_resolver import _resolve_embed, _resolve_dailymotion

        if "dailymotion" in raw_player_data:
            return await _resolve_dailymotion(raw_player_data)
        return await _resolve_embed(raw_player_data)

    @staticmethod
    def _clean_html(text: str) -> str:
        """Remove HTML tags from text."""
        if not text:
            return ""
        return re.sub(r"<[^>]+>", "", text).strip()

    @staticmethod
    def _parse_float(value) -> float | None:
        try:
            return float(value) if value else None
        except (ValueError, TypeError):
            return None

    @staticmethod
    def _parse_int(value) -> int | None:
        try:
            return int(value) if value else None
        except (ValueError, TypeError):
            return None
