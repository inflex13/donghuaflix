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


@app.get("/app/version")
async def get_app_version():
    """Returns the latest app version info for in-app update checks."""
    import os
    apk_path = "/srv/DonghuaFlix.apk"
    apk_size = os.path.getsize(apk_path) if os.path.exists(apk_path) else 0
    return {
        "version_code": 26,
        "version_name": "2.8.0",
        "download_url": "https://dl.donghuaflix.cloud",
        "apk_size": apk_size,
        "changelog": "Player controls fix, watchlist indicator, episode watch history",
    }
