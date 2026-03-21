import re


def normalize_title(title: str) -> str:
    """Normalize a show title for deduplication across websites.

    Strips special characters, lowercases, removes common suffixes
    like "season 1", "s1", "part 2", etc.
    """
    title = title.lower().strip()
    # Remove special characters
    title = re.sub(r'[^a-z0-9\s]', '', title)
    # Collapse whitespace
    title = re.sub(r'\s+', ' ', title)
    # Remove common season/part suffixes
    title = re.sub(r'\s*(season|s)\s*\d+', '', title)
    title = re.sub(r'\s*(part|p)\s*\d+', '', title)
    # Remove trailing "episode X" or "ep X"
    title = re.sub(r'\s*(episode|ep)\s*\d+', '', title)
    return title.strip()
