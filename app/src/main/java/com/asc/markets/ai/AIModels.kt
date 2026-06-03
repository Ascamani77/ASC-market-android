package com.asc.markets.ai

import com.asc.markets.data.buildAiPageAccessPermissions
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Represents an AI decision for a specific asset.
 * Data comes from the AI backend's /latest-ai endpoint.
 */
data class AIDecision(
    val asset: String,                    // e.g., "EURUSD", "BTCUSDT"
    val direction: String,                // "LONG", "SHORT", "NEUTRAL"
    val confidence: Double,               // 0.0 to 1.0
    val score: Int,                       // 0 to 100 (journal_score)
    val preMoveScore: Double,             // 0.0 to 1.0 (pre_move_ai_score from backend)
    val reason: String,                   // Human-readable explanation
    val deploymentBucket: String,         // "HIGH", "MEDIUM", "LOW"
    val timestamp: Long                   // Unix timestamp
)

/**
 * Impact level for news events.
 */
enum class ImpactLevel {
    HIGH,    // Red - Major market-moving event
    MEDIUM,  // Yellow - Moderate impact
    LOW      // Gray - Minor/informational
}

/**
 * Represents a news item with AI-generated impact rating.
 * Data comes from the AI backend's /news-impact endpoint or enhanced /latest-ai.
 */
data class NewsImpact(
    val headline: String,
    val source: String,
    val timestamp: String,                // "8h ago", "Just now"
    val assetType: String,                // "forex", "crypto", "stocks", etc.
    val assetSymbol: String,              // "EUR/USD", "BTC/USDT"
    val impact: ImpactLevel,              // HIGH, MEDIUM, LOW
    val affectedAssets: List<String>,     // ["EURUSD", "GBPUSD"]
    val aiConfidence: Double,             // 0.0 to 1.0
    val imageUrl: String = ""
)

/**
 * Represents the configuration and access permissions for the AI system.
 */
data class AIPlatformContext(
    val accessPermissions: Map<String, Boolean>,
    val dataSources: Map<String, String>,
    val operationalRules: List<String>,
    val version: String = "1.0.0"
)

/**
 * Overall state of the AI Context Service.
 * Exposed via StateFlow for reactive updates.
 */
data class AIContextState(
    val decisions: Map<String, AIDecision>,     // Key: normalized asset symbol
    val newsImpacts: List<NewsImpact>,
    val platformContext: AIPlatformContext,
    val lastUpdated: Long,                      // Unix timestamp
    val isConnected: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        /**
         * Initial empty state when service starts.
         */
        fun initial() = AIContextState(
            decisions = emptyMap(),
            newsImpacts = emptyList(),
            platformContext = AIPlatformContext(
                accessPermissions = buildAiPageAccessPermissions(),
                dataSources = mapOf(
                    "Forex" to "Pepperstone",
                    "Commodities" to "Pepperstone",
                    "Indices" to "Pepperstone",
                    "Stocks" to "Pepperstone",
                    "Bonds" to "Pepperstone",
                    "USDT" to "Binance",
                    "Crypto (Non-USDT)" to "Pepperstone"
                ),
                operationalRules = listOf(
                    "Unrestricted navigation enabled for all platform pages.",
                    "Pepperstone is the exclusive data source for all asset classes.",
                    "USDT pairs are the sole exception, sourced via Binance.",
                    "AI Analysis follows Surveillance-First logic prioritizing pre-move timing."
                )
            ),
            lastUpdated = 0L,
            isConnected = false,
            errorMessage = null
        )
    }
}
