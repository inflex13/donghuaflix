package com.donghuaflix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donghuaflix.data.repository.ImagePreloader
import com.donghuaflix.data.repository.ShowRepository
import com.donghuaflix.data.repository.SyncRepository
import com.donghuaflix.data.repository.WatchRepository
import com.donghuaflix.sync.AppUpdater
import com.donghuaflix.domain.model.Show
import com.donghuaflix.domain.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeSection(
    val title: String,
    val shows: List<Show>,
    val type: String = "",
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<HomeSection> = emptyList(),
    val continueWatching: List<Pair<Show, WatchProgress>> = emptyList(),
    val watchlistIds: Set<Int> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val watchRepository: WatchRepository,
    private val syncRepository: SyncRepository,
    private val imagePreloader: ImagePreloader,
    private val appUpdater: AppUpdater,
) : ViewModel() {

    val updateState = appUpdater.updateState

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                syncRepository.syncAll()
            } catch (_: Exception) {}
            loadHome()
            // Image preloading disabled — Coil's lazy loading + disk cache handles this
        }
        observeContinueWatching()
        observeWatchlist()
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            watchRepository.observeWatchlist().collect { ids ->
                _uiState.update { it.copy(watchlistIds = ids.toSet()) }
            }
        }
    }

    private suspend fun loadHome() {
        _uiState.update { it.copy(isLoading = true) }

        try {
            val sections = mutableListOf<HomeSection>()

            // Recently Added — per website
            val recent = showRepository.getRecentShows(100)
            val donghuafunShows = recent.filter { show ->
                show.websites.any { it.name == "donghuafun" }
            }.take(20)
            val animekhorShows = recent.filter { show ->
                show.websites.any { it.name == "animekhor" }
            }.take(20)

            if (donghuafunShows.isNotEmpty()) {
                sections.add(HomeSection("DonghuaFun", donghuafunShows, "donghuafun"))
            }
            if (animekhorShows.isNotEmpty()) {
                sections.add(HomeSection("AnimeKhor", animekhorShows, "animekhor"))
            }

            // By Genre
            val genres = showRepository.getAllGenres().take(4)
            for (genre in genres) {
                val shows = showRepository.getShowsByGenre(genre).take(20)
                if (shows.isNotEmpty()) {
                    sections.add(HomeSection(genre, shows, "genre"))
                }
            }

            // Completed
            val completed = showRepository.getCompletedShows().take(20)
            if (completed.isNotEmpty()) {
                sections.add(HomeSection("Completed Series", completed, "completed"))
            }

            _uiState.update { it.copy(isLoading = false, sections = sections, error = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            watchRepository.observeContinueWatching().collect { progressList ->
                val pairs = progressList.mapNotNull { progress ->
                    val show = showRepository.getShow(progress.showId)
                    show?.let { it to progress }
                }
                _uiState.update { it.copy(continueWatching = pairs) }
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            try {
                appUpdater.checkForUpdate()
            } catch (_: Exception) {}
        }
    }

    fun triggerUpdate() {
        try {
            appUpdater.dismissUpdate()
            appUpdater.downloadAndInstall()
        } catch (_: Exception) {}
    }

    fun dismissUpdateMessage() {
        appUpdater.dismissUpdate()
    }

    fun refresh() {
        viewModelScope.launch {
            syncRepository.syncAll()
            loadHome()
        }
    }

    fun fullResync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                syncRepository.fullResync()
                loadHome()
            } catch (_: Exception) {
                loadHome()
            }
        }
    }
}
