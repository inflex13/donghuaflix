package com.donghuaflix.data.repository

import com.donghuaflix.data.local.dao.ShowDao
import com.donghuaflix.data.local.dao.WatchHistoryDao
import com.donghuaflix.data.mapper.ShowMapper
import com.donghuaflix.data.remote.DonghuaApi
import com.donghuaflix.domain.model.Episode
import com.donghuaflix.domain.model.Show
import com.donghuaflix.domain.model.SubtitleTrack
import com.donghuaflix.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowRepository @Inject constructor(
    private val api: DonghuaApi,
    private val showDao: ShowDao,
    private val watchHistoryDao: WatchHistoryDao,
) {
    fun observeAllShows(): Flow<List<Show>> = showDao.getAllShows().map { entities ->
        entities.map { ShowMapper.entityToDomain(it) }
    }

    suspend fun getShowsFromApi(website: String, pageSize: Int = 20): List<Show> {
        return try {
            val result = api.getShows(page = 1, pageSize = pageSize, website = website)
            result.items.map { ShowMapper.dtoToDomain(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getShow(id: Int): Show? {
        // Try local first
        val local = showDao.getShowById(id)
        if (local != null) {
            val progress = watchHistoryDao.getLastWatchedForShow(id)
            return ShowMapper.entityToDomain(local, progress?.let { ShowMapper.watchHistoryToDomain(it) })
        }

        // Fallback to API
        return try {
            val dto = api.getShow(id)
            showDao.upsertShow(ShowMapper.dtoToEntity(dto))
            ShowMapper.dtoToDomain(dto)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchShows(query: String): List<Show> {
        // Search locally first
        val local = showDao.searchShows(query)
        if (local.isNotEmpty()) return local.map { ShowMapper.entityToDomain(it) }

        // Fallback to API
        return try {
            val result = api.searchShows(query)
            val entities = result.items.map { ShowMapper.dtoToEntity(it) }
            showDao.upsertShows(entities)
            result.items.map { ShowMapper.dtoToDomain(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getShowsByGenre(genre: String): List<Show> {
        val local = showDao.getShowsByGenre(genre)
        return local.map { ShowMapper.entityToDomain(it) }
    }

    suspend fun getRecentShows(limit: Int = 20): List<Show> {
        return showDao.getRecentShows(limit).map { ShowMapper.entityToDomain(it) }
    }

    suspend fun getCompletedShows(): List<Show> {
        return showDao.getShowsByStatus("completed").map { ShowMapper.entityToDomain(it) }
    }

    suspend fun getEpisodes(showId: Int, website: String? = null): List<Episode> {
        return try {
            api.getShowEpisodes(showId, website).map {
                Episode(it.id, it.episodeNumber, it.title, it.externalUrl, it.websiteName)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getEpisodeSources(episodeId: Int): List<VideoSource> {
        return try {
            api.getEpisodeSources(episodeId).map {
                VideoSource(it.id, it.sourceName, it.sourceKey, it.sourceUrl, it.sourceType, it.websiteName)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun resolveSource(sourceId: Int): VideoSource? {
        return try {
            val dto = api.resolveSource(sourceId)
            val subtitles = dto.subtitles?.mapNotNull { (lang, sub) ->
                val url = sub.url ?: return@mapNotNull null
                SubtitleTrack(
                    language = lang,
                    label = sub.label ?: lang,
                    url = url,
                )
            } ?: emptyList()
            VideoSource(dto.id, dto.sourceName, dto.sourceKey, dto.sourceUrl, dto.sourceType, dto.websiteName, subtitles)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllGenres(): List<String> {
        val localGenres = showDao.getAllGenreStrings()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        if (localGenres.isNotEmpty()) return localGenres

        return try {
            api.getGenres()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
