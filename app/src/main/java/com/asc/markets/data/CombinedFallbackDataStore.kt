package com.asc.markets.data

import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object CombinedFallbackDataStore {
    private const val historyLength = 40
    private const val timedHistoryLength = 4000

    private val _allPairs = MutableStateFlow<List<ForexPair>>(emptyList())
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
            if (pair == null) emptyList() else history[pair.symbol] ?: emptyList()
        }.distinctUntilChanged()
    }

    fun updatePair(incoming: ForexPair) {
        if (isUsdtSymbol(incoming.symbol)) {
            return
        }

        val canonical = canonicalPair(incoming)
        val currentPairs = _allPairs.value
        val hasExisting = currentPairs.any { matchesSymbol(it.symbol, canonical.symbol) }
        val updatedPairs = if (hasExisting) {
            currentPairs.map { existing ->
                if (matchesSymbol(existing.symbol, canonical.symbol)) {
                    existing.copy(
                        name = existing.name.ifBlank { canonical.name },
                        price = canonical.price,
                        change = canonical.change,
                        changePercent = canonical.changePercent,
                        category = canonical.category
                    )
                } else {
                    existing
                }
            }
        } else {
            currentPairs + canonical
        }

        _allPairs.value = updatedPairs

        val updateTimestamp = System.currentTimeMillis()
        val nextHistory = _priceHistory.value.toMutableMap()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        val previous = nextHistory[canonical.symbol].orEmpty()
        nextHistory[canonical.symbol] = (previous + canonical.price).takeLast(historyLength)
        val previousTimed = nextTimedHistory[canonical.symbol].orEmpty()
        nextTimedHistory[canonical.symbol] = (previousTimed + TimedPrice(updateTimestamp, canonical.price)).takeLast(timedHistoryLength)
        _priceHistory.value = nextHistory
        _timedPriceHistory.value = nextTimedHistory
    }

    fun clear() {
        _allPairs.value = emptyList()
        _priceHistory.value = emptyMap()
        _timedPriceHistory.value = emptyMap()
    }

    fun replaceHistory(symbol: String, prices: List<Double>) {
        if (isUsdtSymbol(symbol)) {
            return
        }
        val pair = pairSnapshot(symbol) ?: canonicalPair(ForexPair(symbol, symbol, prices.lastOrNull() ?: 0.0, 0.0, 0.0, MarketCategory.FOREX))
        val sanitized = prices
            .filter { it.isFinite() && it > 0.0 }
            .takeLast(historyLength)
        if (sanitized.isEmpty()) {
            return
        }
        ensurePair(pair)
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
        if (isUsdtSymbol(symbol)) {
            return
        }
        val pair = pairSnapshot(symbol)
            ?: canonicalPair(ForexPair(symbol, symbol, prices.lastOrNull()?.price ?: 0.0, 0.0, 0.0, MarketCategory.FOREX))
        val sanitized = prices
            .filter { it.timestampMillis > 0L && it.price.isFinite() && it.price > 0.0 }
            .sortedBy { it.timestampMillis }
            .takeLast(timedHistoryLength)
        if (sanitized.isEmpty()) {
            return
        }
        ensurePair(pair)
        val nextHistory = _priceHistory.value.toMutableMap()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        nextHistory[pair.symbol] = sanitized.map { it.price }.takeLast(historyLength)
        nextTimedHistory[pair.symbol] = sanitized
        _priceHistory.value = nextHistory
        _timedPriceHistory.value = nextTimedHistory
    }

    fun matchesSymbol(left: String, right: String): Boolean {
        return !isUsdtSymbol(left) && !isUsdtSymbol(right) && normalizeSymbol(left) == normalizeSymbol(right)
    }

    private fun ensurePair(pair: ForexPair) {
        if (_allPairs.value.none { matchesSymbol(it.symbol, pair.symbol) }) {
            _allPairs.value = _allPairs.value + pair
        }
    }

    private fun canonicalPair(incoming: ForexPair): ForexPair {
        val template = FOREX_PAIRS.firstOrNull { !isUsdtSymbol(it.symbol) && matchesSymbol(it.symbol, incoming.symbol) }
        val previous = pairSnapshot(template?.symbol ?: incoming.symbol) ?: template
        val previousPrice = previous?.price ?: incoming.price
        val change = if (incoming.change != 0.0) incoming.change else incoming.price - previousPrice
        val changePercent = if (incoming.changePercent != 0.0) {
            incoming.changePercent
        } else if (previousPrice != 0.0) {
            (change / previousPrice) * 100.0
        } else {
            0.0
        }
        return ForexPair(
            symbol = template?.symbol ?: incoming.symbol,
            name = template?.name ?: incoming.name.ifBlank { incoming.symbol },
            price = incoming.price,
            change = change,
            changePercent = changePercent,
            category = template?.category ?: incoming.category
        )
    }

    private fun isUsdtSymbol(symbol: String): Boolean {
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
