from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Watchlist(Base):
    __tablename__ = "watchlist"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    show_id: Mapped[int] = mapped_column(ForeignKey("shows.id", ondelete="CASCADE"), unique=True)
    added_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    show: Mapped["Show"] = relationship(back_populates="watchlist_entry")


from app.models.show import Show  # noqa: E402
