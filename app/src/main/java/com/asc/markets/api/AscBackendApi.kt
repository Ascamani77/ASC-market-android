package com.asc.markets.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ASC Backend API Client - Clean Architecture
 * 
 * This client connects to the ASC Backend API which serves
 * AI-generated market signals from Redis.
 * 
 * Architecture:
 *     Android App → Backend API → Redis → AI Service
 *     
 * The app NEVER talks to the AI directly.
 * It only requests the latest market state from the backend.
 */

// ============================================================================
// Data Models
// ============================================================================

@Serializable
data class MarketSignal(
    val asset: String,
    val direction: String, // "BUY", "SELL", "NEUTRAL"
    val confidence: Double, // 0-100
    val ai_score: Double, // 0-100
    val trend: String, // "Bullish", "Bearish", "Sideways"
    val risk_level: String, // "Low", "Medium", "High"
    val setup_type: String, // "Order Block", "Liquidity Sweep", etc.
    val entry_price: Double? = null,
    val stop_loss: Double? = null,
    val take_profit: Double? = null,
    val risk_reward_ratio: Double? = null,
    val position_size: Double? = null,
    val last_updated: String,
    val phase: String, // "EXPANSION", "PRE-MOVE", etc.
    val volatility_state: String, // "EXPLOSIVE", "BURST", etc.
    val confluence_score: Double? = null,
    val structure_score: Double? = null,
    val entry_quality: Double? = null
)

@Serializable
data class MarketOverview(
    val total_signals: Int,
    val buy_signals: Int,
    val sell_signals: Int,
    val neutral_signals: Int,
    val high_confidence_count: Int,
    val market_condition: String, // "Trending", "Ranging", "Volatile"
    val last_updated: String
)

@Serializable
data class AssetAnalysis(
    val asset: String,
    val signal: MarketSignal,
    val regime: String,
    val volatility: Map<String, JsonElement> = emptyMap(),
    val structure: Map<String, JsonElement> = emptyMap(),
    val confluence: Map<String, JsonElement> = emptyMap(),
    val feeder_scores: Map<String, Double> = emptyMap(),
    val reasoning: String,
    val trade_plan: String? = null,
    val portfolio_allocation: Double? = null,
    val max_position_size: Double? = null,
    val recommended_risk_pct: Double
)

@Serializable
data class HealthCheck(
    val status: String,
    val redis_connected: Boolean,
    val ai_system_active: Boolean,
    val last_ai_update: String? = null,
    val active_feeders: Int,
    val timestamp: String
)

// ============================================================================
// API Client
// ============================================================================

class AscBackendApi(
    private val baseUrl: String = "http://localhost:8001"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Check if the backend API is healthy and AI system is active
     */
    suspend fun checkHealth(): Result<HealthCheck> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        Exception("Empty response")
                    )
                    val health = json.decodeFromString<HealthCheck>(body)
                    Result.success(health)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get overall market overview
     */
    suspend fun getMarketOverview(): Result<MarketOverview> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/market/overview")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        Exception("Empty response")
                    )
                    val overview = json.decodeFromString<MarketOverview>(body)
                    Result.success(overview)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all market signals with optional filters
     * 
     * @param minConfidence Minimum confidence level (0-100)
     * @param direction Filter by direction ("BUY", "SELL", "NEUTRAL")
     * @param phase Filter by phase ("EXPANSION", "PRE-MOVE", etc.)
     */
    suspend fun getAllSignals(
        minConfidence: Double = 0.0,
        direction: String? = null,
        phase: String? = null
    ): Result<List<MarketSignal>> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder("$baseUrl/market/signals?")
            urlBuilder.append("min_confidence=$minConfidence")
            direction?.let { urlBuilder.append("&direction=$it") }
            phase?.let { urlBuilder.append("&phase=$it") }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        Exception("Empty response")
                    )
                    val signals = json.decodeFromString<List<MarketSignal>>(body)
                    Result.success(signals)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get signal for a specific asset
     * 
     * @param asset Asset symbol (e.g., "EURUSD", "BTCUSDT")
     */
    suspend fun getAssetSignal(asset: String): Result<MarketSignal> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/market/$asset")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        Exception("Empty response")
                    )
                    val signal = json.decodeFromString<MarketSignal>(body)
                    Result.success(signal)
                } else if (response.code == 404) {
                    Result.failure(Exception("Asset $asset not found"))
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get detailed analysis for a specific asset
     * 
     * @param asset Asset symbol (e.g., "EURUSD", "BTCUSDT")
     */
    suspend fun getAssetAnalysis(asset: String): Result<AssetAnalysis> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/market/$asset/analysis")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        Exception("Empty response")
                    )
                    val analysis = json.decodeFromString<AssetAnalysis>(body)
                    Result.success(analysis)
                } else if (response.code == 404) {
                    Result.failure(Exception("Asset $asset not found"))
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scanner - Get high-probability trading opportunities
     * 
     * @param minConfidence Minimum confidence (default 70)
     * @param minAiScore Minimum AI score (default 60)
     * @param phase Comma-separated phases (default: "EXPANSION,PRE-MOVE")
     */
    suspend fun getTradingOpportunities(
        minConfidence: Double = 70.0,
        minAiScore: Double = 60.0,
        phase: String = "EXPANSION,PRE-MOVE"
    ): Result<List<MarketSignal>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/market/scanner/opportunities?" +
                    "min_confidence=$minConfidence&" +
                    "min_ai_score=$minAiScore&" +
                    "phase=$phase"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext Result.failure(
                        Exception("Empty response")
                    )
                    val opportunities = json.decodeFromString<List<MarketSignal>>(body)
                    Result.success(opportunities)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * Create an API client instance
         * 
         * For local development: http://YOUR_PC_IP:8001
         * For production: https://your-domain.com
         */
        fun create(baseUrl: String = com.asc.markets.data.NetworkConfig.DEFAULT_BACKEND_URL): AscBackendApi {
            return AscBackendApi(baseUrl)
        }
    }
}
