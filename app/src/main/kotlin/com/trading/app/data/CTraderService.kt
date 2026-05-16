package com.trading.app.data

import android.util.Log
import com.asc.markets.BuildConfig
import com.trading.app.components.SymbolQuote
import com.trading.app.models.Position
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * cTrader Pepperstone Service
 * Connects to cTrader Open API for live trading data
 */
class CTraderService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit = {},
    private val onPositionsUpdate: (List<Position>) -> Unit = {},
    private val onAccountUpdate: (Mt5Service.AccountInfo?) -> Unit = {},
    private val onBalanceHistoryUpdate: (List<com.trading.app.models.BalanceRecord>) -> Unit = {},
    private val onConnectionStatusUpdate: (Boolean) -> Unit = {}
) {
    private val tag = "CTraderService"
    private fun createScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var scope = createScope()
    private val currentPositions = linkedMapOf<String, Position>()
    private var cumulativeRealizedPnl: Double = 0.0
    private val balanceHistory = mutableListOf<com.trading.app.models.BalanceRecord>()
    
    // Configuration from BuildConfig
    private val bridgeHost = BuildConfig.CTRADER_BRIDGE_HOST
    private val bridgePort = BuildConfig.CTRADER_BRIDGE_PORT
    
    // Bridge WebSocket URL
    private val wsUrl = "ws://$bridgeHost:$bridgePort"
    
    fun connect() {
        if (!scope.isActive) {
            scope = createScope()
        }
        
        Log.i(tag, "Connecting to cTrader bridge at $wsUrl...")
        
        scope.launch {
            try {
                // Connect to the bridge WebSocket
                connectWebSocket()
                
            } catch (e: Exception) {
                Log.e(tag, "Connection failed: ${e.message}", e)
                onConnectionStatusUpdate(false)
            }
        }
    }
    
    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "WebSocket connected to cTrader bridge")
                isConnected = true
                onConnectionStatusUpdate(true)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(tag, "Bridge message received: ${text.take(200)}")
                handleMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket failure: ${t.message}", t)
                isConnected = false
                onConnectionStatusUpdate(false)
                
                // Attempt reconnection after delay
                scope.launch {
                    delay(5000)
                    if (!isConnected) {
                        Log.i(tag, "Attempting to reconnect...")
                        connectWebSocket()
                    }
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(tag, "WebSocket closed: $code - $reason")
                isConnected = false
                onConnectionStatusUpdate(false)
            }
        })
    }
    
    fun subscribe(symbol: String) {
        if (!isConnected) {
            Log.w(tag, "Not connected, cannot subscribe to $symbol")
            return
        }
        
        val subscribeMessage = JSONObject().apply {
            put("action", "subscribe")
            put("symbol", symbol)
        }
        
        webSocket?.send(subscribeMessage.toString())
        Log.d(tag, "Subscribed to symbol: $symbol")
    }
    
    fun placeMarketOrder(
        symbol: String,
        side: String,
        volume: Double,
        stopLoss: Double? = null,
        takeProfit: Double? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isConnected) {
            Log.w(tag, "Not connected, cannot place order")
            onResult(false, "Not connected to bridge")
            return
        }
        
        scope.launch {
            try {
                val orderMessage = JSONObject().apply {
                    put("action", "place_order")
                    put("symbol", symbol)
                    put("side", side.lowercase())
                    put("volume", volume)
                    if (stopLoss != null && stopLoss > 0) {
                        put("stopLoss", stopLoss)
                    }
                    if (takeProfit != null && takeProfit > 0) {
                        put("takeProfit", takeProfit)
                    }
                }
                
                webSocket?.send(orderMessage.toString())
                Log.d(tag, "Placed $side order: $symbol, volume=$volume, SL=$stopLoss, TP=$takeProfit")
                onResult(true, "Order sent")
                
            } catch (e: Exception) {
                Log.e(tag, "Error placing order: ${e.message}", e)
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun closePosition(
        positionId: String,
        volume: Double,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isConnected) {
            Log.w(tag, "Not connected, cannot close position")
            onResult(false, "Not connected to bridge")
            return
        }

        val parsedPositionId = positionId.toLongOrNull()
        if (parsedPositionId == null || parsedPositionId <= 0L || volume <= 0.0) {
            Log.w(tag, "Invalid close request: positionId=$positionId volume=$volume")
            onResult(false, "Invalid close parameters")
            return
        }

        scope.launch {
            try {
                val closeMessage = JSONObject().apply {
                    put("action", "close_position")
                    put("positionId", parsedPositionId)
                    put("volume", volume)
                }

                webSocket?.send(closeMessage.toString())
                Log.d(tag, "Requested close for positionId=$parsedPositionId volume=$volume")
                onResult(true, "Close request sent")
            } catch (e: Exception) {
                Log.e(tag, "Error closing position: ${e.message}", e)
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }
    
    private fun handleMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type", "")
            
            Log.d(tag, "Received message type: $type")
            
            when (type) {
                "tick" -> handleQuoteUpdate(json)
                "ACCOUNT" -> {
                    Log.d(tag, "ACCOUNT message received: $message")
                    handleAccountUpdate(json)
                }
                "POSITIONS" -> {
                    Log.d(tag, "POSITIONS message received")
                    handlePositionsUpdate(json)
                }
                "ORDER_UPDATE" -> {
                    val status = json.optString("status", "")
                    val orderId = json.optString("orderId", "")
                    val profit = json.optDouble("profit", Double.NaN)
                    if (!profit.isNaN() && status == "executed") {
                        cumulativeRealizedPnl += profit
                        Log.i(tag, "Order update: $status (ID: $orderId) profit=$profit, cumulativeRealizedPnl=$cumulativeRealizedPnl")
                        val balanceBefore = json.optDouble("balanceBefore", 0.0)
                        val balanceAfter = json.optDouble("balanceAfter", balanceBefore + profit)
                        balanceHistory.add(
                            com.trading.app.models.BalanceRecord(
                                time = System.currentTimeMillis(),
                                balanceBefore = balanceBefore,
                                balanceAfter = balanceAfter,
                                realizedPnl = profit,
                                action = "PEPPERSTONE_CLOSE"
                            )
                        )
                        onBalanceHistoryUpdate(balanceHistory.toList())
                    } else {
                        Log.i(tag, "Order update: $status (ID: $orderId)")
                    }
                }
                "history" -> {
                    // Historical candle data - could be handled if needed
                    Log.d(tag, "Historical data received for ${json.optString("symbol")}")
                }
                "status" -> {
                    val state = json.optString("state", "")
                    val msg = json.optString("message", "")
                    Log.i(tag, "Bridge status: $state - $msg")
                }
                "error" -> {
                    val error = json.optString("message", "Unknown error")
                    Log.e(tag, "Error from bridge: $error")
                }
                else -> {
                    Log.d(tag, "Unknown message type: $type")
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling message: ${e.message}", e)
        }
    }
    
    private fun handlePositionsUpdate(json: JSONObject) {
        try {
            Log.d(tag, "handlePositionsUpdate called with JSON: $json")
            
            val positionsArray = json.optJSONArray("positions")
            if (positionsArray == null) {
                Log.w(tag, "No positions array in message")
                return
            }
            
            Log.d(tag, "Positions array length: ${positionsArray.length()}")
            
            val updatedPositions = mutableListOf<Position>()
            for (i in 0 until positionsArray.length()) {
                val posJson = positionsArray.optJSONObject(i) ?: continue
                
                val id = posJson.optString("id", "")
                val symbol = posJson.optString("symbol", "")
                val side = posJson.optString("side", "buy")
                val volume = posJson.optDouble("volume", 0.0).toFloat()
                val entryPrice = posJson.optDouble("entryPrice", 0.0).toFloat()
                
                Log.d(tag, "Position $i: id=$id, symbol=$symbol, side=$side, volume=$volume, entryPrice=$entryPrice")
                
                if (id.isNotBlank() && symbol.isNotBlank()) {
                    updatedPositions.add(
                        Position(
                            id = id,
                            symbol = symbol,
                            type = side,
                            volume = volume,
                            entryPrice = entryPrice,
                            time = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            Log.d(tag, "Parsed ${updatedPositions.size} positions, calling onPositionsUpdate")
            
            currentPositions.clear()
            updatedPositions.forEach { currentPositions[it.id] = it }
            onPositionsUpdate(currentPositions.values.toList())
            
            Log.d(tag, "Updated ${updatedPositions.size} positions, onPositionsUpdate callback completed")
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing positions: ${e.message}", e)
        }
    }
    
    private fun handleQuoteUpdate(json: JSONObject) {
        try {
            val symbol = json.optString("symbol", "")
            val bid = json.optDouble("bid", 0.0)
            val ask = json.optDouble("ask", 0.0)
            val lastPrice = (bid + ask) / 2.0
            
            val quote = SymbolQuote(
                name = symbol,
                lastPrice = lastPrice.toFloat(),
                change = 0f,
                changePercent = 0f,
                open = 0f,
                high = 0f,
                low = 0f,
                prevClose = 0f,
                bid = bid.toFloat(),
                ask = ask.toFloat(),
                volume = 0f,
                time = System.currentTimeMillis()
            )
            
            onQuoteUpdate(quote)
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing quote: ${e.message}", e)
        }
    }
    
    private fun handlePositionUpdate(json: JSONObject) {
        try {
            val positionId = json.optString("positionId")
                .ifBlank { json.optString("id") }
                .ifBlank { json.optString("tradeId") }
            val symbol = json.optString("symbol").ifBlank { json.optString("symbolName") }
            if (positionId.isBlank() || symbol.isBlank()) {
                return
            }

            val side = json.optString("tradeSide")
                .ifBlank { json.optString("side") }
                .ifBlank { json.optString("type") }
                .lowercase()
                .let { if (it.contains("sell")) "sell" else "buy" }
            val entryPrice = json.optDouble("price")
                .takeIf { it > 0.0 }
                ?: json.optDouble("entryPrice")
                    .takeIf { it > 0.0 }
                ?: json.optDouble("openPrice")
            val volume = json.optDouble("volume")
                .takeIf { it > 0.0 }
                ?: json.optDouble("quantity")
                    .takeIf { it > 0.0 }
                ?: json.optDouble("lotSize")
            val updatedPosition = Position(
                id = positionId,
                symbol = symbol,
                type = side,
                entryPrice = entryPrice.toFloat(),
                volume = volume.toFloat(),
                time = json.optLong("utcTimestampInMillis", System.currentTimeMillis()),
                tp = json.optDouble("takeProfit").takeIf { it > 0.0 }?.toFloat(),
                sl = json.optDouble("stopLoss").takeIf { it > 0.0 }?.toFloat()
            )

            val status = json.optString("status").lowercase()
            if (status.contains("closed")) {
                currentPositions.remove(positionId)
            } else {
                currentPositions[positionId] = updatedPosition
            }
            onPositionsUpdate(currentPositions.values.toList())
            Log.d(tag, "Position update received")
        } catch (e: Exception) {
            Log.e(tag, "Error parsing position: ${e.message}", e)
        }
    }
    
    private fun handleAccountUpdate(json: JSONObject) {
        try {
            Log.d(tag, "handleAccountUpdate called with JSON: $json")
            
            val balance = json.optDouble("balance", 0.0)
            val equity = json.optDouble("equity", 0.0)
            val margin = json.optDouble("margin", 0.0)
            val bridgeRealizedPnl = json.optDouble("realizedPnl", 0.0)
            if (bridgeRealizedPnl != 0.0) {
                cumulativeRealizedPnl += bridgeRealizedPnl
                Log.d(tag, "Accumulated realizedPnl from bridge: $bridgeRealizedPnl, total=$cumulativeRealizedPnl")
            }
            
            Log.d(tag, "Parsed values - balance: $balance, equity: $equity, margin: $margin")
            
            val accountInfo = Mt5Service.AccountInfo(
                balance = balance,
                equity = equity,
                margin = margin,
                availableFunds = (equity - margin),
                unrealizedPnl = equity - balance,
                realizedPnl = cumulativeRealizedPnl,
                marginBuffer = if (equity > 0) ((equity - margin) / equity * 100.0) else 100.0,
                ordersMargin = 0.0
            )
            
            Log.d(tag, "Calling onAccountUpdate with accountInfo: $accountInfo")
            onAccountUpdate(accountInfo)
            Log.d(tag, "onAccountUpdate callback completed")
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing account: ${e.message}", e)
        }
    }
    
    fun disconnect() {
        Log.i(tag, "Disconnecting from cTrader...")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        currentPositions.clear()
        cumulativeRealizedPnl = 0.0
        balanceHistory.clear()
        onPositionsUpdate(emptyList())
        onAccountUpdate(null)
        onBalanceHistoryUpdate(emptyList())
        onConnectionStatusUpdate(false)
        scope.coroutineContext.cancelChildren()
    }
    
    fun isConnected(): Boolean = isConnected
}
