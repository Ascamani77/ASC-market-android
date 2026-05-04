package com.trading.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData
import okhttp3.*
import org.json.JSONObject
import java.util.Locale

class BinanceService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val symbols = mutableSetOf<String>()

    fun connect() {
        if (symbols.isEmpty()) return
        val streams = symbols.joinToString("/") { "$it@ticker" }
        val url = "wss://stream.binance.com:9443/stream?streams=$streams"
        
        webSocket?.close(1000, "Reconnecting")
        
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val data = root.optJSONObject("data") ?: return
                    val symbol = data.optString("s")
                    
                    val quote = SymbolQuote(
                        name = symbol, // e.g., BTCUSDT
                        lastPrice = data.optString("c").toFloat(),
                        change = data.optString("p").toFloat(),
                        changePercent = data.optString("P").toFloat(),
                        open = data.optString("o").toFloat(),
                        high = data.optString("h").toFloat(),
                        low = data.optString("l").toFloat(),
                        prevClose = (data.optString("c").toDouble() - data.optString("p").toDouble()).toFloat(),
                        bid = data.optString("b").toFloat(),
                        ask = data.optString("a").toFloat(),
                        volume = data.optString("v").toFloat(),
                        time = data.optLong("E")
                    )
                    
                    mainHandler.post {
                        onQuoteUpdate(quote)
                    }
                } catch (e: Exception) {
                    Log.e("BinanceService", "Error parsing Binance message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BinanceService", "Binance WebSocket Failure: ${t.message}")
                mainHandler.postDelayed({ if (!isClosing) connect() }, 5000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("BinanceService", "Binance WebSocket Closed: $reason")
            }
        })
    }

    private var isClosing = false

    fun subscribe(symbol: String) {
        val binanceSymbol = symbol.lowercase(Locale.US)
        if (symbols.add(binanceSymbol)) {
            connect() // Reconnect with new stream
        }
    }

    fun streamActiveSymbol(symbol: String) {
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
        
        var url = "https://api.binance.com/api/v3/klines?symbol=${symbol.uppercase()}&interval=$binanceInterval&limit=500"
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
        webSocket?.close(1000, "App closing")
        webSocket = null
    }
}
