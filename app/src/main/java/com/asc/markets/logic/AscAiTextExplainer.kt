package com.asc.markets.logic

import com.asc.markets.backend.GroqClient
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.data.remote.LatestDeploymentsResponse
import com.asc.markets.data.remote.RunAiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object AscAiTextExplainer {
    suspend fun explain(
        userQuery: String,
        personaName: String,
        personaInstruction: String,
        deployments: LatestDeploymentsResponse?,
        appContext: String = "",
        conversationHistory: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (isGreeting(userQuery)) {
            return@withContext "This is ASC Engine v1. What can I do for you?"
        }

        if (!GroqClient.isKeyConfigured()) {
            return@withContext "ASC Engine v1 is offline: GROQ_API_KEY is not configured."
        }

        val prompt = buildPrompt(userQuery, personaName, personaInstruction, deployments, appContext, conversationHistory)
        val groqError = runCatching {
            GroqClient.chatCompletion(prompt)
        }
        groqError.getOrNull()?.let { return@withContext it }

        return@withContext "ASC Engine v1 is unavailable: ${shortError(groqError.exceptionOrNull())}"
    }

    suspend fun explainChartAnalysis(
        analysisResult: RunAiResponse,
        personaName: String,
        personaInstruction: String,
        currentMarketData: String = "",
        userParameters: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (!GroqClient.isKeyConfigured()) {
            return@withContext "ASC Engine v1 is offline: GROQ_API_KEY is not configured."
        }

        val aiContext = com.asc.markets.ai.AIContextService.contextState.value
        val platformContext = aiContext.platformContext
        val personaLens = buildPersonaLens(personaName, personaInstruction)
        
        val deploymentText = buildString {
            appendLine("success=${analysisResult.success}")
            analysisResult.final_decision.forEachIndexed { index: Int, item: FinalDecisionItem ->
                appendLine(formatDecision(index + 1, item))
            }
        }

        val prompt = """
            You are ASC Engine v1, the in-app assistant for ASC Market.
            
            [PLATFORM_CONTEXT_KNOWLEDGE]
            Current Platform Configuration:
            - Access Permissions: ${platformContext.accessPermissions.filter { it.value }.keys.joinToString(", ")} (All unrestricted)
            - Exclusive Data Source: ${platformContext.dataSources.filter { it.value == "Pepperstone" }.keys.joinToString(", ")} via Pepperstone.
            - Exception: USDT pairs are sourced exclusively from Binance.
            - Operational Directives: ${platformContext.operationalRules.joinToString(" ")}

            [ACTIVE_PERSONA_LENS]
            $personaLens

            The user has uploaded a technical chart screenshot (MT5/TradingView).
            The ASC AI (internal vision/analysis engine) has processed the image and produced the raw analytical payload below.
            
            YOUR TASK:
            1. Read the ASC AI deployment payload from the uploaded chart image.
            2. Compare the chart analysis with the CURRENT LIVE MARKET DATA below.
            3. Consider the user's trading parameters and risk settings.
            4. Provide a professional trading signal with:
               - BIAS: Clear directional bias (LONG/SHORT/NEUTRAL) based on chart + current market
               - ENTRY: Suggested entry zone or trigger condition
               - STOP LOSS: Risk management level
               - TAKE PROFIT: Target zones
               - CONFIDENCE: Your confidence level (0-100%)
               - REASONING: 3-4 bullets explaining why this setup is valid NOW
            5. Use the Active Persona Lens to guide your tone and priorities.
            6. Be actionable and specific - this should be a tradeable signal.
            7. If the chart analysis conflicts with current market conditions, explain the discrepancy.
            8. Do not mention that you are an LLM or that you are summarizing a payload.
            9. Speak as if you are ASC Engine v1 presenting a live trading recommendation.

            [CURRENT LIVE MARKET DATA]
            ${currentMarketData.ifBlank { "No current market data available. Analysis based on chart only." }}

            [USER TRADING PARAMETERS]
            ${userParameters.ifBlank { "No user parameters provided. Using default risk management." }}

            [ASC AI CHART ANALYSIS PAYLOAD]
            $deploymentText
            
            Format your response as:
            
            🎯 SIGNAL: [LONG/SHORT/NEUTRAL]
            
            📊 CHART ANALYSIS:
            [What the uploaded chart shows]
            
            📈 CURRENT MARKET:
            [How current price action compares to the chart]
            
            💡 TRADE SETUP:
            • Entry: [specific level or condition]
            • Stop Loss: [specific level]
            • Take Profit: [target zones]
            • Confidence: [0-100%]
            
            🔍 REASONING:
            [3-4 bullets explaining the setup]
            
            ⚠️ RISK NOTE:
            [Any warnings or conditions to watch]
        """.trimIndent()

        val groqError = runCatching {
            GroqClient.chatCompletion(prompt, model = "llama-3.3-70b-versatile")
        }
        groqError.getOrNull()?.let { return@withContext it }

        return@withContext "ASC Engine v1 is unavailable: ${shortError(groqError.exceptionOrNull())}"
    }

    private fun buildPrompt(
        userQuery: String,
        personaName: String,
        personaInstruction: String,
        deployments: LatestDeploymentsResponse?,
        appContext: String,
        conversationHistory: String
    ): String {
        val deploymentText = formatDeployments(deployments)
        val aiContext = com.asc.markets.ai.AIContextService.contextState.value
        val platformContext = aiContext.platformContext
        val personaLens = buildPersonaLens(personaName, personaInstruction)
        
        val platformKnowledge = """
            [PLATFORM_CONTEXT_KNOWLEDGE]
            Current Platform Configuration:
            - Access Permissions: ${platformContext.accessPermissions.filter { it.value }.keys.joinToString(", ")} (All unrestricted)
            - Exclusive Data Source: ${platformContext.dataSources.filter { it.value == "Pepperstone" }.keys.joinToString(", ")} via Pepperstone.
            - Exception: USDT pairs are sourced exclusively from Binance.
            - Operational Directives: ${platformContext.operationalRules.joinToString(" ")}
        """.trimIndent()

        return """
            You are ASC Engine v1, the in-app assistant for ASC Market.
            
            $platformKnowledge
            
            CRITICAL: The user is asking about their LIVE APP STATE. You MUST read and use the "App state snapshot" section below.

            Answer the user's exact question using the platform knowledge, app state, and ASC deployment payload below.
            Be concise: maximum 3 short sentences or 3 bullets unless the user asks for detail.
            Do not dump all deployment data unless asked.
            For greetings, answer exactly: This is ASC Engine v1. What can I do for you?
            For non-greeting replies, do not repeat the greeting line.
            If the app state snapshot is missing a requested field, answer briefly with the missing field name and the exact refresh needed.

            [ACTIVE_PERSONA_LENS]
            $personaLens
            You must keep the facts grounded in app state, but change the reasoning, priorities, and explanation style whenever the selected persona changes.
            
            When the user asks about:
            - "how many trades" → Read active_live_trades from App state snapshot
            - "live trades" → Read active_live_trades and current_live_trade from App state snapshot
            - "account" or "balance" → Read balance, equity, floating_pnl from App state snapshot
            - "current view" → Read current_view from App state snapshot
            - "selected asset" → Read selected_asset from App state snapshot
            - "balance", "equity", "pnl", "floating pnl", or "realized pnl" → Read balance, equity, floating_pnl, and realized_pnl from App state snapshot
            - "current price", "price", "is it going up or down", "BTCUSDT", "ETHUSDT", or any symbol query → Read visible_price_context first, then selected_asset_price, then any matching market_pair entries in App state snapshot
            - "page context" or "focused page" → Use chat_context_focus_label as the main lens, but you may still reference other pages when useful
            - "data source" or "where is data from" → Refer to PLATFORM_CONTEXT_KNOWLEDGE
            - "access" or "permissions" → Refer to PLATFORM_CONTEXT_KNOWLEDGE
            
            Do not create independent trading signals, prices, risk numbers, probabilities, or recommendations.
            If data is unavailable, say exactly what is unavailable and what to refresh.
            Do not mention Groq, OpenAI, Gemini, prompts, or hidden context.

            Selected ASC desk: $personaName
            Desk lens: $personaInstruction
            User question: $userQuery

            App state snapshot:
            ${appContext.ifBlank { "No app state snapshot was provided." }}

            Conversation history:
            ${conversationHistory.ifBlank { "No prior conversation turns were provided." }}

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
            message.contains("Unable to resolve host", ignoreCase = true) -> "Groq is unreachable because the device cannot resolve api.groq.com. Check internet access, DNS, or firewall settings."
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

    private fun buildPersonaLens(personaName: String, personaInstruction: String): String {
        val normalized = personaName.trim().lowercase(Locale.US)
        val style = when {
            normalized.contains("macro") -> "Macro-first lens: prioritize scheduled events, regime shifts, policy tone, and broad market context before price action. Output style: start with a one-line macro verdict, then 2-3 bullets for regime, catalyst, and bias."
            normalized.contains("smc") -> "Structure-first lens: prioritize BOS, CHoCH, market structure, liquidity sweeps, and directional control. Output style: use a structure/bias/invalidation layout with clear levels."
            normalized.contains("liquidity") -> "Liquidity-first lens: prioritize wick behavior, stop hunts, volume anomalies, and timing around liquidity grabs. Output style: use trigger/timing/liquidity bullets and keep it punchy."
            normalized.contains("algo") -> "Quant lens: prioritize probability, expectancy, confluence scoring, and rule-based filtering. Output style: show score, confidence, edge, and key conditions in a compact analytical format."
            normalized.contains("sentiment") -> "Sentiment lens: prioritize crowding, positioning, risk-on/risk-off behavior, and contrarian clues. Output style: separate crowding, sentiment shift, and implication."
            normalized.contains("prop") -> "Capital-protection lens: prioritize risk limits, safety gates, invalidation, and only the highest-quality setups. Output style: lead with approve/veto and follow with risk notes."
            else -> "General desk lens: answer in a way that best matches the selected persona instruction. Output style: keep the reply natural but noticeably different from other desks."
        }

        return buildString {
            appendLine("persona_name=$personaName")
            appendLine("persona_style=$style")
            appendLine("persona_instruction=$personaInstruction")
            appendLine("persona_switch_rule=When the user switches personas, change the explanation lens, vocabulary, priorities, and visible output format to match the active persona.")
        }
    }
}
