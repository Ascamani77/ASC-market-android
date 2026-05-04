package com.asc.markets.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AiUiState(
    val isLoading: Boolean = false,
    val message: String = "",
    val decisions: List<FinalDecisionItem> = emptyList(),
    val error: String? = null
)

class AiViewModel : ViewModel() {

    private val repository = AiRepository()

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState

    init {
        // Observe repository deployments and update UI state
        viewModelScope.launch {
            repository.deployments.collect { response ->
                if (response != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Updated ${response.last_updated ?: ""}",
                        decisions = response.final_decision,
                        error = null
                    )
                }
            }
        }
    }

    fun fetchLatest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.fetchLatestDeployments()
        }
    }

    fun runAi() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.runAiPipeline()
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
        }
    }
}
