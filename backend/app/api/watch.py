from datetime import datetime

from fastapi import APIRouter, Depends
from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Show, WatchHistory
from app.schemas.show import WatchProgressRequest, WatchProgressResponse

router = APIRouter()


@router.post("/progress", response_model=WatchProgressResponse)
async def update_progress(req: WatchProgressRequest, db: AsyncSession = Depends(get_db)):
    # Upsert watch history
    query = select(WatchHistory).where(
        WatchHistory.show_id == req.show_id,
        WatchHistory.episode_number == req.episode_number,
    )
    result = await db.execute(query)
    entry = result.scalar_one_or_none()

    completed = False
    if req.duration_seconds and req.progress_seconds >= req.duration_seconds * 0.9:
        completed = True

    if entry:
        entry.progress_seconds = req.progress_seconds
        entry.duration_seconds = req.duration_seconds or entry.duration_seconds
        entry.completed = completed
        entry.episode_id = req.episode_id or entry.episode_id
        entry.watched_at = datetime.utcnow()
    else:
        entry = WatchHistory(
            show_id=req.show_id,
            episode_number=req.episode_number,
            episode_id=req.episode_id,
            progress_seconds=req.progress_seconds,
            duration_seconds=req.duration_seconds,
            completed=completed,
        )
        db.add(entry)

    await db.commit()
    await db.refresh(entry)

    return WatchProgressResponse(
        id=entry.id,
        show_id=entry.show_id,
        episode_number=entry.episode_number,
        progress_seconds=entry.progress_seconds,
        duration_seconds=entry.duration_seconds,
        completed=entry.completed,
        watched_at=entry.watched_at,
    )


@router.get("/continue", response_model=list[WatchProgressResponse])
async def get_continue_watching(db: AsyncSession = Depends(get_db)):
    query = (
        select(WatchHistory)
        .where(
            WatchHistory.completed == False,  # noqa: E712
            WatchHistory.progress_seconds > 0,
            WatchHistory.duration_seconds > 0,
            WatchHistory.duration_seconds < 86400,
        )
        .order_by(WatchHistory.watched_at.desc())
        .limit(20)
    )
    result = await db.execute(query)
    entries = result.scalars().all()

    return [
        WatchProgressResponse(
            id=e.id,
            show_id=e.show_id,
            episode_number=e.episode_number,
            progress_seconds=e.progress_seconds,
            duration_seconds=e.duration_seconds,
            completed=e.completed,
            watched_at=e.watched_at,
        )
        for e in entries
    ]


@router.get("/history", response_model=list[WatchProgressResponse])
async def get_watch_history(
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
):
    query = (
        select(WatchHistory)
        .order_by(WatchHistory.watched_at.desc())
        .limit(limit)
    )
    result = await db.execute(query)
    entries = result.scalars().all()

    return [
        WatchProgressResponse(
            id=e.id,
            show_id=e.show_id,
            episode_number=e.episode_number,
            progress_seconds=e.progress_seconds,
            duration_seconds=e.duration_seconds,
            completed=e.completed,
            watched_at=e.watched_at,
        )
        for e in entries
    ]
