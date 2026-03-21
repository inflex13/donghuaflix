from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Episode(Base):
    __tablename__ = "episodes"
    __table_args__ = (UniqueConstraint("website_show_id", "episode_number"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    website_show_id: Mapped[int] = mapped_column(ForeignKey("website_shows.id", ondelete="CASCADE"))
    episode_number: Mapped[int] = mapped_column(Integer, nullable=False)
    title: Mapped[str | None] = mapped_column(String(500), nullable=True)
    external_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    website_show: Mapped["WebsiteShow"] = relationship(back_populates="episodes")
    sources: Mapped[list["Source"]] = relationship(back_populates="episode", cascade="all, delete-orphan")
    watch_history: Mapped[list["WatchHistory"]] = relationship(back_populates="episode")


from app.models.source import Source  # noqa: E402
from app.models.watch_history import WatchHistory  # noqa: E402
from app.models.website_show import WebsiteShow  # noqa: E402
