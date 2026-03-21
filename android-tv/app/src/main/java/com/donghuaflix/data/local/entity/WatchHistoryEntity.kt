package com.donghuaflix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    indices = [Index("showId")],
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val showId: Int,
    val episodeNumber: Int,
    val episodeId: Int? = null,
    val progressSeconds: Int = 0,
    val durationSeconds: Int? = null,
    val completed: Boolean = false,
    val watchedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
