package com.donghuaflix.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donghuaflix.data.repository.ShowRepository
import com.donghuaflix.data.repository.WatchRepository
import com.donghuaflix.domain.model.Episode
import com.donghuaflix.domain.model.Show
import com.donghuaflix.domain.model.WatchProgress
import com.donghuaflix.domain.model.WebsiteInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpisodeWatchInfo(
    val episodeNumber: Int,
    val progressFraction: Float, // 0.0 to 1.0
    val completed: Boolean,
)

data class DetailUiState(
    val show: Show? = null,
    val episodes: List<Episode> = emptyList(),
    val selectedWebsite: WebsiteInfo? = null,
    val lastWatched: WatchProgress? = null,
    val isInWatchlist: Boolean = false,
    val isLoading: Boolean = true,
    val watchedEpisodes: Map<Int, EpisodeWatchInfo> = emptyMap(),
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val showRepository: ShowRepository,
    private val watchRepository: WatchRepository,
) : ViewModel() {

    private val showId: Int = savedStateHandle["showId"] ?: 0

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadShow()
    }

    private fun loadShow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val show = showRepository.getShow(showId)
            val inWatchlist = watchRepository.isInWatchlist(showId)
            val lastWatched = watchRepository.getLastWatchedForShow(showId)

            // Fetch all watched episodes for this show
            val watchHistory = watchRepository.getWatchedEpisodesForShow(showId)
            val watchedEpisodes = watchHistory.mapValues { (_, wp) ->
                val fraction = if (wp.durationSeconds != null && wp.durationSeconds > 0) {
                    wp.progressSeconds.toFloat() / wp.durationSeconds
                } else 0f
                EpisodeWatchInfo(
                    episodeNumber = wp.episodeNumber,
                    progressFraction = fraction,
                    completed = wp.completed,
                )
            }

            if (show != null) {
                val defaultWebsite = show.websites.firstOrNull()
                _uiState.update {
                    it.copy(
                        show = show,
                        selectedWebsite = defaultWebsite,
                        lastWatched = lastWatched,
                        isInWatchlist = inWatchlist,
                        isLoading = false,
                        watchedEpisodes = watchedEpisodes,
                    )
                }
                // Load episodes for default website
                if (defaultWebsite != null) {
                    loadEpisodes(defaultWebsite.name)
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectWebsite(website: WebsiteInfo) {
        _uiState.update { it.copy(selectedWebsite = website) }
        loadEpisodes(website.name)
    }

    private fun loadEpisodes(websiteName: String) {
        viewModelScope.launch {
            val episodes = showRepository.getEpisodes(showId, websiteName)
            _uiState.update { it.copy(episodes = episodes) }
        }
    }

    fun refreshWatchHistory() {
        viewModelScope.launch {
            val lastWatched = watchRepository.getLastWatchedForShow(showId)
            val watchHistory = watchRepository.getWatchedEpisodesForShow(showId)
            val watchedEpisodes = watchHistory.mapValues { (_, wp) ->
                val fraction = if (wp.durationSeconds != null && wp.durationSeconds > 0) {
                    wp.progressSeconds.toFloat() / wp.durationSeconds
                } else 0f
                EpisodeWatchInfo(
                    episodeNumber = wp.episodeNumber,
                    progressFraction = fraction,
                    completed = wp.completed,
                )
            }
            _uiState.update {
                it.copy(
                    lastWatched = lastWatched,
                    watchedEpisodes = watchedEpisodes,
                )
            }
        }
    }

    fun markEpisodeWatched(episodeNumber: Int) {
        viewModelScope.launch {
            watchRepository.markAsWatched(showId, episodeNumber)
            refreshWatchHistory()
        }
    }

    fun markEpisodeUnwatched(episodeNumber: Int) {
        viewModelScope.launch {
            watchRepository.markAsUnwatched(showId, episodeNumber)
            refreshWatchHistory()
        }
    }

    fun markAllWatchedUpTo(episodeNumber: Int) {
        viewModelScope.launch {
            val episodes = _uiState.value.episodes.filter { it.episodeNumber <= episodeNumber }
            for (ep in episodes) {
                watchRepository.markAsWatched(showId, ep.episodeNumber)
            }
            refreshWatchHistory()
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            watchRepository.toggleWatchlist(showId)
            _uiState.update { it.copy(isInWatchlist = !it.isInWatchlist) }
        }
    }
}
