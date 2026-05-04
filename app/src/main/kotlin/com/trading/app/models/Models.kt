package com.trading.app.models

import androidx.compose.ui.graphics.vector.ImageVector

data class ColorPickerState(
    val title: String,
    val initialHex: String,
    val isCrosshair: Boolean = false,
    val showThickness: Boolean = true,
    val showLineStyle: Boolean = false,
    val initialThickness: Int = 1,
    val initialLineStyle: Int = 0,
    val onCrosshairUpdate: ((String, Int, Int) -> Unit)? = null,
    val onThicknessChange: ((Int) -> Unit)? = null,
    val onLineStyleChange: ((Int) -> Unit)? = null,
    val onAddClick: (() -> Unit)? = null,
    val onColorSelect: (String) -> Unit
)

data class SymbolSettings(
    val upColor: String = "#089981",
    val downColor: String = "#f23645",
    val bodyVisible: Boolean = true,
    val borderVisible: Boolean = true,
    val borderColorUp: String = "#089981",
    val borderColorDown: String = "#f23645",
    val wickVisible: Boolean = true,
    val wickColorUp: String = "#089981",
    val wickColorDown: String = "#f23645",
    val barColorer: Boolean = false, // Color bars based on previous close
    val hlcBars: Boolean = false,
    val thinBars: Boolean = false,
    val openVisible: Boolean = true,
    val highVisible: Boolean = true,
    val lowVisible: Boolean = true,
    val closeVisible: Boolean = true,
    val precision: String = "Default",
    val timezone: String = "(UTC-7) Los Angeles"
)

data class StatusLineSettings(
    val logo: Boolean = true,
    val symbol: Boolean = true,
    val titleMode: String = "Description",
    val openMarketStatus: Boolean = true,
    val ohlc: Boolean = false, // Set to false by default
    val barChangeValues: Boolean = true,
    val volume: Boolean = true,
    val lastDayChange: Boolean = false,
    val indicatorTitles: Boolean = true,
    val indicatorInputs: Boolean = true,
    val indicatorValues: Boolean = true,
    val indicatorBackground: Boolean = true,
    val indicatorBackgroundColor: String = "#2a2e39",
    val indicatorBackgroundOpacity: Int = 50
)

data class ScalesSettings(
    val currencyAndUnit: String = "Always visible",
    val scaleModes: String = "Visible on tap",
    val autoScale: Boolean = true,
    val scaleType: String = "Regular", // Options: "Regular", "Percent", "Indexed to 100", "Logarithmic"
    val lockRatio: Boolean = false,
    val lockRatioValue: String = "17.2210131",
    val scalePriceChartOnly: Boolean = false,
    val invertScale: Boolean = false,
    val scalesPlacement: String = "Auto",
    val noOverlappingLabels: Boolean = false,
    val plusButton: Boolean = true,
    val countdown: Boolean = false, // Set to false by default as requested
    val symbolLabel: String = "Price",
    val symbolLineColor: String = "#FFFFFF",
    val symbolLastValueMode: String = "Value according to scale",
    val highLowMode: String = "Value, line",
    val highLowLineColor: String = "#FFFFFF",
    val highLowLabelColor: String = "#2962FF",
    val highLowCalculationMode: String = "Dynamic", // Options: "500 candles", "100 candles", "Dynamic"
    val indicatorsAndFinancials: String = "Value or name",
    val bidAskMode: String = "Value, line",
    val bidColor: String = "#2962FF",
    val askColor: String = "#F05252",
    val bidAskLabels: Boolean = false,
    val indicatorsAndFinancialsNameLabels: Boolean = false,
    val indicatorsAndFinancialsValueLabels: Boolean = false,
    val symbolLastPriceLine: Boolean = true,
    val symbolPrevCloseLine: Boolean = false,
    val prePostMarketPriceLine: Boolean = false,
    val highLowPriceLines: Boolean = true,
    val bidAskLines: Boolean = false,
    val dayOfWeekOnLabels: Boolean = true,
    val dateFormat: String = "Mon 29 Sep '97",
    val timeFormat: String = "24-hours",
    val saveLeftEdge: Boolean = true,
    val symbolNameLabel: Boolean = false,
    val symbolLastPriceLabel: Boolean = false,
    val symbolPrevCloseLabel: Boolean = false,
    val prePostMarketPriceLabel: Boolean = false,
    val highLowPriceLabels: Boolean = false,
    val hideHeaderPane: Boolean = false,
    val hideAssetLastViewedPane: Boolean = false
)

data class CanvasSettings(
    val backgroundType: String = "Solid",
    val background: String = "#000000",
    val backgroundGradientEnd: String = "#0c0c0d",
    val gridVisible: Boolean = true,
    val gridType: String = "Vert and horz",
    val gridColor: String = "#1f222d",
    val horzGridColor: String = "#1f222d",
    val gridOpacity: Int = 20, // 0 to 100
    val crosshairColor: String = "#758696",
    val crosshairThickness: Int = 1,
    val crosshairLineStyle: String = "Dashed", // Options: "Solid", "Dashed", "Dotted"
    val watermarkVisible: Boolean = false,
    val watermarkType: String = "Replay mode",
    val watermarkColor: String = "#662A2E39",
    val scaleTextColor: String = "#d1d4dc",
    val scaleFontSize: Int = 11,
    val scaleFontBold: Boolean = false,
    val headerFontSize: Int = 14,
    val headerFontBold: Boolean = false,
    val bottomFontSize: Int = 13,
    val bottomFontBold: Boolean = false,
    val sidebarFontSize: Int = 15,
    val sidebarFontBold: Boolean = false,
    val sidebarIconSize: Int = 24,
    val chartItemFontSize: Int = 12,
    val symbolFontSize: Int = 14,
    val scaleLineColor: String = "#2a2e39",
    val navigationButtons: String = "Visible on mouse over",
    val paneButtons: String = "Visible on mouse over",
    val marginTop: Int = 15,
    val marginBottom: Int = 1,
    val marginRight: Int = 10,
    val fullChartColor: String = "Default", // Options: "Default", "Pure Black", "Dark Blue", "OLED Black"
    val headerVisible: Boolean = true,
    val headerVisibility: String = "Always visible", // Options: "Always visible", "Auto-hide"
    val swapHeaderAndFooter: Boolean = false
)

data class TradingSettings(
    val buySellButtons: Boolean = false, // Set to false by default
    val showBuySellLabels: Boolean = true,
    val oneClickTrading: Boolean = false,
    val executionSound: Boolean = false,
    val executionSoundVolume: Int = 50,
    val executionSoundType: String = "Alarm Clock",
    val rejectionNotifications: Boolean = false,
    val positionsAndOrders: Boolean = true,
    val reversePositionButton: Boolean = true,
    val projectOrder: Boolean = false,
    val profitLossValue: Boolean = true,
    val positionsMode: String = "Money",
    val bracketsMode: String = "Money",
    val executionMarks: Boolean = true,
    val executionLabels: Boolean = false,
    val extendedPriceLines: Boolean = true,
    val alignment: String = "Right",
    val screenshotVisibility: Boolean = false
)

data class AlertsSettings(
    val alertLines: Boolean = true,
    val alertLinesColor: String = "#FF0000",
    val onlyActiveAlerts: Boolean = false,
    val hideToasts: Boolean = false
)

data class EventsSettings(
    val ideas: Boolean = true,
    val ideasMode: String = "All ideas",
    val economicEvents: Boolean = true,
    val onlyFutureEvents: Boolean = false,
    val eventsBreaks: Boolean = false,
    val eventsBreaksColor: String = "#434651",
    val latestNews: Boolean = false,
    val newsNotification: Boolean = false
)

data class QuickActionsSettings(
    val isLocked: Boolean = false,
    val buttonX: Int = 16,
    val buttonY: Int = 400,
    val modalX: Int = 100,
    val modalY: Int = 200,
    val isSidebarVisible: Boolean = false,
    val isTimezoneVisible: Boolean = true
)

data class IndicatorsSettings(
    val showRsi: Boolean = false,
    val rsiPeriod: Int = 14,
    val rsiShowLabels: Boolean = true,
    val rsiShowLines: Boolean = false,
    val showEma10: Boolean = false,
    val ema10Period: Int = 10,
    val ema10ShowLabels: Boolean = true,
    val ema10ShowLines: Boolean = false,
    val showEma20: Boolean = false,
    val ema20Period: Int = 20,
    val ema20ShowLabels: Boolean = true,
    val ema20ShowLines: Boolean = false,
    val showSma1: Boolean = false,
    val sma1Period: Int = 21,
    val sma1ShowLabels: Boolean = true,
    val sma1ShowLines: Boolean = false,
    val showSma2: Boolean = false,
    val sma2Period: Int = 10,
    val sma2ShowLabels: Boolean = true,
    val sma2ShowLines: Boolean = false,
    val showVwap: Boolean = false,
    val vwapShowLabels: Boolean = true,
    val vwapShowLines: Boolean = false,
    val showBb: Boolean = false,
    val bbPeriod: Int = 20,
    val bbStdDev: Float = 2f,
    val bbShowLabels: Boolean = true,
    val bbShowLines: Boolean = false,
    val showAtr: Boolean = false,
    val atrPeriod: Int = 14,
    val atrShowLabels: Boolean = true,
    val atrShowLines: Boolean = false,
    val showMacd: Boolean = false,
    val macdFast: Int = 12,
    val macdSlow: Int = 26,
    val macdSignal: Int = 9,
    val macdShowLabels: Boolean = true,
    val macdShowLines: Boolean = false,
    val volumeShowLabels: Boolean = true,
    val volumeShowLines: Boolean = false
)

data class ChartSettings(
    val symbol: SymbolSettings = SymbolSettings(),
    val statusLine: StatusLineSettings = StatusLineSettings(),
    val scales: ScalesSettings = ScalesSettings(),
    val canvas: CanvasSettings = CanvasSettings(),
    val trading: TradingSettings = TradingSettings(),
    val alerts: AlertsSettings = AlertsSettings(),
    val events: EventsSettings = EventsSettings(),
    val quickActions: QuickActionsSettings = QuickActionsSettings(),
    val indicators: IndicatorsSettings = IndicatorsSettings()
)

data class TradeNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val volume: Float,
    val price: Float,
    val isBuy: Boolean,
    val type: String // "executed", "tp_placed", "sl_placed"
)

data class OHLCData(
    val time: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float = 0f
)

data class Drawing(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,
    val points: List<ChartPoint>,
    val color: String = "#2962FF",
    val width: Float = 2f,
    val text: String? = null,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true
)

data class ChartPoint(
    val time: Long,
    val price: Float,
    val x: Float = 0f,
    val y: Float = 0f
)

data class SymbolInfo(
    val ticker: String,
    val name: String,
    val exchange: String = "",
    val type: String = "",
    val brokerSymbol: String = ticker,
    val price: Float = 0f,
    val change: Float = 0f,
    val changePercent: Float = 0f
)

data class TimeZone(
    val label: String,
    val value: String,
    val offsetLabel: String
)

data class UserAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val condition: String,
    val price: Float,
    val message: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class PartialOrder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tp: Float? = null,
    val sl: Float? = null,
    val volume: Float,
    val tpOrderPrice: String = "Market",
    val slOrderPrice: String = "Market"
)

data class Position(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val type: String, // "buy" or "sell"
    val entryPrice: Float,
    val volume: Float,
    val time: Long,
    val tp: Float? = null,
    val sl: Float? = null,
    val leverage: String = "1x",
    val margin: Float = 0f,
    val isSelected: Boolean = false,
    val partialOrders: List<PartialOrder> = emptyList()
)

data class Order(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val type: String, // "buy" or "sell"
    val orderType: String, // "Market", "Limit", "Stop", "Stop Limit" etc
    val status: String, // "Working", "Inactive", "Filled", "Cancelled", "Rejected"
    val price: Float,
    val stopLimitPrice: Float? = null,
    val volume: Float,
    val time: Long,
    val closingTime: Long? = null,
    val leverage: String = "1x",
    val margin: Float = 0f,
    val filledQuantity: Float = 0f,
    val averagePrice: Float = 0f,
    val tp: Float? = null,
    val sl: Float? = null,
    val expiry: Long? = null
)

data class BalanceRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val time: Long,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val realizedPnl: Double,
    val action: String
)

data class CalendarDayChip(
    val isoDate: String,
    val dayNumber: Int,
    val dayLabel: String,
    val isSelected: Boolean = false,
    val isToday: Boolean = false
)

data class EconomicCalendarDisplayEvent(
    val id: Long,
    val isoDateTime: String,
    val releaseTimeLabel: String,
    val countryCode: String,
    val countryName: String,
    val currencyCode: String,
    val title: String,
    val actual: String,
    val forecast: String,
    val previous: String,
    val importance: String,
    val impactDirection: Int,
    val isSpeechOrReport: Boolean = false,
    val isAllDay: Boolean = false,
    val detailsUrl: String? = null
)

data class EconomicCalendarDisplayPayload(
    val sourceLabel: String,
    val rangeStartIso: String,
    val rangeEndIso: String,
    val selectedDateIso: String,
    val headerDateLabel: String,
    val dayChips: List<CalendarDayChip>,
    val events: List<EconomicCalendarDisplayEvent>,
    val lastUpdatedIso: String
)

data class EconomicCalendarAiEvent(
    val id: Long,
    val isoDateTime: String,
    val dateIso: String,
    val currencyCode: String,
    val countryCode: String,
    val countryName: String,
    val title: String,
    val importance: String,
    val actual: String,
    val forecast: String,
    val previous: String,
    val impactDirection: Int,
    val eventType: Int,
    val timeMode: Int,
    val processed: Boolean,
    val detailsUrl: String? = null
)

data class EconomicCalendarAiPayload(
    val source: String,
    val generatedAtIso: String,
    val selectedDateIso: String,
    val rangeStartIso: String,
    val rangeEndIso: String,
    val events: List<EconomicCalendarAiEvent>
)

data class EconomicCalendarPayload(
    val display: EconomicCalendarDisplayPayload,
    val ai: EconomicCalendarAiPayload
)

data class JournalEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val time: String,
    val text: String
)

data class Indicator(
    val id: String,
    val name: String,
    val description: String,
    val favorite: Boolean = false
)

data class ToolItem(
    val id: String,
    val name: String,
    val icon: ImageVector
)

data class ChartSnapshot(
    val drawings: List<Drawing>,
    val activeIndicators: List<String>
)

data class NewsItem(
    val id: Int,
    val title: String,
    val timeLabel: String,
    val isoDateTime: String,
    val countryCode: String,
    val category: String,
    val detailsUrl: String? = null
)

data class NewsPayload(
    val type: String,
    val items: List<NewsItem>,
    val lastUpdatedIso: String
)

data class IndicatorData(
    val rsi: Float? = null,
    val macd: Float? = null,
    val macdSignal: Float? = null,
    val macdHistogram: Float? = null,
    val atr: Float? = null
)

data class BondData(
    val seriesId: String,
    val value: Float,
    val date: String,
    val name: String = seriesId
)
