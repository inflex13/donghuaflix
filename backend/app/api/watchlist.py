from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models import Show, Watchlist, WebsiteShow
from app.schemas.show import ShowResponse

router = APIRouter()


@router.get("", response_model=list[ShowResponse])
async def get_watchlist(db: AsyncSession = Depends(get_db)):
    query = (
        select(Watchlist)
        .options(
            selectinload(Watchlist.show)
            .selectinload(Show.website_shows)
            .selectinload(WebsiteShow.website)
        )
        .order_by(Watchlist.added_at.desc())
    )
    result = await db.execute(query)
    entries = result.scalars().all()

    from app.api.shows import _show_to_response
    return [_show_to_response(e.show) for e in entries]


@router.post("/{show_id}", status_code=201)
async def add_to_watchlist(show_id: int, db: AsyncSession = Depends(get_db)):
    # Check show exists
    show = await db.get(Show, show_id)
    if not show:
        raise HTTPException(status_code=404, detail="Show not found")

    # Check if already in watchlist
    existing = await db.execute(
        select(Watchlist).where(Watchlist.show_id == show_id)
    )
    if existing.scalar_one_or_none():
        return {"message": "Already in watchlist"}

    entry = Watchlist(show_id=show_id)
    db.add(entry)
    await db.commit()
    return {"message": "Added to watchlist"}


@router.delete("/{show_id}")
async def remove_from_watchlist(show_id: int, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        delete(Watchlist).where(Watchlist.show_id == show_id)
    )
    if result.rowcount == 0:
        raise HTTPException(status_code=404, detail="Not in watchlist")
    await db.commit()
    return {"message": "Removed from watchlist"}
