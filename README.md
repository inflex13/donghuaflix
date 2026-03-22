# DonghuaFlix

Personal Netflix-like Android TV app for watching Chinese animation (donghua). Scrapes content from multiple websites, streams 4K video with subtitles, and tracks watch progress.

## Architecture

```
┌─────────────────────┐     ┌──────────────────────────────────┐
│   Android TV App    │     │     VPS (Hostinger KVM2)         │
│   (Kotlin/Compose)  │◄───►│                                  │
│                     │ API │  ┌─────────┐  ┌──────────────┐   │
│  ┌───────────────┐  │     │  │ FastAPI  │  │ Celery Worker│   │
│  │ Room DB       │  │     │  │ + Caddy  │  │ + Playwright │   │
│  │ (local cache) │  │     │  └────┬────┘  └──────┬───────┘   │
│  └───────────────┘  │     │       │              │           │
│  ┌───────────────┐  │     │  ┌────▼──────────────▼───────┐   │
│  │ ExoPlayer     │  │     │  │     PostgreSQL + Redis     │   │
│  │ (multi-source)│  │     │  └────────────────────────────┘   │
│  └───────────────┘  │     │                                  │
└─────────────────────┘     │  donghuafun.com ◄── scrapers     │
                            │  animekhor.org  ◄──              │
                            └──────────────────────────────────┘
```

## Features

### Android TV App
- Netflix-style home screen with Continue Watching, per-website rows, genre rows
- Show detail with website selector, episode grid with watch history indicators
- ExoPlayer with HLS/DASH/MP4, English subtitles (SRT from Dailymotion)
- Multi-source video with D-pad controls (seek bar, play/pause, source switcher, prev/next EP, autoplay)
- Source auto-fallback (Rumble → ok.ru if source is removed)
- Watch progress tracking with resume
- Mark All Watched button
- Watchlist with gradient glow indicator on cards
- In-app update checker with modal dialog
- Full resync button
- Splash screen with custom donghua artwork
- Release signed APK served at `dl.donghuaflix.cloud`

### Backend
- **FastAPI** REST API with async SQLAlchemy
- **PostgreSQL** database with multi-website dedup
- **Celery + Redis** for scheduled scraping (every 30 min)
- **Playwright** for extracting video sources from AnimeKhor episode pages
- **Caddy** reverse proxy with automatic SSL
- **Two website scrapers**:
  - **DonghuaFun** (MacCMS API) — 4K Dailymotion sources with subtitles
  - **AnimeKhor** (WordPress) — ok.ru + RumblePlayer sources via Playwright
- Title deduplication across websites with normalized matching
- Image caching on VPS (JPEG conversion for TV compatibility)
- Add-show-by-URL endpoint for manual additions
- Incremental scraping with auto source extraction for new episodes

## Tech Stack

| Component | Technology |
|-----------|-----------|
| TV App | Kotlin, Jetpack Compose for TV, ExoPlayer (Media3) |
| Local DB | Room |
| Networking | Retrofit + OkHttp |
| DI | Hilt |
| Image Loading | Coil (100MB disk cache) |
| Background Sync | WorkManager |
| Backend API | Python, FastAPI, SQLAlchemy (async) |
| Database | PostgreSQL 16 |
| Task Queue | Celery + Redis |
| Scraping | httpx, BeautifulSoup, Playwright |
| SSL/Proxy | Caddy |
| Hosting | Hostinger KVM2 VPS (2 CPU, 8GB RAM, Ubuntu 25.10) |

## Infrastructure

| Service | URL |
|---------|-----|
| API | `https://api.donghuaflix.cloud` |
| APK Download | `https://dl.donghuaflix.cloud` |
| Swagger Docs | `https://api.donghuaflix.cloud/docs` |
| Database | `31.220.104.18:5432` |

## Project Structure

```
├── backend/
│   ├── docker-compose.yml      # All services
│   ├── Dockerfile
│   ├── Caddyfile               # SSL + reverse proxy
│   ├── app/
│   │   ├── main.py             # FastAPI app + endpoints
│   │   ├── config.py
│   │   ├── database.py
│   │   ├── models/             # SQLAlchemy models
│   │   ├── schemas/            # Pydantic schemas
│   │   ├── api/                # REST endpoints
│   │   ├── scraper/
│   │   │   ├── base.py         # Abstract BaseScraper
│   │   │   ├── donghuafun.py   # MacCMS API scraper
│   │   │   ├── animekhor.py    # WordPress + Playwright scraper
│   │   │   ├── dedup.py        # Title normalization
│   │   │   ├── registry.py     # Scraper registry
│   │   │   └── tasks.py        # Celery scheduled tasks
│   │   └── services/
│   │       ├── sync_service.py
│   │       └── source_resolver.py  # Dailymotion, Rumble, ok.ru resolvers
│
├── android-tv/
│   ├── app/
│   │   └── src/main/java/com/donghuaflix/
│   │       ├── di/             # Hilt modules
│   │       ├── data/
│   │       │   ├── local/      # Room DB, DAOs, entities
│   │       │   ├── remote/     # Retrofit API, DTOs
│   │       │   ├── repository/ # Offline-first repositories
│   │       │   └── mapper/
│   │       ├── domain/model/   # Domain models
│   │       ├── ui/
│   │       │   ├── theme/      # Dark cinematic theme
│   │       │   ├── navigation/ # Nav graph
│   │       │   ├── home/       # Netflix-style home
│   │       │   ├── detail/     # Show detail + episodes
│   │       │   ├── player/     # ExoPlayer + D-pad controls
│   │       │   ├── browse/     # Genre browser
│   │       │   ├── search/
│   │       │   ├── watchlist/
│   │       │   └── components/ # ShowCard, etc.
│   │       └── sync/           # AppUpdater, SyncWorker
│
├── assets/                     # Brand assets, splash screens
└── .env                        # Credentials (gitignored)
```

## API Endpoints

```
# Shows
GET  /api/shows                    — List shows (filters: genre, status, website)
GET  /api/shows/{id}               — Show detail + available websites
GET  /api/shows/search?q=          — Search
GET  /api/shows/{id}/episodes      — Episodes (filter by website)

# Sources
GET  /api/episodes/{id}/sources    — Stream providers for episode
POST /api/sources/{id}/resolve     — Resolve video URL on-demand

# Watch Tracking
POST /api/watch/progress           — Update watch progress
GET  /api/watch/continue           — Continue Watching list
GET  /api/watch/history            — Full history

# Watchlist
POST /api/watchlist/{showId}       — Add to watchlist
DELETE /api/watchlist/{showId}     — Remove
GET  /api/watchlist                — Get watchlist

# Sync & Discovery
GET  /api/sync                     — Delta sync
GET  /api/home                     — Home screen data
GET  /api/genres                   — All genres
GET  /api/websites                 — Registered websites

# Management
POST /api/add-show?url=            — Scrape and add show by URL
GET  /app/version                  — Latest app version for updates
GET  /health                       — Health check
```

## Deployment

### Backend (VPS)
```bash
cd backend
docker compose up -d
# Seed websites
docker exec backend-api-1 python -m app.seed
# Initial full scrape
docker exec backend-worker-1 celery -A app.scraper.tasks call app.scraper.tasks.initial_full_scrape
```

### Android TV App
```bash
cd android-tv
./gradlew assembleRelease
# APK at app/build/outputs/apk/release/app-release.apk
# Sideload: adb install app-release.apk
# Or download from dl.donghuaflix.cloud
```

### Adding Shows Manually
```bash
# Via API
curl -X POST "https://api.donghuaflix.cloud/api/add-show?url=https://animekhor.org/anime/against-the-gods/"

# Supported URLs:
# https://animekhor.org/anime/<slug>/
# https://donghuafun.com/index.php/vod/detail/id/<id>.html
```

## Database Schema

- `websites` — Registered scraping sites
- `shows` — Canonical deduplicated shows
- `website_shows` — Per-site show entries linked to canonical
- `episodes` — Per website_show
- `sources` — Video stream providers per episode (ok.ru, Rumble, Dailymotion)
- `watch_history` — Playback progress tracking
- `watchlist` — User's "My List"

## Video Source Priority

| Website | Source | Type | Quality |
|---------|--------|------|---------|
| DonghuaFun | Dailymotion | HLS | 4K with SRT subtitles |
| AnimeKhor | RumblePlayer | MP4/HLS | HD (recent episodes) |
| AnimeKhor | ok.ru | HLS | HD (stable, older episodes) |

## License

Private project — personal use only.
