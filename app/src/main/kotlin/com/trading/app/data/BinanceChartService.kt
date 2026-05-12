package com.trading.app.data

import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData

class BinanceChartService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val delegate = BinanceService(
        onQuoteUpdate = onQuoteUpdate,
        onHistoryUpdate = onHistoryUpdate
    )

    fun streamActiveSymbol(symbol: String) {
        delegate.streamActiveSymbol(normalizeSymbol(symbol))
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
        val cleaned = symbol.trim().uppercase().replace("/", "").replace("_", "").replace(" ", "")
        return if (cleaned.endsWith("USDT")) cleaned else "${cleaned}T"
    }
}
