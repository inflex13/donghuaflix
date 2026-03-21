from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql+asyncpg://postgres:donghuaflix2026@localhost:5432/donghuaflix"
    SYNC_DATABASE_URL: str = "postgresql://postgres:donghuaflix2026@localhost:5432/donghuaflix"
    REDIS_URL: str = "redis://localhost:6379/0"

    # Scraper settings
    SCRAPE_INTERVAL_MINUTES: int = 30
    SCRAPE_USER_AGENT: str = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    SCRAPE_REQUEST_DELAY: float = 1.0

    # Poster priority: which website's poster to prefer for deduplicated shows
    POSTER_PRIORITY_WEBSITE: str = "donghuafun"

    class Config:
        env_file = ".env"


settings = Settings()
