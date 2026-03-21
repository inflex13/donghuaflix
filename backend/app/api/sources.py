from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models import Source

router = APIRouter()


@router.post("/{source_id}/resolve")
async def resolve_source(source_id: int, db: AsyncSession = Depends(get_db)):
    query = select(Source).where(Source.id == source_id)
    result = await db.execute(query)
    source = result.scalar_one_or_none()
    if not source:
        raise HTTPException(status_code=404, detail="Source not found")

    # If URL is cached and not expired, return it
    if source.source_url:
        from datetime import datetime
        if not source.url_expires_at or source.url_expires_at > datetime.utcnow():
            return {
                "id": source.id,
                "source_name": source.source_name,
                "source_key": source.source_key,
                "source_url": source.source_url,
                "source_type": source.source_type,
                "subtitles": {},
            }

    # Resolve via source resolver
    from app.services.source_resolver import resolve_video_url
    resolved = await resolve_video_url(source)
    if not resolved:
        raise HTTPException(status_code=502, detail="Could not resolve video URL")

    source.source_url = resolved["url"]
    source.source_type = resolved["type"]
    if resolved.get("expires_at"):
        source.url_expires_at = resolved["expires_at"]
    await db.commit()
    await db.refresh(source)

    return {
        "id": source.id,
        "source_name": source.source_name,
        "source_key": source.source_key,
        "source_url": source.source_url,
        "source_type": source.source_type,
        "subtitles": resolved.get("subtitles", {}),
    }
