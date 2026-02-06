package com.asc.markets.backend

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Institutional Surveillance State Container
 * Tracks arming status and active policy-locks for the Macro Intelligence Stream.
 */
object SurveillanceStateManager {
    data class GlobalSurveillanceState(
        val isArmed: Boolean = false,
        val activePolicy: String = "L14_STANDARD",
        val lastAuditId: String? = null
    )

    private val _state = MutableStateFlow(GlobalSurveillanceState())
    val state = _state.asStateFlow()

    fun setArmed(armed: Boolean) {
        _state.value = _state.value.copy(isArmed = armed)
    }
}