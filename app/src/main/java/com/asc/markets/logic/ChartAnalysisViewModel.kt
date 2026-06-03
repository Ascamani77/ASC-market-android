package com.asc.markets.logic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.api.ApiClient
import com.asc.markets.data.remote.ChartAnalysisRequest
import com.asc.markets.data.remote.RunAiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

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
        val description = _chartDescription.value
        if (description.isBlank()) {
            _uiState.value = ChartAnalysisUiState.Error("Please provide a chart description (symbol, timeframe, and your analysis)")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = ChartAnalysisUiState.Loading
            
            try {
                // Gather current market data and user parameters
                val currentMarketData = buildCurrentMarketData(forexViewModel)
                val userParameters = buildUserParameters(forexViewModel)
                
                // Use Groq to analyze the user's chart description + compare with current market
                val summary = analyzeChartWithGroq(
                    chartDescription = description,
                    personaName = personaName,
                    personaInstruction = personaInstruction,
                    currentMarketData = currentMarketData,
                    userParameters = userParameters
                )
                
                // Create a mock response for display
                val mockResponse = RunAiResponse(
                    success = true,
                    message = "Chart analyzed successfully",
                    final_decision = emptyList()
                )
                
                _uiState.value = ChartAnalysisUiState.Success(summary, mockResponse)
            } catch (e: Exception) {
                _uiState.value = ChartAnalysisUiState.Error("Analysis error: ${e.message}")
            }
        }
    }
    
    private suspend fun analyzeChartWithGroq(
        chartDescription: String,
        personaName: String,
        personaInstruction: String,
        currentMarketData: String,
        userParameters: String
    ): String {
        if (!com.asc.markets.backend.GroqClient.isKeyConfigured()) {
            return "ASC Engine v1 is offline: GROQ_API_KEY is not configured."
        }
        
        val aiContext = com.asc.markets.ai.AIContextService.contextState.value
        val platformContext = aiContext.platformContext
        
        val prompt = """
            You are ASC Engine v1, an expert trading analyst for ASC Market platform.
            
            [PLATFORM_CONTEXT]
            - Data Sources: Pepperstone cTrader (FOREX, commodities, indices, crypto) + Binance (USDT pairs)
            - Analysis Style: $personaName
            - Persona Instruction: $personaInstruction
            
            [USER'S CHART ANALYSIS]
            The user has provided the following chart analysis:
            
            $chartDescription
            
            [YOUR TASK]
            1. EXTRACT KEY INFORMATION: Identify the asset, timeframe, and key observations from the user's analysis
            2. VALIDATE WITH CURRENT MARKET: Compare the user's analysis with live market data below
            3. ENHANCE THE ANALYSIS: Add your expert insights based on current market conditions
            4. PROVIDE TRADING SIGNAL: Give specific entry, stop loss, and take profit levels
            
            [CURRENT LIVE MARKET DATA]
            $currentMarketData
            
            [USER TRADING PARAMETERS]
            $userParameters
            
            [OUTPUT FORMAT]
            Provide your analysis in this exact format:
            
            🎯 SIGNAL: [LONG/SHORT/NEUTRAL]
            
            📊 USER'S CHART ANALYSIS:
            • Asset: [Extract from user's description]
            • Timeframe: [Extract from user's description]
            • User's Observation: [Summarize what user described]
            • Pattern/Setup: [What user identified]
            
            📈 CURRENT MARKET VALIDATION:
            • Current Price: [From live data - find the asset user mentioned]
            • Price Action: [How is price moving right now?]
            • Alignment: [Does current market support user's analysis? YES/NO/PARTIAL]
            • Market Context: [Any relevant current market conditions]
            
            💡 ENHANCED TRADE SETUP:
            • Entry: [Specific price level based on user's analysis + current market]
            • Stop Loss: [Specific price level with reasoning]
            • Take Profit: TP1: [level] | TP2: [level] | TP3: [level]
            • Risk/Reward: [Calculate ratio]
            • Confidence: [0-100% based on alignment with current market]
            
            🔍 EXPERT REASONING:
            • [Why user's analysis is valid or needs adjustment]
            • [What current market data confirms or contradicts]
            • [Key factors to consider for this trade]
            • [What to watch for as the trade develops]
            
            ⚠️ RISK NOTE:
            [Any warnings, invalidation levels, or conditions that would cancel the setup]
            
            IMPORTANT RULES:
            1. Extract the EXACT asset symbol and timeframe from user's description
            2. Find that asset in the current market data
            3. Be specific with price levels based on user's analysis
            4. If user's analysis conflicts with current market, explain why
            5. Provide actionable advice that respects user's original analysis
        """.trimIndent()
        
        return try {
            com.asc.markets.backend.GroqClient.chatCompletion(prompt, model = "llama-3.3-70b-versatile")
        } catch (e: Exception) {
            "Unable to analyze chart: ${e.message}"
        }
    }
    
    private fun buildCurrentMarketData(forexViewModel: ForexViewModel?): String {
        if (forexViewModel == null) return "ForexViewModel not available"
        
        return buildString {
            appendLine("=== CURRENT MARKET DATA ===")
            
            // Get all available pairs from MarketDataStore and BinanceDataStore
            val marketPairs = com.asc.markets.data.MarketDataStore.allPairs.value
            val binancePairs = com.asc.markets.data.BinanceDataStore.allPairs.value
            val allPairs = (marketPairs + binancePairs).distinctBy { it.symbol }
            
            if (allPairs.isNotEmpty()) {
                appendLine("Available Assets (${allPairs.size} total):")
                allPairs.take(10).forEach { pair ->
                    appendLine("  ${pair.symbol}: ${pair.price} (${if (pair.changePercent >= 0) "+" else ""}${pair.changePercent}%)")
                }
                if (allPairs.size > 10) {
                    appendLine("  ... and ${allPairs.size - 10} more assets")
                }
                appendLine()
            }
            
            // Add AI deployments context if available
            val aiDeployments = forexViewModel.aiDeployments.value
            if (aiDeployments != null && aiDeployments.final_decision.isNotEmpty()) {
                appendLine("=== CURRENT AI DEPLOYMENTS ===")
                appendLine("Total Deployments: ${aiDeployments.count}")
                appendLine("Last Updated: ${aiDeployments.last_updated ?: "unknown"}")
                appendLine()
                appendLine("Top AI Signals:")
                aiDeployments.final_decision.take(5).forEach { deployment ->
                    appendLine("  ${deployment.asset_1}: ${deployment.journal_direction} (Score: ${deployment.pre_move_ai_score})")
                }
                appendLine()
            }
        }
    }
    
    private fun buildUserParameters(forexViewModel: ForexViewModel?): String {
        if (forexViewModel == null) return "ForexViewModel not available"
        
        return buildString {
            appendLine("=== USER TRADING PARAMETERS ===")
            
            // Get current view context
            val currentView = forexViewModel.currentView.value
            appendLine("Current View: $currentView")
            
            // Get selected asset
            val selectedPair = forexViewModel.selectedPair.value
            appendLine("Selected Asset: ${selectedPair.symbol}")
            appendLine("Selected Asset Price: ${selectedPair.price}")
            appendLine("Selected Asset Change: ${selectedPair.changePercent}%")
            appendLine()
            
            // Get AI deployments for context
            val aiDeployments = forexViewModel.aiDeployments.value
            if (aiDeployments != null && aiDeployments.final_decision.isNotEmpty()) {
                appendLine("Active AI Deployments: ${aiDeployments.count}")
                appendLine("Last Updated: ${aiDeployments.last_updated ?: "unknown"}")
                appendLine()
            }
            
            // Add risk management context
            appendLine("Risk Management Guidelines:")
            appendLine("  - Risk per trade: Conservative (1-2% per trade)")
            appendLine("  - Stop loss: Mandatory on all positions")
            appendLine("  - Position sizing: Based on account size and volatility")
            appendLine("  - Max concurrent trades: Diversified across assets")
            appendLine()
            
            // Add platform context
            appendLine("Platform Configuration:")
            appendLine("  - Primary Data: Pepperstone cTrader (FOREX, commodities, indices, crypto)")
            appendLine("  - Secondary Data: Binance (USDT pairs)")
            appendLine("  - AI Analysis: ASC Engine v1 with real-time deployments")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun clear() {
        _selectedBitmap.value = null
        _chartDescription.value = ""
        _uiState.value = ChartAnalysisUiState.Idle
    }
}
