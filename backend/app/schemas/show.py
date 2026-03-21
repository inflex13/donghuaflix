from datetime import datetime

from pydantic import BaseModel


class WebsiteInfo(BaseModel):
    id: int
    name: str
    display_name: str
    episode_count: int | None = None

    class Config:
        from_attributes = True


class ShowBase(BaseModel):
    title: str
    title_chinese: str | None = None
    poster_url: str | None = None
    description: str | None = None
    rating: float | None = None
    year: int | None = None
    status: str | None = None
    genres: list[str] | None = None
    total_episodes: int | None = None
    category: str | None = None


class ShowResponse(ShowBase):
    id: int
    slug: str | None = None
    created_at: datetime
    updated_at: datetime
    remote_updated_at: datetime | None = None
    websites: list[WebsiteInfo] = []

    class Config:
        from_attributes = True


class ShowListResponse(BaseModel):
    items: list[ShowResponse]
    total: int
    page: int
    page_size: int


class EpisodeResponse(BaseModel):
    id: int
    episode_number: int
    title: str | None = None
    external_url: str | None = None
    website_name: str | None = None

    class Config:
        from_attributes = True


class SourceResponse(BaseModel):
    id: int
    source_name: str
    source_key: str
    source_url: str | None = None
    source_type: str | None = None
    website_name: str | None = None

    class Config:
        from_attributes = True


class WatchProgressRequest(BaseModel):
    show_id: int
    episode_number: int
    progress_seconds: int
    duration_seconds: int | None = None
    episode_id: int | None = None


class WatchProgressResponse(BaseModel):
    id: int
    show_id: int
    episode_number: int
    progress_seconds: int
    duration_seconds: int | None = None
    completed: bool
    watched_at: datetime
    show: ShowBase | None = None

    class Config:
        from_attributes = True


class SyncResponse(BaseModel):
    shows: list[ShowResponse]
    watch_history: list[WatchProgressResponse]
    watchlist: list[int]  # show IDs
    timestamp: datetime


class HomeSection(BaseModel):
    title: str
    section_type: str  # continue_watching, recently_added, genre, completed
    shows: list[ShowResponse]


class HomeResponse(BaseModel):
    sections: list[HomeSection]
