package com.donghuaflix.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donghuaflix.data.repository.ShowRepository
import com.donghuaflix.domain.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Show> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val showRepository: ShowRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }

        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(results = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _uiState.update { it.copy(isLoading = true) }
            val results = showRepository.searchShows(query)
            _uiState.update { it.copy(results = results, isLoading = false) }
        }
    }
}
