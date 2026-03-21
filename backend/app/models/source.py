from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Source(Base):
    __tablename__ = "sources"
    __table_args__ = (UniqueConstraint("episode_id", "source_key"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    episode_id: Mapped[int] = mapped_column(ForeignKey("episodes.id", ondelete="CASCADE"))
    source_name: Mapped[str] = mapped_column(String(100), nullable=False)
    source_key: Mapped[str] = mapped_column(String(100), nullable=False)
    source_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    source_type: Mapped[str | None] = mapped_column(String(20), nullable=True)
    url_expires_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    raw_player_data: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    episode: Mapped["Episode"] = relationship(back_populates="sources")


from app.models.episode import Episode  # noqa: E402
