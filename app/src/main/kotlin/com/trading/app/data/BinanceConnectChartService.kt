package com.trading.app.data

import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData

/**
 * Chart Service Wrapper for Binance Connect
 * Independent wrapper that uses BinanceConnectService
 */
class BinanceConnectChartService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val delegate = BinanceConnectService(
        onQuoteUpdate = onQuoteUpdate,
        onHistoryUpdate = onHistoryUpdate
    )

    fun streamActiveSymbol(symbol: String, timeframe: String) {
        delegate.streamActiveSymbol(normalizeSymbol(symbol), timeframe)
    }

    fun fetchHistory(symbol: String, timeframe: String, endTime: Long? = null) {
        delegate.fetchHistory(normalizeSymbol(symbol), timeframe, endTime)
    }

    fun stopActiveStream() {
        delegate.stopActiveStream()
    }

    fun disconnect() {
        delegate.disconnect()
    }

    private fun normalizeSymbol(symbol: String): String {
        return symbol.trim()
            .uppercase(java.util.Locale.US)
            .replace("/", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }
}
