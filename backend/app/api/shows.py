from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models import Show, Website, WebsiteShow
from app.schemas.show import ShowListResponse, ShowResponse, WebsiteInfo

router = APIRouter()


def _show_to_response(show: Show, website_shows: list[WebsiteShow] | None = None) -> ShowResponse:
    websites = []
    if website_shows:
        for ws in website_shows:
            websites.append(WebsiteInfo(
                id=ws.website.id,
                name=ws.website.name,
                display_name=ws.website.display_name,
                episode_count=ws.episode_count_on_site,
            ))
    elif show.website_shows:
        for ws in show.website_shows:
            websites.append(WebsiteInfo(
                id=ws.website.id,
                name=ws.website.name,
                display_name=ws.website.display_name,
                episode_count=ws.episode_count_on_site,
            ))

    return ShowResponse(
        id=show.id,
        title=show.title,
        title_chinese=show.title_chinese,
        slug=show.slug,
        poster_url=show.poster_url,
        description=show.description,
        rating=float(show.rating) if show.rating else None,
        year=show.year,
        status=show.status,
        genres=show.genres,
        total_episodes=show.total_episodes,
        category=show.category,
        created_at=show.created_at,
        updated_at=show.updated_at,
        remote_updated_at=show.remote_updated_at,
        websites=websites,
    )


@router.get("", response_model=ShowListResponse)
async def list_shows(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    genre: str | None = None,
    status: str | None = None,
    category: str | None = None,
    website: str | None = None,
    db: AsyncSession = Depends(get_db),
):
    query = select(Show).options(
        selectinload(Show.website_shows).selectinload(WebsiteShow.website)
    )

    if genre:
        query = query.where(Show.genres.any(genre))
    if status:
        query = query.where(Show.status == status)
    if category:
        query = query.where(Show.category == category)
    if website:
        query = query.join(Show.website_shows).join(WebsiteShow.website).where(Website.name == website)

    count_query = select(func.count()).select_from(query.subquery())
    total = (await db.execute(count_query)).scalar() or 0

    # Sort by website_show's timestamp when filtering by website, otherwise by show's
    if website:
        query = query.order_by(WebsiteShow.remote_updated_at.desc().nulls_last(), Show.updated_at.desc())
    else:
        query = query.order_by(Show.remote_updated_at.desc().nulls_last(), Show.updated_at.desc())
    query = query.offset((page - 1) * page_size).limit(page_size)
    result = await db.execute(query)
    shows = result.scalars().unique().all()

    return ShowListResponse(
        items=[_show_to_response(s) for s in shows],
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/search", response_model=ShowListResponse)
async def search_shows(
    q: str = Query(..., min_length=1),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
):
    query = select(Show).options(
        selectinload(Show.website_shows).selectinload(WebsiteShow.website)
    ).where(Show.title.ilike(f"%{q}%"))

    count_query = select(func.count()).select_from(query.subquery())
    total = (await db.execute(count_query)).scalar() or 0

    query = query.order_by(Show.title).offset((page - 1) * page_size).limit(page_size)
    result = await db.execute(query)
    shows = result.scalars().unique().all()

    return ShowListResponse(
        items=[_show_to_response(s) for s in shows],
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/{show_id}", response_model=ShowResponse)
async def get_show(show_id: int, db: AsyncSession = Depends(get_db)):
    query = select(Show).options(
        selectinload(Show.website_shows).selectinload(WebsiteShow.website)
    ).where(Show.id == show_id)
    result = await db.execute(query)
    show = result.scalar_one_or_none()
    if not show:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="Show not found")
    return _show_to_response(show)


@router.get("/{show_id}/websites")
async def get_show_websites(show_id: int, db: AsyncSession = Depends(get_db)):
    query = select(WebsiteShow).options(
        selectinload(WebsiteShow.website)
    ).where(WebsiteShow.show_id == show_id)
    result = await db.execute(query)
    website_shows = result.scalars().all()
    return [
        WebsiteInfo(
            id=ws.website.id,
            name=ws.website.name,
            display_name=ws.website.display_name,
            episode_count=ws.episode_count_on_site,
        )
        for ws in website_shows
    ]
