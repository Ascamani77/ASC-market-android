package com.trading.app.data

import android.util.Log
import com.trading.app.models.Order
import com.trading.app.models.Position
import okhttp3.*
import org.json.JSONObject

/**
 * Mt5ReverseBridge handles sending execution commands from the Mobile App back to MT5.
 * This ensures a two-way synchronization where app actions are reflected on the MT5 chart.
 */
class Mt5ReverseBridge(
    private val pcIpAddress: String = "10.233.78.133",
    private val port: Int = 8081
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val TAG = "MT5_REVERSE_BRIDGE"

    fun connect() {
        val url = "ws://$pcIpAddress:$port"
        val request = Request.Builder().url(url).build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Reverse Bridge Connected to MT5")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Reverse Bridge Failure: ${t.message}")
            }
        })
    }

    fun placeOrder(order: Order) {
        val json = JSONObject().apply {
            put("action", "place_order")
            put("symbol", order.symbol)
            put("type", order.type.lowercase()) // "buy" or "sell"
            
            // Map the detailed order types to MT5-compatible strings
            val typeStr = when (order.orderType) {
                "Buy Limit" -> "limit"
                "Sell Limit" -> "limit"
                "Buy Stop" -> "stop"
                "Sell Stop" -> "stop"
                "Buy Stop Limit" -> "stoplimit"
                "Sell Stop Limit" -> "stoplimit"
                else -> order.orderType.lowercase()
            }
            put("orderType", typeStr)
            put("price", order.price.toDouble())
            if (order.stopLimitPrice != null) {
                put("stopLimitPrice", order.stopLimitPrice.toDouble())
            }
            put("volume", order.volume.toDouble())
            put("tp", (order.tp ?: 0.0).toDouble())
            put("sl", (order.sl ?: 0.0).toDouble())
            put("comment", "App Execution")
        }
        send(json.toString())
    }

    fun placePosition(position: Position) {
        val json = JSONObject().apply {
            put("action", "place_order")
            put("symbol", position.symbol)
            put("type", position.type.lowercase())
            put("orderType", "market")
            put("price", position.entryPrice.toDouble())
            put("volume", position.volume.toDouble())
            put("tp", (position.tp ?: 0.0).toDouble())
            put("sl", (position.sl ?: 0.0).toDouble())
            put("comment", "App Execution (Quick)")
        }
        send(json.toString())
    }

    fun closePosition(position: Position) {
        val json = JSONObject().apply {
            put("action", "close_position")
            // Ensure ticket is sent as a number if it's purely numeric
            try {
                put("ticket", position.id.toLong())
            } catch (e: Exception) {
                put("ticket", position.id)
            }
            put("symbol", position.symbol)
            put("volume", position.volume.toDouble())
        }
        send(json.toString())
    }

    fun modifyPosition(position: Position, tp: Float?, sl: Float?) {
        val json = JSONObject().apply {
            put("action", "modify_position")
            try {
                put("ticket", position.id.toLong())
            } catch (e: Exception) {
                put("ticket", position.id)
            }
            if (tp != null) put("tp", tp.toDouble())
            if (sl != null) put("sl", sl.toDouble())
        }
        send(json.toString())
    }

    private fun send(message: String) {
        if (webSocket?.send(message) != true) {
            Log.e(TAG, "Failed to send command to MT5: $message")
        } else {
            Log.d(TAG, "Sent to MT5: $message")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "App closing")
    }
}
