from datetime import datetime

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models import Show, Watchlist, WatchHistory, WebsiteShow
from app.schemas.show import ShowResponse, SyncResponse, WatchProgressResponse

router = APIRouter()


@router.get("", response_model=SyncResponse)
async def sync_data(
    since: datetime | None = Query(None),
    db: AsyncSession = Depends(get_db),
):
    from app.api.shows import _show_to_response

    # Get shows updated since timestamp (or all if no timestamp)
    show_query = select(Show).options(
        selectinload(Show.website_shows).selectinload(WebsiteShow.website)
    )
    if since:
        show_query = show_query.where(Show.updated_at > since)
    show_query = show_query.order_by(Show.remote_updated_at.desc().nulls_last(), Show.updated_at.desc())

    result = await db.execute(show_query)
    shows = result.scalars().unique().all()

    # Get watch history
    wh_query = select(WatchHistory)
    if since:
        wh_query = wh_query.where(WatchHistory.watched_at > since)
    wh_result = await db.execute(wh_query)
    watch_entries = wh_result.scalars().all()

    # Get watchlist show IDs
    wl_result = await db.execute(select(Watchlist.show_id))
    watchlist_ids = [row[0] for row in wl_result.all()]

    return SyncResponse(
        shows=[_show_to_response(s) for s in shows],
        watch_history=[
            WatchProgressResponse(
                id=e.id,
                show_id=e.show_id,
                episode_number=e.episode_number,
                progress_seconds=e.progress_seconds,
                duration_seconds=e.duration_seconds,
                completed=e.completed,
                watched_at=e.watched_at,
            )
            for e in watch_entries
        ],
        watchlist=watchlist_ids,
        timestamp=datetime.utcnow(),
    )
