package com.asc.markets.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Centralized AI Context Store
 * 
 * Single source of truth for all AI-generated insights across the app.
 * All pages (Market Watch, Macro Stream, AI Terminal, etc.) observe this store.
 */
object AIContextStore {
    private const val TAG = "AIContextStore"
    private const val UPDATE_INTERVAL_MS = 10_000L // 10 seconds
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // Backend URL (configurable)
    private var backendUrl = "http://10.164.138.133:8000"
    
    // Complete AI state
    private val _aiState = MutableStateFlow(AIState())
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()
    
    // Connection status
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Last update timestamp
    private val _lastUpdate = MutableStateFlow(0L)
    val lastUpdate: StateFlow<Long> = _lastUpdate.asStateFlow()
    
    /**
     * Configure backend URL
     */
    fun configure(url: String) {
        backendUrl = url.removeSuffix("/")
        Log.i(TAG, "Configured backend URL: $backendUrl")
    }
    
    /**
     * Start polling AI backend
     */
    fun startPolling() {
        scope.launch {
            Log.i(TAG, "Started AI context polling")
            while (isActive) {
                try {
                    fetchLatestAI()
                    delay(UPDATE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop: ${e.message}")
                    _isConnected.value = false
                    delay(5000) // Wait 5s before retry on error
                }
            }
        }
    }
    
    /**
     * Fetch latest AI state from backend
     */
    private suspend fun fetchLatestAI() {
        try {
            val request = Request.Builder()
                .url("$backendUrl/latest-ai")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return
                val aiResponse = json.decodeFromString<AIResponse>(body)
                
                // Update state
                _aiState.value = AIState(
                    assetDecisions = aiResponse.final_decision.map { it.toAssetDecision() },
                    marketRegime = extractMarketRegime(aiResponse.final_decision),
                    lastUpdated = System.currentTimeMillis()
                )
                
                _isConnected.value = true
                _lastUpdate.value = System.currentTimeMillis()
                
                Log.d(TAG, "Updated AI state: ${aiResponse.final_decision.size} assets")
            } else {
                Log.w(TAG, "AI backend returned ${response.code}")
                _isConnected.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch AI state: ${e.message}")
            _isConnected.value = false
        }
    }
    
    /**
     * Get AI decision for specific asset
     */
    fun getAssetDecision(symbol: String): AssetDecision? {
        val normalized = symbol.uppercase().replace("/", "").replace("-", "")
        return _aiState.value.assetDecisions.find { 
            it.symbol.uppercase().replace("/", "").replace("-", "") == normalized 
        }
    }
    
    /**
     * Get all high-priority assets
     */
    fun getHighPriorityAssets(): List<AssetDecision> {
        return _aiState.value.assetDecisions
            .filter { it.priority == "HIGH" || it.score >= 0.7 }
            .sortedByDescending { it.score }
    }
    
    /**
     * Get news impact for symbol
     */
    fun getNewsImpact(symbol: String): NewsImpact? {
        // TODO: Implement when news analysis is added to AI backend
        return null
    }
    
    private fun extractMarketRegime(decisions: List<AIDecision>): MarketRegime {
        if (decisions.isEmpty()) return MarketRegime()
        
        // Aggregate regime data from all assets
        val regimes = decisions.mapNotNull { it.regime_state }
        val trends = decisions.mapNotNull { it.trend_state }
        
        return MarketRegime(
            dominantRegime = regimes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "UNKNOWN",
            dominantTrend = trends.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "UNKNOWN",
            volatilityLevel = decisions.mapNotNull { it.feeder_volatility_score }.average().takeIf { !it.isNaN() } ?: 0.0
        )
    }
}

/**
 * Complete AI state for the entire app
 */
@Serializable
data class AIState(
    val assetDecisions: List<AssetDecision> = emptyList(),
    val marketRegime: MarketRegime = MarketRegime(),
    val lastUpdated: Long = 0L
)

/**
 * AI decision for a single asset
 */
@Serializable
data class AssetDecision(
    val symbol: String,
    val direction: String,
    val label: String,
    val priority: String,
    val score: Double,
    val ignitionProbability: Double,
    val expansionProbability: Double,
    val confluenceScore: Double,
    val entryWindow: String,
    val exitPlan: String,
    val reason: String,
    val regimeState: String? = null,
    val trendState: String? = null,
    val volatilityScore: Double? = null,
    val preMovePhase: String? = null,
    val preMoveScore: Double? = null
)

/**
 * Market regime analysis
 */
@Serializable
data class MarketRegime(
    val dominantRegime: String = "UNKNOWN",
    val dominantTrend: String = "UNKNOWN",
    val volatilityLevel: Double = 0.0
)

/**
 * News impact on specific asset
 */
@Serializable
data class NewsImpact(
    val symbol: String,
    val headline: String,
    val impact: String, // HIGH, MEDIUM, LOW
    val sentiment: String, // BULLISH, BEARISH, NEUTRAL
    val score: Double,
    val timestamp: Long
)

/**
 * Response from AI backend /latest-ai endpoint
 */
@Serializable
data class AIResponse(
    val success: Boolean,
    val final_decision: List<AIDecision>
)

/**
 * Individual AI decision from backend
 */
@Serializable
data class AIDecision(
    val asset_1: String? = null,
    val journal_direction: String? = null,
    val journal_label: String? = null,
    val journal_priority: String? = null,
    val journal_score: Double? = null,
    val ignition_probability: Double? = null,
    val expansion_probability: Double? = null,
    val confluence_score: Double? = null,
    val entry_window: String? = null,
    val exit_plan: String? = null,
    val portfolio_decision_reason: String? = null,
    val regime_state: String? = null,
    val trend_state: String? = null,
    val feeder_volatility_score: Double? = null,
    val pre_move_ai_phase: String? = null,
    val pre_move_ai_score: Double? = null
) {
    fun toAssetDecision() = AssetDecision(
        symbol = asset_1 ?: "UNKNOWN",
        direction = journal_direction ?: "NONE",
        label = journal_label ?: "UNKNOWN",
        priority = journal_priority ?: "LOW",
        score = journal_score ?: 0.0,
        ignitionProbability = ignition_probability ?: 0.0,
        expansionProbability = expansion_probability ?: 0.0,
        confluenceScore = confluence_score ?: 0.0,
        entryWindow = entry_window ?: "WAIT",
        exitPlan = exit_plan ?: "STANDARD INVALIDATION",
        reason = portfolio_decision_reason ?: "",
        regimeState = regime_state,
        trendState = trend_state,
        volatilityScore = feeder_volatility_score,
        preMovePhase = pre_move_ai_phase,
        preMoveScore = pre_move_ai_score
    )
}
