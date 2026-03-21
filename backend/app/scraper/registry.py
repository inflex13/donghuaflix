from app.scraper.base import BaseScraper


# Registry of all available scrapers
_SCRAPERS: dict[str, type[BaseScraper]] = {}


def register_scraper(name: str):
    """Decorator to register a scraper class."""
    def decorator(cls):
        _SCRAPERS[name] = cls
        return cls
    return decorator


def get_scraper(name: str, base_url: str) -> BaseScraper:
    """Get an instance of a scraper by name."""
    if name not in _SCRAPERS:
        raise ValueError(f"Unknown scraper: {name}. Available: {list(_SCRAPERS.keys())}")
    return _SCRAPERS[name](website_name=name, base_url=base_url)


def get_all_scraper_names() -> list[str]:
    """Get all registered scraper names."""
    return list(_SCRAPERS.keys())


# Import scrapers to trigger registration
from app.scraper.donghuafun import DonghuaFunScraper  # noqa: F401, E402
from app.scraper.animekhor import AnimeKhorScraper  # noqa: F401, E402
