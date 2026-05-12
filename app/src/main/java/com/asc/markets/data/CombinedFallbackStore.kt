package com.asc.markets.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CombinedFallbackDecision {
    PENDING,
    ACCEPTED,
    DENIED
}

data class CombinedFallbackState(
    val requestId: Long = 0L,
    val isActive: Boolean = false,
    val decision: CombinedFallbackDecision = CombinedFallbackDecision.DENIED,
    val source: String = "",
    val reason: String = "",
    val requestedAtMillis: Long = 0L
)

object CombinedFallbackStore {
    private val _state = MutableStateFlow(CombinedFallbackState())
    val state: StateFlow<CombinedFallbackState> = _state.asStateFlow()

    fun setManualEnabled(enabled: Boolean) {
        _state.value = if (enabled) {
            val now = System.currentTimeMillis()
            CombinedFallbackState(
                requestId = now,
                isActive = true,
                decision = CombinedFallbackDecision.ACCEPTED,
                source = "Combined fallback",
                reason = "Enabled manually in Settings.",
                requestedAtMillis = now
            )
        } else {
            CombinedFallbackDataStore.clear()
            CombinedFallbackState()
        }
    }

    fun request(source: String, reason: String): CombinedFallbackState {
        val current = _state.value
        if (current.isActive) {
            return current
        }
        val now = System.currentTimeMillis()
        val next = CombinedFallbackState(
            requestId = now,
            isActive = true,
            decision = CombinedFallbackDecision.PENDING,
            source = source,
            reason = reason,
            requestedAtMillis = now
        )
        _state.value = next
        return next
    }

    fun accept(requestId: Long = _state.value.requestId) {
        val current = _state.value
        if (current.isActive && current.requestId == requestId) {
            _state.value = current.copy(decision = CombinedFallbackDecision.ACCEPTED)
        }
    }

    fun deny(requestId: Long = _state.value.requestId) {
        val current = _state.value
        if (current.isActive && current.requestId == requestId) {
            _state.value = current.copy(decision = CombinedFallbackDecision.DENIED)
        }
    }

    fun markPrimaryRestored() {
        if (_state.value.isActive && _state.value.decision != CombinedFallbackDecision.ACCEPTED) {
            _state.value = CombinedFallbackState()
        }
    }

    fun canUseFallback(): Boolean {
        val current = _state.value
        return current.isActive && current.decision == CombinedFallbackDecision.ACCEPTED
    }
}
