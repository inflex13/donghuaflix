package com.donghuaflix.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donghuaflix.data.repository.ShowRepository
import com.donghuaflix.data.repository.WatchRepository
import com.donghuaflix.domain.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val shows: List<Show> = emptyList(),
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val showRepository: ShowRepository,
    private val watchRepository: WatchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            watchRepository.observeWatchlist().collect { showIds ->
                val shows = showIds.mapNotNull { showRepository.getShow(it) }
                _uiState.update { it.copy(shows = shows) }
            }
        }
    }
}
