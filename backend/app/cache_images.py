"""Download all poster images, convert to JPEG, save thumb + full, update DB."""
import asyncio
import hashlib
import io
import logging
from pathlib import Path

import httpx
from PIL import Image
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.models import Show

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

FULL_DIR = Path("/app/assets/posters/full")
THUMB_DIR = Path("/app/assets/posters/thumb")
FULL_DIR.mkdir(parents=True, exist_ok=True)
THUMB_DIR.mkdir(parents=True, exist_ok=True)

API_BASE = "https://api.donghuaflix.cloud"


async def cache_all():
    engine = create_engine("postgresql://postgres:donghuaflix2026@db:5432/donghuaflix")

    with Session(engine) as db:
        shows = db.execute(select(Show)).scalars().all()
        logger.info(f"Processing {len(shows)} shows")

        cached = 0
        failed = 0

        async with httpx.AsyncClient(timeout=20, follow_redirects=True) as client:
            for show in shows:
                url = show.poster_url
                if not url or not url.startswith("http"):
                    continue
                if "api.donghuaflix.cloud/assets" in url:
                    cached += 1
                    continue

                url_hash = hashlib.md5(url.encode()).hexdigest()
                full_path = FULL_DIR / f"{url_hash}.jpg"
                thumb_path = THUMB_DIR / f"{url_hash}.jpg"

                if full_path.exists() and thumb_path.exists():
                    show.poster_url = f"{API_BASE}/assets/posters-full/{url_hash}.jpg"
                    cached += 1
                    continue

                try:
                    resp = await client.get(url, headers={
                        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                        "Referer": "https://donghuafun.com/",
                    })
                    if resp.status_code != 200:
                        logger.warning(f"HTTP {resp.status_code} for {show.title}")
                        failed += 1
                        continue

                    img = Image.open(io.BytesIO(resp.content))
                    img = img.convert("RGB")

                    # Full res (max 800px tall, high quality)
                    full_img = img.copy()
                    if full_img.height > 800:
                        ratio = 800 / full_img.height
                        full_img = full_img.resize(
                            (int(full_img.width * ratio), 800), Image.LANCZOS
                        )
                    full_img.save(str(full_path), "JPEG", quality=90)

                    # Thumbnail (300px tall)
                    thumb_img = img.copy()
                    ratio = 300 / thumb_img.height
                    thumb_img = thumb_img.resize(
                        (int(thumb_img.width * ratio), 300), Image.LANCZOS
                    )
                    thumb_img.save(str(thumb_path), "JPEG", quality=80)

                    show.poster_url = f"{API_BASE}/assets/posters-full/{url_hash}.jpg"
                    cached += 1

                    if cached % 10 == 0:
                        logger.info(f"Cached {cached} images...")

                except Exception as e:
                    logger.error(f"Failed {show.title}: {e}")
                    failed += 1

        db.commit()
        logger.info(f"Done: {cached} cached, {failed} failed")


if __name__ == "__main__":
    asyncio.run(cache_all())
