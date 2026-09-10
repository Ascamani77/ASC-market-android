package com.asc.markets.data

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ScannerSignal(
    val asset: String,
    val direction: String = "WAIT",
    val confidence: Double = 0.0,
    @kotlinx.serialization.SerialName("p_trade")
    val pTrade: Double = -1.0,
    val age: Int = 0,
    val combined: Double = 0.0,
    val ts: Double = 0.0
)

@Serializable
data class ScannerSignalsResponse(
    val signals: List<ScannerSignal> = emptyList()
)

object ScannerSignalsStore {
    private const val TAG = "ScannerSignalsStore"
    private const val POLL_INTERVAL_MS = 10_000L

    private var scannerUrl: String = ""

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _signals = MutableStateFlow<List<ScannerSignal>>(emptyList())
    val signals: StateFlow<List<ScannerSignal>> = _signals

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var pollingJob: Job? = null

    fun start(context: Context) {
        if (pollingJob?.isActive == true) return
        scannerUrl = NetworkConfig.scannerBaseUrl(context) + "/signals"
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Starting scanner polling at $scannerUrl")
            while (isActive) {
                try {
                    fetchSignals()
                    delay(POLL_INTERVAL_MS)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                    _isConnected.value = false
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _isConnected.value = false
    }

    private suspend fun fetchSignals() {
        try {
            val client = HttpClient(OkHttp) {
                install(HttpTimeout) { requestTimeoutMillis = 5000 }
            }
            val response: HttpResponse = client.get(scannerUrl)
            val text = response.body<String>()
            val data = json.decodeFromString<ScannerSignalsResponse>(text)
            _signals.value = data.signals
            _isConnected.value = data.signals.isNotEmpty() || response.status.value == 200
            Log.d(TAG, "Fetched ${data.signals.size} scanner signals")
            // Log top 3 for debug
            data.signals.take(3).forEach {
                Log.d(TAG, "  ${it.asset} ${it.direction} conf=${it.confidence} pTrade=${it.pTrade} age=${it.age}s")
            }
            client.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching scanner signals", e)
            _isConnected.value = false
        }
    }
}
