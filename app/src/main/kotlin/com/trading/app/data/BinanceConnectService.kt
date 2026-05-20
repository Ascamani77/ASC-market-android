package com.trading.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Independent Binance Connect Service
 * Uses public Binance WebSocket for real-time chart data
 * Completely independent from other Binance services
 */
class BinanceConnectService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val tag = "BinanceConnectService"
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webSocket: WebSocket? = null
    private var activeSymbol: String? = null
    private var activeQuoteSymbols: Set<String> = emptySet()
    private var activeInterval: String = "1h"
    private var activeTradeCandle: OHLCData? = null
    private var isClosing = false
    private var reconnectRunnable: Runnable? = null

    // Public Binance WebSocket URL (Spot)
    private val wsUrl = "wss://stream.binance.com:9443"
    private val restUrl = "https://api.binance.com"

    fun streamActiveSymbol(symbol: String, timeframe: String) {
        val normalized = normalizeSymbol(symbol)
        val interval = mapTimeframeToInterval(timeframe)
        Log.d(tag, "Starting stream for symbol: $normalized interval: $interval")
        
        isClosing = false
        activeSymbol = normalized
        activeQuoteSymbols = emptySet()
        activeInterval = interval
        activeTradeCandle = null
        connectWebSocket(normalized, interval)
    }

    fun subscribeSymbols(symbolsToSubscribe: List<String>) {
        val normalized = symbolsToSubscribe
            .asSequence()
            .map(::normalizeSymbol)
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        if (normalized.isEmpty()) {
            stopActiveStream()
            return
        }
        val normalizedSet = normalized.toSet()
        if (activeQuoteSymbols == normalizedSet && webSocket != null) return
        isClosing = false
        activeSymbol = null
        activeQuoteSymbols = normalizedSet
        activeTradeCandle = null
        connectQuoteWebSocket(normalized)
    }

    private fun connectWebSocket(symbol: String, interval: String) {
        if (isClosing) return
        
        val lowerSymbol = symbol.lowercase(Locale.US)
        val url = "$wsUrl/stream?streams=${lowerSymbol}@trade/${lowerSymbol}@ticker/${lowerSymbol}@kline_$interval"
        
        Log.d(tag, "Connecting to: $url")
        
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        webSocket?.close(1000, "Reconnecting")
        
        val request = Request.Builder().url(url).build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connected for $symbol")
                reconnectRunnable = Runnable {
                    if (!isClosing && activeSymbol == symbol && activeInterval == interval) {
                        connectWebSocket(symbol, interval)
                    }
                }
                reconnectRunnable?.let {
                    mainHandler.postDelayed(it, 23 * 60 * 60 * 1000L + 30 * 60 * 1000L)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val data = json.optJSONObject("data") ?: return
                    val stream = json.optString("stream", "")
                    val eventType = data.optString("e", "")

                    when {
                        eventType == "serverShutdown" -> scheduleReconnect(symbol, interval)
                        stream.contains("@trade") || eventType == "trade" -> handleTrade(data, symbol)
                        stream.contains("@ticker") || eventType == "24hrTicker" -> handleTicker(data, symbol)
                        stream.contains("@kline") || eventType == "kline" -> handleKline(data, symbol)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket error: ${t.message}")
                if (!isClosing) {
                    scheduleReconnect(symbol, interval)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed: $reason")
                if (!isClosing && activeSymbol == symbol && code != 1000) {
                    scheduleReconnect(symbol, interval)
                }
            }
        })
    }

    private fun connectQuoteWebSocket(symbols: List<String>) {
        if (isClosing) return
        val streams = symbols.flatMap { symbol ->
            val lowerSymbol = symbol.lowercase(Locale.US)
            listOf("${lowerSymbol}@trade", "${lowerSymbol}@ticker")
        }.joinToString("/")
        val url = "$wsUrl/stream?streams=$streams"

        Log.d(tag, "Connecting quote stream to: $url")

        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        webSocket?.close(1000, "Reconnecting")

        val request = Request.Builder().url(url).build()
        val expectedSymbols = symbols.toSet()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectRunnable = Runnable {
                    if (!isClosing && activeQuoteSymbols == expectedSymbols) {
                        connectQuoteWebSocket(symbols)
                    }
                }
                reconnectRunnable?.let {
                    mainHandler.postDelayed(it, 23 * 60 * 60 * 1000L + 30 * 60 * 1000L)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val data = json.optJSONObject("data") ?: return
                    val stream = json.optString("stream", "")
                    val eventType = data.optString("e", "")
                    val symbol = data.optString("s").ifBlank {
                        stream.substringBefore("@").uppercase(Locale.US)
                    }

                    when {
                        eventType == "serverShutdown" -> scheduleQuoteReconnect(symbols, expectedSymbols)
                        stream.contains("@trade") || eventType == "trade" -> handleTrade(data, symbol, false)
                        stream.contains("@ticker") || eventType == "24hrTicker" -> handleTicker(data, symbol)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing quote message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "Quote WebSocket error: ${t.message}")
                if (!isClosing) scheduleQuoteReconnect(symbols, expectedSymbols)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Quote WebSocket closed: $reason")
                if (!isClosing && activeQuoteSymbols == expectedSymbols && code != 1000) {
                    scheduleQuoteReconnect(symbols, expectedSymbols)
                }
            }
        })
    }

    private fun handleTrade(data: JSONObject, symbol: String, emitCandle: Boolean = true) {
        try {
            val resolvedSymbol = data.optString("s").ifBlank { symbol }
            val price = data.optString("p", "0").toFloatOrNull() ?: return
            val quantity = data.optString("q", "0").toFloatOrNull() ?: 0f
            val time = data.optLong("T", System.currentTimeMillis())
            val candle = if (emitCandle) updateTradeCandle(time, price, quantity) else null
            
            val quote = SymbolQuote(
                name = resolvedSymbol,
                lastPrice = price,
                change = 0f,
                changePercent = 0f,
                open = price,
                high = price,
                low = price,
                prevClose = price,
                bid = price,
                ask = price,
                volume = quantity,
                spread = 0f,
                time = time
            )
            
            mainHandler.post {
                onQuoteUpdate(quote)
                candle?.let { onHistoryUpdate(resolvedSymbol, listOf(it)) }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling trade: ${e.message}")
        }
    }

    private fun handleTicker(data: JSONObject, symbol: String) {
        try {
            val resolvedSymbol = data.optString("s").ifBlank { symbol }
            val close = data.optString("c", "0").toFloatOrNull() ?: return
            val open = data.optString("o", "0").toFloatOrNull() ?: close
            val high = data.optString("h", "0").toFloatOrNull() ?: close
            val low = data.optString("l", "0").toFloatOrNull() ?: close
            val volume = data.optString("v", "0").toFloatOrNull() ?: 0f
            val prevClose = data.optString("x", "0").toFloatOrNull() ?: open
            val priceChange = data.optString("p", "0").toFloatOrNull() ?: 0f
            val priceChangePercent = data.optString("P", "0").toFloatOrNull() ?: 0f
            val time = data.optLong("E", System.currentTimeMillis())
            
            val quote = SymbolQuote(
                name = resolvedSymbol,
                lastPrice = close,
                change = priceChange,
                changePercent = priceChangePercent,
                open = open,
                high = high,
                low = low,
                prevClose = prevClose,
                bid = close,
                ask = close,
                volume = volume,
                spread = 0f,
                time = time
            )
            
            mainHandler.post { onQuoteUpdate(quote) }
        } catch (e: Exception) {
            Log.e(tag, "Error handling ticker: ${e.message}")
        }
    }

    private fun handleKline(data: JSONObject, symbol: String) {
        try {
            val kline = data.optJSONObject("k") ?: return
            val candle = OHLCData(
                time = kline.optLong("t") / 1000L,
                open = kline.optString("o", "0").toFloatOrNull() ?: return,
                high = kline.optString("h", "0").toFloatOrNull() ?: return,
                low = kline.optString("l", "0").toFloatOrNull() ?: return,
                close = kline.optString("c", "0").toFloatOrNull() ?: return,
                volume = kline.optString("v", "0").toFloatOrNull() ?: 0f
            )
            activeTradeCandle = candle
            mainHandler.post { onHistoryUpdate(symbol, listOf(candle)) }
        } catch (e: Exception) {
            Log.e(tag, "Error handling kline: ${e.message}")
        }
    }

    fun fetchHistory(symbol: String, timeframe: String, endTime: Long? = null) {
        val normalized = normalizeSymbol(symbol)
        Log.d(tag, "Fetching history for $normalized, timeframe: $timeframe")
        
        Thread {
            try {
                val interval = mapTimeframeToInterval(timeframe)
                val limit = 500
                
                val urlBuilder = StringBuilder("$restUrl/api/v3/klines?symbol=$normalized&interval=$interval&limit=$limit")
                if (endTime != null) {
                    val endTimeMillis = if (endTime < 10_000_000_000L) endTime * 1000L else endTime
                    urlBuilder.append("&endTime=$endTimeMillis")
                }
                
                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@Thread
                
                if (!response.isSuccessful) {
                    Log.e(tag, "History fetch failed: ${response.code} - $body")
                    return@Thread
                }
                
                val candles = parseKlines(body)
                Log.d(tag, "Fetched ${candles.size} candles for $normalized")
                mainHandler.post { onHistoryUpdate(normalized, candles) }
                
            } catch (e: Exception) {
                Log.e(tag, "Error fetching history: ${e.message}")
                mainHandler.post { onHistoryUpdate(normalized, emptyList()) }
            }
        }.start()
    }

    private fun parseKlines(json: String): List<OHLCData> {
        try {
            val array = JSONArray(json)
            val candles = mutableListOf<OHLCData>()
            
            for (i in 0 until array.length()) {
                val kline = array.getJSONArray(i)
                val candle = OHLCData(
                    time = kline.getLong(0) / 1000, // Convert to seconds
                    open = kline.getString(1).toFloat(),
                    high = kline.getString(2).toFloat(),
                    low = kline.getString(3).toFloat(),
                    close = kline.getString(4).toFloat(),
                    volume = kline.getString(5).toFloatOrNull() ?: 0f
                )
                candles.add(candle)
            }
            
            return candles
        } catch (e: Exception) {
            Log.e(tag, "Error parsing klines: ${e.message}")
            return emptyList()
        }
    }

    private fun mapTimeframeToInterval(timeframe: String): String {
        val raw = timeframe.trim()
        if (raw == "1M" || raw == "M") return "1M"
        return when (raw.lowercase(Locale.US)) {
            "1s" -> "1s"
            "1", "1m" -> "1m"
            "3", "3m" -> "3m"
            "5", "5m" -> "5m"
            "15", "15m" -> "15m"
            "30", "30m" -> "30m"
            "60", "1h" -> "1h"
            "120", "2h" -> "2h"
            "240", "4h" -> "4h"
            "360", "6h" -> "6h"
            "480", "8h" -> "8h"
            "720", "12h" -> "12h"
            "d", "1d" -> "1d"
            "3d" -> "3d"
            "w", "1w" -> "1w"
            else -> "1h"
        }
    }

    private fun intervalToSeconds(interval: String): Long {
        return when (interval) {
            "1s" -> 1L
            "1m" -> 60L
            "3m" -> 3 * 60L
            "5m" -> 5 * 60L
            "15m" -> 15 * 60L
            "30m" -> 30 * 60L
            "1h" -> 60 * 60L
            "2h" -> 2 * 60 * 60L
            "4h" -> 4 * 60 * 60L
            "6h" -> 6 * 60 * 60L
            "8h" -> 8 * 60 * 60L
            "12h" -> 12 * 60 * 60L
            "1d" -> 24 * 60 * 60L
            "3d" -> 3 * 24 * 60 * 60L
            "1w" -> 7 * 24 * 60 * 60L
            "1M" -> 30 * 24 * 60 * 60L
            else -> 60 * 60L
        }
    }

    private fun updateTradeCandle(tradeTime: Long, price: Float, quantity: Float): OHLCData? {
        if (!price.isFinite() || price <= 0f) return null
        val timestampSeconds = if (tradeTime >= 1_000_000_000_000L) tradeTime / 1000L else tradeTime
        if (timestampSeconds <= 0L) return null
        val intervalSeconds = intervalToSeconds(activeInterval)
        val candleTime = if (intervalSeconds > 0L) {
            (timestampSeconds / intervalSeconds) * intervalSeconds
        } else {
            timestampSeconds
        }
        val previous = activeTradeCandle
        val updated = if (previous == null || previous.time != candleTime) {
            OHLCData(
                time = candleTime,
                open = price,
                high = price,
                low = price,
                close = price,
                volume = quantity.coerceAtLeast(0f)
            )
        } else {
            previous.copy(
                high = maxOf(previous.high, price),
                low = minOf(previous.low, price),
                close = price,
                volume = previous.volume + quantity.coerceAtLeast(0f)
            )
        }
        activeTradeCandle = updated
        return updated
    }

    private fun normalizeSymbol(symbol: String): String {
        return symbol.trim()
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }

    fun stopActiveStream() {
        Log.d(tag, "Stopping active stream")
        isClosing = true
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = null
        webSocket?.close(1000, "User requested")
        webSocket = null
        activeSymbol = null
        activeQuoteSymbols = emptySet()
        activeTradeCandle = null
    }

    fun disconnect() {
        Log.d(tag, "Disconnecting")
        stopActiveStream()
    }

    private fun scheduleReconnect(symbol: String, interval: String) {
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = Runnable {
            if (!isClosing && activeSymbol == symbol && activeInterval == interval) {
                connectWebSocket(symbol, interval)
            }
        }
        reconnectRunnable?.let { mainHandler.postDelayed(it, 5000L) }
    }

    private fun scheduleQuoteReconnect(symbols: List<String>, expectedSymbols: Set<String>) {
        reconnectRunnable?.let { mainHandler.removeCallbacks(it) }
        reconnectRunnable = Runnable {
            if (!isClosing && activeQuoteSymbols == expectedSymbols) {
                connectQuoteWebSocket(symbols)
            }
        }
        reconnectRunnable?.let { mainHandler.postDelayed(it, 5000L) }
    }
}
