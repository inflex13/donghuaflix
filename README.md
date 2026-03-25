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

**Home Screen**
- Netflix-style horizontal scrolling rows (Continue Watching, per-website rows, genre rows)
- Hero banner with poster art, genres, rating, year, episode count, "Watch Now" button
- Splash screen with custom donghua artwork and branding
- Top nav bar: Browse, Search, My List, Sync, Update, Exit

**Show Detail Screen**
- Cinematic layout with poster, metadata (rating, year, status, genres)
- Website selector (DonghuaFun / AnimeKhor) when show exists on multiple sites
- Episode grid with watch history indicators (checkmarks for watched, progress bars)
- Resume button for continue watching
- Mark All Watched / Mark as Unwatched per episode (immediately synced to server)
- Watchlist toggle (+ My List)

**Video Player**
- ExoPlayer with HLS, DASH, MP4 support
- SRT subtitles from Dailymotion (10 languages, English auto-selected)
- Subtitle controls: CC On/Off, Size (S/M/L), Background toggle (30% black / transparent)
- All subtitle settings persisted to device via SharedPreferences
- D-pad controls: seek bar, play/pause, source switcher, subtitle controls, prev/next EP, autoplay toggle
- Multi-source with auto-fallback (tries each source until one works)
- Watch progress saved every 10 seconds + on pause/exit
- Keep screen on during playback (prevents screensaver)
- Resume from last position

**Browse Screen**
- Filter by website (All / DonghuaFun / AnimeKhor)
- Filter by genre (scrolling genre chips)
- Grid layout with show cards

**Search**
- Real-time search across all shows

**Watchlist**
- "My List" with all saved shows
- Gradient glow indicator on cards across all screens

**System**
- In-app auto-updater (checks `/app/version`, downloads and installs APK)
- Full resync button (wipes local + re-pulls from server)
- Background sync via WorkManager every 2 hours
- Every action pushes to server immediately (mark watched, progress, watchlist)
- Crash logging — uncaught exceptions + player errors sent to server
- Release signed APK served at `dl.donghuaflix.cloud`

**Theme**
- Dark cinematic theme (Obsidian background)
- Purple/fuchsia gradient accents
- Fuchsia focus borders for D-pad navigation
- Custom show cards with poster images, rating badges, episode counts

### Backend

**API (FastAPI)**
- REST API with async SQLAlchemy + PostgreSQL
- Show listing with filters (genre, status, website, search)
- Episode listing per show per website
- On-demand video URL resolution (Dailymotion, ok.ru, Rumble, embed)
- SRT subtitle fetching from Dailymotion metadata API
- Watch progress tracking + continue watching
- Watchlist management
- Delta sync endpoint
- Home screen aggregate endpoint
- Add-show-by-URL endpoint (paste a URL, auto-scrape)
- App version endpoint for auto-updater
- Crash log collection + stats endpoint
- Image/poster caching on VPS (JPEG conversion for TV compatibility)

**Scrapers**
- Pluggable scraper architecture (`BaseScraper` + `@register_scraper` decorator)
- **DonghuaFun** (MacCMS API) — 4K Dailymotion sources with SRT subtitles
- **AnimeKhor** (WordPress + Playwright) — ok.ru + RumblePlayer sources
- Title deduplication across websites (normalized matching)
- Incremental scraping every 30 minutes (Celery Beat)
- Full scrape on demand
- Auto source extraction for new episodes

**Video Source Resolution**
- Dailymotion → HLS stream + SRT subtitles (cached with expiry)
- ok.ru → HLS stream
- Rumble → HLS/MP4
- play.donghuafun.com embeds → Playwright network interception

## Tech Stack

| Component | Technology |
|-----------|-----------|
| TV App | Kotlin, Jetpack Compose for TV, ExoPlayer (Media3) |
| Local DB | Room |
| Networking | Retrofit + OkHttp |
| DI | Hilt |
| Image Loading | Coil (100MB disk cache) |
| Background Sync | WorkManager |
| Subtitle Prefs | SharedPreferences |
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
| Crash Logs | `https://api.donghuaflix.cloud/api/crash-logs` |
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
│   │   │   └── crash_log.py    # Crash/error log model
│   │   ├── schemas/            # Pydantic schemas
│   │   ├── api/
│   │   │   ├── shows.py        # Show CRUD
│   │   │   ├── episodes.py     # Episode listing
│   │   │   ├── sources.py      # Source resolution + subtitles
│   │   │   ├── watch.py        # Watch progress
│   │   │   ├── watchlist.py    # Watchlist
│   │   │   ├── sync.py         # Delta sync
│   │   │   ├── discovery.py    # Home, genres, websites
│   │   │   ├── crash_logs.py   # Crash log collection
│   │   │   └── assets.py       # Image serving
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
│   │       ├── DonghuaFlixApp.kt       # App class + crash handler
│   │       ├── di/                     # Hilt modules
│   │       ├── data/
│   │       │   ├── local/              # Room DB, DAOs, entities
│   │       │   │   └── SubtitlePreferences.kt  # Persisted subtitle settings
│   │       │   ├── remote/             # Retrofit API, DTOs
│   │       │   │   └── CrashLogger.kt  # Error logging singleton
│   │       │   ├── repository/         # Repositories (push-first sync)
│   │       │   └── mapper/
│   │       ├── domain/model/           # Domain models
│   │       ├── ui/
│   │       │   ├── theme/              # Dark cinematic theme
│   │       │   ├── navigation/         # Nav graph
│   │       │   ├── home/               # Netflix-style home
│   │       │   ├── detail/             # Show detail + episodes
│   │       │   ├── player/             # ExoPlayer + D-pad controls + subtitles
│   │       │   ├── browse/             # Genre browser
│   │       │   ├── search/
│   │       │   ├── watchlist/
│   │       │   └── components/         # ShowCard, etc.
│   │       └── sync/                   # AppUpdater, SyncWorker
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
POST /api/sources/{id}/resolve     — Resolve video URL + subtitles on-demand

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

# Crash Logs
POST /api/crash-logs               — Submit crash/error log
GET  /api/crash-logs               — List recent logs (filter by level)
GET  /api/crash-logs/stats         — Count by level (last 24h)

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
- `crash_logs` — App error/crash logs with device info

## Video Source Priority

| Website | Source | Type | Quality |
|---------|--------|------|---------|
| DonghuaFun | Dailymotion | HLS | 4K with SRT subtitles |
| AnimeKhor | RumblePlayer | MP4/HLS | HD (recent episodes) |
| AnimeKhor | ok.ru | HLS | HD (stable, older episodes) |

## License

Private project — personal use only.
