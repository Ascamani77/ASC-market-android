package com.trading.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.asc.markets.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Binance Futures Trading Service for USDT-M Futures (Perpetual Contracts)
 * Supports leverage trading with proper position tracking, margin, and PnL
 */
class BinanceFuturesService(
    private val tradingMode: BinanceTradingMode = BinanceTradingMode.LIVE
) {
    companion object {
        private const val TAG = "BinanceFutures"
        private const val BASE_URL = "https://fapi.binance.com"
        private const val DEMO_URL = "https://demo-fapi.binance.com"
    }

    private val apiKey: String
        get() = if (tradingMode == BinanceTradingMode.DEMO) {
            BuildConfig.BINANCE_DEMO_API_KEY
        } else {
            BuildConfig.BINANCE_API_KEY
        }
    
    private val secretKey: String
        get() = if (tradingMode == BinanceTradingMode.DEMO) {
            BuildConfig.BINANCE_DEMO_SECRET_KEY
        } else {
            BuildConfig.BINANCE_SECRET_KEY
        }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var isRegionBlocked = false
    fun isRegionBlocked(): Boolean = isRegionBlocked
    
    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // WebSocket needs no read timeout
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getBaseUrl(): String = if (tradingMode == BinanceTradingMode.DEMO) DEMO_URL else BASE_URL
    private fun getWsBaseUrl(): String = if (tradingMode == BinanceTradingMode.DEMO) "wss://testnet.binancefuture.com" else "wss://fstream.binance.com"

    private fun generateSignature(queryString: String): String {
        if (secretKey.isBlank()) {
            throw IllegalStateException("Binance secret key not configured")
        }
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(queryString.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private suspend fun signedRequest(
        method: String,
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): JSONObject = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Binance API key not configured")
        }

        val timestamp = System.currentTimeMillis().toString()
        val allParams = params + ("timestamp" to timestamp)
        val queryString = allParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val signature = generateSignature(queryString)
        val signedQuery = "$queryString&signature=$signature"

        val url = "${getBaseUrl()}$endpoint?$signedQuery"
        Log.d(TAG, "Request: $method $endpoint")

        val request = when (method) {
            "GET" -> Request.Builder().url(url).addHeader("X-MBX-APIKEY", apiKey).get().build()
            "POST" -> {
                val body = FormBody.Builder().apply {
                    signedQuery.split("&").forEach { param ->
                        val (key, value) = param.split("=", limit = 2)
                        add(key, value)
                    }
                }.build()
                Request.Builder().url("${getBaseUrl()}$endpoint").addHeader("X-MBX-APIKEY", apiKey).post(body).build()
            }
            "DELETE" -> Request.Builder().url(url).addHeader("X-MBX-APIKEY", apiKey).delete().build()
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "{}"
            Log.d(TAG, "Response: ${response.code} - ${body.take(200)}")
            if (response.code == 451) {
                isRegionBlocked = true
                throw IllegalStateException("Binance region blocked (HTTP 451): $body")
            }
            if (!response.isSuccessful) {
                throw Exception("Binance Futures API error: ${response.code} - $body")
            }
            JSONObject(body)
        }
    }

    private suspend fun signedArrayRequest(
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): JSONArray = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Binance API key not configured")
        }

        val timestamp = System.currentTimeMillis().toString()
        val allParams = params + ("timestamp" to timestamp)
        val queryString = allParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val signature = generateSignature(queryString)
        val signedQuery = "$queryString&signature=$signature"
        val url = "${getBaseUrl()}$endpoint?$signedQuery"
        val request = Request.Builder().url(url).addHeader("X-MBX-APIKEY", apiKey).get().build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            Log.d(TAG, "Response: ${response.code} - ${body.take(200)}")
            if (response.code == 451) {
                isRegionBlocked = true
                throw IllegalStateException("Binance region blocked (HTTP 451): $body")
            }
            if (!response.isSuccessful) {
                throw Exception("Binance Futures API error: ${response.code} - $body")
            }
            JSONArray(body)
        }
    }

    // ==================== Account ====================

    data class FuturesAccountInfo(
        val totalWalletBalance: Double,
        val totalUnrealizedProfit: Double,
        val totalMarginBalance: Double,
        val availableBalance: Double,
        val maxWithdrawAmount: Double,
        val assets: List<FuturesAsset>,
        val positions: List<FuturesPosition>
    )

    data class FuturesAsset(
        val asset: String,
        val walletBalance: Double,
        val unrealizedProfit: Double,
        val marginBalance: Double,
        val availableBalance: Double
    )

    data class FuturesPosition(
        val symbol: String,
        val positionAmt: Double,
        val entryPrice: Double,
        val markPrice: Double,
        val unRealizedProfit: Double,
        val liquidationPrice: Double,
        val leverage: Int,
        val marginType: String,
        val isolatedMargin: Double,
        val positionSide: String
    )

    /**
     * Get Futures account information including balance and positions
     */
    suspend fun getAccountInfo(): FuturesAccountInfo {
        val json = signedRequest("GET", "/fapi/v2/account")
        
        val assets = json.optJSONArray("assets")?.let { arr ->
            (0 until arr.length()).map { i ->
                val a = arr.getJSONObject(i)
                FuturesAsset(
                    asset = a.optString("asset"),
                    walletBalance = a.optDouble("walletBalance", 0.0),
                    unrealizedProfit = a.optDouble("unrealizedProfit", 0.0),
                    marginBalance = a.optDouble("marginBalance", 0.0),
                    availableBalance = a.optDouble("availableBalance", 0.0)
                )
            }
        } ?: emptyList()

        val positions = json.optJSONArray("positions")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val p = arr.getJSONObject(i)
                val positionAmt = p.optDouble("positionAmt", 0.0)
                // Only include positions with non-zero amount
                if (positionAmt != 0.0) {
                    FuturesPosition(
                        symbol = p.optString("symbol"),
                        positionAmt = positionAmt,
                        entryPrice = p.optDouble("entryPrice", 0.0),
                        markPrice = p.optDouble("markPrice", 0.0),
                        unRealizedProfit = p.optDouble("unRealizedProfit", 0.0),
                        liquidationPrice = p.optDouble("liquidationPrice", 0.0),
                        leverage = p.optInt("leverage", 1),
                        marginType = p.optString("marginType", "cross"),
                        isolatedMargin = p.optDouble("isolatedMargin", 0.0),
                        positionSide = p.optString("positionSide", "BOTH")
                    )
                } else null
            }
        } ?: emptyList()

        return FuturesAccountInfo(
            totalWalletBalance = json.optDouble("totalWalletBalance", 0.0),
            totalUnrealizedProfit = json.optDouble("totalUnrealizedProfit", 0.0),
            totalMarginBalance = json.optDouble("totalMarginBalance", 0.0),
            availableBalance = json.optDouble("availableBalance", 0.0),
            maxWithdrawAmount = json.optDouble("maxWithdrawAmount", 0.0),
            assets = assets,
            positions = positions
        )
    }

    /**
     * Get position information for a specific symbol
     */
    suspend fun getPositionInfo(symbol: String? = null): List<FuturesPosition> {
        val params = if (symbol != null) mapOf("symbol" to symbol.uppercase()) else emptyMap()
        val json = signedArrayRequest("/fapi/v2/positionRisk", params)
        
        return (0 until json.length()).mapNotNull { i ->
            val p = json.getJSONObject(i)
            val positionAmt = p.optDouble("positionAmt", 0.0)
            if (positionAmt != 0.0) {
                FuturesPosition(
                    symbol = p.optString("symbol"),
                    positionAmt = positionAmt,
                    entryPrice = p.optDouble("entryPrice", 0.0),
                    markPrice = p.optDouble("markPrice", 0.0),
                    unRealizedProfit = p.optDouble("unRealizedProfit", 0.0),
                    liquidationPrice = p.optDouble("liquidationPrice", 0.0),
                    leverage = p.optInt("leverage", 1),
                    marginType = p.optString("marginType", "cross"),
                    isolatedMargin = p.optDouble("isolatedMargin", 0.0),
                    positionSide = p.optString("positionSide", "BOTH")
                )
            } else null
        }
    }

    // ==================== Order Operations ====================

    data class OrderResult(
        val orderId: Long,
        val clientOrderId: String,
        val symbol: String,
        val status: String,
        val executedQty: Double,
        val avgPrice: Double
    )

    /**
     * Place a market order
     */
    suspend fun placeMarketOrder(
        symbol: String,
        side: String,
        quantity: Double,
        positionSide: String = "BOTH",
        reduceOnly: Boolean = false
    ): OrderResult {
        val params = mutableMapOf(
            "symbol" to symbol.uppercase(),
            "side" to side.uppercase(),
            "type" to "MARKET",
            "quantity" to formatQuantity(quantity),
            "positionSide" to positionSide.uppercase(),
            "newOrderRespType" to "RESULT"
        )
        
        if (reduceOnly) {
            params["reduceOnly"] = "true"
        }

        val json = signedRequest("POST", "/fapi/v1/order", params)
        return OrderResult(
            orderId = json.optLong("orderId", 0),
            clientOrderId = json.optString("clientOrderId"),
            symbol = json.optString("symbol"),
            status = json.optString("status"),
            executedQty = json.optDouble("executedQty", 0.0),
            avgPrice = json.optDouble("avgPrice", 0.0)
        )
    }

    /**
     * Place a limit order
     */
    suspend fun placeLimitOrder(
        symbol: String,
        side: String,
        quantity: Double,
        price: Double,
        positionSide: String = "BOTH",
        timeInForce: String = "GTC"
    ): OrderResult {
        val params = mapOf(
            "symbol" to symbol.uppercase(),
            "side" to side.uppercase(),
            "type" to "LIMIT",
            "quantity" to formatQuantity(quantity),
            "price" to formatPrice(price),
            "timeInForce" to timeInForce,
            "positionSide" to positionSide.uppercase()
        )

        val json = signedRequest("POST", "/fapi/v1/order", params)
        return OrderResult(
            orderId = json.optLong("orderId", 0),
            clientOrderId = json.optString("clientOrderId"),
            symbol = json.optString("symbol"),
            status = json.optString("status"),
            executedQty = json.optDouble("executedQty", 0.0),
            avgPrice = json.optDouble("avgPrice", 0.0)
        )
    }

    /**
     * Place a stop market order
     */
    suspend fun placeStopMarketOrder(
        symbol: String,
        side: String,
        quantity: Double,
        stopPrice: Double,
        positionSide: String = "BOTH"
    ): OrderResult {
        val params = mapOf(
            "symbol" to symbol.uppercase(),
            "side" to side.uppercase(),
            "type" to "STOP_MARKET",
            "quantity" to formatQuantity(quantity),
            "stopPrice" to formatPrice(stopPrice),
            "positionSide" to positionSide.uppercase()
        )

        val json = signedRequest("POST", "/fapi/v1/order", params)
        return OrderResult(
            orderId = json.optLong("orderId", 0),
            clientOrderId = json.optString("clientOrderId"),
            symbol = json.optString("symbol"),
            status = json.optString("status"),
            executedQty = json.optDouble("executedQty", 0.0),
            avgPrice = json.optDouble("avgPrice", 0.0)
        )
    }

    /**
     * Cancel an order
     */
    suspend fun cancelOrder(symbol: String, orderId: Long): Boolean {
        return try {
            signedRequest("DELETE", "/fapi/v1/order", mapOf(
                "symbol" to symbol.uppercase(),
                "orderId" to orderId.toString()
            ))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel order: ${e.message}")
            false
        }
    }

    /**
     * Get open orders
     */
    suspend fun getOpenOrders(symbol: String? = null): List<JSONObject> {
        val params = if (symbol != null) mapOf("symbol" to symbol.uppercase()) else emptyMap()
        val json = signedArrayRequest("/fapi/v1/openOrders", params)
        val orders = mutableListOf<JSONObject>()
        for (i in 0 until json.length()) {
            orders.add(json.getJSONObject(i))
        }
        return orders
    }

    /**
     * Change leverage for a symbol
     */
    suspend fun changeLeverage(symbol: String, leverage: Int): Boolean {
        return try {
            signedRequest("POST", "/fapi/v1/leverage", mapOf(
                "symbol" to symbol.uppercase(),
                "leverage" to leverage.toString()
            ))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change leverage: ${e.message}")
            false
        }
    }

    /**
     * Change margin type (ISOLATED or CROSSED)
     */
    suspend fun changeMarginType(symbol: String, marginType: String): Boolean {
        return try {
            signedRequest("POST", "/fapi/v1/marginType", mapOf(
                "symbol" to symbol.uppercase(),
                "marginType" to marginType.uppercase()
            ))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change margin type: ${e.message}")
            false
        }
    }

    // ==================== Helpers ====================

    private fun formatQuantity(qty: Double): String {
        return if (qty >= 1.0) "%.3f".format(qty).trimEnd('0').trimEnd('.')
               else "%.6f".format(qty).trimEnd('0').trimEnd('.')
    }

    private fun formatPrice(price: Double): String {
        return "%.2f".format(price)
    }

    // ==================== User Data Stream ====================
    
    private var userDataWebSocket: WebSocket? = null
    private var listenKey: String? = null
    private val keepAliveHandler = Handler(Looper.getMainLooper())
    private var keepAliveRunnable: Runnable? = null
    
    var onPositionUpdate: ((List<FuturesPosition>) -> Unit)? = null
    var onAccountUpdate: ((FuturesAccountInfo) -> Unit)? = null
    
    suspend fun startUserDataStream(): String? = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/fapi/v1/listenKey"
            val request = Request.Builder()
                .url(url)
                .post(FormBody.Builder().build())
                .addHeader("X-MBX-APIKEY", apiKey)
                .build()
            
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                Log.d(TAG, "startUserDataStream: ${response.code} - $body")
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    listenKey = json.optString("listenKey", null)
                    listenKey?.also { Log.i(TAG, "User data stream started, listenKey: ${it.take(10)}...") }
                } else {
                    Log.e(TAG, "Failed to start user data stream: ${response.code} - $body")
                }
            }
            listenKey
        } catch (e: Exception) {
            Log.e(TAG, "Error starting user data stream: ${e.message}", e)
            null
        }
    }
    
    fun connectUserDataStream(): Boolean {
        val key = listenKey ?: return false.also { Log.w(TAG, "Cannot connect: no listenKey") }
        val url = "${getWsBaseUrl()}/private/ws/$key"
        Log.d(TAG, "Connecting to user data stream: ${url.take(50)}...")
        
        val request = Request.Builder().url(url).build()
        userDataWebSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i(TAG, "User data stream connected")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = json.optString("e", "")
                    Log.d(TAG, "User data event: $event")
                    when (event) {
                        "ACCOUNT_UPDATE" -> handleAccountUpdateEvent(json)
                        "listenKeyExpired" -> {
                            Log.w(TAG, "ListenKey expired, restarting stream")
                            // Will be handled by TradingApp reconnection
                        }
                        else -> Log.d(TAG, "Unhandled user data event: $event")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user data event: ${e.message}", e)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "User data stream closing: $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "User data stream failure: ${t.message}", t)
            }
        })
        
        // Start keep-alive timer (every 30 minutes)
        scheduleKeepAlive()
        return true
    }
    
    private fun scheduleKeepAlive() {
        keepAliveRunnable?.let { keepAliveHandler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                if (listenKey != null) {
                    Log.d(TAG, "Sending keep-alive for listenKey")
                    val url = "${getBaseUrl()}/fapi/v1/listenKey"
                    val request = Request.Builder()
                        .url(url)
                        .put(FormBody.Builder().build())
                        .addHeader("X-MBX-APIKEY", apiKey)
                        .build()
                    client.newCall(request).enqueue(object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                            Log.e(TAG, "Keep-alive failed: ${e.message}")
                        }
                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            Log.d(TAG, "Keep-alive response: ${response.code}")
                            response.close()
                        }
                    })
                }
                keepAliveHandler.postDelayed(this, 30 * 60 * 1000L)
            }
        }
        keepAliveRunnable = runnable
        keepAliveHandler.postDelayed(runnable, 30 * 60 * 1000L)
    }
    
    private fun handleAccountUpdateEvent(json: JSONObject) {
        try {
            val a = json.optJSONObject("a") ?: return
            val positions = a.optJSONArray("P") ?: JSONArray()
            val balances = a.optJSONArray("B") ?: JSONArray()
            
            val updatedPositions = mutableListOf<FuturesPosition>()
            for (i in 0 until positions.length()) {
                val p = positions.getJSONObject(i)
                val amt = p.optDouble("pa", 0.0)
                if (amt != 0.0) {
                    updatedPositions.add(
                        FuturesPosition(
                            symbol = p.optString("s"),
                            positionAmt = amt,
                            entryPrice = p.optDouble("ep", 0.0),
                            markPrice = 0.0,
                            unRealizedProfit = p.optDouble("up", 0.0),
                            liquidationPrice = 0.0,
                            leverage = 1,
                            marginType = p.optString("mt", "cross"),
                            isolatedMargin = p.optDouble("iw", 0.0),
                            positionSide = p.optString("ps", "BOTH")
                        )
                    )
                }
            }
            
            if (updatedPositions.isNotEmpty()) {
                Log.i(TAG, "ACCOUNT_UPDATE: ${updatedPositions.size} positions changed")
                onPositionUpdate?.invoke(updatedPositions)
            }
            
            // Also try to build account info from balances
            if (balances.length() > 0) {
                val usdt = (0 until balances.length()).map { balances.getJSONObject(it) }
                    .find { it.optString("a") == "USDT" }
                if (usdt != null) {
                    val accountInfo = FuturesAccountInfo(
                        totalWalletBalance = usdt.optDouble("wb", 0.0),
                        totalUnrealizedProfit = 0.0,
                        totalMarginBalance = usdt.optDouble("cw", 0.0),
                        availableBalance = 0.0,
                        maxWithdrawAmount = 0.0,
                        assets = emptyList(),
                        positions = updatedPositions
                    )
                    onAccountUpdate?.invoke(accountInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling ACCOUNT_UPDATE: ${e.message}", e)
        }
    }
    
    fun disconnectUserDataStream() {
        Log.i(TAG, "Disconnecting user data stream")
        keepAliveRunnable?.let { keepAliveHandler.removeCallbacks(it) }
        keepAliveRunnable = null
        userDataWebSocket?.close(1000, "Client disconnect")
        userDataWebSocket = null
        listenKey = null
    }

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && secretKey.isNotBlank()
    }

    suspend fun testConnection(): Boolean {
        return try {
            val url = "${getBaseUrl()}/fapi/v1/ping"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Binance Futures connection test failed: ${e.message}")
            false
        }
    }
}
