package com.asc.markets.network

import android.util.Log
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class TiingoFxWebSocketManager(
    private val apiKey: String,
    private val scope: CoroutineScope,
    private val thresholdLevel: Int = 5
) {
    companion object {
        private const val TAG = "TiingoFxWS"
        private const val WS_URL = "wss://api.tiingo.com/fx"
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val FIRST_PRICE_TIMEOUT_MS = 15_000L
    }

    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        ERROR_UNAUTHORIZED,
        ERROR_UNAVAILABLE
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _priceUpdates = MutableSharedFlow<ForexPair>(extraBufferCapacity = 100)
    val priceUpdates: SharedFlow<ForexPair> = _priceUpdates

    private val _connectionState = MutableSharedFlow<ConnectionState>(extraBufferCapacity = 10)
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    private val symbolLock = Any()
    private val tickers = linkedSetOf<String>()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var firstPriceTimeoutJob: Job? = null

    @Volatile
    private var manuallyDisconnected = false

    @Volatile
    private var receivedLivePrices = false

    @Volatile
    private var subscriptionAccepted = false

    @Volatile
    private var suppressNextCloseReconnect = false

    fun connect(targetSymbols: List<String>) {
        val normalizedTickers = targetSymbols
            .asSequence()
            .map(::normalizeTicker)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        synchronized(symbolLock) {
            tickers.clear()
            tickers.addAll(normalizedTickers)
        }

        disconnectInternal(suppressReconnect = true)
        manuallyDisconnected = false
        receivedLivePrices = false
        subscriptionAccepted = false

        if (apiKey.isBlank()) {
            Log.w(TAG, "Tiingo FX websocket not started: TIINGO_API_KEY is blank")
            _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
            return
        }

        if (normalizedTickers.isEmpty()) {
            Log.w(TAG, "Tiingo FX websocket not started: no forex tickers requested")
            _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
            return
        }

        Log.i(TAG, "Connecting Tiingo FX for ${normalizedTickers.joinToString(",")}")

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Connected to Tiingo FX websocket")
                scheduleFirstPriceTimeout()
                val subscribedTickers = currentTickers()
                Log.i(TAG, "Sending Tiingo FX subscribe threshold=$thresholdLevel tickers=${subscribedTickers.joinToString(",")}")
                webSocket.send(buildSubscribePayload(subscribedTickers).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Tiingo FX websocket closed: $code $reason")
                firstPriceTimeoutJob?.cancel()
                receivedLivePrices = false
                subscriptionAccepted = false
                if (suppressNextCloseReconnect || manuallyDisconnected) {
                    suppressNextCloseReconnect = false
                    return
                }
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val message = buildString {
                    append(t.message.orEmpty())
                    val responseBody = response?.body?.string().orEmpty()
                    if (responseBody.isNotBlank()) {
                        append(' ')
                        append(responseBody)
                    }
                }.trim()

                Log.e(TAG, "Tiingo FX websocket failure: $message", t)
                firstPriceTimeoutJob?.cancel()
                receivedLivePrices = false

                if (message.contains("401") || message.contains("403")) {
                    _connectionState.tryEmit(ConnectionState.ERROR_UNAUTHORIZED)
                } else {
                    _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
                }

                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        manuallyDisconnected = true
        disconnectInternal(suppressReconnect = true)
    }

    private fun handleMessage(text: String) {
        val payload = try {
            JSONObject(text)
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring non-JSON Tiingo message: $text", e)
            return
        }

        when (payload.optString("messageType")) {
            "I" -> {
                val code = payload.optJSONObject("response")?.optInt("code") ?: 0
                if (code == 200) {
                    subscriptionAccepted = true
                    Log.i(TAG, "Tiingo FX subscription accepted")
                    _connectionState.tryEmit(ConnectionState.CONNECTED)
                } else {
                    Log.w(TAG, "Tiingo FX subscription rejected with code=$code payload=$payload")
                    _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
                }
            }

            "H" -> {
                Log.i(TAG, "Tiingo FX heartbeat")
            }

            "A" -> {
                val pair = parseQuote(payload.optJSONArray("data")) ?: return
                Log.i(TAG, "Tiingo FX tick ${pair.symbol} ${pair.price}")
                receivedLivePrices = true
                firstPriceTimeoutJob?.cancel()
                _connectionState.tryEmit(ConnectionState.CONNECTED)
                _priceUpdates.tryEmit(pair)
            }

            else -> {
                val code = payload.optJSONObject("response")?.optInt("code") ?: 0
                if (code == 401 || code == 403) {
                    _connectionState.tryEmit(ConnectionState.ERROR_UNAUTHORIZED)
                }
            }
        }
    }

    private fun parseQuote(data: JSONArray?): ForexPair? {
        if (data == null || data.length() < 6) {
            return null
        }

        if (!data.optString(0).equals("Q", ignoreCase = true)) {
            return null
        }

        val symbol = normalizeDisplaySymbol(data.optString(1))
        if (symbol.isBlank()) {
            return null
        }

        val bid = data.optDouble(4, Double.NaN)
        val mid = data.optDouble(5, Double.NaN)
        val askFromExampleIndex = data.optDouble(7, Double.NaN)
        val askFromDocIndex = data.optDouble(6, Double.NaN)
        val ask = when {
            askFromExampleIndex.isFinite() && askFromExampleIndex > 0.0 && askFromExampleIndex < 10_000.0 -> askFromExampleIndex
            askFromDocIndex.isFinite() && askFromDocIndex > 0.0 && askFromDocIndex < 10_000.0 -> askFromDocIndex
            else -> Double.NaN
        }

        val price = when {
            bid.isFinite() && ask.isFinite() && bid > 0.0 && ask > 0.0 -> (bid + ask) / 2.0
            mid.isFinite() && mid > 0.0 -> mid
            bid.isFinite() && bid > 0.0 -> bid
            else -> return null
        }

        return buildForexPair(symbol, price)
    }

    private fun buildForexPair(symbol: String, price: Double): ForexPair {
        val existing = MarketDataStore.pairSnapshot(symbol)
        val previousPrice = existing?.price ?: price
        val change = price - previousPrice
        val changePercent = if (previousPrice != 0.0) (change / previousPrice) * 100.0 else 0.0

        val category = FOREX_PAIRS.firstOrNull {
            it.category == MarketCategory.FOREX &&
                it.symbol.replace("/", "").uppercase(Locale.US) == symbol
        }?.category ?: MarketCategory.FOREX

        return ForexPair(
            symbol = symbol,
            name = symbol,
            price = price,
            change = change,
            changePercent = changePercent,
            category = category
        )
    }

    private fun buildSubscribePayload(tickers: List<String>): JSONObject =
        JSONObject().apply {
            put("eventName", "subscribe")
            put("authorization", apiKey)
            put(
                "eventData",
                JSONObject().apply {
                    put("thresholdLevel", thresholdLevel.coerceAtLeast(0).toString())
                    put("tickers", JSONArray(tickers))
                }
            )
        }

    private fun scheduleReconnect() {
        if (manuallyDisconnected || currentTickers().isEmpty()) {
            return
        }
        if (reconnectJob?.isActive == true) {
            return
        }

        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(RECONNECT_DELAY_MS)
            if (!manuallyDisconnected && currentTickers().isNotEmpty()) {
                connect(currentTickers())
            }
        }
    }

    private fun scheduleFirstPriceTimeout() {
        firstPriceTimeoutJob?.cancel()
        firstPriceTimeoutJob = scope.launch(Dispatchers.IO) {
            delay(FIRST_PRICE_TIMEOUT_MS)
            if (!manuallyDisconnected && !receivedLivePrices) {
                if (subscriptionAccepted) {
                    Log.w(TAG, "No Tiingo FX prices received yet; subscription is accepted and websocket is still waiting for quote updates")
                } else {
                    Log.w(TAG, "Timed out waiting for Tiingo FX subscription confirmation or prices")
                    _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
                    scheduleReconnect()
                }
            }
        }
    }

    private fun disconnectInternal(suppressReconnect: Boolean = false) {
        reconnectJob?.cancel()
        reconnectJob = null
        firstPriceTimeoutJob?.cancel()
        firstPriceTimeoutJob = null
        try {
            val socket = webSocket
            if (socket != null && suppressReconnect) {
                suppressNextCloseReconnect = true
            }
            socket?.close(1000, "Normal closure")
        } catch (_: Exception) {
        } finally {
            webSocket = null
        }
    }

    private fun currentTickers(): List<String> = synchronized(symbolLock) { tickers.toList() }

    private fun normalizeTicker(symbol: String): String {
        return symbol
            .trim()
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .lowercase(Locale.US)
    }

    private fun normalizeDisplaySymbol(symbol: String): String {
        return symbol
            .trim()
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .uppercase(Locale.US)
    }
}
