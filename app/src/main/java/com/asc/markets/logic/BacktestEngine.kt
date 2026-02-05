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
            append("Synthetic Audit Summary:\n")
            append("Parameters: FastMA=${params.fastMa} SlowMA=${params.slowMa} RSI=${params.rsiPeriod}(${params.rsiLow}-${params.rsiHigh}) TF=${params.timeframe} on ${params.pair}\n\n")
            append("Model observed alignment with institutional cycles: ")
            append(if (verdict == "BUY") "Accumulation bias visible across sessions." else if (verdict == "SELL") "Distribution characteristics observed." else "Mixed signals; insufficient conviction.")
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
