package com.donghuaflix.domain.model

data class Show(
    val id: Int,
    val title: String,
    val titleChinese: String? = null,
    val slug: String? = null,
    val posterUrl: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val totalEpisodes: Int? = null,
    val category: String? = null,
    val websites: List<WebsiteInfo> = emptyList(),
    val watchProgress: WatchProgress? = null,
)

data class WebsiteInfo(
    val id: Int,
    val name: String,
    val displayName: String,
    val episodeCount: Int? = null,
)

data class Episode(
    val id: Int,
    val episodeNumber: Int,
    val title: String? = null,
    val externalUrl: String? = null,
    val websiteName: String? = null,
)

data class SubtitleTrack(
    val language: String,
    val label: String,
    val url: String,
)

data class VideoSource(
    val id: Int,
    val sourceName: String,
    val sourceKey: String,
    val sourceUrl: String? = null,
    val sourceType: String? = null,
    val websiteName: String? = null,
    val subtitles: List<SubtitleTrack> = emptyList(),
)

data class WatchProgress(
    val showId: Int,
    val episodeNumber: Int,
    val progressSeconds: Int = 0,
    val durationSeconds: Int? = null,
    val completed: Boolean = false,
)

data class StreamInfo(
    val url: String,
    val type: StreamType,
)

enum class StreamType {
    HLS, DASH, MP4, UNKNOWN;

    companion object {
        fun fromUrl(url: String): StreamType = when {
            url.contains(".m3u8") -> HLS
            url.contains(".mpd") -> DASH
            url.contains(".mp4") || url.contains(".mkv") || url.contains(".webm") -> MP4
            else -> UNKNOWN
        }
    }
}
