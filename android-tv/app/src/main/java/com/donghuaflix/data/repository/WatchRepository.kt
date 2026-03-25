package com.donghuaflix.data.repository

import com.donghuaflix.data.local.dao.WatchHistoryDao
import com.donghuaflix.data.local.dao.WatchlistDao
import com.donghuaflix.data.local.entity.WatchHistoryEntity
import com.donghuaflix.data.local.entity.WatchlistEntity
import com.donghuaflix.data.remote.DonghuaApi
import com.donghuaflix.data.remote.dto.WatchProgressDto
import com.donghuaflix.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchRepository @Inject constructor(
    private val api: DonghuaApi,
    private val watchHistoryDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao,
) {
    fun observeContinueWatching(): Flow<List<WatchProgress>> =
        watchHistoryDao.getContinueWatching().map { entries ->
            entries.map { WatchProgress(it.showId, it.episodeNumber, it.progressSeconds, it.durationSeconds, it.completed) }
        }

    fun observeWatchHistory(): Flow<List<WatchProgress>> =
        watchHistoryDao.getWatchHistory().map { entries ->
            entries.map { WatchProgress(it.showId, it.episodeNumber, it.progressSeconds, it.durationSeconds, it.completed) }
        }

    suspend fun updateProgress(
        showId: Int,
        episodeNumber: Int,
        progressSeconds: Int,
        durationSeconds: Int?,
        episodeId: Int? = null,
    ) {
        val completed = durationSeconds != null && progressSeconds >= durationSeconds * 0.9

        val entity = WatchHistoryEntity(
            showId = showId,
            episodeNumber = episodeNumber,
            episodeId = episodeId,
            progressSeconds = progressSeconds,
            durationSeconds = durationSeconds,
            completed = completed,
            watchedAt = System.currentTimeMillis(),
            isSynced = false,
        )

        // Check if existing entry
        val existing = watchHistoryDao.getProgressForEpisode(showId, episodeNumber)
        if (existing != null) {
            watchHistoryDao.upsert(entity.copy(id = existing.id))
        } else {
            watchHistoryDao.upsert(entity)
        }

        // Sync to backend (fire-and-forget)
        try {
            api.updateProgress(WatchProgressDto(
                showId = showId,
                episodeNumber = episodeNumber,
                progressSeconds = progressSeconds,
                durationSeconds = durationSeconds,
                episodeId = episodeId,
            ))
        } catch (_: Exception) {
            // Will sync later
        }
    }

    suspend fun markAsWatched(showId: Int, episodeNumber: Int) {
        val existing = watchHistoryDao.getProgressForEpisode(showId, episodeNumber)
        val progress = existing?.durationSeconds ?: 1
        val duration = existing?.durationSeconds ?: 1
        val entity = WatchHistoryEntity(
            id = existing?.id ?: 0,
            showId = showId,
            episodeNumber = episodeNumber,
            progressSeconds = progress,
            durationSeconds = duration,
            completed = true,
            watchedAt = System.currentTimeMillis(),
            isSynced = false,
        )
        watchHistoryDao.upsert(entity)

        // Push to server immediately
        try {
            api.updateProgress(WatchProgressDto(
                showId = showId,
                episodeNumber = episodeNumber,
                progressSeconds = progress,
                durationSeconds = duration,
                completed = true,
            ))
            watchHistoryDao.upsert(entity.copy(isSynced = true))
        } catch (_: Exception) {
            // Will sync later
        }
    }

    suspend fun markAsUnwatched(showId: Int, episodeNumber: Int) {
        val existing = watchHistoryDao.getProgressForEpisode(showId, episodeNumber)
        if (existing != null) {
            watchHistoryDao.upsert(existing.copy(completed = false, progressSeconds = 0, isSynced = false))
            // Push to server immediately
            try {
                api.updateProgress(WatchProgressDto(
                    showId = showId,
                    episodeNumber = episodeNumber,
                    progressSeconds = 0,
                    durationSeconds = existing.durationSeconds,
                    completed = false,
                ))
                watchHistoryDao.upsert(existing.copy(completed = false, progressSeconds = 0, isSynced = true))
            } catch (_: Exception) {}
        }
    }

    suspend fun getProgressForEpisode(showId: Int, episodeNumber: Int): WatchProgress? {
        val entity = watchHistoryDao.getProgressForEpisode(showId, episodeNumber) ?: return null
        return WatchProgress(entity.showId, entity.episodeNumber, entity.progressSeconds, entity.durationSeconds, entity.completed)
    }

    suspend fun getWatchedEpisodesForShow(showId: Int): Map<Int, WatchProgress> {
        val entries = watchHistoryDao.getHistoryForShow(showId)
        return entries
            .filter { it.progressSeconds > 0 && it.durationSeconds != null && it.durationSeconds > 0 && it.durationSeconds < 86400 }
            .associate { it.episodeNumber to WatchProgress(it.showId, it.episodeNumber, it.progressSeconds, it.durationSeconds, it.completed) }
    }

    suspend fun getLastWatchedForShow(showId: Int): WatchProgress? {
        val entity = watchHistoryDao.getLastWatchedForShow(showId) ?: return null
        return WatchProgress(entity.showId, entity.episodeNumber, entity.progressSeconds, entity.durationSeconds, entity.completed)
    }

    // Watchlist
    fun observeWatchlist(): Flow<List<Int>> =
        watchlistDao.getWatchlist().map { it.map { e -> e.showId } }

    suspend fun isInWatchlist(showId: Int): Boolean =
        watchlistDao.isInWatchlist(showId) != null

    suspend fun toggleWatchlist(showId: Int) {
        if (isInWatchlist(showId)) {
            watchlistDao.remove(showId)
            try { api.removeFromWatchlist(showId) } catch (_: Exception) {}
        } else {
            watchlistDao.add(WatchlistEntity(showId = showId))
            try { api.addToWatchlist(showId) } catch (_: Exception) {}
        }
    }
}
