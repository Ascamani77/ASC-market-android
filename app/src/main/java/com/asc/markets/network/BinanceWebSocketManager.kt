package com.asc.markets.network

import android.util.Log
import com.asc.markets.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.Locale
import java.util.concurrent.TimeUnit

class BinanceWebSocketManager(
    private val scope: CoroutineScope,
    // Make Redis config injectable; defaults work for emulator/local dev
    private val redisHost: String = "10.0.2.2",
    private val redisPort: Int = 6379,
    private val redisPassword: String? = null,
    private val redisUseSsl: Boolean = false,
    private val streamName: String = "market.ticks.stream",
    private val fieldName: String = "data",
    // Optional backend proxy URL to POST ticks to (recommended for mobile). If null/empty, falls back to direct Redis XADD.
    private val backendUrl: String? = null,
    // Optional API key to include as x-api-key header when posting to backend
    private val publishApiKey: String? = null
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(3, TimeUnit.MINUTES)
        .build()

    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    // Redis pool (configured via constructor)
    private var jedisPool: JedisPool? = null

    init {
        setupRedis()
    }

    private fun setupRedis() {
        try {
            val poolConfig = JedisPoolConfig().apply {
                maxTotal = 10
                maxIdle = 5
                minIdle = 1
                jmxEnabled = false
            }
            jedisPool = if (redisPassword.isNullOrEmpty()) {
                JedisPool(poolConfig, redisHost, redisPort)
            } else {
                // last boolean param = ssl
                JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword, redisUseSsl)
            }
            Log.i("BinanceWS", "Redis Pool initialized at $redisHost:$redisPort (ssl=$redisUseSsl)")
        } catch (e: Exception) {
            Log.e("BinanceWS", "Failed to initialize Redis Pool: ${e.message}")
        }
    }

    private fun publishTickToRedis(tick: MarketTick) {
        // If backend proxy is configured, POST to it; otherwise fall back to Redis XADD
        if (!backendUrl.isNullOrBlank()) {
            publishTickToProxy(tick)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                jedisPool?.resource?.use { jedis ->
                    val tickJson = json.encodeToString(tick)
                    val params = mapOf(fieldName to tickJson)
                    // Use XADD to push to the stream
                    jedis.xadd(streamName, redis.clients.jedis.params.XAddParams.xAddParams(), params)
                    Log.d("BinanceWS", "XADD to $streamName: $tickJson")
                }
            } catch (e: Exception) {
                Log.e("BinanceWS", "Redis XADD Error: ${e.message}")
            }
        }
    }

    private fun publishTickToProxy(tick: MarketTick) {
        scope.launch(Dispatchers.IO) {
            try {
                val tickJson = json.encodeToString(tick)
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = tickJson.toRequestBody(mediaType)
                val reqBuilder = Request.Builder()
                    .url(backendUrl!! + "/publish_tick")
                    .post(body)

                if (!publishApiKey.isNullOrBlank()) {
                    reqBuilder.addHeader("x-api-key", publishApiKey)
                }

                val request = reqBuilder.build()
                val resp = client.newCall(request).execute()
                resp.use { r ->
                    if (!r.isSuccessful) {
                        Log.e("BinanceWS", "Proxy publish failed: ${r.code} ${r.message}")
                    } else {
                        Log.d("BinanceWS", "Proxied tick to $backendUrl: $tickJson")
                    }
                }
            } catch (e: Exception) {
                Log.e("BinanceWS", "Proxy publish error: ${e.message}")
            }
        }
    }

    private val _priceUpdates = MutableSharedFlow<ForexPair>(extraBufferCapacity = 100)
    val priceUpdates: SharedFlow<ForexPair> = _priceUpdates

    private val symbols = mutableSetOf<String>()
    private val baseWsUrl = "wss://stream.binance.com:9443"

    private val _accountStatus = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val accountStatus: SharedFlow<String> = _accountStatus

    fun connect(initialSymbols: List<String>) {
        val nextSymbols = initialSymbols
            .map { it.replace("/", "").lowercase() }
            .filter { it.isNotBlank() && it.endsWith("usdt") } // Only allow USDT pairs for Binance
            .toSet()

        if (nextSymbols.isEmpty()) {
            return
        }

        symbols.clear()
        symbols.addAll(nextSymbols)
        disconnect()

        val streams = symbols.joinToString("/") { "$it@ticker" }
        Log.i("BinanceWS", "Connecting to Binance streams for symbols=${symbols.joinToString(",")}")
        val url = "$baseWsUrl/stream?streams=$streams"

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    if (text.contains("\"result\"") || text.contains("\"error\"")) {
                         // Likely a response to a request (logon, account.status)
                         Log.d("BinanceWS", "Response received: $text")
                         scope.launch { _accountStatus.emit(text) }
                         return
                    }

                    val data = JSONObject(text).optJSONObject("data") ?: return
                    val symbol = data.optString("s")
                    val displaySymbol = formatSymbol(symbol)
                    val price = data.optString("c").toDouble()
                    val change = data.optString("p").toDouble()
                    val changePercent = data.optString("P").toDouble()
                    val bid = data.optString("b").toDoubleOrNull() ?: price
                    val ask = data.optString("a").toDoubleOrNull() ?: price

                    // AI Pipeline Tick (MarketTick)
                    val marketTick = MarketTick(
                        ts = data.optLong("E"),
                        symbol = symbol,
                        bid = bid,
                        ask = ask,
                        last = price,
                        volume = data.optString("v").toDouble(),
                        source = "binance"
                    )
                    publishTickToRedis(marketTick)

                    val pair = ForexPair(
                        symbol = displaySymbol,
                        name = "", // We can look this up if needed
                        price = price,
                        change = change,
                        changePercent = changePercent,
                        category = MarketCategory.CRYPTO
                    )
                    scope.launch {
                        _priceUpdates.emit(pair)
                    }
                } catch (e: Exception) {
                    Log.e("BinanceWS", "Error parsing message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                // Reconnect on failure
                scope.launch {
                    delay(5000)
                    if (symbols.isNotEmpty()) {
                        connect(symbols.toList())
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("BinanceWS", "WebSocket Closed: $reason")
            }
        })
    }

    private fun parseTicker(text: String): BinanceMiniTicker? {
        return try {
            json.decodeFromString<BinanceMiniTicker>(text)
        } catch (_: Exception) {
            try {
                json.decodeFromString<BinanceMiniTickerEnvelope>(text).data
            } catch (e: Exception) {
                Log.e("BinanceWS", "Unsupported ticker payload: ${e.message}")
                null
            }
        }
    }

    private fun formatSymbol(raw: String): String {
        val upper = raw.uppercase(Locale.US)
        return when {
            upper.endsWith("USDT") -> upper.substring(0, upper.length - 4) + "/USDT"
            else -> upper
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {
        } finally {
            webSocket = null
        }
    }

    fun logon(apiKey: String) {
        val timestamp = System.currentTimeMillis()
        val payload = "apiKey=$apiKey&timestamp=$timestamp"
        val signature = com.asc.markets.security.SecurityManager.signPayload(payload)
        
        val request = BinanceWsRequest(
            id = "logon_1",
            method = "session.logon",
            params = mapOf(
                "apiKey" to apiKey,
                "timestamp" to timestamp.toString(),
                "signature" to signature
            )
        )
        webSocket?.send(json.encodeToString(BinanceWsRequest.serializer(), request))
    }

    fun fetchAccountStatus() {
        val request = BinanceWsRequest(
            id = "account_status_1",
            method = "account.status"
        )
        webSocket?.send(json.encodeToString(BinanceWsRequest.serializer(), request))
    }
}
