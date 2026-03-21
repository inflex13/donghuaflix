import re


def normalize_title(title: str) -> str:
    """Normalize a show title for deduplication across websites.

    Strips special characters, lowercases, removes common suffixes
    like "season 1", "s1", "part 2", etc.
    Preserves meaningful suffixes like "3d", "movie", "ova".
    """
    title = title.lower().strip()
    # Remove special characters but keep alphanumeric + spaces
    title = re.sub(r'[^a-z0-9\s]', '', title)
    # Collapse whitespace
    title = re.sub(r'\s+', ' ', title)
    # Remove "season X" (full word only, not "s" inside other words)
    title = re.sub(r'\bseason\s*\d+\b', '', title)
    # Remove standalone "s1", "s2" etc (must be preceded by space or start)
    title = re.sub(r'\bs\d+\b', '', title)
    # Remove "part X"
    title = re.sub(r'\bpart\s*\d+\b', '', title)
    # Remove trailing "episode X" or "ep X"
    title = re.sub(r'\b(?:episode|ep)\s*\d+\b', '', title)
    # Collapse whitespace again
    title = re.sub(r'\s+', ' ', title)
    return title.strip()
