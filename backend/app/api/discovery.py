from fastapi import APIRouter, Depends
from sqlalchemy import distinct, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models import Show, Watchlist, WatchHistory, Website, WebsiteShow
from app.schemas.show import HomeResponse, HomeSection, ShowResponse

router = APIRouter()


@router.get("/genres")
async def list_genres(db: AsyncSession = Depends(get_db)):
    query = select(func.unnest(Show.genres)).distinct()
    result = await db.execute(query)
    genres = sorted([row[0] for row in result.all() if row[0]])
    return genres


@router.get("/websites")
async def list_websites(db: AsyncSession = Depends(get_db)):
    query = select(Website).where(Website.enabled == True)  # noqa: E712
    result = await db.execute(query)
    websites = result.scalars().all()
    return [
        {
            "id": w.id,
            "name": w.name,
            "display_name": w.display_name,
            "base_url": w.base_url,
        }
        for w in websites
    ]


@router.get("/home", response_model=HomeResponse)
async def get_home(db: AsyncSession = Depends(get_db)):
    from app.api.shows import _show_to_response

    sections = []

    # Continue Watching
    cw_query = (
        select(WatchHistory)
        .where(WatchHistory.completed == False)  # noqa: E712
        .order_by(WatchHistory.watched_at.desc())
        .limit(10)
    )
    cw_result = await db.execute(cw_query)
    cw_entries = cw_result.scalars().all()
    if cw_entries:
        show_ids = [e.show_id for e in cw_entries]
        shows_query = select(Show).options(
            selectinload(Show.website_shows).selectinload(WebsiteShow.website)
        ).where(Show.id.in_(show_ids))
        shows_result = await db.execute(shows_query)
        shows = {s.id: s for s in shows_result.scalars().unique().all()}
        sections.append(HomeSection(
            title="Continue Watching",
            section_type="continue_watching",
            shows=[_show_to_response(shows[e.show_id]) for e in cw_entries if e.show_id in shows],
        ))

    # Recently Added
    recent_query = select(Show).options(
        selectinload(Show.website_shows).selectinload(WebsiteShow.website)
    ).order_by(Show.created_at.desc()).limit(20)
    recent_result = await db.execute(recent_query)
    recent_shows = recent_result.scalars().unique().all()
    if recent_shows:
        sections.append(HomeSection(
            title="Recently Added",
            section_type="recently_added",
            shows=[_show_to_response(s) for s in recent_shows],
        ))

    # By Genre (top 3 genres)
    genre_query = select(
        func.unnest(Show.genres).label("genre"),
        func.count().label("cnt"),
    ).group_by("genre").order_by(func.count().desc()).limit(3)
    genre_result = await db.execute(genre_query)
    top_genres = [row[0] for row in genre_result.all() if row[0]]

    for genre in top_genres:
        genre_shows_query = select(Show).options(
            selectinload(Show.website_shows).selectinload(WebsiteShow.website)
        ).where(Show.genres.any(genre)).order_by(Show.rating.desc().nulls_last()).limit(20)
        genre_shows_result = await db.execute(genre_shows_query)
        genre_shows = genre_shows_result.scalars().unique().all()
        if genre_shows:
            sections.append(HomeSection(
                title=genre,
                section_type="genre",
                shows=[_show_to_response(s) for s in genre_shows],
            ))

    # Completed Series
    completed_query = select(Show).options(
        selectinload(Show.website_shows).selectinload(WebsiteShow.website)
    ).where(Show.status == "completed").order_by(Show.rating.desc().nulls_last()).limit(20)
    completed_result = await db.execute(completed_query)
    completed_shows = completed_result.scalars().unique().all()
    if completed_shows:
        sections.append(HomeSection(
            title="Completed Series",
            section_type="completed",
            shows=[_show_to_response(s) for s in completed_shows],
        ))

    return HomeResponse(sections=sections)


@router.get("/shows/{show_id}/episodes")
async def get_show_episodes(
    show_id: int,
    website: str | None = None,
    db: AsyncSession = Depends(get_db),
):
    from app.models import Episode
    from app.schemas.show import EpisodeResponse

    query = select(Episode).join(WebsiteShow).where(WebsiteShow.show_id == show_id)
    if website:
        query = query.join(Website).where(Website.name == website)
    query = query.order_by(Episode.episode_number)

    result = await db.execute(query)
    episodes = result.scalars().all()

    # Get website name for each episode
    ws_query = select(WebsiteShow).options(
        selectinload(WebsiteShow.website)
    ).where(WebsiteShow.show_id == show_id)
    ws_result = await db.execute(ws_query)
    ws_map = {ws.id: ws.website.name for ws in ws_result.scalars().all()}

    return [
        EpisodeResponse(
            id=ep.id,
            episode_number=ep.episode_number,
            title=ep.title,
            external_url=ep.external_url,
            website_name=ws_map.get(ep.website_show_id),
        )
        for ep in episodes
    ]
