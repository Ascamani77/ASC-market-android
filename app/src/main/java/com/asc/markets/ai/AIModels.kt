package com.asc.markets.ai

/**
 * Data models for the AI Context Service.
 */

data class AIDecision(
    val asset: String,
    val direction: String,
    val confidence: Double,
    val score: Double,
    val reason: String,
    val ignitionProbability: Double,
    val expansionProbability: Double,
    val preMoveScore: Double? = null,
    val deploymentBucket: String = "MEDIUM",
    val timestamp: Long = System.currentTimeMillis()
)

enum class ImpactLevel {
    HIGH,    // Red - Major market-moving
    MEDIUM,  // Yellow - Moderate impact
    LOW      // Gray - Minor/informational
}

data class NewsImpact(
    val headline: String,
    val source: String,
    val timestamp: String,
    val assetType: String,
    val assetSymbol: String,
    val impact: ImpactLevel,
    val affectedAssets: List<String>,
    val aiConfidence: Double,
    val imageUrl: String = ""
)

data class PlatformContext(
    val accessPermissions: Map<String, Boolean> = emptyMap(),
    val dataSources: Map<String, String> = emptyMap(),
    val operationalRules: List<String> = emptyList()
)

data class AIContextState(
    val decisions: Map<String, AIDecision> = emptyMap(),
    val newsImpacts: List<NewsImpact> = emptyList(),
    val platformContext: PlatformContext = PlatformContext(),
    val lastUpdated: Long = 0L,
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)
