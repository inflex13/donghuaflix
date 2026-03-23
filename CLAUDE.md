# DonghuaFlix — Development Guide

## Project Overview

Personal Netflix-like Android TV app for watching Chinese animation (donghua). Two components: a Python backend (FastAPI) that scrapes content from donghuafun.com and animekhor.org, and a Kotlin Android TV app that streams video with ExoPlayer.

## Quick Start

### Backend
```bash
cd backend
docker compose up -d
# Seed websites: docker exec backend-api-1 python -m app.seed
# Full scrape: docker exec backend-worker-1 celery -A app.scraper.tasks call app.scraper.tasks.initial_full_scrape
```

### Android TV App
```bash
cd android-tv
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

## Architecture

- **Backend**: FastAPI + PostgreSQL + Redis + Celery + Playwright, all in Docker Compose behind Caddy (SSL)
- **Android TV**: Kotlin + Jetpack Compose for TV + ExoPlayer (Media3) + Room + Hilt + Retrofit
- **VPS**: Hostinger KVM2, Ubuntu 25.10, `31.220.104.18`
- **Domain**: `api.donghuaflix.cloud` (API), `dl.donghuaflix.cloud` (APK download)

## Key Conventions

### Versioning & Deployment
- Version is tracked in TWO places that must stay in sync:
  - `android-tv/app/build.gradle.kts` — `versionCode` and `versionName`
  - `backend/app/main.py` — `get_app_version()` endpoint returns version for auto-updater
- The app has an auto-updater that checks `/app/version` endpoint
- APK is uploaded to VPS at `/opt/donghuaflix/backend/downloads/DonghuaFlix.apk`
- Backend code is volume-mounted at `/opt/donghuaflix/backend/app` — deploy by copying files and restarting container

### Video Sources
- **DonghuaFun**: Dailymotion = 4K quality. This is the preferred source
- **AnimeKhor**: ok.ru and RumblePlayer sources extracted via Playwright
- SRT subtitles come from Dailymotion metadata API, fetched fresh on each resolve (not cached in DB)
- Source resolution is on-demand via `POST /api/sources/{id}/resolve`

### Scraping
- Two scrapers registered via `@register_scraper` decorator in `scraper/registry.py`
- DonghuaFun uses MacCMS API (`/api.php/provide/vod/`)
- AnimeKhor uses Playwright to extract video sources from episode pages
- Shows are deduplicated across websites by normalized title
- Celery beat runs incremental scrapes every 30 minutes

### Android TV App
- All screens use Compose for TV with custom D-pad navigation
- Theme: dark cinematic with purple/fuchsia gradient accents (`ui/theme/`)
- Player controls are custom overlay (not ExoPlayer's built-in controller)
- Focus states use fuchsia border highlight
- ShowCard component is shared across home, browse, search, watchlist screens

## Common Tasks

### Adding a new show manually
```bash
curl -X POST "https://api.donghuaflix.cloud/api/add-show?url=<show_url>"
```

### Deploying backend changes
```bash
# Copy changed file to VPS
cat backend/app/<file>.py | ssh root@31.220.104.18 "cat > /opt/donghuaflix/backend/app/<file>.py"
# Restart API
ssh root@31.220.104.18 "docker restart backend-api-1"
# For worker changes: docker restart backend-worker-1
```

### Building and deploying a new app version
1. Bump `versionCode` and `versionName` in `android-tv/app/build.gradle.kts`
2. Build: `cd android-tv && ./gradlew assembleRelease`
3. Upload: `scp android-tv/app/build/outputs/apk/release/app-release.apk root@31.220.104.18:/opt/donghuaflix/backend/downloads/DonghuaFlix.apk`
4. Update version in `backend/app/main.py` `get_app_version()`
5. Deploy main.py to VPS and restart API
6. App auto-updater will detect new version

### Checking logs
```bash
ssh root@31.220.104.18 "docker logs backend-api-1 --tail 50"
ssh root@31.220.104.18 "docker logs backend-worker-1 --tail 50"
```

### Querying the database
```bash
ssh root@31.220.104.18 "docker exec backend-db-1 psql -U postgres donghuaflix -c '<SQL>'"
```

## Important Files

| File | Purpose |
|------|---------|
| `backend/app/main.py` | FastAPI app, add-show endpoint, version endpoint |
| `backend/app/api/sources.py` | Source resolution with subtitle fetching |
| `backend/app/services/source_resolver.py` | Dailymotion/ok.ru/Rumble/embed URL extraction |
| `backend/app/scraper/donghuafun.py` | DonghuaFun scraper (MacCMS API) |
| `backend/app/scraper/animekhor.py` | AnimeKhor scraper (WordPress + Playwright) |
| `backend/app/scraper/tasks.py` | Celery scheduled scraping tasks |
| `backend/app/services/sync_service.py` | DB sync logic for scraped data |
| `android-tv/app/build.gradle.kts` | App version, dependencies, signing |
| `android-tv/.../ui/player/PlayerScreen.kt` | ExoPlayer + subtitle rendering + D-pad controls |
| `android-tv/.../ui/player/PlayerViewModel.kt` | Player state, source resolution, episode navigation |
| `android-tv/.../ui/home/HomeScreen.kt` | Netflix-style home with hero banner |
| `android-tv/.../ui/detail/DetailScreen.kt` | Show detail + episode grid |
| `android-tv/.../data/repository/ShowRepository.kt` | API calls + subtitle mapping |
| `android-tv/.../sync/AppUpdater.kt` | In-app update checker |
