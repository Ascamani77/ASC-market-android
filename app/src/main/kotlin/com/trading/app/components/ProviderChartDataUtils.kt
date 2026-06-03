package com.trading.app.components

import com.trading.app.models.OHLCData
import java.util.Locale

internal fun providerNormalizeSymbol(symbol: String): String {
    return symbol.trim().uppercase(Locale.US).replace("/", "").replace("-", "").replace("_", "").replace(" ", "")
        .let { if (it.length > 1 && it.endsWith("M")) it.dropLast(1) else it }
}

internal fun providerSymbolsMatch(left: String, right: String): Boolean {
    return providerNormalizeSymbol(left) == providerNormalizeSymbol(right)
}

internal fun sanitizeProviderHistory(history: List<OHLCData>): List<OHLCData> {
    return history
        .asSequence()
        .mapNotNull { candle ->
            if (candle.time <= 0L) return@mapNotNull null
            if (!candle.open.isFinite() || !candle.high.isFinite() || !candle.low.isFinite() || !candle.close.isFinite()) return@mapNotNull null
            val high = maxOf(candle.high, candle.open, candle.close)
            val low = minOf(candle.low, candle.open, candle.close)
            OHLCData(
                time = candle.time,
                open = candle.open,
                high = high,
                low = low,
                close = candle.close,
                volume = candle.volume.coerceAtLeast(0f)
            )
        }
        .sortedBy(OHLCData::time)
        .distinctBy(OHLCData::time)
        .toList()
}

internal fun mergeProviderHistory(existing: List<OHLCData>, incoming: List<OHLCData>, loadingMore: Boolean): List<OHLCData> {
    val cleanIncoming = sanitizeProviderHistory(incoming)
    if (cleanIncoming.isEmpty()) return existing
    
    // Always merge to prevent losing history when receiving single-candle updates
    return (existing + cleanIncoming)
        .distinctBy(OHLCData::time)
        .sortedBy(OHLCData::time)
        .takeLast(10000)
}

internal fun providerDisplayQuote(quote: SymbolQuote, displaySymbol: String, candles: List<OHLCData>): SymbolQuote {
    val prevClose = candles.getOrNull(candles.size - 2)?.close ?: quote.lastPrice
    val change = quote.lastPrice - prevClose
    val changePercent = if (prevClose != 0f) (change / prevClose) * 100f else 0f
    return quote.copy(
        name = displaySymbol,
        change = change,
        changePercent = changePercent
    )
}
