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
    val comparisonMode: String = "ABOVE",
    val phaseState: String = "ANY",
    val regimeFilter: String = "ANY", // "ANY", "TRENDING_BULL", "TRENDING_BEAR", "RANGING", "BREAKOUT"
    val volatilityFilter: String = "ANY", // "ANY", "EXPANDING", "COMPRESSED", "DEAD"
    val confluenceThreshold: Int = 0,  // Minimum confluence score (0-100), 0 = disabled
    // Zone alert fields
    val zoneType: String = "",         // "FVG", "SUPPLY", "DEMAND", "PREMIUM", "DISCOUNT", "ORDER_BLOCK", "OTE"
    val zoneTop: Double? = null,       // Upper boundary of the zone
    val zoneBottom: Double? = null,    // Lower boundary of the zone
    val zoneNotification: Boolean = true // Send system notification on trigger
)

data class RejectedPattern(
    val pair: String,
    val pattern: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TriggeredAlert(
    val id: String = UUID.randomUUID().toString(),
    val nodeId: String,
    val pair: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

object VigilanceNodeEngine {
    private val activeNodes = mutableListOf<VigilanceNode>()
    private val rejectedPatterns = mutableListOf<RejectedPattern>()
    private val _activeNodeCount = MutableStateFlow(0)
    val activeNodeCount: StateFlow<Int> = _activeNodeCount.asStateFlow()
    private val _triggeredAlerts = MutableStateFlow<List<TriggeredAlert>>(emptyList())
    val triggeredAlerts: StateFlow<List<TriggeredAlert>> = _triggeredAlerts.asStateFlow()
    private val _triggeredCount = MutableStateFlow(0)
    val triggeredCount: StateFlow<Int> = _triggeredCount.asStateFlow()
    
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
        comparisonMode: String = "ABOVE",
        phaseState: String = "ANY",
        volatilityState: String = "ANY",
        cooldownMinutes: Int = 15
    ): VigilanceNode {
        val id = UUID.randomUUID().toString()
        val baseScore = when (trigger) {
            "PRICE_THRESHOLD" -> if (priceLevel != null) 45 else 35
            "RSI_LEVEL" -> 45
            "MA_CROSS" -> 50
            "TRENDLINE_BREAK" -> 40
            "AI_LINE_CROSS_PRICE" -> 65
            "AI_LINE_TOUCH_PRICE" -> 60
            "VOLATILITY_SCORE" -> 58
            "AI_PROGRESSIVE_SCALE" -> 62
            "AI_PHASE_STATE" -> 55
            "VOLATILITY_STATE" -> 55
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
            if (priceLevel != null) append(" | $comparisonMode $priceLevel")
            if (phaseState != "ANY") append(" | Phase: $phaseState")
            if (volatilityState != "ANY") append(" | Volatility: $volatilityState")
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
            comparisonMode = comparisonMode,
            phaseState = phaseState,
            volatilityFilter = volatilityState,
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
                    "AI_LINE_CROSS_PRICE" -> 65
                    "AI_LINE_TOUCH_PRICE" -> 60
                    "VOLATILITY_SCORE" -> 58
                    "AI_PROGRESSIVE_SCALE" -> 62
                    "AI_PHASE_STATE" -> 55
                    "VOLATILITY_STATE" -> 55
                    else -> 40
                }
                put("Trigger Rule", baseScore)
                put("Direction", if (node.direction != "BOTH") 5 else 0)
            }
        }
    }
    
    /**
     * Create an EA-based alert that monitors real-time EA signals.
     * Fires when the EA's live signal meets the user's threshold conditions.
     */
    fun createEAAlert(
        pair: String,
        voteThreshold: Int,
        confidenceThreshold: Int,
        directionFilter: String,
        qualityTierFilter: String,
        requireFvg: Boolean,
        requireBos: Boolean,
        requireSweep: Boolean,
        requirePd: Boolean = false,
        pTradeThreshold: Int = 0,
        combineScoreThreshold: Int = 0,
        validatorFilter: String = "ANY", // "ANY", "LONG", "SHORT", "INACTIVE"
        eaSide: String = "ANY", // "ANY", "LONG", "SHORT" — EA score applies to this side only
        aiSide: String = "ANY", // "ANY", "LONG", "SHORT" — AI score applies to this side only
        cooldownMinutes: Int
    ): VigilanceNode {
        val id = UUID.randomUUID().toString()

        val conditions = mutableListOf<String>()
        var score = 0

        if (voteThreshold > 0) { conditions.add(if (eaSide != "ANY") "EA Score $eaSide >= ${voteThreshold}%" else "EA Score >= ${voteThreshold}%"); score += 20 }
        if (confidenceThreshold > 0) { conditions.add(if (aiSide != "ANY") "AI Score $aiSide >= ${confidenceThreshold}%" else "AI Score >= ${confidenceThreshold}%"); score += 20 }
        if (directionFilter != "ANY") { conditions.add("Direction = $directionFilter"); score += 15 }
        // Normalize UI tier names to the EA's quality_tier values so evaluation can match.
        val normalizedTierFilter = when (qualityTierFilter.uppercase()) {
            "HIGH" -> "STRONG"
            "STANDARD" -> "VALID"
            else -> qualityTierFilter
        }
        if (qualityTierFilter != "ANY") { conditions.add("Tier = $normalizedTierFilter"); score += 15 }
        if (requireFvg) { conditions.add("FVG required"); score += 10 }
        if (requireBos) { conditions.add("BOS required"); score += 10 }
        if (requireSweep) { conditions.add("Sweep required"); score += 10 }
        if (requirePd) { conditions.add("PD required"); score += 10 }
        if (pTradeThreshold > 0) { conditions.add("P_TRADE >= ${pTradeThreshold}%"); score += 15 }
        if (combineScoreThreshold > 0) { conditions.add("COMBINE >= ${combineScoreThreshold}%"); score += 15 }
        // Validator state gate: LONG / SHORT / INACTIVE.
        if (validatorFilter != "ANY") { conditions.add("Validator $validatorFilter"); score += 10 }

        score = score.coerceIn(0, 100)

        val strength = when {
            score >= 70 -> "STRONG"
            score >= 45 -> "MEDIUM"
            else -> "EARLY_STRUCTURE"
        }

        val description = buildString {
            append("EA Monitor: ")
            append(conditions.joinToString(" + "))
        }

        val node = VigilanceNode(
            id = id,
            pair = pair,
            alertType = "EA_LIVE",
            trigger = "EA_SIGNAL_MONITOR",
            timeframe = "LIVE",
            confidenceScore = score,
            strength = strength,
            description = description,
            direction = directionFilter,
            cooldownMinutes = cooldownMinutes
        )

        activeNodes.add(node)
        _activeNodeCount.value = activeNodes.size
        return node
    }

    /**
     * Evaluate a live EA signal against an EA_LIVE node's thresholds.
     * Returns true if the signal matches the node's conditions.
     */
    fun evaluateEANode(nodeId: String, votePct: Double, confidence: Double, direction: String, qualityTier: String, fvgBull: Boolean, fvgBear: Boolean, bosBull: Boolean, bosBear: Boolean, sweepHigh: Boolean, sweepLow: Boolean, pdActive: Boolean = false, pTradePct: Double = -1.0, pwinPct: Double = -1.0, validatorActive: Boolean = false, validatorAllowed: Boolean = false, validatorDirection: String = ""): Boolean {
        if (!canTriggerNode(nodeId)) return false
        val checks = checkEANode(nodeId, votePct, confidence, direction, qualityTier, fvgBull, fvgBear, bosBull, bosBear, sweepHigh, sweepLow, pdActive, pTradePct, pwinPct, validatorActive, validatorAllowed, validatorDirection)
        if (checks.isEmpty()) return false
        return checks.all { it.passed }
    }

    data class ConditionCheck(val label: String, val passed: Boolean)

    /**
     * Per-condition breakdown of an EA_LIVE node against live values (no cooldown
     * gate) so the UI can show exactly which conditions pass/fail right now.
     */
    fun checkEANode(nodeId: String, votePct: Double, confidence: Double, direction: String, qualityTier: String, fvgBull: Boolean, fvgBear: Boolean, bosBull: Boolean, bosBear: Boolean, sweepHigh: Boolean, sweepLow: Boolean, pdActive: Boolean = false, pTradePct: Double = -1.0, pwinPct: Double = -1.0, validatorActive: Boolean = false, validatorAllowed: Boolean = false, validatorDirection: String = ""): List<ConditionCheck> {
        val node = activeNodes.find { it.id == nodeId } ?: return emptyList()
        if (node.alertType != "EA_LIVE") return emptyList()
        val out = mutableListOf<ConditionCheck>()
        val desc = node.description

        if (node.direction != "ANY") {
            val ok = direction.equals(node.direction, ignoreCase = true)
            out.add(ConditionCheck("Direction $direction = ${node.direction}", ok))
        }
        if (desc.contains("EA Score >=")) {
            val m = Regex("EA Score (LONG|SHORT)?\\s*>=\\s*(\\d+)%").find(desc)
            val side = m?.groupValues?.get(1) ?: ""
            val thresh = m?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val sideOk = side.isEmpty() || direction.equals(side, ignoreCase = true)
            val sideLabel = if (side.isEmpty()) "" else "$side "
            out.add(ConditionCheck("EA Score $sideLabel${votePct.toInt()}% ≥ $thresh%", sideOk && votePct >= thresh))
        }
        if (desc.contains("AI Score >=")) {
            val m = Regex("AI Score (LONG|SHORT)?\\s*>=\\s*(\\d+)%").find(desc)
            val side = m?.groupValues?.get(1) ?: ""
            val thresh = m?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val sideOk = side.isEmpty() || direction.equals(side, ignoreCase = true)
            val sideLabel = if (side.isEmpty()) "" else "$side "
            val actual = (confidence * 100).toInt()
            out.add(ConditionCheck("AI Score $sideLabel$actual% ≥ $thresh%", sideOk && (confidence * 100) >= thresh))
        }
        if (desc.contains("Tier =")) {
            val tier = Regex("Tier = (\\w+)").find(desc)?.groupValues?.get(1) ?: "ANY"
            out.add(ConditionCheck("Tier ${qualityTier.ifBlank { "—" }} = $tier", qualityTier.equals(tier, ignoreCase = true)))
        }
        if (desc.contains("FVG required")) out.add(ConditionCheck("FVG present", fvgBull || fvgBear))
        if (desc.contains("BOS required")) out.add(ConditionCheck("BOS present", bosBull || bosBear))
        if (desc.contains("Sweep required")) out.add(ConditionCheck("Sweep present", sweepHigh || sweepLow))
        if (desc.contains("PD required")) out.add(ConditionCheck("Premium/Discount zone", pdActive))
        if (desc.contains("P_TRADE >=")) {
            val thresh = Regex("P_TRADE >= (\\d+)%").find(desc)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            out.add(ConditionCheck("P(Trade) ${pTradePct.toInt()}% ≥ $thresh%", pTradePct >= thresh))
        }
        if (desc.contains("COMBINE >=")) {
            val thresh = Regex("COMBINE >= (\\d+)%").find(desc)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val combined = ((votePct + (confidence * 100)) / 2.0).toInt()
            out.add(ConditionCheck("Combined ${combined}% ≥ $thresh%", combined >= thresh))
        }
        if (desc.contains("Validator LONG") || desc.contains("Validator SHORT") || desc.contains("Validator INACTIVE")) {
            val want = Regex("Validator (LONG|SHORT|INACTIVE)").find(desc)?.groupValues?.get(1) ?: "ANY"
            val vDir = validatorDirection.uppercase()
            val isActive = validatorActive && (vDir == "LONG" || vDir == "BUY" || vDir == "SHORT" || vDir == "SELL")
            val current = when {
                !isActive -> "INACTIVE"
                vDir == "LONG" || vDir == "BUY" -> "LONG"
                vDir == "SHORT" || vDir == "SELL" -> "SHORT"
                else -> "INACTIVE"
            }
            out.add(ConditionCheck("Validator $current = $want", current == want))
        }

        if (out.isEmpty()) out.add(ConditionCheck("Signal live", true))
        return out
    }

    fun getActiveNodes(): List<VigilanceNode> = activeNodes
    fun getRejectedPatterns(): List<RejectedPattern> = rejectedPatterns.takeLast(5)

    /**
     * Mark an EA node as triggered: starts its cooldown and bumps the fired count.
     * Called by the live monitor after [evaluateEANode] returns true.
     */
    fun markEANodeTriggered(nodeId: String): VigilanceNode? {
        val idx = activeNodes.indexOfFirst { it.id == nodeId }
        if (idx < 0) return null
        val updated = activeNodes[idx].copy(lastTriggeredAt = System.currentTimeMillis())
        activeNodes[idx] = updated
        _triggeredCount.value = _triggeredCount.value + 1
        return updated
    }

    fun recordTriggeredAlert(alert: TriggeredAlert) {
        _triggeredAlerts.value = listOf(alert) + _triggeredAlerts.value.take(49)
    }

    fun clearTriggeredAlerts() {
        _triggeredAlerts.value = emptyList()
    }    fun getNodeCount(): Int = activeNodes.size
    fun clearNode(nodeId: String) {
        activeNodes.removeAll { it.id == nodeId }
        _activeNodeCount.value = activeNodes.size
    }

    /**
     * Create a Zone Entry alert. Fires when price enters a specified zone.
     * Supported zoneTypes: "FVG", "SUPPLY", "DEMAND", "PREMIUM", "DISCOUNT", "ORDER_BLOCK", "OTE"
     */
    fun createZoneAlert(
        pair: String,
        zoneType: String,
        zoneTop: Double,
        zoneBottom: Double,
        timeframe: String = "H1",
        direction: String = "BOTH",
        cooldownMinutes: Int = 15,
        zoneNotification: Boolean = true
    ): VigilanceNode {
        val id = "zone_${UUID.randomUUID().toString().takeLast(8)}"
        val score = when (zoneType) {
            "FVG" -> 65
            "SUPPLY", "DEMAND" -> 60
            "ORDER_BLOCK" -> 70
            "PREMIUM", "DISCOUNT" -> 50
            "OTE" -> 68
            else -> 55
        }
        val node = VigilanceNode(
            id = id,
            pair = pair,
            alertType = "ZONE",
            trigger = "ZONE_TOUCH",
            timeframe = timeframe,
            confidenceScore = score,
            strength = "STRONG",
            isActive = true,
            cooldownMinutes = cooldownMinutes,
            direction = direction,
            description = "$zoneType zone touch on $pair (${String.format("%.5f", zoneBottom)}-${String.format("%.5f", zoneTop)})",
            zoneType = zoneType,
            zoneTop = zoneTop,
            zoneBottom = zoneBottom,
            zoneNotification = zoneNotification
        )
        activeNodes.add(node)
        _activeNodeCount.value = activeNodes.size
        return node
    }

    /**
     * Evaluate whether current price is inside a zone node's boundaries.
     * Returns the node if triggered, null otherwise.
     */
    fun evaluateZoneNode(nodeId: String, currentPrice: Double): VigilanceNode? {
        val node = activeNodes.find { it.id == nodeId } ?: return null
        if (node.alertType != "ZONE") return null
        if (!canTriggerNode(nodeId)) return null
        val top = node.zoneTop ?: return null
        val bottom = node.zoneBottom ?: return null
        if (currentPrice in bottom..top) {
            markTriggered(nodeId)
            return node
        }
        return null
    }

    /**
     * Evaluate all active zone nodes for a given pair against the current price.
     * Returns list of triggered nodes.
     */
    fun evaluateAllZoneNodes(pair: String, currentPrice: Double): List<VigilanceNode> {
        return activeNodes
            .filter { it.pair == pair && it.alertType == "ZONE" && it.isActive }
            .mapNotNull { evaluateZoneNode(it.id, currentPrice) }
    }

    private fun markTriggered(nodeId: String) {
        val idx = activeNodes.indexOfFirst { it.id == nodeId }
        if (idx >= 0) {
            activeNodes[idx] = activeNodes[idx].copy(lastTriggeredAt = System.currentTimeMillis())
        }
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
