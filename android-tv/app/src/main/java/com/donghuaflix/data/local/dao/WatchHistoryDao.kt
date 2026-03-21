package com.donghuaflix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.donghuaflix.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Query("""
        SELECT * FROM watch_history
        WHERE completed = 0 AND progressSeconds > 0 AND durationSeconds > 0 AND durationSeconds < 86400
        ORDER BY watchedAt DESC
        LIMIT :limit
    """)
    fun getContinueWatching(limit: Int = 20): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    fun getWatchHistory(limit: Int = 50): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE showId = :showId ORDER BY episodeNumber DESC")
    suspend fun getHistoryForShow(showId: Int): List<WatchHistoryEntity>

    @Query("""
        SELECT * FROM watch_history
        WHERE showId = :showId AND episodeNumber = :episodeNumber
    """)
    suspend fun getProgressForEpisode(showId: Int, episodeNumber: Int): WatchHistoryEntity?

    @Query("""
        SELECT * FROM watch_history
        WHERE showId = :showId AND completed = 0 AND progressSeconds > 0 AND durationSeconds > 0 AND durationSeconds < 86400
        ORDER BY watchedAt DESC
        LIMIT 1
    """)
    suspend fun getLastWatchedForShow(showId: Int): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<WatchHistoryEntity>

    @Query("UPDATE watch_history SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Int>)

    @Query("DELETE FROM watch_history")
    suspend fun deleteAll()
}
