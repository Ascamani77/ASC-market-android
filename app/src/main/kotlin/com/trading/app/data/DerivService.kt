package com.trading.app.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.asc.markets.data.SystemTelemetry
import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * DerivService - WebSocket client for Deriv API
 * Provides real-time market data for crypto (BTC/USD, ETH/USD) and commodities
 * 
 * Uses public WebSocket endpoint for read-only market data (no authentication required)
 * For authenticated trading, use the OTP endpoint flow
 */
class DerivService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private data class ActiveSymbolInfo(
        val symbol: String,
        val displayName: String,
        val market: String,
        val submarket: String,
        val marketDisplayName: String,
        val submarketDisplayName: String
    )

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val subscribedSymbols = mutableSetOf<String>()
    private val sentSubscriptions = mutableMapOf<String, String>()
    private val activeSymbolsByCode = mutableMapOf<String, ActiveSymbolInfo>()
    private val requestedProductTypes = linkedSetOf<String>()
    private var isClosing = false
    private var isConnected = false
    
    // Map Deriv symbols to app symbols
    private val symbolMapping = mapOf(
        // FOREX pairs
        "frxEURUSD" to "EUR/USD",
        "frxGBPUSD" to "GBP/USD",
        "frxUSDJPY" to "USD/JPY",
        "frxUSDCHF" to "USD/CHF",
        "frxAUDUSD" to "AUD/USD",
        // Crypto pairs
        "frxBTCUSD" to "BTC/USD",
        "frxETHUSD" to "ETH/USD",
        // Commodities
        "frxXAUUSD" to "XAU/USD", // Gold
        "frxXAGUSD" to "XAG/USD", // Silver
        "frxUKOIL" to "BROUSD", // Brent Crude Oil
        "frxUSOIL" to "USOIL",   // WTI Crude Oil
        // Indices
        "OTC_NDX" to "NAS100",   // US Tech 100
        "OTC_DJI" to "US30",     // Wall Street 30
        "OTC_SPC" to "SPX500"    // US 500
    )
    
    private val reverseSymbolMapping = buildMap {
        symbolMapping.forEach { (derivSymbol, appSymbol) ->
            put(appSymbol, derivSymbol)
            // Also map without slashes
            put(appSymbol.replace("/", ""), derivSymbol)
        }
    }
    
    companion object {
        private const val TAG = "DerivService"
        private const val PUBLIC_WS_URL = "wss://ws.derivws.com/websockets/v3?app_id=1089"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    fun connect() {
        if (isConnected || webSocket != null) {
            Log.d(TAG, "Already connected or connecting")
            return
        }
        
        Log.i(TAG, "Connecting to Deriv WebSocket...")
        val request = Request.Builder()
            .url(PUBLIC_WS_URL)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Deriv WebSocket Connected")
                isConnected = true
                sentSubscriptions.clear()
                activeSymbolsByCode.clear()
                requestedProductTypes.clear()
                SystemTelemetry.recordConnectionEvent("DERIV", "WEBSOCKET_CONNECTED")

                requestSymbolDiscovery()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    
                    when {
                        json.has("tick") -> handleTickMessage(json)
                        json.has("candles") -> handleCandlesMessage(json)
                        json.has("active_symbols") -> handleActiveSymbolsMessage(json)
                        json.has("error") -> handleErrorMessage(json)
                        json.has("msg_type") -> {
                            val msgType = json.getString("msg_type")
                            if (msgType == "tick" || msgType == "candles") {
                                Log.d(TAG, "Subscription confirmed: $msgType")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Deriv message: ${e.message}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Deriv WebSocket Failure: ${t.message}")
                isConnected = false
                this@DerivService.webSocket = null
                sentSubscriptions.clear()
                SystemTelemetry.recordConnectionEvent("DERIV", "CONNECTION_FAILED")
                
                if (!isClosing) {
                    mainHandler.postDelayed({ connect() }, RECONNECT_DELAY_MS)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Deriv WebSocket Closed: code=$code reason=$reason")
                isConnected = false
                this@DerivService.webSocket = null
                sentSubscriptions.clear()
                SystemTelemetry.recordConnectionEvent("DERIV", "CONNECTION_CLOSED ($reason)")
                
                if (!isClosing) {
                    mainHandler.postDelayed({ connect() }, RECONNECT_DELAY_MS)
                }
            }
        })
    }

    private fun handleTickMessage(json: JSONObject) {
        try {
            val tick = json.getJSONObject("tick")
            val derivSymbol = tick.getString("symbol")
            val appSymbol = symbolMapping[derivSymbol] ?: derivSymbol
            
            val quote = tick.optDouble("quote", 0.0)
            if (quote <= 0.0) return
            
            val timestamp = tick.optLong("epoch", System.currentTimeMillis() / 1000) * 1000
            
            // Calculate latency
            val latency = System.currentTimeMillis() - timestamp
            SystemTelemetry.recordTick("DERIV", latency.toDouble().coerceAtLeast(1.0))
            
            // Create SymbolQuote
            val symbolQuote = SymbolQuote(
                name = appSymbol,
                lastPrice = quote.toFloat(),
                change = 0f, // Deriv doesn't provide change in tick
                changePercent = 0f,
                open = 0f,
                high = 0f,
                low = 0f,
                prevClose = 0f,
                bid = tick.optDouble("bid", quote).toFloat(),
                ask = tick.optDouble("ask", quote).toFloat(),
                volume = 0f,
                time = timestamp
            )
            
            mainHandler.post {
                onQuoteUpdate(symbolQuote)
            }
            
            Log.d(TAG, "Tick: $appSymbol = $quote")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling tick message: ${e.message}", e)
        }
    }

    private fun handleCandlesMessage(json: JSONObject) {
        try {
            val candles = json.getJSONArray("candles")
            val derivSymbol = json.optString("echo_req")
                .let { if (it.isNotEmpty()) JSONObject(it).optString("ticks_history") else "" }
            
            if (derivSymbol.isEmpty()) {
                Log.w(TAG, "Cannot determine symbol from candles response")
                return
            }
            
            val appSymbol = symbolMapping[derivSymbol] ?: derivSymbol
            val ohlcList = mutableListOf<OHLCData>()
            
            for (i in 0 until candles.length()) {
                val candle = candles.getJSONObject(i)
                ohlcList.add(
                    OHLCData(
                        time = candle.getLong("epoch"),
                        open = candle.getDouble("open").toFloat(),
                        high = candle.getDouble("high").toFloat(),
                        low = candle.getDouble("low").toFloat(),
                        close = candle.getDouble("close").toFloat(),
                        volume = 0f // Deriv doesn't always provide volume
                    )
                )
            }
            
            mainHandler.post {
                onHistoryUpdate(appSymbol, ohlcList)
            }
            
            Log.d(TAG, "Received ${ohlcList.size} candles for $appSymbol")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling candles message: ${e.message}", e)
        }
    }

    private fun handleActiveSymbolsMessage(json: JSONObject) {
        try {
            val activeSymbols = json.getJSONArray("active_symbols")
            val echoReq = json.optJSONObject("echo_req")
            val productType = echoReq?.optString("product_type")
                ?.takeIf { it.isNotBlank() }
                ?: "default"
            var addedCount = 0
            for (index in 0 until activeSymbols.length()) {
                val item = activeSymbols.optJSONObject(index) ?: continue
                val code = item.optString("symbol").trim()
                if (code.isEmpty()) continue
                if (activeSymbolsByCode.put(
                        code,
                        ActiveSymbolInfo(
                            symbol = code,
                            displayName = item.optString("display_name"),
                            market = item.optString("market"),
                            submarket = item.optString("submarket"),
                            marketDisplayName = item.optString("market_display_name"),
                            submarketDisplayName = item.optString("submarket_display_name")
                        )
                    ) == null
                ) {
                    addedCount += 1
                }
            }
            Log.i(
                TAG,
                "Loaded ${activeSymbols.length()} active Deriv symbols for product_type=$productType (merged total=${activeSymbolsByCode.size}, added=$addedCount)"
            )
            logMarketSummary()
            logActiveSymbolDiscovery()
            logOilCandidates()
            logStockCandidates()
            logKnownStockProbe()
            logCommodityCandidates()

            // Subscribe only after active symbols are loaded so we use Deriv's live symbol list.
            subscribedSymbols.forEach { appSymbol ->
                subscribeToSymbol(appSymbol)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling active_symbols message: ${e.message}", e)
        }
    }

    private fun handleErrorMessage(json: JSONObject) {
        val error = json.getJSONObject("error")
        val code = error.optString("code", "UNKNOWN")
        val message = error.optString("message", "Unknown error")
        val echoReq = json.optJSONObject("echo_req")
        val requestedSymbol = echoReq?.optString("ticks").orEmpty()
        when (code) {
            "MarketIsClosed" -> Log.w(TAG, "Deriv market closed for $requestedSymbol: $message")
            "InvalidSymbol" -> {
                Log.w(TAG, "Deriv invalid symbol for $requestedSymbol: $message")
                requestSymbolDiscovery()
            }
            else -> Log.e(TAG, "Deriv API Error: [$code] $message")
        }
    }

    private fun requestSymbolDiscovery() {
        requestActiveSymbols("basic")
        requestActiveSymbols(null)
    }

    private fun requestActiveSymbols(productType: String?) {
        val key = productType ?: "default"
        if (!requestedProductTypes.add(key)) {
            return
        }
        val request = JSONObject().apply {
            put("active_symbols", "brief")
            if (!productType.isNullOrBlank()) {
                put("product_type", productType)
            }
        }
        val sent = webSocket?.send(request.toString()) ?: false
        if (!sent) {
            Log.w(TAG, "Failed to request active symbols for product_type=$key")
        } else {
            Log.i(TAG, "Requested active symbols for product_type=$key")
        }
    }

    /**
     * Subscribe to a symbol for real-time tick updates
     */
    fun subscribe(symbol: String) {
        subscribedSymbols.add(symbol)
        
        if (!isConnected) {
            connect()
            return
        }
        
        subscribeToSymbol(symbol)
    }

    private fun subscribeToSymbol(symbol: String) {
        val derivSymbol = resolveDerivSymbol(symbol)
        if (derivSymbol == null) {
            sentSubscriptions.remove(symbol)
            Log.w(TAG, "No Deriv symbol resolved for $symbol yet")
            return
        }
        if (sentSubscriptions[symbol] == derivSymbol) {
            return
        }
        
        val subscribeMessage = JSONObject().apply {
            put("ticks", derivSymbol)
            put("subscribe", 1)
        }
        
        val sent = webSocket?.send(subscribeMessage.toString()) ?: false
        if (sent) {
            sentSubscriptions[symbol] = derivSymbol
            Log.i(TAG, "Subscribed to $derivSymbol (app: $symbol)")
        } else {
            Log.w(TAG, "Failed to subscribe to $derivSymbol")
        }
    }

    /**
     * Stream a single active symbol (unsubscribe from others)
     */
    fun streamActiveSymbol(symbol: String) {
        subscribedSymbols.clear()
        subscribedSymbols.add(symbol)
        
        if (!isConnected) {
            connect()
            return
        }
        
        // Deriv doesn't have a bulk unsubscribe, so we just subscribe to the new symbol
        // The old subscriptions will remain but we can ignore them
        subscribeToSymbol(symbol)
    }

    /**
     * Stop streaming the active symbol
     */
    fun stopActiveStream() {
        subscribedSymbols.clear()
        // Note: Deriv subscriptions remain active until connection closes
        // For a clean implementation, you could send forget messages for each subscription
    }

    /**
     * Fetch historical candle data
     * @param symbol The symbol to fetch (e.g., "BTCUSD")
     * @param timeframe The timeframe (e.g., "1h", "1d")
     * @param endTime Optional end time in seconds (null for latest)
     */
    fun fetchHistory(symbol: String, timeframe: String, endTime: Long? = null) {
        val derivSymbol = resolveDerivSymbol(symbol) ?: symbol
        
        if (!isConnected) {
            connect()
            // Will fetch after connection is established
            return
        }
        
        // Convert timeframe to Deriv granularity (in seconds)
        val granularity = when (timeframe.lowercase()) {
            "1m" -> 60
            "5m" -> 300
            "15m" -> 900
            "30m" -> 1800
            "1h" -> 3600
            "4h" -> 14400
            "1d", "d" -> 86400
            else -> 3600 // Default to 1 hour
        }
        
        val historyMessage = JSONObject().apply {
            put("ticks_history", derivSymbol)
            put("adjust_start_time", 1)
            put("count", 500)
            put("end", endTime ?: "latest")
            put("start", 1)
            put("style", "candles")
            put("granularity", granularity)
        }
        
        val sent = webSocket?.send(historyMessage.toString()) ?: false
        if (sent) {
            Log.i(TAG, "Requested history for $derivSymbol ($timeframe)")
        } else {
            Log.w(TAG, "Failed to request history for $derivSymbol")
            mainHandler.post {
                onHistoryUpdate(symbol, emptyList())
            }
        }
    }

    /**
     * Unsubscribe from a specific symbol
     */
    fun unsubscribe(symbol: String) {
        subscribedSymbols.remove(symbol)
        sentSubscriptions.remove(symbol)
        
        // Send forget message (requires subscription_id, which we'd need to track)
        // For simplicity, we'll just remove from our tracking
        Log.d(TAG, "Unsubscribed from $symbol")
    }

    /**
     * Disconnect from Deriv WebSocket
     */
    fun disconnect() {
        isClosing = true
        subscribedSymbols.clear()
        sentSubscriptions.clear()
        webSocket?.close(1000, "App closing")
        webSocket = null
        isConnected = false
        Log.i(TAG, "Disconnected from Deriv")
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected

    private fun resolveDerivSymbol(symbol: String): String? {
        val normalized = normalizeSymbolKey(symbol)
        val hardcoded = reverseSymbolMapping[normalized]
        // Use hardcoded mapping for commodities and indices immediately (like commodities pattern)
        if (hardcoded != null) {
            return hardcoded
        }

        if (activeSymbolsByCode.isEmpty()) {
            return null
        }

        val discovered = when (normalized) {
            "XAUUSD", "GOLD" -> findActiveSymbol(
                codeHints = listOf("XAUUSD"),
                textHints = listOf("gold/usd", "gold", "xau"),
                marketHint = "commodities"
            )
            "XAGUSD", "SILVER" -> findActiveSymbol(
                codeHints = listOf("XAGUSD"),
                textHints = listOf("silver/usd", "silver", "xag"),
                marketHint = "commodities"
            )
            "USOIL", "WTIUSD", "CRUDE" -> findActiveSymbol(
                codeHints = listOf("WTIUSD", "WTIOUSD", "USOIL"),
                textHints = listOf("wti/usd", "wti", "us oil", "west texas", "crude"),
                marketHint = "commodities"
            )
            "BROUSD", "BRENT" -> findActiveSymbol(
                codeHints = listOf("BRNUSD", "UKOIL", "BROUSD", "BRENT"),
                textHints = listOf("brn/usd", "brent oil", "brent", "uk oil"),
                marketHint = "commodities"
            )
            "NAS100" -> findActiveSymbol(
                codeHints = listOf("OTCNDX"),
                textHints = listOf("us tech 100", "nasdaq 100", "nas100"),
                marketHint = "indices"
            )
            "US30" -> findActiveSymbol(
                codeHints = listOf("OTCDJI"),
                textHints = listOf("wall street 30", "dow jones 30", "us30"),
                marketHint = "indices"
            )
            "SPX500" -> findActiveSymbol(
                codeHints = listOf("OTCSPC"),
                textHints = listOf("us 500", "s&p 500", "spx500"),
                marketHint = "indices"
            )
            else -> findActiveSymbol(
                codeHints = listOf(normalized),
                textHints = listOf(symbol),
                marketHint = null
            )
        }

        return discovered?.symbol ?: hardcoded?.takeIf { activeSymbolsByCode.containsKey(it) }
    }

    private fun findActiveSymbol(
        codeHints: List<String>,
        textHints: List<String>,
        marketHint: String?
    ): ActiveSymbolInfo? {
        val normalizedCodeHints = codeHints.map(::normalizeSymbolKey)
        val loweredTextHints = textHints.map { it.lowercase(Locale.US) }
        val loweredMarketHint = marketHint?.lowercase(Locale.US)

        fun ActiveSymbolInfo.matchesMarketHint(): Boolean {
            if (loweredMarketHint.isNullOrBlank()) return true
            val marketText = listOf(marketDisplayName, submarketDisplayName)
                .joinToString(" ")
                .lowercase(Locale.US)
            return marketText.contains(loweredMarketHint)
        }

        return activeSymbolsByCode.values.firstOrNull { info ->
            info.matchesMarketHint() &&
                normalizedCodeHints.any { hint -> normalizeSymbolKey(info.symbol) == hint }
        } ?: activeSymbolsByCode.values.firstOrNull { info ->
            val haystack = listOf(
                info.symbol,
                info.displayName,
                info.marketDisplayName,
                info.submarketDisplayName
            ).joinToString(" ").lowercase(Locale.US)
            info.matchesMarketHint() &&
                loweredTextHints.any { hint -> hint.isNotBlank() && haystack.contains(hint) }
        }
    }

    private fun normalizeSymbolKey(symbol: String): String {
        return symbol
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }

    private fun logOilCandidates() {
        val candidates = activeSymbolsByCode.values.filter { info ->
            val haystack = listOf(
                info.symbol,
                info.displayName,
                info.market,
                info.submarket,
                info.marketDisplayName,
                info.submarketDisplayName
            ).joinToString(" ").lowercase(Locale.US)
            haystack.contains("oil") || haystack.contains("wti") || haystack.contains("brent")
        }
        if (candidates.isNotEmpty()) {
            Log.i(
                TAG,
                "Deriv oil candidates: ${candidates.joinToString { "${it.symbol}:${it.displayName}" }}"
            )
        }
    }

    private fun logCommodityCandidates() {
        val candidates = activeSymbolsByCode.values.filter { info ->
            info.marketDisplayName.equals("Commodities", ignoreCase = true)
        }
        if (candidates.isNotEmpty()) {
            Log.i(
                TAG,
                "Deriv commodity symbols: ${candidates.joinToString { "${it.symbol}:${it.displayName}" }}"
            )
        }
    }

    private fun logStockCandidates() {
        val stockIndices = activeSymbolsByCode.values.filter { info ->
            info.marketDisplayName.equals("Stock Indices", ignoreCase = true)
        }
        val individualStocks = activeSymbolsByCode.values.filter { info ->
            val tokens = symbolSearchTokens(info)
            !info.marketDisplayName.equals("Stock Indices", ignoreCase = true) &&
                (
                    tokens.contains("stock") ||
                        tokens.any { it.startsWith("equit") } ||
                        tokens.contains("share")
                    )
        }

        if (individualStocks.isNotEmpty()) {
            Log.i(
                TAG,
                "Deriv individual stock candidates: ${
                    individualStocks.joinToString { "${it.symbol}:${it.displayName}" }
                }"
            )
        } else {
            Log.i(TAG, "Deriv individual stock candidates: none")
        }

        if (stockIndices.isNotEmpty()) {
            Log.i(
                TAG,
                "Deriv stock indices: ${stockIndices.joinToString { "${it.symbol}:${it.displayName}" }}"
            )
        } else {
            Log.i(TAG, "Deriv stock indices: none")
        }
    }

    private fun logKnownStockProbe() {
        val probes = listOf(
            "AAPL" to listOf("aapl", "apple"),
            "TSLA" to listOf("tsla", "tesla"),
            "NVDA" to listOf("nvda", "nvidia"),
            "MSFT" to listOf("msft", "microsoft"),
            "AMZN" to listOf("amzn", "amazon"),
            "GOOGL" to listOf("googl", "google", "alphabet"),
            "META" to listOf("meta", "facebook"),
            "NFLX" to listOf("nflx", "netflix")
        )

        val probeResults = probes.map { (label, terms) ->
            val matches = activeSymbolsByCode.values.filter { info ->
                val tokens = symbolSearchTokens(info)
                terms.any { term -> tokens.contains(term) }
            }
            label to matches
        }

        val summary = probeResults.joinToString { (label, matches) ->
            if (matches.isEmpty()) {
                "$label=missing"
            } else {
                "$label=${matches.first().symbol}"
            }
        }
        Log.i(TAG, "Deriv stock probe summary: $summary")

        probeResults.forEach { (label, matches) ->
            if (matches.isEmpty()) {
                Log.i(TAG, "Deriv stock probe $label: no match")
            } else {
                Log.i(
                    TAG,
                    "Deriv stock probe $label: ${
                        matches.take(5).joinToString { "${it.symbol}:${it.displayName}" }
                    }"
                )
            }
        }
    }

    private fun symbolSearchTokens(info: ActiveSymbolInfo): Set<String> {
        return listOf(
            info.symbol,
            info.displayName,
            info.market,
            info.submarket,
            info.marketDisplayName,
            info.submarketDisplayName
        ).flatMap { text ->
            text.lowercase(Locale.US)
                .split(Regex("[^a-z0-9]+"))
                .filter { it.isNotBlank() }
        }.toSet()
    }

    private fun logMarketSummary() {
        val byMarket = activeSymbolsByCode.values
            .groupBy { info ->
                info.marketDisplayName.ifBlank {
                    info.market.ifBlank { "Unknown" }
                }
            }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .entries
            .joinToString { (market, items) -> "$market=${items.size}" }

        Log.i(TAG, "Deriv market summary: $byMarket")
    }

    private fun logActiveSymbolDiscovery() {
        activeSymbolsByCode.values
            .sortedWith(compareBy({ it.market }, { it.submarket }, { it.displayName }))
            .forEach { item ->
                Log.i(
                    "DerivDiscovery",
                    "[${item.market}] ${item.displayName} -> ID: ${item.symbol} (submarket=${item.submarket})"
                )
            }
    }
}
