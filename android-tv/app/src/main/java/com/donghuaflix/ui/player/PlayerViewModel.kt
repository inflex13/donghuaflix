package com.donghuaflix.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donghuaflix.data.repository.ShowRepository
import com.donghuaflix.data.repository.WatchRepository
import com.donghuaflix.domain.model.Episode
import com.donghuaflix.domain.model.StreamType
import com.donghuaflix.domain.model.SubtitleTrack
import com.donghuaflix.domain.model.VideoSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlayerControl { PLAY_PAUSE, PREV_EP, NEXT_EP, AUTOPLAY }

data class PlayerUiState(
    val showTitle: String = "",
    val episodeNumber: Int = 1,
    val streamUrl: String? = null,
    val streamType: StreamType = StreamType.UNKNOWN,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val sources: List<VideoSource> = emptyList(),
    val selectedSource: VideoSource? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val resumePositionMs: Long = 0,
    val showControls: Boolean = false,
    val autoPlayNext: Boolean = true,
    val hasNextEpisode: Boolean = false,
    val hasPrevEpisode: Boolean = false,
    val playbackEnded: Boolean = false,
    val focusedControl: PlayerControl = PlayerControl.PLAY_PAUSE,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val showRepository: ShowRepository,
    private val watchRepository: WatchRepository,
) : ViewModel() {

    private val showId: Int = savedStateHandle["showId"] ?: 0
    private val initialEpisodeNumber: Int = savedStateHandle["episodeNumber"] ?: 1
    private val website: String? = savedStateHandle.get<String>("website")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(PlayerUiState(episodeNumber = initialEpisodeNumber))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        loadPlayer()
    }

    private fun loadPlayer() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Get show info
            val show = showRepository.getShow(showId)
            if (show != null) {
                _uiState.update { it.copy(showTitle = show.title) }
            }

            // Load episodes
            val episodes = showRepository.getEpisodes(showId, website)
            _uiState.update { it.copy(episodes = episodes) }

            // Find current episode
            val currentEp = episodes.find { it.episodeNumber == _uiState.value.episodeNumber }
            if (currentEp != null) {
                loadEpisodeSources(currentEp)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Episode not found") }
            }

            // Check resume position
            val progress = watchRepository.getProgressForEpisode(showId, _uiState.value.episodeNumber)
            if (progress != null && !progress.completed) {
                _uiState.update { it.copy(resumePositionMs = progress.progressSeconds * 1000L) }
            }
        }
    }

    private suspend fun loadEpisodeSources(episode: Episode) {
        val sources = showRepository.getEpisodeSources(episode.id)
        if (sources.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, error = "No sources available") }
            return
        }

        // Prioritize dailymotion (4K) sources first, skip 1080eng/1080indo
        val sortedSources = sources.sortedByDescending {
            when {
                it.sourceName.contains("dailymotion", ignoreCase = true) -> 3
                it.sourceName.contains("4k", ignoreCase = true) -> 2
                else -> 0
            }
        }

        _uiState.update { it.copy(sources = sortedSources) }

        // Try each source until one works
        for (source in sortedSources) {
            val resolved = tryResolveSource(source)
            if (resolved != null) {
                _uiState.update {
                    it.copy(
                        selectedSource = source,
                        streamUrl = resolved.sourceUrl,
                        streamType = StreamType.fromUrl(resolved.sourceUrl ?: ""),
                        subtitles = resolved.subtitles,
                        isLoading = false,
                        error = null,
                    )
                }
                return
            }
        }

        _uiState.update { it.copy(isLoading = false, error = "No working source found") }
    }

    private suspend fun tryResolveSource(source: VideoSource): VideoSource? {
        if (source.sourceUrl != null) return source
        return showRepository.resolveSource(source.id)
    }

    fun selectSource(source: VideoSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedSource = source) }

            val resolved = tryResolveSource(source)
            if (resolved?.sourceUrl != null) {
                _uiState.update {
                    it.copy(
                        streamUrl = resolved.sourceUrl,
                        streamType = StreamType.fromUrl(resolved.sourceUrl),
                        subtitles = resolved.subtitles,
                        isLoading = false,
                        error = null,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load source") }
            }
        }
    }

    fun playEpisode(episodeNumber: Int) {
        _uiState.update { it.copy(episodeNumber = episodeNumber, resumePositionMs = 0, playbackEnded = false) }
        viewModelScope.launch {
            val episode = _uiState.value.episodes.find { it.episodeNumber == episodeNumber }
            if (episode != null) {
                _uiState.update { it.copy(isLoading = true) }
                loadEpisodeSources(episode)
                updateHasNext()
            }
        }
    }

    fun nextEpisode() {
        val current = _uiState.value.episodeNumber
        val sortedEps = _uiState.value.episodes.sortedBy { it.episodeNumber }
        val next = sortedEps.firstOrNull { it.episodeNumber > current }
        if (next != null) {
            playEpisode(next.episodeNumber)
        }
    }

    fun previousEpisode() {
        val current = _uiState.value.episodeNumber
        val sortedEps = _uiState.value.episodes.sortedBy { it.episodeNumber }
        val prev = sortedEps.lastOrNull { it.episodeNumber < current }
        if (prev != null) {
            playEpisode(prev.episodeNumber)
        }
    }

    fun onPlaybackEnded() {
        val hasNext = _uiState.value.hasNextEpisode
        if (hasNext && _uiState.value.autoPlayNext) {
            nextEpisode()
        } else {
            _uiState.update { it.copy(playbackEnded = true) }
        }
    }

    fun toggleAutoPlay() {
        _uiState.update { it.copy(autoPlayNext = !it.autoPlayNext) }
    }

    private fun updateHasNext() {
        val current = _uiState.value.episodeNumber
        val hasNext = _uiState.value.episodes.any { it.episodeNumber > current }
        val hasPrev = _uiState.value.episodes.any { it.episodeNumber < current }
        _uiState.update { it.copy(hasNextEpisode = hasNext, hasPrevEpisode = hasPrev) }
    }

    fun focusNextControl() {
        val controls = getAvailableControls()
        val currentIdx = controls.indexOf(_uiState.value.focusedControl)
        val nextIdx = (currentIdx + 1).coerceAtMost(controls.size - 1)
        _uiState.update { it.copy(focusedControl = controls[nextIdx]) }
    }

    fun focusPrevControl() {
        val controls = getAvailableControls()
        val currentIdx = controls.indexOf(_uiState.value.focusedControl)
        val prevIdx = (currentIdx - 1).coerceAtLeast(0)
        _uiState.update { it.copy(focusedControl = controls[prevIdx]) }
    }

    fun activateFocusedControl(isPlaying: Boolean): Boolean {
        return when (_uiState.value.focusedControl) {
            PlayerControl.PLAY_PAUSE -> false // handled by caller (needs player reference)
            PlayerControl.PREV_EP -> { previousEpisode(); true }
            PlayerControl.NEXT_EP -> { nextEpisode(); true }
            PlayerControl.AUTOPLAY -> { toggleAutoPlay(); true }
        }
    }

    private fun getAvailableControls(): List<PlayerControl> {
        val controls = mutableListOf(PlayerControl.PLAY_PAUSE)
        if (_uiState.value.hasPrevEpisode) controls.add(PlayerControl.PREV_EP)
        if (_uiState.value.hasNextEpisode) controls.add(PlayerControl.NEXT_EP)
        controls.add(PlayerControl.AUTOPLAY)
        return controls
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(500) // debounce
            watchRepository.updateProgress(
                showId = showId,
                episodeNumber = _uiState.value.episodeNumber,
                progressSeconds = (positionMs / 1000).toInt(),
                durationSeconds = (durationMs / 1000).toInt(),
            )
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun hideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun showControls() {
        _uiState.update { it.copy(showControls = true) }
    }
}
