package com.donghuaflix.data.mapper

import com.donghuaflix.data.local.entity.ShowEntity
import com.donghuaflix.data.local.entity.WatchHistoryEntity
import com.donghuaflix.data.remote.dto.ShowDto
import com.donghuaflix.data.remote.dto.WebsiteInfoDto
import com.donghuaflix.domain.model.Show
import com.donghuaflix.domain.model.WatchProgress
import com.donghuaflix.domain.model.WebsiteInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ShowMapper {

    private fun parseTimestamp(ts: String?): Long {
        if (ts == null) return 0
        // Strip microseconds if present (e.g., .571065)
        val clean = ts.replace(Regex("\\.\\d+$"), "")
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(clean)?.time ?: 0
        } catch (_: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(clean)?.time ?: 0
            } catch (_: Exception) { 0 }
        }
    }

    private val gson = Gson()

    fun dtoToEntity(dto: ShowDto): ShowEntity = ShowEntity(
        id = dto.id,
        title = dto.title,
        titleChinese = dto.titleChinese,
        slug = dto.slug,
        posterUrl = dto.posterUrl,
        description = dto.description,
        rating = dto.rating,
        year = dto.year,
        status = dto.status,
        genres = dto.genres?.joinToString(","),
        totalEpisodes = dto.totalEpisodes,
        category = dto.category,
        websitesJson = dto.websites?.let { gson.toJson(it) },
        remoteUpdatedAt = parseTimestamp(dto.remoteUpdatedAt),
        updatedAt = System.currentTimeMillis(),
    )

    fun entityToDomain(entity: ShowEntity, watchProgress: WatchProgress? = null): Show {
        val websites = entity.websitesJson?.let {
            try {
                val type = object : TypeToken<List<WebsiteInfoDto>>() {}.type
                val dtos: List<WebsiteInfoDto> = gson.fromJson(it, type)
                dtos.map { dto -> WebsiteInfo(dto.id, dto.name, dto.displayName, dto.episodeCount) }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return Show(
            id = entity.id,
            title = entity.title,
            titleChinese = entity.titleChinese,
            slug = entity.slug,
            posterUrl = entity.posterUrl,
            description = entity.description,
            rating = entity.rating,
            year = entity.year,
            status = entity.status,
            genres = entity.genres?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            totalEpisodes = entity.totalEpisodes,
            category = entity.category,
            websites = websites,
            watchProgress = watchProgress,
        )
    }

    fun dtoToDomain(dto: ShowDto): Show = Show(
        id = dto.id,
        title = dto.title,
        titleChinese = dto.titleChinese,
        slug = dto.slug,
        posterUrl = dto.posterUrl,
        description = dto.description,
        rating = dto.rating,
        year = dto.year,
        status = dto.status,
        genres = dto.genres ?: emptyList(),
        totalEpisodes = dto.totalEpisodes,
        category = dto.category,
        websites = dto.websites?.map {
            WebsiteInfo(it.id, it.name, it.displayName, it.episodeCount)
        } ?: emptyList(),
    )

    fun watchHistoryToDomain(entity: WatchHistoryEntity): WatchProgress = WatchProgress(
        showId = entity.showId,
        episodeNumber = entity.episodeNumber,
        progressSeconds = entity.progressSeconds,
        durationSeconds = entity.durationSeconds,
        completed = entity.completed,
    )
}
