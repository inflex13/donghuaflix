import asyncio
import logging
from datetime import datetime

from celery import Celery
from celery.schedules import crontab
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.config import settings

logger = logging.getLogger(__name__)

celery_app = Celery("donghuaflix", broker=settings.REDIS_URL, backend=settings.REDIS_URL)

celery_app.conf.beat_schedule = {
    "scrape-all-sites": {
        "task": "app.scraper.tasks.scrape_all_sites",
        "schedule": crontab(minute=f"*/{settings.SCRAPE_INTERVAL_MINUTES}"),
    },
}
celery_app.conf.timezone = "UTC"


# Website configurations
WEBSITE_CONFIGS = [
    {
        "name": "donghuafun",
        "display_name": "DonghuaFun",
        "base_url": "https://donghuafun.com",
        "scraper_type": "maccms",
    },
    {
        "name": "animekhor",
        "display_name": "AnimeKhor",
        "base_url": "https://animekhor.org",
        "scraper_type": "wordpress",
    },
]


def _get_sync_session() -> Session:
    """Get a synchronous SQLAlchemy session for Celery tasks."""
    engine = create_engine(settings.SYNC_DATABASE_URL)
    return Session(engine)


def _ensure_websites(db: Session):
    """Ensure all website entries exist in the database."""
    from app.models import Website

    for config in WEBSITE_CONFIGS:
        existing = db.execute(
            select(Website).where(Website.name == config["name"])
        ).scalar_one_or_none()

        if not existing:
            website = Website(
                name=config["name"],
                display_name=config["display_name"],
                base_url=config["base_url"],
                scraper_type=config["scraper_type"],
            )
            db.add(website)

    db.commit()


@celery_app.task(name="app.scraper.tasks.scrape_all_sites")
def scrape_all_sites():
    """Scrape all enabled websites (incremental - page 1 only)."""
    logger.info("Starting incremental scrape of all sites")
    db = _get_sync_session()

    try:
        _ensure_websites(db)

        from app.models import Website
        websites = db.execute(
            select(Website).where(Website.enabled == True)  # noqa: E712
        ).scalars().all()

        for website in websites:
            try:
                scrape_site.delay(website.name)
            except Exception as e:
                logger.error(f"Error queueing scrape for {website.name}: {e}")
    finally:
        db.close()


@celery_app.task(name="app.scraper.tasks.scrape_site")
def scrape_site(website_name: str, full: bool = False):
    """Scrape a specific website."""
    logger.info(f"Scraping {website_name} (full={full})")
    db = _get_sync_session()

    try:
        _ensure_websites(db)

        from app.models import Website
        website = db.execute(
            select(Website).where(Website.name == website_name)
        ).scalar_one_or_none()

        if not website:
            logger.error(f"Website {website_name} not found")
            return

        from app.scraper.registry import get_scraper
        scraper = get_scraper(website_name, website.base_url)

        # Run async scraper in sync context
        loop = asyncio.new_event_loop()
        try:
            raw_shows = loop.run_until_complete(scraper.scrape_catalog(full=full))
        finally:
            loop.close()

        logger.info(f"Found {len(raw_shows)} shows from {website_name}")

        from app.services.sync_service import sync_show_to_db
        scraped_external_ids = []
        for raw_show in raw_shows:
            try:
                sync_show_to_db(db, raw_show, website)
                db.commit()
                scraped_external_ids.append(raw_show.external_id)
            except Exception as e:
                db.rollback()
                logger.error(f"Error syncing show '{raw_show.title}': {e}")

        # Scrape details only for shows from this scrape run that need it
        from app.models import WebsiteShow, Episode
        for ext_id in scraped_external_ids:
            ws = db.execute(
                select(WebsiteShow).where(
                    WebsiteShow.website_id == website.id,
                    WebsiteShow.external_id == ext_id,
                )
            ).scalar_one_or_none()
            if not ws:
                continue

            ep_count = db.execute(
                select(Episode).where(Episode.website_show_id == ws.id)
            ).scalars().all()

            if not ep_count or full:
                scrape_show_detail.delay(website_name, ws.external_id)

        # Update last scraped timestamp
        website.last_scraped_at = datetime.utcnow()
        db.commit()

    except Exception as e:
        logger.error(f"Error scraping {website_name}: {e}")
        db.rollback()
    finally:
        db.close()


@celery_app.task(name="app.scraper.tasks.scrape_show_detail")
def scrape_show_detail(website_name: str, external_id: str):
    """Scrape full detail for a specific show."""
    logger.info(f"Scraping show detail: {website_name}/{external_id}")
    db = _get_sync_session()

    try:
        _ensure_websites(db)

        from app.models import Website
        website = db.execute(
            select(Website).where(Website.name == website_name)
        ).scalar_one_or_none()

        if not website:
            return

        from app.scraper.registry import get_scraper
        scraper = get_scraper(website_name, website.base_url)

        loop = asyncio.new_event_loop()
        try:
            detail = loop.run_until_complete(scraper.scrape_show_detail(external_id))
        finally:
            loop.close()

        if detail:
            from app.services.sync_service import sync_detail_to_db
            sync_detail_to_db(db, detail, website)
        else:
            logger.warning(f"No detail found for {website_name}/{external_id}")

    except Exception as e:
        logger.error(f"Error scraping show detail {website_name}/{external_id}: {e}")
        db.rollback()
    finally:
        db.close()


@celery_app.task(name="app.scraper.tasks.initial_full_scrape")
def initial_full_scrape():
    """Run a full scrape of all sites (first time only)."""
    logger.info("Running initial full scrape of all sites")
    for config in WEBSITE_CONFIGS:
        scrape_site.delay(config["name"], full=True)


@celery_app.task(name="app.scraper.tasks.cache_all_images")
def cache_all_images():
    """Download and cache all poster images to local storage."""
    logger.info("Caching all poster images")
    db = _get_sync_session()
    try:
        from app.scraper.image_cacher import cache_all_posters
        cached, failed = cache_all_posters(db)
        logger.info(f"Image caching complete: {cached} cached, {failed} failed")
    except Exception as e:
        logger.error(f"Error caching images: {e}")
        db.rollback()
    finally:
        db.close()
