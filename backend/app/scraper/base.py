from abc import ABC, abstractmethod
from dataclasses import dataclass, field


@dataclass
class RawShow:
    """Raw show data scraped from a website."""
    external_id: str
    title: str
    title_chinese: str | None = None
    poster_url: str | None = None
    description: str | None = None
    rating: float | None = None
    rating_count: int | None = None
    year: int | None = None
    status: str | None = None  # "ongoing" or "completed"
    genres: list[str] = field(default_factory=list)
    region: str | None = None
    language: str | None = None
    total_episodes: int | None = None
    category: str | None = None  # "movie" or "donghua"
    external_url: str | None = None


@dataclass
class RawEpisode:
    """Raw episode data scraped from a website."""
    episode_number: int
    title: str | None = None
    external_url: str | None = None
    sources: list["RawSource"] = field(default_factory=list)


@dataclass
class RawSource:
    """Raw video source data scraped from a website."""
    source_name: str
    source_key: str
    raw_player_data: str | None = None  # embed URL, UUID, or direct URL
    source_type: str | None = None  # hls, mp4, dash, embed, dailymotion


@dataclass
class ShowDetail:
    """Full show detail including episodes."""
    show: RawShow
    episodes: list[RawEpisode] = field(default_factory=list)


class BaseScraper(ABC):
    """Abstract base class for website scrapers.

    Each supported website implements this interface.
    """

    def __init__(self, website_name: str, base_url: str):
        self.website_name = website_name
        self.base_url = base_url

    @abstractmethod
    async def scrape_catalog(self, full: bool = False) -> list[RawShow]:
        """Scrape the catalog of shows.

        Args:
            full: If True, scrape all pages. If False, only scrape page 1
                  for incremental updates.

        Returns:
            List of raw show data.
        """
        ...

    @abstractmethod
    async def scrape_show_detail(self, external_id: str) -> ShowDetail | None:
        """Scrape full details for a specific show.

        Args:
            external_id: The site-specific show ID.

        Returns:
            Show detail with episodes and sources, or None if not found.
        """
        ...

    @abstractmethod
    async def extract_video_url(self, raw_player_data: str) -> dict | None:
        """Extract the actual video URL from raw player data.

        Args:
            raw_player_data: The raw embed URL, UUID, or player config.

        Returns:
            Dict with 'url', 'type', and optionally 'expires_at', or None.
        """
        ...
