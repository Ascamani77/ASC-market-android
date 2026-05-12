package com.asc.markets.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

data class RelayData(
    val title: String,
    val latency: Double,
    val buffer: Double,
    val id: String
)

data class LogLineData(
    val level: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object SystemTelemetry {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _globalThroughput = MutableStateFlow(0.0)
    val globalThroughput = _globalThroughput.asStateFlow()

    private val _aggLatency = MutableStateFlow(0.0)
    val aggLatency = _aggLatency.asStateFlow()

    private val _relays = MutableStateFlow(
        listOf(
            RelayData("LMAX NY4 RELAY", 0.0, 0.0, "LMAX"),
            RelayData("BINANCE AGGREGATOR", 0.0, 0.0, "BINANCE"),
            RelayData("MT5 BRIDGE", 0.0, 0.0, "MT5"),
            RelayData("ASC AI BACKEND", 0.0, 0.0, "ASC_AI")
        )
    )
    val relays = _relays.asStateFlow()

    private val _logs = MutableStateFlow<List<LogLineData>>(emptyList())
    val logs = _logs.asStateFlow()

    // Tracking queues
    private val tickTimestamps = ConcurrentLinkedQueue<Long>()
    private val sourceTickTimestamps = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()
    private val recentLatencies = ConcurrentLinkedQueue<Double>()
    
    // Relay-specific
    private var binanceLatency = 0.0
    private var mt5Latency = 0.0
    private var lmaxLatency = 0.0
    private var ascAiLatency = 0.0

    private var binanceBuffer = 0.0
    private var mt5Buffer = 0.0
    private var lmaxBuffer = 0.0
    private var ascAiBuffer = 0.0

    init {
        scope.launch {
            while (true) {
                calculateMetrics()
                delay(1000)
            }
        }
    }

    fun recordTick(source: String, latencyMs: Double) {
        val now = System.currentTimeMillis()
        val normalizedSource = source.uppercase()
        tickTimestamps.add(now)
        sourceTickTimestamps.getOrPut(normalizedSource) { ConcurrentLinkedQueue() }.add(now)
        recentLatencies.add(latencyMs)
        
        when (normalizedSource) {
            "BINANCE" -> {
                binanceLatency = latencyMs
            }
            "MT5" -> {
                mt5Latency = latencyMs
            }
            "LMAX" -> {
                lmaxLatency = latencyMs
            }
            "ASC_AI", "ASC-AI", "ASC AI" -> {
                ascAiLatency = latencyMs
            }
        }
        
        // Optionally emit a log occasionally
        if (Math.random() < 0.05) {
            addLog("[DATA]", "PRICE_TICK via $source | LATENCY: ${latencyMs.toInt()}ms")
        }
    }
    
    fun recordConnectionEvent(source: String, event: String) {
        addLog("[INFO]", "$source: $event")
    }

    private fun addLog(level: String, msg: String) {
        val list = _logs.value.toMutableList()
        list.add(0, LogLineData(level, msg))
        if (list.size > 20) {
            list.removeAt(list.size - 1)
        }
        _logs.value = list
    }

    private fun calculateMetrics() {
        val now = System.currentTimeMillis()
        val oneSecAgo = now - 1000

        // Clean up old ticks
        while (tickTimestamps.isNotEmpty() && tickTimestamps.peek()!! < oneSecAgo) {
            tickTimestamps.poll()
        }
        sourceTickTimestamps.values.forEach { queue ->
            while (queue.isNotEmpty() && queue.peek()!! < oneSecAgo) {
                queue.poll()
            }
        }
        while (recentLatencies.isNotEmpty() && recentLatencies.size > 100) {
            recentLatencies.poll()
        }

        val tps = tickTimestamps.size.toDouble()
        val avgLat = if (recentLatencies.isEmpty()) 0.0 else recentLatencies.average()
        val binanceTps = sourceTickTimestamps["BINANCE"]?.size?.toDouble() ?: 0.0
        val mt5Tps = sourceTickTimestamps["MT5"]?.size?.toDouble() ?: 0.0
        val lmaxTps = sourceTickTimestamps["LMAX"]?.size?.toDouble() ?: 0.0
        val ascAiTps = sourceTickTimestamps["ASC_AI"]?.size?.toDouble() ?: 0.0

        _globalThroughput.value = tps
        _aggLatency.value = avgLat

        binanceBuffer = (binanceTps * 20.0).coerceIn(0.0, 100.0)
        mt5Buffer = (mt5Tps * 20.0).coerceIn(0.0, 100.0)
        lmaxBuffer = (lmaxTps * 20.0).coerceIn(0.0, 100.0)
        ascAiBuffer = (ascAiTps * 20.0).coerceIn(0.0, 100.0)

        _relays.value = listOf(
            RelayData("LMAX NY4 RELAY", lmaxLatency, lmaxBuffer, "LMAX"),
            RelayData("BINANCE AGGREGATOR", binanceLatency, binanceBuffer, "BINANCE"),
            RelayData("MT5 BRIDGE", mt5Latency, mt5Buffer, "MT5"),
            RelayData("ASC AI BACKEND", ascAiLatency, ascAiBuffer, "ASC_AI")
        )
    }
}
