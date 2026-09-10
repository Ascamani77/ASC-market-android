package com.asc.markets.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for managing ASC Backend API connection
 * 
 * This handles all communication with the backend API and maintains
 * the app's state. The app never talks to Redis or the AI system directly.
 */

sealed class BackendConnectionState {
    object Disconnected : BackendConnectionState()
    object Connecting : BackendConnectionState()
    data class Connected(val health: HealthCheck) : BackendConnectionState()
    data class Error(val message: String) : BackendConnectionState()
}

data class BackendUiState(
    val connectionState: BackendConnectionState = BackendConnectionState.Disconnected,
    val marketOverview: MarketOverview? = null,
    val allSignals: List<MarketSignal> = emptyList(),
    val selectedAssetSignal: MarketSignal? = null,
    val selectedAssetAnalysis: AssetAnalysis? = null,
    val tradingOpportunities: List<MarketSignal> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AscBackendViewModel(
    private val apiBaseUrl: String = com.asc.markets.data.NetworkConfig.DEFAULT_BACKEND_URL
) : ViewModel() {

    private val api = AscBackendApi.create(apiBaseUrl)

    private val _uiState = MutableStateFlow(BackendUiState())
    val uiState: StateFlow<BackendUiState> = _uiState.asStateFlow()

    private var autoRefreshEnabled = false

    init {
        // Auto-connect on initialization
        connectToBackend()
    }

    // ========================================================================
    // Connection Management
    // ========================================================================

    /**
     * Connect to the backend API and verify health
     */
    fun connectToBackend() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionState = BackendConnectionState.Connecting,
                error = null
            )

            api.checkHealth().fold(
                onSuccess = { health ->
                    if (health.ai_system_active) {
                        _uiState.value = _uiState.value.copy(
                            connectionState = BackendConnectionState.Connected(health),
                            error = null
                        )
                        // Load initial data
                        refreshAll()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            connectionState = BackendConnectionState.Error(
                                "AI system not active. Run feeders first."
                            ),
                            error = "AI system not running"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        connectionState = BackendConnectionState.Error(
                            error.message ?: "Connection failed"
                        ),
                        error = "Cannot connect to backend: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Enable/disable auto-refresh (every 5 seconds)
     */
    fun setAutoRefresh(enabled: Boolean) {
        autoRefreshEnabled = enabled
        if (enabled) {
            startAutoRefresh()
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (autoRefreshEnabled) {
                delay(5000) // 5 seconds
                refreshAll()
            }
        }
    }

    // ========================================================================
    // Data Loading
    // ========================================================================

    /**
     * Refresh all data (overview + signals)
     */
    fun refreshAll() {
        loadMarketOverview()
        loadAllSignals()
        loadTradingOpportunities()
    }

    /**
     * Load market overview
     */
    fun loadMarketOverview() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            api.getMarketOverview().fold(
                onSuccess = { overview ->
                    _uiState.value = _uiState.value.copy(
                        marketOverview = overview,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load market overview: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Load all market signals
     */
    fun loadAllSignals(
        minConfidence: Double = 0.0,
        direction: String? = null,
        phase: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            api.getAllSignals(minConfidence, direction, phase).fold(
                onSuccess = { signals ->
                    _uiState.value = _uiState.value.copy(
                        allSignals = signals,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load signals: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Load signal for a specific asset
     */
    fun loadAssetSignal(asset: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            api.getAssetSignal(asset).fold(
                onSuccess = { signal ->
                    _uiState.value = _uiState.value.copy(
                        selectedAssetSignal = signal,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load signal for $asset: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Load detailed analysis for a specific asset
     */
    fun loadAssetAnalysis(asset: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            api.getAssetAnalysis(asset).fold(
                onSuccess = { analysis ->
                    _uiState.value = _uiState.value.copy(
                        selectedAssetAnalysis = analysis,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load analysis for $asset: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Load trading opportunities (scanner)
     */
    fun loadTradingOpportunities(
        minConfidence: Double = 70.0,
        minAiScore: Double = 60.0,
        phase: String = "EXPANSION,PRE-MOVE"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            api.getTradingOpportunities(minConfidence, minAiScore, phase).fold(
                onSuccess = { opportunities ->
                    _uiState.value = _uiState.value.copy(
                        tradingOpportunities = opportunities,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load opportunities: ${error.message}"
                    )
                }
            )
        }
    }

    // ========================================================================
    // UI Helpers
    // ========================================================================

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return _uiState.value.connectionState is BackendConnectionState.Connected
    }

    /**
     * Get connection status message
     */
    fun getConnectionStatusMessage(): String {
        return when (val state = _uiState.value.connectionState) {
            is BackendConnectionState.Disconnected -> "Disconnected"
            is BackendConnectionState.Connecting -> "Connecting..."
            is BackendConnectionState.Connected -> "Connected (${state.health.active_feeders} feeders active)"
            is BackendConnectionState.Error -> "Error: ${state.message}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshEnabled = false
    }
}
