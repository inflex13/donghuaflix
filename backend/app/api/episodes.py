from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.models import Episode, Source, Website, WebsiteShow
from app.schemas.show import EpisodeResponse, SourceResponse

router = APIRouter()


@router.get("/{episode_id}/sources", response_model=list[SourceResponse])
async def get_episode_sources(episode_id: int, db: AsyncSession = Depends(get_db)):
    query = select(Source).where(Source.episode_id == episode_id)
    result = await db.execute(query)
    sources = result.scalars().all()

    # Get website name for context
    ep_query = select(Episode).options(
        selectinload(Episode.website_show).selectinload(WebsiteShow.website)
    ).where(Episode.id == episode_id)
    ep_result = await db.execute(ep_query)
    episode = ep_result.scalar_one_or_none()
    website_name = episode.website_show.website.name if episode else None

    return [
        SourceResponse(
            id=s.id,
            source_name=s.source_name,
            source_key=s.source_key,
            source_url=s.source_url,
            source_type=s.source_type,
            website_name=website_name,
        )
        for s in sources
    ]
