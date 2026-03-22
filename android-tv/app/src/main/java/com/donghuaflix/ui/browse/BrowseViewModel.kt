package com.donghuaflix.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donghuaflix.data.repository.ShowRepository
import com.donghuaflix.domain.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val shows: List<Show> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val selectedWebsite: String? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val showRepository: ShowRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadGenres()
        loadShows()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            val genres = showRepository.getAllGenres()
            _uiState.update { it.copy(genres = genres) }
        }
    }

    private fun loadShows() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val genre = _uiState.value.selectedGenre
            val website = _uiState.value.selectedWebsite

            var shows = if (genre != null) {
                showRepository.getShowsByGenre(genre)
            } else {
                showRepository.getRecentShows(500)
            }

            // Filter by website if selected
            if (website != null) {
                shows = shows.filter { show ->
                    show.websites.any { it.name == website }
                }
            }

            _uiState.update { it.copy(shows = shows, isLoading = false) }
        }
    }

    fun selectGenre(genre: String?) {
        _uiState.update { it.copy(selectedGenre = genre) }
        loadShows()
    }

    fun selectWebsite(website: String?) {
        _uiState.update { it.copy(selectedWebsite = website) }
        loadShows()
    }
}
