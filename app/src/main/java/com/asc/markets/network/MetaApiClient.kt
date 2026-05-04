package com.asc.markets.network

import android.util.Log
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * MetaApi streaming client aligned with the documented lifecycle:
 * subscribe -> authenticated -> synchronize -> waitSynchronized -> subscribeToMarketData.
 */
class MetaApiClient(
    private val accountId: String,
    private val token: String,
    private val apiUrl: String = "https://mt-client-api-v1.london.agiliumtrade.ai",
    private val scope: CoroutineScope = GlobalScope,
    private val brokerSuffix: String = "m"
) {
    companion object {
        private const val TAG = "MetaApi"
        private const val APPLICATION_NAME = "MetaApi"
        private const val AUTH_TIMEOUT_MS = 15_000L
        private const val WAIT_SYNC_TIMEOUT_MS = 25_000L
        private const val FIRST_PRICE_TIMEOUT_MS = 10_000L
        private const val WAIT_SYNC_TIMEOUT_SECONDS = 20
        private const val QUOTE_INTERVAL_MS = 500
        private const val SYNC_LOOKBACK_SECONDS = 24L * 60L * 60L
    }

    private val baseUrl = apiUrl.trimEnd('/')
    private val normalizedBrokerSuffix = brokerSuffix.uppercase(Locale.US)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    private val _priceUpdates = MutableSharedFlow<ForexPair>(extraBufferCapacity = 100)
    val priceUpdates: SharedFlow<ForexPair> = _priceUpdates

    private val _connectionState = MutableSharedFlow<ConnectionState>(extraBufferCapacity = 10)
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    enum class ConnectionState {
        CONNECTED, DISCONNECTED, ERROR_UNAUTHORIZED, ERROR_UNAVAILABLE, TIMEOUT
    }

    private val symbolLock = Any()
    private val symbols = linkedSetOf<String>()

    private var socket: Socket? = null
    private var authenticationTimeoutJob: Job? = null
    private var waitSynchronizedTimeoutJob: Job? = null
    private var firstPriceTimeoutJob: Job? = null
    private var waitSynchronizedRequestId: String? = null

    @Volatile
    private var manuallyDisconnected = false

    @Volatile
    private var authenticated = false

    @Volatile
    private var synchronizedWithTerminal = false

    @Volatile
    private var subscribedToMarketData = false

    @Volatile
    private var receivedLivePrices = false

    fun connect(targetSymbols: List<String>) {
        val normalizedSymbols = targetSymbols
            .asSequence()
            .map(::normalizeSymbol)
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        synchronized(symbolLock) {
            symbols.clear()
            symbols.addAll(normalizedSymbols)
        }

        disconnectInternal()
        manuallyDisconnected = false
        resetSessionState()

        if (normalizedSymbols.isEmpty()) {
            return
        }

        try {
            val options = IO.Options().apply {
                path = "/ws"
                forceNew = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1_000
                reconnectionDelayMax = 5_000
                timeout = 10_000
                query = "auth-token=${URLEncoder.encode(token, StandardCharsets.UTF_8.name())}"
                callFactory = httpClient
                webSocketFactory = httpClient
            }

            socket = IO.socket(URI.create(baseUrl), options).also { activeSocket ->
                attachListeners(activeSocket)
                activeSocket.connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MetaApi stream", e)
            _connectionState.tryEmit(ConnectionState.DISCONNECTED)
        }
    }

    fun disconnect() {
        manuallyDisconnected = true
        disconnectInternal()
    }

    private fun attachListeners(activeSocket: Socket) {
        activeSocket.on(Socket.EVENT_CONNECT) {
            Log.i(TAG, "Socket connected; subscribing to terminal events")
            resetSessionState()
            scheduleAuthenticationTimeout()
            emitRequest(buildTerminalSubscribeRequest())
        }

        activeSocket.on("response") { args ->
            handleResponse(args.firstOrNull())
        }

        activeSocket.on("synchronization") { args ->
            handleSynchronization(args.firstOrNull())
        }

        activeSocket.on("processingError") { args ->
            handleProcessingError(args.firstOrNull())
        }

        activeSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            handleSocketFailure("connect_error", args.firstOrNull())
        }

        activeSocket.on(Socket.EVENT_DISCONNECT) { args ->
            if (manuallyDisconnected) {
                return@on
            }
            val reason = args.firstOrNull()?.toString().orEmpty()
            Log.w(TAG, "Socket disconnected: $reason")
            cancelTimeoutJobs()
            authenticated = false
            synchronizedWithTerminal = false
            subscribedToMarketData = false
            receivedLivePrices = false
            _connectionState.tryEmit(ConnectionState.DISCONNECTED)
        }
    }

    private fun handleSynchronization(rawPayload: Any?) {
        val payload = toJsonObject(rawPayload) ?: return
        when (payload.optString("type")) {
            "authenticated" -> {
                Log.i(TAG, "Terminal authenticated; starting synchronization")
                authenticated = true
                authenticationTimeoutJob?.cancel()
                synchronizedWithTerminal = false
                subscribedToMarketData = false
                receivedLivePrices = false

                emitRequest(buildSynchronizeRequest())

                val requestId = UUID.randomUUID().toString()
                waitSynchronizedRequestId = requestId
                emitRequest(buildWaitSynchronizedRequest(requestId))
                scheduleWaitSynchronizedTimeout()
            }

            "prices" -> {
                handlePrices(payload.optJSONArray("prices"))
            }

            "synchronizationStarted" -> {
                Log.d(TAG, "Synchronization started")
            }
        }
    }

    private fun handleResponse(rawPayload: Any?) {
        val payload = toJsonObject(rawPayload) ?: return
        if (payload.optString("type") != "response") {
            return
        }

        val requestId = payload.optString("requestId")
        if (requestId.isBlank()) {
            return
        }

        if (requestId == waitSynchronizedRequestId) {
            Log.i(TAG, "MetaApi waitSynchronized completed; subscribing to market data")
            waitSynchronizedRequestId = null
            waitSynchronizedTimeoutJob?.cancel()
            synchronizedWithTerminal = true
            _connectionState.tryEmit(ConnectionState.CONNECTED)
            subscribeToAllSymbols()
            scheduleFirstPriceTimeout()
        }
    }

    private fun handlePrices(prices: JSONArray?) {
        if (prices == null || prices.length() == 0) {
            return
        }

        receivedLivePrices = true
        firstPriceTimeoutJob?.cancel()
        _connectionState.tryEmit(ConnectionState.CONNECTED)

        for (index in 0 until prices.length()) {
            val priceObject = prices.optJSONObject(index) ?: continue
            val rawSymbol = priceObject.optString("symbol")
            val symbol = mapIncomingSymbol(rawSymbol) ?: continue
            val bid = priceObject.optDouble("bid", 0.0)
            val ask = priceObject.optDouble("ask", 0.0)
            if (bid <= 0.0 || ask <= 0.0) {
                continue
            }
            _priceUpdates.tryEmit(buildForexPair(symbol, bid, ask))
        }
    }

    private fun handleProcessingError(rawPayload: Any?) {
        val payload = toJsonObject(rawPayload)
        val message = buildString {
            append(payload?.optString("name").orEmpty())
            if (isNotEmpty()) append(' ')
            append(payload?.optString("message").orEmpty())
            if (isNotEmpty()) append(' ')
            append(rawPayload?.toString().orEmpty())
        }.trim()

        Log.e(TAG, "Processing error: $message")

        when {
            message.contains("401") ||
                message.contains("unauthorized", ignoreCase = true) -> {
                _connectionState.tryEmit(ConnectionState.ERROR_UNAUTHORIZED)
            }

            message.contains("429") ||
                message.contains("TooManyRequestsError", ignoreCase = true) ||
                message.contains("too many requests", ignoreCase = true) ||
                message.contains("ValidationError", ignoreCase = true) ||
                message.contains("InternalError", ignoreCase = true) ||
                message.contains("unavailable", ignoreCase = true) -> {
                _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
            }

            message.contains("timeout", ignoreCase = true) -> {
                _connectionState.tryEmit(ConnectionState.TIMEOUT)
            }

            message.contains("NotAuthenticatedError", ignoreCase = true) -> {
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
            }
        }
    }

    private fun handleSocketFailure(event: String, rawPayload: Any?) {
        if (manuallyDisconnected) {
            return
        }

        val message = rawPayload?.toString().orEmpty()
        Log.e(TAG, "Socket failure ($event): $message")
        cancelTimeoutJobs()
        authenticated = false
        synchronizedWithTerminal = false
        subscribedToMarketData = false
        receivedLivePrices = false

        if (message.contains("401") || message.contains("unauthorized", ignoreCase = true)) {
            _connectionState.tryEmit(ConnectionState.ERROR_UNAUTHORIZED)
        } else if (message.contains("timeout", ignoreCase = true)) {
            _connectionState.tryEmit(ConnectionState.TIMEOUT)
        } else {
            _connectionState.tryEmit(ConnectionState.DISCONNECTED)
        }
    }

    private fun subscribeToAllSymbols() {
        currentSymbols().forEach { symbol ->
            emitRequest(buildMarketSubscriptionRequest(symbol))
        }
        subscribedToMarketData = true
    }

    private fun emitRequest(payload: JSONObject) {
        try {
            socket?.emit("request", payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit request: $payload", e)
            _connectionState.tryEmit(ConnectionState.DISCONNECTED)
        }
    }

    private fun buildTerminalSubscribeRequest(): JSONObject =
        JSONObject().apply {
            put("accountId", accountId)
            put("type", "subscribe")
            put("requestId", UUID.randomUUID().toString())
            put("application", APPLICATION_NAME)
        }

    private fun buildSynchronizeRequest(): JSONObject {
        val syncStart = Instant.now().minusSeconds(SYNC_LOOKBACK_SECONDS).toString()
        return JSONObject().apply {
            put("accountId", accountId)
            put("type", "synchronize")
            put("requestId", UUID.randomUUID().toString())
            put("application", APPLICATION_NAME)
            put("startingDealTime", syncStart)
            put("startingHistoryOrderTime", syncStart)
            put("version", 2)
        }
    }

    private fun buildWaitSynchronizedRequest(requestId: String): JSONObject =
        JSONObject().apply {
            put("accountId", accountId)
            put("type", "waitSynchronized")
            put("requestId", requestId)
            put("application", APPLICATION_NAME)
            put("timeoutInSeconds", WAIT_SYNC_TIMEOUT_SECONDS)
        }

    private fun buildMarketSubscriptionRequest(symbol: String): JSONObject =
        JSONObject().apply {
            put("accountId", accountId)
            put("type", "subscribeToMarketData")
            put("requestId", UUID.randomUUID().toString())
            put("application", APPLICATION_NAME)
            put("symbol", brokerSymbol(symbol))
            put(
                "subscriptions",
                JSONArray().put(
                    JSONObject().apply {
                        put("type", "quotes")
                        put("intervalInMilliseconds", QUOTE_INTERVAL_MS)
                    }
                )
            )
        }

    private fun scheduleAuthenticationTimeout() {
        authenticationTimeoutJob?.cancel()
        authenticationTimeoutJob = scope.launch(Dispatchers.IO) {
            delay(AUTH_TIMEOUT_MS)
            if (!manuallyDisconnected && !authenticated) {
                Log.w(TAG, "Timed out waiting for MetaApi authenticated event")
                _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
            }
        }
    }

    private fun scheduleWaitSynchronizedTimeout() {
        waitSynchronizedTimeoutJob?.cancel()
        waitSynchronizedTimeoutJob = scope.launch(Dispatchers.IO) {
            delay(WAIT_SYNC_TIMEOUT_MS)
            if (!manuallyDisconnected && authenticated && !synchronizedWithTerminal) {
                Log.w(TAG, "Timed out waiting for MetaApi synchronization completion")
                _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
            }
        }
    }

    private fun scheduleFirstPriceTimeout() {
        firstPriceTimeoutJob?.cancel()
        firstPriceTimeoutJob = scope.launch(Dispatchers.IO) {
            delay(FIRST_PRICE_TIMEOUT_MS)
            if (!manuallyDisconnected && synchronizedWithTerminal && !receivedLivePrices) {
                Log.w(TAG, "Timed out waiting for initial MetaApi price stream")
                _connectionState.tryEmit(ConnectionState.ERROR_UNAVAILABLE)
            }
        }
    }

    private fun cancelTimeoutJobs() {
        authenticationTimeoutJob?.cancel()
        authenticationTimeoutJob = null
        waitSynchronizedTimeoutJob?.cancel()
        waitSynchronizedTimeoutJob = null
        firstPriceTimeoutJob?.cancel()
        firstPriceTimeoutJob = null
    }

    private fun resetSessionState() {
        cancelTimeoutJobs()
        waitSynchronizedRequestId = null
        authenticated = false
        synchronizedWithTerminal = false
        subscribedToMarketData = false
        receivedLivePrices = false
    }

    private fun disconnectInternal() {
        cancelTimeoutJobs()
        waitSynchronizedRequestId = null
        try {
            socket?.off()
            socket?.disconnect()
        } catch (_: Exception) {
        } finally {
            socket = null
            authenticated = false
            synchronizedWithTerminal = false
            subscribedToMarketData = false
            receivedLivePrices = false
        }
    }

    private fun currentSymbols(): List<String> = synchronized(symbolLock) { symbols.toList() }

    private fun normalizeSymbol(symbol: String): String = symbol.trim().uppercase(Locale.US)

    private fun brokerSymbol(symbol: String): String {
        val normalized = normalizeSymbol(symbol)
        if (normalizedBrokerSuffix.isBlank() || normalized.endsWith(normalizedBrokerSuffix)) {
            return normalized
        }
        return normalized + normalizedBrokerSuffix
    }

    private fun mapIncomingSymbol(rawSymbol: String): String? {
        if (rawSymbol.isBlank()) {
            return null
        }

        val normalized = normalizeSymbol(rawSymbol)
        val cleanSymbol = if (
            normalizedBrokerSuffix.isNotBlank() &&
            normalized.endsWith(normalizedBrokerSuffix)
        ) {
            normalized.dropLast(normalizedBrokerSuffix.length)
        } else {
            normalized
        }

        return currentSymbols().firstOrNull { it == cleanSymbol }
    }

    private fun toJsonObject(rawPayload: Any?): JSONObject? = try {
        when (rawPayload) {
            is JSONObject -> rawPayload
            is String -> JSONObject(rawPayload)
            else -> rawPayload?.toString()?.takeIf { it.startsWith("{") }?.let(::JSONObject)
        }
    } catch (_: Exception) {
        null
    }

    private fun buildForexPair(symbol: String, bid: Double, ask: Double): ForexPair {
        val price = (bid + ask) / 2.0
        val existing = MarketDataStore.pairSnapshot(symbol)
        val previousPrice = existing?.price ?: price
        val change = price - previousPrice
        val changePercent = if (previousPrice != 0.0) (change / previousPrice) * 100 else 0.0

        val category = FOREX_PAIRS.find {
            it.symbol.replace("/", "").uppercase(Locale.US) == symbol
        }?.category ?: MarketCategory.FOREX

        return ForexPair(
            symbol = symbol,
            name = symbol,
            price = price,
            change = change,
            changePercent = changePercent,
            category = category
        )
    }
}
