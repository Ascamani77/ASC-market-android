package com.asc.markets.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.math.abs

data class VigilanceNode(
    val id: String,
    val pair: String,
    val alertType: String,  // "SIMPLE" or "SMART"
    val trigger: String,     // e.g., "PRICE_THRESHOLD", "RSI_LEVEL", "MA_CROSS", "LIQUIDITY_SWEEP"
    val timeframe: String,   // "M5", "M15", "H1", "H4", "D1"
    val confidenceScore: Int, // 0-100
    val strength: String,    // "STRONG", "MEDIUM", "EARLY_STRUCTURE"
    val isActive: Boolean = true,
    val cooldownMinutes: Int = 15, // 15-120 mins
    val lastTriggeredAt: Long? = null,
    val confirmations: List<String> = emptyList(),
    val environmentContext: String? = null,
    val riskFilters: List<String> = emptyList(),
    val description: String = "",
    // NEW FIELDS
    val direction: String = "BOTH", // "LONG", "SHORT", "BOTH"
    val priceLevel: Double? = null,   // For PRICE_THRESHOLD trigger
    val rsiPeriod: Int = 14,          // RSI period (typically 14)
    val rsiLevel: Int = 70,           // RSI overbought level (typically 70 for long, 30 for short)
    val maFastPeriod: Int = 9,        // Fast MA period
    val maSlowPeriod: Int = 21,       // Slow MA period
    val regimeFilter: String = "ANY", // "ANY", "TRENDING_BULL", "TRENDING_BEAR", "RANGING", "BREAKOUT"
    val volatilityFilter: String = "ANY", // "ANY", "EXPANDING", "COMPRESSED", "DEAD"
    val confluenceThreshold: Int = 0  // Minimum confluence score (0-100), 0 = disabled
)

data class RejectedPattern(
    val pair: String,
    val pattern: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

object VigilanceNodeEngine {
    private val activeNodes = mutableListOf<VigilanceNode>()
    private val rejectedPatterns = mutableListOf<RejectedPattern>()
    private val _activeNodeCount = MutableStateFlow(0)
    val activeNodeCount: StateFlow<Int> = _activeNodeCount.asStateFlow()
    
    /**
     * Create a Simple Alert (Deterministic) with full configuration
     */
    fun createSimpleAlert(
        pair: String,
        trigger: String,  // PRICE_THRESHOLD, RSI_LEVEL, MA_CROSS
        timeframe: String,
        direction: String = "BOTH",
        priceLevel: Double? = null,
        rsiPeriod: Int = 14,
        rsiLevel: Int = 70,
        maFastPeriod: Int = 9,
        maSlowPeriod: Int = 21,
        cooldownMinutes: Int = 15
    ): VigilanceNode {
        val id = UUID.randomUUID().toString()
        val baseScore = when (trigger) {
            "PRICE_THRESHOLD" -> if (priceLevel != null) 45 else 35
            "RSI_LEVEL" -> 45
            "MA_CROSS" -> 50
            "TRENDLINE_BREAK" -> 40
            else -> 40
        }
        
        // Add direction bonus
        val directionScore = when (direction) {
            "LONG", "SHORT" -> 5
            else -> 0
        }
        
        val finalScore = (baseScore + directionScore).coerceIn(0, 100)
        
        val description = buildString {
            append("$trigger")
            if (direction != "BOTH") append(" [$direction]")
            if (priceLevel != null) append(" @ $priceLevel")
            append(" on $pair at $timeframe")
        }
        
        val node = VigilanceNode(
            id = id,
            pair = pair,
            alertType = "SIMPLE",
            trigger = trigger,
            timeframe = timeframe,
            confidenceScore = finalScore,
            strength = if (finalScore >= 50) "MEDIUM" else "EARLY_STRUCTURE",
            description = description,
            direction = direction,
            priceLevel = priceLevel,
            rsiPeriod = rsiPeriod,
            rsiLevel = rsiLevel,
            maFastPeriod = maFastPeriod,
            maSlowPeriod = maSlowPeriod,
            cooldownMinutes = cooldownMinutes
        )
        
        activeNodes.add(node)
        _activeNodeCount.value = activeNodes.size
        return node
    }
    
    /**
     * Create a Smart Alert (5-Step Algorithmic Pipeline) with regime and volatility filters
     */
    fun createSmartAlert(
        pair: String,
        primaryEvent: String,        // e.g., "CHANGE_OF_CHARACTER", "LIQUIDITY_SWEEP"
        confirmations: List<String>, // e.g., ["ENGULFING_CANDLE", "MA_SLOPE"]
        environmentContext: String,  // "HTF_ALIGNMENT", "LONDON_SESSION", etc.
        riskFilters: List<String>,  // e.g., ["NO_NEWS_BLOCKS", "HIGH_LIQUIDITY"]
        direction: String = "BOTH",
        regimeFilter: String = "ANY",
        volatilityFilter: String = "ANY",
        confluenceThreshold: Int = 0,
        cooldownMinutes: Int = 30
    ): VigilanceNode {
        val id = UUID.randomUUID().toString()
        
        // Base score from primary event
        var score = when (primaryEvent) {
            "CHANGE_OF_CHARACTER" -> 50
            "LIQUIDITY_SWEEP" -> 55
            "BREAKOUT_STRUCTURE" -> 45
            else -> 40
        }
        
        // Add weight for each confirmation
        score += confirmations.size * 12  // +12 per confirmation
        
        // Environment context bonus
        score += if (environmentContext.isNotEmpty()) 8 else 0
        
        // Direction bonus
        if (direction != "BOTH") score += 5
        
        // Regime filter bonus (specific regimes add confidence)
        if (regimeFilter != "ANY") score += 3
        
        // Volatility filter bonus
        if (volatilityFilter == "EXPANDING") score += 5
        
        // Risk filter impact (can reduce score if violated, but in creation we assume filters pass)
        score = score.coerceIn(0, 100)
        
        val strength = when {
            score >= 75 -> "STRONG"
            score >= 50 -> "MEDIUM"
            else -> "EARLY_STRUCTURE"
        }
        
        val description = buildString {
            append("$primaryEvent")
            if (direction != "BOTH") append(" [$direction]")
            if (confirmations.isNotEmpty()) append(" + ${confirmations.size} confirmations")
            if (environmentContext.isNotEmpty()) append(" | $environmentContext")
            if (regimeFilter != "ANY") append(" | Regime: $regimeFilter")
            if (volatilityFilter != "ANY") append(" | Vol: $volatilityFilter")
        }
        
        val node = VigilanceNode(
            id = id,
            pair = pair,
            alertType = "SMART",
            trigger = primaryEvent,
            timeframe = "H1",
            confidenceScore = score,
            strength = strength,
            confirmations = confirmations,
            environmentContext = environmentContext,
            riskFilters = riskFilters,
            description = description,
            direction = direction,
            regimeFilter = regimeFilter,
            volatilityFilter = volatilityFilter,
            confluenceThreshold = confluenceThreshold,
            cooldownMinutes = cooldownMinutes
        )
        
        activeNodes.add(node)
        _activeNodeCount.value = activeNodes.size
        return node
    }
    
    /**
     * Apply chart drawing as a trigger (Trendline or Horizontal)
     */
    fun applyChartDrawing(
        pair: String,
        drawingType: String,  // "TRENDLINE" or "HORIZONTAL"
        x1: Float,
        y1: Float,
        x2: Float? = null,
        y2: Float? = null,
        triggerOnTouch: Boolean = false
    ): VigilanceNode {
        val id = UUID.randomUUID().toString()
        val baseScore = if (triggerOnTouch) 40 else 55
        
        val node = VigilanceNode(
            id = id,
            pair = pair,
            alertType = "SIMPLE",
            trigger = "CHART_DRAWING_${drawingType.uppercase()}",
            timeframe = "M15",
            confidenceScore = baseScore,
            strength = "MEDIUM",
            description = "$drawingType drawn on $pair chart (${if (triggerOnTouch) "Touch" else "Close Beyond"})"
        )
        
        activeNodes.add(node)
        _activeNodeCount.value = activeNodes.size
        return node
    }
    
    /**
     * Check if a node can trigger (respects cooldown)
     */
    fun canTriggerNode(nodeId: String): Boolean {
        val node = activeNodes.find { it.id == nodeId } ?: return false
        if (!node.isActive) return false
        
        node.lastTriggeredAt?.let {
            val cooldownMs = node.cooldownMinutes * 60 * 1000L
            val timeSinceLastTrigger = System.currentTimeMillis() - it
            return timeSinceLastTrigger >= cooldownMs
        }
        
        return true
    }
    
    /**
     * Log a rejected pattern (alert that almost triggered but was blocked by risk filters)
     */
    fun logRejectedPattern(pair: String, pattern: String, reason: String) {
        val rejected = RejectedPattern(pair, pattern, reason)
        rejectedPatterns.add(rejected)
        // Keep only last 20 rejected patterns
        if (rejectedPatterns.size > 20) {
            rejectedPatterns.removeAt(0)
        }
    }
    
    /**
     * Get scoring breakdown for a node (shows which factors contribute to score)
     */
    fun getScoringBreakdown(nodeId: String): Map<String, Int> {
        val node = activeNodes.find { it.id == nodeId } ?: return emptyMap()
        
        return when {
            node.alertType == "SMART" -> {
                val baseScore = when (node.trigger) {
                    "CHANGE_OF_CHARACTER" -> 50
                    "LIQUIDITY_SWEEP" -> 55
                    "BREAKOUT_STRUCTURE" -> 45
                    else -> 40
                }
                mutableMapOf<String, Int>().apply {
                    put("Base Trigger", baseScore)
                    put("Confirmations (${node.confirmations.size}x)", (node.confirmations.size * 12).coerceAtMost(60))
                    put("Environment Context", if (node.environmentContext?.isNotEmpty() == true) 8 else 0)
                    put("Direction", if (node.direction != "BOTH") 5 else 0)
                    put("Regime Filter", if (node.regimeFilter != "ANY") 3 else 0)
                    put("Volatility Filter", if (node.volatilityFilter == "EXPANDING") 5 else 0)
                }
            }
            else -> mutableMapOf<String, Int>().apply {
                val baseScore = when (node.trigger) {
                    "PRICE_THRESHOLD" -> if (node.priceLevel != null) 45 else 35
                    "RSI_LEVEL" -> 45
                    "MA_CROSS" -> 50
                    "TRENDLINE_BREAK" -> 40
                    else -> 40
                }
                put("Trigger Rule", baseScore)
                put("Direction", if (node.direction != "BOTH") 5 else 0)
            }
        }
    }
    
    fun getActiveNodes(): List<VigilanceNode> = activeNodes
    fun getRejectedPatterns(): List<RejectedPattern> = rejectedPatterns.takeLast(5)
    fun getNodeCount(): Int = activeNodes.size
    fun clearNode(nodeId: String) {
        activeNodes.removeAll { it.id == nodeId }
        _activeNodeCount.value = activeNodes.size
    }

    /**
     * Convert active vigilance nodes into MacroEvent objects for ingestion into MacroStream.
     * This mapping intentionally ignores microstructure specifics and focuses on macro signals.
     */
    fun toMacroEvents(): List<com.asc.markets.data.MacroEvent> {
        return activeNodes.map { node ->
            val priority = when {
                node.confidenceScore >= 75 -> com.asc.markets.data.ImpactPriority.CRITICAL
                node.confidenceScore >= 50 -> com.asc.markets.data.ImpactPriority.HIGH
                else -> com.asc.markets.data.ImpactPriority.MEDIUM
            }

            com.asc.markets.data.MacroEvent(
                title = com.asc.markets.data.formatEventTitle(node.description.ifEmpty { node.trigger }),
                currency = node.pair,
                datetimeUtc = System.currentTimeMillis(),
                priority = priority,
                status = if (node.isActive) com.asc.markets.data.MacroEventStatus.UPCOMING else com.asc.markets.data.MacroEventStatus.CONFIRMED,
                source = "VigilanceNodeEngine",
                details = "${node.trigger} | ${node.strength} | conf=${node.confidenceScore}"
            )
        }
    }
}
