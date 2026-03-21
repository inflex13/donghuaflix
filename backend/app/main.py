from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import shows, episodes, sources, watch, watchlist, sync, discovery, assets


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(
    title="DonghuaFlix API",
    description="Backend API for DonghuaFlix Android TV app",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(shows.router, prefix="/api/shows", tags=["shows"])
app.include_router(episodes.router, prefix="/api/episodes", tags=["episodes"])
app.include_router(sources.router, prefix="/api/sources", tags=["sources"])
app.include_router(watch.router, prefix="/api/watch", tags=["watch"])
app.include_router(watchlist.router, prefix="/api/watchlist", tags=["watchlist"])
app.include_router(sync.router, prefix="/api/sync", tags=["sync"])
app.include_router(discovery.router, prefix="/api", tags=["discovery"])
app.include_router(assets.router, prefix="/assets", tags=["assets"])


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/api/add-show")
async def add_show_by_url(url: str):
    """Add a show by pasting its URL from donghuafun.com or animekhor.org."""
    import re
    from sqlalchemy import create_engine, select
    from sqlalchemy.orm import Session
    from app.config import settings
    from app.models import Website
    from app.services.sync_service import sync_show_to_db, sync_detail_to_db

    import os
    sync_url = os.environ.get("SYNC_DATABASE_URL", settings.SYNC_DATABASE_URL)
    if "localhost" in sync_url:
        sync_url = sync_url.replace("localhost", "db")
    engine = create_engine(sync_url)

    # Detect which site
    if "donghuafun.com" in url:
        website_name = "donghuafun"
        id_match = re.search(r"/id/(\d+)", url)
        if not id_match:
            return {"error": "Could not parse show ID from URL"}
        external_id = id_match.group(1)
    elif "animekhor.org" in url:
        website_name = "animekhor"
        slug_match = re.search(r"/anime/([^/]+)", url)
        if not slug_match:
            return {"error": "Could not parse slug from URL"}
        external_id = slug_match.group(1).rstrip("/")
    else:
        return {"error": "Unsupported site. Use donghuafun.com or animekhor.org URLs"}

    with Session(engine) as db:
        website = db.execute(select(Website).where(Website.name == website_name)).scalar_one_or_none()
        if not website:
            return {"error": f"Website {website_name} not found in DB"}

    # Use registry to get scraper (avoids circular imports)
    from app.scraper.registry import get_scraper
    base_urls = {"donghuafun": "https://donghuafun.com", "animekhor": "https://animekhor.org"}
    scraper = get_scraper(website_name, base_urls[website_name])

    detail = await scraper.scrape_show_detail(external_id)
    if not detail:
        return {"error": f"Could not scrape show from {url}"}

    with Session(engine) as db:
        website = db.execute(select(Website).where(Website.name == website_name)).scalar_one()
        sync_detail_to_db(db, detail, website)

    return {
        "success": True,
        "title": detail.show.title,
        "episodes": len(detail.episodes),
        "website": website_name,
    }


@app.get("/app/version")
async def get_app_version():
    """Returns the latest app version info for in-app update checks."""
    import os
    apk_path = "/srv/DonghuaFlix.apk"
    apk_size = os.path.getsize(apk_path) if os.path.exists(apk_path) else 0
    return {
        "version_code": 42,
        "version_name": "2.14.2",
        "download_url": "https://dl.donghuaflix.cloud",
        "apk_size": apk_size,
        "changelog": "Player controls fix, watchlist indicator, episode watch history",
    }
