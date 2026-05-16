package com.trading.app.data

import com.trading.app.components.SymbolQuote
import com.trading.app.models.OHLCData

class BinanceChartService(
    private val tradingMode: BinanceTradingMode = BinanceTradingMode.LIVE,
    private val marketType: BinanceMarketType = BinanceMarketType.FUTURES,
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onHistoryUpdate: (String, List<OHLCData>) -> Unit = { _, _ -> }
) {
    private val delegate = BinanceService(
        tradingMode = tradingMode,
        marketType = marketType,
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

    fun isRegionBlocked(): Boolean = delegate.isRegionBlocked()

    fun disconnect() {
        delegate.disconnect()
    }

    private fun normalizeSymbol(symbol: String): String {
        val cleaned = symbol.trim().uppercase().replace("/", "").replace("_", "").replace(" ", "")
        return if (cleaned.endsWith("USDT")) cleaned else "${cleaned}T"
    }
}
