package com.asc.markets.logic

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

data class BacktestParams(
    val fastMa: Int,
    val slowMa: Int,
    val rsiPeriod: Int,
    val rsiLow: Int,
    val rsiHigh: Int,
    val timeframe: String,
    val pair: String
)

data class BacktestResult(
    val winRate: Double,
    val profitFactor: Double,
    val sharpe: Double,
    val recoveryRatio: Double,
    val sessions: List<String>,
    val verdict: String, // BUY / SELL / WAIT
    val rationale: String,
    val logs: List<String>
)

object BacktestEngine {
    /**
     * Simulate sending params to an external AI (Gemini 3 Pro) and produce a BacktestResult.
     * Emits log lines through onLog callback while running.
     */
    suspend fun runBacktest(params: BacktestParams, onLog: (String) -> Unit): BacktestResult {
        val logs = mutableListOf<String>()
        fun emit(msg: String) {
            logs.add(msg)
            onLog(msg)
        }

        emit("[Sim_Engine_Node_L14] Initializing synthetic audit...")
        delay(600)
        emit("[Sim_Engine_Node_L14] Sending parameters to Gemini-3-Pro model")
        delay(600)
        emit("[Sim_Engine_Node_L14] Calibrating institutional cycle priors")
        delay(800)

        // synthetic analysis complexity depending on param tightness
        val complexity = ((params.slowMa - params.fastMa).coerceAtLeast(1)).coerceAtMost(200)
        val steps = 6 + (complexity / 20)
        repeat(steps) { i ->
            if (!Thread.currentThread().isAlive) return BacktestResult(0.0,0.0,0.0,0.0, emptyList(), "WAIT", "Interrupted", logs)
            emit("[Sim_Engine_Node_L14] Analyzing phase ${i + 1}/$steps...")
            delay(400)
        }

        emit("[Sim_Engine_Node_L14] Performing session liquidity attribution")
        delay(600)

        // produce deterministic-ish metrics from params
        val base = (params.fastMa + params.slowMa) % 50
        val winRate = (40.0 + (params.rsiHigh - params.rsiLow) * 0.4 + (base % 10)) .coerceIn(10.0, 95.0)
        val profitFactor = (1.1 + (params.slowMa - params.fastMa) * 0.02 + Random.nextDouble(-0.2, 0.6)).coerceIn(0.3, 5.0)
        val sharpe = (0.2 + (params.rsiHigh - params.rsiLow) * 0.01 + Random.nextDouble(-0.5, 1.2)).coerceIn(-1.0, 3.5)
        val recovery = (0.5 + profitFactor * 0.4).coerceIn(0.2, 6.0)

        // sessions attribution heuristic
        val sessions = mutableListOf<String>()
        if (params.timeframe in listOf("M15", "M30", "H1")) sessions.add("London")
        if (params.timeframe in listOf("H1", "H4", "D1")) sessions.add("New York")
        if (sessions.isEmpty()) sessions.add("London")

        // verdict heuristic
        val score = winRate * 0.5 + profitFactor * 10 + sharpe * 15
        val verdict = when {
            score >= 120 -> "BUY"
            score <= 40 -> "SELL"
            else -> "WAIT"
        }

        val rationale = buildString {
            append("Institutional Audit & Strategy Analysis\n\n")

            // Strategy overview
            append("**Strategy Overview**: The simulation utilized a Moving Average Crossover (${params.fastMa}/${params.slowMa}) on the ${params.timeframe} timeframe, filtered by RSI(${params.rsiPeriod}) to avoid entries during extreme overbought (>${params.rsiHigh}) or oversold (<${params.rsiLow}) conditions. This filter is intended to reduce false entries during consolidation phases.\n\n")

            // Performance metrics
            append("**Performance Metrics**:\n")
            val pfStr = String.format("%.2f", profitFactor)
            val wrStr = String.format("%.1f", winRate)
            val sharpeStr = String.format("%.2f", sharpe)
            val recStr = String.format("%.2f", recovery)
            if (profitFactor > 1.0) {
                append("- **Profitability**: The system demonstrates a positive mathematical expectancy. Win rate is ${wrStr}%, Profit Factor is ${pfStr}.\n")
            } else {
                append("- **Profitability**: The system shows weak expectancy on these parameters. Win rate is ${wrStr}%, Profit Factor is ${pfStr}.\n")
            }
            append("- **Risk Profile**: Sharpe Ratio ${sharpeStr}, Recovery Ratio ${recStr}. Max drawdown not estimated in this synthetic run.\n\n")

            // Institutional alignment
            append("**Institutional Alignment**:\n")
            if (sessions.isNotEmpty()) {
                append("- **Liquidity**: Majority of attributed sessions: ${sessions.joinToString(", ")}. ")
                if (sessions.contains("London") && sessions.contains("New York")) {
                    append("Notable opportunity around London/New York overlap where institutional orderflow is commonly injected.\n")
                } else {
                    append("Observed session alignment may favour ${sessions.first()}.\n")
                }
            } else {
                append("- **Liquidity**: No specific session alignment detected.\n")
            }
            append("- **Market Structure**: The ${params.slowMa} EMA acts as a dynamic pivot; current signals show ${if (verdict == "BUY") "accumulation" else if (verdict == "SELL") "distribution" else "mixed"} characteristics.\n\n")

            // Conclusion
            append("**Conclusion**: The strategy is generally more robust in trending environments and may underperform during range-bound regimes. Current structured output suggests a ${if (verdict == "BUY") "bullish" else if (verdict == "SELL") "bearish" else "neutral/mixed"} bias.\n")
        }

        emit("[Sim_Engine_Node_L14] Finalizing structured output")
        delay(500)
        emit("[Sim_Engine_Node_L14] Audit complete")

        return BacktestResult(
            winRate = String.format("%.1f", winRate).toDouble(),
            profitFactor = String.format("%.2f", profitFactor).toDouble(),
            sharpe = String.format("%.2f", sharpe).toDouble(),
            recoveryRatio = String.format("%.2f", recovery).toDouble(),
            sessions = sessions,
            verdict = verdict,
            rationale = rationale,
            logs = logs
        )
    }
}
