package com.donghuaflix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val showId: Int,
    val addedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
