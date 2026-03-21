from datetime import datetime

from sqlalchemy import ARRAY, DateTime, Integer, Numeric, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Show(Base):
    __tablename__ = "shows"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    title_normalized: Mapped[str] = mapped_column(String(500), unique=True, nullable=False)
    title_chinese: Mapped[str | None] = mapped_column(String(500), nullable=True)
    slug: Mapped[str | None] = mapped_column(String(500), nullable=True)
    poster_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    rating: Mapped[float | None] = mapped_column(Numeric(3, 1), nullable=True)
    rating_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    year: Mapped[int | None] = mapped_column(Integer, nullable=True)
    status: Mapped[str | None] = mapped_column(String(50), nullable=True)
    genres: Mapped[list[str] | None] = mapped_column(ARRAY(String), nullable=True)
    region: Mapped[str | None] = mapped_column(String(100), nullable=True)
    language: Mapped[str | None] = mapped_column(String(100), nullable=True)
    total_episodes: Mapped[int | None] = mapped_column(Integer, nullable=True)
    category: Mapped[str | None] = mapped_column(String(50), nullable=True)
    remote_updated_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    website_shows: Mapped[list["WebsiteShow"]] = relationship(back_populates="show")
    watch_history: Mapped[list["WatchHistory"]] = relationship(back_populates="show")
    watchlist_entry: Mapped["Watchlist | None"] = relationship(back_populates="show")


from app.models.watch_history import WatchHistory  # noqa: E402
from app.models.watchlist import Watchlist  # noqa: E402
from app.models.website_show import WebsiteShow  # noqa: E402
