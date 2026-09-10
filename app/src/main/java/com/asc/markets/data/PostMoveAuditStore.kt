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
    val reconstructionLines: List<String> = emptyList(),
    val regimeStack: String = "",
    val volumeAtEntry: Double? = null,
    val spreadPips: Double? = null
)

data class AuditStats(
    val totalRecords: Int = 0,
    val winningRecords: Int = 0,
    val losingRecords: Int = 0,
    val winRate: Int = 0,
    val totalPnl: Double = 0.0,
    val avgPnl: Double = 0.0,
    val profitFactor: Double? = null,
    val expectancy: Double? = null,
    val avgRMultiple: Double? = null,
    val maxConsecutiveLosses: Int = 0,
    val maxConsecutiveWins: Int = 0,
    val avgSlippage: Double? = null,
    val efficiency: Int? = null,
    val avgTradeDurationMs: Long? = null,
    val bestTradePnl: Double? = null,
    val worstTradePnl: Double? = null,
    val regimeBreakdown: Map<String, RegimeStats> = emptyMap(),
    val sourceBreakdown: Map<String, Int> = emptyMap()
)

data class RegimeStats(
    val count: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRate: Int = 0,
    val totalPnl: Double = 0.0,
    val avgPnl: Double = 0.0
)

object PostMoveAuditStore {

    fun computeStats(cases: List<PostMoveAuditCase>): AuditStats {
        if (cases.isEmpty()) return AuditStats()

        val totalRecords = cases.size
        val wins = cases.count { it.targetHit == true }
        val losses = cases.count { it.invalidationHit == true }
        val winRate = if (totalRecords > 0) (wins.toDouble() / totalRecords * 100).toInt() else 0

        val pnls = cases.mapNotNull { it.pnl }
        val totalPnl = pnls.sum()
        val avgPnl = if (pnls.isNotEmpty()) pnls.average() else 0.0

        val grossWins = pnls.filter { it > 0.0 }.sum()
        val grossLosses = abs(pnls.filter { it < 0.0 }.sum())
        val profitFactor = if (grossLosses > 0.0) grossWins / grossLosses else if (grossWins > 0.0) Double.MAX_VALUE else null

        val expectancy = if (pnls.isNotEmpty()) avgPnl else null

        val rMultiples = cases.mapNotNull { case ->
            val entry = case.entryPrice ?: return@mapNotNull null
            val sl = case.maxAdverseExcursionPct?.let { entry * (1.0 + it / 100.0) } ?: return@mapNotNull null
            val risk = abs(entry - sl)
            if (risk <= 0.0 || case.pnl == null) null else case.pnl / risk
        }
        val avgRMultiple = if (rMultiples.isNotEmpty()) rMultiples.average() else null

        var maxConsecLosses = 0
        var maxConsecWins = 0
        var curLoss = 0
        var curWin = 0
        cases.sortedByDescending { it.timestamp }.forEach { case ->
            when {
                case.targetHit == true -> { curWin++; curLoss = 0; maxConsecWins = max(maxConsecWins, curWin) }
                case.invalidationHit == true -> { curLoss++; curWin = 0; maxConsecLosses = max(maxConsecLosses, curLoss) }
                else -> { curLoss = 0; curWin = 0 }
            }
        }

        val slippages = cases.mapNotNull { it.slippagePips }
        val avgSlippage = if (slippages.isNotEmpty()) slippages.average() else null

        val scores = cases.mapNotNull { it.modelAccuracyScore }
        val efficiency = if (scores.isNotEmpty()) {
            val scored = cases.mapNotNull { case ->
                case.actualMovePct?.let { accuracyScore(it) }
            }
            if (scored.isNotEmpty()) scored.average().toInt().coerceIn(0, 100) else null
        } else null

        val durations = cases.mapNotNull { it.timeToTargetMs }
        val avgDuration = if (durations.isNotEmpty()) durations.average()?.toLong() else null

        val bestPnl = pnls.maxOrNull()
        val worstPnl = pnls.minOrNull()

        val regimeBreakdown = cases.groupBy { it.regimeStack.ifBlank { "UNKNOWN" } }
            .mapValues { (_, regimeCases) ->
                val rWins = regimeCases.count { it.targetHit == true }
                val rLosses = regimeCases.count { it.invalidationHit == true }
                val rPnls = regimeCases.mapNotNull { it.pnl }
                RegimeStats(
                    count = regimeCases.size,
                    wins = rWins,
                    losses = rLosses,
                    winRate = if (regimeCases.isNotEmpty()) (rWins.toDouble() / regimeCases.size * 100).toInt() else 0,
                    totalPnl = rPnls.sum(),
                    avgPnl = if (rPnls.isNotEmpty()) rPnls.average() else 0.0
                )
            }

        val sourceBreakdown = cases.groupBy { it.source.label() }.mapValues { it.value.size }

        return AuditStats(
            totalRecords = totalRecords,
            winningRecords = wins,
            losingRecords = losses,
            winRate = winRate,
            totalPnl = totalPnl,
            avgPnl = avgPnl,
            profitFactor = profitFactor,
            expectancy = expectancy,
            avgRMultiple = avgRMultiple,
            maxConsecutiveLosses = maxConsecLosses,
            maxConsecutiveWins = maxConsecWins,
            avgSlippage = avgSlippage,
            efficiency = efficiency,
            avgTradeDurationMs = avgDuration,
            bestTradePnl = bestPnl,
            worstTradePnl = worstPnl,
            regimeBreakdown = regimeBreakdown,
            sourceBreakdown = sourceBreakdown
        )
    }

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
            reconstructionLines = lines,
            regimeStack = trade.regimeStack
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
            reconstructionLines = lines,
            regimeStack = record.deploymentLabel ?: ""
        )
    }

    fun averageSlippage(cases: List<PostMoveAuditCase>): Double? {
        val values = cases.mapNotNull { it.slippagePips }
        return if (values.isEmpty()) null else values.average()
    }

    fun outcomeEfficiency(cases: List<PostMoveAuditCase>): Int? {
        val scored = cases.mapNotNull { case ->
            case.actualMovePct?.let { accuracyScore(it) }
        }
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

    fun formatDuration(ms: Long?): String {
        if (ms == null || ms <= 0) return "N/A"
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun exportCsvHeader(): String {
        return "ID,Source,Symbol,Direction,Status,Entry,Exit,PnL,Win,Actual Move %,MFE %,MAE %,Regime,Confidence,Risk %,Duration (ms),Timestamp,Reviewed"
    }

    fun exportCsvRow(c: PostMoveAuditCase): String {
        return listOf(
            c.id,
            c.source.label(),
            c.symbol,
            c.direction,
            c.status,
            c.entryPrice?.let { String.format(Locale.US, "%.5f", it) } ?: "",
            c.exitPrice?.let { String.format(Locale.US, "%.5f", it) } ?: "",
            c.pnl?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            c.win?.toString() ?: "",
            c.actualMovePct?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            c.maxFavorableExcursionPct?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            c.maxAdverseExcursionPct?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            c.regimeStack.ifBlank { "UNKNOWN" },
            (c.modelAccuracyScore ?: c.confidence)?.toString() ?: "",
            c.riskPct?.let { String.format(Locale.US, "%.1f", it) } ?: "",
            c.timeToTargetMs?.toString() ?: "",
            c.timestamp.toString(),
            c.reviewed.toString()
        ).joinToString(",")
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
