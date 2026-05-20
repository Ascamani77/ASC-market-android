package com.asc.markets.network

import android.util.Log
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.SystemTelemetry
import com.trading.app.data.Mt5Service
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
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

class CTraderBridgeClient(
    private val bridgeUrl: String,
    private val scope: CoroutineScope,
    private val onAccountUpdate: (Mt5Service.AccountInfo) -> Unit = {}
) {
    companion object {
        private const val TAG = "CTraderBridge"
        private const val RECONNECT_DELAY_MS = 5_000L
        private const val RECENT_PRICE_MAX_AGE_MS = 15_000L
    }

    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
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

    private val lastPriceAtMillisBySymbol = ConcurrentHashMap<String, Long>()
    private val pendingActions = ArrayDeque<String>()
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var targetSymbols: List<String> = emptyList()

    @Volatile
    private var manuallyDisconnected = false

    fun connect(symbols: List<String>) {
        targetSymbols = symbols
            .map(::subscriptionSymbol)
            .filter { it.isNotBlank() }
            .distinct()

        if (targetSymbols.isEmpty() && pendingActions.isEmpty()) {
            Log.w(TAG, "cTrader bridge not started: no target symbols")
            return
        }

        disconnectInternal()
        manuallyDisconnected = false

        val url = normalizedWsUrl(bridgeUrl)
        Log.i(TAG, "Connecting Pepperstone cTrader bridge at $url symbols=${targetSymbols.joinToString(",")}")
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Pepperstone cTrader bridge connected")
                SystemTelemetry.recordConnectionEvent("CTRADER", "WEBSOCKET_CONNECTED")
                _connectionState.tryEmit(ConnectionState.CONNECTED)
                if (targetSymbols.isNotEmpty()) {
                    sendSubscribe(webSocket)
                }
                flushPendingActions(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Pepperstone cTrader bridge closed: $code $reason")
                SystemTelemetry.recordConnectionEvent("CTRADER", "CONNECTION_CLOSED ($reason)")
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
                if (!manuallyDisconnected) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Pepperstone cTrader bridge failure: ${t.message}", t)
                SystemTelemetry.recordConnectionEvent("CTRADER", "CONNECTION_FAILED")
                _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
                if (!manuallyDisconnected) {
                    scheduleReconnect()
                }
            }
        })
    }

    fun hasRecentPrice(symbol: String, maxAgeMs: Long = RECENT_PRICE_MAX_AGE_MS): Boolean {
        val normalized = normalizeSymbol(symbol)
        val now = System.currentTimeMillis()
        return lastPriceAtMillisBySymbol.any { (storedSymbol, timestamp) ->
            storedSymbol == normalized && now - timestamp <= maxAgeMs
        }
    }

    private fun handleAccount(payload: JSONObject) {
        onAccountUpdate(
            Mt5Service.AccountInfo(
                balance = payload.optDouble("balance", 0.0),
                equity = payload.optDouble("equity", 0.0),
                unrealizedPnl = payload.optDouble("unrealizedPnl", 0.0),
                realizedPnl = payload.optDouble("realizedPnl", 0.0),
                margin = payload.optDouble("margin", 0.0),
                availableFunds = payload.optDouble("freeMargin", payload.optDouble("availableFunds", 0.0)),
                ordersMargin = payload.optDouble("ordersMargin", 0.0),
                marginBuffer = payload.optDouble("marginLevel", payload.optDouble("marginBuffer", 0.0))
            )
        )
    }

    fun hasAnyRecentPrice(maxAgeMs: Long = RECENT_PRICE_MAX_AGE_MS): Boolean {
        val now = System.currentTimeMillis()
        return lastPriceAtMillisBySymbol.values.any { timestamp -> now - timestamp <= maxAgeMs }
    }

    fun requestAccountStatus() {
        sendAction("get_account", emptyMap())
        if (webSocket == null) {
            connect(targetSymbols)
        }
    }

    fun disconnect() {
        manuallyDisconnected = true
        disconnectInternal()
    }

    private fun handleMessage(text: String) {
        val payload = try {
            JSONObject(text)
        } catch (e: Exception) {
            val arrayPayload = try {
                JSONArray(text)
            } catch (_: Exception) {
                Log.w(TAG, "Ignoring non-JSON cTrader bridge message: $text", e)
                return
            }
            handleTickArray(arrayPayload)
            return
        }

        val messageType = payload.optString("type").lowercase(Locale.US)
        when (messageType) {
            "tick", "quote", "price_update", "market_tick", "snapshot" -> handleTick(payload)
            "ticks", "quotes", "prices" -> handleTickArray(payload.optJSONArray(messageType))
            "account" -> handleAccount(payload)
            "status" -> Log.i(TAG, "cTrader status=${payload.optString("state")} message=${payload.optString("message")}")
            "error" -> Log.w(TAG, "cTrader bridge error: ${payload.optString("message")}")
            "ack", "pong" -> Unit
            else -> {
                if (hasTickFields(payload)) {
                    handleTick(payload)
                } else {
                    handleTickArray(payload.optJSONArray("data") ?: payload.optJSONArray("ticks") ?: payload.optJSONArray("quotes"))
                }
            }
        }
    }

    private fun handleTickArray(payload: JSONArray?) {
        payload ?: return
        for (index in 0 until payload.length()) {
            val tick = payload.optJSONObject(index) ?: continue
            handleTick(tick)
        }
    }

    private fun hasTickFields(payload: JSONObject): Boolean {
        val symbol = payload.optString("displaySymbol")
            .ifBlank { payload.optString("symbol") }
            .ifBlank { payload.optString("name") }
            .ifBlank { payload.optString("asset") }
        return symbol.isNotBlank() &&
            (payload.has("price") || payload.has("last") || payload.has("mid") || payload.has("bid") || payload.has("ask"))
    }

    private fun handleTick(payload: JSONObject) {
        val symbol = payload.optString("displaySymbol")
            .ifBlank { payload.optString("symbol") }
            .ifBlank { payload.optString("name") }
            .ifBlank { payload.optString("asset") }
        if (symbol.isBlank()) {
            return
        }

        val bid = payload.optDouble("bid", Double.NaN)
        val ask = payload.optDouble("ask", Double.NaN)
        val explicitPrice = payload.optDouble("price", Double.NaN)
        val last = payload.optDouble("last", Double.NaN)
        val mid = payload.optDouble("mid", Double.NaN)
        val price = when {
            explicitPrice.isFinite() && explicitPrice > 0.0 -> explicitPrice
            last.isFinite() && last > 0.0 -> last
            mid.isFinite() && mid > 0.0 -> mid
            bid.isFinite() && ask.isFinite() && bid > 0.0 && ask > 0.0 -> (bid + ask) / 2.0
            bid.isFinite() && bid > 0.0 -> bid
            ask.isFinite() && ask > 0.0 -> ask
            else -> return
        }

        val pair = buildPair(symbol, payload.optString("category"), price)
        lastPriceAtMillisBySymbol[normalizeSymbol(pair.symbol)] = System.currentTimeMillis()
        SystemTelemetry.recordTick("CTRADER", 1.0)
        scope.launch {
            Log.i(TAG, "Routing Pepperstone cTrader tick to MarketDataStore ${pair.symbol} ${pair.price}")
            _priceUpdates.emit(pair)
        }
    }

    private fun buildPair(symbol: String, categoryName: String, price: Double): ForexPair {
        val existing = MarketDataStore.pairSnapshot(symbol)
        val resolvedSymbol = existing?.symbol ?: symbol
        val previousPrice = existing?.price ?: price
        val change = price - previousPrice
        val changePercent = if (previousPrice != 0.0) (change / previousPrice) * 100.0 else 0.0
        val category = existing?.category ?: parseCategory(categoryName, resolvedSymbol)

        return ForexPair(
            symbol = resolvedSymbol,
            name = existing?.name ?: resolvedSymbol,
            price = price,
            change = change,
            changePercent = changePercent,
            category = category
        )
    }

    private fun parseCategory(categoryName: String, symbol: String): MarketCategory {
        val normalizedCategory = categoryName.uppercase(Locale.US)
        MarketCategory.values().firstOrNull { it.name == normalizedCategory }?.let { return it }
        return FOREX_PAIRS.firstOrNull { MarketDataStore.matchesSymbol(it.symbol, symbol) }?.category ?: MarketCategory.FOREX
    }

    private fun sendSubscribe(socket: WebSocket? = webSocket) {
        val activeSocket = socket ?: return
        activeSocket.send(
            JSONObject().apply {
                put("action", "subscribe")
                put("symbols", JSONArray(targetSymbols))
            }.toString()
        )
    }

    private fun sendAction(action: String, params: Map<String, Any>) {
        val message = JSONObject().apply {
            put("action", action)
            params.forEach { (key, value) ->
                put(key, value)
            }
        }.toString()
        val activeSocket = webSocket
        if (activeSocket?.send(message) == true) {
            return
        }
        synchronized(pendingActions) {
            pendingActions.addLast(message)
        }
    }

    private fun flushPendingActions(socket: WebSocket? = webSocket) {
        val activeSocket = socket ?: return
        synchronized(pendingActions) {
            while (pendingActions.isNotEmpty()) {
                val next = pendingActions.removeFirst()
                if (!activeSocket.send(next)) {
                    pendingActions.addFirst(next)
                    break
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true || targetSymbols.isEmpty()) {
            return
        }
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(RECONNECT_DELAY_MS)
            if (!manuallyDisconnected && targetSymbols.isNotEmpty()) {
                connect(targetSymbols)
            }
        }
    }

    private fun disconnectInternal() {
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {
        } finally {
            webSocket = null
        }
    }

    private fun normalizedWsUrl(value: String): String {
        val trimmed = value.trim().ifBlank { "127.0.0.1:8082" }
        return when {
            trimmed.startsWith("ws://", ignoreCase = true) || trimmed.startsWith("wss://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> "ws://" + trimmed.removePrefix("http://")
            trimmed.startsWith("https://", ignoreCase = true) -> "wss://" + trimmed.removePrefix("https://")
            else -> "ws://$trimmed"
        }
    }

    private fun normalizeSymbol(symbol: String): String {
        return symbol
            .trim()
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }

    private fun subscriptionSymbol(symbol: String): String {
        return symbol
            .trim()
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("_", "")
            .replace(" ", "")
    }
}
