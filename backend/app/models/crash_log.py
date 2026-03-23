from sqlalchemy import Column, Integer, String, Text, DateTime
from datetime import datetime

from app.database import Base


class CrashLog(Base):
    __tablename__ = "crash_logs"

    id = Column(Integer, primary_key=True)
    level = Column(String(20), default="error")  # error, crash, warning
    message = Column(Text, nullable=False)
    stacktrace = Column(Text)
    app_version = Column(String(20))
    device_info = Column(String(200))
    screen = Column(String(100))  # which screen the error occurred on
    extra = Column(Text)  # JSON string for additional context
    created_at = Column(DateTime, default=datetime.utcnow)
