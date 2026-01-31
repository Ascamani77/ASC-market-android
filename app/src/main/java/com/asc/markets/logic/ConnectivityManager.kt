package com.asc.markets.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.math.min
import kotlin.math.pow

enum class ConnectionState { LIVE, STALE, DEGRADED, DISCONNECTED, PAUSED }

object ConnectivityManager {
    private val _state = MutableStateFlow(ConnectionState.LIVE)
    val state = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private var lastHeartbeat: Long = System.currentTimeMillis()
    private var circuitBreakerActive: Boolean = false
    private var reconnectAttempts = 0
    private const val MAX_BACKOFF = 30000L

    fun recordHeartbeat() {
        lastHeartbeat = System.currentTimeMillis()
        reconnectAttempts = 0
        if (!circuitBreakerActive) {
            _state.value = ConnectionState.LIVE
        }
    }

    fun logDiagnostic(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        _logs.value = (listOf(entry) + _logs.value).take(100)
    }

    fun activateCircuitBreaker() {
        if (circuitBreakerActive) return
        circuitBreakerActive = true
        _state.value = ConnectionState.PAUSED
        logDiagnostic("CIRCUIT_BREAKER_ACTIVE: Rate Limit breach. Locking dispatches for 30s.")
        
        Timer().schedule(object : TimerTask() {
            override fun run() {
                circuitBreakerActive = false
                _state.value = ConnectionState.LIVE
                logDiagnostic("CIRCUIT_BREAKER_RELEASED: Resuming data ingestion.")
            }
        }, 30000)
    }

    fun checkWatchdog() {
        if (circuitBreakerActive) return
        
        val diff = System.currentTimeMillis() - lastHeartbeat
        val newState = when {
            diff > 20000 -> {
                if (_state.value != ConnectionState.DISCONNECTED) {
                    logDiagnostic("CRITICAL: Stream heartbeat lost. Initiating backoff.")
                }
                ConnectionState.DISCONNECTED
            }
            diff > 10000 -> ConnectionState.STALE
            diff > 5000 -> ConnectionState.DEGRADED
            else -> ConnectionState.LIVE
        }

        if (_state.value != newState) {
            _state.value = newState
            logDiagnostic("SYSTEM_STATE_CHANGE: $newState")
        }
    }
}