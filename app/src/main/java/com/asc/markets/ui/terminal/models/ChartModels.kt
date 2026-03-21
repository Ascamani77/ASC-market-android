package com.asc.markets.ui.terminal.models

data class DrawingItem(
    val id: String,
    val type: String,
    val visible: Boolean = true,
    val locked: Boolean = false
)

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double? = null,
    val rsi: Double? = null
)

data class ChartSettings(
    val canvas: CanvasSettings = CanvasSettings(),
    val statusLine: StatusLineSettings = StatusLineSettings()
)

data class CanvasSettings(
    val gridColor: String = "#1E222D",
    val textColor: String = "#9194A1",
    val upColor: String = "#089981",
    val downColor: String = "#F23645"
)

data class StatusLineSettings(
    val showTitle: Boolean = true,
    val showOhlc: Boolean = true,
    val showIndicators: Boolean = true
)
