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

data class TimedPrice(val timestampMillis: Long, val price: Double)

object MarketDataStore {
    private const val historyLength = 40
    private const val timedHistoryLength = 4000
    private const val TAG = "MarketDataStore"

    private val _allPairs = MutableStateFlow(FOREX_PAIRS.filterNot { isUsdtSymbol(it.symbol) })
    val allPairs: StateFlow<List<ForexPair>> = _allPairs.asStateFlow()

    private val _priceHistory = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Double>>> = _priceHistory.asStateFlow()
    private val _timedPriceHistory = MutableStateFlow<Map<String, List<TimedPrice>>>(emptyMap())
    val timedPriceHistory: StateFlow<Map<String, List<TimedPrice>>> = _timedPriceHistory.asStateFlow()

    fun pairSnapshot(symbol: String): ForexPair? {
        if (isUsdtSymbol(symbol)) {
            return BinanceDataStore.pairSnapshot(symbol)
        }
        return findBestMatch(_allPairs.value, symbol)
    }

    fun historySnapshot(symbol: String): List<Double> {
        if (isUsdtSymbol(symbol)) {
            return BinanceDataStore.historySnapshot(symbol)
        }
        val pair = pairSnapshot(symbol) ?: return emptyList()
        return _priceHistory.value[pair.symbol] ?: emptyList()
    }

    fun pairFlow(symbol: String): Flow<ForexPair?> {
        if (isUsdtSymbol(symbol)) {
            return BinanceDataStore.pairFlow(symbol)
        }
        return allPairs
            .map { pairs -> findBestMatch(pairs, symbol) }
            .distinctUntilChanged()
    }

    fun historyFlow(symbol: String): Flow<List<Double>> {
        if (isUsdtSymbol(symbol)) {
            return BinanceDataStore.historyFlow(symbol)
        }
        return combine(pairFlow(symbol), priceHistory) { pair, history ->
            if (pair == null) {
                emptyList()
            } else {
                history[pair.symbol] ?: emptyList()
            }
        }.distinctUntilChanged()
    }

    fun matchesSymbol(left: String, right: String): Boolean {
        val leftVariants = normalizedVariants(left)
        val rightVariants = normalizedVariants(right)
        val match = leftVariants.intersect(rightVariants).isNotEmpty()
        val leftQuote = cryptoQuoteAsset(left)
        val rightQuote = cryptoQuoteAsset(right)
        val sameQuote = leftQuote != null && leftQuote == rightQuote
        
        if (!match && sameQuote && (left.startsWith("ETH", true) && right.startsWith("ETH", true))) {
            // Force match for ETH variants if they somehow missed the variant check
            return true
        }
        if (!match && sameQuote && (left.startsWith("BTC", true) && right.startsWith("BTC", true))) {
            // Force match for BTC variants
            return true
        }
        
        return match
    }

    fun updatePair(incoming: ForexPair) {
        if (isUsdtSymbol(incoming.symbol)) {
            BinanceDataStore.updatePair(incoming)
            return
        }

        val currentPairs = _allPairs.value
        val updatedPairs = currentPairs.map { existing ->
            if (!shouldMirrorUpdate(existing, incoming)) {
                existing
            } else {
                existing.copy(
                    price = incoming.price,
                    change = incoming.change,
                    changePercent = incoming.changePercent
                )
            }
        }

        if (updatedPairs == currentPairs) {
            if (incoming.category == MarketCategory.FOREX || incoming.category == MarketCategory.STOCK) {
                Log.w(TAG, "Ignored unmatched ${incoming.category} update: ${incoming.symbol} ${incoming.price}")
            }
            return
        }

        _allPairs.value = updatedPairs

        if (incoming.category == MarketCategory.FOREX || incoming.category == MarketCategory.STOCK) {
            Log.i(TAG, "Applied ${incoming.category} update: ${incoming.symbol} ${incoming.price}")
        }
        
        // Record telemetry for Market Data Bus - use CTRADER_LIVE as default for Pepperstone
        SystemTelemetry.recordTick("CTRADER_LIVE", 5.0) // cTrader typically has ~5ms latency

        val updateTimestamp = System.currentTimeMillis()
        val nextHistory = _priceHistory.value.toMutableMap()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        updatedPairs
            .filter { shouldMirrorUpdate(it, incoming) }
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
        if (isUsdtSymbol(symbol)) {
            BinanceDataStore.replaceHistory(symbol, prices)
            return
        }

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
        if (isUsdtSymbol(symbol)) {
            BinanceDataStore.replaceTimedHistory(symbol, prices)
            return
        }

        val pair = pairSnapshot(symbol) ?: return
        val sanitized = prices
            .filter { it.timestampMillis > 0L && it.price.isFinite() && it.price > 0.0 }
            .sortedBy { it.timestampMillis }
            .takeLast(timedHistoryLength)
        if (sanitized.isEmpty()) {
            return
        }

        val matchingPairs = _allPairs.value
            .filter { shouldMirrorUpdate(it, pair) }
            .ifEmpty { listOf(pair) }
        val nextHistory = _priceHistory.value.toMutableMap()
        val nextTimedHistory = _timedPriceHistory.value.toMutableMap()
        matchingPairs.forEach { matchingPair ->
            nextHistory[matchingPair.symbol] = sanitized.map { it.price }.takeLast(historyLength)
            nextTimedHistory[matchingPair.symbol] = sanitized
        }
        _priceHistory.value = nextHistory
        _timedPriceHistory.value = nextTimedHistory
    }

    private fun shouldMirrorUpdate(existing: ForexPair, incoming: ForexPair): Boolean {
        if (existing.category != incoming.category) {
            return false
        }

        if (matchesSymbol(existing.symbol, incoming.symbol)) {
            return true
        }

        val existingBase = cryptoBaseAsset(existing.symbol, existing.category)
        val incomingBase = cryptoBaseAsset(incoming.symbol, incoming.category)
        val existingQuote = cryptoQuoteAsset(existing.symbol)
        val incomingQuote = cryptoQuoteAsset(incoming.symbol)
        return existingBase != null &&
            existingBase == incomingBase &&
            existingQuote != null &&
            existingQuote == incomingQuote
    }

    private fun normalizedVariants(symbol: String): Set<String> {
        val normalized = normalizeSymbol(symbol)
        return setOf(normalized)
    }

    private fun cryptoQuoteAsset(symbol: String): String? {
        val normalized = normalizeSymbol(symbol)
        return when {
            normalized.endsWith("USDT") -> "USDT"
            normalized.endsWith("USD") -> "USD"
            else -> null
        }
    }

    private fun cryptoBaseAsset(symbol: String, category: MarketCategory?): String? {
        // More lenient check for crypto base assets
        val normalized = normalizeSymbol(symbol)
        
        // List of common crypto assets to explicitly match
        val knownBases = listOf("BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "DOGE", "AVAX")
        for (base in knownBases) {
            if (normalized.startsWith(base)) {
                return base
            }
        }

        val quote = when {
            normalized.endsWith("USDT") -> "USDT"
            normalized.endsWith("USD") -> "USD"
            else -> null
        } ?: return null

        val base = normalized.removeSuffix(quote)
        return base.takeIf { it.length in 2..10 && it.any(Char::isLetter) }
    }

    private fun normalizeSymbol(symbol: String): String {
        var normalized = symbol
            .uppercase(Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
            .replace(".", "")
        
        // Strip common trading suffixes to ensure "MSFT.US-24" matches "MSFT"
        val suffixes = listOf("US24", "US", "F", "M", "PRO", "ECN", "S", "SPOT", "P")
        for (suffix in suffixes) {
            if (normalized.endsWith(suffix) && normalized.length > suffix.length) {
                // Only strip if it's a suffix and leaves a valid base
                // Special case: don't strip 'F' from 'USDCHF' or 'US' from 'EURUSD'
                if (suffix == "F" && (normalized.endsWith("CHF") || normalized.endsWith("XAU") || normalized.endsWith("XAG"))) continue
                if (suffix == "US" && (normalized.startsWith("EUR") || normalized.startsWith("GBP") || normalized.startsWith("AUD"))) continue
                
                normalized = normalized.substring(0, normalized.length - suffix.length)
                break
            }
        }

        // Special mappings for Indices and Bonds
        return when (normalized) {
            "USTN10YR" -> "US10Y"
            "USTN2YR" -> "US02Y"
            else -> normalized
        }
    }

    private fun isUsdtSymbol(symbol: String): Boolean {
        return normalizeSymbol(symbol).endsWith("USDT")
    }

    private fun findBestMatch(pairs: List<ForexPair>, symbol: String): ForexPair? {
        val normalized = normalizeSymbol(symbol)
        return pairs.firstOrNull { normalizeSymbol(it.symbol) == normalized }
            ?: pairs.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
            ?: pairs.firstOrNull { matchesSymbol(it.symbol, symbol) }
    }
}
