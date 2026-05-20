package com.trading.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData
import com.asc.markets.data.SystemTelemetry
import okhttp3.*
import org.json.JSONObject
import java.util.Locale

class BinanceService(
    private val tradingMode: BinanceTradingMode = BinanceTradingMode.LIVE,
    private val marketType: BinanceMarketType = BinanceMarketType.FUTURES,
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val symbols = mutableSetOf<String>()
    private var connectionStartTime: Long = 0
    private val reconnectRunnable = Runnable { 
        if (!isClosing && !isRegionBlocked) {
            Log.i("BinanceService", "24-hour reconnection triggered")
            connect() 
        }
    }
    private val streamBaseUrl: String
        get() = when (tradingMode) {
            BinanceTradingMode.DEMO -> "wss://demo-fstream.binance.com"
            BinanceTradingMode.LIVE -> "wss://fstream.binance.com"
        }
    private val restBaseUrl: String
        get() = when (tradingMode) {
            BinanceTradingMode.DEMO -> "https://demo-fapi.binance.com"
            BinanceTradingMode.LIVE -> "https://fapi.binance.com"
        }

    fun connect() {
        if (isRegionBlocked) {
            Log.w("BinanceService", "Skipping connect: region blocked")
            return
        }
        if (symbols.isEmpty()) return
        isClosing = false
        
        // Binance Futures uses different stream format
        // For Futures: wss://fstream.binance.com/stream?streams=btcusdt@aggTrade/btcusdt@miniTicker
        val streams = symbols.flatMap { symbol ->
            listOf("${symbol}@aggTrade", "${symbol}@miniTicker")
        }.joinToString("/")
        
        val url = "$streamBaseUrl/stream?streams=$streams"
        
        Log.d("BinanceService", "Connecting to: $url")
        Log.d("BinanceService", "Market type: FUTURES, Trading mode: $tradingMode")
        Log.d("BinanceService", "Symbols: ${symbols.joinToString(", ")}")
        
        webSocket?.close(1000, "Reconnecting")
        
        val request = Request.Builder().url(url).build()
        
        // Cache for storing latest quote data per symbol
        val quoteCache = mutableMapOf<String, SymbolQuote>()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val stream = root.optString("stream", "")
                    val data = root.optJSONObject("data") ?: return
                    
                    when {
                        stream.endsWith("@aggTrade") -> {
                            // Fast price updates (every trade)
                            val symbol = data.optString("s")
                            val price = data.optString("p", "0").toFloatOrNull() ?: 0f
                            val quantity = data.optString("q", "0").toFloatOrNull() ?: 0f
                            val time = data.optLong("T")
                            
                            // Get or create quote
                            val existing = quoteCache[symbol] ?: SymbolQuote(
                                name = symbol,
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
                                time = time
                            )
                            
                            // Update with new price
                            val updated = existing.copy(
                                lastPrice = price,
                                bid = price,
                                ask = price,
                                volume = quantity,
                                time = time
                            )
                            
                            quoteCache[symbol] = updated
                            
                            mainHandler.post {
                                onQuoteUpdate(updated)
                            }
                        }
                        
                        stream.endsWith("@miniTicker") -> {
                            // 24h statistics (once per second)
                            val symbol = data.optString("s")
                            
                            fun parsePrice(key: String, default: Float = 0f): Float {
                                val str = data.optString(key, "")
                                return if (str.isNotEmpty()) str.toFloatOrNull() ?: default else default
                            }
                            
                            val lastPrice = parsePrice("c")
                            val change = parsePrice("p")
                            val changePercent = parsePrice("P")
                            val open = parsePrice("o")
                            val high = parsePrice("h")
                            val low = parsePrice("l")
                            val volume = parsePrice("v")
                            val time = data.optLong("E")
                            
                            val quote = SymbolQuote(
                                name = symbol,
                                lastPrice = lastPrice,
                                change = change,
                                changePercent = changePercent,
                                open = open,
                                high = high,
                                low = low,
                                prevClose = lastPrice - change,
                                bid = lastPrice,
                                ask = lastPrice,
                                volume = volume,
                                time = time
                            )
                            
                            quoteCache[symbol] = quote
                            
                            val latency = System.currentTimeMillis() - quote.time
                            SystemTelemetry.recordTick("BINANCE", latency.toDouble().coerceAtLeast(1.0))
                            
                            mainHandler.post {
                                onQuoteUpdate(quote)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BinanceService", "Error parsing Binance message: ${e.message}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BinanceService", "Binance WebSocket Failure: ${t.message}", t)
                if (t.message?.contains("451") == true || t.message?.contains("restricted") == true) {
                    isRegionBlocked = true
                    Log.e("BinanceService", "Region block detected on WebSocket failure")
                    return
                }
                mainHandler.postDelayed({ if (!isClosing && !isRegionBlocked) connect() }, 5000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("BinanceService", "Binance WebSocket Closed: code=$code reason=$reason")
                if (code == 1006 || reason.isBlank()) {
                    isRegionBlocked = true
                    Log.e("BinanceService", "Region block detected on WebSocket close")
                }
                SystemTelemetry.recordConnectionEvent("BINANCE", "CONNECTION_CLOSED ($reason)")
            }
            
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("BinanceService", "Binance WebSocket CONNECTED (fast mode)")
                SystemTelemetry.recordConnectionEvent("BINANCE", "WEBSOCKET_CONNECTED")
                connectionStartTime = System.currentTimeMillis()
                
                // Schedule reconnection after 23.5 hours (before 24-hour limit)
                mainHandler.removeCallbacks(reconnectRunnable)
                mainHandler.postDelayed(reconnectRunnable, 23 * 60 * 60 * 1000L + 30 * 60 * 1000L)
            }
        })
    }

    private var isClosing = false
    private var isRegionBlocked = false

    fun isRegionBlocked(): Boolean = isRegionBlocked

    fun subscribe(symbol: String) {
        val binanceSymbol = symbol.lowercase(Locale.US)
        if (symbols.add(binanceSymbol)) {
            connect() // Reconnect with new stream
        }
    }

    fun subscribeSymbols(symbolsToSubscribe: List<String>) {
        if (isRegionBlocked) {
            Log.w("BinanceService", "Skipping subscribeSymbols: region blocked")
            return
        }
        val normalized = symbolsToSubscribe
            .asSequence()
            .map { it.trim().lowercase(Locale.US) }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        if (normalized.toSet() == symbols && webSocket != null) return
        symbols.clear()
        symbols.addAll(normalized)
        if (symbols.isEmpty()) {
            webSocket?.close(1000, "No Binance symbols")
            webSocket = null
        } else {
            connect()
        }
    }

    fun streamActiveSymbol(symbol: String) {
        if (isRegionBlocked) {
            Log.w("BinanceService", "Skipping streamActiveSymbol: region blocked")
            return
        }
        val binanceSymbol = symbol.lowercase(Locale.US)
        if (symbols.size == 1 && symbols.contains(binanceSymbol) && webSocket != null) return
        symbols.clear()
        symbols.add(binanceSymbol)
        connect()
    }

    fun stopActiveStream() {
        symbols.clear()
        webSocket?.close(1000, "Stopping active stream")
        webSocket = null
    }

    fun fetchHistory(symbol: String, timeframe: String, endTime: Long? = null) {
        val binanceInterval = when (timeframe.lowercase()) {
            "1m" -> "1m"
            "5m" -> "5m"
            "15m" -> "15m"
            "30m" -> "30m"
            "1h" -> "1h"
            "4h" -> "4h"
            "1d", "d" -> "1d"
            "1w", "w" -> "1w"
            "1m_month", "m_month" -> "1M"
            else -> "1h"
        }
        
        val klinesEndpoint = "/fapi/v1/klines"
        var url = "$restBaseUrl$klinesEndpoint?symbol=${symbol.uppercase()}&interval=$binanceInterval&limit=500"
        if (endTime != null) {
            url += "&endTime=${endTime * 1000L}"
        }
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("BinanceService", "Failed to fetch history: ${e.message}")
                mainHandler.post { onHistoryUpdate(symbol, emptyList()) }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    if (response.code == 451) {
                        isRegionBlocked = true
                        Log.e("BinanceService", "Region block detected: HTTP 451. $body")
                        mainHandler.post { onHistoryUpdate(symbol, emptyList()) }
                        return
                    }
                    if (!response.isSuccessful) {
                        Log.e("BinanceService", "History HTTP ${response.code}: $body")
                        mainHandler.post { onHistoryUpdate(symbol, emptyList()) }
                        return
                    }
                    val jsonArray = org.json.JSONArray(body)
                    val history = mutableListOf<OHLCData>()
                    for (i in 0 until jsonArray.length()) {
                        val k = jsonArray.getJSONArray(i)
                        history.add(OHLCData(
                            time = k.getLong(0) / 1000L,
                            open = k.getString(1).toFloat(),
                            high = k.getString(2).toFloat(),
                            low = k.getString(3).toFloat(),
                            close = k.getString(4).toFloat(),
                            volume = k.getString(5).toFloat()
                        ))
                    }
                    mainHandler.post {
                        onHistoryUpdate(symbol, history)
                    }
                } catch (e: Exception) {
                    Log.e("BinanceService", "Error parsing history: ${e.message}")
                    mainHandler.post { onHistoryUpdate(symbol, emptyList()) }
                }
            }
        })
    }

    fun disconnect() {
        isClosing = true
        mainHandler.removeCallbacks(reconnectRunnable)
        webSocket?.close(1000, "App closing")
        webSocket = null
    }
}
