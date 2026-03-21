from app.scraper.base import BaseScraper


# Registry of all available scrapers
_SCRAPERS: dict[str, type[BaseScraper]] = {}
_loaded = False


def register_scraper(name: str):
    """Decorator to register a scraper class."""
    def decorator(cls):
        _SCRAPERS[name] = cls
        return cls
    return decorator


def _load_scrapers():
    """Lazy import scrapers to trigger registration."""
    global _loaded
    if _loaded:
        return
    _loaded = True
    from app.scraper.donghuafun import DonghuaFunScraper  # noqa: F401
    from app.scraper.animekhor import AnimeKhorScraper  # noqa: F401


def get_scraper(name: str, base_url: str) -> BaseScraper:
    """Get an instance of a scraper by name."""
    _load_scrapers()
    if name not in _SCRAPERS:
        raise ValueError(f"Unknown scraper: {name}. Available: {list(_SCRAPERS.keys())}")
    return _SCRAPERS[name](website_name=name, base_url=base_url)


def get_all_scraper_names() -> list[str]:
    """Get all registered scraper names."""
    _load_scrapers()
    return list(_SCRAPERS.keys())
