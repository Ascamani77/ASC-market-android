package com.asc.markets.data

import com.asc.markets.data.trade.TradeEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun PostMoveAuditSource.label(): String {
    return when (this) {
        PostMoveAuditSource.CLOSED_TRADE -> "CLOSED TRADE"
        PostMoveAuditSource.AI_DECISION -> "AI OUTCOME"
    }
}

enum class PostMoveAuditSource {
    CLOSED_TRADE,
    AI_DECISION
}

data class PostMoveAuditCase(
    val id: String,
    val originAuditRecordId: String? = null,
    val source: PostMoveAuditSource,
    val symbol: String,
    val direction: String,
    val status: String,
    val timestamp: Long,
    val entryPrice: Double? = null,
    val exitPrice: Double? = null,
    val pnl: Double? = null,
    val win: Boolean? = null,
    val thesis: String,
    val postMoveOutcome: String,
    val confidence: Int? = null,
    val riskPct: Double? = null,
    val deploymentLabel: String? = null,
    val preMoveScoreAtEntry: Int? = null,
    val compressionScoreAtEntry: Int? = null,
    val ignitionScoreAtEntry: Int? = null,
    val liquidityTarget: String? = null,
    val targetHit: Boolean? = null,
    val invalidationHit: Boolean? = null,
    val actualMovePct: Double? = null,
    val maxFavorableExcursionPct: Double? = null,
    val maxAdverseExcursionPct: Double? = null,
    val timeToTargetMs: Long? = null,
    val slippagePips: Double? = null,
    val modelAccuracyScore: Int? = null,
    val failureReason: String? = null,
    val nodeId: String = "LOCAL",
    val integrityHash: String = "",
    val reviewed: Boolean = false,
    val reconstructionLines: List<String> = emptyList()
)

object PostMoveAuditStore {
    fun buildCases(
        trades: List<TradeEntity>,
        auditRecords: List<AuditRecord>,
        candidates: List<PreMoveCandidate>,
        timedHistory: Map<String, List<TimedPrice>>
    ): List<PostMoveAuditCase> {
        val tradeCases = trades.map { trade -> buildTradeCase(trade, candidates, timedHistory) }
        val auditCases = auditRecords.map { record -> buildAuditCase(record, candidates, timedHistory) }
        return (tradeCases + auditCases).sortedByDescending { it.timestamp }
    }

    fun buildTradeCase(
        trade: TradeEntity,
        candidates: List<PreMoveCandidate>,
        timedHistory: Map<String, List<TimedPrice>>
    ): PostMoveAuditCase {
        val candidate = candidateFor(trade.asset, candidates)
        val direction = normalizeDirection(trade.direction)
        val multiplier = directionMultiplier(direction)
        val actualMovePct = directionalMovePercent(trade.entryPrice, trade.exitPrice, multiplier)
        val postWindow = historyFor(trade.asset, timedHistory).filter { it.timestampMillis >= trade.timestamp }
        val mfe = excursionPercent(trade.entryPrice, postWindow, multiplier, favorable = true)
        val mae = excursionPercent(trade.entryPrice, postWindow, multiplier, favorable = false)
        val outcome = if (trade.win) {
            "Target-side outcome confirmed with realized PnL ${formatSigned(trade.pnl)}."
        } else {
            "Setup closed negative with realized PnL ${formatSigned(trade.pnl)}."
        }
        val lines = listOf(
            "Regime stack at execution: ${trade.regimeStack}",
            "Directional result: ${actualMovePct?.let { formatPercent(it) } ?: "not available"}",
            "Entry volatility: ${formatPercent(trade.entryVolatility)}",
            "Entry correlation: ${String.format(Locale.US, "%.2f", trade.entryCorrelation)}"
        )
        return PostMoveAuditCase(
            id = "trade-${trade.id}",
            source = PostMoveAuditSource.CLOSED_TRADE,
            symbol = trade.asset,
            direction = direction,
            status = if (trade.win) "TARGET HIT" else "INVALIDATED",
            timestamp = trade.timestamp,
            entryPrice = trade.entryPrice,
            exitPrice = trade.exitPrice,
            pnl = trade.pnl,
            win = trade.win,
            thesis = "Captured closed trade under ${trade.regimeStack} with ${direction.lowercase(Locale.US)} directional intent.",
            postMoveOutcome = outcome,
            preMoveScoreAtEntry = candidate?.preMoveScore,
            compressionScoreAtEntry = candidate?.compressionScore,
            ignitionScoreAtEntry = candidate?.ignitionScore,
            liquidityTarget = candidate?.liquidityMagnet,
            targetHit = trade.win,
            invalidationHit = !trade.win,
            actualMovePct = actualMovePct,
            maxFavorableExcursionPct = mfe,
            maxAdverseExcursionPct = mae,
            modelAccuracyScore = if (trade.win) 100 else 0,
            failureReason = if (trade.win) null else "Closed below model expectation under captured regime stack.",
            nodeId = "TRADE_HISTORY",
            reviewed = false,
            reconstructionLines = lines
        )
    }

    fun buildAuditCase(
        record: AuditRecord,
        candidates: List<PreMoveCandidate>,
        timedHistory: Map<String, List<TimedPrice>>
    ): PostMoveAuditCase {
        val candidate = candidateFor(record.assets, candidates)
        val direction = normalizeDirection(record.direction ?: candidate?.directionBias ?: "UNSPECIFIED")
        val multiplier = directionMultiplier(direction)
        val history = historyFor(record.assets, timedHistory).filter { it.timestampMillis >= record.timeUtc }
        val entry = history.firstOrNull()?.price
        val exit = history.lastOrNull()?.price
        val actualMovePct = if (entry != null && exit != null && direction != "UNSPECIFIED") directionalMovePercent(entry, exit, multiplier) else null
        val targetHit = actualMovePct?.let { it > 0.0 }
        val status = when (targetHit) {
            true -> "MOVE CONFIRMED"
            false -> "THESIS FAILED"
            null -> "UNRESOLVED"
        }
        val outcome = when (targetHit) {
            true -> "Post-signal movement aligned with the recorded ${direction.lowercase(Locale.US)} thesis."
            false -> "Post-signal movement moved against the recorded ${direction.lowercase(Locale.US)} thesis."
            null -> "No sufficient post-signal market path is available yet for outcome scoring."
        }
        val lines = listOf(
            "Recorded impact: ${record.impact}",
            "Decision bucket: ${record.deploymentLabel ?: "not captured"}",
            "Risk allocation: ${record.riskPct?.let { formatPercent(it) } ?: "not captured"}",
            "Integrity hash: ${record.integrityHash.ifBlank { "not captured" }}"
        )
        return PostMoveAuditCase(
            id = "audit-${record.id}",
            originAuditRecordId = record.id,
            source = PostMoveAuditSource.AI_DECISION,
            symbol = record.assets,
            direction = direction,
            status = status,
            timestamp = record.timeUtc,
            entryPrice = entry,
            exitPrice = exit,
            win = targetHit,
            thesis = record.reasoning.ifBlank { record.headline },
            postMoveOutcome = outcome,
            confidence = record.confidence,
            riskPct = record.riskPct,
            deploymentLabel = record.deploymentLabel,
            preMoveScoreAtEntry = candidate?.preMoveScore,
            compressionScoreAtEntry = candidate?.compressionScore,
            ignitionScoreAtEntry = candidate?.ignitionScore,
            liquidityTarget = candidate?.liquidityMagnet,
            targetHit = targetHit,
            invalidationHit = targetHit?.not(),
            actualMovePct = actualMovePct,
            maxFavorableExcursionPct = entry?.let { excursionPercent(it, history, multiplier, favorable = true) },
            maxAdverseExcursionPct = entry?.let { excursionPercent(it, history, multiplier, favorable = false) },
            modelAccuracyScore = actualMovePct?.let { accuracyScore(it) },
            failureReason = if (targetHit == false) "Post-signal path contradicted the recorded directional thesis." else null,
            nodeId = record.nodeId,
            integrityHash = record.integrityHash,
            reviewed = record.audited,
            reconstructionLines = lines
        )
    }

    fun averageSlippage(cases: List<PostMoveAuditCase>): Double? {
        val values = cases.mapNotNull { it.slippagePips }
        return if (values.isEmpty()) null else values.average()
    }

    fun outcomeEfficiency(cases: List<PostMoveAuditCase>): Int? {
        val scored = cases.mapNotNull { it.modelAccuracyScore }
        return if (scored.isEmpty()) null else scored.average().toInt().coerceIn(0, 100)
    }

    fun formatPrice(value: Double?): String {
        if (value == null) return "NOT CAPTURED"
        return when {
            value >= 1000.0 -> String.format(Locale.US, "%,.2f", value)
            value >= 1.0 -> String.format(Locale.US, "%.5f", value)
            else -> String.format(Locale.US, "%.6f", value)
        }
    }

    fun formatPercent(value: Double?): String {
        return value?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "NOT CAPTURED"
    }

    fun formatSigned(value: Double?): String {
        return value?.let { String.format(Locale.US, "%+,.2f", it) } ?: "NOT CAPTURED"
    }

    private fun candidateFor(symbol: String, candidates: List<PreMoveCandidate>): PreMoveCandidate? {
        return candidates.firstOrNull { MarketDataStore.matchesSymbol(it.symbol, symbol) }
    }

    private fun historyFor(symbol: String, timedHistory: Map<String, List<TimedPrice>>): List<TimedPrice> {
        return timedHistory.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, symbol) }?.value.orEmpty()
    }

    private fun normalizeDirection(value: String): String {
        val upper = value.uppercase(Locale.US)
        return when {
            upper.contains("BUY") || upper.contains("LONG") || upper.contains("BULL") || upper.contains("UP") -> "LONG"
            upper.contains("SELL") || upper.contains("SHORT") || upper.contains("BEAR") || upper.contains("DOWN") -> "SHORT"
            else -> "UNSPECIFIED"
        }
    }

    private fun directionMultiplier(direction: String): Int {
        return if (direction == "SHORT") -1 else 1
    }

    private fun directionalMovePercent(entry: Double, exit: Double, multiplier: Int): Double? {
        if (!entry.isFinite() || !exit.isFinite() || entry <= 0.0) return null
        return ((exit - entry) / entry) * 100.0 * multiplier
    }

    private fun excursionPercent(entry: Double, points: List<TimedPrice>, multiplier: Int, favorable: Boolean): Double? {
        if (entry <= 0.0 || points.isEmpty()) return null
        val moves = points.mapNotNull { directionalMovePercent(entry, it.price, multiplier) }
        if (moves.isEmpty()) return null
        return if (favorable) max(0.0, moves.maxOrNull() ?: 0.0) else min(0.0, moves.minOrNull() ?: 0.0)
    }

    private fun accuracyScore(actualMovePct: Double): Int {
        val magnitude = (abs(actualMovePct) * 20.0).toInt().coerceAtMost(50)
        return if (actualMovePct >= 0.0) 50 + magnitude else 50 - magnitude
    }
}
