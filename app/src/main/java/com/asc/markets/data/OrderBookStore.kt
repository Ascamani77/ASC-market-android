package com.asc.markets.data

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
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
    val isStale: Boolean
)

object OrderBookStore {
    private const val tag = "OrderBookStore"
    private const val levelLimit = 8
    private const val tradeLimit = 7
    private const val liveRefreshMs = 250L

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
            val pair = livePairSnapshot(seedPair.symbol) ?: seedPair
            val previous = _snapshots.value[key]
            val nextSnapshot = fetchLiveSnapshot(pair)
                ?: previous?.copy(isStale = true)

            if (nextSnapshot != null) {
                _snapshots.value = _snapshots.value + (key to nextSnapshot)
            }

            delay(liveRefreshMs)
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

    private fun buildSnapshot(
        pair: ForexPair,
        venueSymbol: String?,
        source: String,
        bids: List<OrderBookLevel>,
        asks: List<OrderBookLevel>,
        recentTrades: List<OrderBookTrade>,
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

    private fun livePairSnapshot(symbol: String): ForexPair? {
        return MarketDataStore.pairSnapshot(symbol)
    }
}
