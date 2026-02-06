package com.asc.markets.logic

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
    val description: String = ""
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
    
    /**
     * Create a Simple Alert (Deterministic)
     */
    fun createSimpleAlert(
        pair: String,
        trigger: String,  // PRICE_THRESHOLD, RSI_LEVEL, MA_CROSS
        timeframe: String,
        value: Double? = null
    ): VigilanceNode {
        val id = UUID.randomUUID().toString()
        val baseScore = when (trigger) {
            "PRICE_THRESHOLD" -> 35
            "RSI_LEVEL" -> 45
            "MA_CROSS" -> 50
            "TRENDLINE_BREAK" -> 40
            else -> 40
        }
        
        val node = VigilanceNode(
            id = id,
            pair = pair,
            alertType = "SIMPLE",
            trigger = trigger,
            timeframe = timeframe,
            confidenceScore = baseScore,
            strength = "EARLY_STRUCTURE",
            description = "$trigger on $pair at $timeframe"
        )
        
        activeNodes.add(node)
        return node
    }
    
    /**
     * Create a Smart Alert (5-Step Algorithmic Pipeline)
     */
    fun createSmartAlert(
        pair: String,
        primaryEvent: String,        // e.g., "CHANGE_OF_CHARACTER", "LIQUIDITY_SWEEP"
        confirmations: List<String>, // e.g., ["ENGULFING_CANDLE", "MA_SLOPE"]
        environmentContext: String,  // "HTF_ALIGNMENT", "LONDON_SESSION", etc.
        riskFilters: List<String>    // e.g., ["NO_NEWS_BLOCKS", "HIGH_LIQUIDITY"]
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
        
        // Risk filter impact (can reduce score if violated, but in creation we assume filters pass)
        score = score.coerceIn(0, 100)
        
        val strength = when {
            score >= 75 -> "STRONG"
            score >= 50 -> "MEDIUM"
            else -> "EARLY_STRUCTURE"
        }
        
        val description = buildString {
            append("$primaryEvent")
            if (confirmations.isNotEmpty()) append(" + ${confirmations.size} confirmations")
            if (environmentContext.isNotEmpty()) append(" | $environmentContext")
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
            description = description
        )
        
        activeNodes.add(node)
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
            node.alertType == "SMART" -> mapOf(
                "Base Trigger" to 40,
                "Confirmations" to (node.confirmations.size * 12).coerceAtMost(60),
                "Environment Context" to if (node.environmentContext?.isNotEmpty() == true) 8 else 0
            )
            else -> mapOf(
                "Trigger Rule" to node.confidenceScore
            )
        }
    }
    
    fun getActiveNodes(): List<VigilanceNode> = activeNodes
    fun getRejectedPatterns(): List<RejectedPattern> = rejectedPatterns.takeLast(5)
    fun getNodeCount(): Int = activeNodes.size
    fun clearNode(nodeId: String) {
        activeNodes.removeAll { it.id == nodeId }
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
                title = node.description.ifEmpty { node.trigger },
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
