from datetime import datetime, timedelta

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.crash_log import CrashLog

router = APIRouter()


class CrashLogRequest(BaseModel):
    level: str = "error"
    message: str
    stacktrace: str | None = None
    app_version: str | None = None
    device_info: str | None = None
    screen: str | None = None
    extra: str | None = None


class CrashLogResponse(BaseModel):
    id: int
    level: str
    message: str
    stacktrace: str | None = None
    app_version: str | None = None
    device_info: str | None = None
    screen: str | None = None
    extra: str | None = None
    created_at: datetime | None = None


@router.post("", response_model=CrashLogResponse)
async def create_crash_log(req: CrashLogRequest, db: AsyncSession = Depends(get_db)):
    entry = CrashLog(
        level=req.level,
        message=req.message,
        stacktrace=req.stacktrace,
        app_version=req.app_version,
        device_info=req.device_info,
        screen=req.screen,
        extra=req.extra,
    )
    db.add(entry)
    await db.commit()
    await db.refresh(entry)

    return CrashLogResponse(
        id=entry.id,
        level=entry.level,
        message=entry.message,
        stacktrace=entry.stacktrace,
        app_version=entry.app_version,
        device_info=entry.device_info,
        screen=entry.screen,
        extra=entry.extra,
        created_at=entry.created_at,
    )


@router.get("", response_model=list[CrashLogResponse])
async def list_crash_logs(
    level: str | None = None,
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
):
    query = select(CrashLog).order_by(CrashLog.created_at.desc()).limit(limit)
    if level:
        query = query.where(CrashLog.level == level)

    result = await db.execute(query)
    entries = result.scalars().all()

    return [
        CrashLogResponse(
            id=e.id,
            level=e.level,
            message=e.message,
            stacktrace=e.stacktrace,
            app_version=e.app_version,
            device_info=e.device_info,
            screen=e.screen,
            extra=e.extra,
            created_at=e.created_at,
        )
        for e in entries
    ]


@router.get("/stats")
async def crash_log_stats(db: AsyncSession = Depends(get_db)):
    since = datetime.utcnow() - timedelta(hours=24)
    query = (
        select(CrashLog.level, func.count(CrashLog.id))
        .where(CrashLog.created_at >= since)
        .group_by(CrashLog.level)
    )
    result = await db.execute(query)
    rows = result.all()

    stats = {level: count for level, count in rows}
    stats["total"] = sum(stats.values())
    return stats
