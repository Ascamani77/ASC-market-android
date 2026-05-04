package com.asc.markets.data

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class OrderBookLevel(
    val price: Double,
    val quantity: Double,
    val cumulativeQuantity: Double
)

enum class OrderTradeSide {
    BUY,
    SELL
}

data class OrderBookTrade(
    val price: Double,
    val quantity: Double,
    val timestamp: Long,
    val side: OrderTradeSide
)

data class OrderBookSnapshot(
    val symbol: String,
    val venueSymbol: String?,
    val source: String,
    val lastUpdated: Long,
    val bidLevels: List<OrderBookLevel>,
    val askLevels: List<OrderBookLevel>,
    val recentTrades: List<OrderBookTrade>,
    val spread: Double,
    val midPrice: Double,
    val imbalance: Double,
    val isFallback: Boolean,
    val isStale: Boolean
)

object OrderBookStore {
    private const val tag = "OrderBookStore"
    private const val levelLimit = 8
    private const val tradeLimit = 7
    private const val liveRefreshMs = 2_000L
    private const val fallbackRefreshMs = 4_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monitor = Any()
    private val _snapshots = MutableStateFlow<Map<String, OrderBookSnapshot>>(emptyMap())
    private val jobs = mutableMapOf<String, Job>()
    private val subscribers = mutableMapOf<String, Int>()

    val snapshots: StateFlow<Map<String, OrderBookSnapshot>> = _snapshots.asStateFlow()

    fun snapshotFlow(symbol: String): Flow<OrderBookSnapshot?> {
        val key = symbolKey(symbol)
        return snapshots
            .map { current -> current[key] }
            .distinctUntilChanged()
    }

    fun seedSnapshot(pair: ForexPair): OrderBookSnapshot = deterministicSnapshot(pair)

    fun subscribe(pair: ForexPair) {
        val key = symbolKey(pair.symbol)
        synchronized(monitor) {
            subscribers[key] = (subscribers[key] ?: 0) + 1
            if (jobs[key] == null) {
                jobs[key] = scope.launch {
                    pollSnapshot(key, pair)
                }
            }
        }
    }

    fun unsubscribe(symbol: String) {
        val key = symbolKey(symbol)
        synchronized(monitor) {
            val nextCount = (subscribers[key] ?: 1) - 1
            if (nextCount <= 0) {
                subscribers.remove(key)
                jobs.remove(key)?.cancel()
            } else {
                subscribers[key] = nextCount
            }
        }
    }

    private suspend fun pollSnapshot(key: String, seedPair: ForexPair) {
        while (currentCoroutineContext().isActive) {
            val pair = MarketDataStore.pairSnapshot(seedPair.symbol) ?: seedPair
            val previous = _snapshots.value[key]
            val nextSnapshot = fetchLiveSnapshot(pair)
                ?: previous?.takeUnless { it.isFallback }?.copy(isStale = true)
                ?: deterministicSnapshot(pair)

            _snapshots.value = _snapshots.value + (key to nextSnapshot)

            delay(
                when {
                    nextSnapshot.isFallback -> fallbackRefreshMs
                    nextSnapshot.isStale -> liveRefreshMs
                    else -> liveRefreshMs
                }
            )
        }
    }

    private fun fetchLiveSnapshot(pair: ForexPair): OrderBookSnapshot? {
        val venueSymbol = liveVenueSymbol(pair) ?: return null
        return try {
            val depthPayload = requestJson(
                "https://fapi.binance.com/fapi/v1/depth?symbol=$venueSymbol&limit=$levelLimit"
            )
            val tradesPayload = requestJson(
                "https://fapi.binance.com/fapi/v1/trades?symbol=$venueSymbol&limit=$tradeLimit"
            )

            val depthJson = JSONObject(depthPayload)
            val bids = parseLevels(depthJson.optJSONArray("bids"), isBid = true)
            val asks = parseLevels(depthJson.optJSONArray("asks"), isBid = false)
            if (bids.isEmpty() || asks.isEmpty()) {
                return null
            }

            val trades = parseTrades(JSONArray(tradesPayload))
            buildSnapshot(
                pair = pair,
                venueSymbol = venueSymbol,
                source = "Binance Futures",
                bids = bids,
                asks = asks,
                recentTrades = trades,
                isFallback = false,
                isStale = false
            )
        } catch (t: Throwable) {
            Log.w(tag, "Live order book fetch failed for ${pair.symbol}: ${t.message}")
            null
        }
    }

    private fun requestJson(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ASC-Markets/1.0")
        }

        return connection.useAndRead()
    }

    private fun HttpURLConnection.useAndRead(): String {
        return try {
            val responseCode = responseCode
            val stream = if (responseCode in 200..299) inputStream else errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode: $body")
            }
            body
        } finally {
            disconnect()
        }
    }

    private fun parseLevels(levels: JSONArray?, isBid: Boolean): List<OrderBookLevel> {
        if (levels == null) return emptyList()

        val rawLevels = buildList {
            for (index in 0 until minOf(levels.length(), levelLimit)) {
                val row = levels.optJSONArray(index) ?: continue
                val price = row.optString(0).toDoubleOrNull() ?: continue
                val quantity = row.optString(1).toDoubleOrNull() ?: continue
                if (quantity > 0.0) {
                    add(price to quantity)
                }
            }
        }

        val sortedLevels = if (isBid) {
            rawLevels.sortedByDescending { (price, _) -> price }
        } else {
            rawLevels.sortedBy { (price, _) -> price }
        }

        var cumulativeQuantity = 0.0
        return sortedLevels.map { (price, quantity) ->
            cumulativeQuantity += quantity
            OrderBookLevel(price = price, quantity = quantity, cumulativeQuantity = cumulativeQuantity)
        }
    }

    private fun parseTrades(trades: JSONArray): List<OrderBookTrade> {
        return buildList {
            for (index in 0 until minOf(trades.length(), tradeLimit)) {
                val trade = trades.optJSONObject(index) ?: continue
                val price = trade.optString("price").toDoubleOrNull() ?: continue
                val quantity = trade.optString("qty").toDoubleOrNull() ?: continue
                val isBuyerMaker = trade.optBoolean("isBuyerMaker", trade.optBoolean("buyerMaker", false))
                add(
                    OrderBookTrade(
                        price = price,
                        quantity = quantity,
                        timestamp = trade.optLong("time", System.currentTimeMillis()),
                        side = if (isBuyerMaker) OrderTradeSide.SELL else OrderTradeSide.BUY
                    )
                )
            }
        }
    }

    private fun deterministicSnapshot(pair: ForexPair): OrderBookSnapshot {
        val livePair = MarketDataStore.pairSnapshot(pair.symbol) ?: pair
        val currentPrice = livePair.price
        val history = MarketDataStore.historySnapshot(livePair.symbol).ifEmpty { List(12) { currentPrice } }
        val baseTick = baseTickSize(livePair.symbol, currentPrice)
        val volatility = history
            .zipWithNext { previous, next -> abs(next - previous) }
            .average()
            .takeIf { it > 0.0 }
            ?: baseTick
        val adaptiveTick = max(baseTick, volatility * 0.65)
        val signature = symbolSignature(livePair.symbol)
        val baseQuantity = baseLiquidityFor(livePair.category, currentPrice)
        val trend = history.lastOrNull().orZero() - history.firstOrNull().orZero()
        val trendBias = if (currentPrice == 0.0) 0.0 else trend / currentPrice
        val timeNow = System.currentTimeMillis()

        val bids = buildSideLevels(
            centerPrice = currentPrice,
            tickSize = adaptiveTick,
            baseQuantity = baseQuantity,
            signature = signature,
            trendBias = trendBias,
            isBid = true
        )
        val asks = buildSideLevels(
            centerPrice = currentPrice,
            tickSize = adaptiveTick,
            baseQuantity = baseQuantity,
            signature = signature,
            trendBias = trendBias,
            isBid = false
        )

        val recentHistory = history.takeLast(tradeLimit + 1).ifEmpty { List(tradeLimit + 1) { currentPrice } }
        val trades = buildList {
            for (index in 1 until recentHistory.size) {
                val previousPrice = recentHistory[index - 1]
                val currentTradePrice = recentHistory[index]
                val directionalBias = when {
                    currentTradePrice > previousPrice -> OrderTradeSide.BUY
                    currentTradePrice < previousPrice -> OrderTradeSide.SELL
                    ((signature + index) and 1) == 0 -> OrderTradeSide.BUY
                    else -> OrderTradeSide.SELL
                }
                val quantity =
                    (baseQuantity * 0.16 * stableWave(signature, index, if (directionalBias == OrderTradeSide.BUY) 0.33 else 1.21))
                        .coerceAtLeast(baseQuantity * 0.04)

                add(
                    OrderBookTrade(
                        price = currentTradePrice,
                        quantity = quantity,
                        timestamp = timeNow - ((recentHistory.size - index).toLong() * 1_100L),
                        side = directionalBias
                    )
                )
            }
        }

        return buildSnapshot(
            pair = livePair,
            venueSymbol = null,
            source = "Deterministic quote model",
            bids = bids,
            asks = asks,
            recentTrades = trades,
            isFallback = true,
            isStale = false
        )
    }

    private fun buildSideLevels(
        centerPrice: Double,
        tickSize: Double,
        baseQuantity: Double,
        signature: Int,
        trendBias: Double,
        isBid: Boolean
    ): List<OrderBookLevel> {
        val directionalBias = if (isBid) max(trendBias, 0.0) else max(-trendBias, 0.0)
        val rawLevels = mutableListOf<Pair<Double, Double>>()

        for (level in 1..levelLimit) {
            val offset = tickSize * level
            val price = if (isBid) centerPrice - offset else centerPrice + offset
            val wave = stableWave(signature, level, if (isBid) 0.65 else 1.55)
            val quantity = (
                baseQuantity *
                    (1.0 + (level * 0.18)) *
                    (0.92 + directionalBias * 4.0) *
                    wave
                ).coerceAtLeast(baseQuantity * 0.18)
            rawLevels += price to quantity
        }

        val sortedLevels = if (isBid) {
            rawLevels.sortedByDescending { (price, _) -> price }
        } else {
            rawLevels.sortedBy { (price, _) -> price }
        }

        var cumulativeQuantity = 0.0
        return sortedLevels.map { (price, quantity) ->
            cumulativeQuantity += quantity
            OrderBookLevel(
                price = price,
                quantity = quantity,
                cumulativeQuantity = cumulativeQuantity
            )
        }
    }

    private fun buildSnapshot(
        pair: ForexPair,
        venueSymbol: String?,
        source: String,
        bids: List<OrderBookLevel>,
        asks: List<OrderBookLevel>,
        recentTrades: List<OrderBookTrade>,
        isFallback: Boolean,
        isStale: Boolean
    ): OrderBookSnapshot {
        val bestBid = bids.firstOrNull()?.price ?: pair.price
        val bestAsk = asks.firstOrNull()?.price ?: pair.price
        val midPrice = if (bestBid > 0.0 && bestAsk > 0.0) (bestBid + bestAsk) / 2.0 else pair.price
        val spread = (bestAsk - bestBid).coerceAtLeast(0.0)
        val totalBidQuantity = bids.sumOf { it.quantity }
        val totalAskQuantity = asks.sumOf { it.quantity }
        val totalDisplayedQuantity = totalBidQuantity + totalAskQuantity
        val imbalance = if (totalDisplayedQuantity == 0.0) {
            0.0
        } else {
            (totalBidQuantity - totalAskQuantity) / totalDisplayedQuantity
        }

        return OrderBookSnapshot(
            symbol = pair.symbol,
            venueSymbol = venueSymbol,
            source = source,
            lastUpdated = System.currentTimeMillis(),
            bidLevels = bids,
            askLevels = asks,
            recentTrades = recentTrades,
            spread = spread,
            midPrice = midPrice,
            imbalance = imbalance,
            isFallback = isFallback,
            isStale = isStale
        )
    }

    private fun liveVenueSymbol(pair: ForexPair): String? {
        if (pair.category != MarketCategory.CRYPTO) {
            return null
        }

        val normalized = symbolKey(pair.symbol)
        return when {
            normalized.endsWith("USDT") -> normalized
            normalized.endsWith("USD") -> normalized.removeSuffix("USD") + "USDT"
            else -> null
        }
    }

    private fun symbolKey(symbol: String): String {
        return symbol
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }

    private fun symbolSignature(symbol: String): Int {
        return symbolKey(symbol).fold(0) { acc, char -> (acc * 31) + char.code }
    }

    private fun stableWave(signature: Int, index: Int, offset: Double): Double {
        val angle = (signature * 0.013) + (index * 0.77) + offset
        return 0.78 + ((sin(angle) + 1.0) * 0.24)
    }

    private fun baseLiquidityFor(category: MarketCategory, price: Double): Double {
        return when (category) {
            MarketCategory.CRYPTO -> if (price >= 1_000.0) 0.18 else 4_200.0
            MarketCategory.FOREX -> 125_000.0
            MarketCategory.STOCK -> 2_400.0
            MarketCategory.COMMODITIES -> 1_050.0
            MarketCategory.INDICES -> 220.0
            MarketCategory.BONDS -> 480.0
            MarketCategory.FUTURES -> 260.0
        }
    }

    private fun baseTickSize(symbol: String, price: Double): Double {
        val normalized = symbolKey(symbol)
        return when {
            normalized.endsWith("JPY") -> 0.01
            normalized.endsWith("USDT") && price >= 10_000.0 -> 0.5
            normalized.endsWith("USDT") && price >= 1_000.0 -> 0.1
            normalized.endsWith("USDT") && price >= 1.0 -> 0.01
            normalized.endsWith("USD") && price >= 1_000.0 -> 0.1
            normalized.contains("XAU") -> 0.1
            normalized.contains("XAG") -> 0.01
            normalized.contains("USOIL") -> 0.01
            normalized.contains("SPX") || normalized.contains("NAS") || normalized.contains("US30") -> 1.0
            normalized.length == 6 && price < 10.0 -> 0.0001
            price >= 1_000.0 -> 0.5
            price >= 100.0 -> 0.05
            price >= 1.0 -> 0.01
            else -> 0.0001
        }
    }

    private fun Double?.orZero(): Double = this ?: 0.0
}
