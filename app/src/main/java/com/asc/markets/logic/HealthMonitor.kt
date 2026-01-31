package com.asc.markets.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HealthMetrics(
    val memoryUsageMb: Int = 0,
    val clockDriftMs: Int = 0,
    val integrityStatus: String = "INTEGRITY_OK",
    val throttlingDetected: Boolean = false,
    val uptimePercent: Float = 99.9f
)

object HealthMonitor {
    private val _metrics = MutableStateFlow(HealthMetrics())
    val metrics = _metrics.asStateFlow()

    /**
     * Structural scan of local persistence for corruption detection.
     */
    fun performIntegrityCheck(keys: List<String>) {
        val isCorrupt = keys.any { it.isEmpty() }
        _metrics.value = _metrics.value.copy(
            integrityStatus = if (isCorrupt) "CORRUPTION_DETECTED" else "INTEGRITY_OK"
        )
    }

    fun updateProfiler(usedMemory: Int, drift: Int) {
        _metrics.value = _metrics.value.copy(
            memoryUsageMb = usedMemory,
            clockDriftMs = drift
        )
    }
}