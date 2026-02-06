package com.asc.markets.logic

import kotlinx.coroutines.delay
import com.asc.markets.backend.OpenAIClient
import com.asc.markets.BuildConfig
import kotlin.random.Random

data class TradeRecord(
    val id: String,
    val side: String,
    val pair: String,
    val lots: Double,
    val algo: String,
    val status: String,
    val audit: String
)

data class ExecutionResult(val success: Boolean, val message: String, val record: TradeRecord?)

object TradingAssistantEngine {
    // Safety lock initially active (pipeline not armed)
    var safetyLockActive: Boolean = true
    var armed: Boolean = false

    // Execution profile
    var executionAlgo: String = "MARKET"

    // Simple in-memory ledger for mocked automated trades (Paper Bridge)
    val MOCK_AUTOMATED_TRADES: MutableList<TradeRecord> = mutableListOf()

    private val tradeCmd = Regex("(?i)\\b(BUY|SELL)\\s+([A-Z/]{3,7})\\s+([\\d.]+)")
    private val setAlgoCmd = Regex("(?i)SET ALGO\\s+(VWAP|TWAP|MARKET)")
    private val armCmd = Regex("(?i)ARM\\s+PIPELINE")

    suspend fun handleInput(raw: String): Pair<String, ExecutionResult?> {
        val text = raw.trim()

        // ARM SURVEILLANCE command
        if (armCmd.containsMatchIn(text)) {
            safetyLockActive = false
            armed = true
            return "[CONFIRMATION] Surveillance pipeline armed. Macro Intelligence Stream enabled." to null
        }

        // Set algorithm
        setAlgoCmd.find(text)?.let { m ->
            val algo = m.groupValues[1].uppercase()
            executionAlgo = algo
            return "[CONFIRMATION] Surveillance algorithm set to $algo." to null
        }

        // Trade command
        tradeCmd.find(text)?.let { m ->
            val side = m.groupValues[1].uppercase()
            val pair = m.groupValues[2].uppercase()
            val lots = m.groupValues[3].toDoubleOrNull() ?: 0.0

            if (safetyLockActive || !armed) {
                return "[REJECTION] Surveillance locked — ARM SURVEILLANCE required before dispatch." to null
            }

            // basic validation
            if (lots <= 0.0) {
                return "[REJECTION] Invalid lot size." to null
            }

            val execRes = executeInstitutionalTrade(side, pair, lots, executionAlgo)
            return (if (execRes.success) "[CONFIRMATION] ${execRes.message}" else "[REJECTION] ${execRes.message}") to execRes
        }

        // Fallback to AI chat for non-command inputs
        val reply = chatWithForexExpert(text)
        return reply to null
    }

    private suspend fun executeInstitutionalTrade(side: String, pair: String, lots: Double, algo: String): ExecutionResult {
        // Simulate checks and latency
        delay(400)

        // Simulate success rate depending on algo (mocked)
        val success = Random.nextDouble() > 0.05

        // generate mock audit
        val audit = generateClinicalAudit(side, pair, lots, algo)

        val id = "TR-${System.currentTimeMillis().toString().takeLast(6)}-${Random.nextInt(100,999)}"
        val record = TradeRecord(id = id, side = side, pair = pair, lots = lots, algo = algo, status = if (success) "DISPATCHED" else "FAILED", audit = audit)

        // Paper Bridge: push to ledger regardless (hydration)
        MOCK_AUTOMATED_TRADES.add(0, record)

        return ExecutionResult(success = success, message = if (success) "Trade $id dispatched via $algo" else "Dispatch failed due to execution error", record = record)
    }

    private suspend fun chatWithForexExpert(question: String): String {
        // If the OpenAI API key is configured, call the remote model; otherwise use a local stub
        try {
            if (BuildConfig.OPENAI_API_KEY.isNotBlank()) {
                val prompt = com.asc.markets.ai.AiPrompts.buildAnalysisPrompt(question)
                val resp = OpenAIClient.chatCompletion(prompt)
                return "[ANALYSIS] " + resp
            }
        } catch (t: Throwable) {
            // fall through to local stub on error
        }

        delay(300)
        return "[ANALYSIS] (synthetic) Response: The inquiry '$question' requires market data; provide live quotes for full clinical analysis."
    }

    private fun generateClinicalAudit(side: String, pair: String, lots: Double, algo: String): String {
        return buildString {
            append("Clinical Audit:\n")
            append("Surveillance Node: $side $pair $lots lots via $algo algorithm.\n")
            append("Rationale: Aligning with session liquidity and institutional flow; targeting minimized slippage and block routing where possible.\n")
            append("Safety: Trade annotated with internal prop-guard tags; simulated compliance checks passed.\n")
        }
    }
}
