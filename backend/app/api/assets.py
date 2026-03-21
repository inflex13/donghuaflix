import hashlib
import logging
from pathlib import Path
from urllib.parse import unquote

import httpx
from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse, Response

from app.config import settings

logger = logging.getLogger(__name__)

router = APIRouter()

ASSETS_DIR = Path("/app/assets/posters")
ASSETS_DIR.mkdir(parents=True, exist_ok=True)


def _url_to_filename(url: str) -> str:
    """Convert a URL to a safe filename using hash + extension."""
    ext = ".webp"
    for e in [".webp", ".jpg", ".jpeg", ".png", ".gif"]:
        if e in url.lower():
            ext = e
            break
    url_hash = hashlib.md5(url.encode()).hexdigest()
    return f"{url_hash}{ext}"


async def download_and_cache_image(url: str) -> str | None:
    """Download an image and cache it locally. Returns the local filename."""
    if not url or not url.startswith("http"):
        return None

    filename = _url_to_filename(url)
    filepath = ASSETS_DIR / filename

    if filepath.exists():
        return filename

    try:
        async with httpx.AsyncClient(timeout=15, follow_redirects=True) as client:
            resp = await client.get(url, headers={
                "User-Agent": settings.SCRAPE_USER_AGENT,
                "Referer": "https://donghuafun.com/",
            })
            if resp.status_code == 200:
                filepath.write_bytes(resp.content)
                logger.info(f"Cached image: {filename} ({len(resp.content)} bytes)")
                return filename
    except Exception as e:
        logger.error(f"Failed to download image {url}: {e}")

    return None


@router.get("/posters-full/{filename}")
async def get_poster_full(filename: str):
    filepath = ASSETS_DIR / "full" / filename
    if not filepath.exists():
        raise HTTPException(status_code=404, detail="Image not found")
    return _serve_image(filepath)


@router.get("/posters-thumb/{filename}")
async def get_poster_thumb(filename: str):
    filepath = ASSETS_DIR / "thumb" / filename
    if not filepath.exists():
        raise HTTPException(status_code=404, detail="Image not found")
    return _serve_image(filepath)


@router.get("/posters/{filename}")
async def get_poster(filename: str):
    filepath = ASSETS_DIR / filename
    if not filepath.exists():
        raise HTTPException(status_code=404, detail="Image not found")
    return _serve_image(filepath)


def _serve_image(filepath):
    content_type = "image/jpeg"
    name = filepath.name
    if name.endswith(".webp"):
        content_type = "image/webp"
    elif name.endswith(".png"):
        content_type = "image/png"
    return FileResponse(
        filepath,
        media_type=content_type,
        headers={"Cache-Control": "public, max-age=2592000"},
    )


@router.get("/proxy")
async def proxy_image(url: str):
    """Proxy and cache an image. First request downloads, subsequent serve from cache.

    Usage: /assets/proxy?url=https://donghuafun.com/upload/vod/xxx.webp
    """
    url = unquote(url)
    if not url.startswith("http"):
        raise HTTPException(status_code=400, detail="Invalid URL")

    filename = _url_to_filename(url)
    filepath = ASSETS_DIR / filename

    # Serve from cache if exists
    if filepath.exists():
        content_type = "image/webp"
        if filename.endswith(".jpg") or filename.endswith(".jpeg"):
            content_type = "image/jpeg"
        elif filename.endswith(".png"):
            content_type = "image/png"
        return FileResponse(
            filepath,
            media_type=content_type,
            headers={"Cache-Control": "public, max-age=2592000"},
        )

    # Download, cache, and serve
    try:
        async with httpx.AsyncClient(timeout=15, follow_redirects=True) as client:
            resp = await client.get(url, headers={
                "User-Agent": settings.SCRAPE_USER_AGENT,
                "Referer": "https://donghuafun.com/",
            })
            if resp.status_code == 200:
                filepath.write_bytes(resp.content)
                content_type = resp.headers.get("content-type", "image/webp")
                return Response(
                    content=resp.content,
                    media_type=content_type,
                    headers={"Cache-Control": "public, max-age=2592000"},
                )
    except Exception as e:
        logger.error(f"Proxy failed for {url}: {e}")

    raise HTTPException(status_code=502, detail="Failed to fetch image")
