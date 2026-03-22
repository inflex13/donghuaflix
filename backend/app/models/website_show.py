from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class WebsiteShow(Base):
    __tablename__ = "website_shows"
    __table_args__ = (UniqueConstraint("website_id", "external_id"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    show_id: Mapped[int] = mapped_column(ForeignKey("shows.id", ondelete="CASCADE"))
    website_id: Mapped[int] = mapped_column(ForeignKey("websites.id", ondelete="CASCADE"))
    external_id: Mapped[str] = mapped_column(String(200), nullable=False)
    external_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    title_on_site: Mapped[str | None] = mapped_column(String(500), nullable=True)
    poster_url_on_site: Mapped[str | None] = mapped_column(Text, nullable=True)
    episode_count_on_site: Mapped[int | None] = mapped_column(Integer, nullable=True)
    remote_updated_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    show: Mapped["Show"] = relationship(back_populates="website_shows")
    website: Mapped["Website"] = relationship(back_populates="website_shows")
    episodes: Mapped[list["Episode"]] = relationship(back_populates="website_show", cascade="all, delete-orphan")


from app.models.episode import Episode  # noqa: E402
from app.models.show import Show  # noqa: E402
from app.models.website import Website  # noqa: E402
