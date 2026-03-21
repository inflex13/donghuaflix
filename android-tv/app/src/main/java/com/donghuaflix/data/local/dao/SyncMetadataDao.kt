package com.donghuaflix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.donghuaflix.data.local.entity.SyncMetadataEntity

@Dao
interface SyncMetadataDao {

    @Query("SELECT value FROM sync_metadata WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(entity: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll()
}
