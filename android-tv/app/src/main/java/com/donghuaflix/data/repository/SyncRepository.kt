package com.donghuaflix.data.repository

import com.donghuaflix.data.local.dao.ShowDao
import com.donghuaflix.data.local.dao.SyncMetadataDao
import com.donghuaflix.data.local.dao.WatchHistoryDao
import com.donghuaflix.data.local.dao.WatchlistDao
import com.donghuaflix.data.local.entity.SyncMetadataEntity
import com.donghuaflix.data.local.entity.WatchHistoryEntity
import com.donghuaflix.data.local.entity.WatchlistEntity
import com.donghuaflix.data.mapper.ShowMapper
import com.donghuaflix.data.remote.DonghuaApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val api: DonghuaApi,
    private val showDao: ShowDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao,
    private val syncMetadataDao: SyncMetadataDao,
) {
    suspend fun fullResync(): Result<Unit> = runCatching {
        // Wipe local show cache but keep watch history
        showDao.deleteAll()
        syncMetadataDao.deleteAll()
        // Now do a full sync
        syncAll().getOrThrow()
    }

    suspend fun syncAll(): Result<Unit> = runCatching {
        val lastSynced = syncMetadataDao.getValue("last_synced_at")

        // First sync: don't pass 'since' so we get everything
        val response = api.sync(since = if (lastSynced == null) null else lastSynced)

        // Upsert shows
        if (response.shows.isNotEmpty()) {
            val entities = response.shows.map { ShowMapper.dtoToEntity(it) }
            showDao.upsertShows(entities)
        }

        // Merge watch history (server wins for newer entries)
        for (wp in response.watchHistory) {
            val existing = watchHistoryDao.getProgressForEpisode(wp.showId, wp.episodeNumber)
            if (existing == null || !existing.isSynced) {
                watchHistoryDao.upsert(
                    WatchHistoryEntity(
                        id = existing?.id ?: 0,
                        showId = wp.showId,
                        episodeNumber = wp.episodeNumber,
                        episodeId = wp.episodeId,
                        progressSeconds = wp.progressSeconds,
                        durationSeconds = wp.durationSeconds,
                        completed = wp.completed,
                        isSynced = true,
                    )
                )
            }
        }

        // Sync watchlist
        for (showId in response.watchlist) {
            if (watchlistDao.isInWatchlist(showId) == null) {
                watchlistDao.add(WatchlistEntity(showId = showId, isSynced = true))
            }
        }

        // Upload un-synced local watch history
        val unsynced = watchHistoryDao.getUnsyncedEntries()
        for (entry in unsynced) {
            try {
                api.updateProgress(
                    com.donghuaflix.data.remote.dto.WatchProgressDto(
                        showId = entry.showId,
                        episodeNumber = entry.episodeNumber,
                        progressSeconds = entry.progressSeconds,
                        durationSeconds = entry.durationSeconds,
                        episodeId = entry.episodeId,
                    )
                )
            } catch (_: Exception) {}
        }
        if (unsynced.isNotEmpty()) {
            watchHistoryDao.markSynced(unsynced.map { it.id })
        }

        // Update last sync timestamp
        syncMetadataDao.setValue(
            SyncMetadataEntity(key = "last_synced_at", value = response.timestamp)
        )
    }
}
