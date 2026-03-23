package com.donghuaflix.data.remote

import com.donghuaflix.data.remote.dto.*
import retrofit2.http.*

interface DonghuaApi {

    // Shows
    @GET("api/shows")
    suspend fun getShows(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("genre") genre: String? = null,
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("website") website: String? = null,
    ): ShowListDto

    @GET("api/shows/search")
    suspend fun searchShows(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ShowListDto

    @GET("api/shows/{id}")
    suspend fun getShow(@Path("id") id: Int): ShowDto

    @GET("api/shows/{id}/websites")
    suspend fun getShowWebsites(@Path("id") id: Int): List<WebsiteInfoDto>

    // Episodes
    @GET("api/shows/{showId}/episodes")
    suspend fun getShowEpisodes(
        @Path("showId") showId: Int,
        @Query("website") website: String? = null,
    ): List<EpisodeDto>

    @GET("api/episodes/{episodeId}/sources")
    suspend fun getEpisodeSources(@Path("episodeId") episodeId: Int): List<SourceDto>

    // Sources
    @POST("api/sources/{sourceId}/resolve")
    suspend fun resolveSource(@Path("sourceId") sourceId: Int): SourceDto

    // Watch tracking
    @POST("api/watch/progress")
    suspend fun updateProgress(@Body progress: WatchProgressDto): WatchProgressDto

    @GET("api/watch/continue")
    suspend fun getContinueWatching(): List<WatchProgressDto>

    @GET("api/watch/history")
    suspend fun getWatchHistory(@Query("limit") limit: Int = 50): List<WatchProgressDto>

    // Watchlist
    @GET("api/watchlist")
    suspend fun getWatchlist(): List<ShowDto>

    @POST("api/watchlist/{showId}")
    suspend fun addToWatchlist(@Path("showId") showId: Int)

    @DELETE("api/watchlist/{showId}")
    suspend fun removeFromWatchlist(@Path("showId") showId: Int)

    // Sync
    @GET("api/sync")
    suspend fun sync(@Query("since") since: String? = null): SyncResponseDto

    // Discovery
    @GET("api/home")
    suspend fun getHome(): HomeResponseDto

    @GET("api/genres")
    suspend fun getGenres(): List<String>

    @GET("api/websites")
    suspend fun getWebsites(): List<Map<String, Any>>

    // App update
    @GET("app/version")
    suspend fun getLatestVersion(): AppVersionDto

    // Crash logging
    @POST("api/crash-logs")
    suspend fun sendCrashLog(@Body log: CrashLogRequest): retrofit2.Response<Unit>
}
