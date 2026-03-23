package com.donghuaflix.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ShowDto(
    val id: Int,
    val title: String,
    @SerializedName("title_chinese") val titleChinese: String? = null,
    val slug: String? = null,
    @SerializedName("poster_url") val posterUrl: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val status: String? = null,
    val genres: List<String>? = null,
    @SerializedName("total_episodes") val totalEpisodes: Int? = null,
    val category: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("remote_updated_at") val remoteUpdatedAt: String? = null,
    val websites: List<WebsiteInfoDto>? = null,
)

data class WebsiteInfoDto(
    val id: Int,
    val name: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("episode_count") val episodeCount: Int? = null,
)

data class ShowListDto(
    val items: List<ShowDto>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
)

data class EpisodeDto(
    val id: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    val title: String? = null,
    @SerializedName("external_url") val externalUrl: String? = null,
    @SerializedName("website_name") val websiteName: String? = null,
)

data class SubtitleDto(
    val label: String? = null,
    val url: String? = null,
)

data class SourceDto(
    val id: Int,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("source_key") val sourceKey: String,
    @SerializedName("source_url") val sourceUrl: String? = null,
    @SerializedName("source_type") val sourceType: String? = null,
    @SerializedName("website_name") val websiteName: String? = null,
    val subtitles: Map<String, SubtitleDto>? = null,
)

data class WatchProgressDto(
    val id: Int? = null,
    @SerializedName("show_id") val showId: Int,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("progress_seconds") val progressSeconds: Int = 0,
    @SerializedName("duration_seconds") val durationSeconds: Int? = null,
    val completed: Boolean = false,
    @SerializedName("watched_at") val watchedAt: String? = null,
    @SerializedName("episode_id") val episodeId: Int? = null,
)

data class SyncResponseDto(
    val shows: List<ShowDto>,
    @SerializedName("watch_history") val watchHistory: List<WatchProgressDto>,
    val watchlist: List<Int>,
    val timestamp: String,
)

data class HomeSectionDto(
    val title: String,
    @SerializedName("section_type") val sectionType: String,
    val shows: List<ShowDto>,
)

data class HomeResponseDto(
    val sections: List<HomeSectionDto>,
)

data class AppVersionDto(
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("apk_size") val apkSize: Long = 0,
    val changelog: String? = null,
)

data class CrashLogRequest(
    val level: String,
    val message: String,
    val stacktrace: String? = null,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("device_info") val deviceInfo: String,
    val screen: String? = null,
    val extra: String? = null,
)
