package com.trading.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.SystemTelemetry
import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class PepperstoneChartService(
    private val host: String,
    private val port: Int,
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webSocket: WebSocket? = null
    private var activeSymbol: String? = null
    private var activeSymbols: Set<String> = emptySet()
    private var activeTimeframe: String = "1h"
    private var isClosing = false
    private var reconnectScheduled = false

    fun connect() {
        isClosing = false
        val url = "ws://${NetworkConfig.normalizedHost(host)}:$port"
        webSocket?.close(1000, "Reconnecting")
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                SystemTelemetry.recordConnectionEvent("PEPPERSTONE_CHART", "WEBSOCKET_CONNECTED")
                if (activeSymbols.isNotEmpty()) {
                    subscribe(activeSymbols, activeTimeframe)
                } else {
                    activeSymbol?.let { subscribe(it, activeTimeframe) }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Pepperstone chart WebSocket failure: ${t.message}")
                SystemTelemetry.recordConnectionEvent("PEPPERSTONE_CHART", "CONNECTION_FAILED")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                SystemTelemetry.recordConnectionEvent("PEPPERSTONE_CHART", "CONNECTION_CLOSED ($reason)")
                if (!isClosing) scheduleReconnect()
            }
        })
    }

    fun streamActiveSymbol(symbol: String, timeframe: String = "1h", count: Int = 500) {
        activeSymbol = normalizeSymbol(symbol)
        activeSymbols = setOfNotNull(activeSymbol)
        activeTimeframe = timeframe
        if (webSocket == null) connect() else subscribe(activeSymbol ?: return, timeframe, count)
    }

    fun subscribeSymbols(symbols: List<String>, timeframe: String = "1h") {
        val normalized = symbols
            .asSequence()
            .map(::normalizeSymbol)
            .filter { it.isNotEmpty() }
            .distinctBy { it.uppercase(Locale.US) }
            .toSet()
        if (normalized == activeSymbols && webSocket != null) return
        val removed = activeSymbols - normalized
        if (removed.isNotEmpty()) {
            send(JSONObject().apply {
                put("action", "unsubscribe")
                put("symbols", JSONArray(removed.toList()))
                put("symbol", removed.first())
            })
        }
        activeSymbols = normalized
        activeSymbol = normalized.firstOrNull()
        activeTimeframe = timeframe
        if (normalized.isEmpty()) return
        if (webSocket == null) connect() else subscribe(normalized, timeframe)
    }

    fun stopActiveStream() {
        val symbolsToStop = activeSymbols.ifEmpty { activeSymbol?.let { setOf(it) } ?: emptySet() }
        if (symbolsToStop.isNotEmpty()) {
            send(JSONObject().apply {
                put("action", "unsubscribe")
                put("symbols", JSONArray(symbolsToStop.toList()))
                put("symbol", symbolsToStop.first())
            })
        }
        activeSymbol = null
        activeSymbols = emptySet()
    }

    fun fetchHistory(symbol: String, timeframe: String, endTime: Long? = null, count: Int = 500) {
        subscribe(normalizeSymbol(symbol), timeframe, count, endTime)
    }

    fun disconnect() {
        isClosing = true
        mainHandler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "App closing")
        webSocket = null
    }

    private fun subscribe(symbol: String, timeframe: String, count: Int = 500, endTime: Long? = null) {
        subscribe(setOf(symbol), timeframe, count, endTime)
    }

    private fun subscribe(symbols: Collection<String>, timeframe: String, count: Int = 500, endTime: Long? = null) {
        val cleanSymbols = symbols
            .map(::normalizeSymbol)
            .filter { it.isNotEmpty() }
            .distinctBy { it.uppercase(Locale.US) }
        if (cleanSymbols.isEmpty()) return
        send(JSONObject().apply {
            put("action", "subscribe")
            put("symbols", JSONArray(cleanSymbols))
            put("symbol", cleanSymbols.first())
            put("timeframe", timeframe)
            put("count", count)
            put("limit", count)
            if (endTime != null) {
                put("end_time", endTime)
                put("endTime", endTime)
            }
        })
    }

    private fun send(payload: JSONObject) {
        if (webSocket?.send(payload.toString()) != true && !isClosing) {
            connect()
        }
    }

    private fun handleMessage(text: String) {
        val payload = try {
            JSONObject(text)
        } catch (_: Exception) {
            val array = try { JSONArray(text) } catch (_: Exception) { return }
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(::handleTick)
            }
            return
        }

        val type = payload.optString("type", payload.optString("event", "")).lowercase(Locale.US)
        when (type) {
            "history", "historical", "bars", "candles" -> handleHistory(payload)
            "tick", "quote", "price_update", "market_tick", "snapshot" -> handleTick(payload)
            "ticks", "quotes", "prices" -> {
                val array = payload.optJSONArray(type) ?: payload.optJSONArray("data") ?: return
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::handleTick)
                }
            }
            else -> {
                if (hasTickFields(payload)) {
                    handleTick(payload)
                } else {
                    val array = payload.optJSONArray("data") ?: payload.optJSONArray("ticks") ?: payload.optJSONArray("quotes") ?: return
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let(::handleTick)
                    }
                }
            }
        }
    }

    private fun handleHistory(payload: JSONObject) {
        val symbol = normalizeSymbol(payload.optString("symbol", payload.optString("name", activeSymbol.orEmpty())))
        val data = payload.optJSONArray("data") ?: payload.optJSONArray("bars") ?: payload.optJSONArray("candles") ?: return
        val history = mutableListOf<OHLCData>()
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val close = firstFinite(item, "close", "c", "price", "last", "bid").toFloat()
            if (!close.isFinite() || close <= 0f) continue
            val openRaw = firstFinite(item, "open", "o").toFloat()
            val highRaw = firstFinite(item, "high", "h").toFloat()
            val lowRaw = firstFinite(item, "low", "l").toFloat()
            val open = openRaw.takeIf { it.isFinite() && it > 0f } ?: close
            val high = maxOf(highRaw.takeIf { it.isFinite() && it > 0f } ?: close, open, close)
            val low = minOf(lowRaw.takeIf { it.isFinite() && it > 0f } ?: close, open, close)
            val time = normalizeEpochSeconds(firstLong(item, "time", "timestamp", "t"))
            if (time <= 0L) continue
            history.add(OHLCData(time, open, high, low, close, firstFinite(item, "volume", "tick_volume", "vol", "v").toFloat().takeIf { it.isFinite() } ?: 0f))
        }
        mainHandler.post { onHistoryUpdate(symbol, history.sortedBy(OHLCData::time).distinctBy(OHLCData::time)) }
    }

    private fun handleTick(payload: JSONObject) {
        val symbol = normalizeSymbol(
            payload.optString("displaySymbol")
                .ifBlank { payload.optString("symbol") }
                .ifBlank { payload.optString("name") }
                .ifBlank { payload.optString("asset") }
        )
        if (symbol.isBlank()) return
        val bid = payload.optDouble("bid", Double.NaN)
        val ask = payload.optDouble("ask", Double.NaN)
        val price = when {
            payload.optDouble("price", Double.NaN).isFinite() && payload.optDouble("price") > 0.0 -> payload.optDouble("price")
            payload.optDouble("last", Double.NaN).isFinite() && payload.optDouble("last") > 0.0 -> payload.optDouble("last")
            payload.optDouble("mid", Double.NaN).isFinite() && payload.optDouble("mid") > 0.0 -> payload.optDouble("mid")
            bid.isFinite() && ask.isFinite() && bid > 0.0 && ask > 0.0 -> (bid + ask) / 2.0
            bid.isFinite() && bid > 0.0 -> bid
            ask.isFinite() && ask > 0.0 -> ask
            else -> return
        }.toFloat()
        val time = firstLong(payload, "time", "timestamp", "ts").takeIf { it > 0L } ?: System.currentTimeMillis()
        val quote = SymbolQuote(
            name = symbol,
            lastPrice = price,
            change = 0f,
            changePercent = 0f,
            open = price,
            high = price,
            low = price,
            prevClose = price,
            bid = bid.toFloat().takeIf { it.isFinite() && it > 0f } ?: price,
            ask = ask.toFloat().takeIf { it.isFinite() && it > 0f } ?: price,
            volume = payload.optDouble("volume", 0.0).toFloat().takeIf { it.isFinite() } ?: 0f,
            time = time
        )
        SystemTelemetry.recordTick("PEPPERSTONE_CHART", 1.0)
        mainHandler.post { onQuoteUpdate(quote) }
    }

    private fun hasTickFields(payload: JSONObject): Boolean {
        return payload.has("symbol") && (payload.has("price") || payload.has("last") || payload.has("mid") || payload.has("bid") || payload.has("ask"))
    }

    private fun scheduleReconnect() {
        if (isClosing || reconnectScheduled) return
        reconnectScheduled = true
        mainHandler.postDelayed({
            reconnectScheduled = false
            if (!isClosing) connect()
        }, 3000L)
    }

    private fun normalizeSymbol(symbol: String): String {
        return symbol.trim().uppercase(Locale.US).replace("/", "").replace("-", "").replace("_", "").replace(" ", "")
    }

    private fun firstFinite(obj: JSONObject, vararg fields: String): Double {
        for (field in fields) {
            if (!obj.has(field)) continue
            val value = obj.optDouble(field, Double.NaN)
            if (value.isFinite()) return value
        }
        return Double.NaN
    }

    private fun firstLong(obj: JSONObject, vararg fields: String): Long {
        for (field in fields) {
            if (!obj.has(field)) continue
            val value = obj.opt(field)
            val parsed = when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            if (parsed > 0L) return parsed
        }
        return 0L
    }

    private fun normalizeEpochSeconds(timestamp: Long): Long {
        return when {
            timestamp <= 0L -> 0L
            timestamp >= 1_000_000_000_000L -> timestamp / 1000L
            else -> timestamp
        }
    }

    private companion object {
        private const val TAG = "PepperstoneChartService"
    }
}
