import logging

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.config import settings
from app.models import Episode, Show, Source, Website, WebsiteShow
from app.scraper.base import RawShow, ShowDetail
from app.scraper.dedup import normalize_title

logger = logging.getLogger(__name__)


def sync_show_to_db(
    db: Session,
    raw_show: RawShow,
    website: Website,
) -> tuple[Show, WebsiteShow]:
    """Sync a scraped show into the database with deduplication.

    Returns the canonical Show and the WebsiteShow entry.
    """
    normalized = normalize_title(raw_show.title)

    # Check for existing canonical show
    existing = db.execute(
        select(Show).where(Show.title_normalized == normalized)
    ).scalar_one_or_none()

    if existing:
        show = existing
        # Update canonical show if this is the priority website (donghuafun)
        if website.name == settings.POSTER_PRIORITY_WEBSITE:
            if raw_show.poster_url:
                show.poster_url = raw_show.poster_url
            if raw_show.description:
                show.description = raw_show.description
            if raw_show.rating:
                show.rating = raw_show.rating
        # Always update certain fields if they're missing
        if not show.description and raw_show.description:
            show.description = raw_show.description
        if not show.genres and raw_show.genres:
            show.genres = raw_show.genres
        if raw_show.total_episodes and (not show.total_episodes or raw_show.total_episodes > show.total_episodes):
            show.total_episodes = raw_show.total_episodes
        if raw_show.status:
            show.status = raw_show.status
    else:
        show = Show(
            title=raw_show.title,
            title_normalized=normalized,
            title_chinese=raw_show.title_chinese,
            slug=normalized.replace(" ", "-"),
            poster_url=raw_show.poster_url,
            description=raw_show.description,
            rating=raw_show.rating,
            rating_count=raw_show.rating_count,
            year=raw_show.year,
            status=raw_show.status,
            genres=raw_show.genres or [],
            region=raw_show.region,
            language=raw_show.language,
            total_episodes=raw_show.total_episodes,
            category=raw_show.category,
        )
        db.add(show)
        db.flush()

    # Upsert website_show entry
    ws = db.execute(
        select(WebsiteShow).where(
            WebsiteShow.website_id == website.id,
            WebsiteShow.external_id == raw_show.external_id,
        )
    ).scalar_one_or_none()

    if ws:
        ws.title_on_site = raw_show.title
        ws.poster_url_on_site = raw_show.poster_url
        ws.episode_count_on_site = raw_show.total_episodes
        ws.show_id = show.id
    else:
        ws = WebsiteShow(
            show_id=show.id,
            website_id=website.id,
            external_id=raw_show.external_id,
            external_url=raw_show.external_url,
            title_on_site=raw_show.title,
            poster_url_on_site=raw_show.poster_url,
            episode_count_on_site=raw_show.total_episodes,
        )
        db.add(ws)
        db.flush()

    return show, ws


def sync_detail_to_db(
    db: Session,
    detail: ShowDetail,
    website: Website,
) -> None:
    """Sync a full show detail (with episodes and sources) into the database."""
    show, ws = sync_show_to_db(db, detail.show, website)

    for raw_ep in detail.episodes:
        # Upsert episode
        ep = db.execute(
            select(Episode).where(
                Episode.website_show_id == ws.id,
                Episode.episode_number == raw_ep.episode_number,
            )
        ).scalar_one_or_none()

        if not ep:
            ep = Episode(
                website_show_id=ws.id,
                episode_number=raw_ep.episode_number,
                title=raw_ep.title,
                external_url=raw_ep.external_url,
            )
            db.add(ep)
            db.flush()
        else:
            if raw_ep.title:
                ep.title = raw_ep.title
            if raw_ep.external_url:
                ep.external_url = raw_ep.external_url

        # Upsert sources
        for raw_src in raw_ep.sources:
            src = db.execute(
                select(Source).where(
                    Source.episode_id == ep.id,
                    Source.source_key == raw_src.source_key,
                )
            ).scalar_one_or_none()

            if not src:
                src = Source(
                    episode_id=ep.id,
                    source_name=raw_src.source_name,
                    source_key=raw_src.source_key,
                    raw_player_data=raw_src.raw_player_data,
                    source_type=raw_src.source_type,
                )
                db.add(src)
            else:
                if raw_src.raw_player_data:
                    src.raw_player_data = raw_src.raw_player_data
                if raw_src.source_type:
                    src.source_type = raw_src.source_type

    # Update episode count on website_show
    ws.episode_count_on_site = len(detail.episodes) if detail.episodes else ws.episode_count_on_site

    db.commit()
    logger.info(f"Synced show '{detail.show.title}' with {len(detail.episodes)} episodes")
