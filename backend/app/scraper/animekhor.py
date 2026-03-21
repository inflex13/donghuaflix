import asyncio
import logging
import re

import httpx
from bs4 import BeautifulSoup

from app.config import settings
from app.scraper.base import BaseScraper, RawEpisode, RawShow, RawSource, ShowDetail
from app.scraper.registry import register_scraper

logger = logging.getLogger(__name__)


@register_scraper("animekhor")
class AnimeKhorScraper(BaseScraper):
    """Scraper for animekhor.org (WordPress-based)."""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.headers = {
            "User-Agent": settings.SCRAPE_USER_AGENT,
            "Referer": self.base_url,
        }

    async def scrape_catalog(self, full: bool = False) -> list[RawShow]:
        """Scrape show catalog from animekhor.org."""
        shows = []
        max_pages = 50 if full else 1

        async with httpx.AsyncClient(timeout=30, headers=self.headers, follow_redirects=True) as client:
            for page in range(1, max_pages + 1):
                try:
                    url = f"{self.base_url}/donghua-series/"
                    if page > 1:
                        url = f"{self.base_url}/donghua-series/page/{page}/"

                    resp = await client.get(url)
                    if resp.status_code != 200:
                        break

                    soup = BeautifulSoup(resp.text, "lxml")
                    page_shows = self._parse_catalog_page(soup)

                    if not page_shows:
                        break

                    shows.extend(page_shows)
                    logger.info(f"Scraped page {page}: {len(page_shows)} shows")

                except Exception as e:
                    logger.error(f"Error scraping catalog page {page}: {e}")
                    break

                await asyncio.sleep(settings.SCRAPE_REQUEST_DELAY)

        logger.info(f"Scraped {len(shows)} shows from animekhor.org")
        return shows

    def _parse_catalog_page(self, soup: BeautifulSoup) -> list[RawShow]:
        """Parse shows from a catalog listing page."""
        shows = []

        # AnimeKhor uses article cards with anime links
        for article in soup.select(".bs .bsx, .listupd .bs"):
            try:
                link = article.select_one("a")
                if not link:
                    continue

                href = link.get("href", "")
                if "/anime/" not in href:
                    continue

                # Extract slug from URL
                slug_match = re.search(r"/anime/([^/]+)", href)
                if not slug_match:
                    continue
                slug = slug_match.group(1).rstrip("/")

                title_tag = article.select_one("h2")
                title = title_tag.get_text(strip=True) if title_tag else ""
                if not title:
                    continue

                # Poster
                img = article.select_one("img")
                poster_url = None
                if img:
                    poster_url = img.get("data-src") or img.get("src") or ""

                # Status/type
                status_tag = article.select_one(".status, .type")
                status_text = status_tag.get_text(strip=True).lower() if status_tag else ""
                status = "completed" if "completed" in status_text else "ongoing"

                # Rating
                rating = None
                rating_tag = article.select_one(".rating .numscore, .score")
                if rating_tag:
                    try:
                        rating = float(rating_tag.get_text(strip=True))
                    except ValueError:
                        pass

                # Episode count
                ep_tag = article.select_one(".epx, .episode")
                total_episodes = None
                if ep_tag:
                    ep_match = re.search(r"(\d+)", ep_tag.get_text())
                    if ep_match:
                        total_episodes = int(ep_match.group(1))

                shows.append(RawShow(
                    external_id=slug,
                    title=title,
                    poster_url=poster_url,
                    status=status,
                    rating=rating,
                    total_episodes=total_episodes,
                    category="donghua",
                    external_url=href,
                ))

            except Exception as e:
                logger.error(f"Error parsing show card: {e}")
                continue

        return shows

    async def scrape_show_detail(self, external_id: str) -> ShowDetail | None:
        """Scrape full show detail from animekhor.org."""
        async with httpx.AsyncClient(timeout=30, headers=self.headers, follow_redirects=True) as client:
            try:
                url = f"{self.base_url}/anime/{external_id}/"
                resp = await client.get(url)
                if resp.status_code != 200:
                    return None

                soup = BeautifulSoup(resp.text, "lxml")

                # Title
                title_tag = soup.select_one("h1.entry-title, .infox h1")
                title = title_tag.get_text(strip=True) if title_tag else external_id

                # Description
                desc_tag = soup.select_one(".synp .entry-content, .desc, [itemprop='description']")
                description = desc_tag.get_text(strip=True) if desc_tag else None

                # Poster
                poster_tag = soup.select_one(".thumb img, .bigcoverimg img")
                poster_url = None
                if poster_tag:
                    poster_url = poster_tag.get("data-src") or poster_tag.get("src")

                # Genres
                genres = []
                for genre_tag in soup.select(".genxed a, .genre-info a"):
                    genres.append(genre_tag.get_text(strip=True))

                # Status
                status = "ongoing"
                for info in soup.select(".spe span, .info-content span"):
                    text = info.get_text(strip=True).lower()
                    if "status" in text and "completed" in text:
                        status = "completed"
                        break

                # Rating
                rating = None
                rating_tag = soup.select_one("[itemprop='ratingValue'], .rating .num")
                if rating_tag:
                    try:
                        rating = float(rating_tag.get_text(strip=True))
                    except ValueError:
                        pass

                # Year
                year = None
                for info in soup.select(".spe span, .info-content span"):
                    text = info.get_text(strip=True)
                    year_match = re.search(r"(\d{4})", text)
                    if year_match:
                        year = int(year_match.group(1))
                        break

                # Chinese title
                title_chinese = None
                alt_tag = soup.select_one(".alter, .alternative")
                if alt_tag:
                    title_chinese = alt_tag.get_text(strip=True)

                show = RawShow(
                    external_id=external_id,
                    title=title,
                    title_chinese=title_chinese,
                    poster_url=poster_url,
                    description=description,
                    rating=rating,
                    year=year,
                    status=status,
                    genres=genres,
                    category="donghua",
                    external_url=f"{self.base_url}/anime/{external_id}/",
                )

                # Episodes
                episodes = self._parse_episodes(soup)

                # Fetch sources using a single Playwright browser session
                try:
                    from playwright.async_api import async_playwright
                    import base64

                    async with async_playwright() as p:
                        browser = await p.chromium.launch(headless=True, args=["--no-sandbox"])
                        page = await browser.new_page()

                        for ep in episodes:
                            if not ep.external_url:
                                continue
                            try:
                                await page.goto(ep.external_url, wait_until="networkidle", timeout=20000)
                                await page.wait_for_timeout(1500)

                                options = await page.query_selector_all("select option")
                                for opt in options:
                                    name = (await opt.text_content() or "").strip()
                                    value = await opt.get_attribute("value") or ""
                                    if not value or "Select" in name:
                                        continue
                                    name_lower = name.lower()
                                    if "ok.ru" not in name_lower and "rumble" not in name_lower:
                                        continue
                                    try:
                                        iframe_html = base64.b64decode(value).decode("utf-8")
                                        src_match = re.search(r'src="([^"]+)"', iframe_html)
                                        if not src_match:
                                            continue
                                        embed_url = src_match.group(1)
                                        if embed_url.startswith("//"):
                                            embed_url = "https:" + embed_url
                                        source_type = "okru" if "ok.ru" in embed_url else "rumble"
                                        ep.sources.append(RawSource(
                                            source_name=name,
                                            source_key=source_type,
                                            raw_player_data=embed_url,
                                            source_type=source_type,
                                        ))
                                    except Exception:
                                        pass

                                if ep.sources:
                                    logger.info(f"EP{ep.episode_number}: {len(ep.sources)} sources")
                            except Exception as e:
                                logger.error(f"Failed EP{ep.episode_number}: {e}")

                        await browser.close()
                except Exception as e:
                    logger.error(f"Playwright source extraction failed: {e}")

                return ShowDetail(show=show, episodes=episodes)

            except Exception as e:
                logger.error(f"Error scraping show detail {external_id}: {e}")
                return None

    def _parse_episodes(self, soup: BeautifulSoup) -> list[RawEpisode]:
        """Parse episode list from show detail page."""
        episodes = []

        for ep_item in soup.select(".eplister ul li, .episodelist ul li"):
            try:
                link = ep_item.select_one("a")
                if not link:
                    continue

                href = link.get("href", "")
                ep_text = link.get_text(strip=True)

                # Parse episode number
                ep_num_match = re.search(r"(?:episode|ep)\s*(\d+)", ep_text, re.IGNORECASE)
                if not ep_num_match:
                    # Try from URL
                    ep_num_match = re.search(r"episode-(\d+)", href)
                if not ep_num_match:
                    continue

                ep_num = int(ep_num_match.group(1))

                episodes.append(RawEpisode(
                    episode_number=ep_num,
                    title=ep_text,
                    external_url=href,
                    sources=[],
                ))

            except Exception as e:
                logger.error(f"Error parsing episode: {e}")
                continue

        return sorted(episodes, key=lambda e: e.episode_number)

    async def scrape_episode_sources(
        self, client: httpx.AsyncClient, episode_url: str
    ) -> list[RawSource]:
        """Use Playwright to extract video server sources from episode page dropdown."""
        import base64

        sources = []
        try:
            from playwright.async_api import async_playwright

            async with async_playwright() as p:
                browser = await p.chromium.launch(headless=True, args=["--no-sandbox"])
                page = await browser.new_page()
                await page.goto(episode_url, wait_until="networkidle", timeout=30000)
                await page.wait_for_timeout(2000)

                options = await page.query_selector_all("select option")
                for opt in options:
                    name = (await opt.text_content() or "").strip()
                    value = await opt.get_attribute("value") or ""
                    if not value or "Select" in name:
                        continue

                    # Only keep ok.ru and RumblePlayer
                    name_lower = name.lower()
                    if "ok.ru" not in name_lower and "rumble" not in name_lower:
                        continue

                    try:
                        iframe_html = base64.b64decode(value).decode("utf-8")
                        src_match = re.search(r'src="([^"]+)"', iframe_html)
                        if not src_match:
                            continue

                        embed_url = src_match.group(1)
                        if embed_url.startswith("//"):
                            embed_url = "https:" + embed_url

                        source_type = "okru" if "ok.ru" in embed_url else "rumble"

                        sources.append(RawSource(
                            source_name=name,
                            source_key=source_type,
                            raw_player_data=embed_url,
                            source_type=source_type,
                        ))
                        logger.info(f"Found {source_type} source: {embed_url[:60]}...")

                    except Exception as e:
                        logger.error(f"Error decoding server option: {e}")

                await browser.close()

        except Exception as e:
            logger.error(f"Error scraping episode sources from {episode_url}: {e}")

        return sources

    async def extract_video_url(self, raw_player_data: str) -> dict | None:
        """Extract video URL from ok.ru or Rumble embed."""
        if "ok.ru" in raw_player_data:
            return await self._resolve_okru(raw_player_data)
        if "rumble.com" in raw_player_data:
            return await self._resolve_rumble(raw_player_data)
        from app.services.source_resolver import _resolve_embed
        return await _resolve_embed(raw_player_data)

    async def _resolve_okru(self, embed_url: str) -> dict | None:
        """Resolve ok.ru embed to direct video URL."""
        try:
            import httpx as hx
            async with hx.AsyncClient(timeout=15, follow_redirects=True) as client:
                resp = await client.get(embed_url, headers={"User-Agent": "Mozilla/5.0"})
                if resp.status_code == 200:
                    # ok.ru embeds contain video URLs in data attributes or JSON
                    match = re.search(r'"hlsManifestUrl":"([^"]+)"', resp.text)
                    if match:
                        hls_url = match.group(1).replace("\\\\u0026", "&").replace("\\/", "/")
                        return {"url": hls_url, "type": "hls"}
                    # Try finding direct MP4
                    match = re.search(r'"videoUrl":"([^"]+)"', resp.text)
                    if match:
                        return {"url": match.group(1).replace("\\/", "/"), "type": "mp4"}
        except Exception as e:
            logger.error(f"ok.ru resolve failed: {e}")
        return None

    async def _resolve_rumble(self, embed_url: str) -> dict | None:
        """Resolve Rumble embed to direct video URL."""
        try:
            import httpx as hx
            async with hx.AsyncClient(timeout=15, follow_redirects=True) as client:
                resp = await client.get(embed_url, headers={"User-Agent": "Mozilla/5.0"})
                if resp.status_code == 200:
                    # Rumble embeds have mp4 URLs in JSON
                    match = re.search(r'"mp4":.*?"url":"([^"]+)"', resp.text)
                    if match:
                        return {"url": match.group(1), "type": "mp4"}
                    # Try HLS
                    match = re.search(r'"hls":.*?"url":"([^"]+)"', resp.text)
                    if match:
                        return {"url": match.group(1), "type": "hls"}
        except Exception as e:
            logger.error(f"Rumble resolve failed: {e}")
        return None
