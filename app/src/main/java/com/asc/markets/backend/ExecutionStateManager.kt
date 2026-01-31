package com.asc.markets.backend

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Institutional Execution State Container
 * Tracks arming status and active policy-locks.
 */
object ExecutionStateManager {
    data class GlobalExecutionState(
        val isArmed: Boolean = false,
        val activePolicy: String = "L14_STANDARD",
        val lastAuditId: String? = null
    )

    private val _state = MutableStateFlow(GlobalExecutionState())
    val state = _state.asStateFlow()

    fun setArmed(armed: Boolean) {
        _state.value = _state.value.copy(isArmed = armed)
    }
}