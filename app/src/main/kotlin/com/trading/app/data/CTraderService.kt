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
    private val onConnectionStatusUpdate: (Boolean) -> Unit = {}
) {
    private val tag = "CTraderService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Configuration from BuildConfig
    private val hostType = BuildConfig.CTRADER_HOST_TYPE
    private val clientId = BuildConfig.CTRADER_CLIENT_ID
    private val clientSecret = BuildConfig.CTRADER_CLIENT_SECRET
    private val accessToken = BuildConfig.CTRADER_ACCESS_TOKEN
    private val accountId = BuildConfig.CTRADER_ACCOUNT_ID
    
    // API endpoints
    private val baseUrl = if (hostType == "live") {
        "https://live.ctraderapi.com"
    } else {
        "https://demo.ctraderapi.com"
    }
    
    private val wsUrl = if (hostType == "live") {
        "wss://live.ctraderapi.com"
    } else {
        "wss://demo.ctraderapi.com"
    }
    
    fun connect() {
        if (accessToken.isBlank()) {
            Log.e(tag, "Access token not configured")
            return
        }
        
        Log.i(tag, "Connecting to cTrader $hostType...")
        
        scope.launch {
            try {
                // First, get account info via REST API
                fetchAccountInfo()
                
                // Then connect WebSocket for live updates
                connectWebSocket()
                
            } catch (e: Exception) {
                Log.e(tag, "Connection failed: ${e.message}", e)
                onConnectionStatusUpdate(false)
            }
        }
    }
    
    private suspend fun fetchAccountInfo() = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/v2/accounts"
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && body != null) {
                Log.d(tag, "Account info received: ${body.take(200)}")
                parseAccountInfo(body)
            } else {
                Log.e(tag, "Failed to fetch account info: ${response.code} - $body")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching account info: ${e.message}", e)
        }
    }
    
    private fun parseAccountInfo(json: String) {
        try {
            val jsonObj = JSONObject(json)
            // Parse account data based on cTrader API response format
            // This is a placeholder - adjust based on actual API response
            
            Log.d(tag, "Account info parsed successfully")
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing account info: ${e.message}", e)
        }
    }
    
    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "WebSocket connected")
                isConnected = true
                onConnectionStatusUpdate(true)
                
                // Subscribe to account updates
                subscribeToAccount()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(tag, "Message received: ${text.take(200)}")
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
    
    private fun subscribeToAccount() {
        if (accountId.isBlank()) {
            Log.w(tag, "Account ID not configured, skipping subscription")
            return
        }
        
        // Subscribe to account updates
        val subscribeMessage = JSONObject().apply {
            put("type", "SUBSCRIBE")
            put("accountId", accountId)
        }
        
        webSocket?.send(subscribeMessage.toString())
        Log.d(tag, "Subscribed to account: $accountId")
    }
    
    fun subscribe(symbol: String) {
        if (!isConnected) {
            Log.w(tag, "Not connected, cannot subscribe to $symbol")
            return
        }
        
        val subscribeMessage = JSONObject().apply {
            put("type", "SUBSCRIBE_SYMBOL")
            put("symbol", symbol)
        }
        
        webSocket?.send(subscribeMessage.toString())
        Log.d(tag, "Subscribed to symbol: $symbol")
    }
    
    private fun handleMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type", "")
            
            when (type) {
                "QUOTE" -> handleQuoteUpdate(json)
                "POSITION" -> handlePositionUpdate(json)
                "ACCOUNT" -> handleAccountUpdate(json)
                "ERROR" -> {
                    val error = json.optString("message", "Unknown error")
                    Log.e(tag, "Error from server: $error")
                }
                else -> {
                    Log.d(tag, "Unknown message type: $type")
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling message: ${e.message}", e)
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
            // Parse position data based on cTrader API format
            // This is a placeholder - adjust based on actual API response
            
            Log.d(tag, "Position update received")
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing position: ${e.message}", e)
        }
    }
    
    private fun handleAccountUpdate(json: JSONObject) {
        try {
            val balance = json.optDouble("balance", 0.0)
            val equity = json.optDouble("equity", 0.0)
            val margin = json.optDouble("margin", 0.0)
            
            val accountInfo = Mt5Service.AccountInfo(
                balance = balance,
                equity = equity,
                margin = margin,
                availableFunds = (equity - margin),
                unrealizedPnl = equity - balance,
                realizedPnl = 0.0,
                marginBuffer = if (equity > 0) ((equity - margin) / equity * 100.0) else 100.0,
                ordersMargin = 0.0
            )
            
            onAccountUpdate(accountInfo)
            
        } catch (e: Exception) {
            Log.e(tag, "Error parsing account: ${e.message}", e)
        }
    }
    
    fun disconnect() {
        Log.i(tag, "Disconnecting from cTrader...")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        scope.cancel()
    }
    
    fun isConnected(): Boolean = isConnected
}
