package com.asc.markets.logic

import kotlinx.coroutines.*
import okhttp3.*
import com.trading.app.data.Mt5Service
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MT5BridgeClient(
    private val bridgeUrl: String,  // e.g., "192.168.1.100:62100"
    private val scope: CoroutineScope = GlobalScope,
    private val brokerSuffix: String = "m",
    private val onAccountUpdate: (Mt5Service.AccountInfo) -> Unit = {}
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val wsUrl = "ws://${bridgeUrl.replace("http://", "").replace("https://", "")}"
    private var wsClient: OkHttpClient? = null
    private var ws: WebSocket? = null
    private val priceUpdates = mutableMapOf<String, (bid: Double, ask: Double, time: Long, volume: Int) -> Unit>()
    @Volatile
    private var pendingAccountRequest: Boolean = false
    @Volatile
    private var webSocketOpen: Boolean = false

    suspend fun getTick(symbol: String): TickData? = withContext(Dispatchers.IO) {
        try {
            val brokerSymbol = if (brokerSuffix.isNotBlank() && !symbol.endsWith(brokerSuffix, ignoreCase = true)) "$symbol$brokerSuffix" else symbol
            val request = Request.Builder()
                .url("http://${bridgeUrl}/tick?symbol=${brokerSymbol}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { parseTickData(it, symbol) }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun requestAccountStatus() {
        pendingAccountRequest = true
        if (webSocketOpen) {
            sendWebSocketMessage("get_account", "")
            pendingAccountRequest = false
            return
        }
        if (ws == null) {
            connectWebSocket()
        }
    }

    suspend fun getHistoricalBars(
        symbol: String,
        timeframe: String = "1m",
        limit: Int = 200
    ): List<OHLCBar> = withContext(Dispatchers.IO) {
        try {
            val brokerSymbol = if (brokerSuffix.isNotBlank() && !symbol.endsWith(brokerSuffix, ignoreCase = true)) "$symbol$brokerSuffix" else symbol
            val request = Request.Builder()
                .url("http://${bridgeUrl}/historical?symbol=${brokerSymbol}&timeframe=${timeframe}&limit=${limit}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { parseHistoricalBars(it) } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun subscribeToPriceUpdates(
        symbol: String,
        callback: (bid: Double, ask: Double, time: Long, volume: Int) -> Unit
    ) {
        priceUpdates[symbol] = callback
        val brokerSymbol = if (brokerSuffix.isNotBlank() && !symbol.endsWith(brokerSuffix, ignoreCase = true)) "$symbol$brokerSuffix" else symbol
        if (ws == null) {
            connectWebSocket()
        } else {
            sendWebSocketMessage("subscribe", brokerSymbol)
        }
    }

    private fun connectWebSocket() {
        try {
            // Close existing if any
            try { ws?.close(1000, "Reconnecting"); ws = null } catch (_: Exception) {}
            
            wsClient = OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(wsUrl.replace(":62100", ":62101")).build()

            ws = wsClient!!.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocketOpen = true
                    priceUpdates.keys.forEach { cleanSymbol ->
                        val brokerSymbol = if (brokerSuffix.isNotBlank() && !cleanSymbol.endsWith(brokerSuffix, ignoreCase = true)) "$cleanSymbol$brokerSuffix" else cleanSymbol
                        sendWebSocketMessage("subscribe", brokerSymbol)
                    }
                    if (pendingAccountRequest) {
                        sendWebSocketMessage("get_account", "")
                        pendingAccountRequest = false
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type", "").lowercase()
                        if (type == "account") {
                            onAccountUpdate(
                                Mt5Service.AccountInfo(
                                    balance = json.optDouble("balance", 0.0),
                                    equity = json.optDouble("equity", 0.0),
                                    unrealizedPnl = json.optDouble("unrealizedPnl", 0.0),
                                    realizedPnl = json.optDouble("realizedPnl", 0.0),
                                    margin = json.optDouble("margin", 0.0),
                                    availableFunds = json.optDouble("availableFunds", json.optDouble("freeMargin", 0.0)),
                                    ordersMargin = json.optDouble("ordersMargin", 0.0),
                                    marginBuffer = json.optDouble("marginBuffer", json.optDouble("marginLevel", 0.0))
                                )
                            )
                            return
                        }
                        val rawSymbol = json.optString("symbol") ?: return
                        val cleanSymbol = if (brokerSuffix.isNotBlank() && rawSymbol.endsWith(brokerSuffix, ignoreCase = true)) {
                            rawSymbol.removeSuffix(brokerSuffix)
                        } else {
                            rawSymbol
                        }
                        val bid = json.optDouble("bid", 0.0)
                        val ask = json.optDouble("ask", 0.0)
                        val time = json.optLong("time", 0L)
                        val volume = json.optInt("volume", 0)
                        
                        if (bid > 0 && ask > 0) {
                            priceUpdates[cleanSymbol]?.invoke(bid, ask, time, volume)
                        }
                    } catch (e: Exception) { 
                        android.util.Log.e("MT5Bridge", "Error parsing WS message", e)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    android.util.Log.e("MT5Bridge", "WebSocket failure: ${t.message}")
                    webSocketOpen = false
                    this@MT5BridgeClient.ws = null
                    scope.launch { 
                        delay(5000)
                        if (!manuallyDisconnected) connectWebSocket() 
                    }
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    android.util.Log.i("MT5Bridge", "WebSocket closed: $reason")
                    webSocketOpen = false
                    this@MT5BridgeClient.ws = null
                }
            })
        } catch (e: Exception) { 
            android.util.Log.e("MT5Bridge", "Connection error", e)
        }
    }

    private var manuallyDisconnected = false

    fun disconnect() {
        manuallyDisconnected = true
        try {
            ws?.close(1000, "Normal closure")
        } catch (_: Exception) {
        } finally {
            ws = null
            webSocketOpen = false
        }
    }

    private fun sendWebSocketMessage(action: String, symbol: String) {
        ws?.send(JSONObject().apply {
            put("action", action)
            if (symbol.isNotBlank()) {
                put("symbol", symbol)
            }
        }.toString())
    }

    private fun parseTickData(json: String, symbol: String): TickData? {
        return try {
            val obj = JSONObject(json)
            TickData(symbol, obj.optDouble("bid", 0.0), obj.optDouble("ask", 0.0), obj.optLong("time", 0L), obj.optInt("volume", 0))
        } catch (e: Exception) { null }
    }

    private fun parseHistoricalBars(json: String): List<OHLCBar> {
        return try {
            val obj = JSONObject(json)
            val barsArray = obj.optJSONArray("bars") ?: return emptyList()
            (0 until barsArray.length()).map { i ->
                val bar = barsArray.getJSONObject(i)
                OHLCBar(bar.getLong("time"), bar.getDouble("open"), bar.getDouble("high"), bar.getDouble("low"), bar.getDouble("close"), bar.optInt("volume", 0))
            }
        } catch (e: Exception) { emptyList() }
    }
}

data class TickData(val symbol: String, val bid: Double, val ask: Double, val time: Long, val volume: Int)
data class OHLCBar(val time: Long, val open: Double, val high: Double, val low: Double, val close: Double, val volume: Int)
