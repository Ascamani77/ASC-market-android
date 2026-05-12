package com.asc.markets.logic

import com.asc.markets.backend.GroqClient
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.data.remote.LatestDeploymentsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object AscAiTextExplainer {
    suspend fun explain(
        userQuery: String,
        personaName: String,
        personaInstruction: String,
        deployments: LatestDeploymentsResponse?,
        appContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (isGreeting(userQuery)) {
            return@withContext "This is ASC Engine v1. What can I do for you?"
        }

        if (!GroqClient.isKeyConfigured()) {
            return@withContext "ASC Engine v1 is offline: GROQ_API_KEY is not configured."
        }

        val prompt = buildPrompt(userQuery, personaName, personaInstruction, deployments, appContext)
        val groqError = runCatching {
            GroqClient.chatCompletion(prompt)
        }
        groqError.getOrNull()?.let { return@withContext it }

        return@withContext "ASC Engine v1 is unavailable: ${shortError(groqError.exceptionOrNull())}"
    }

    private fun buildPrompt(
        userQuery: String,
        personaName: String,
        personaInstruction: String,
        deployments: LatestDeploymentsResponse?,
        appContext: String
    ): String {
        val deploymentText = formatDeployments(deployments)
        return """
            You are ASC Engine v1, the in-app assistant for ASC Market.
            
            CRITICAL: The user is asking about their LIVE APP STATE. You MUST read and use the "App state snapshot" section below.
            
            Answer the user's exact question using the app state and ASC deployment payload below.
            Be concise: maximum 3 short sentences or 3 bullets unless the user asks for detail.
            Do not dump all deployment data unless asked.
            For greetings, answer exactly: This is ASC Engine v1. What can I do for you?
            
            When the user asks about:
            - "how many trades" → Read active_live_trades from App state snapshot
            - "live trades" → Read active_live_trades and current_live_trade from App state snapshot
            - "account" or "balance" → Read balance, equity, floating_pnl from App state snapshot
            - "current view" → Read current_view from App state snapshot
            - "selected asset" → Read selected_asset from App state snapshot
            
            Do not create independent trading signals, prices, risk numbers, probabilities, or recommendations.
            If data is unavailable, say exactly what is unavailable and what to refresh.
            Do not mention Groq, OpenAI, Gemini, prompts, or hidden context.

            Selected ASC desk: $personaName
            Desk lens: $personaInstruction
            User question: $userQuery

            App state snapshot:
            ${appContext.ifBlank { "No app state snapshot was provided." }}

            ASC AI deployment payload:
            $deploymentText
        """.trimIndent()
    }

    private fun isGreeting(userQuery: String): Boolean {
        val normalized = userQuery.trim().lowercase(Locale.US).trim('.', '!', '?')
        return normalized in setOf("hi", "hello", "hey", "yo", "good morning", "good afternoon", "good evening")
    }

    private fun shortError(error: Throwable?): String {
        val message = error?.message.orEmpty()
        return when {
            message.contains("401") || message.contains("403") -> "Groq rejected the API key."
            message.contains("429") -> "Groq rate limit reached."
            message.contains("timeout", ignoreCase = true) -> "Groq request timed out."
            message.isBlank() -> "unknown Groq error."
            else -> message.lineSequence().firstOrNull()?.take(160) ?: "unknown Groq error."
        }
    }

    private fun formatDeployments(deployments: LatestDeploymentsResponse?): String {
        if (deployments == null) return "No LatestDeploymentsResponse is loaded in the app state."
        val decisions = deployments.final_decision
        if (decisions.isEmpty()) {
            return buildString {
                appendLine("success=${deployments.success}")
                appendLine("last_updated=${deployments.last_updated ?: "unknown"}")
                appendLine("count=${deployments.count}")
                appendLine("final_decision=[]")
            }
        }
        return buildString {
            appendLine("success=${deployments.success}")
            appendLine("last_updated=${deployments.last_updated ?: "unknown"}")
            appendLine("count=${deployments.count}")
            decisions.take(8).forEachIndexed { index, item ->
                appendLine(formatDecision(index + 1, item))
            }
            if (decisions.size > 8) appendLine("Additional decisions not shown: ${decisions.size - 8}")
        }
    }

    private fun formatDecision(index: Int, item: FinalDecisionItem): String {
        return buildString {
            appendLine("Decision $index")
            appendLine("asset=${item.asset_1 ?: "unknown"}")
            appendLine("direction=${item.journal_direction ?: "unknown"}")
            appendLine("label=${item.journal_label ?: "unknown"}")
            appendLine("priority=${item.journal_priority ?: "unknown"}")
            appendLine("portfolio_decision=${item.portfolio_decision_label ?: "unknown"}")
            appendLine("deployment_bucket=${item.portfolio_deployment_bucket ?: "unknown"}")
            appendLine("reason=${item.portfolio_decision_reason ?: "none"}")
            appendLine("entry_window=${item.entry_window ?: "unknown"}")
            appendLine("entry_quality=${fmt(item.entry_quality_score)}")
            appendLine("exit_plan=${item.exit_plan ?: "unknown"}")
            appendLine("exit_pressure=${fmt(item.exit_pressure_score)}")
            appendLine("journal_score=${fmt(item.journal_score)}")
            appendLine("ignition_probability=${fmt(item.ignition_probability)}")
            appendLine("expansion_probability=${fmt(item.expansion_probability)}")
            appendLine("confluence_score=${fmt(item.confluence_score)}")
            appendLine("confluence=${item.confluence_count ?: 0}/${item.confluence_total ?: 0}")
            appendLine("risk_pct=${fmt(item.final_risk_pct ?: item.recommended_risk_pct)}")
            appendLine("position_scale=${fmt(item.final_position_scale ?: item.recommended_position_scale)}")
            appendLine("correlation_regime=${item.correlation_regime ?: "unknown"}")
            appendLine("correlation_warning=${item.correlation_warning ?: "none"}")
            appendLine("regime_state=${item.regime_state ?: "unknown"}")
            appendLine("trend_state=${item.trend_state ?: "unknown"}")
            appendLine("structure=${item.structure_label ?: item.structure_state ?: "unknown"}")
            appendLine("pre_move_phase=${item.pre_move_ai_phase ?: "unknown"}")
            appendLine("pre_move_score=${fmt(item.pre_move_ai_score)}")
            appendLine("live_tick_status=${item.live_tick_status ?: "unknown"}")
            appendLine("live_tick_count=${item.live_tick_count ?: 0}")
        }
    }

    private fun fmt(value: Double?): String {
        return value?.let { String.format(Locale.US, "%.4f", it) } ?: "unknown"
    }
}
