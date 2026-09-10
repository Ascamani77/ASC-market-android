package com.asc.markets.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Singleton service that provides AI context and decisions to the entire application.
 */
object AIContextService {
    private val _contextState = MutableStateFlow(AIContextState())
    val contextState: StateFlow<AIContextState> = _contextState.asStateFlow()

    fun start() {
        // Implementation for polling would go here
    }

    fun stop() {
        // Implementation for stopping would go here
    }

    fun refresh() {
        // Force refresh logic
    }

    fun getDecisionForAsset(symbol: String): AIDecision? {
        val normalized = normalize(symbol)
        return _contextState.value.decisions[normalized] ?: _contextState.value.decisions.entries.firstOrNull {
            normalize(it.key) == normalized
        }?.value
    }

    fun getAllDecisions(): Map<String, AIDecision> = _contextState.value.decisions

    fun getAllNewsImpacts(): List<NewsImpact> = _contextState.value.newsImpacts

    private fun normalize(symbol: String): String = symbol.uppercase(Locale.US)
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
        .replace(".", "")
}
