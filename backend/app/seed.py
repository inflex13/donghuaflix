"""Seed the database with initial website entries and run first scrape."""
import asyncio
import sys

from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.config import settings
from app.database import Base
from app.models import Website


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


def seed_websites():
    """Create website entries in the database."""
    engine = create_engine(settings.SYNC_DATABASE_URL)
    Base.metadata.create_all(engine)

    with Session(engine) as db:
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
                print(f"Created website: {config['display_name']}")
            else:
                print(f"Website already exists: {config['display_name']}")

        db.commit()
    print("Seeding complete!")


if __name__ == "__main__":
    seed_websites()
