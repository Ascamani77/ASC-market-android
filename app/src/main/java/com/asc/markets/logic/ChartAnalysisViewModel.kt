package com.asc.markets.logic

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.asc.markets.data.remote.RunAiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ChartAnalysisUiState {
    object Idle : ChartAnalysisUiState()
    object Loading : ChartAnalysisUiState()
    data class Success(val summary: String, val rawPayload: RunAiResponse) : ChartAnalysisUiState()
    data class Error(val message: String) : ChartAnalysisUiState()
}

class ChartAnalysisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChartAnalysisUiState>(ChartAnalysisUiState.Idle)
    val uiState: StateFlow<ChartAnalysisUiState> = _uiState.asStateFlow()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()
    
    private val _chartDescription = MutableStateFlow("")
    val chartDescription: StateFlow<String> = _chartDescription.asStateFlow()

    fun onImageSelected(bitmap: Bitmap) {
        _selectedBitmap.value = bitmap
        _uiState.value = ChartAnalysisUiState.Idle
    }
    
    fun clearImage() {
        _selectedBitmap.value = null
    }
    
    fun onChartDescriptionChanged(description: String) {
        _chartDescription.value = description
    }

    fun analyzeChart(
        personaName: String,
        personaInstruction: String,
        forexViewModel: ForexViewModel? = null
    ) {
        _uiState.value = ChartAnalysisUiState.Error("AI Analysis service is currently offline for architectural updates.")
    }

    fun clear() {
        _selectedBitmap.value = null
        _chartDescription.value = ""
        _uiState.value = ChartAnalysisUiState.Idle
    }
}
