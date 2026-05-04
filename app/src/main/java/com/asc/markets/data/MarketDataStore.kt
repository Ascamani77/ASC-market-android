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

object MarketDataStore {
    private const val historyLength = 40
    private const val TAG = "MarketDataStore"

    private val _allPairs = MutableStateFlow(FOREX_PAIRS)
    val allPairs: StateFlow<List<ForexPair>> = _allPairs.asStateFlow()

    private val _priceHistory = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Double>>> = _priceHistory.asStateFlow()

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

    fun matchesSymbol(left: String, right: String): Boolean {
        val leftVariants = normalizedVariants(left)
        val rightVariants = normalizedVariants(right)
        val match = leftVariants.intersect(rightVariants).isNotEmpty()
        
        if (!match && (left.startsWith("ETH", true) && right.startsWith("ETH", true))) {
            // Force match for ETH variants if they somehow missed the variant check
            return true
        }
        if (!match && (left.startsWith("BTC", true) && right.startsWith("BTC", true))) {
            // Force match for BTC variants
            return true
        }
        
        return match
    }

    fun updatePair(incoming: ForexPair) {
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

        val nextHistory = _priceHistory.value.toMutableMap()
        updatedPairs
            .filter { shouldMirrorUpdate(it, incoming) }
            .forEach { pair ->
                val previous = nextHistory[pair.symbol].orEmpty()
                nextHistory[pair.symbol] = (previous + pair.price).takeLast(historyLength)
            }
        _priceHistory.value = nextHistory
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
        return existingBase != null && existingBase == incomingBase
    }

    private fun normalizedVariants(symbol: String): Set<String> {
        val normalized = normalizeSymbol(symbol)
        val variants = mutableSetOf(normalized)
        val cryptoBase = cryptoBaseAsset(symbol, null)
        if (cryptoBase != null) {
            variants += "${cryptoBase}USD"
            variants += "${cryptoBase}USDT"
        }
        return variants
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
        
        // Strip common broker suffixes to ensure "BTCUSD.m" matches "BTCUSD"
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
