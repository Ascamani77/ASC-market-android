package com.intelligence.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intelligence.dashboard.model.IntelligenceEvent
import com.intelligence.dashboard.repository.IntelligenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class IntelligenceUiState {
    object Loading : IntelligenceUiState()
    data class Success(val events: List<IntelligenceEvent>) : IntelligenceUiState()
    data class Error(val message: String) : IntelligenceUiState()
}

class IntelligenceViewModel(
    private val repository: IntelligenceRepository = IntelligenceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<IntelligenceUiState>(IntelligenceUiState.Loading)
    val uiState: StateFlow<IntelligenceUiState> = _uiState.asStateFlow()

    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    init {
        fetchEvents()
    }

    fun toggleWatchlist(id: String) {
        _watchlist.value = if (_watchlist.value.contains(id)) {
            _watchlist.value - id
        } else {
            _watchlist.value + id
        }
    }

    private fun fetchEvents() {
        viewModelScope.launch {
            try {
                repository.getIntelligenceEvents().collect { events ->
                    _uiState.value = IntelligenceUiState.Success(events)
                }
            } catch (e: Exception) {
                _uiState.value = IntelligenceUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
