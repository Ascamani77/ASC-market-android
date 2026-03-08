package com.asc.markets.risk

import kotlin.math.max

data class TradeResult(
    val asset: String,
    val pnl: Double,
    val win: Boolean,
    val regimeStack: String,
    val timestamp: Long
)

data class SurfaceStats(
    val winRate: Double,
    val volatility: Double,
    val tail05: Double,
    val regimeFrequency: Double
)

data class DiagnosticsReport(
    val winRateDrift: Boolean,
    val volatilityDrift: Boolean,
    val regimeInstability: Boolean,
    val correlationClustering: Boolean,
    val tailBreak: Boolean,
    val notes: List<String>
)

/*
RiskDiagnosticsEngine is a monitoring layer.

It MUST NOT:
- Modify ASC probabilities
- Modify RiskAI sizing
- Override signals
- Execute trades

It only generates warnings for UI display and logging.

All outputs must be deterministic.
*/
class RiskDiagnosticsEngine {

    fun generateReport(
        recentTrades: List<TradeResult>,
        surfaceStats: SurfaceStats,
        realizedVolatility: Double,
        rollingCorrelation: Double
    ): DiagnosticsReport {

        val notes = mutableListOf<String>()

        // --- 1️⃣ Win Rate Drift ---
        val window = 100
        val lastWindow = recentTrades.takeLast(window)
        val rollingWinRate = if (lastWindow.isEmpty()) 0.0 else lastWindow.count { it.win }.toDouble() / lastWindow.size

        val winRateDrift = rollingWinRate < surfaceStats.winRate - 0.10

        if (winRateDrift) {
            notes.add("Win rate drift detected")
        }

        // --- 2️⃣ Volatility Drift ---
        val volatilityDrift = realizedVolatility > surfaceStats.volatility * 1.5

        if (volatilityDrift) {
            notes.add("Realized volatility exceeds modeled volatility")
        }

        // --- 3️⃣ Regime Frequency Instability ---
        val regimeCounts = recentTrades.groupingBy { it.regimeStack }.eachCount()
        val mostFrequentRegime = regimeCounts.maxByOrNull { it.value }

        val regimeInstability = if (recentTrades.isEmpty()) false else
            (mostFrequentRegime != null &&
                    (mostFrequentRegime.value.toDouble() / recentTrades.size) >
                    surfaceStats.regimeFrequency * 1.5)

        if (regimeInstability) {
            notes.add("Regime frequency instability detected")
        }

        // --- 4️⃣ Correlation Clustering ---
        val correlationClustering = rollingCorrelation > 0.75

        if (correlationClustering) {
            notes.add("Correlation clustering during drawdown")
        }

        // --- 5️⃣ Tail Event Break ---
        val worstTrades = recentTrades.sortedBy { it.pnl }.take(5)
        val worstLoss = worstTrades.firstOrNull()?.pnl ?: 0.0

        val tailBreak = worstLoss < surfaceStats.tail05 * 1.5

        if (tailBreak) {
            notes.add("Tail loss exceeds modeled 5% tail")
        }

        return DiagnosticsReport(
            winRateDrift = winRateDrift,
            volatilityDrift = volatilityDrift,
            regimeInstability = regimeInstability,
            correlationClustering = correlationClustering,
            tailBreak = tailBreak,
            notes = notes
        )
    }

    // Read-only helper that queries a TradeHistoryRepository for recent trades and computes the same report.
    suspend fun generateReportFromRepository(
        repository: com.asc.markets.data.trade.TradeHistoryRepository,
        surfaceStats: SurfaceStats,
        realizedVolatility: Double,
        rollingCorrelation: Double
    ): DiagnosticsReport {
        val recent = repository.getLast100Trades().map {
            TradeResult(asset = it.asset, pnl = it.pnl, win = it.win, regimeStack = it.regimeStack, timestamp = it.timestamp)
        }
        val worst5 = repository.getWorst5Trades()

        // reuse generateReport logic
        return generateReport(recent, surfaceStats, realizedVolatility, rollingCorrelation)
    }
}
