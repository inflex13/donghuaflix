package com.donghuaflix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.donghuaflix.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE showId = :showId")
    suspend fun isInWatchlist(showId: Int): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE showId = :showId")
    suspend fun remove(showId: Int)

    @Query("SELECT * FROM watchlist WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<WatchlistEntity>

    @Query("UPDATE watchlist SET isSynced = 1 WHERE showId IN (:showIds)")
    suspend fun markSynced(showIds: List<Int>)

    @Query("DELETE FROM watchlist")
    suspend fun deleteAll()
}
