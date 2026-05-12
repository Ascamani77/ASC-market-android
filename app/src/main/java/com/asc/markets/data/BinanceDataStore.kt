package com.asc.markets.data

import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object BinanceDataStore {
    private const val historyLength = 40
    private const val timedHistoryLength = 4000
    private const val TAG = "BinanceDataStore"

    private val _allPairs = MutableStateFlow(FOREX_PAIRS.filter { isUsdtSymbol(it.symbol) })
    val allPairs: StateFlow<List<ForexPair>> = _allPairs.asStateFlow()

    private val _priceHistory = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Double>>> = _priceHistory.asStateFlow()

    private val _timedPriceHistory = MutableStateFlow<Map<String, List<TimedPrice>>>(emptyMap())
    val timedPriceHistory: StateFlow<Map<String, List<TimedPrice>>> = _timedPriceHistory.asStateFlow()

    fun pairSnapshot(symbol: String): ForexPair? {
        return findBestMatch(_allPairs.value, symbol)
    }

    fun historySnapshot(symbol: String): List<Double> {
        val pair = pairSnapshot(symbol) ?: return emptyList()
        return _priceHistory.value[pair.symbol] ?: emptyList()
    }

    fun pairFlow(symbol: String): Flow<ForexPair?> {
        return allPairs
            .map { pairs -> findBestMatch(pairs, symbol) }
            .distinctUntilChanged()
    }

    fun historyFlow(symbol: String): Flow<List<Double>> {
        return combine(pairFlow(symbol), priceHistory) { pair, history ->
            if (pair == null) {
                emptyList()
            } else {
                history[pair.symbol] ?: emptyList()
            }
        }.distinctUntilChanged()
    }

    fun updatePair(incoming: ForexPair) {
        if (!isUsdtSymbol(incoming.symbol)) {
            return
        }

        val currentPairs = _allPairs.value
        val updatedPairs = currentPairs.map { existing ->
            if (!matchesSymbol(existing.symbol, incoming.symbol)) {
                existing
            } else {
                existing.copy(
                    name = existing.name.ifBlank { incoming.name },
                    price = incoming.price,
                    change = incoming.change,
                    changePercent = incoming.changePercent,
                    category = MarketCategory.CRYPTO
                )
            }
        }

        if (updatedPairs == currentPairs) {
            Log.w(TAG, "Ignored unmatched Binance update: ${incoming.symbol} ${incoming.price}")
            return
        }

        _allPairs.value = updatedPairs

        val updateTimestamp = System.currentTimeMillis()
        val nextHistory = _priceHistory.value.toMutableMap()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        updatedPairs
            .filter { matchesSymbol(it.symbol, incoming.symbol) }
            .forEach { pair ->
                val previous = nextHistory[pair.symbol].orEmpty()
                nextHistory[pair.symbol] = (previous + pair.price).takeLast(historyLength)
                val previousTimed = nextTimedHistory[pair.symbol].orEmpty()
                nextTimedHistory[pair.symbol] = (previousTimed + TimedPrice(updateTimestamp, pair.price)).takeLast(timedHistoryLength)
            }
        _priceHistory.value = nextHistory
        _timedPriceHistory.value = nextTimedHistory
    }

    fun replaceHistory(symbol: String, prices: List<Double>) {
        val pair = pairSnapshot(symbol) ?: return
        val sanitized = prices
            .filter { it.isFinite() && it > 0.0 }
            .takeLast(historyLength)
        if (sanitized.isEmpty()) {
            return
        }

        val nextHistory = _priceHistory.value.toMutableMap()
        nextHistory[pair.symbol] = sanitized
        _priceHistory.value = nextHistory

        val now = System.currentTimeMillis()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        nextTimedHistory[pair.symbol] = sanitized.mapIndexed { index, price ->
            TimedPrice(now - ((sanitized.lastIndex - index).toLong() * 60_000L), price)
        }
        _timedPriceHistory.value = nextTimedHistory
    }

    fun replaceTimedHistory(symbol: String, prices: List<TimedPrice>) {
        val pair = pairSnapshot(symbol) ?: return
        val sanitized = prices
            .filter { it.timestampMillis > 0L && it.price.isFinite() && it.price > 0.0 }
            .sortedBy { it.timestampMillis }
            .takeLast(timedHistoryLength)
        if (sanitized.isEmpty()) {
            return
        }

        val nextHistory = _priceHistory.value.toMutableMap()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        nextHistory[pair.symbol] = sanitized.map { it.price }.takeLast(historyLength)
        nextTimedHistory[pair.symbol] = sanitized
        _priceHistory.value = nextHistory
        _timedPriceHistory.value = nextTimedHistory
    }

    fun matchesSymbol(left: String, right: String): Boolean {
        return isUsdtSymbol(left) && isUsdtSymbol(right) && normalizeSymbol(left) == normalizeSymbol(right)
    }

    fun isUsdtSymbol(symbol: String): Boolean {
        return normalizeSymbol(symbol).endsWith("USDT")
    }

    private fun normalizeSymbol(symbol: String): String {
        var normalized = symbol
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")

        val suffixes = listOf(".M", ".PRO", ".ECN", ".S", ".SPOT", "M", "+", ".P")
        for (suffix in suffixes) {
            if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length - suffix.length)
                break
            }
        }
        return normalized
    }

    private fun findBestMatch(pairs: List<ForexPair>, symbol: String): ForexPair? {
        val normalized = normalizeSymbol(symbol)
        return pairs.firstOrNull { normalizeSymbol(it.symbol) == normalized }
            ?: pairs.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
            ?: pairs.firstOrNull { matchesSymbol(it.symbol, symbol) }
    }
}
