package com.donghuaflix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.donghuaflix.data.local.dao.ShowDao
import com.donghuaflix.data.local.dao.SyncMetadataDao
import com.donghuaflix.data.local.dao.WatchHistoryDao
import com.donghuaflix.data.local.dao.WatchlistDao
import com.donghuaflix.data.local.entity.ShowEntity
import com.donghuaflix.data.local.entity.SyncMetadataEntity
import com.donghuaflix.data.local.entity.WatchHistoryEntity
import com.donghuaflix.data.local.entity.WatchlistEntity

@Database(
    entities = [
        ShowEntity::class,
        WatchHistoryEntity::class,
        WatchlistEntity::class,
        SyncMetadataEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
