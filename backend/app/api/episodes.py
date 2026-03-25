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
    sources = list(result.scalars().all())

    # Get episode + website info
    ep_query = select(Episode).options(
        selectinload(Episode.website_show).selectinload(WebsiteShow.website)
    ).where(Episode.id == episode_id)
    ep_result = await db.execute(ep_query)
    episode = ep_result.scalar_one_or_none()
    if not episode:
        raise HTTPException(status_code=404, detail="Episode not found")

    website_name = episode.website_show.website.name if episode else None

    # On-demand source extraction: if no sources and episode has a page URL
    if not sources and episode.external_url:
        import logging
        logger = logging.getLogger(__name__)
        logger.info(f"On-demand source extraction for EP{episode.episode_number}: {episode.external_url}")
        try:
            from app.scraper.registry import get_scraper
            scraper = get_scraper(website_name, episode.website_show.website.base_url)
            import httpx
            async with httpx.AsyncClient(timeout=30, follow_redirects=True) as client:
                raw_sources = await scraper.scrape_episode_sources(client, episode.external_url)

            from sqlalchemy.dialects.postgresql import insert as pg_insert
            for rs in raw_sources:
                stmt = pg_insert(Source).values(
                    episode_id=episode_id,
                    source_name=rs.source_name,
                    source_key=rs.source_key,
                    raw_player_data=rs.raw_player_data,
                    source_type=rs.source_type,
                ).on_conflict_do_nothing(index_elements=["episode_id", "source_key"])
                await db.execute(stmt)
            await db.commit()

            # Re-fetch sources
            result2 = await db.execute(select(Source).where(Source.episode_id == episode_id))
            sources = list(result2.scalars().all())
            logger.info(f"Extracted {len(sources)} sources on-demand for EP{episode.episode_number}")
        except Exception as e:
            import logging
            logging.getLogger(__name__).error(f"On-demand source extraction failed: {e}")

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
