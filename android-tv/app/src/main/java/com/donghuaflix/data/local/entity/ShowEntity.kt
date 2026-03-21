package com.donghuaflix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shows")
data class ShowEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val titleChinese: String? = null,
    val slug: String? = null,
    val posterUrl: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val status: String? = null,
    val genres: String? = null, // comma-separated
    val totalEpisodes: Int? = null,
    val category: String? = null,
    val websitesJson: String? = null, // JSON array of WebsiteInfo
    val remoteUpdatedAt: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)
