package com.trading.app.data

import android.util.Log
import com.asc.markets.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Binance Trading Service for executing real trades on Binance.
 * Supports futures trading with market, limit, and stop-limit orders.
 */
class BinanceTradingService(
    private val tradingMode: BinanceTradingMode = BinanceTradingMode.LIVE
) {
    companion object {
        private const val TAG = "BinanceTrading"
        private const val BASE_URL = "https://api.binance.com"
        private const val DEMO_URL = "https://demo-api.binance.com"
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

    private fun getBaseUrl(): String = if (tradingMode == BinanceTradingMode.DEMO) DEMO_URL else BASE_URL

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
            if (!response.isSuccessful) {
                throw Exception("Binance API error: ${response.code} - $body")
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
            if (!response.isSuccessful) {
                throw Exception("Binance API error: ${response.code} - $body")
            }
            JSONArray(body)
        }
    }

    private suspend fun publicRequest(endpoint: String): JSONObject = withContext(Dispatchers.IO) {
        val url = "${getBaseUrl()}$endpoint"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "{}"
            if (!response.isSuccessful) {
                throw Exception("Binance API error: ${response.code}")
            }
            JSONObject(body)
        }
    }

    // ==================== Account ====================

    data class AccountInfo(
        val makerCommission: Double,
        val takerCommission: Double,
        val canTrade: Boolean,
        val canWithdraw: Boolean,
        val canDeposit: Boolean,
        val balances: List<Balance>
    )

    data class Balance(
        val asset: String,
        val free: Double,
        val locked: Double
    )

    suspend fun getAccountInfo(): AccountInfo {
        val json = signedRequest("GET", "/api/v3/account")
        return AccountInfo(
            makerCommission = json.optDouble("makerCommission", 0.0) / 100.0,
            takerCommission = json.optDouble("takerCommission", 0.0) / 100.0,
            canTrade = json.optBoolean("canTrade", false),
            canWithdraw = json.optBoolean("canWithdraw", false),
            canDeposit = json.optBoolean("canDeposit", false),
            balances = json.optJSONArray("balances")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val b = arr.getJSONObject(i)
                    Balance(
                        asset = b.optString("asset"),
                        free = b.optDouble("free", 0.0),
                        locked = b.optDouble("locked", 0.0)
                    )
                }
            } ?: emptyList()
        )
    }

    // ==================== Order Operations ====================

    data class OrderResult(
        val orderId: Long,
        val clientOrderId: String,
        val symbol: String,
        val status: String,
        val executedQty: Double,
        val avgPrice: Double?
    )

    /**
     * Place a market order
     * @param symbol Trading pair (e.g., "BTCUSDT")
     * @param side "BUY" or "SELL"
     * @param quantity Amount to buy/sell
     * @param quoteOrderQty For BUY orders, amount in quote asset (e.g., buy $100 worth of BTC)
     */
    suspend fun placeMarketOrder(
        symbol: String,
        side: String,
        quantity: Double? = null,
        quoteOrderQty: Double? = null
    ): OrderResult {
        val params = mutableMapOf(
            "symbol" to symbol.uppercase(),
            "side" to side.uppercase(),
            "type" to "MARKET"
        )

        if (quantity != null) {
            params["quantity"] = formatQuantity(quantity)
        } else if (quoteOrderQty != null && side.uppercase() == "BUY") {
            params["quoteOrderQty"] = formatQuantity(quoteOrderQty)
        } else {
            throw IllegalArgumentException("Must specify quantity or quoteOrderQty")
        }

        val json = signedRequest("POST", "/api/v3/order", params)
        return OrderResult(
            orderId = json.optLong("orderId", 0),
            clientOrderId = json.optString("clientOrderId"),
            symbol = json.optString("symbol"),
            status = json.optString("status"),
            executedQty = json.optDouble("executedQty", 0.0),
            avgPrice = json.optJSONArray("fills")?.let { arr ->
                if (arr.length() > 0) {
                    var totalQty = 0.0
                    var totalValue = 0.0
                    for (i in 0 until arr.length()) {
                        val fill = arr.getJSONObject(i)
                        val qty = fill.optDouble("qty", 0.0)
                        val price = fill.optDouble("price", 0.0)
                        totalQty += qty
                        totalValue += qty * price
                    }
                    if (totalQty > 0) totalValue / totalQty else null
                } else null
            }
        )
    }

    /**
     * Place a limit order
     * @param symbol Trading pair
     * @param side "BUY" or "SELL"
     * @param quantity Amount to buy/sell
     * @param price Limit price
     * @param timeInForce "GTC" (Good Till Cancel), "IOC" (Immediate or Cancel), "FOK" (Fill or Kill)
     */
    suspend fun placeLimitOrder(
        symbol: String,
        side: String,
        quantity: Double,
        price: Double,
        timeInForce: String = "GTC"
    ): OrderResult {
        val params = mapOf(
            "symbol" to symbol.uppercase(),
            "side" to side.uppercase(),
            "type" to "LIMIT",
            "timeInForce" to timeInForce,
            "quantity" to formatQuantity(quantity),
            "price" to formatPrice(price)
        )

        val json = signedRequest("POST", "/api/v3/order", params)
        return OrderResult(
            orderId = json.optLong("orderId", 0),
            clientOrderId = json.optString("clientOrderId"),
            symbol = json.optString("symbol"),
            status = json.optString("status"),
            executedQty = json.optDouble("executedQty", 0.0),
            avgPrice = null
        )
    }

    /**
     * Place a stop-limit order
     */
    suspend fun placeStopLimitOrder(
        symbol: String,
        side: String,
        quantity: Double,
        price: Double,
        stopPrice: Double,
        timeInForce: String = "GTC"
    ): OrderResult {
        val params = mapOf(
            "symbol" to symbol.uppercase(),
            "side" to side.uppercase(),
            "type" to "STOP_LOSS_LIMIT",
            "timeInForce" to timeInForce,
            "quantity" to formatQuantity(quantity),
            "price" to formatPrice(price),
            "stopPrice" to formatPrice(stopPrice)
        )

        val json = signedRequest("POST", "/api/v3/order", params)
        return OrderResult(
            orderId = json.optLong("orderId", 0),
            clientOrderId = json.optString("clientOrderId"),
            symbol = json.optString("symbol"),
            status = json.optString("status"),
            executedQty = json.optDouble("executedQty", 0.0),
            avgPrice = null
        )
    }

    /**
     * Cancel an order
     */
    suspend fun cancelOrder(symbol: String, orderId: Long): Boolean {
        return try {
            signedRequest("DELETE", "/api/v3/order", mapOf(
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
        val json = signedArrayRequest("/api/v3/openOrders", params)
        val orders = mutableListOf<JSONObject>()
        for (i in 0 until json.length()) {
            orders.add(json.getJSONObject(i))
        }
        return orders
    }

    /**
     * Get order status
     */
    suspend fun getOrder(symbol: String, orderId: Long): JSONObject {
        return signedRequest("GET", "/api/v3/order", mapOf(
            "symbol" to symbol.uppercase(),
            "orderId" to orderId.toString()
        ))
    }

    /**
     * Get account trade history
     * @param symbol Trading pair (required)
     * @param limit Number of trades to return (default 500, max 1000)
     */
    suspend fun getMyTrades(symbol: String, limit: Int = 500): List<JSONObject> {
        val params = mapOf(
            "symbol" to symbol.uppercase(),
            "limit" to limit.toString()
        )
        val json = signedArrayRequest("/api/v3/myTrades", params)
        val trades = mutableListOf<JSONObject>()
        for (i in 0 until json.length()) {
            trades.add(json.getJSONObject(i))
        }
        return trades
    }

    /**
     * Get all orders (open, filled, cancelled) for a symbol
     * @param symbol Trading pair (required)
     * @param limit Number of orders to return (default 500, max 1000)
     */
    suspend fun getAllOrders(symbol: String, limit: Int = 500): List<JSONObject> {
        val params = mapOf(
            "symbol" to symbol.uppercase(),
            "limit" to limit.toString()
        )
        val json = signedArrayRequest("/api/v3/allOrders", params)
        val orders = mutableListOf<JSONObject>()
        for (i in 0 until json.length()) {
            orders.add(json.getJSONObject(i))
        }
        return orders
    }

    // ==================== Market Data ====================

    /**
     * Get current price
     */
    suspend fun getPrice(symbol: String): Double {
        val json = publicRequest("/api/v3/ticker/price?symbol=${symbol.uppercase()}")
        return json.optDouble("price", 0.0)
    }

    /**
     * Get exchange info (lot sizes, tick sizes, etc.)
     */
    suspend fun getExchangeInfo(symbol: String? = null): JSONObject {
        val endpoint = if (symbol != null) {
            "/api/v3/exchangeInfo?symbol=${symbol.uppercase()}"
        } else {
            "/api/v3/exchangeInfo"
        }
        return publicRequest(endpoint)
    }

    // ==================== Helpers ====================

    private fun formatQuantity(qty: Double): String {
        return if (qty >= 1.0) "%.6f".format(qty).trimEnd('0').trimEnd('.')
               else "%.8f".format(qty).trimEnd('0').trimEnd('.')
    }

    private fun formatPrice(price: Double): String {
        return "%.8f".format(price).trimEnd('0').trimEnd('.')
    }

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && secretKey.isNotBlank()
    }

    // Test connectivity
    suspend fun testConnection(): Boolean {
        return try {
            publicRequest("/api/v3/ping")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Binance connection test failed: ${e.message}")
            false
        }
    }
}
