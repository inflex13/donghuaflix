"""Batch download and cache all poster images from scraped shows."""
import asyncio
import logging

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.assets import download_and_cache_image, _url_to_filename
from app.models import Show, WebsiteShow

logger = logging.getLogger(__name__)

API_BASE = "https://api.donghuaflix.cloud"


async def _cache_single_poster(original_url: str) -> str | None:
    """Download and return the cached asset URL."""
    if not original_url or "api.donghuaflix.cloud" in original_url:
        return None  # Already cached

    filename = await download_and_cache_image(original_url)
    if filename:
        return f"{API_BASE}/assets/posters/{filename}"
    return None


def cache_all_posters(db: Session):
    """Download all poster images and update DB with local URLs."""
    shows = db.execute(select(Show)).scalars().all()
    logger.info(f"Caching posters for {len(shows)} shows")

    loop = asyncio.new_event_loop()
    cached = 0
    failed = 0

    try:
        for show in shows:
            if show.poster_url and "api.donghuaflix.cloud" not in (show.poster_url or ""):
                new_url = loop.run_until_complete(_cache_single_poster(show.poster_url))
                if new_url:
                    show.poster_url = new_url
                    cached += 1
                else:
                    failed += 1

        # Also cache website_show posters
        ws_list = db.execute(select(WebsiteShow)).scalars().all()
        for ws in ws_list:
            if ws.poster_url_on_site and "api.donghuaflix.cloud" not in (ws.poster_url_on_site or ""):
                new_url = loop.run_until_complete(_cache_single_poster(ws.poster_url_on_site))
                if new_url:
                    ws.poster_url_on_site = new_url
                    cached += 1

        db.commit()
    finally:
        loop.close()

    logger.info(f"Cached {cached} posters, {failed} failed")
    return cached, failed
