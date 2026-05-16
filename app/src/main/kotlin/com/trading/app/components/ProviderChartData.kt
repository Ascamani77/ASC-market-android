package com.trading.app.components

import com.trading.app.models.OHLCData

data class ProviderChartData(
    val candles: List<OHLCData> = emptyList(),
    val quote: SymbolQuote? = null,
    val isLoadingMore: Boolean = false,
    val hasMoreHistory: Boolean = true,
    val onLoadMoreHistory: (Long) -> Unit = {}
)
