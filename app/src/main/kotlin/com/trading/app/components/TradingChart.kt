package com.trading.app.components

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.asc.markets.data.NetworkConfig
import com.trading.app.data.ChartFeedType
import com.trading.app.data.Mt5Service
import com.trading.app.data.Mt5ReverseBridge
import com.trading.app.data.chartFeedQuotes
import com.trading.app.data.chartFeedSymbolFor
import com.trading.app.models.ChartSettings
import com.trading.app.models.Drawing
import com.trading.app.models.Position
import com.trading.app.models.Order
import com.trading.app.models.BalanceRecord
import com.trading.app.models.UserAlert
import com.trading.app.models.EconomicCalendarPayload
import com.trading.app.models.SymbolInfo
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.series.common.PriceLine
import com.tradingview.lightweightcharts.api.options.enums.PriceAxisPosition
import com.tradingview.lightweightcharts.api.options.models.*
import com.tradingview.lightweightcharts.api.series.models.*
import com.tradingview.lightweightcharts.view.ChartsView
import java.util.*
import com.tradingview.lightweightcharts.api.series.enums.*
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import android.graphics.Color as AndroidColor
import com.trading.app.models.OHLCData
import com.trading.app.models.IndicatorData
import com.trading.app.indicators.BbandsData
import com.trading.app.indicators.VwapData
import com.trading.app.utils.Indicators
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import com.tradingview.lightweightcharts.api.series.models.Time
import com.asc.markets.ui.components.AppBottomNavHeight

private const val LOG_TAG = "TradingChart"
private const val MACD_SCALE_KEY = "macd_pane"
private const val VOLUME_SCALE_KEY = "volume_pane"
private const val ATR_SCALE_KEY = "atr_pane"
private const val MT5_HISTORY_PAGE_SIZE = 100
private const val INDICATOR_PANE_HEIGHT = 0.14f
private const val RSI_PANE_HEIGHT = 0.18f
private const val INDICATOR_PANE_GAP = 0.08f
// Fraction of the total chart height given to the dedicated RSI ChartsView (real pane split)
private const val RSI_PANE_SPLIT_FRACTION = 0.22f
private const val CHART_TICK_THROTTLE_MS = 300L

private enum class MainSeriesKind {
    BAR,
    LINE,
    AREA,
    BASELINE,
    CANDLESTICK
}

fun Time.toTimestamp(): Long = (this as? Time.Utc)?.timestamp ?: 0L
private fun Long.toChartTime(): Time = Time.Utc(this)
private fun OHLCData.toCandlestickData(): CandlestickData =
    CandlestickData(time = time.toChartTime(), open = open, high = high, low = low, close = close)
private fun OHLCData.toBarSeriesData(): BarData =
    BarData(time = time.toChartTime(), open = open, high = high, low = low, close = close)
private fun OHLCData.toLineSeriesData(value: Float = close): LineData =
    LineData(time = time.toChartTime(), value = value)
private fun OHLCData.toAreaSeriesData(value: Float = close): AreaData =
    AreaData(time = time.toChartTime(), value = value)
private fun OHLCData.toBaselineSeriesData(value: Float = close): BaselineData =
    BaselineData(time = time.toChartTime(), value = value)
private fun OHLCData.hlc3(): Float = (high + low + close) / 3f

private fun resolveMainSeriesKind(style: String): MainSeriesKind {
    // The Android binding supports fewer native chart styles than the selector exposes.
    // Unsupported desktop-only styles fall back to the closest visible renderer.
    return when (style) {
        "bars", "high_low", "columns" -> MainSeriesKind.BAR
        "line", "line_markers", "step_line", "line_break", "kagi" -> MainSeriesKind.LINE
        "area", "hlc_area" -> MainSeriesKind.AREA
        "baseline" -> MainSeriesKind.BASELINE
        else -> MainSeriesKind.CANDLESTICK
    }
}

private fun areaValueForStyle(style: String, candle: OHLCData): Float {
    return when (style) {
        "hlc_area" -> candle.hlc3()
        else -> candle.close
    }
}

// Data class to match the "Quote" structure
data class SymbolQuote(
    val name: String,
    val lastPrice: Float,
    val change: Float,
    val changePercent: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val prevClose: Float,
    val bid: Float,
    val ask: Float,
    val volume: Float,
    val spread: Float = 0.2f,
    val time: Long = 0L
)

fun getFullSymbolName(symbol: String): String {
    return when (symbol.uppercase()) {
        "BTCUSD" -> "Bitcoin / U.S. Dollar"
        "BTCUSDT" -> "Bitcoin / TetherUS"
        "ETHUSD" -> "Ethereum / U.S. Dollar"
        "ETHUSDT" -> "Ethereum / TetherUS"
        "EURUSD" -> "Euro / U.S. Dollar"
        "GBPUSD" -> "British Pound / U.S. Dollar"
        "USDJPY" -> "U.S. Dollar / Japanese Yen"
        "AUDUSD" -> "Australian Dollar / US Dollar"
        "USDCAD" -> "U.S. Dollar / Canadian Dollar"
        "USDCHF" -> "U.S. Dollar / Swiss Franc"
        "Crude-F" -> "WTI Crude Oil"
        "USOIL" -> "WTI Crude Oil"
        "Brent-F" -> "Brent Crude Oil"
        "BRENTOIL" -> "Brent Crude Oil"
        "XAUUSD" -> "Gold / US Dollar"
        "XAGUSD" -> "Silver / US Dollar"
        "AAPL" -> "Apple Inc."
        "MSFT" -> "Microsoft Corporation"
        "AMZN" -> "Amazon.com, Inc."
        "NVDA" -> "NVIDIA Corporation"
        "TSLA" -> "Tesla, Inc."
        "SPX" -> "S&P 500 Index"
        "NASDAQ100" -> "Nasdaq 100 Index"
        "DJIA" -> "Dow Jones Industrial Average"
        "US10Y" -> "United States 10Y Gov Bond"
        "US02Y" -> "United States 2Y Gov Bond"
        "DGS2" -> "US 2-Year Treasury Yield"
        "DGS10" -> "US 10-Year Treasury Yield"
        else -> symbol
    }
}

private fun applyOpacity(color: Int, opacity: Int): Int {
    val alpha = (opacity / 100f * 255).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (alpha shl 24)
}

private fun getFullChartColor(colorSetting: String, customBg: String): Int {
    return when (colorSetting) {
        "Pure Black" -> android.graphics.Color.BLACK
        "Dark Blue" -> android.graphics.Color.parseColor("#0a0e27")
        "OLED Black" -> android.graphics.Color.parseColor("#0d0f1a")
        else -> try { android.graphics.Color.parseColor(customBg) } catch (e: Exception) { android.graphics.Color.BLACK }
    }
}

private fun toPriceScaleMode(scaleType: String): PriceScaleMode {
    return when (scaleType) {
        "Percent" -> PriceScaleMode.PERCENTAGE
        "Indexed to 100" -> PriceScaleMode.INDEXED_TO_100
        "Logarithmic" -> PriceScaleMode.LOGARITHMIC
        else -> PriceScaleMode.NORMAL
    }
}

private fun normalizeEpochSeconds(timestamp: Long): Long {
    return when {
        timestamp <= 0L -> 0L
        timestamp >= 1_000_000_000_000L -> timestamp / 1000L
        else -> timestamp
    }
}

private fun normalizeChartSymbol(symbol: String): String {
    return symbol
        .trim()
        .uppercase(Locale.US)
        .replace("/", "")
        .removeSuffix(".M")
        .removeSuffix(".PRO")
        .removeSuffix(".ECN")
        .removeSuffix(".S")
        .removeSuffix(".SPOT")
        .removeSuffix("+")
        .let { if (it.length > 1 && it.endsWith("M")) it.dropLast(1) else it }
}

private fun chartSymbolsMatch(left: String, right: String): Boolean {
    return left.trim().uppercase() == right.trim().uppercase()
}

private fun timeframeToSeconds(timeframe: String): Long {
    return when (timeframe.lowercase(Locale.US)) {
        "1m" -> 60L
        "5m" -> 5 * 60L
        "15m" -> 15 * 60L
        "30m" -> 30 * 60L
        "1h" -> 60 * 60L
        "4h" -> 4 * 60 * 60L
        "1d" -> 24 * 60 * 60L
        else -> 60 * 60L
    }
}

private fun alignToTimeframeStart(timestampSeconds: Long, timeframe: String): Long {
    val interval = timeframeToSeconds(timeframe)
    if (timestampSeconds <= 0L || interval <= 0L) return timestampSeconds
    return (timestampSeconds / interval) * interval
}

private fun applyTickToCandles(
    candles: List<OHLCData>,
    timeframe: String,
    lastPrice: Float,
    tickTimestampSeconds: Long,
    tickVolume: Float
): List<OHLCData> {
    if (!lastPrice.isFinite() || lastPrice <= 0f) return candles

    if (candles.isEmpty()) {
        val seededTime = if (tickTimestampSeconds > 0L) {
            alignToTimeframeStart(tickTimestampSeconds, timeframe)
        } else {
            0L
        }
        if (seededTime <= 0L) return candles
        return listOf(
            OHLCData(
                time = seededTime,
                open = lastPrice,
                high = lastPrice,
                low = lastPrice,
                close = lastPrice,
                volume = tickVolume.coerceAtLeast(0f)
            )
        )
    }

    val orderedCandles = candles.sortedBy(OHLCData::time)
    val interval = timeframeToSeconds(timeframe)
    val lastCandle = orderedCandles.last()
    val resolvedTickTime = if (tickTimestampSeconds > 0L) {
        alignToTimeframeStart(tickTimestampSeconds, timeframe)
    } else {
        lastCandle.time
    }

    if (resolvedTickTime < lastCandle.time) {
        return orderedCandles
    }

    val updatedCandles = orderedCandles.toMutableList()
    if (resolvedTickTime == lastCandle.time) {
        updatedCandles[updatedCandles.lastIndex] = lastCandle.copy(
            high = maxOf(lastCandle.high, lastPrice),
            low = minOf(lastCandle.low, lastPrice),
            close = lastPrice,
            volume = maxOf(lastCandle.volume, tickVolume.coerceAtLeast(0f))
        )
        return updatedCandles
    }

    var previousClose = lastCandle.close
    var nextBarTime = lastCandle.time + interval
    while (nextBarTime < resolvedTickTime) {
        updatedCandles.add(
            OHLCData(
                time = nextBarTime,
                open = previousClose,
                high = previousClose,
                low = previousClose,
                close = previousClose,
                volume = 0f
            )
        )
        nextBarTime += interval
    }

    updatedCandles.add(
        OHLCData(
            time = resolvedTickTime,
            open = previousClose,
            high = maxOf(previousClose, lastPrice),
            low = minOf(previousClose, lastPrice),
            close = lastPrice,
            volume = tickVolume.coerceAtLeast(0f)
        )
    )
    return updatedCandles
}

private fun safelyRemovePriceLine(api: SeriesApi?, priceLine: PriceLine?) {
    if (api == null || priceLine == null) return
    runCatching {
        api.removePriceLine(priceLine)
    }.onFailure { error ->
        Log.w(LOG_TAG, "Ignoring stale price line removal: ${error.message}")
    }
}

private fun calculateHeikinAshi(data: List<OHLCData>): List<CandlestickData> {
    if (data.isEmpty()) return emptyList()
    val haData = mutableListOf<CandlestickData>()
    var prevOpen = data[0].open
    var prevClose = data[0].close

    data.forEach { candle ->
        val close = (candle.open + candle.high + candle.low + candle.close) / 4f
        val open = (prevOpen + prevClose) / 2f
        val high = maxOf(candle.high, maxOf(open, close))
        val low = minOf(candle.low, minOf(open, close))
        
        haData.add(CandlestickData(candle.time.toChartTime(), open, high, low, close))
        
        prevOpen = open
        prevClose = close
    }
    return haData
}

private fun buildVolumeHistogramData(
    data: List<OHLCData>,
    growingColor: ComposeColor = ComposeColor(0xFF089981),
    fallingColor: ComposeColor = ComposeColor(0xFFF23645),
    colorBasedOnPreviousClose: Boolean = false
): List<HistogramData> {
    if (data.isEmpty()) return emptyList()

    val hasRealVolume = data.any { it.volume > 0f }
    val values = if (hasRealVolume) {
        data.map { it.volume.coerceAtLeast(0f) }
    } else {
        data.map {
            val body = kotlin.math.abs(it.close - it.open)
            val range = (it.high - it.low).coerceAtLeast(0f)
            maxOf(range, body, 0.0001f)
        }
    }

    return data.mapIndexed { index, candle ->
        val isGrowing = if (colorBasedOnPreviousClose && index > 0) {
            candle.close >= data[index - 1].close
        } else {
            candle.close >= candle.open
        }

        HistogramData(
            time = candle.time.toChartTime(),
            value = values[index],
            color = if (isGrowing) {
                IntColor(growingColor.toArgb())
            } else {
                IntColor(fallingColor.toArgb())
            }
        )
    }
}

private fun resolvePaneMargins(
    showVolume: Boolean,
    showRsi: Boolean,
    showMacd: Boolean,
    showAtr: Boolean = false,
    hiddenIndicators: Set<String> = emptySet()
): Map<String, PriceScaleMargins> {
    val margins = mutableMapOf<String, PriceScaleMargins>()

    val activePanes = buildList {
        if (showAtr && "ATR" !in hiddenIndicators) add(ATR_SCALE_KEY)
        if (showMacd && "MACD" !in hiddenIndicators) add(MACD_SCALE_KEY)
        // RSI renders in its own dedicated ChartsView below the main one - it no longer
        // reserves space inside the main chart's price scale.
    }

    val bottomInset = if (activePanes.firstOrNull() == RSI_SCALE_KEY) 0.04f else 0.0f
    var currentBottom = bottomInset

    activePanes.forEachIndexed { index, paneKey ->
        val paneHeight = if (paneKey == RSI_SCALE_KEY) RSI_PANE_HEIGHT else INDICATOR_PANE_HEIGHT
        margins[paneKey] = PriceScaleMargins(
            top = (1f - currentBottom - paneHeight).coerceAtLeast(0f),
            bottom = currentBottom
        )
        currentBottom += paneHeight
        if (index < activePanes.lastIndex) {
            currentBottom += INDICATOR_PANE_GAP
        }
    }

    val finalBottom = if (currentBottom == 0.0f) 0.04f else currentBottom
    margins["main"] = PriceScaleMargins(top = 0.06f, bottom = finalBottom)
    
    return margins
}

@Composable
fun TradingChart(
    symbol: String,
    timeframe: String,
    style: String,
    chartSettings: ChartSettings,
    drawings: List<Drawing>,
    onDrawingUpdate: (Drawing) -> Unit,
    activeTool: String?,
    onToolReset: () -> Unit,
    userAlerts: List<UserAlert> = emptyList(),
    onAlertTriggered: (UserAlert) -> Unit = {},
    showRsi: Boolean = false,
    rsiPeriod: Int = 14,
    showEma10: Boolean = false,
    ema10Period: Int = 10,
    showEma20: Boolean = false,
    ema20Period: Int = 20,
    showSma1: Boolean = false,
    sma1Period: Int = 21,
    showSma2: Boolean = false,
    sma2Period: Int = 10,
    showVwap: Boolean = false,
    showBb: Boolean = false,
    bbPeriod: Int = 20,
    bbStdDev: Float = 2f,
    showAtr: Boolean = false,
    atrPeriod: Int = 14,
    showMacd: Boolean = false,
    macdFast: Int = 12,
    macdSlow: Int = 26,
    macdSignal: Int = 9,
    showVolume: Boolean = true,
    showVolumeMa: Boolean = true,
    volumeMaLength: Int = 20,
    volumeMaColor: ComposeColor = ComposeColor(0xFF2962FF),
    volumeGrowingColor: ComposeColor = ComposeColor(0xFF089981),
    volumeFallingColor: ComposeColor = ComposeColor(0xFFF23645),
    volumeColorBasedOnPreviousClose: Boolean = false,
    isCrosshairActive: Boolean = false,
    onCrosshairToggle: (Boolean) -> Unit = {},
    onVolumeToggle: (Boolean) -> Unit = {},
    onIndicatorSettingsClick: (String) -> Unit = {},
    hiddenIndicators: Set<String> = emptySet(),
    onIndicatorHide: (String) -> Unit = {},
    chartFeedType: ChartFeedType? = null,
    providerChartData: ProviderChartData? = null,
    isMagnetEnabled: Boolean = false,
    isLocked: Boolean = false,
    isVisible: Boolean = true,
    selectedCurrency: String = "USD",
    onCurrencyClick: () -> Unit = {},
    isFullscreen: Boolean = false,
    onFullscreenExit: () -> Unit = {},
    scrollToTimestamp: Long? = null,
    onScrollDone: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    showCurrencySelector: Boolean = true,
    showSettingsButton: Boolean = true,
    onDataLoaded: (List<OHLCData>) -> Unit = {},
    selectedTimeZone: String = "UTC",
    onQuoteUpdate: (SymbolQuote) -> Unit = {},
    onAnyQuoteUpdate: (SymbolQuote) -> Unit = {},
    watchlistSymbols: List<String> = emptyList(),
    positions: List<Position> = emptyList(),
    onPositionUpdate: (Position) -> Unit = {},
    onPositionDelete: (String) -> Unit = {},
    onAccountUpdate: (Mt5Service.AccountInfo) -> Unit = {},
    onPositionsUpdate: (List<Position>) -> Unit = {},
    orders: List<Order> = emptyList(),
    onOrdersUpdate: (List<Order>) -> Unit = {},
    onHistoryOrdersUpdate: (List<Order>) -> Unit = {},
    onBalanceHistoryUpdate: (List<BalanceRecord>) -> Unit = {},
    onCalendarUpdate: (EconomicCalendarPayload) -> Unit = {},
    isCalendarVisible: Boolean = false,
    calendarRequestDateIso: String? = null,
    calendarRequestVersion: Int = 0,
    isNewsVisible: Boolean = false,
    onNewsUpdate: (com.trading.app.models.NewsPayload) -> Unit = {},
    onSymbolsUpdate: (List<SymbolInfo>) -> Unit = {},
    onDoubleClick: (Float) -> Unit = {},
    reverseBridge: Mt5ReverseBridge? = null,
    onRsiToggle: (Boolean) -> Unit = {},
    onEma10Toggle: (Boolean) -> Unit = {},
    onEma20Toggle: (Boolean) -> Unit = {},
    onSma1Toggle: (Boolean) -> Unit = {},
    onSma2Toggle: (Boolean) -> Unit = {},
    onVwapToggle: (Boolean) -> Unit = {},
    onBbToggle: (Boolean) -> Unit = {},
    onAtrToggle: (Boolean) -> Unit = {},
    onMacdToggle: (Boolean) -> Unit = {},
    rsiShowLabels: Boolean = true,
    rsiShowLines: Boolean = false,
    ema10ShowLabels: Boolean = true,
    ema10ShowLines: Boolean = false,
    ema20ShowLabels: Boolean = true,
    ema20ShowLines: Boolean = false,
    sma1ShowLabels: Boolean = true,
    sma1ShowLines: Boolean = false,
    sma2ShowLabels: Boolean = true,
    sma2ShowLines: Boolean = false,
    vwapShowLabels: Boolean = true,
    vwapShowLines: Boolean = false,
    bbShowLabels: Boolean = true,
    bbShowLines: Boolean = false,
    atrShowLabels: Boolean = true,
    atrShowLines: Boolean = false,
    macdShowLabels: Boolean = true,
    macdShowLines: Boolean = false,
    volumeShowLabels: Boolean = true,
    volumeShowLines: Boolean = false,
    showPremiumDiscount: Boolean = false,
    onPremiumDiscountToggle: (Boolean) -> Unit = {},
    showFairValueGap: Boolean = false,
    onFairValueGapToggle: (Boolean) -> Unit = {},
    fvgSettings: com.trading.app.indicators.FairValueGapSettings = com.trading.app.indicators.FairValueGapSettings(),
    onFvgSettingsClick: () -> Unit = {},
    showSupplyDemandDaily: Boolean = false,
    onSupplyDemandDailyToggle: (Boolean) -> Unit = {},
    showOteVisibleChart: Boolean = false,
    onOteVisibleChartToggle: (Boolean) -> Unit = {},
    showLiquidityDeltaProfiler: Boolean = false,
    ldpSettings: com.trading.app.indicators.LiquidityDeltaProfilerSettings = com.trading.app.indicators.LiquidityDeltaProfilerSettings(),
    onLdpSettingsClick: () -> Unit = {},
    onLiquidityDeltaProfilerToggle: (Boolean) -> Unit = {},
    showEqhEqlLiquidityZones: Boolean = false,
    eqhEqlSettings: com.trading.app.indicators.EqhEqlLiquidityZonesSettings = com.trading.app.indicators.EqhEqlLiquidityZonesSettings(),
    onEqhEqlSettingsClick: () -> Unit = {},
    onEqhEqlLiquidityZonesToggle: (Boolean) -> Unit = {},
    showPowerHourBreakout: Boolean = false,
    powerHourSettings: com.trading.app.indicators.PowerHourBreakoutSettings = com.trading.app.indicators.PowerHourBreakoutSettings(),
    onPowerHourSettingsClick: () -> Unit = {},
    onPowerHourBreakoutToggle: (Boolean) -> Unit = {},
    showTrendlineBreakouts: Boolean = false,
    trendlineSettings: com.trading.app.indicators.TrendlineBreakoutsSettings = com.trading.app.indicators.TrendlineBreakoutsSettings(),
    onTrendlineSettingsClick: () -> Unit = {},
    onTrendlineBreakoutsToggle: (Boolean) -> Unit = {},
    showTrendlineNavigator: Boolean = false,
    navigatorSettings: com.trading.app.indicators.TrendlineNavigatorSettings = com.trading.app.indicators.TrendlineNavigatorSettings(),
    onNavigatorSettingsClick: () -> Unit = {},
    onTrendlineNavigatorToggle: (Boolean) -> Unit = {},
    showLiquidityPools: Boolean = false,
    liquidityPoolsSettings: com.trading.app.indicators.LiquidityPoolsSettings = com.trading.app.indicators.LiquidityPoolsSettings(),
    onLiquidityPoolsSettingsClick: () -> Unit = {},
    onLiquidityPoolsToggle: (Boolean) -> Unit = {},
    showOrderBlockBreaker: Boolean = false,
    obbSettings: com.trading.app.indicators.OrderBlockBreakerSettings = com.trading.app.indicators.OrderBlockBreakerSettings(),
    onObbSettingsClick: () -> Unit = {},
    onOrderBlockBreakerToggle: (Boolean) -> Unit = {},
    showVolumaticFvg: Boolean = false,
    volumaticFvgSettings: com.trading.app.indicators.VolumaticFvgSettings = com.trading.app.indicators.VolumaticFvgSettings(),
    onVolumaticFvgSettingsClick: () -> Unit = {},
    onVolumaticFvgToggle: (Boolean) -> Unit = {},
    showAutoFib: Boolean = false,
    autoFibEnabled: Boolean = false,
    onAutoFibToggle: (Boolean) -> Unit = {},
    onAutoFibHide: (Boolean) -> Unit = {},
    autoFibSettings: com.trading.app.indicators.AutoFibSettings = com.trading.app.indicators.AutoFibSettings(),
    onAutoFibSettingsChange: (com.trading.app.indicators.AutoFibSettings) -> Unit = {},
    onAutoFibSettingsClick: () -> Unit = {},
    sdVrSettings: com.trading.app.indicators.SupplyDemandVrSettings = com.trading.app.indicators.SupplyDemandVrSettings(),
    onSdVrSettingsClick: () -> Unit = {},
    cfvgSettings: com.trading.app.indicators.ConfluenceFvgSettings = com.trading.app.indicators.ConfluenceFvgSettings(),
    onCfvgSettingsClick: () -> Unit = {},
    showConfluenceFvg: Boolean = false,
    confluenceFvgEnabled: Boolean = false,
    onConfluenceFvgToggle: (Boolean) -> Unit = {},
    onConfluenceFvgHide: (Boolean) -> Unit = {},
    selectedIndicatorId: String? = null,
    onSelectedIndicatorIdChange: (String?) -> Unit = {},
    onIndicatorDataUpdate: (IndicatorData) -> Unit = {}
) {
    var visibleTimeRange by remember { mutableStateOf<TimeRange?>(null) }
    val barSpacingState = remember { mutableStateOf(6f) }
    val context = LocalContext.current
    val networkPrefs = remember { context.getSharedPreferences(NetworkConfig.PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val mt5Host = remember { NetworkConfig.mt5Host(context) }
    val mt5Port = remember { NetworkConfig.mt5Port(context) }

    val providerManagedData = providerChartData != null
    var ohlcData by remember(chartFeedType) { mutableStateOf<List<OHLCData>>(emptyList()) }
    var isLoadingMore by remember(chartFeedType) { mutableStateOf(false) }
    var hasMoreHistory by remember(chartFeedType) { mutableStateOf(true) }
    
    val candlestickData by remember {
        derivedStateOf { ohlcData.map(OHLCData::toCandlestickData) }
    }
    var currentQuoteState by remember(chartFeedType) { mutableStateOf<SymbolQuote?>(null) }
    var mainPriceScaleWidthPx by remember { mutableFloatStateOf(0f) }
    var seriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var chartsViewApi by remember(chartFeedType) { mutableStateOf<ChartsView?>(null) }
    var rsiChartsViewApi by remember(chartFeedType) { mutableStateOf<ChartsView?>(null) }
    var hasFittedInitialHistory by remember(chartFeedType) { mutableStateOf(false) }
    var showMarketStatus by remember { mutableStateOf(false) }
    var showIndicatorsList by remember { mutableStateOf(true) }
    var showIndicatorMoreMenu by remember { mutableStateOf(false) }
    var indicatorMoreMenuTarget by remember { mutableStateOf<String?>(null) }
    // Guards live tick updates while a render pass (history setData, indicator overlay
    // rebuild) is writing into the chart - the Android JS bridge throws asynchronously
    // ("Error: Value is null") if a series update lands mid-rebuild and that exception
    // cannot be caught by runCatching, so we avoid issuing the update at all.
    var chartBusy by remember { mutableStateOf(false) }

    // Indicator series state
    val rsiPaneRefs = rememberRsiPaneRefs()
    var ema10SeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var ema20SeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var sma1SeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var sma2SeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var vwapBandFillSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var vwapBandMaskSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var vwapUpperSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var vwapSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var vwapLowerSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var atrSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var bbBandFillSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var bbBandMaskSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var bbUpperSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var bbMiddleSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var bbLowerSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var macdLineSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var macdSignalSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var macdHistogramSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var volumeSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
    var volumeMaSeriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }

    fun resetChartSeriesHandles() {
        chartsViewApi = null
        seriesApi = null
        // NOTE: rsiPaneRefs is owned by the dedicated RSI ChartsView lifecycle - not reset here
        ema10SeriesApi = null
        ema20SeriesApi = null
        sma1SeriesApi = null
        sma2SeriesApi = null
        vwapBandFillSeriesApi = null
        vwapBandMaskSeriesApi = null
        vwapUpperSeriesApi = null
        vwapSeriesApi = null
        vwapLowerSeriesApi = null
        atrSeriesApi = null
        bbBandFillSeriesApi = null
        bbBandMaskSeriesApi = null
        bbUpperSeriesApi = null
        bbMiddleSeriesApi = null
        bbLowerSeriesApi = null
        macdLineSeriesApi = null
        macdSignalSeriesApi = null
        macdHistogramSeriesApi = null
        volumeSeriesApi = null
        volumeMaSeriesApi = null
    }

    LaunchedEffect(symbol, timeframe, chartFeedType, providerManagedData) {
        hasFittedInitialHistory = false
    }

    // Indicator Price Lines state
    val rsiLineState = remember { mutableStateOf<PriceLine?>(null) }
    val ema10LineState = remember { mutableStateOf<PriceLine?>(null) }
    val ema20LineState = remember { mutableStateOf<PriceLine?>(null) }
    val sma1LineState = remember { mutableStateOf<PriceLine?>(null) }
    val sma2LineState = remember { mutableStateOf<PriceLine?>(null) }
    val vwapLineState = remember { mutableStateOf<PriceLine?>(null) }
    val vwapUpperLineState = remember { mutableStateOf<PriceLine?>(null) }
    val vwapLowerLineState = remember { mutableStateOf<PriceLine?>(null) }
    val bbMiddleLineState = remember { mutableStateOf<PriceLine?>(null) }
    val bbUpperLineState = remember { mutableStateOf<PriceLine?>(null) }
    val bbLowerLineState = remember { mutableStateOf<PriceLine?>(null) }
    val atrLineState = remember { mutableStateOf<PriceLine?>(null) }
    val macdLinePriceLineState = remember { mutableStateOf<PriceLine?>(null) }
    val macdSignalPriceLineState = remember { mutableStateOf<PriceLine?>(null) }
    val volumeLineState = remember { mutableStateOf<PriceLine?>(null) }
    val volumeMaLineState = remember { mutableStateOf<PriceLine?>(null) }

    // Alert lines — persistent so we can remove old ones before re-rendering (prevents stacking)
    val alertPriceLines = remember { mutableStateMapOf<String, PriceLine>() }
    var alertPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }

    // Premium & Discount (BigBeluga) - box edge/equilibrium price lines (boxes are BaselineSeries)
    val pdSrUpperTopState = remember { mutableStateOf<PriceLine?>(null) }
    val pdSrLowerBottomState = remember { mutableStateOf<PriceLine?>(null) }
    val pdEquilibriumState = remember { mutableStateOf<PriceLine?>(null) }
    val pdMacroEquilibriumState = remember { mutableStateOf<PriceLine?>(null) }
    var pdPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    // Premium & Discount boxes rendered as BaselineSeries rectangles (chart-owned)
    val pdBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    // Legacy fallback price lines for low-priced symbols (baseline baseValue is Int-only)
    val pdFallbackLines = remember { mutableStateListOf<PriceLine>() }
    // Signature of last rendered Premium & Discount layout (prevents per-tick box rebuilds)
    var lastPdSig by remember { mutableStateOf<String?>(null) }

    // Fair Value Gap (LuxAlgo) price lines - up to 500 boxes as paired price lines
    val fvgPriceLines = remember { mutableStateListOf<PriceLine>() }
    var fvgPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    // FVG boxes rendered as BaselineSeries rectangles (chart-owned)
    val fvgBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val fvgLineSeries = remember { mutableStateListOf<SeriesApi>() }
    // Signature of last rendered FVG layout (prevents per-tick rebuilds)
    var lastFvgSig by remember { mutableStateOf<String?>(null) }
    var fvgDashboardData by remember { mutableStateOf<com.trading.app.indicators.FairValueGapData?>(null) }
    val fvgPrevInside = remember { mutableStateMapOf<Long, Boolean>() }
    val fvgLastAlertMs = remember { mutableStateMapOf<Long, Long>() }

    // Supply and Demand Daily (LuxAlgo) price lines
    val sdSupplyTopState = remember { mutableStateOf<PriceLine?>(null) }
    val sdSupplyBottomState = remember { mutableStateOf<PriceLine?>(null) }
    val sdSupplyAvgState = remember { mutableStateOf<PriceLine?>(null) }
    val sdSupplyWavgState = remember { mutableStateOf<PriceLine?>(null) }
    val sdDemandTopState = remember { mutableStateOf<PriceLine?>(null) }
    val sdDemandBottomState = remember { mutableStateOf<PriceLine?>(null) }
    val sdDemandAvgState = remember { mutableStateOf<PriceLine?>(null) }
    val sdDemandWavgState = remember { mutableStateOf<PriceLine?>(null) }
    var sdPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    // SD zones rendered as BaselineSeries boxes + avg/wavg LineSeries (chart-owned)
    val sdBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val sdLineSeries = remember { mutableStateListOf<SeriesApi>() }
    var lastSdSig by remember { mutableStateOf<String?>(null) }

    // Supply & Demand Visible Range - dynamic recomputation on pan/zoom
    val sdVrVisibleTick = remember { mutableIntStateOf(0) }
    val sdVrGen = remember { mutableIntStateOf(0) }
    var sdVrZones by remember { mutableStateOf<com.trading.app.indicators.SdVrResult?>(null) }
    val sdVrPrevInside = remember { mutableStateMapOf("supply" to false, "demand" to false) }
    val sdVrLastAlertMs = remember { mutableStateMapOf("supply" to 0L, "demand" to 0L) }

    // Confluence FVG zone-entry alarms
    var cfvgZones by remember { mutableStateOf<List<com.trading.app.indicators.ConfluenceFvgZone>>(emptyList()) }
    val cfvgPrevInside = remember { mutableStateMapOf<Long, Boolean>() }
    val cfvgLastAlertMs = remember { mutableStateMapOf<Long, Long>() }

    // OTE visible chart (twingall) - fib levels + box + extensions
    val otePriceLines = remember { mutableStateListOf<PriceLine>() }
    var otePriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    // OTE box + fib lines rendered as chart-owned series (Pine-exact look)
    val oteBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val oteLineSeries = remember { mutableStateListOf<SeriesApi>() }
    var lastOteSig by remember { mutableStateOf<String?>(null) }

    // Liquidity Delta Profiler (LuxAlgo) - BSL/SSL pivot zones + delta quadrants
    val ldpBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val ldpPriceLines = remember { mutableStateListOf<PriceLine>() }
    var ldpPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastLdpSig by remember { mutableStateOf<String?>(null) }
    // Async series creations still in flight (onSeriesCreated not yet fired)
    var ldpPendingSeries by remember { mutableIntStateOf(0) }

    // EQH/EQL Liquidity Zones (LuxAlgo) - equal highs/lows boxes + cluster labels
    val eqhBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val eqhPriceLines = remember { mutableStateListOf<PriceLine>() }
    var eqhPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastEqhSig by remember { mutableStateOf<String?>(null) }
    // Async series creations still in flight (onSeriesCreated not yet fired)
    var eqhPendingSeries by remember { mutableIntStateOf(0) }

    // Power Hour Breakout (LuxAlgo) - NY session boxes + level/ext lines + fibos + breakout markers
    val phBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val phPriceLines = remember { mutableStateListOf<PriceLine>() }
    var phPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastPhSig by remember { mutableStateOf<String?>(null) }
    // Async series creations still in flight (onSeriesCreated not yet fired)
    var phPendingSeries by remember { mutableIntStateOf(0) }

    // Trendline Breakouts With Targets (ChartPrime) - pivot trendlines + bands + signals + targets
    val tbtBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val tbtPriceLines = remember { mutableStateListOf<PriceLine>() }
    var tbtPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastTbtSig by remember { mutableStateOf<String?>(null) }
    // Async series creations still in flight (onSeriesCreated not yet fired)
    var tbtPendingSeries by remember { mutableIntStateOf(0) }

    // Trendline Breakout Navigator (LuxAlgo) - swing trendlines + wick dots + HH/LL tags
    val tnavBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val tnavPriceLines = remember { mutableStateListOf<PriceLine>() }
    var tnavPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastTnavSig by remember { mutableStateOf<String?>(null) }
    // Async series creations still in flight (onSeriesCreated not yet fired)
    var tnavPendingSeries by remember { mutableIntStateOf(0) }

    // Liquidity Pools (LuxAlgo) - running-extreme zone boxes + volume labels
    val lpBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val lpPriceLines = remember { mutableStateListOf<PriceLine>() }
    var lpPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastLpSig by remember { mutableStateOf<String?>(null) }
    // Async series creations still in flight (onSeriesCreated not yet fired)
    var lpPendingSeries by remember { mutableIntStateOf(0) }
    val obbSeries = remember { mutableStateListOf<SeriesApi>() }
    val obbPriceLines = remember { mutableStateListOf<PriceLine>() }
    var obbPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastObbSig by remember { mutableStateOf<String?>(null) }
    var obbPendingSeries by remember { mutableIntStateOf(0) }
    // Volumatic Fair Value Gaps (BigBeluga) - volume-split FVG zones
    val vfvgSeries = remember { mutableStateListOf<SeriesApi>() }
    val vfvgPriceLines = remember { mutableStateListOf<PriceLine>() }
    var vfvgPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastVfvgSig by remember { mutableStateOf<String?>(null) }
    var vfvgPendingSeries by remember { mutableIntStateOf(0) }
    // Volumatic FVG dashboard counts (Pine dash table: bullish/bearish on-chart counts)
    var vfvgBullCount by remember { mutableIntStateOf(0) }
    var vfvgBearCount by remember { mutableIntStateOf(0) }
    var lastOverlayMarkersSig by remember { mutableStateOf<String?>(null) }

    // Auto Fib Retracement - fib levels rendered as chart-owned LineSeries + baseline fills
    val autoFibSeries = remember { mutableStateListOf<SeriesApi>() }
    val autoFibPriceLines = remember { mutableStateListOf<PriceLine>() }
    var autoFibPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastAutoFibSig by remember { mutableStateOf<String?>(null) }
    // ratio -> last side of price vs level (+1 above / -1 below) for crossing alerts
    val autoFibPrevSideState = remember { mutableMapOf<String, Int>() }

    // Confluence FVG Finder - merged multi-TF zones + info label lines
    val cfBoxSeries = remember { mutableStateListOf<SeriesApi>() }
    val cfPriceLines = remember { mutableStateListOf<PriceLine>() }
    var cfPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    var lastCfSig by remember { mutableStateOf<String?>(null) }

    // High/Low lines state (Line and Label separate for color independence)
    val highLineState = remember { mutableStateOf<PriceLine?>(null) }
    val highLabelState = remember { mutableStateOf<PriceLine?>(null) }
    val lowLineState = remember { mutableStateOf<PriceLine?>(null) }
    val lowLabelState = remember { mutableStateOf<PriceLine?>(null) }
    var highLowPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    
    val bidPriceLineState = remember { mutableStateOf<PriceLine?>(null) }
    val askPriceLineState = remember { mutableStateOf<PriceLine?>(null) }
    var bidAskPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }

    val updatedOnQuoteUpdate = rememberUpdatedState(onQuoteUpdate)
    val updatedOnAnyQuoteUpdate = rememberUpdatedState(onAnyQuoteUpdate)
    val updatedOnDataLoaded = rememberUpdatedState(onDataLoaded)
    val updatedOnAccountUpdate = rememberUpdatedState(onAccountUpdate)
    val updatedOnPositionsUpdate = rememberUpdatedState(onPositionsUpdate)
    val updatedOnOrdersUpdate = rememberUpdatedState(onOrdersUpdate)
    val updatedOnHistoryOrdersUpdate = rememberUpdatedState(onHistoryOrdersUpdate)
    val updatedOnBalanceHistoryUpdate = rememberUpdatedState(onBalanceHistoryUpdate)
    val updatedOnCalendarUpdate = rememberUpdatedState(onCalendarUpdate)
    val updatedOnNewsUpdate = rememberUpdatedState(onNewsUpdate)
    val updatedOnSymbolsUpdate = rememberUpdatedState(onSymbolsUpdate)

    val currentSymbol = rememberUpdatedState(symbol)
    val currentTimeframe = rememberUpdatedState(timeframe)
    var pendingChartQuote by remember { mutableStateOf<SymbolQuote?>(null) }
    var lastChartQuoteAppliedAt by remember { mutableLongStateOf(0L) }
    val showInlineRsiPane = showRsi && "RSI" !in hiddenIndicators
    val mainSeriesKind = resolveMainSeriesKind(style)

    fun applyQuoteToChart(quote: SymbolQuote) {
        currentQuoteState = quote
        if (providerManagedData && ohlcData.isEmpty()) {
            updatedOnQuoteUpdate.value(quote)
            updatedOnAnyQuoteUpdate.value(quote)
            return
        }
        ohlcData = applyTickToCandles(
            candles = ohlcData,
            timeframe = currentTimeframe.value,
            lastPrice = quote.lastPrice,
            tickTimestampSeconds = normalizeEpochSeconds(quote.time),
            tickVolume = quote.volume
        )
        updatedOnQuoteUpdate.value(quote)
        updatedOnAnyQuoteUpdate.value(quote)
    }

    fun scheduleChartQuote(quote: SymbolQuote) {
        val now = System.currentTimeMillis()
        val throttleMs = CHART_TICK_THROTTLE_MS
        if (now - lastChartQuoteAppliedAt < throttleMs) {
            pendingChartQuote = quote
            return
        }
        lastChartQuoteAppliedAt = now
        pendingChartQuote = null
        applyQuoteToChart(quote)
    }

    LaunchedEffect(pendingChartQuote) {
        val quote = pendingChartQuote ?: return@LaunchedEffect
        val throttleMs = CHART_TICK_THROTTLE_MS
        val waitMs = (throttleMs - (System.currentTimeMillis() - lastChartQuoteAppliedAt)).coerceAtLeast(0L)
        kotlinx.coroutines.delay(waitMs)
        if (pendingChartQuote == quote) {
            lastChartQuoteAppliedAt = System.currentTimeMillis()
            pendingChartQuote = null
            applyQuoteToChart(quote)
        }
    }

    LaunchedEffect(providerManagedData, providerChartData?.candles) {
        if (providerManagedData) {
            val incomingCandles = providerChartData?.candles.orEmpty()
            Log.d(LOG_TAG, "provider candles -> ${incomingCandles.size} bars")
            if (incomingCandles.isEmpty()) hasFittedInitialHistory = false
            ohlcData = incomingCandles
            isLoadingMore = providerChartData?.isLoadingMore ?: false
            hasMoreHistory = providerChartData?.hasMoreHistory ?: true
            updatedOnDataLoaded.value(incomingCandles)
        }
    }

    LaunchedEffect(providerManagedData, providerChartData?.quote) {
        if (providerManagedData) {
            providerChartData?.quote?.let(::applyQuoteToChart)
        }
    }

    // Range-based H/L state
    var visibleRangeHighLow by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    val chartBgColor = getFullChartColor(chartSettings.canvas.fullChartColor, chartSettings.canvas.background)
    val rsiDataState = remember(ohlcData, rsiPeriod) {
        calculateRsiChartData(
            candles = ohlcData,
            enabled = true,
            period = rsiPeriod
        )
    }
    val currentRsiDataState by rememberUpdatedState(rsiDataState)
    val bbDataState = remember(ohlcData, showBb, bbPeriod, bbStdDev) {
        if (showBb) {
            com.trading.app.indicators.BbandsIndicator(bbPeriod, bbStdDev).calculateBands(ohlcData)
        } else {
            BbandsData(
                upperBand = emptyList(),
                middleBand = emptyList(),
                lowerBand = emptyList()
            )
        }
    }
    val vwapDataState = remember(ohlcData, showVwap) {
        if (showVwap) {
            com.trading.app.indicators.VwapIndicator().calculateBands(ohlcData)
        } else {
            VwapData(
                vwap = emptyList(),
                upperBand = emptyList(),
                lowerBand = emptyList()
            )
        }
    }
    val atrDataState = remember(ohlcData, atrPeriod) {
        com.trading.app.indicators.AtrIndicator(atrPeriod).calculate(ohlcData)
    }
    val macdDataState = remember(ohlcData, macdFast, macdSlow, macdSignal) {
        val indicator = com.trading.app.indicators.MacdIndicator(macdFast, macdSlow, macdSignal)
        val macdLine = indicator.calculateMacdLine(ohlcData)
        val signalLine = indicator.calculateSignalLine(macdLine)
        val histogram = indicator.calculateHistogram(macdLine, signalLine)
        Triple(macdLine, signalLine, histogram)
    }

    // Update parent with indicator data for AI
    LaunchedEffect(rsiDataState, macdDataState, atrDataState) {
        onIndicatorDataUpdate(
            IndicatorData(
                rsi = rsiDataState.latestValue,
                macd = macdDataState.first.lastOrNull(),
                macdSignal = macdDataState.second.lastOrNull(),
                macdHistogram = macdDataState.third.lastOrNull(),
                atr = atrDataState.lastOrNull()
            )
        )
    }
    val volumeMaDataState = remember(ohlcData, showVolumeMa, volumeMaLength) {
        if (showVolumeMa && ohlcData.isNotEmpty()) {
            val hasRealVolume = ohlcData.any { it.volume > 0f }
            val volumeValues = if (hasRealVolume) {
                ohlcData.map { it.volume.coerceAtLeast(0f) }
            } else {
                ohlcData.map {
                    val body = kotlin.math.abs(it.close - it.open)
                    val range = (it.high - it.low).coerceAtLeast(0f)
                    maxOf(range, body, 0.0001f)
                }
            }
            Indicators.calculateSma(volumeValues, volumeMaLength)
        } else {
            emptyList()
        }
    }
    val paneMargins = remember(showVolume, showInlineRsiPane, showMacd, showAtr) {
        resolvePaneMargins(showVolume, showInlineRsiPane, showMacd, showAtr, hiddenIndicators)
    }

    fun String.toIntColor(): IntColor = try {
        IntColor(AndroidColor.parseColor(this))
    } catch (e: Exception) {
        IntColor(AndroidColor.GRAY)
    }
    
    fun Int.toLineWidth(): LineWidth = when (this) {
        1 -> LineWidth.ONE
        2 -> LineWidth.TWO
        3 -> LineWidth.THREE
        4 -> LineWidth.FOUR
        else -> LineWidth.ONE
    }
    
    fun String.toLineStyle(): LineStyle = when (this) {
        "Solid" -> LineStyle.SOLID
        "Dashed" -> LineStyle.DASHED
        "Dotted" -> LineStyle.DOTTED
        else -> LineStyle.SOLID
    }

    fun applyHistoryUpdate(source: String, receivedSymbol: String, history: List<OHLCData>) {
        if (receivedSymbol.isNotEmpty() && !chartSymbolsMatch(receivedSymbol, currentSymbol.value)) return
        val processedHistory = history
            .asSequence()
            .mapNotNull { candle ->
                if (candle.time <= 0L) return@mapNotNull null
                if (!candle.open.isFinite() || !candle.high.isFinite() || !candle.low.isFinite() || !candle.close.isFinite()) {
                    return@mapNotNull null
                }
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

        if (isLoadingMore) {
            val combined = (processedHistory + ohlcData)
                .distinctBy { it.time }
                .sortedBy { it.time }
                .takeLast(10000)
            ohlcData = combined
            isLoadingMore = false
            if (processedHistory.size < MT5_HISTORY_PAGE_SIZE) hasMoreHistory = false
        } else {
            ohlcData = processedHistory
            if (processedHistory.size < MT5_HISTORY_PAGE_SIZE) hasMoreHistory = false
            updatedOnDataLoaded.value(processedHistory)
        }
        Log.d(LOG_TAG, "onHistoryUpdate ($source): received ${processedHistory.size} candles for $receivedSymbol")
    }

    // PEPPERSTONE CHART SERVICE REMOVED - EA ONLY

    val mt5Service = remember {
        Mt5Service(
            pcIpAddress = mt5Host,
            port = mt5Port,
            onHistoryUpdate = { receivedSymbol: String, history: List<OHLCData> ->
                if (receivedSymbol.isEmpty() || chartSymbolsMatch(receivedSymbol, currentSymbol.value)) {
                    applyHistoryUpdate("MT5", receivedSymbol, history)
                }
            },
            onQuoteUpdate = { quote: SymbolQuote ->
                if (chartSymbolsMatch(quote.name, currentSymbol.value)) {
                    val prevClose = ohlcData.getOrNull(ohlcData.size - 2)?.close ?: quote.lastPrice
                    val change = quote.lastPrice - prevClose
                    val changePercent = if (prevClose != 0f) (change / prevClose) * 100f else 0f
                    
                    val updatedQuote = quote.copy(
                        change = change,
                        changePercent = changePercent
                    )
                    scheduleChartQuote(updatedQuote)
                    Log.d(LOG_TAG, "Applied MT5 tick for ${quote.name} price=${quote.lastPrice}")
                }
            },
            onAccountUpdate = { accountInfo: Mt5Service.AccountInfo ->
                updatedOnAccountUpdate.value(accountInfo)
            },
            onPositionsUpdate = { positions: List<Position> -> updatedOnPositionsUpdate.value(positions) },
            onOrdersUpdate = { orders: List<Order> -> updatedOnOrdersUpdate.value(orders) },
            onHistoryOrdersUpdate = { history: List<Order> -> updatedOnHistoryOrdersUpdate.value(history) },
            onBalanceHistoryUpdate = { balanceRecords: List<BalanceRecord> -> updatedOnBalanceHistoryUpdate.value(balanceRecords) },
            onCalendarUpdate = { calendar: EconomicCalendarPayload -> updatedOnCalendarUpdate.value(calendar) },
            onNewsUpdate = { news: com.trading.app.models.NewsPayload -> updatedOnNewsUpdate.value(news) },
            onSymbolsUpdate = { symbols: List<SymbolInfo> -> updatedOnSymbolsUpdate.value(symbols) }
        )
    }

    LaunchedEffect(chartFeedType, providerManagedData) {
        if (providerManagedData) return@LaunchedEffect
        if (chartFeedType == null || chartFeedType == ChartFeedType.EXNESS) {
            mt5Service.connect()
            mt5Service.requestSymbols()
            reverseBridge?.connect()
        } else {
            reverseBridge?.disconnect()
            mt5Service.disconnect()
        }
    }

    LaunchedEffect(symbol, timeframe, chartFeedType, providerManagedData) {
        if (providerManagedData) return@LaunchedEffect
        mt5Service.stopActiveStream()
        ohlcData = emptyList()
        currentQuoteState = null
        pendingChartQuote = null
        lastChartQuoteAppliedAt = 0L
        isLoadingMore = false
        hasMoreHistory = true
        hasFittedInitialHistory = false
        when (chartFeedType) {
            ChartFeedType.EXNESS -> {
                // MT5 bridge expects broker symbol with 'm' suffix (e.g. BTCUSDm) — chartFeedSymbolFor returns ticker, so resolve brokerSymbol
                val catalog = chartFeedQuotes(ChartFeedType.EXNESS)
                val broker = catalog.firstOrNull { it.ticker.equals(symbol, ignoreCase = true) || it.brokerSymbol.equals(symbol, ignoreCase = true) }?.brokerSymbol
                    ?: if (symbol.endsWith("m", ignoreCase = true) || symbol.endsWith("m")) symbol else symbol + "m"
                Log.d(LOG_TAG, "Subscribing Exness MT5 chart route for $broker (ticker=$symbol) timeframe=$timeframe mt5Host=$mt5Host:$mt5Port")
                mt5Service.streamActiveSymbol(broker, timeframe, 500)
                return@LaunchedEffect
            }
            null -> Unit
        }
        val streamSymbol = normalizeChartSymbol(symbol).let { if (it.endsWith("m", ignoreCase = true)) it else it + "m" }
        Log.d(LOG_TAG, "Subscribing MT5 chart route for $streamSymbol timeframe=$timeframe mt5Host=$mt5Host:$mt5Port")
        mt5Service.streamActiveSymbol(streamSymbol, timeframe, 500)
    }

    LaunchedEffect(isCalendarVisible, calendarRequestDateIso, calendarRequestVersion) {
        if (providerManagedData) return@LaunchedEffect
        if (isCalendarVisible) {
            mt5Service.requestCalendar(calendarRequestDateIso)
        }
    }

    LaunchedEffect(isNewsVisible) {
        if (providerManagedData) return@LaunchedEffect
        if (isNewsVisible) {
            mt5Service.requestNews()
        }
    }

    // Keep the dedicated RSI pane's time scale in sync with the main chart (both directions),
    // so zoom/scroll on either pane moves both - like TradingView panes.
    LaunchedEffect(chartsViewApi, rsiChartsViewApi, showInlineRsiPane) {
        val main = chartsViewApi ?: return@LaunchedEffect
        val rsi = rsiChartsViewApi ?: return@LaunchedEffect
        if (!showInlineRsiPane) return@LaunchedEffect

        var syncing = false
        val forward: (TimeRange?) -> Unit = { range ->
            if (!syncing && range != null) {
                syncing = true
                try { rsi.api.timeScale.setVisibleRange(range) } finally { syncing = false }
            }
        }
        val backward: (TimeRange?) -> Unit = { range ->
            if (!syncing && range != null) {
                syncing = true
                try { main.api.timeScale.setVisibleRange(range) } finally { syncing = false }
            }
        }
        main.api.timeScale.subscribeVisibleTimeRangeChange(forward)
        rsi.api.timeScale.subscribeVisibleTimeRangeChange(backward)
        visibleTimeRange?.let { initial ->
            runCatching { rsi.api.timeScale.setVisibleRange(initial) }
        }
        try {
            awaitCancellation()
        } finally {
            runCatching { main.api.timeScale.unsubscribeVisibleTimeRangeChange(forward) }
            runCatching { rsi.api.timeScale.unsubscribeVisibleTimeRangeChange(backward) }
        }
    }

    LaunchedEffect(ohlcData, seriesApi, style, chartBgColor, rsiChartsViewApi,
        showRsi, rsiPeriod, rsiShowLabels, rsiShowLines,
        showEma10, ema10Period, ema10ShowLabels, ema10ShowLines,
        showEma20, ema20Period, ema20ShowLabels, ema20ShowLines,
        showSma1, sma1Period, sma1ShowLabels, sma1ShowLines,
        showSma2, sma2Period, sma2ShowLabels, sma2ShowLines,
        showVwap, vwapShowLabels, vwapShowLines,
        showBb, bbPeriod, bbStdDev, bbShowLabels, bbShowLines,
        showAtr, atrPeriod, atrShowLabels, atrShowLines,
        showMacd, macdFast, macdSlow, macdSignal, macdShowLabels, macdShowLines,
        showVolume, volumeColorBasedOnPreviousClose, volumeShowLabels, volumeShowLines,
        showPremiumDiscount, showFairValueGap, showSupplyDemandDaily, showOteVisibleChart,
        showAutoFib, showConfluenceFvg, autoFibSettings,
        volumeMaSeriesApi, userAlerts) {
        val mainSeriesApi = seriesApi
        val ohlcList = ohlcData
        Log.d(LOG_TAG, "renderEffect bars=${ohlcList.size} seriesNull=${mainSeriesApi == null}")

        if (ohlcList.isEmpty()) {
            // Asset switched (or history not loaded yet) - wipe whatever the previous
            // asset rendered so the switch is immediate instead of lingering.
            chartBusy = false
            runCatching {
                when (mainSeriesKind) {
                    MainSeriesKind.BAR -> mainSeriesApi?.setData(emptyList<BarData>())
                    MainSeriesKind.LINE -> mainSeriesApi?.setData(emptyList<LineData>())
                    MainSeriesKind.AREA -> mainSeriesApi?.setData(emptyList<AreaData>())
                    MainSeriesKind.BASELINE -> mainSeriesApi?.setData(emptyList<BaselineData>())
                    MainSeriesKind.CANDLESTICK -> mainSeriesApi?.setData(emptyList<CandlestickData>())
                }
            }
            updateInlineRsiPaneData(
                refs = rsiPaneRefs,
                candles = emptyList(),
                data = currentRsiDataState,
                enabled = showInlineRsiPane,
                showLabels = rsiShowLabels,
                showLines = rsiShowLines
            )
            return@LaunchedEffect
        }

        chartBusy = true
        if (ohlcData.isNotEmpty()) {
            when (mainSeriesKind) {
                MainSeriesKind.BAR -> mainSeriesApi?.setData(ohlcData.map(OHLCData::toBarSeriesData))
                MainSeriesKind.LINE -> mainSeriesApi?.setData(ohlcData.map { it.toLineSeriesData() })
                MainSeriesKind.AREA -> mainSeriesApi?.setData(
                    ohlcData.map { candle ->
                        candle.toAreaSeriesData(areaValueForStyle(style, candle))
                    }
                )
                MainSeriesKind.BASELINE -> mainSeriesApi?.setData(ohlcData.map { it.toBaselineSeriesData() })
                MainSeriesKind.CANDLESTICK -> {
                    if (style == "heikin_ashi") {
                        mainSeriesApi?.setData(calculateHeikinAshi(ohlcData))
                    } else {
                        mainSeriesApi?.setData(candlestickData)
                    }
                }
            }
            if (!hasFittedInitialHistory && ohlcData.size > 1) {
                chartsViewApi?.api?.timeScale?.let { timeScale ->
                    timeScale.fitContent()
                    timeScale.applyOptions(
                        TimeScaleOptions(
                            barSpacing = 6f
                        )
                    )
                }
                hasFittedInitialHistory = true
            }

updateInlineRsiPaneData(
                refs = rsiPaneRefs,
                candles = ohlcData,
                data = rsiDataState,
                enabled = showInlineRsiPane,
                showLabels = rsiShowLabels,
                showLines = rsiShowLines
            )

            // ── Alert Lines: render horizontal lines for active user alerts (persistent, no stacking) ──
            if (mainSeriesApi != null) {
                // Remove lines for alerts that no longer exist
                val activeIds = userAlerts.filter { it.isActive && it.symbol == symbol }.map { it.id }.toSet()
                val staleIds = alertPriceLines.keys.filter { it !in activeIds }
                staleIds.forEach { id ->
                    safelyRemovePriceLine(alertPriceLineOwner ?: mainSeriesApi, alertPriceLines[id])
                    alertPriceLines.remove(id)
                }
                alertPriceLineOwner = mainSeriesApi
                val settings = chartSettings.alerts
                if (settings.alertLines && userAlerts.any { it.isActive && it.symbol == symbol }) {
                    val alertColor = parseAlertColor(settings.alertLinesColor)
                    runCatching {
                        userAlerts.filter { it.isActive && it.symbol == symbol && it.condition != "SMC" }.forEach { alert ->
                            val existing = alertPriceLines[alert.id]
                            if (existing != null) {
                                // Re-create to update price/title (lightweight-charts PriceLine price is immutable after creation)
                                safelyRemovePriceLine(mainSeriesApi, existing)
                            }
                            val line = mainSeriesApi.createPriceLine(
                                PriceLineOptions(
                                    price = alert.price,
                                    color = IntColor(alertColor.toArgb()),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = LineStyle.DASHED,
                                    lineVisible = true,
                                    axisLabelVisible = true,
                                    title = "🔔 ${alert.condition} ${formatPrice(alert.price, symbol)}"
                                )
                            )
                            if (line != null) alertPriceLines[alert.id] = line
                        }
                    }
                }
            }

        } else {
            // clear Premium & Discount + FVG + Supply/Demand + OTE lines when no data
            pdPriceLineOwner?.let { api ->
                safelyRemovePriceLine(api, pdSrUpperTopState.value); pdSrUpperTopState.value = null
                safelyRemovePriceLine(api, pdSrLowerBottomState.value); pdSrLowerBottomState.value = null
                safelyRemovePriceLine(api, pdEquilibriumState.value); pdEquilibriumState.value = null
                safelyRemovePriceLine(api, pdMacroEquilibriumState.value); pdMacroEquilibriumState.value = null
            }
            chartsViewApi?.api?.let { chartApi ->
                pdBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                pdBoxSeries.clear()
            }
            fvgPriceLineOwner?.let { api ->
                fvgPriceLines.forEach { safelyRemovePriceLine(api, it) }
                fvgPriceLines.clear()
            }
            chartsViewApi?.api?.let { chartApi ->
                fvgBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                fvgBoxSeries.clear()
            }
            lastFvgSig = null
            sdPriceLineOwner?.let { api ->
                safelyRemovePriceLine(api, sdSupplyTopState.value); sdSupplyTopState.value = null
                safelyRemovePriceLine(api, sdSupplyBottomState.value); sdSupplyBottomState.value = null
                safelyRemovePriceLine(api, sdSupplyAvgState.value); sdSupplyAvgState.value = null
                safelyRemovePriceLine(api, sdSupplyWavgState.value); sdSupplyWavgState.value = null
                safelyRemovePriceLine(api, sdDemandTopState.value); sdDemandTopState.value = null
                safelyRemovePriceLine(api, sdDemandBottomState.value); sdDemandBottomState.value = null
                safelyRemovePriceLine(api, sdDemandAvgState.value); sdDemandAvgState.value = null
                safelyRemovePriceLine(api, sdDemandWavgState.value); sdDemandWavgState.value = null
            }
            chartsViewApi?.api?.let { chartApi ->
                sdBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                sdLineSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            sdBoxSeries.clear()
            sdLineSeries.clear()
            lastSdSig = null
            otePriceLineOwner?.let { api ->
                otePriceLines.forEach { safelyRemovePriceLine(api, it) }
                otePriceLines.clear()
            }
            chartsViewApi?.api?.let { chartApi ->
                oteBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                oteLineSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            oteBoxSeries.clear()
            oteLineSeries.clear()
            lastOteSig = null
            ldpPriceLineOwner?.let { api ->
                ldpPriceLines.forEach { safelyRemovePriceLine(api, it) }
                ldpPriceLines.clear()
            }
            chartsViewApi?.api?.let { chartApi ->
                ldpBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            ldpBoxSeries.clear()
            lastLdpSig = null
            eqhPriceLineOwner?.let { api ->
                eqhPriceLines.forEach { safelyRemovePriceLine(api, it) }
                eqhPriceLines.clear()
            }
            chartsViewApi?.api?.let { chartApi ->
                eqhBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            eqhBoxSeries.clear()
            lastEqhSig = null
            mainSeriesApi?.setData(emptyList())
            seriesApi?.priceScale()?.applyOptions(PriceScaleOptions(autoScale = true))
            rsiPaneRefs.clearData()
            ema10SeriesApi?.setData(emptyList())
            ema20SeriesApi?.setData(emptyList())
            sma1SeriesApi?.setData(emptyList())
            sma2SeriesApi?.setData(emptyList())
            vwapBandFillSeriesApi?.setData(emptyList())
            vwapBandMaskSeriesApi?.setData(emptyList())
            vwapUpperSeriesApi?.setData(emptyList())
            vwapSeriesApi?.setData(emptyList())
            vwapLowerSeriesApi?.setData(emptyList())
            atrSeriesApi?.setData(emptyList())
            bbBandFillSeriesApi?.setData(emptyList())
            bbBandMaskSeriesApi?.setData(emptyList())
            bbUpperSeriesApi?.setData(emptyList())
            bbMiddleSeriesApi?.setData(emptyList())
            bbLowerSeriesApi?.setData(emptyList())
            macdLineSeriesApi?.setData(emptyList())
            macdSignalSeriesApi?.setData(emptyList())
            macdHistogramSeriesApi?.setData(emptyList())
            volumeSeriesApi?.setData(emptyList())
            volumeMaSeriesApi?.setData(emptyList())
        }
        chartBusy = false
    }


    // Auto Fib Retracement - standalone render pass (extracted to keep the main effect under the JVM method limit)
    LaunchedEffect(showAutoFib, ohlcData, seriesApi, chartsViewApi, autoFibSettings, timeframe, currentQuoteState) {
        runCatching {
        val mainSeriesApi = seriesApi
        val ohlcList = ohlcData
        // Auto Fib Retracement - deviation+depth zigzag anchored on the last two pivots
        fun clearAutoFibRender(targetApi: SeriesApi?) {
            val lineOwner = autoFibPriceLineOwner ?: targetApi
            autoFibPriceLines.forEach { safelyRemovePriceLine(lineOwner, it) }
            autoFibPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                autoFibSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            autoFibSeries.clear()
            autoFibPriceLineOwner = targetApi ?: autoFibPriceLineOwner
        }
        if (showAutoFib && mainSeriesApi != null && ohlcData.size >= 30 && chartsViewApi != null) {
            val ownerChanged = autoFibPriceLineOwner != seriesApi
            val af = com.trading.app.indicators.AutoFibRetracementIndicator()
                .calculateAutoFib(ohlcData, autoFibSettings)
            val sig = if (af == null) "null" else listOf(
                af.leftTime, af.rightTime,
                String.format(java.util.Locale.US, "%.6f", af.leftPrice),
                String.format(java.util.Locale.US, "%.6f", af.rightPrice),
                autoFibSettings.deviation, autoFibSettings.depth,
                autoFibSettings.reverse, autoFibSettings.extendLeft, autoFibSettings.extendRight,
                autoFibSettings.showPrices, autoFibSettings.showLevels,
                autoFibSettings.levelsFormatValues, autoFibSettings.backgroundTransparency
            ).joinToString(",") + "|" + af.levels.joinToString(";") { "${it.ratio}:${String.format(java.util.Locale.US, "%.6f", it.price)}:${it.colorInt}" } + "|${ohlcData.size}|$timeframe"
            if (ownerChanged || lastAutoFibSig != sig || autoFibSeries.isEmpty()) {
                clearAutoFibRender(seriesApi)
                autoFibPriceLineOwner = seriesApi
                if (af != null) {
                    val tfSec = timeframeToSeconds(timeframe).coerceAtLeast(60L)
                    // Pine extending: left/right/both around the pivot leg
                    val legLeftT = if (autoFibSettings.extendLeft) af.leftTime - 200L * tfSec else af.leftTime
                    val legRightT = if (autoFibSettings.extendRight) ohlcData.last().time + 40L * tfSec else af.rightTime
                    // Dashed gray zigzag connector between the two pivots (Pine lineLast)
                    chartsViewApi?.api?.addLineSeries(
                        options = LineSeriesOptions(
                            color = IntColor(AndroidColor.parseColor("#787b86")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            priceLineVisible = false,
                            lastValueVisible = false,
                            crosshairMarkerVisible = false
                        ),
                        onSeriesCreated = { connector ->
                            connector.setData(listOf(
                                LineData(time = Time.Utc(af.leftTime), value = af.leftPrice),
                                LineData(time = Time.Utc(af.rightTime), value = af.rightPrice)
                            ))
                            autoFibSeries.add(connector)
                        }
                    )
                    fun afLabelTitle(ratio: Float, price: Float): String {
                        val levelTxt = if (!autoFibSettings.showLevels) "" else if (autoFibSettings.levelsFormatValues) "${trimRatio(ratio)}" else "${trimRatio(ratio * 100f)}%"
                        val priceTxt = if (!autoFibSettings.showPrices) "" else " (${formatPrice(price, symbol)})"
                        return levelTxt + priceTxt
                    }
                    fun afAddLevel(price: Float, ratio: Float, colorInt: Int) {
                        chartsViewApi?.api?.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(colorInt or 0xFF000000.toInt()),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { line ->
                                line.setData(listOf(LineData(time = Time.Utc(legLeftT), value = price), LineData(time = Time.Utc(legRightT), value = price)))
                                autoFibSeries.add(line)
                            }
                        )
                        // Writeup label pinned to the level (axis side per labelsPosition setting)
                        autoFibPriceLines.add(
                            mainSeriesApi.createPriceLine(
                                PriceLineOptions(
                                    price = price,
                                    color = IntColor(colorInt),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = LineStyle.SOLID,
                                    lineVisible = false,
                                    axisLabelVisible = true,
                                    title = afLabelTitle(ratio, price)
                                )
                            )
                        )
                    }
                    // Fills between consecutive shown levels (Pine linefill, bg transparency setting)
                    val fillOpacityPct = (100 - autoFibSettings.backgroundTransparency).coerceIn(0, 100)
                    for (i in 0 until af.levels.size - 1) {
                        val upperLvl = af.levels[i]
                        val lowerLvl = af.levels[i + 1]
                        if (upperLvl.price <= lowerLvl.price) continue
                        chartsViewApi?.api?.addBaselineSeries(
                            options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(lowerLvl.price.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = IntColor(applyOpacity(upperLvl.colorInt, fillOpacityPct)),
                                topFillColor2 = IntColor(applyOpacity(upperLvl.colorInt, fillOpacityPct)),
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { fill ->
                                fill.setData(listOf(BaselineData(time = Time.Utc(legLeftT), value = upperLvl.price), BaselineData(time = Time.Utc(legRightT), value = upperLvl.price)))
                                autoFibSeries.add(fill)
                            }
                        )
                    }
                    for (lvl in af.levels) afAddLevel(lvl.price, lvl.ratio, lvl.colorInt)

                    // Alerts: Pine alert() when close crosses a level between bars
                    val prevSides = autoFibPrevSideState
                    val livePrice = currentQuoteState?.lastPrice ?: ohlcData.last().close
                    for (lvl in af.levels) {
                        val key = lvl.ratio.toString()
                        val curSide = when {
                            livePrice > lvl.price -> 1
                            livePrice < lvl.price -> -1
                            else -> 0
                        }
                        val old = prevSides[key]
                        if (old != null && old != 0 && curSide != 0 && old != curSide) {
                            try {
                                com.asc.markets.notifications.NotificationHelper.showAlert(
                                    context,
                                    "AutoFib Level Cross",
                                    "$symbol crossing level ${lvl.ratio}",
                                    type = "autofib_cross",
                                    symbol = symbol
                                )
                            } catch (_: Exception) { }
                            android.util.Log.d("TradingChart", "Autofib: $symbol crossing level ${lvl.ratio}")
                        }
                        prevSides[key] = curSide
                    }
                }
                lastAutoFibSig = sig
            }
        } else {
            clearAutoFibRender(seriesApi)
            if (!showAutoFib) { autoFibPriceLineOwner = null; autoFibPrevSideState.clear() }
            lastAutoFibSig = null
        }
        }.onFailure { android.util.Log.w("TradingChart", "render pass skipped: " + it.message) }
    }

    // Confluence FVG Finder - standalone render pass (settings-driven, full Pine feature set)
    LaunchedEffect(showConfluenceFvg, ohlcData, seriesApi, chartsViewApi, timeframe, cfvgSettings) {
        runCatching {
        val mainSeriesApi = seriesApi
        val ohlcList = ohlcData
        // Confluence FVG Finder - merged MTF zones with info label writeups
        fun clearCfRender(targetApi: SeriesApi?) {
            cfPriceLines.forEach { safelyRemovePriceLine(cfPriceLineOwner ?: targetApi, it) }
            cfPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                cfBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            cfBoxSeries.clear()
            cfPriceLineOwner = targetApi ?: cfPriceLineOwner
        }
        if (showConfluenceFvg && mainSeriesApi != null && ohlcData.size >= 30 && chartsViewApi != null) {
            val ownerChanged = cfPriceLineOwner != seriesApi
            val chartTfSec = timeframeToSeconds(timeframe).coerceAtLeast(60L)
            val cf = com.trading.app.indicators.ConfluenceFvgIndicator()
                .calculateZones(ohlcData, chartTfSec, ohlcData.last().close, cfvgSettings)
            cfvgZones = cf?.zones ?: emptyList()
            val sig = if (cf == null) "null" else cf.zones.joinToString(";") { z ->
                listOf(
                    z.formTime, z.confluenceCount, z.session,
                    String.format(java.util.Locale.US, "%.6f", z.top),
                    String.format(java.util.Locale.US, "%.6f", z.bot),
                    String.format(java.util.Locale.US, "%.1f", z.strength),
                    z.bullish.toString(), z.mitigated.toString(), cfvgSettings.hashCode()
                ).joinToString(",")
            } + "|${ohlcData.size}|$timeframe"
            if (ownerChanged || lastCfSig != sig || cfBoxSeries.isEmpty()) {
                clearCfRender(seriesApi)
                cfPriceLineOwner = seriesApi
                if (cf != null) {
                    val extendT = ohlcData.last().time + 40L * chartTfSec
                    for (z in cf.zones) {
                        val fillCss = when {
                            z.mitigated -> runCatching { AndroidColor.parseColor(cfvgSettings.mitigatedColorHex) }.getOrDefault(AndroidColor.parseColor("#787b86"))
                            z.bullish -> AndroidColor.parseColor(cfvgSettings.bullishColorHex)
                            else -> AndroidColor.parseColor(cfvgSettings.bearishColorHex)
                        }
                        val borderCss = when {
                            z.mitigated -> fillCss
                            z.bullish -> runCatching { AndroidColor.parseColor(cfvgSettings.bullishBorderHex) }.getOrDefault(fillCss)
                            else -> runCatching { AndroidColor.parseColor(cfvgSettings.bearishBorderHex) }.getOrDefault(fillCss)
                        }
                        val borderAlphaPct = if (z.mitigated) 30 else 70
                        val borderW = when (cfvgSettings.borderWidth.coerceIn(1, 5)) {
                            1 -> LineWidth.ONE; 2 -> LineWidth.TWO; 3 -> LineWidth.THREE
                            else -> LineWidth.FOUR
                        }
                        if (cfvgSettings.styleBoxes) {
                            chartsViewApi?.api?.addBaselineSeries(
                                options = BaselineSeriesOptions(
                                    baseValue = com.trading.app.indicators.FloatPriceBaseValue(z.bot.toDouble()),
                                    baseLineVisible = true,
                                    baseLineColor = IntColor(applyOpacity(borderCss, borderAlphaPct)),
                                    topLineColor = IntColor(applyOpacity(borderCss, borderAlphaPct)),
                                    topFillColor1 = IntColor(applyOpacity(fillCss, 15)),
                                    topFillColor2 = IntColor(applyOpacity(fillCss, 15)),
                                    bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                    bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                    bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                    lineWidth = borderW,
                                    lineStyle = if (z.mitigated) LineStyle.DASHED else LineStyle.SOLID,
                                    priceLineVisible = false,
                                    lastValueVisible = false,
                                    crosshairMarkerVisible = false
                                ),
                                onSeriesCreated = { box ->
                                    runCatching {
                                        box.setData(listOf(BaselineData(time = Time.Utc(z.formTime), value = z.top), BaselineData(time = Time.Utc(extendT), value = z.top)))
                                    }.onFailure { android.util.Log.w("TradingChart", "FVG zone skipped: ${it.message}") }
                                    cfBoxSeries.add(box)
                                }
                            )
                        }
                        // Direction label / extended info writeup on the price axis
                        val dirLabel = if (z.bullish) "Bull FVG" else "Bear FVG"
                        val mitigTag = if (z.mitigated) " \u2022 MITIGATED" else ""
                        val writeup = "$dirLabel \u2022 ${z.confluenceCount}TF \u2022 ${String.format("%.1f", z.strength)}/10 \u2022 ${z.session} \u2022 ${z.pips}p$mitigTag"
                        val title = when {
                            !cfvgSettings.stylePaneLabels -> ""
                            !cfvgSettings.showDirectionLabels -> ""
                            cfvgSettings.showExtendedInfo -> writeup
                            else -> "$dirLabel${if (z.mitigated) " MIT" else ""} ${z.pips}p"
                        }
                        if (title.isNotEmpty()) {
                            val mid = (z.top + z.bot) / 2f
                            cfPriceLines.add(
                                mainSeriesApi.createPriceLine(
                                    PriceLineOptions(
                                        price = mid,
                                        color = IntColor(fillCss),
                                        lineWidth = LineWidth.ONE,
                                        lineStyle = LineStyle.DOTTED,
                                        lineVisible = false,
                                        axisLabelVisible = true,
                                        title = title
                                    )
                                )
                            )
                        }
                    }
                }
                lastCfSig = sig
            }
        } else {
            clearCfRender(seriesApi)
            if (!showConfluenceFvg) cfPriceLineOwner = null
            lastCfSig = null
            cfvgZones = emptyList()
        }
        }.onFailure { android.util.Log.w("TradingChart", "render pass skipped: " + it.message) }
    }

    // Confluence FVG zone-entry alarms (30s cooldown per zone)
    LaunchedEffect(currentQuoteState, cfvgZones, cfvgSettings.alertsEnabled, showConfluenceFvg) {
        if (!showConfluenceFvg || !cfvgSettings.alertsEnabled) return@LaunchedEffect
        val price = currentQuoteState?.lastPrice ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        for (z in cfvgZones) {
            if (z.mitigated) continue
            val inside = price >= z.bot && price <= z.top
            val was = cfvgPrevInside[z.formTime] ?: false
            if (inside && !was && now - (cfvgLastAlertMs[z.formTime] ?: 0L) > 30_000L) {
                cfvgLastAlertMs[z.formTime] = now
                val direction = if (z.bullish) "Bullish" else "Bearish"
                com.asc.markets.notifications.NotificationHelper.showAlert(
                    context,
                    "Confluence FVG Zone",
                    "$direction FVG zone (${z.confluenceCount}TF)",
                    type = "cfvg_zone",
                    symbol = symbol
                )
            }
            cfvgPrevInside[z.formTime] = inside
        }
    }

    // Supply & Demand Visible Range: recompute zones when the user pans/zooms.
    // Visible-range events are debounced, then bump sdVrVisibleTick which is a
    // key of the Editors' Picks render pass below.
    val updatedShowSdVr = rememberUpdatedState(showSupplyDemandDaily)
    val sdVrScope = rememberCoroutineScope()
    DisposableEffect(chartsViewApi, showSupplyDemandDaily) {
        val tsApi = chartsViewApi?.api?.timeScale
        var job: kotlinx.coroutines.Job? = null
        val cb: (com.tradingview.lightweightcharts.api.series.models.TimeRange?) -> Unit = {
            if (updatedShowSdVr.value) {
                job?.cancel()
                job = sdVrScope.launch {
                    kotlinx.coroutines.delay(250)
                    sdVrVisibleTick.intValue++
                }
            }
        }
        runCatching { tsApi?.subscribeVisibleTimeRangeChange(cb) }
        onDispose {
            runCatching { tsApi?.unsubscribeVisibleTimeRangeChange(cb) }
            job?.cancel()
        }
    }

    // Zone-entry alarms: toast when price crosses into a supply/demand zone
    // (30s cooldown per side to avoid spam while price hugs the boundary).
    LaunchedEffect(currentQuoteState, sdVrZones, sdVrSettings.alertsEnabled, showSupplyDemandDaily) {
        if (!showSupplyDemandDaily || !sdVrSettings.alertsEnabled) return@LaunchedEffect
        val zones = sdVrZones ?: return@LaunchedEffect
        val price = currentQuoteState?.lastPrice ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        val inSupply = zones.supply.found && price >= zones.supply.bottom && price <= zones.supply.top
        val inDemand = zones.demand.found && price >= zones.demand.bottom && price <= zones.demand.top
        listOf("supply" to inSupply, "demand" to inDemand).forEach { (side, inside) ->
            val was = sdVrPrevInside[side] ?: false
            if (inside && !was && now - (sdVrLastAlertMs[side] ?: 0L) > 30_000L) {
                sdVrLastAlertMs[side] = now
                val label = if (side == "supply") "SUPPLY" else "DEMAND"
                com.asc.markets.notifications.NotificationHelper.showAlert(
                    context,
                    "Price Entered $label Zone",
                    "Price entered $label zone for $symbol",
                    type = "sd_zone",
                    symbol = symbol
                )
            }
            sdVrPrevInside[side] = inside
        }
    }

    // Editors' Picks overlays: Premium/Discount, FVG, Supply & Demand, OTE - standalone render pass
    LaunchedEffect(showPremiumDiscount, showFairValueGap, showSupplyDemandDaily, showOteVisibleChart, showLiquidityDeltaProfiler, showEqhEqlLiquidityZones, showPowerHourBreakout, showTrendlineBreakouts, showTrendlineNavigator, showLiquidityPools, showOrderBlockBreaker, showVolumaticFvg, ldpSettings, eqhEqlSettings, powerHourSettings, trendlineSettings, navigatorSettings, liquidityPoolsSettings, obbSettings, volumaticFvgSettings, hiddenIndicators, ohlcData, seriesApi, chartsViewApi, timeframe) {
        chartBusy = true
        // Heavy LDP pivot/zone math off the main thread so ticks don't queue behind it
        val ldpPrecomputed = if (showLiquidityDeltaProfiler && "LIQUIDITY_DELTA_PROFILER" !in hiddenIndicators && ohlcData.size >= 30) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.LiquidityDeltaProfilerIndicator.calculate(ohlcData, ldpSettings)
            }
        } else null
        // Heavy EQH/EQL pivot/zone math off the main thread so ticks don't queue behind it
        val eqhPrecomputed = if (showEqhEqlLiquidityZones && "EQH_EQL_LIQUIDITY_ZONES" !in hiddenIndicators && ohlcData.size >= 30) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.EqhEqlLiquidityZonesIndicator.calculate(ohlcData, eqhEqlSettings)
            }
        } else null
        // Power Hour Breakout session scan off the main thread (session math + breakout pass)
        val phPrecomputed = if (showPowerHourBreakout && "POWER_HOUR_BREAKOUT" !in hiddenIndicators && ohlcData.size >= 30) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.PowerHourBreakoutIndicator.calculate(ohlcData, powerHourSettings)
            }
        } else null
        // Trendline Breakouts pivot/signal math off the main thread (ATR + pivots + one trade state)
        val tbtPrecomputed = if (showTrendlineBreakouts && "TRENDLINE_BREAKOUTS" !in hiddenIndicators && ohlcData.size >= 60) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.TrendlineBreakoutsIndicator.calculate(ohlcData, trendlineSettings)
            }
        } else null
        // Trendline Navigator swing math off the main thread (pivots + active-line state machine)
        val tnavPrecomputed = if (showTrendlineNavigator && "TRENDLINE_NAVIGATOR" !in hiddenIndicators && ohlcData.size >= 80) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.TrendlineNavigatorIndicator.calculate(ohlcData, navigatorSettings)
            }
        } else null
        // Liquidity Pools running-extreme math off the main thread (contacts + volume + zone state)
        val lpPrecomputed = if (showLiquidityPools && "LIQUIDITY_POOLS" !in hiddenIndicators && ohlcData.size >= 15) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.LiquidityPoolsIndicator.calculate(ohlcData, liquidityPoolsSettings)
            }
        } else null
        // Pure Price Action Order & Breaker Blocks swing/OB math off the main thread (vector swings + breaker state)
        val obbPrecomputed = if (showOrderBlockBreaker && "ORDER_BLOCK_BREAKER" !in hiddenIndicators && ohlcData.size >= 15) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.OrderBlockBreakerIndicator.calculate(ohlcData, obbSettings)
            }
        } else null
        // Volumatic Fair Value Gaps volume-split + cleanup math off the main thread (rolling max filter + zone state)
        val vfvgPrecomputed = if (showVolumaticFvg && "VOLUMATIC_FVG" !in hiddenIndicators && ohlcData.size >= 15) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.trading.app.indicators.VolumaticFvgIndicator.calculate(ohlcData, volumaticFvgSettings)
            }
        } else null
        runCatching {
        val mainSeriesApi = seriesApi
        // Premium & Discount Delta Volume [BigBeluga] - Editors' picks overlay (exact Pine logic)
        // Boxes rendered as BaselineSeries rectangles: flat top line + baseValue = filled box across [lookback .. +future bars]
        val pdUpColorInt = AndroidColor.parseColor("#79c1f1")
        val pdDownColorInt = AndroidColor.parseColor("#f19579")
        val pdEqColorInt = AndroidColor.parseColor("#787B86")
        fun clearPdRender(mainApi: SeriesApi?) {
            val lineOwner = pdPriceLineOwner ?: mainApi
            safelyRemovePriceLine(lineOwner, pdSrUpperTopState.value); pdSrUpperTopState.value = null
            safelyRemovePriceLine(lineOwner, pdSrLowerBottomState.value); pdSrLowerBottomState.value = null
            safelyRemovePriceLine(lineOwner, pdEquilibriumState.value); pdEquilibriumState.value = null
            safelyRemovePriceLine(lineOwner, pdMacroEquilibriumState.value); pdMacroEquilibriumState.value = null
            pdFallbackLines.forEach { safelyRemovePriceLine(mainApi ?: lineOwner, it) }
            pdFallbackLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                pdBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            pdBoxSeries.clear()
        }
        if (showPremiumDiscount && "PREMIUM_DISCOUNT" !in hiddenIndicators && ohlcData.size >= 10 && mainSeriesApi != null && chartsViewApi != null) {
            val pd = com.trading.app.indicators.PremiumDiscountIndicator(50, 200).calculatePremiumDiscount(ohlcData)
            val ownerChanged = pdPriceLineOwner != mainSeriesApi
            // Recreating future-dated box series on every tick grows the time scale and
            // slides candles right — rebuild only when levels/bars/timeframe actually change
            val sig = if (pd == null) "null" else listOf(
                pd.srUpperTop, pd.srUpperBottom, pd.srLowerTop, pd.srLowerBottom,
                pd.macroUpperTop, pd.macroUpperBottom, pd.macroLowerTop, pd.macroLowerBottom,
                pd.equilibrium, pd.macroEquilibrium, pd.deltaVolSR, pd.deltaVolMacro
            ).joinToString(",") { String.format(java.util.Locale.US, "%.6f", it) } + "|${ohlcData.size}|$timeframe"
            if (ownerChanged || lastPdSig != sig || pdBoxSeries.isEmpty()) {
                clearPdRender(mainSeriesApi)
                pdPriceLineOwner = mainSeriesApi
                if (pd != null) {
                val tfSec = timeframeToSeconds(timeframe).coerceAtLeast(60L)
                val lastT = ohlcData.last().time
                // FloatPriceBaseValue supports fractional bases - zones render on ALL symbols now
                val canDrawBoxes = true
                fun pdAddBox(top: Float, bottom: Float, colorInt: Int, fillOpacityPct: Int, borderVisible: Boolean, startBackBars: Int, extendFutureBars: Long) {
                    if (!canDrawBoxes) return
                    val startT = ohlcData.getOrNull((ohlcData.size - 1 - startBackBars).coerceAtLeast(0))?.time ?: lastT
                    val endT = lastT + extendFutureBars * tfSec
                    val fillColor = IntColor(applyOpacity(colorInt, fillOpacityPct))
                    val lineColor = if (borderVisible) IntColor(colorInt or 0xFF000000.toInt()) else IntColor(AndroidColor.TRANSPARENT)
                    chartsViewApi?.api?.addBaselineSeries(
                        options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(bottom.toDouble()),
                            baseLineVisible = false,
                            baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                            topLineColor = lineColor,
                            topFillColor1 = fillColor,
                            topFillColor2 = fillColor,
                            bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                            bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                            bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.SOLID,
                            priceLineVisible = false,
                            lastValueVisible = false,
                            crosshairMarkerVisible = false
                        ),
                        onSeriesCreated = { createdBox ->
                            createdBox.setData(listOf(BaselineData(time = Time.Utc(startT), value = top), BaselineData(time = Time.Utc(endT), value = top)))
                            pdBoxSeries.add(createdBox)
                        }
                    )
                }
                // Fallback for low-priced symbols: paired band lines (top solid / bottom dashed)
                fun pdAddFallbackPair(top: Float, bottom: Float, colorInt: Int, label: String) {
                    if (canDrawBoxes) return
                    val c = IntColor(colorInt or 0xFF000000.toInt())
                    val owner = mainSeriesApi ?: return
                    pdFallbackLines.add(owner.createPriceLine(PriceLineOptions(price = top, color = c, lineWidth = LineWidth.ONE, lineStyle = LineStyle.SOLID, lineVisible = true, axisLabelVisible = false, title = label + " upper")))
                    pdFallbackLines.add(owner.createPriceLine(PriceLineOptions(price = bottom, color = c, lineWidth = LineWidth.ONE, lineStyle = LineStyle.DASHED, lineVisible = true, axisLabelVisible = false, title = label + " lower")))
                }
                // Macro boxes (200 lookback, extends 70 bars into future) - 60% transparent fills like Pine bgcolor 60
                pdAddBox(pd.macroUpperTop, pd.macroUpperBottom, pdDownColorInt, 40, false, 200, 70)
                pdAddBox(pd.macroLowerTop, pd.macroLowerBottom, pdUpColorInt, 40, false, 200, 70)
                // SR Premium box: [srHighs.max(), srHighs.max()+atr] with colored border (Pine bg 100 -> subtle tint)
                pdAddBox(pd.srUpperTop, pd.srUpperBottom, pdDownColorInt, 14, true, 50, 50)
                // SR Discount box: [srLows.min()-atr, srLows.min()]
                pdAddBox(pd.srLowerTop, pd.srLowerBottom, pdUpColorInt, 14, true, 50, 50)
                // Mid delta-volume box between SR boxes (Pine bg 93 -> 7% opacity)
                val midDeltaColorInt = if (pd.deltaVolSR > 0) pdUpColorInt else pdDownColorInt
                pdAddBox(pd.srUpperBottom, pd.srLowerTop, midDeltaColorInt, 7, false, 50, 50)
                // Fallback bands when box rendering unavailable
                pdAddFallbackPair(pd.srUpperTop, pd.srUpperBottom, pdDownColorInt, "PREMIUM")
                pdAddFallbackPair(pd.macroUpperTop, pd.macroUpperBottom, pdDownColorInt, "Macro PREMIUM")
                pdAddFallbackPair(pd.srLowerTop, pd.srLowerBottom, pdUpColorInt, "DISCOUNT")
                pdAddFallbackPair(pd.macroLowerTop, pd.macroLowerBottom, pdUpColorInt, "Macro DISCOUNT")
                // Labels via price lines at box outer edges + equilibrium lines
                pdSrUpperTopState.value = mainSeriesApi.createPriceLine(
                    PriceLineOptions(price = pd.srUpperTop, color = IntColor(pdDownColorInt), lineWidth = LineWidth.ONE, lineStyle = LineStyle.DOTTED, lineVisible = true, axisLabelVisible = false, title = "PREMIUM: ${String.format("%.0f", kotlin.math.abs(pd.negVolSRSum))} vol")
                )
                pdSrLowerBottomState.value = mainSeriesApi.createPriceLine(
                    PriceLineOptions(price = pd.srLowerBottom, color = IntColor(pdUpColorInt), lineWidth = LineWidth.ONE, lineStyle = LineStyle.DOTTED, lineVisible = true, axisLabelVisible = false, title = "DISCOUNT: ${String.format("%.0f", kotlin.math.abs(pd.posVolSRSum))} vol")
                )
                pdEquilibriumState.value = mainSeriesApi.createPriceLine(
                    PriceLineOptions(price = pd.equilibrium, color = IntColor(pdEqColorInt), lineWidth = LineWidth.ONE, lineStyle = LineStyle.DASHED, lineVisible = true, axisLabelVisible = true, title = "Eq ΔVol ${String.format("%.1f%%", pd.deltaVolSR)} ${if (pd.deltaVolSR > 0) "↑ Discount" else "↓ Premium"}")
                )
                pdMacroEquilibriumState.value = mainSeriesApi.createPriceLine(
                    PriceLineOptions(price = pd.macroEquilibrium, color = IntColor(AndroidColor.parseColor("#363A45")), lineWidth = LineWidth.ONE, lineStyle = LineStyle.DASHED, lineVisible = true, axisLabelVisible = true, title = "Macro ΔVol ${String.format("%.1f%%", pd.deltaVolMacro)}")
                )
            }
                lastPdSig = sig
            }
        } else {
            clearPdRender(mainSeriesApi)
            if (!showPremiumDiscount) pdPriceLineOwner = null
            lastPdSig = null
        }

        // Fair Value Gap [LuxAlgo] - Editors' picks overlay (Pine-exact with full settings)
        fun clearFvgRender(targetApi: SeriesApi?) {
            val api = targetApi ?: fvgPriceLineOwner ?: return
            fvgPriceLines.forEach { safelyRemovePriceLine(api, it) }
            fvgPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                fvgBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                fvgLineSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            fvgBoxSeries.clear()
            fvgLineSeries.clear()
        }
        if (showFairValueGap && "FAIR_VALUE_GAP" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 3 && chartsViewApi != null) {
            val ownerChanged = fvgPriceLineOwner != mainSeriesApi
            val chartTfSec = timeframeToSeconds(timeframe).coerceAtLeast(60L)
            val fvgData = com.trading.app.indicators.FairValueGapIndicator().calculateFvg(ohlcData, fvgSettings, chartTfSec)
            fvgDashboardData = fvgData
            val toShow = fvgData.fvgs.takeLast(50)
            val sig = toShow.joinToString(";") { "${it.time}|${String.format(java.util.Locale.US, "%.6f", it.max)}|${String.format(java.util.Locale.US, "%.6f", it.min)}|${it.isBull}" } +
                "|${fvgData.unmitigatedLines.size}|${fvgData.mitigatedLines.size}|${ohlcData.size}|$timeframe|${fvgSettings.hashCode()}"
            if (ownerChanged || lastFvgSig != sig || fvgBoxSeries.isEmpty()) {
                clearFvgRender(mainSeriesApi)
                fvgPriceLineOwner = mainSeriesApi
                val tfSec = chartTfSec
                val lastTime = ohlcData.last().time
                if (fvgSettings.styleBoxes && !fvgSettings.dynamic) {
                    for (fvg in toShow) {
                        val cssInt = if (fvg.isBull) runCatching { AndroidColor.parseColor(fvgSettings.bullColorHex) }.getOrDefault(AndroidColor.parseColor("#089981"))
                        else runCatching { AndroidColor.parseColor(fvgSettings.bearColorHex) }.getOrDefault(AndroidColor.parseColor("#f23645"))
                        val fillColor = IntColor(applyOpacity(cssInt, 30))
                        chartsViewApi?.api?.addBaselineSeries(
                            options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(fvg.min.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = fillColor,
                                topFillColor2 = fillColor,
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { createdBox ->
                                val leftT = ohlcData.getOrNull((fvg.index - 2).coerceAtLeast(0))?.time ?: fvg.time
                                var rightT = fvg.time + fvgSettings.extend.toLong() * tfSec
                                if (rightT <= leftT) rightT = leftT + tfSec
                                runCatching {
                                    createdBox.setData(listOf(BaselineData(time = Time.Utc(leftT), value = fvg.max), BaselineData(time = Time.Utc(rightT), value = fvg.max)))
                                }.onFailure { android.util.Log.w("TradingChart", "FVG box skipped: ${it.message}") }
                                fvgBoxSeries.add(createdBox)
                            }
                        )
                    }
                }
                if (fvgSettings.styleLines && fvgData.unmitigatedLines.isNotEmpty()) {
                    for (fvg in fvgData.unmitigatedLines) {
                        val cssInt = if (fvg.isBull) runCatching { AndroidColor.parseColor(fvgSettings.bullColorHex) }.getOrDefault(AndroidColor.parseColor("#089981"))
                        else runCatching { AndroidColor.parseColor(fvgSettings.bearColorHex) }.getOrDefault(AndroidColor.parseColor("#f23645"))
                        val lvl = if (fvg.isBull) fvg.min else fvg.max
                        chartsViewApi?.api?.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(cssInt or 0xFF000000.toInt()),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { line ->
                                runCatching {
                                    line.setData(listOf(LineData(time = Time.Utc(fvg.time), value = lvl), LineData(time = Time.Utc(lastTime), value = lvl)))
                                }.onFailure { android.util.Log.w("TradingChart", "FVG line skipped: ${it.message}") }
                                fvgLineSeries.add(line)
                            }
                        )
                    }
                }
                if (fvgSettings.styleLines && fvgSettings.mitigationLevels && fvgData.mitigatedLines.isNotEmpty()) {
                    for (fvg in fvgData.mitigatedLines) {
                        val cssInt = if (fvg.isBull) runCatching { AndroidColor.parseColor(fvgSettings.bullColorHex) }.getOrDefault(AndroidColor.parseColor("#089981"))
                        else runCatching { AndroidColor.parseColor(fvgSettings.bearColorHex) }.getOrDefault(AndroidColor.parseColor("#f23645"))
                        val lvl = if (fvg.isBull) fvg.min else fvg.max
                        chartsViewApi?.api?.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(cssInt or 0xFF000000.toInt()),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.DASHED,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { line ->
                                runCatching {
                                    line.setData(listOf(LineData(time = Time.Utc(fvg.time), value = lvl), LineData(time = Time.Utc(lastTime), value = lvl)))
                                }.onFailure { android.util.Log.w("TradingChart", "FVG mitigated skipped: ${it.message}") }
                                fvgLineSeries.add(line)
                            }
                        )
                    }
                }
                lastFvgSig = sig
            }
        } else {
            clearFvgRender(mainSeriesApi)
            if (!showFairValueGap) {
                fvgPriceLineOwner = null
                fvgDashboardData = null
            }
            lastFvgSig = null
        }

        // Supply and Demand Visible Range [LuxAlgo] - Editors' picks overlay
        // Zones span the VISIBLE range and recompute on pan/zoom (sdVrVisibleTick),
        // with volume histogram columns, equilibrium lines and zone-entry alarms.
        fun clearSdRender(targetApi: SeriesApi?) {
            val api = targetApi ?: sdPriceLineOwner ?: return
            safelyRemovePriceLine(api, sdSupplyTopState.value); sdSupplyTopState.value = null
            safelyRemovePriceLine(api, sdSupplyBottomState.value); sdSupplyBottomState.value = null
            safelyRemovePriceLine(api, sdSupplyAvgState.value); sdSupplyAvgState.value = null
            safelyRemovePriceLine(api, sdSupplyWavgState.value); sdSupplyWavgState.value = null
            safelyRemovePriceLine(api, sdDemandTopState.value); sdDemandTopState.value = null
            safelyRemovePriceLine(api, sdDemandBottomState.value); sdDemandBottomState.value = null
            safelyRemovePriceLine(api, sdDemandAvgState.value); sdDemandAvgState.value = null
            safelyRemovePriceLine(api, sdDemandWavgState.value); sdDemandWavgState.value = null
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                sdBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                sdLineSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            sdBoxSeries.clear()
            sdLineSeries.clear()
        }
        if (showSupplyDemandDaily && "SUPPLY_DEMAND_DAILY" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 5 && chartsViewApi != null) {
            @Suppress("UNUSED_EXPRESSION") sdVrVisibleTick.intValue
            val myGen = ++sdVrGen.intValue
            chartsViewApi?.api?.timeScale?.getVisibleRange { range ->
                if (sdVrGen.intValue != myGen) return@getVisibleRange
                val fromSec = (range?.from as? com.tradingview.lightweightcharts.api.series.models.Time.Utc)?.timestamp
                val toSec = (range?.to as? com.tradingview.lightweightcharts.api.series.models.Time.Utc)?.timestamp
                if (fromSec == null || toSec == null) return@getVisibleRange
                // Slice candles to the visible window (Pine: left/right visible bar time)
                var lo = ohlcData.indexOfFirst { it.time >= fromSec }; if (lo < 0) lo = 0
                var hi = ohlcData.indexOfLast { it.time <= toSec }; if (hi < lo) hi = ohlcData.lastIndex
                val slice = ohlcData.subList(lo, hi + 1)
                val s = com.trading.app.indicators.SupplyDemandVrIndicator.calculate(
                    slice, sdVrSettings.thresholdPercent, sdVrSettings.resolution
                )
                sdVrZones = s
                val owner = mainSeriesApi
                clearSdRender(owner)
                sdPriceLineOwner = owner
                if (s != null) {
                    val x1T = slice.first().time
                    val lastT = slice.last().time
                    val tfSec = when (currentTimeframe.value) {
                        "1m" -> 60L; "5m" -> 300L; "15m" -> 900L; "30m" -> 1800L
                        "1h" -> 3600L; "4h" -> 14400L; "1D" -> 86400L; "1W" -> 604800L
                        else -> 60L
                    }
                    fun addZoneBox(leftT: Long, rightTIn: Long, top: Float, btm: Float, colorInt: Int, fillOpacityPct: Int) {
                        // Zero-width windows/columns produce duplicate timestamps,
                        // which the chart library rejects with a fatal assert.
                        var rightT = rightTIn
                        if (rightT <= leftT) rightT = leftT + tfSec
                        chartsViewApi?.api?.addBaselineSeries(
                            options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(btm.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = IntColor(applyOpacity(colorInt, fillOpacityPct)),
                                topFillColor2 = IntColor(applyOpacity(colorInt, fillOpacityPct)),
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { createdZone ->
                                runCatching {
                                    createdZone.setData(listOf(BaselineData(time = Time.Utc(leftT), value = top), BaselineData(time = Time.Utc(rightT), value = top)))
                                }.onFailure { android.util.Log.w("TradingChart", "SD zone skipped: ${it.message}") }
                                sdBoxSeries.add(createdZone)
                            }
                        )
                    }
                    fun addLevel(price: Float, colorInt: Int, dashed: Boolean) {
                        chartsViewApi?.api?.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(colorInt or 0xFF000000.toInt()),
                                lineWidth = LineWidth.ONE,
                                lineStyle = if (dashed) LineStyle.DASHED else LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { line ->
                                runCatching {
                                    line.setData(listOf(LineData(time = Time.Utc(x1T), value = price), LineData(time = Time.Utc(lastT), value = price)))
                                }.onFailure { android.util.Log.w("TradingChart", "SD level skipped: ${it.message}") }
                                sdLineSeries.add(line)
                            }
                        )
                    }
                    val supColor = AndroidColor.parseColor(sdVrSettings.supplyColorHex)
                    val demColor = AndroidColor.parseColor(sdVrSettings.demandColorHex)
                    val eqColor = AndroidColor.parseColor(sdVrSettings.equilibriumColorHex)
                    if (s.supply.found && sdVrSettings.showSupply) {
                        if (sdVrSettings.supplyArea) {
                            addZoneBox(x1T, lastT, s.supply.top, s.supply.bottom, supColor, 20)
                            s.supply.columns.forEach { c ->
                                addZoneBox(x1T, x1T + c.widthBars * tfSec, c.top, c.btm, supColor, 50)
                            }
                        }
                        if (sdVrSettings.supplyAvg) addLevel(s.supply.avg, supColor, dashed = false)
                        if (sdVrSettings.supplyWavg) addLevel(s.supply.wavg, supColor, dashed = true)
                    }
                    if (s.demand.found && sdVrSettings.showDemand) {
                        if (sdVrSettings.demandArea) {
                            addZoneBox(x1T, lastT, s.demand.top, s.demand.bottom, demColor, 20)
                            s.demand.columns.forEach { c ->
                                addZoneBox(x1T, x1T + c.widthBars * tfSec, c.top, c.btm, demColor, 50)
                            }
                        }
                        if (sdVrSettings.demandAvg) addLevel(s.demand.avg, demColor, dashed = false)
                        if (sdVrSettings.demandWavg) addLevel(s.demand.wavg, demColor, dashed = true)
                    }
                    if (sdVrSettings.showEquilibrium && s.supply.found && s.demand.found) {
                        if (sdVrSettings.equilibriumAvg) addLevel(s.equiAvg, eqColor, dashed = false)
                        if (sdVrSettings.equilibriumWavg) addLevel(s.equiWavg, eqColor, dashed = true)
                    }
                }
            }
        } else {
            clearSdRender(mainSeriesApi)
            if (!showSupplyDemandDaily) sdPriceLineOwner = null
            sdVrZones = null
        }

        // OTE visible chart [twingall] - Editors' picks overlay (Pine-exact)
        // Fib box 61.8-78.6% (yellow transp 82, dashed border) + dotted width-2 fib lines
        // spanning [leftTime..rightTime] with extend.right + red dotted extensions
        fun clearOteRender(targetApi: SeriesApi?) {
            val api = targetApi ?: otePriceLineOwner ?: return
            otePriceLines.forEach { safelyRemovePriceLine(api, it) }
            otePriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                oteBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
                oteLineSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            oteBoxSeries.clear()
            oteLineSeries.clear()
        }
        if (showOteVisibleChart && "OTE_VISIBLE_CHART" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 3 && chartsViewApi != null) {
            val ownerChanged = otePriceLineOwner != mainSeriesApi
            val ote = com.trading.app.indicators.OteVisibleChartIndicator().calculateOte(ohlcData)
            // Rebuild only on real changes — per-tick recreation slides candles right
            val sig = if (ote == null) "null" else listOf(
                ote.chartHigh, ote.chartLow, ote.boxTop, ote.boxBottom,
                ote.isBull.toString(),
                ote.leftTime, ote.rightTime, ohlcData.size.toLong(), timeframeToSeconds(timeframe)
            ).joinToString(",") + "|" + (ote.levels.joinToString(";") { "${it.label}:${String.format(java.util.Locale.US, "%.6f", it.price)}" }) + "|" + (ote.extensions.joinToString(";") { String.format(java.util.Locale.US, "%.6f", it.price) })
            if (ownerChanged || lastOteSig != sig || (oteBoxSeries.isEmpty() && oteLineSeries.isEmpty())) {
                clearOteRender(mainSeriesApi)
                otePriceLineOwner = mainSeriesApi
                if (ote != null) {
                    // FloatPriceBaseValue supports fractional bases - zones render on ALL symbols now
                    val canDrawBoxes = true
                    if (canDrawBoxes) {
                        val tfSec = timeframeToSeconds(timeframe).coerceAtLeast(60L)
                        val lastT = ohlcData.last().time
                        val leftT = ote.leftTime
                        val rightT = ote.rightTime
                        val extendT = lastT + 40L * tfSec // Pine extend.right approximation
                        // Fib box: yellow 82-transp fill, invisible dashed border, [left..right] + extend right
                        chartsViewApi?.api?.addBaselineSeries(
                            options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(kotlin.math.min(ote.boxTop, ote.boxBottom).toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = IntColor(applyOpacity(AndroidColor.parseColor("#FFEB3B"), 18)),
                                topFillColor2 = IntColor(applyOpacity(AndroidColor.parseColor("#FFEB3B"), 18)),
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { createdBox ->
                                createdBox.setData(listOf(BaselineData(time = Time.Utc(leftT), value = kotlin.math.max(ote.boxTop, ote.boxBottom)), BaselineData(time = Time.Utc(extendT), value = kotlin.math.max(ote.boxTop, ote.boxBottom))))
                                oteBoxSeries.add(createdBox)
                            }
                        )
                        // Dotted width-2 fib lines: [leftTime..rightTime] extended right
                        fun oteAddFibLine(price: Float, colorInt: Int) {
                            chartsViewApi?.api?.addLineSeries(
                                options = LineSeriesOptions(
                                    color = IntColor(colorInt or 0xFF000000.toInt()),
                                    lineWidth = LineWidth.TWO,
                                    lineStyle = LineStyle.DOTTED,
                                    priceLineVisible = false,
                                    lastValueVisible = false,
                                    crosshairMarkerVisible = false
                                ),
                                onSeriesCreated = { line ->
                                    line.setData(listOf(LineData(time = Time.Utc(leftT), value = price), LineData(time = Time.Utc(rightT), value = price), LineData(time = Time.Utc(extendT), value = price)))
                                    oteLineSeries.add(line)
                                }
                            )
                        }
                        // Retracements: 100/0 purple #D94CD9, 50 gray #787B86, opt greens #4CAF50
                        for (lvl in ote.levels) {
                            val c = when (lvl.label) {
                                "50" -> AndroidColor.parseColor("#787B86")
                                "100", "0" -> AndroidColor.parseColor("#D94CD9")
                                else -> AndroidColor.parseColor("#4CAF50")
                            }
                            oteAddFibLine(lvl.price, c)
                        }
                        // Extensions: red dotted
                        for (ext in ote.extensions) {
                            oteAddFibLine(ext.price, AndroidColor.parseColor("#F23645"))
                        }
                    } else {
                        // Low-priced fallback: full-width price lines
                        val boxColor = IntColor(AndroidColor.parseColor("#FFEB3B"))
                        otePriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(price = ote.boxTop, color = boxColor, lineWidth = LineWidth.ONE, lineStyle = LineStyle.DASHED, lineVisible = true, axisLabelVisible = true, title = "OTE 78.6 ${String.format("%.2f", ote.boxTop)}")))
                        otePriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(price = ote.boxBottom, color = boxColor, lineWidth = LineWidth.ONE, lineStyle = LineStyle.DASHED, lineVisible = true, axisLabelVisible = true, title = "OTE 61.8 ${String.format("%.2f", ote.boxBottom)}${if (ote.isBull) " Bull" else " Bear"}")))
                        for (lvl in ote.levels) {
                            val c = when (lvl.label) {
                                "50" -> IntColor(AndroidColor.parseColor("#787B86"))
                                "100", "0" -> IntColor(AndroidColor.parseColor("#D94CD9"))
                                else -> IntColor(AndroidColor.parseColor("#4CAF50"))
                            }
                            otePriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(price = lvl.price, color = c, lineWidth = LineWidth.ONE, lineStyle = LineStyle.DOTTED, lineVisible = true, axisLabelVisible = true, title = "${lvl.label}% ${String.format("%.2f", lvl.price)}")))
                        }
                        for (ext in ote.extensions) {
                            otePriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(price = ext.price, color = IntColor(AndroidColor.parseColor("#F23645")), lineWidth = LineWidth.ONE, lineStyle = LineStyle.DOTTED, lineVisible = true, axisLabelVisible = true, title = "${ext.label} ${String.format("%.2f", ext.price)}")))
                        }
                    }
                }
                lastOteSig = sig
            }
        } else {
            clearOteRender(mainSeriesApi)
            if (!showOteVisibleChart) otePriceLineOwner = null
            lastOteSig = null
        }

        // Liquidity Delta Profiler [LuxAlgo] - Editors' picks overlay
        // BSL/SSL pivot zones: 4 delta-colored quadrants (BaselineSeries boxes) + decay % + reversal signals
        fun clearLdpRender(targetApi: SeriesApi?) {
            val api = targetApi ?: ldpPriceLineOwner ?: return
            ldpPriceLines.forEach { safelyRemovePriceLine(api, it) }
            ldpPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                ldpBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            ldpBoxSeries.clear()
        }
        if (showLiquidityDeltaProfiler && "LIQUIDITY_DELTA_PROFILER" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 30 && chartsViewApi != null) {
            val ldp = ldpPrecomputed
            val ownerChanged = ldpPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "LDP pass: bars=${ohlcData.size} bsl=${ldp?.bslZones?.size} ssl=${ldp?.sslZones?.size} lastSigNull=${lastLdpSig == null} ownerChanged=$ownerChanged")
            // Rebuild only when the computed layout actually changes (bars moved, zones/signals changed)
            val sig = if (ldp == null) "null" else (
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" + ldpSettings.showSwept + "|" + ldpSettings.showDecay +
                (ldp.bslZones + ldp.sslZones).joinToString(";") { z ->
                    val ds = z.deltas.joinToString(",") { String.format(java.util.Locale.US, "%.2f", it) }
                    "${if (z.isBsl) "b" else "s"}${z.leftIdx}${z.rightIdx}-${String.format(java.util.Locale.US, "%.5f", z.top)}-${String.format(java.util.Locale.US, "%.5f", z.bottom)}-${z.swept}-${z.healthPct}-${z.signalType}-${ds}"
                }
            )
            if (ownerChanged || lastLdpSig != sig || ldpBoxSeries.isEmpty() && ldpPriceLines.isEmpty()) {
                clearLdpRender(mainSeriesApi)
                ldpPriceLineOwner = mainSeriesApi
                ldpPendingSeries = 0
                if (ldp != null) {
                    fun LdpZoneBox(z: com.trading.app.indicators.LiquidityDeltaProfilerZone) {
                        if (z.swept && !ldpSettings.showSwept) return
                        if (z.leftIdx < 0 || z.leftIdx >= ohlcData.size) return
                        val leftT = ohlcData[z.leftIdx].time
                        val rightIdx = z.rightIdx.coerceIn(0, ohlcData.size - 1)
                        val rightT = ohlcData[rightIdx].time
                        val zoneColorInt = AndroidColor.parseColor(if (z.isBsl) ldpSettings.bslColorHex else ldpSettings.sslColorHex)
                        val step = (z.top - z.bottom) / 4f
                        var maxD = 0f
                        for (d in z.deltas) { val a = kotlin.math.abs(d); if (a > maxD) maxD = a }
                        val baseFillInt = if (z.isBsl) AndroidColor.parseColor(ldpSettings.bslColorHex) else AndroidColor.parseColor(ldpSettings.sslColorHex)
                        for (j in 0..3) {
                            val qBot = z.bottom + j * step
                            val qTop = z.bottom + (j + 1) * step
                            val d = z.deltas[j]
                            val fillColorInt = if (kotlin.math.abs(d) > 0.001f && maxD > 0f)
                                (if (d > 0f) AndroidColor.parseColor(ldpSettings.buyDeltaColorHex) else AndroidColor.parseColor(ldpSettings.sellDeltaColorHex))
                            else baseFillInt
                            // Pine transparency: delta-driven 100-(|d|/max)*60, default 60+((3-i)|i)*10
                            val opPct = if (z.swept) 10
                                else if (maxD > 0f && kotlin.math.abs(d) > 0.001f) (kotlin.math.abs(d) / maxD * 60f).toInt().coerceIn(0, 60)
                                else if (z.isBsl) 10 + j * 10
                                else 40 - j * 10
                            val lineColor = IntColor(applyOpacity(zoneColorInt, if (z.swept) 20 else 40))
                            val ldpChartApi = chartsViewApi?.api
                            if (ldpChartApi != null) {
                                ldpPendingSeries++
                                ldpChartApi.addBaselineSeries(
                                options = BaselineSeriesOptions(
                                    baseValue = com.trading.app.indicators.FloatPriceBaseValue(qBot.toDouble()),
                                    baseLineVisible = false,
                                    baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                    topLineColor = lineColor,
                                    topFillColor1 = IntColor(applyOpacity(fillColorInt, opPct)),
                                    topFillColor2 = IntColor(applyOpacity(fillColorInt, opPct)),
                                    bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                    bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                    bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = if (z.swept) LineStyle.DASHED else LineStyle.SOLID,
                                    priceLineVisible = false,
                                    lastValueVisible = false,
                                    crosshairMarkerVisible = false
                                ),
                                onSeriesCreated = { box ->
                                    box.setData(listOf(BaselineData(time = Time.Utc(leftT), value = qTop), BaselineData(time = Time.Utc(rightT), value = qTop)))
                                    ldpBoxSeries.add(box)
                                    ldpPendingSeries = (ldpPendingSeries - 1).coerceAtLeast(0)
                                }
                            )
                            }
                        }
                    }
                    for (z in ldp.bslZones + ldp.sslZones) LdpZoneBox(z)
                    for (z in ldp.bslZones + ldp.sslZones) {
                        if (z.swept && !ldpSettings.showSwept) continue
                        // Zone decay: dashed mid line labeled with remaining health %
                        if (ldpSettings.showDecay && !z.swept && z.healthPct >= 0) {
                            ldpPriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(
                                price = (z.top + z.bottom) / 2f,
                                color = IntColor(applyOpacity(AndroidColor.parseColor(if (z.isBsl) ldpSettings.bslColorHex else ldpSettings.sslColorHex), 60)),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.DOTTED,
                                lineVisible = true,
                                axisLabelVisible = true,
                                title = "${z.healthPct}%"
                            )))
                        }
                        // Reversal signal: dotted line labeled ABS/EXH/DIV/REJ
                        if (z.signaled) {
                            ldpPriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(
                                price = z.signalPrice,
                                color = IntColor(AndroidColor.parseColor(if (z.isBsl) "#f23645" else "#089981")),
                                lineWidth = LineWidth.TWO,
                                lineStyle = LineStyle.DOTTED,
                                lineVisible = true,
                                axisLabelVisible = true,
                                title = (if (z.isBsl) "BSL " else "SSL ") + z.signalType
                            )))
                        }
                    }
                }
                lastLdpSig = sig
                Log.d(LOG_TAG, "LDP drawn: boxes issued pending=$ldpPendingSeries priceLines=${ldpPriceLines.size}")
            }
        } else {
            clearLdpRender(mainSeriesApi)
            if (!showLiquidityDeltaProfiler) ldpPriceLineOwner = null
            lastLdpSig = null
        }

        // EQH/EQL Liquidity Zones [LuxAlgo] - Editors' picks overlay
        // Equal highs/lows boxes (BaselineSeries band) + dashed midline + cluster label lines + swept states
        fun clearEqhRender(targetApi: SeriesApi?) {
            val api = targetApi ?: eqhPriceLineOwner ?: return
            eqhPriceLines.forEach { safelyRemovePriceLine(api, it) }
            eqhPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                eqhBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            eqhBoxSeries.clear()
        }
        if (showEqhEqlLiquidityZones && "EQH_EQL_LIQUIDITY_ZONES" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 30 && chartsViewApi != null) {
            val eqh = eqhPrecomputed
            val ownerChanged = eqhPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "EQH pass: bars=${ohlcData.size} active=${eqh?.active?.size} swept=${eqh?.swept?.size} lastSigNull=${lastEqhSig == null} ownerChanged=$ownerChanged")
            val allZones = (eqh?.active.orEmpty() + eqh?.swept.orEmpty())
            val sig = (
                eqhEqlSettings.zoneTransp.toString() + "|" + eqhEqlSettings.bullColorHex + "|" + eqhEqlSettings.bearColorHex + "|" +
                eqhEqlSettings.showMidline + "|" + eqhEqlSettings.midlineColorHex + "|" + eqhEqlSettings.showVolume + "|" + eqhEqlSettings.showLabels + "|" + eqhEqlSettings.deleteOnSweep + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                allZones.joinToString(";") { z ->
                    "${if (z.isHigh) "h" else "l"}${z.leftIdx}${z.rightIdx}-${String.format(java.util.Locale.US, "%.5f", z.top)}-${String.format(java.util.Locale.US, "%.5f", z.bottom)}-${z.swept}-${z.clusterLabel}"
                }
            )
            if (ownerChanged || lastEqhSig != sig || eqhBoxSeries.isEmpty() && eqhPriceLines.isEmpty()) {
                clearEqhRender(mainSeriesApi)
                eqhPriceLineOwner = mainSeriesApi
                eqhPendingSeries = 0
                if (eqh != null) {
                    fun EqhZoneBox(z: com.trading.app.indicators.EqhEqlZone) {
                        if (z.leftIdx < 0 || z.leftIdx >= ohlcData.size) return
                        val leftT = ohlcData[z.leftIdx].time
                        val rightIdx = z.rightIdx.coerceIn(0, ohlcData.size - 1)
                        var rightT = ohlcData[rightIdx].time
                        // SD guards against duplicate timestamps (fatal assert in chart lib)
                        if (rightT <= leftT) {
                            val tfSec = when (currentTimeframe.value) {
                                "1m" -> 60L; "5m" -> 300L; "15m" -> 900L; "30m" -> 1800L
                                "1h" -> 3600L; "4h" -> 14400L; "1D" -> 86400L; "1W" -> 604800L
                                else -> 60L
                            }
                            rightT = leftT + tfSec
                        }
                        val zoneHex = if (z.swept) "#787b86" else if (z.isHigh) eqhEqlSettings.bearColorHex else eqhEqlSettings.bullColorHex
                        val zoneColorInt = AndroidColor.parseColor(zoneHex)
                        // Pine: active fill = color.new(zone, zoneTransp); swept fill = color.new(fg, 95)
                        // Use a 32% min fill so sub-pixel bands (0.03% of AUDUSD ~ 1px) read as shaded
                        // zones rather than bare lines; on dark OLED Polo's 15% is near-invisible.
                        val opPct = if (z.swept) 14 else maxOf(32, (100 - eqhEqlSettings.zoneTransp).coerceIn(0, 100))
                        val eqhChartApi = chartsViewApi?.api
                        if (eqhChartApi != null) {
                            val edgeColor = IntColor(applyOpacity(zoneColorInt, if (z.swept) 35 else 100))
                            val fillColor = IntColor(applyOpacity(zoneColorInt, opPct))
                            eqhPendingSeries++
                            eqhChartApi.addBaselineSeries(
                                options = BaselineSeriesOptions(
                                    baseValue = com.trading.app.indicators.FloatPriceBaseValue(z.bottom.toDouble()),
                                    baseLineVisible = false,
                                    baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                    topLineColor = edgeColor,
                                    topFillColor1 = fillColor,
                                    topFillColor2 = fillColor,
                                    bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                    bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                    bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                    lineWidth = LineWidth.TWO,
                                    lineStyle = LineStyle.SOLID,
                                    priceLineVisible = false,
                                    lastValueVisible = false,
                                    crosshairMarkerVisible = false
                                ),
                                onSeriesCreated = { box ->
                                    // Flat top line + fill down to base (bottom) — matches box.new(prev.idx, top, bar_index, bottom)
                                    box.setData(listOf(BaselineData(time = Time.Utc(leftT), value = z.top), BaselineData(time = Time.Utc(rightT), value = z.top)))
                                    eqhBoxSeries.add(box)
                                    eqhPendingSeries = (eqhPendingSeries - 1).coerceAtLeast(0)
                                }
                            )
                            // Bottom border: separate LineSeries is more reliable than baseline at base==value
                            eqhPendingSeries++
                            eqhChartApi.addLineSeries(
                                options = LineSeriesOptions(
                                    color = edgeColor,
                                    lineWidth = LineWidth.TWO,
                                    lineStyle = LineStyle.SOLID,
                                    priceLineVisible = false,
                                    lastValueVisible = false,
                                    crosshairMarkerVisible = false
                                ),
                                onSeriesCreated = { edge ->
                                    edge.setData(listOf(LineData(time = Time.Utc(leftT), value = z.bottom), LineData(time = Time.Utc(rightT), value = z.bottom)))
                                    eqhBoxSeries.add(edge)
                                    eqhPendingSeries = (eqhPendingSeries - 1).coerceAtLeast(0)
                                }
                            )
                        }
                    }
                    for (z in allZones) EqhZoneBox(z)
                    for (z in allZones) {
                        // Optional dashed midline at box mid - a short dashed line across the box, not a full priceLine label
                        if (eqhEqlSettings.showMidline && !z.swept) {
                            eqhPriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(
                                price = z.mid,
                                color = IntColor(AndroidColor.parseColor(eqhEqlSettings.midlineColorHex)),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.DASHED,
                                lineVisible = true,
                                axisLabelVisible = false,
                                title = ""
                            )))
                        }
                        // Pine boxes use a text label at box mid (label.style_label_left) - not a full-width price line.
                        // Use price scale axis label only (no horizontal line) so the zone reads as a shaded box.
                        // Hideable via the "Show Labels" setting.
                        if (z.clusterLabel.isNotEmpty() && eqhEqlSettings.showLabels) {
                            val labelHex = if (z.swept) "#868993" else if (z.isHigh) eqhEqlSettings.bearColorHex else eqhEqlSettings.bullColorHex
                            eqhPriceLines.add(mainSeriesApi.createPriceLine(PriceLineOptions(
                                price = z.mid,
                                color = IntColor(AndroidColor.parseColor(labelHex)),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                lineVisible = false,
                                axisLabelVisible = true,
                                title = z.clusterLabel
                            )))
                        }
                    }
                }
                lastEqhSig = sig
                Log.d(LOG_TAG, "EQH drawn: boxes issued pending=$eqhPendingSeries priceLines=${eqhPriceLines.size}")
            }
        } else {
            clearEqhRender(mainSeriesApi)
            if (!showEqhEqlLiquidityZones) eqhPriceLineOwner = null
            lastEqhSig = null
        }
        // ---------- Power Hour Breakout [LuxAlgo] (Editors' picks) ----------
        fun clearPhRender(targetApi: SeriesApi?) {
            val api = targetApi ?: phPriceLineOwner ?: return
            phPriceLines.forEach { safelyRemovePriceLine(api, it) }
            phPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                phBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            phBoxSeries.clear()
        }
        if (showPowerHourBreakout && "POWER_HOUR_BREAKOUT" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 30 && chartsViewApi != null) {
            val ph = phPrecomputed
            val ownerChanged = phPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "PH pass: bars=${ohlcData.size} frames=${ph?.frames?.size} breakouts=${ph?.breakouts?.size} lastSigNull=${lastPhSig == null} ownerChanged=$ownerChanged")
            val sig = (
                powerHourSettings.toJson() + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                (ph?.frames?.joinToString(";") { f ->
                    "${f.startTime}-${f.endTime}-${f.endSession}-${String.format(java.util.Locale.US, "%.5f", f.top)}-${String.format(java.util.Locale.US, "%.5f", f.bottom)}-${f.topExt ?: Float.NaN}-${f.bottomExt ?: Float.NaN}"
                }.orEmpty()) + "|" +
                (ph?.breakouts?.joinToString(";") { b -> "${b.time}-${b.bull}" }.orEmpty())
            )
            if (ownerChanged || lastPhSig != sig || phBoxSeries.isEmpty() && phPriceLines.isEmpty()) {
                clearPhRender(mainSeriesApi)
                phPriceLineOwner = mainSeriesApi
                phPendingSeries = 0
                if (ph != null) {
                    val chartApi = chartsViewApi?.api
                    if (chartApi != null) {
                        // Pine: box border_color = color.new(color.silver, 90), bgcolor = color.new(color.silver, 90)
                        // 90% transparent = 10% opacity
                        val silver10 = IntColor(applyOpacity(AndroidColor.parseColor("#c0c0c0"), 10))
                        val topColorInt = AndroidColor.parseColor(powerHourSettings.topColorHex)
                        val bottomColorInt = AndroidColor.parseColor(powerHourSettings.bottomColorHex)
                        val extOpacity = (100 - powerHourSettings.transparency).coerceIn(0, 95) // transparency 80 → 20% opacity
                        // Fibo lines default to SILVER_50 (50% opacity silver)
                        val fiboDefaultOpacity = 50
                        // Main-series markers (PH triangles, TBT arrows, Navigator wick dots) share one
                        // slot and are written once by the consolidated step after all overlay blocks.
                        fun PhBox(l: Long, r: Long, top: Float, bottom: Float) {
                            // Pine: box.new(startTime, top, endTime, bottom, border_color=silver10, bgcolor=silver10)
                            // BaselineSeries: base=bottom, value=top, topLineColor=silver10, topFillColor=silver10
                            phPendingSeries++
                            chartApi.addBaselineSeries(options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(bottom.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = silver10,
                                topFillColor1 = silver10,
                                topFillColor2 = silver10,
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ), onSeriesCreated = { s ->
                                s.setData(listOf(BaselineData(time = Time.Utc(l), value = top), BaselineData(time = Time.Utc(r), value = top)))
                                phBoxSeries.add(s); phPendingSeries = (phPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun PhLine(l: Long, r: Long, price: Float, color: Int, style: LineStyle) {
                            // Pine: line.new(startTime, level, endSession, level, color=color, width=1, style=style)
                            phPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(color), lineWidth = LineWidth.ONE, lineStyle = style, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = price), LineData(time = Time.Utc(r), value = price)))
                                phBoxSeries.add(s); phPendingSeries = (phPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun PhExtLine(l: Long, r: Long, price: Float, color: Int) {
                            // Extension line (solid, width 1) at extension level
                            phPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(color), lineWidth = LineWidth.ONE, lineStyle = LineStyle.SOLID, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = price), LineData(time = Time.Utc(r), value = price)))
                                phBoxSeries.add(s); phPendingSeries = (phPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun PhFill(l: Long, r: Long, level: Float, ext: Float, color: Int) {
                            // Pine: linefill.new(levelLine, extLine, color.new(color, transparency))
                            // BaselineSeries fill between level and ext, from l to r
                            phPendingSeries++
                            val lo = minOf(level, ext).toDouble()
                            val hi = maxOf(level, ext).toDouble()
                            val fillColor = IntColor(applyOpacity(color, extOpacity))
                            chartApi.addBaselineSeries(options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(lo),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = fillColor,
                                topFillColor2 = fillColor,
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ), onSeriesCreated = { s ->
                                s.setData(listOf(BaselineData(time = Time.Utc(l), value = hi.toFloat()), BaselineData(time = Time.Utc(r), value = hi.toFloat())))
                                phBoxSeries.add(s); phPendingSeries = (phPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun PhFiboLabel(price: Float, text: String, color: Int) {
                            runCatching {
                                val pl = mainSeriesApi.createPriceLine(PriceLineOptions(
                                    price = price,
                                    color = IntColor(color),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = LineStyle.DOTTED,
                                    axisLabelVisible = true,
                                    lineVisible = false,
                                    title = text
                                ))
                                phPriceLines.add(pl)
                            }
                        }
                        ph.frames.forEachIndexed { fi, fr ->
                            val leftT = fr.startTime
                            val endT = fr.endTime
                            val sessionT = fr.endSession
                            // Pine boxes/lines can have zero time width on timeframes with one inside bar.
                            // The chart library rejects duplicate timestamps, so skip zero-width shapes.
                            if (endT > leftT) {
                                // Box: startTime -> endTime (Pine uses endTime for box right edge)
                                PhBox(leftT, endT, fr.top, fr.bottom)
                            }
                            if (sessionT > leftT) {
                                // Top level line: startTime -> endSession
                                PhLine(leftT, sessionT, fr.top, topColorInt, LineStyle.SOLID)
                                // Top extension: line + fill
                                if (fr.topExt != null) {
                                    PhExtLine(leftT, sessionT, fr.topExt, topColorInt)
                                    PhFill(leftT, sessionT, fr.top, fr.topExt, topColorInt)
                                }
                                // Bottom level line: startTime -> endSession
                                PhLine(leftT, sessionT, fr.bottom, bottomColorInt, LineStyle.SOLID)
                                // Bottom extension: line + fill (note: bottom extension is below bottom)
                                if (fr.bottomExt != null) {
                                    PhExtLine(leftT, sessionT, fr.bottomExt, bottomColorInt)
                                    PhFill(leftT, sessionT, fr.bottom, fr.bottomExt, bottomColorInt)
                                }
                                // Fibonacci levels: startTime -> endSession, 50% opacity, style per input
                                fr.fibos.forEach { fib ->
                                    if (fib.display) {
                                        val fibColorInt = applyOpacity(AndroidColor.parseColor(fib.colorHex), fiboDefaultOpacity)
                                        PhLine(leftT, sessionT, fib.price, fibColorInt, when (fib.style) {
                                            "Dotted" -> LineStyle.DOTTED
                                            "Dashed" -> LineStyle.DASHED
                                            else -> LineStyle.SOLID
                                        })
                                        if (powerHourSettings.fibosLabels) {
                                            val txt = String.format(java.util.Locale.US, "%.3f (%.4f)", fib.level, fib.price)
                                            PhFiboLabel(fib.price, txt, fibColorInt)
                                        }
                                    }
                                }
                            } else if (powerHourSettings.fibosLabels) {
                                fr.fibos.forEach { fib ->
                                    if (fib.display) {
                                        val fibColorInt = applyOpacity(AndroidColor.parseColor(fib.colorHex), fiboDefaultOpacity)
                                        val txt = String.format(java.util.Locale.US, "%.3f (%.4f)", fib.level, fib.price)
                                        PhFiboLabel(fib.price, txt, fibColorInt)
                                    }
                                }
                            }
                        }
                        lastPhSig = sig
                        Log.d(LOG_TAG, "PH drawn: boxes issued pending=$phPendingSeries priceLines=${phPriceLines.size}")
                    }
                }
            }
        } else {
            clearPhRender(mainSeriesApi)
            if (!showPowerHourBreakout) phPriceLineOwner = null
            lastPhSig = null
        }
        // ---------- Trendline Breakouts With Targets [ChartPrime] ----------
        fun clearTbtRender(targetApi: SeriesApi?) {
            val api = targetApi ?: tbtPriceLineOwner ?: return
            tbtPriceLines.forEach { safelyRemovePriceLine(api, it) }
            tbtPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                tbtBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            tbtBoxSeries.clear()
        }
        if (showTrendlineBreakouts && "TRENDLINE_BREAKOUTS" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 60 && chartsViewApi != null) {
            val tbt = tbtPrecomputed
            val ownerChanged = tbtPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "TBT pass: bars=${ohlcData.size} segments=${tbt?.segments?.size} signals=${tbt?.signals?.size} targets=${tbt?.targets?.size} lastSigNull=${lastTbtSig == null} ownerChanged=$ownerChanged")
            val sig = (
                trendlineSettings.toJson() + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                (tbt?.segments?.joinToString(";") { s ->
                    "${s.startTime}-${s.endTime}-${String.format(java.util.Locale.US, "%.5f", s.startPrice)}-${String.format(java.util.Locale.US, "%.5f", s.endPrice)}-${String.format(java.util.Locale.US, "%.5f", s.band)}-${s.support}"
                }.orEmpty()) + "|" +
                (tbt?.signals?.joinToString(";") { m -> "${m.time}-${m.bull}" }.orEmpty()) + "|" +
                (tbt?.targets?.joinToString(";") { t ->
                    "${t.entryTime}-${t.exitTime}-${String.format(java.util.Locale.US, "%.5f", t.tp)}-${t.won}-${t.active}-${t.bull}"
                }.orEmpty())
            )
            if (ownerChanged || lastTbtSig != sig || tbtBoxSeries.isEmpty() && tbtPriceLines.isEmpty()) {
                clearTbtRender(mainSeriesApi)
                tbtPriceLineOwner = mainSeriesApi
                tbtPendingSeries = 0
                if (tbt != null) {
                    val chartApi = chartsViewApi?.api
                    if (chartApi != null) {
                        // Main-series markers are written once by the consolidated step below.
                        fun TbtLine(l: Long, r: Long, p1: Float, p2: Float, color: Int, width: LineWidth, style: LineStyle) {
                            if (r <= l || !p1.isFinite() || !p2.isFinite()) return
                            tbtPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(color), lineWidth = width, lineStyle = style, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = p1), LineData(time = Time.Utc(r), value = p2)))
                                tbtBoxSeries.add(s); tbtPendingSeries = (tbtPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun TbtTargetLabel(price: Float, text: String, color: Int) {
                            if (!price.isFinite()) return
                            runCatching {
                                val pl = mainSeriesApi.createPriceLine(PriceLineOptions(
                                    price = price,
                                    color = IntColor(color),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = LineStyle.DOTTED,
                                    axisLabelVisible = true,
                                    lineVisible = false,
                                    title = text
                                ))
                                tbtPriceLines.add(pl)
                            }
                        }
                        val grayBand = applyOpacity(AndroidColor.parseColor(trendlineSettings.lineCol1Hex), 81)
                        val resistanceEdge = applyOpacity(AndroidColor.parseColor("#0b8b07"), 47)
                        val supportEdge = applyOpacity(AndroidColor.parseColor("#d42e00"), 46)
                        val targetLine = AndroidColor.parseColor("#9a6714")
                        val targetWin = applyOpacity(AndroidColor.parseColor("#06800a"), 63)
                        val targetLoss = applyOpacity(AndroidColor.parseColor("#f60707"), 30)
                        // Pine draws three parallel lines per trendline; fills are approximated with edges
                        // because the chart library has no polygon linefill primitive.
                        tbt.segments.forEach { sg ->
                            if (sg.endTime > sg.startTime && sg.startPrice.isFinite() && sg.endPrice.isFinite() && sg.band.isFinite() && sg.band > 0f) {
                                val edge = if (sg.support) supportEdge else resistanceEdge
                                TbtLine(sg.startTime, sg.endTime, sg.startPrice, sg.endPrice, edge, LineWidth.TWO, LineStyle.SOLID)
                                TbtLine(sg.startTime, sg.endTime, sg.startPrice - sg.band, sg.endPrice - sg.band, grayBand, LineWidth.ONE, LineStyle.SOLID)
                                TbtLine(sg.startTime, sg.endTime, sg.startPrice - sg.band * 2f, sg.endPrice - sg.band * 2f, grayBand, LineWidth.ONE, LineStyle.SOLID)
                            }
                        }
                        if (trendlineSettings.showTargets) {
                            tbt.targets.forEach { tg ->
                                if (tg.exitTime >= tg.entryTime && tg.tp.isFinite()) {
                                    TbtLine(tg.entryTime, tg.exitTime, tg.tp, tg.tp, targetLine, LineWidth.ONE, LineStyle.DASHED)
                                    val labelColor = if (tg.active) targetLine else if (tg.won) targetWin else targetLoss
                                    TbtTargetLabel(tg.tp, "Target", labelColor)
                                }
                            }
                        }
                        lastTbtSig = sig
                        Log.d(LOG_TAG, "TBT drawn: lines issued pending=$tbtPendingSeries priceLines=${tbtPriceLines.size}")
                    }
                }
            }
        } else {
            clearTbtRender(mainSeriesApi)
            if (!showTrendlineBreakouts) tbtPriceLineOwner = null
            lastTbtSig = null
        }
        // ---------- Trendline Breakout Navigator [LuxAlgo] ----------
        fun clearTnavRender(targetApi: SeriesApi?) {
            val api = targetApi ?: tnavPriceLineOwner ?: return
            tnavPriceLines.forEach { safelyRemovePriceLine(api, it) }
            tnavPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                tnavBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            tnavBoxSeries.clear()
        }
        if (showTrendlineNavigator && "TRENDLINE_NAVIGATOR" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 80 && chartsViewApi != null) {
            val tnav = tnavPrecomputed
            val ownerChanged = tnavPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "TNAV pass: bars=${ohlcData.size} segments=${tnav?.segments?.size} dots=${tnav?.dots?.size} tags=${tnav?.tags?.size} lastSigNull=${lastTnavSig == null} ownerChanged=$ownerChanged")
            val sig = (
                navigatorSettings.toJson() + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                (tnav?.segments?.joinToString(";") { s ->
                    "${s.startTime}-${s.endTime}-${String.format(java.util.Locale.US, "%.5f", s.startPrice)}-${String.format(java.util.Locale.US, "%.5f", s.endPrice)}-${s.pos}-${s.bull}"
                }.orEmpty()) + "|" +
                (tnav?.dots?.joinToString(";") { d -> "${d.time}-${d.bull}" }.orEmpty()) + "|" +
                (tnav?.tags?.joinToString(";") { t -> "${t.time}-${String.format(java.util.Locale.US, "%.5f", t.price)}-${t.text}" }.orEmpty())
            )
            if (ownerChanged || lastTnavSig != sig || tnavBoxSeries.isEmpty() && tnavPriceLines.isEmpty()) {
                clearTnavRender(mainSeriesApi)
                tnavPriceLineOwner = mainSeriesApi
                tnavPendingSeries = 0
                if (tnav != null) {
                    val chartApi = chartsViewApi?.api
                    if (chartApi != null) {
                        fun TnavLine(l: Long, r: Long, p1: Float, p2: Float, color: Int, width: LineWidth, style: LineStyle) {
                            if (r <= l || !p1.isFinite() || !p2.isFinite()) return
                            tnavPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(color), lineWidth = width, lineStyle = style, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = p1), LineData(time = Time.Utc(r), value = p2)))
                                tnavBoxSeries.add(s); tnavPendingSeries = (tnavPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun TnavTag(price: Float, text: String) {
                            if (!price.isFinite()) return
                            runCatching {
                                val pl = mainSeriesApi.createPriceLine(PriceLineOptions(
                                    price = price,
                                    color = IntColor(AndroidColor.parseColor("#D1D4DC")),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = LineStyle.DOTTED,
                                    axisLabelVisible = true,
                                    lineVisible = false,
                                    title = text
                                ))
                                tnavPriceLines.add(pl)
                            }
                        }
                        val bullLine = AndroidColor.parseColor(navigatorSettings.bullColorHex)
                        val bearLine = AndroidColor.parseColor(navigatorSettings.bearColorHex)
                        tnav.segments.forEach { sg ->
                            val style = when (sg.pos) {
                                2 -> LineStyle.DASHED
                                3 -> LineStyle.DOTTED
                                else -> LineStyle.SOLID
                            }
                            val width = if (sg.pos == 1) LineWidth.TWO else LineWidth.ONE
                            TnavLine(sg.startTime, sg.endTime, sg.startPrice, sg.endPrice, if (sg.bull) bullLine else bearLine, width, style)
                        }
                        tnav.tags.forEach { tg -> TnavTag(tg.price, tg.text) }
                        lastTnavSig = sig
                        Log.d(LOG_TAG, "TNAV drawn: lines issued pending=$tnavPendingSeries priceLines=${tnavPriceLines.size}")
                    }
                }
            }
        } else {
            clearTnavRender(mainSeriesApi)
            if (!showTrendlineNavigator) tnavPriceLineOwner = null
            lastTnavSig = null
        }
        // ---------- Liquidity Pools [LuxAlgo] ----------
        fun clearLpRender(targetApi: SeriesApi?) {
            val api = targetApi ?: lpPriceLineOwner ?: return
            lpPriceLines.forEach { safelyRemovePriceLine(api, it) }
            lpPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                lpBoxSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            lpBoxSeries.clear()
        }
        if (showLiquidityPools && "LIQUIDITY_POOLS" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 15 && chartsViewApi != null) {
            val lp = lpPrecomputed
            val ownerChanged = lpPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "LP pass: bars=${ohlcData.size} zones=${lp?.zones?.size} tags=${lp?.tags?.size} lastSigNull=${lastLpSig == null} ownerChanged=$ownerChanged")
            val sig = (
                liquidityPoolsSettings.toJson() + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                (lp?.zones?.joinToString(";") { z ->
                    "${z.leftTime}-${z.boxRightTime}-${String.format(java.util.Locale.US, "%.5f", z.top)}-${String.format(java.util.Locale.US, "%.5f", z.bottom)}-${z.bull}-${z.vol}-${z.lineStartTime}-${z.lineEndTime}-${z.showLine}"
                }.orEmpty()) + "|" +
                (lp?.tags?.joinToString(";") { t -> "${t.time}-${String.format(java.util.Locale.US, "%.5f", t.price)}-${t.text}-${t.bull}" }.orEmpty())
            )
            if (ownerChanged || lastLpSig != sig || lpBoxSeries.isEmpty() && lpPriceLines.isEmpty()) {
                clearLpRender(mainSeriesApi)
                lpPriceLineOwner = mainSeriesApi
                lpPendingSeries = 0
                if (lp != null) {
                    val chartApi = chartsViewApi?.api
                    if (chartApi != null) {
                        fun LpZoneBox(z: com.trading.app.indicators.LpZone) {
                            if (z.boxRightTime <= z.leftTime || z.top <= z.bottom || !z.top.isFinite() || !z.bottom.isFinite()) return
                            val zoneColorInt = AndroidColor.parseColor(if (z.bull) liquidityPoolsSettings.bullColorHex else liquidityPoolsSettings.bearColorHex)
                            // Pine: box.new(..., bgcolor = color.new(zone, 80), border_color = na) -> fill-only at 20%
                            val fill = IntColor(applyOpacity(zoneColorInt, 20))
                            lpPendingSeries++
                            chartApi.addBaselineSeries(options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(z.bottom.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = fill,
                                topFillColor2 = fill,
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ), onSeriesCreated = { box ->
                                box.setData(listOf(BaselineData(time = Time.Utc(z.leftTime), value = z.top), BaselineData(time = Time.Utc(z.boxRightTime), value = z.top)))
                                lpBoxSeries.add(box); lpPendingSeries = (lpPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun LpLevelLine(l: Long, r: Long, price: Float, color: Int) {
                            if (r <= l || !price.isFinite()) return
                            lpPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(color), lineWidth = LineWidth.ONE, lineStyle = LineStyle.SOLID, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = price), LineData(time = Time.Utc(r), value = price)))
                                lpBoxSeries.add(s); lpPendingSeries = (lpPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        val bullZone = AndroidColor.parseColor(liquidityPoolsSettings.bullColorHex)
                        val bearZone = AndroidColor.parseColor(liquidityPoolsSettings.bearColorHex)
                        lp.zones.forEach { z ->
                            LpZoneBox(z)
                            // Level line (bull: body bottom lst.b, bear: body top hst.t) extends while newest
                            if (z.showLine) {
                                LpLevelLine(z.lineStartTime, z.lineEndTime, z.level, if (z.bull) bullZone else bearZone)
                            }
                        }
                        if (liquidityPoolsSettings.volTog) {
                            lp.tags.takeLast(60).forEach { tg ->
                                if (tg.price.isFinite() && tg.text.isNotBlank()) {
                                    runCatching {
                                        val pl = mainSeriesApi.createPriceLine(PriceLineOptions(
                                            price = tg.price,
                                            color = IntColor(if (tg.bull) bullZone else bearZone),
                                            lineWidth = LineWidth.ONE,
                                            lineStyle = LineStyle.DOTTED,
                                            axisLabelVisible = true,
                                            lineVisible = false,
                                            title = tg.text
                                        ))
                                        lpPriceLines.add(pl)
                                    }
                                }
                            }
                        }
                        lastLpSig = sig
                        Log.d(LOG_TAG, "LP drawn: series issued pending=$lpPendingSeries priceLines=${lpPriceLines.size}")
                    }
                }
            }
        } else {
            clearLpRender(mainSeriesApi)
            if (!showLiquidityPools) lpPriceLineOwner = null
            lastLpSig = null
        }
        // ---------- Pure Price Action Order & Breaker Blocks [LuxAlgo] ----------
        fun clearObbRender(targetApi: SeriesApi?) {
            val api = targetApi ?: obbPriceLineOwner ?: return
            obbPriceLines.forEach { safelyRemovePriceLine(api, it) }
            obbPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                obbSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            obbSeries.clear()
        }
        if (showOrderBlockBreaker && "ORDER_BLOCK_BREAKER" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 15 && chartsViewApi != null) {
            val obb = obbPrecomputed
            val ownerChanged = obbPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "OBB pass: bars=${ohlcData.size} bull=${obb?.bull?.size} bear=${obb?.bear?.size} labels=${obb?.labels?.size} ownerChanged=$ownerChanged")
            val sig = (
                obbSettings.toJson() + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                (obb?.bull?.joinToString(";") { d ->
                    "${d.locTime}-${d.rightTime}-${d.breaker}-${String.format(java.util.Locale.US, "%.5f", d.top)}-${String.format(java.util.Locale.US, "%.5f", d.btm)}"
                }.orEmpty()) + "|" +
                (obb?.bear?.joinToString(";") { d ->
                    "${d.locTime}-${d.rightTime}-${d.breaker}-${String.format(java.util.Locale.US, "%.5f", d.top)}-${String.format(java.util.Locale.US, "%.5f", d.btm)}"
                }.orEmpty()) + "|" +
                (obb?.labels?.joinToString(";") { t -> "${t.time}-${String.format(java.util.Locale.US, "%.5f", t.price)}-${t.down}-${t.hex}" }.orEmpty())
            )
            if (ownerChanged || lastObbSig != sig) {
                clearObbRender(mainSeriesApi)
                obbPriceLineOwner = mainSeriesApi
                obbPendingSeries = 0
                if (obb != null) {
                    val chartApi = chartsViewApi?.api
                    if (chartApi != null) {
                        fun ObbBox(d: com.trading.app.indicators.ObDisplay) {
                            if (d.rightTime <= d.locTime || d.top <= d.btm || !d.top.isFinite() || !d.btm.isFinite()) return
                            val zoneColor = AndroidColor.parseColor(if (d.bull) obbSettings.bullCssHex else obbSettings.bearCssHex)
                            // Pine: box.new(..., bgcolor = color.new(css, 80), border_color = na) -> fill-only at 20%
                            val fill = IntColor(applyOpacity(zoneColor, 20))
                            obbPendingSeries++
                            chartApi.addBaselineSeries(options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(d.btm.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = fill,
                                topFillColor2 = fill,
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ), onSeriesCreated = { box ->
                                box.setData(listOf(BaselineData(time = Time.Utc(d.locTime), value = d.top), BaselineData(time = Time.Utc(d.rightTime), value = d.top)))
                                obbSeries.add(box); obbPendingSeries = (obbPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun ObbLine(l: Long, r: Long, price: Float, color: Int, dotted: Boolean) {
                            if (r <= l || !price.isFinite()) return
                            obbPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(color), lineWidth = LineWidth.ONE, lineStyle = if (dotted) LineStyle.DOTTED else LineStyle.SOLID, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = price), LineData(time = Time.Utc(r), value = price)))
                                obbSeries.add(s); obbPendingSeries = (obbPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        val bullCss = AndroidColor.parseColor(obbSettings.bullCssHex)
                        val bullBreakCss = AndroidColor.parseColor(obbSettings.bullBreakCssHex)
                        val bearCss = AndroidColor.parseColor(obbSettings.bearCssHex)
                        val bearBreakCss = AndroidColor.parseColor(obbSettings.bearBreakCssHex)
                        val lastT = ohlcData.last().time
                        obb.bull.forEach { d ->
                            ObbBox(d)
                            val css = if (d.breaker) bullBreakCss else bullCss
                            val opacity = if (d.breaker) 60 else 20
                            ObbLine(if (d.breaker) d.rightTime else d.locTime, lastT, d.top, applyOpacity(css, opacity), false)
                            ObbLine(if (d.breaker) d.rightTime else d.locTime, lastT, d.btm, applyOpacity(css, opacity), false)
                            ObbLine(d.locTime, lastT, (d.top + d.btm) / 2f, bullCss, true)
                        }
                        obb.bear.forEach { d ->
                            ObbBox(d)
                            val css = if (d.breaker) bearBreakCss else bearCss
                            val opacity = if (d.breaker) 60 else 20
                            ObbLine(if (d.breaker) d.rightTime else d.locTime, lastT, d.top, applyOpacity(css, opacity), false)
                            ObbLine(if (d.breaker) d.rightTime else d.locTime, lastT, d.btm, applyOpacity(css, opacity), false)
                            ObbLine(d.locTime, lastT, (d.top + d.btm) / 2f, bearCss, true)
                        }
                        if (obbSettings.showLabels) {
                            obb.labels.forEach { lb ->
                                if (lb.price.isFinite()) {
                                    runCatching {
                                        val pl = mainSeriesApi.createPriceLine(PriceLineOptions(
                                            price = lb.price,
                                            color = IntColor(AndroidColor.parseColor(lb.hex)),
                                            lineWidth = LineWidth.ONE,
                                            lineStyle = LineStyle.DOTTED,
                                            axisLabelVisible = true,
                                            lineVisible = false,
                                            title = if (lb.down) "▼" else "▲"
                                        ))
                                        obbPriceLines.add(pl)
                                    }
                                }
                            }
                        }
                        lastObbSig = sig
                        Log.d(LOG_TAG, "OBB drawn: series issued pending=$obbPendingSeries priceLines=${obbPriceLines.size}")
                    }
                }
            }
        } else {
            clearObbRender(mainSeriesApi)
            if (!showOrderBlockBreaker) obbPriceLineOwner = null
            lastObbSig = null
        }
        // ---------- Volumatic Fair Value Gaps [BigBeluga] ----------
        fun clearVfvgRender(targetApi: SeriesApi?) {
            val api = targetApi ?: vfvgPriceLineOwner ?: return
            vfvgPriceLines.forEach { safelyRemovePriceLine(api, it) }
            vfvgPriceLines.clear()
            val chartApi = chartsViewApi?.api
            if (chartApi != null) {
                vfvgSeries.forEach { runCatching { chartApi.removeSeries(it) {} } }
            }
            vfvgSeries.clear()
        }
        if (showVolumaticFvg && "VOLUMATIC_FVG" !in hiddenIndicators && mainSeriesApi != null && ohlcData.size >= 15 && chartsViewApi != null) {
            val vfvg = vfvgPrecomputed
            val ownerChanged = vfvgPriceLineOwner != mainSeriesApi
            Log.d(LOG_TAG, "VFVG pass: bars=${ohlcData.size} items=${vfvg?.items?.size} bull=${vfvg?.bullCount} bear=${vfvg?.bearCount} ownerChanged=$ownerChanged")
            val sig = (
                volumaticFvgSettings.toJson() + "|" +
                ohlcData.size.toString() + "|" + ohlcData.last().time + "|" +
                (vfvg?.items?.joinToString(";") { d ->
                    "${d.leftTime}-${d.rightTime}-${d.isBull}-${String.format(java.util.Locale.US, "%.5f", d.top)}-${String.format(java.util.Locale.US, "%.5f", d.bottom)}-${d.bullPct}-${d.bearPct}-${d.bullExtSec}-${d.bearExtSec}"
                }.orEmpty())
            )
            if (ownerChanged || lastVfvgSig != sig) {
                clearVfvgRender(mainSeriesApi)
                vfvgPriceLineOwner = mainSeriesApi
                vfvgPendingSeries = 0
                if (vfvg != null) {
                    val chartApi = chartsViewApi?.api
                    if (chartApi != null) {
                        fun VfvgFillBox(l: Long, r: Long, topPrice: Float, bottomPrice: Float, colorInt: Int, opacity: Int) {
                            if (r <= l || topPrice <= bottomPrice || !topPrice.isFinite() || !bottomPrice.isFinite()) return
                            val fill = IntColor(applyOpacity(colorInt, opacity))
                            vfvgPendingSeries++
                            chartApi.addBaselineSeries(options = BaselineSeriesOptions(
                                baseValue = com.trading.app.indicators.FloatPriceBaseValue(bottomPrice.toDouble()),
                                baseLineVisible = false,
                                baseLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topLineColor = IntColor(AndroidColor.TRANSPARENT),
                                topFillColor1 = fill,
                                topFillColor2 = fill,
                                bottomLineColor = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor1 = IntColor(AndroidColor.TRANSPARENT),
                                bottomFillColor2 = IntColor(AndroidColor.TRANSPARENT),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.SOLID,
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ), onSeriesCreated = { box ->
                                box.setData(listOf(BaselineData(time = Time.Utc(l), value = topPrice), BaselineData(time = Time.Utc(r), value = topPrice)))
                                vfvgSeries.add(box); vfvgPendingSeries = (vfvgPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        val bullCss = AndroidColor.parseColor(volumaticFvgSettings.bullColorHex)
                        val bearCss = AndroidColor.parseColor(volumaticFvgSettings.bearColorHex)
                        // Pine boxes use border_color = chart.bg_color -> dark outline around the split bars
                        val vfvgBorder = chartBgColor
                        fun VfvgEdgeLine(l: Long, r: Long, price: Float) {
                            if (r <= l || !price.isFinite()) return
                            vfvgPendingSeries++
                            chartApi.addLineSeries(options = LineSeriesOptions(color = IntColor(vfvgBorder), lineWidth = LineWidth.ONE, lineStyle = LineStyle.SOLID, priceLineVisible = false, lastValueVisible = false, crosshairMarkerVisible = false), onSeriesCreated = { s ->
                                s.setData(listOf(LineData(time = Time.Utc(l), value = price), LineData(time = Time.Utc(r), value = price)))
                                vfvgSeries.add(s); vfvgPendingSeries = (vfvgPendingSeries - 1).coerceAtLeast(0)
                            })
                        }
                        fun VfvgTag(price: Float, text: String, tagColor: Int) {
                            if (!price.isFinite() || text.isBlank()) return
                            runCatching {
                                val pl = mainSeriesApi.createPriceLine(PriceLineOptions(
                                    price = price,
                                    color = IntColor(tagColor),
                                    lineWidth = LineWidth.ONE,
                                    lineStyle = LineStyle.DOTTED,
                                    axisLabelVisible = true,
                                    lineVisible = false,
                                    title = text
                                ))
                                vfvgPriceLines.add(pl)
                            }
                        }
                        vfvg.items.forEach { d ->
                            val colorInt = if (d.isBull) bullCss else bearCss
                            // Pine: body bgcolor = color.new(css, 70) -> faint full-width band
                            VfvgFillBox(d.leftTime, d.rightTime, d.top, d.bottom, colorInt, 30)
                            // Volume split bars: SOLID bear half [top..mid] on top, SOLID bull half
                            // [mid..bottom] below, widths = share of the previous bar's up/down volume
                            if (volumaticFvgSettings.volumeBars) {
                                val mid = (d.top + d.bottom) / 2f
                                if (d.bearExtSec > 0L) {
                                    val bearR = d.leftTime + d.bearExtSec
                                    VfvgFillBox(d.leftTime, bearR, d.top, mid, bearCss, 85)
                                    VfvgEdgeLine(d.leftTime, bearR, d.top)
                                    VfvgEdgeLine(d.leftTime, bearR, mid)
                                    VfvgTag((d.top + mid) / 2f, "${d.bearPct}%", bearCss)
                                }
                                if (d.bullExtSec > 0L) {
                                    val bullR = d.leftTime + d.bullExtSec
                                    VfvgFillBox(d.leftTime, bullR, mid, d.bottom, bullCss, 85)
                                    VfvgEdgeLine(d.leftTime, bullR, mid)
                                    VfvgEdgeLine(d.leftTime, bullR, d.bottom)
                                    VfvgTag((mid + d.bottom) / 2f, "${d.bullPct}%", bullCss)
                                }
                            }
                        }
                        vfvgBullCount = vfvg.bullCount
                        vfvgBearCount = vfvg.bearCount
                        lastVfvgSig = sig
                        Log.d(LOG_TAG, "VFVG drawn: series issued pending=$vfvgPendingSeries priceLines=${vfvgPriceLines.size}")
                    }
                }
            }
        } else {
            clearVfvgRender(mainSeriesApi)
            if (!showVolumaticFvg) vfvgPriceLineOwner = null
            lastVfvgSig = null
        }
        // Consolidated overlay markers: PH triangles + TBT arrows + Navigator wick dots
        // share the main-series marker slot, so write the combined set once here.
        runCatching {
            val markers = mutableListOf<SeriesMarker>()
            var anyOverlay = false
            if (showPowerHourBreakout && "POWER_HOUR_BREAKOUT" !in hiddenIndicators && mainSeriesApi != null) {
                anyOverlay = true
                val php = phPrecomputed
                if (powerHourSettings.showBreakouts && php != null) {
                    php.breakouts.forEach { bk ->
                        markers.add(
                            SeriesMarker(
                                time = Time.Utc(bk.time),
                                position = if (bk.bull) SeriesMarkerPosition.BELOW_BAR else SeriesMarkerPosition.ABOVE_BAR,
                                shape = if (bk.bull) SeriesMarkerShape.ARROW_UP else SeriesMarkerShape.ARROW_DOWN,
                                size = 2,
                                color = IntColor(AndroidColor.parseColor(if (bk.bull) powerHourSettings.bullBreakColorHex else powerHourSettings.bearBreakColorHex))
                            )
                        )
                    }
                }
            }
            if (showTrendlineBreakouts && "TRENDLINE_BREAKOUTS" !in hiddenIndicators && mainSeriesApi != null) {
                anyOverlay = true
                tbtPrecomputed?.signals?.forEach { sg ->
                    markers.add(
                        SeriesMarker(
                            time = Time.Utc(sg.time),
                            position = if (sg.bull) SeriesMarkerPosition.BELOW_BAR else SeriesMarkerPosition.ABOVE_BAR,
                            shape = if (sg.bull) SeriesMarkerShape.ARROW_UP else SeriesMarkerShape.ARROW_DOWN,
                            size = 2,
                            color = IntColor(applyOpacity(AndroidColor.parseColor(if (sg.bull) "#2ec006" else "#f10202"), 89))
                        )
                    )
                }
            }
            if (showTrendlineNavigator && "TRENDLINE_NAVIGATOR" !in hiddenIndicators && mainSeriesApi != null) {
                anyOverlay = true
                val tnp = tnavPrecomputed
                if (tnp != null) {
                    val wickBull = AndroidColor.parseColor(navigatorSettings.wickBullColorHex)
                    val wickBear = AndroidColor.parseColor(navigatorSettings.wickBearColorHex)
                    tnp.dots.forEach { d ->
                        markers.add(
                            SeriesMarker(
                                time = Time.Utc(d.time),
                                position = if (d.bull) SeriesMarkerPosition.BELOW_BAR else SeriesMarkerPosition.ABOVE_BAR,
                                shape = SeriesMarkerShape.CIRCLE,
                                size = 2,
                                color = IntColor(if (d.bull) wickBull else wickBear)
                            )
                        )
                    }
                }
            }
            if (anyOverlay) {
                val msig = markers.joinToString(";") {
                    val ts = when (val t = it.time) {
                        is Time.Utc -> t.timestamp
                        else -> 0L
                    }
                    "$ts-${it.position}-${it.shape}-${it.color}"
                }
                if (lastOverlayMarkersSig != msig) {
                    mainSeriesApi?.setMarkers(markers.sortedBy {
                        when (val t = it.time) {
                            is Time.Utc -> t.timestamp
                            else -> 0L
                        }
                    })
                    lastOverlayMarkersSig = msig
                }
            } else {
                lastOverlayMarkersSig = null
            }
        }
        }.onFailure { android.util.Log.w("TradingChart", "render pass skipped: " + it.message); chartBusy = false }
        // Hold ticks until async overlay series creations (onSeriesCreated) land - a tick
        // update() racing them throws uncatchably in the JS bridge. Bounded wait.
        var ldpWaits = 0
        while ((ldpPendingSeries > 0 || eqhPendingSeries > 0 || phPendingSeries > 0 || tbtPendingSeries > 0 || tnavPendingSeries > 0 || lpPendingSeries > 0 || obbPendingSeries > 0 || vfvgPendingSeries > 0) && ldpWaits < 200) {
            delay(50)
            ldpWaits++
        }
        chartBusy = false
    }

    // Fair Value Gap zone-entry alerts
    LaunchedEffect(currentQuoteState, fvgDashboardData, fvgSettings.alertsEnabled, showFairValueGap) {
        if (!showFairValueGap || !fvgSettings.alertsEnabled) return@LaunchedEffect
        val data = fvgDashboardData ?: return@LaunchedEffect
        val price = currentQuoteState?.lastPrice ?: return@LaunchedEffect
        val now = System.currentTimeMillis()
        for (fvg in data.fvgs) {
            val inside = price in fvg.min..fvg.max
            val was = fvgPrevInside[fvg.time] ?: false
            if (inside && !was && now - (fvgLastAlertMs[fvg.time] ?: 0L) > 30_000L) {
                fvgLastAlertMs[fvg.time] = now
                val direction = if (fvg.isBull) "Bullish" else "Bearish"
                com.asc.markets.notifications.NotificationHelper.showAlert(
                    context,
                    "FVG Retest",
                    "$direction FVG retest on $symbol",
                    type = "fvg_retest",
                    symbol = symbol
                )
            }
            fvgPrevInside[fvg.time] = inside
        }
    }

    // VigilanceNodeEngine: evaluate user-created zone alerts against live price
    LaunchedEffect(currentQuoteState?.lastPrice, symbol) {
        val price = currentQuoteState?.lastPrice?.toDouble() ?: return@LaunchedEffect
        val triggered = com.asc.markets.logic.VigilanceNodeEngine.evaluateAllZoneNodes(symbol, price)
        for (node in triggered) {
            com.asc.markets.notifications.NotificationHelper.showAlert(
                context,
                "${node.zoneType} Zone Entry",
                "${node.zoneType} zone touched on $symbol (${String.format("%.5f", node.zoneBottom)}-${String.format("%.5f", node.zoneTop)})",
                type = "zone_${node.zoneType.lowercase()}",
                symbol = symbol
            )
        }
    }

    // UserAlert triggers: crossing price-lines and SMC zone touches
    val smcSnapshot = remember(ohlcData, timeframe, userAlerts) {
        // Only compute engines for zones some active SMC alert actually selected
        // (and only for this chart's symbol - foreign alerts are never evaluated here)
        val needed = userAlerts
            .filter { it.condition == "SMC" && it.isActive && it.symbol.equals(symbol, ignoreCase = true) }
            .flatMap { it.smcZones }.toSet()
        if (needed.isNotEmpty()) {
            com.trading.app.indicators.SmcZoneAlerts.snapshot(ohlcData, timeframeToSeconds(timeframe), needed)
        } else null
    }
    val prevPriceForAlert = remember { mutableStateOf<Float?>(null) }
    // Symbol switch: drop the previous tick so the first tick on the new symbol
    // can never look like a cross against the old symbol's price
    LaunchedEffect(symbol) { prevPriceForAlert.value = null }
    LaunchedEffect(currentQuoteState?.lastPrice) {
        val quote = currentQuoteState ?: return@LaunchedEffect
        val price = quote.lastPrice
        val now = System.currentTimeMillis()
        // Weekend/stale feed guard: never evaluate alerts on a provably-stale quote
        if (quote.time > 0L && now - quote.time > 60_000L) return@LaunchedEffect
        val prev = prevPriceForAlert.value
        if (prev != null) {
            for (alert in userAlerts) {
                if (!alert.isActive) continue
                // An alert belongs to its own symbol: alerts for other assets must never
                // be evaluated (or burned) by this chart's price/zones
                if (!alert.symbol.equals(symbol, ignoreCase = true)) continue
                // Throttle "Every time" to once per minute
                if (alert.triggerMode == "Every time" && alert.lastTriggeredAt != null && now - alert.lastTriggeredAt < 60_000L) continue

                val fire: Boolean
                val fireTitle: String
                val fireBody: String
                if (alert.condition == "SMC") {
                    val snap = smcSnapshot
                    if (snap == null) {
                        fire = false; fireTitle = ""; fireBody = ""
                    } else {
                        val touched = com.trading.app.indicators.SmcZoneAlerts.touchedZones(snap, price)
                        val required = if (alert.smcMin in 1..alert.smcZones.size) alert.smcMin else alert.smcZones.size.coerceAtLeast(1)
                        val matched = alert.smcZones.count { it in touched }
                        fire = matched >= required
                        fireTitle = "SMC alert: ${alert.symbol}"
                        fireBody = if (fire) {
                            val hit = alert.smcZones.filter { it in touched }
                            "Price touches ${hit.joinToString(", ")} (${matched}/${required} zones) at ${formatPrice(price, symbol)}"
                        } else ""
                    }
                } else {
                    val level = alert.price
                    val crossed = when (alert.condition) {
                        "Crossing Up" -> prev < level && price >= level
                        "Crossing Down" -> prev > level && price <= level
                        else -> (prev < level && price >= level) || (prev > level && price <= level)
                    }
                    fire = crossed
                    fireTitle = "Price alert: ${alert.symbol}"
                    fireBody = alert.message.ifEmpty { "${alert.symbol} ${alert.condition} $level" }
                }
                if (fire) {
                    com.asc.markets.notifications.NotificationHelper.showAlert(
                        context, fireTitle, fireBody,
                        type = "price_alert", symbol = alert.symbol
                    )
                    com.asc.markets.logic.VigilanceNodeEngine.recordTriggeredAlert(
                        com.asc.markets.logic.TriggeredAlert(
                            nodeId = "chart_alert_${alert.id}",
                            pair = alert.symbol,
                            title = fireTitle,
                            body = fireBody
                        )
                    )
                    onAlertTriggered(alert)
                }
            }
        }
        prevPriceForAlert.value = price
    }

    // Overlays: EMA/SMA/VWAP/ATR/BB/MACD/Volume - standalone render pass
    LaunchedEffect(showEma10, showEma20, showSma1, showSma2, showVwap, showAtr, showBb, showMacd, showVolume, hiddenIndicators, ohlcData, seriesApi, chartsViewApi) {
        runCatching {
        val mainSeriesApi = seriesApi
        val ohlcList = ohlcData
        if (showEma10 && "EMA 10" !in hiddenIndicators) {
            val ema10Data = com.trading.app.indicators.EmaIndicator(ema10Period).calculate(ohlcList)
            val series = ema10SeriesApi
            series?.setData(ema10Data.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            
            safelyRemovePriceLine(series, ema10LineState.value)
            ema10LineState.value = null
            if (series != null && (ema10ShowLabels || ema10ShowLines)) {
                ema10Data.lastOrNull()?.let { lastVal ->
                    ema10LineState.value = series.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(ComposeColor.White.toArgb()),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = ema10ShowLines,
                            axisLabelVisible = ema10ShowLabels,
                            title = "EMA:10 | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            safelyRemovePriceLine(ema10SeriesApi, ema10LineState.value)
            ema10LineState.value = null
        }

        if (showEma20) {
            val ema20Data = com.trading.app.indicators.EmaIndicator(ema20Period).calculate(ohlcList)
            val series = ema20SeriesApi
            series?.setData(ema20Data.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })

            safelyRemovePriceLine(series, ema20LineState.value)
            ema20LineState.value = null
            if (series != null && (ema20ShowLabels || ema20ShowLines)) {
                ema20Data.lastOrNull()?.let { lastVal ->
                    ema20LineState.value = series.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(ComposeColor.White.toArgb()),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = ema20ShowLines,
                            axisLabelVisible = ema20ShowLabels,
                            title = "EMA:20 | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            safelyRemovePriceLine(ema20SeriesApi, ema20LineState.value)
            ema20LineState.value = null
        }

        if (showSma1 && "SMA 1" !in hiddenIndicators) {
            val sma1Data = Indicators.calculateSma(ohlcList.map { it.close }, sma1Period)
            val series = sma1SeriesApi
            series?.setData(sma1Data.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })

            safelyRemovePriceLine(series, sma1LineState.value)
            sma1LineState.value = null
            if (series != null && (sma1ShowLabels || sma1ShowLines)) {
                sma1Data.lastOrNull()?.let { lastVal ->
                    sma1LineState.value = series.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(ComposeColor.White.toArgb()),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = sma1ShowLines,
                            axisLabelVisible = sma1ShowLabels,
                            title = "SMA:1 | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            safelyRemovePriceLine(sma1SeriesApi, sma1LineState.value)
            sma1LineState.value = null
        }

        if (showSma2 && "SMA 2" !in hiddenIndicators) {
            val sma2Data = Indicators.calculateSma(ohlcList.map { it.close }, sma2Period)
            val series = sma2SeriesApi
            series?.setData(sma2Data.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })

            safelyRemovePriceLine(series, sma2LineState.value)
            sma2LineState.value = null
            if (series != null && (sma2ShowLabels || sma2ShowLines)) {
                sma2Data.lastOrNull()?.let { lastVal ->
                    sma2LineState.value = series.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(ComposeColor.White.toArgb()),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = sma2ShowLines,
                            axisLabelVisible = sma2ShowLabels,
                            title = "SMA:2 | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            safelyRemovePriceLine(sma2SeriesApi, sma2LineState.value)
            sma2LineState.value = null
        }

        if (showVwap && "VWAP" !in hiddenIndicators) {
            val vwapBandFillColor = IntColor(applyOpacity(AndroidColor.parseColor("#2B4B60"), 18))
            val vwapBandMaskColor = IntColor(chartBgColor)

            vwapBandFillSeriesApi?.setData(vwapDataState.upperBand.mapIndexedNotNull { index, value ->
                value?.let {
                    AreaData(
                        time = candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null,
                        value = it,
                        lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                        topColor = vwapBandFillColor,
                        bottomColor = vwapBandFillColor
                    )
                }
            })
            vwapBandMaskSeriesApi?.setData(vwapDataState.lowerBand.mapIndexedNotNull { index, value ->
                value?.let {
                    AreaData(
                        time = candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null,
                        value = it,
                        lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                        topColor = vwapBandMaskColor,
                        bottomColor = vwapBandMaskColor
                    )
                }
            })
            vwapUpperSeriesApi?.setData(vwapDataState.upperBand.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            vwapSeriesApi?.setData(vwapDataState.vwap.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            vwapLowerSeriesApi?.setData(vwapDataState.lowerBand.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })

            safelyRemovePriceLine(vwapSeriesApi, vwapLineState.value)
            vwapLineState.value = null
            if (vwapSeriesApi != null && (vwapShowLabels || vwapShowLines)) {
                vwapDataState.vwap.lastOrNull()?.let { lastVal ->
                    vwapLineState.value = vwapSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.CYAN),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = vwapShowLines,
                            axisLabelVisible = vwapShowLabels,
                            title = "VWAP | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }

            safelyRemovePriceLine(vwapUpperSeriesApi, vwapUpperLineState.value)
            vwapUpperLineState.value = null
            if (vwapUpperSeriesApi != null && (vwapShowLabels || vwapShowLines)) {
                vwapDataState.upperBand.lastOrNull()?.let { lastVal ->
                    vwapUpperLineState.value = vwapUpperSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.GRAY),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = vwapShowLines,
                            axisLabelVisible = vwapShowLabels,
                            title = "VWAP:Upper | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }

            safelyRemovePriceLine(vwapLowerSeriesApi, vwapLowerLineState.value)
            vwapLowerLineState.value = null
            if (vwapLowerSeriesApi != null && (vwapShowLabels || vwapShowLines)) {
                vwapDataState.lowerBand.lastOrNull()?.let { lastVal ->
                    vwapLowerLineState.value = vwapLowerSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.GRAY),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = vwapShowLines,
                            axisLabelVisible = vwapShowLabels,
                            title = "VWAP:Lower | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            vwapBandFillSeriesApi?.setData(emptyList())
            vwapBandMaskSeriesApi?.setData(emptyList())
            vwapUpperSeriesApi?.setData(emptyList())
            vwapSeriesApi?.setData(emptyList())
            vwapLowerSeriesApi?.setData(emptyList())
            safelyRemovePriceLine(vwapSeriesApi, vwapLineState.value)
            safelyRemovePriceLine(vwapUpperSeriesApi, vwapUpperLineState.value)
            safelyRemovePriceLine(vwapLowerSeriesApi, vwapLowerLineState.value)
            vwapLineState.value = null
            vwapUpperLineState.value = null
            vwapLowerLineState.value = null
        }

        if (showAtr && "ATR" !in hiddenIndicators) {
            val atrData = com.trading.app.indicators.AtrIndicator(atrPeriod).calculate(ohlcList)
            atrSeriesApi?.setData(atrData.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            
            safelyRemovePriceLine(atrSeriesApi, atrLineState.value)
            atrLineState.value = null
            if (atrSeriesApi != null && (atrShowLabels || atrShowLines)) {
                atrData.lastOrNull()?.let { lastVal ->
                    atrLineState.value = atrSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(ComposeColor(0xFF2962FF).toArgb()),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = atrShowLines,
                            axisLabelVisible = atrShowLabels,
                            title = "ATR | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            safelyRemovePriceLine(atrSeriesApi, atrLineState.value)
            atrLineState.value = null
        }

        if (showBb && "BB" !in hiddenIndicators) {
            val bandFillColor = IntColor(applyOpacity(AndroidColor.parseColor("#2B4B60"), 18))
            val bandMaskColor = IntColor(chartBgColor)

            bbBandFillSeriesApi?.setData(bbDataState.upperBand.mapIndexedNotNull { index, value ->
                value?.let {
                    AreaData(
                        time = candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null,
                        value = it,
                        lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                        topColor = bandFillColor,
                        bottomColor = bandFillColor
                    )
                }
            })
            bbBandMaskSeriesApi?.setData(bbDataState.lowerBand.mapIndexedNotNull { index, value ->
                value?.let {
                    AreaData(
                        time = candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null,
                        value = it,
                        lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                        topColor = bandMaskColor,
                        bottomColor = bandMaskColor
                    )
                }
            })
            bbUpperSeriesApi?.setData(bbDataState.upperBand.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            bbMiddleSeriesApi?.setData(bbDataState.middleBand.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            bbLowerSeriesApi?.setData(bbDataState.lowerBand.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })

            safelyRemovePriceLine(bbMiddleSeriesApi, bbMiddleLineState.value)
            bbMiddleLineState.value = null
            if (bbMiddleSeriesApi != null && (bbShowLabels || bbShowLines)) {
                bbDataState.middleBand.lastOrNull()?.let { lastVal ->
                    bbMiddleLineState.value = bbMiddleSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.parseColor("#2962FF")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = bbShowLines,
                            axisLabelVisible = bbShowLabels,
                            title = "BB:Middle | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }

            safelyRemovePriceLine(bbUpperSeriesApi, bbUpperLineState.value)
            bbUpperLineState.value = null
            if (bbUpperSeriesApi != null && (bbShowLabels || bbShowLines)) {
                bbDataState.upperBand.lastOrNull()?.let { lastVal ->
                    bbUpperLineState.value = bbUpperSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.parseColor("#2962FF")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = bbShowLines,
                            axisLabelVisible = bbShowLabels,
                            title = "BB:Upper | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }

            safelyRemovePriceLine(bbLowerSeriesApi, bbLowerLineState.value)
            bbLowerLineState.value = null
            if (bbLowerSeriesApi != null && (bbShowLabels || bbShowLines)) {
                bbDataState.lowerBand.lastOrNull()?.let { lastVal ->
                    bbLowerLineState.value = bbLowerSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.parseColor("#2962FF")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = bbShowLines,
                            axisLabelVisible = bbShowLabels,
                            title = "BB:Lower | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            bbBandFillSeriesApi?.setData(emptyList())
            bbBandMaskSeriesApi?.setData(emptyList())
            bbUpperSeriesApi?.setData(emptyList())
            bbMiddleSeriesApi?.setData(emptyList())
            bbLowerSeriesApi?.setData(emptyList())
            safelyRemovePriceLine(bbMiddleSeriesApi, bbMiddleLineState.value)
            safelyRemovePriceLine(bbUpperSeriesApi, bbUpperLineState.value)
            safelyRemovePriceLine(bbLowerSeriesApi, bbLowerLineState.value)
            bbMiddleLineState.value = null
            bbUpperLineState.value = null
            bbLowerLineState.value = null
        }

        if (showMacd && "MACD" !in hiddenIndicators) {
            val macdIndicator = com.trading.app.indicators.MacdIndicator(macdFast, macdSlow, macdSignal)
            val macdLine = macdIndicator.calculateMacdLine(ohlcList)
            val signalLine = macdIndicator.calculateSignalLine(macdLine)
            val histogram = macdIndicator.calculateHistogram(macdLine, signalLine)

            macdLineSeriesApi?.setData(macdLine.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            macdSignalSeriesApi?.setData(signalLine.mapIndexedNotNull { index, value ->
                value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
            })
            macdHistogramSeriesApi?.setData(histogram.mapIndexedNotNull { index, value ->
                value?.let {
                    HistogramData(
                        time = candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null,
                        value = it,
                        color = if (it >= 0) IntColor(AndroidColor.parseColor("#089981")) else IntColor(AndroidColor.parseColor("#F23645"))
                    )
                }
            })

            safelyRemovePriceLine(macdLineSeriesApi, macdLinePriceLineState.value)
            macdLinePriceLineState.value = null
            if (macdLineSeriesApi != null && (macdShowLabels || macdShowLines)) {
                macdLine.lastOrNull()?.let { lastVal ->
                    macdLinePriceLineState.value = macdLineSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.parseColor("#2962FF")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = macdShowLines,
                            axisLabelVisible = macdShowLabels,
                            title = "MACD | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }

            safelyRemovePriceLine(macdSignalSeriesApi, macdSignalPriceLineState.value)
            macdSignalPriceLineState.value = null
            if (macdSignalSeriesApi != null && (macdShowLabels || macdShowLines)) {
                signalLine.lastOrNull()?.let { lastVal ->
                    macdSignalPriceLineState.value = macdSignalSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(AndroidColor.parseColor("#FF9800")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = macdShowLines,
                            axisLabelVisible = macdShowLabels,
                            title = "MACD:Signal | ${String.format("%.3f", lastVal)}"
                        )
                    )
                }
            }
        } else {
            macdLineSeriesApi?.setData(emptyList())
            macdSignalSeriesApi?.setData(emptyList())
            macdHistogramSeriesApi?.setData(emptyList())
            safelyRemovePriceLine(macdLineSeriesApi, macdLinePriceLineState.value)
            safelyRemovePriceLine(macdSignalSeriesApi, macdSignalPriceLineState.value)
            macdLinePriceLineState.value = null
            macdSignalPriceLineState.value = null
        }

        if (showVolume && "Volume" !in hiddenIndicators) {
            volumeSeriesApi?.setData(buildVolumeHistogramData(ohlcData, volumeGrowingColor, volumeFallingColor, volumeColorBasedOnPreviousClose))
            
            safelyRemovePriceLine(volumeSeriesApi, volumeLineState.value)
            volumeLineState.value = null
            if (volumeSeriesApi != null && (volumeShowLabels || volumeShowLines)) {
                ohlcData.lastOrNull()?.volume?.let { lastVal ->
                    volumeLineState.value = volumeSeriesApi!!.createPriceLine(
                        PriceLineOptions(
                            price = lastVal,
                            color = IntColor(volumeGrowingColor.toArgb()),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = volumeShowLines,
                            axisLabelVisible = volumeShowLabels,
                            title = "Volume | ${String.format("%.0f", lastVal)}"
                        )
                    )
                }
            }

            if (showVolumeMa) {
                volumeMaSeriesApi?.setData(volumeMaDataState.mapIndexedNotNull { index, value ->
                    value?.let { LineData(candlestickData.getOrNull(index)?.time ?: return@mapIndexedNotNull null, it) }
                })

                safelyRemovePriceLine(volumeMaSeriesApi, volumeMaLineState.value)
                volumeMaLineState.value = null
                if (volumeMaSeriesApi != null && (volumeShowLabels || volumeShowLines)) {
                    volumeMaDataState.lastOrNull()?.let { lastVal ->
                        volumeMaLineState.value = volumeMaSeriesApi!!.createPriceLine(
                            PriceLineOptions(
                                price = lastVal,
                                color = IntColor(volumeMaColor.toArgb()),
                                lineWidth = LineWidth.ONE,
                                lineStyle = LineStyle.DASHED,
                                lineVisible = volumeShowLines,
                                axisLabelVisible = volumeShowLabels,
                                title = "Volume:MA | ${String.format("%.0f", lastVal)}"
                            )
                        )
                    }
                }
            } else {
                volumeMaSeriesApi?.setData(emptyList())
                safelyRemovePriceLine(volumeMaSeriesApi, volumeMaLineState.value)
                volumeMaLineState.value = null
            }
        } else {
            volumeSeriesApi?.setData(emptyList())
            volumeMaSeriesApi?.setData(emptyList())
            safelyRemovePriceLine(volumeSeriesApi, volumeLineState.value)
            safelyRemovePriceLine(volumeMaSeriesApi, volumeMaLineState.value)
            volumeLineState.value = null
            volumeMaLineState.value = null
        }
        }.onFailure { android.util.Log.w("TradingChart", "render pass skipped: " + it.message) }
    }
    LaunchedEffect(
        seriesApi,
        rsiPaneRefs.rsiSeriesApi,
        volumeSeriesApi,
        volumeMaSeriesApi,
        macdLineSeriesApi,
        atrSeriesApi,
        showInlineRsiPane,
        showMacd,
        showVolume,
        showAtr,
        chartSettings.canvas.scaleLineColor,
        chartSettings.scales
    ) {
        val mainScaleMargins = paneMargins["main"]!!
        val macdScaleMargins = paneMargins[MACD_SCALE_KEY] ?: PriceScaleMargins(
            top = 1f - INDICATOR_PANE_HEIGHT,
            bottom = 0f
        )
        val mainBottom = mainScaleMargins.bottom ?: 0f
        val mainTop = mainScaleMargins.top ?: 0f
        val volumeScaleMargins = PriceScaleMargins(
            top = (1f - mainBottom - 0.15f).coerceAtLeast(mainTop),
            bottom = 0f
        )
        val atrScaleMargins = paneMargins[ATR_SCALE_KEY] ?: PriceScaleMargins(0.82f, 0.02f)
        val scaleBorderColor = chartSettings.canvas.scaleLineColor.toIntColor()
        val scales = chartSettings.scales
        val activeScaleMode = toPriceScaleMode(scales.scaleType)
        val autoScaleEnabled = scales.autoScale && !scales.lockRatio
        val scalePosition = if (scales.scalesPlacement == "Left") PriceAxisPosition.LEFT else PriceAxisPosition.RIGHT

        seriesApi?.priceScale()?.applyOptions(
            PriceScaleOptions(
                autoScale = autoScaleEnabled,
                mode = activeScaleMode,
                invertScale = scales.invertScale,
                position = scalePosition,
                scaleMargins = mainScaleMargins
            )
        )

        applyInlineRsiPaneScale(
            refs = rsiPaneRefs,
            scaleMargins = PriceScaleMargins(top = 0.08f, bottom = 0.08f),
            borderColor = scaleBorderColor,
            visible = showInlineRsiPane
        )
        macdLineSeriesApi?.priceScale()?.applyOptions(
            PriceScaleOptions(
                autoScale = true,
                scaleMargins = macdScaleMargins,
                visible = showMacd,
                borderVisible = false,
                borderColor = scaleBorderColor,
                entireTextOnly = true,
                alignLabels = true,
                ticksVisible = false
            )
        )

        volumeSeriesApi?.priceScale()?.applyOptions(
            PriceScaleOptions(
                autoScale = true,
                scaleMargins = volumeScaleMargins,
                visible = false,
                borderVisible = false
            )
        )

        volumeMaSeriesApi?.priceScale()?.applyOptions(
            PriceScaleOptions(
                autoScale = true,
                scaleMargins = volumeScaleMargins,
                visible = false,
                borderVisible = false
            )
        )

        atrSeriesApi?.priceScale()?.applyOptions(
            PriceScaleOptions(
                autoScale = true,
                scaleMargins = atrScaleMargins,
                visible = showAtr,
                borderVisible = false,
                borderColor = scaleBorderColor,
                entireTextOnly = true,
                alignLabels = true,
                ticksVisible = false
            )
        )
    }

    LaunchedEffect(seriesApi, chartSettings.canvas.scaleFontSize) {
        seriesApi?.priceScale()?.width { width ->
            if (width > 0f) {
                mainPriceScaleWidthPx = width
            }
        }
    }

    LaunchedEffect(currentQuoteState, seriesApi, style) {
        val quote = currentQuoteState ?: return@LaunchedEffect
        val api = seriesApi ?: return@LaunchedEffect
        val lastCandle = ohlcData.lastOrNull() ?: return@LaunchedEffect

        if (style == "heikin_ashi") return@LaunchedEffect

        // Render pass in flight (history setData or indicator overlay rebuild): skip this
        // tick. The JS bridge throws asynchronously (uncatchable) if update() lands mid-rebuild.
        if (chartBusy) return@LaunchedEffect

        val updatedCandle = lastCandle.copy(
            high = maxOf(lastCandle.high, quote.lastPrice),
            low = minOf(lastCandle.low, quote.lastPrice),
            close = quote.lastPrice
        )

        // A tick landing while the chart is being rebuilt (asset switch, indicator
        // toggles) makes the JS bridge throw - swallow it instead of crashing.
        runCatching {
            when (mainSeriesKind) {
                MainSeriesKind.BAR -> api.update(updatedCandle.toBarSeriesData())
                MainSeriesKind.LINE -> api.update(updatedCandle.toLineSeriesData())
                MainSeriesKind.AREA -> api.update(
                    updatedCandle.toAreaSeriesData(areaValueForStyle(style, updatedCandle))
                )
                MainSeriesKind.BASELINE -> api.update(updatedCandle.toBaselineSeriesData())
                MainSeriesKind.CANDLESTICK -> api.update(updatedCandle.toCandlestickData())
            }
        }.onFailure { Log.w("TradingChart", "Tick update skipped: ${it.message}") }
    }

    // Double-click detection on the chart
    var lastClickTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(seriesApi) {
        val api = chartsViewApi?.api ?: return@LaunchedEffect
        
        api.subscribeClick { params ->
            // Clear indicator highlight and hide specific indicators on any chart click
            onSelectedIndicatorIdChange(null)
            onEma10Toggle(false)
            onEma20Toggle(false)
            onSma1Toggle(false)
            onSma2Toggle(false)

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 400) {
                // Double click detected
                val lastPrice = currentQuoteState?.lastPrice ?: 0f
                if (lastPrice != 0f) {
                    onDoubleClick(lastPrice)
                }
            }
            lastClickTime = currentTime
        }
    }

    // Manage High/Low Price Lines and Labels
    LaunchedEffect(candlestickData, currentQuoteState, seriesApi, 
        chartSettings.scales.highLowPriceLabels, 
        chartSettings.scales.highLowPriceLines,
        chartSettings.scales.highLowLineColor,
        chartSettings.scales.highLowLabelColor,
        chartSettings.scales.highLowCalculationMode,
        visibleRangeHighLow) {
        
        val api = seriesApi ?: return@LaunchedEffect
        val scales = chartSettings.scales
        
        // Remove existing lines
        if (highLowPriceLineOwner === api) {
            safelyRemovePriceLine(api, highLineState.value)
            safelyRemovePriceLine(api, highLabelState.value)
            safelyRemovePriceLine(api, lowLineState.value)
            safelyRemovePriceLine(api, lowLabelState.value)
        }
        
        highLineState.value = null
        highLabelState.value = null
        lowLineState.value = null
        lowLabelState.value = null
        highLowPriceLineOwner = api

        if (candlestickData.isEmpty()) return@LaunchedEffect

        val calcMode = scales.highLowCalculationMode
        
        var maxHigh: Float
        var minLow: Float

        when (calcMode) {
            "100 candles" -> {
                val subList = candlestickData.takeLast(100)
                maxHigh = subList.maxOf { it.high }
                minLow = subList.minOf { it.low }
            }
            "500 candles" -> {
                val subList = candlestickData.takeLast(500)
                maxHigh = subList.maxOf { it.high }
                minLow = subList.minOf { it.low }
            }
            "Dynamic" -> {
                if (visibleRangeHighLow != null) {
                    maxHigh = visibleRangeHighLow!!.first
                    minLow = visibleRangeHighLow!!.second
                } else {
                    maxHigh = candlestickData.maxOf { it.high }
                    minLow = candlestickData.minOf { it.low }
                }
            }
            else -> {
                maxHigh = candlestickData.maxOf { it.high }
                minLow = candlestickData.minOf { it.low }
            }
        }
        
        // Include current quote in calculation if it's the latest data
        currentQuoteState?.let {
            if (calcMode == "Dynamic" || calcMode == "100 candles" || calcMode == "500 candles") {
                maxHigh = maxOf(maxHigh, it.lastPrice)
                minLow = minOf(minLow, it.lastPrice)
            }
        }

        val showLine = scales.highLowPriceLines
        val showLabel = scales.highLowPriceLabels

        val lineColor = try { IntColor(AndroidColor.parseColor(scales.highLowLineColor)) } catch (e: Exception) { IntColor(AndroidColor.WHITE) }
        val labelColor = try { IntColor(AndroidColor.parseColor(scales.highLowLabelColor)) } catch (e: Exception) { IntColor(AndroidColor.parseColor("#2962FF")) }
        
        // High Line
        if (showLine) {
            highLineState.value = api.createPriceLine(
                PriceLineOptions(
                    price = maxHigh,
                    color = lineColor,
                    lineWidth = LineWidth.ONE,
                    lineStyle = LineStyle.DASHED,
                    lineVisible = true,
                    axisLabelVisible = false,
                    title = "High"
                )
            )
        }

        // High Label
        if (showLabel) {
            highLabelState.value = api.createPriceLine(
                PriceLineOptions(
                    price = maxHigh,
                    color = labelColor,
                    lineWidth = LineWidth.ONE,
                    lineStyle = LineStyle.DASHED,
                    lineVisible = false,
                    axisLabelVisible = true,
                    title = "High"
                )
            )
        }

        // Low Line
        if (showLine) {
            lowLineState.value = api.createPriceLine(
                PriceLineOptions(
                    price = minLow,
                    color = lineColor,
                    lineWidth = LineWidth.ONE,
                    lineStyle = LineStyle.DASHED,
                    lineVisible = true,
                    axisLabelVisible = false,
                    title = "Low"
                )
            )
        }

        // Low Label
        if (showLabel) {
            lowLabelState.value = api.createPriceLine(
                PriceLineOptions(
                    price = minLow,
                    color = labelColor,
                    lineWidth = LineWidth.ONE,
                    lineStyle = LineStyle.DASHED,
                    lineVisible = false,
                    axisLabelVisible = true,
                    title = "Low"
                )
            )
        }
    }

    // Manage Bid/Ask Price Lines
    LaunchedEffect(currentQuoteState, seriesApi, 
        chartSettings.scales.bidAskLabels, 
        chartSettings.scales.bidAskLines,
        chartSettings.scales.bidAskMode, 
        chartSettings.scales.bidColor,
        chartSettings.scales.askColor) {
        
        val api = seriesApi ?: return@LaunchedEffect
        val quote = currentQuoteState ?: return@LaunchedEffect
        val scales = chartSettings.scales
        
        if (bidAskPriceLineOwner === api) {
            safelyRemovePriceLine(api, bidPriceLineState.value)
            safelyRemovePriceLine(api, askPriceLineState.value)
        }
        bidPriceLineState.value = null
        askPriceLineState.value = null
        bidAskPriceLineOwner = api

        if (!scales.bidAskLabels && !scales.bidAskLines) return@LaunchedEffect

        val showLine = scales.bidAskLines
        val showLabel = scales.bidAskLabels

        bidPriceLineState.value = api.createPriceLine(
            PriceLineOptions(
                price = quote.bid,
                color = try { IntColor(AndroidColor.parseColor(scales.bidColor)) } catch (e: Exception) { IntColor(AndroidColor.BLUE) },
                lineWidth = LineWidth.ONE,
                lineStyle = LineStyle.DASHED,
                lineVisible = showLine,
                axisLabelVisible = showLabel,
                title = "Bid"
            )
        )

        askPriceLineState.value = api.createPriceLine(
            PriceLineOptions(
                price = quote.ask,
                color = try { IntColor(AndroidColor.parseColor(scales.askColor)) } catch (e: Exception) { IntColor(AndroidColor.RED) },
                lineWidth = LineWidth.ONE,
                lineStyle = LineStyle.DASHED,
                lineVisible = showLine,
                axisLabelVisible = showLabel,
                title = "Ask"
            )
        )
    }

    // Manage Position Price Lines
    val positionPriceLines = remember { mutableStateListOf<PriceLine>() }
    var positionPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    // Using positions.toList() to ensure the effect re-runs when the list content changes
    val positionsSnapshot = positions.toList()
    LaunchedEffect(positionsSnapshot, seriesApi, symbol, currentQuoteState) {
        val api = seriesApi ?: return@LaunchedEffect
        val lastPrice = currentQuoteState?.lastPrice ?: 0f
        
        // Remove previous position lines
        if (positionPriceLineOwner === api) {
            positionPriceLines.forEach { safelyRemovePriceLine(api, it) }
        }
        positionPriceLines.clear()
        positionPriceLineOwner = api

        positionsSnapshot.filter { pos ->
            val s1 = normalizeChartSymbol(symbol)
            val s2 = normalizeChartSymbol(pos.symbol)
            s1 == s2
        }.forEach { position ->
            val color = if (position.type.equals("buy", ignoreCase = true)) "#089981" else "#F23645"
            val isBuy = position.type.equals("buy", ignoreCase = true)
            
            val pnl = if (lastPrice > 0) {
                (lastPrice - position.entryPrice) * position.volume * (if (isBuy) 1f else -1f)
            } else 0f
            
            val pnlText = if (lastPrice > 0) {
                " [${if (pnl >= 0) "+" else ""}${String.format("%.2f", pnl)}]"
            } else ""

            // Entry Line
            positionPriceLines.add(
                api.createPriceLine(
                    PriceLineOptions(
                        price = position.entryPrice,
                        color = IntColor(AndroidColor.parseColor(color)),
                        lineWidth = LineWidth.TWO,
                        lineStyle = LineStyle.SOLID,
                        lineVisible = true,
                        axisLabelVisible = true,
                        title = "${position.type.uppercase()} ${position.volume} $pnlText"
                    )
                )
            )

            // TP Line
            position.tp?.let { tp ->
                val tpPnl = (tp - position.entryPrice) * position.volume * (if (isBuy) 1f else -1f)
                val tpPnlText = " [${if (tpPnl >= 0) "+" else ""}${String.format("%.2f", tpPnl)}]"
                
                positionPriceLines.add(
                    api.createPriceLine(
                        PriceLineOptions(
                            price = tp,
                            color = IntColor(AndroidColor.parseColor("#089981")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = true,
                            axisLabelVisible = true,
                            title = "TP$tpPnlText"
                        )
                    )
                )
            }

            // SL Line
            position.sl?.let { sl ->
                val slPnl = (sl - position.entryPrice) * position.volume * (if (isBuy) 1f else -1f)
                val slPnlText = " [${if (slPnl >= 0) "+" else ""}${String.format("%.2f", slPnl)}]"
                
                positionPriceLines.add(
                    api.createPriceLine(
                        PriceLineOptions(
                            price = sl,
                            color = IntColor(AndroidColor.parseColor("#F23645")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = true,
                            axisLabelVisible = true,
                            title = "SL$slPnlText"
                        )
                    )
                )
            }
        }
    }

    // Manage Order Price Lines (Pending Orders)
    val orderPriceLines = remember { mutableStateListOf<PriceLine>() }
    var orderPriceLineOwner by remember { mutableStateOf<SeriesApi?>(null) }
    val ordersSnapshot = orders.toList()
    LaunchedEffect(ordersSnapshot, seriesApi, symbol) {
        val api = seriesApi ?: return@LaunchedEffect
        
        // Remove previous order lines
        if (orderPriceLineOwner === api) {
            orderPriceLines.forEach { safelyRemovePriceLine(api, it) }
        }
        orderPriceLines.clear()
        orderPriceLineOwner = api

        ordersSnapshot.filter { ord ->
            val s1 = normalizeChartSymbol(symbol)
            val s2 = normalizeChartSymbol(ord.symbol)
            s1 == s2
        }.forEach { order ->
            val color = if (order.type.equals("buy", ignoreCase = true)) "#089981" else "#F23645"
            
            // Order Price Line
            orderPriceLines.add(
                api.createPriceLine(
                    PriceLineOptions(
                        price = order.price,
                        color = IntColor(AndroidColor.parseColor(color)),
                        lineWidth = LineWidth.ONE,
                        lineStyle = LineStyle.DASHED,
                        lineVisible = true,
                        axisLabelVisible = true,
                        title = "${order.orderType.uppercase()} ${order.volume}"
                    )
                )
            )

            // TP Line for Order
            order.tp?.let { tp ->
                val isBuy = order.type.equals("buy", ignoreCase = true)
                val tpPnl = (tp - order.price) * order.volume * (if (isBuy) 1f else -1f)
                val tpPnlText = " [${if (tpPnl >= 0) "+" else ""}${String.format("%.2f", tpPnl)}]"
                
                orderPriceLines.add(
                    api.createPriceLine(
                        PriceLineOptions(
                            price = tp,
                            color = IntColor(AndroidColor.parseColor("#089981")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = true,
                            axisLabelVisible = true,
                            title = "TP$tpPnlText"
                        )
                    )
                )
            }

            // SL Line for Order
            order.sl?.let { sl ->
                val isBuy = order.type.equals("buy", ignoreCase = true)
                val slPnl = (sl - order.price) * order.volume * (if (isBuy) 1f else -1f)
                val slPnlText = " [${if (slPnl >= 0) "+" else ""}${String.format("%.2f", slPnl)}]"
                
                orderPriceLines.add(
                    api.createPriceLine(
                        PriceLineOptions(
                            price = sl,
                            color = IntColor(AndroidColor.parseColor("#F23645")),
                            lineWidth = LineWidth.ONE,
                            lineStyle = LineStyle.DASHED,
                            lineVisible = true,
                            axisLabelVisible = true,
                            title = "SL$slPnlText"
                        )
                    )
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!providerManagedData) {
                mt5Service.disconnect()
                reverseBridge?.disconnect()
            }
        }
    }

    // Reset handles ONLY when the native view is recreated (style/feed change).
    // NEVER key this on symbol: the chart view persists across asset switches,
    // so nulling here would orphan every series handle with no factory re-run
    // to restore them - freezing the chart on the previous asset.
    DisposableEffect(style, chartFeedType) {
        onDispose {
            resetChartSeriesHandles()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(getFullChartColor(chartSettings.canvas.fullChartColor, chartSettings.canvas.background)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onEma10Toggle(false)
                onEma20Toggle(false)
                onSma1Toggle(false)
                onSma2Toggle(false)
                onSelectedIndicatorIdChange(null)
        }
    ) {
        // Background Canvas for Notifications (Behind Candles)
        if (chartSettings.canvas.showNotifications) {
            val interval = timeframeToSeconds(timeframe)
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val range = visibleTimeRange ?: return@Canvas
                val fromTime = (range.from as? Time.Utc)?.timestamp ?: 0L
                
                // We anchor to a timestamp that is slightly before the current visible range 
                // so it "drags" into view or stays at a specific relative position.
                // To make it truly drag with the chart, we can offset it based on how many bars 
                // have passed from a reference point.
                
                val nativeCanvas = drawContext.canvas.nativeCanvas
                val paint = android.graphics.Paint().apply {
                    color = AndroidColor.parseColor("#787B86")
                    textSize = 11.dp.toPx()
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    isAntiAlias = true
                }
                val valuePaint = android.graphics.Paint().apply {
                    textSize = 11.dp.toPx()
                    typeface = android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL)
                    isAntiAlias = true
                }

                // Rendered BEHIND the candles.
                var currentY = 120.dp.toPx()
                val startX = 12.dp.toPx()
                val boxWidth = 180.dp.toPx()
                val boxHeight = 26.dp.toPx()
                val cornerRadius = 4.dp.toPx()
                
                val bgPaint = android.graphics.Paint().apply {
                    color = AndroidColor.parseColor("#CC131722")
                    this.style = android.graphics.Paint.Style.FILL
                }

                fun drawItem(label: String, value: String? = null, vColor: Int = AndroidColor.parseColor("#D1D4DC")) {
                    nativeCanvas.drawRoundRect(startX, currentY, startX + boxWidth, currentY + boxHeight, cornerRadius, cornerRadius, bgPaint)
                    nativeCanvas.drawText(label, startX + 8.dp.toPx(), currentY + boxHeight / 2 + 4.dp.toPx(), paint)
                    if (value != null) {
                        valuePaint.color = vColor
                        val valX = startX + boxWidth - valuePaint.measureText(value) - 8.dp.toPx()
                        nativeCanvas.drawText(value, valX, currentY + boxHeight / 2 + 4.dp.toPx(), valuePaint)
                    }
                    currentY += boxHeight + 8.dp.toPx()
                }

                positions.filter { it.symbol.uppercase() == symbol.uppercase() }.forEach { pos ->
                    val isBuy = pos.type.lowercase() == "buy"
                    val pnl = ((currentQuoteState?.lastPrice ?: 0f) - pos.entryPrice) * pos.volume * (if (isBuy) 1f else -1f)
                    val pnlColor = if (pnl >= 0) AndroidColor.parseColor("#089981") else AndroidColor.parseColor("#F23645")
                    drawItem("Positions", "${if (isBuy) "Long" else "Short"} x${pos.volume}  ${if (pnl >= 0) "+" else ""}${String.format("%.2f", pnl)} USD", pnlColor)
                    
                    if (chartSettings.trading.reversePositionButton) {
                        val revBgPaint = android.graphics.Paint().apply {
                            color = AndroidColor.parseColor("#F05252")
                            this.style = android.graphics.Paint.Style.FILL
                        }
                        nativeCanvas.drawRoundRect(startX, currentY, startX + boxWidth, currentY + boxHeight, cornerRadius, cornerRadius, revBgPaint)
                        valuePaint.color = AndroidColor.WHITE
                        val text = "Reverse"
                        val textWidth = valuePaint.measureText(text)
                        nativeCanvas.drawText(text, startX + (boxWidth - textWidth) / 2, currentY + boxHeight / 2 + 4.dp.toPx(), valuePaint)
                        currentY += boxHeight + 8.dp.toPx()
                    }
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main price chart region - shrinks when the dedicated RSI pane is visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (showInlineRsiPane) 1f - RSI_PANE_SPLIT_FRACTION else 1f)
                ) {
                key(style) {
                    AndroidView(
                        factory = { context ->
                            resetChartSeriesHandles()
                            ChartsView(context).apply {
                                chartsViewApi = this
                                val uppercaseSymbol = symbol.uppercase()
                                val isBitcoin = uppercaseSymbol.contains("BTC") || uppercaseSymbol.contains("BITCOIN")
                                val isForex = uppercaseSymbol.length == 6 || uppercaseSymbol.contains("/")
                                val mainScaleMargins = paneMargins["main"]!!
                                val macdScaleMargins = paneMargins[MACD_SCALE_KEY] ?: PriceScaleMargins(
                                    top = 1f - INDICATOR_PANE_HEIGHT,
                                    bottom = 0f
                                )
                                val mainBottom = mainScaleMargins.bottom ?: 0f
                                val mainTop = mainScaleMargins.top ?: 0f
                                val volumeScaleMargins = PriceScaleMargins(
                                    top = (1f - mainBottom - 0.15f).coerceAtLeast(mainTop),
                                    bottom = 0f
                                )
                                val atrScaleMargins = paneMargins[ATR_SCALE_KEY] ?: PriceScaleMargins(0.82f, 0.02f)
                                val scales = chartSettings.scales
                                val activeScaleMode = toPriceScaleMode(scales.scaleType)
                                val autoScaleEnabled = scales.autoScale && !scales.lockRatio
                                val useLeftPriceScale = scales.scalesPlacement == "Left"

                                val precision = when {
                                    isBitcoin -> 0
                                    isForex -> 5
                                    else -> 2
                                }
                                val minMove = when {
                                    isBitcoin -> 1f
                                    isForex -> 0.00001f
                                    else -> 0.01f
                                }

                                api.applyOptions {
                            layout = LayoutOptions(
                                background = SolidColor(color = IntColor(chartBgColor)),
                                textColor = chartSettings.canvas.scaleTextColor.toIntColor(),
                                fontSize = chartSettings.canvas.scaleFontSize
                            )
                            grid = GridOptions(
                                vertLines = GridLineOptions(
                                    color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.gridColor), chartSettings.canvas.gridOpacity)),
                                    visible = chartSettings.canvas.gridVisible && chartSettings.canvas.gridType in listOf("Vert and horz", "Vert")
                                ),
                                horzLines = GridLineOptions(
                                    color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.horzGridColor), chartSettings.canvas.gridOpacity)),
                                    visible = chartSettings.canvas.gridVisible && chartSettings.canvas.gridType in listOf("Vert and horz", "Horz")
                                )
                            )
                            crosshair = CrosshairOptions(
                                mode = CrosshairMode.NORMAL,
                                vertLine = CrosshairLineOptions(
                                    color = chartSettings.canvas.crosshairColor.toIntColor(),
                                    width = chartSettings.canvas.crosshairThickness.toLineWidth(),
                                    style = chartSettings.canvas.crosshairLineStyle.toLineStyle()
                                ),
                                horzLine = CrosshairLineOptions(
                                    color = chartSettings.canvas.crosshairColor.toIntColor(),
                                    width = chartSettings.canvas.crosshairThickness.toLineWidth(),
                                    style = chartSettings.canvas.crosshairLineStyle.toLineStyle()
                                )
                            )
                            rightPriceScale = PriceScaleOptions(
                                borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                entireTextOnly = false,
                                autoScale = autoScaleEnabled,
                                mode = activeScaleMode,
                                invertScale = scales.invertScale,
                                position = PriceAxisPosition.RIGHT,
                                visible = !useLeftPriceScale
                            )
                            leftPriceScale = PriceScaleOptions(
                                borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                entireTextOnly = false,
                                autoScale = autoScaleEnabled,
                                mode = activeScaleMode,
                                invertScale = scales.invertScale,
                                position = PriceAxisPosition.LEFT,
                                visible = useLeftPriceScale
                            )
                            timeScale = TimeScaleOptions(
                                borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                visible = true,
                                timeVisible = true,
                                rightOffset = 15f,
                                barSpacing = 6f
                            )
                            handleScroll = HandleScrollOptions(
                                pressedMouseMove = true,
                                horzTouchDrag = true,
                                vertTouchDrag = true
                            )
                            handleScale = HandleScaleOptions(
                                mouseWheel = true,
                                pinch = true,
                                axisPressedMouseMove = AxisPressedMouseMoveOptions(
                                    time = !scales.scalePriceChartOnly,
                                    price = true
                                )
                            )
                            kineticScroll = KineticScrollOptions(
                                touch = true,
                                mouse = true
                            )
                        }

                        api.timeScale.subscribeVisibleTimeRangeChange { range ->
                            visibleTimeRange = range
                            if (range != null && candlestickData.isNotEmpty()) {
                                // Lazy loading logic
                                val loadingMoreNow = if (providerManagedData) providerChartData?.isLoadingMore == true else isLoadingMore
                                val hasMoreNow = if (providerManagedData) providerChartData?.hasMoreHistory != false else hasMoreHistory
                                if (!loadingMoreNow && hasMoreNow && ohlcData.size < 10000) {
                                    val firstCandleTime = ohlcData.first().time
                                    val visibleStart = (range.from as? Time.Utc)?.timestamp ?: 0L
                                    val threshold = timeframeToSeconds(timeframe) * 50 // 50 candles buffer
                                    
                                    if (visibleStart <= firstCandleTime + threshold) {
                                        val endTime = firstCandleTime - 1
                                        if (providerManagedData) {
                                            providerChartData?.onLoadMoreHistory?.invoke(endTime)
                                        } else {
                                            isLoadingMore = true
                                            when (chartFeedType) {
                                                ChartFeedType.EXNESS -> mt5Service.subscribe(chartFeedSymbolFor(ChartFeedType.EXNESS, symbol), timeframe, endTime, 500)
                                                null -> {
                                                    val streamSymbol = normalizeChartSymbol(symbol)
                                                    mt5Service.subscribe(streamSymbol, timeframe, endTime, 500)
                                                }
                                            }
                                        }
                                    }
                                }

                                try {
                                    val start = (range.from as? Time.Utc)?.timestamp ?: 0L
                                    val end = (range.to as? Time.Utc)?.timestamp ?: Long.MAX_VALUE
                                    
                                    val visibleCandles = candlestickData.filter { (it.time as? Time.Utc)?.timestamp in start..end }
                                    if (visibleCandles.isNotEmpty()) {
                                        visibleRangeHighLow = Pair(visibleCandles.maxOf { it.high }, visibleCandles.minOf { it.low })
                                    }
                                } catch (e: Exception) {
                                    Log.e("Chart", "Error calculating visible high/low", e)
                                }
                            }
                        }

                        api.subscribeCrosshairMove { params ->
                            val time = params.time as? Time.Utc
                            if (time != null) {
                                val ts = time.timestamp
                                val index = ohlcData.indexOfFirst { it.time == ts }
                                if (index != -1) {
                                    rsiPaneRefs.crosshairRsiValue = currentRsiDataState.values.getOrNull(index)
                                    rsiPaneRefs.crosshairMaValue = currentRsiDataState.movingAverageValues.getOrNull(index)
                                } else {
                                    rsiPaneRefs.crosshairRsiValue = null
                                    rsiPaneRefs.crosshairMaValue = null
                                }
                            } else {
                                rsiPaneRefs.crosshairRsiValue = null
                                rsiPaneRefs.crosshairMaValue = null
                            }
                        }

                        val priceLineVisible = chartSettings.scales.symbolLastPriceLine
                        val lastValueVisible = chartSettings.scales.symbolLastPriceLabel

                        val mainPriceFormat = PriceFormat.priceFormatBuiltIn(
                            type = PriceFormat.Type.PRICE,
                            precision = precision,
                            minMove = minMove.toFloat()
                        )
                        val upColorRaw = runCatching {
                            AndroidColor.parseColor(chartSettings.symbol.upColor)
                        }.getOrDefault(AndroidColor.WHITE)
                        val downColorRaw = runCatching {
                            AndroidColor.parseColor(chartSettings.symbol.downColor)
                        }.getOrDefault(AndroidColor.BLACK)

                        when (mainSeriesKind) {
                            MainSeriesKind.BAR -> {
                                api.addBarSeries(
                                    options = BarSeriesOptions(
                                        upColor = chartSettings.symbol.upColor.toIntColor(),
                                        downColor = chartSettings.symbol.downColor.toIntColor(),
                                        openVisible = style != "high_low" && !chartSettings.symbol.hlcBars,
                                        thinBars = chartSettings.symbol.thinBars || style == "high_low",
                                        priceFormat = mainPriceFormat,
                                        priceLineVisible = priceLineVisible,
                                        lastValueVisible = lastValueVisible
                                    ),
                                    onSeriesCreated = { createdSeries ->
                                        seriesApi = createdSeries
                                        createdSeries.priceScale().applyOptions(
                                            PriceScaleOptions(
                                                autoScale = true,
                                                scaleMargins = mainScaleMargins
                                            )
                                        )
                                    }
                                )
                            }
                            MainSeriesKind.LINE -> {
                                api.addLineSeries(
                                    options = LineSeriesOptions(
                                        color = chartSettings.symbol.upColor.toIntColor(),
                                        lineType = if (style == "step_line") LineType.WITH_STEPS else LineType.SIMPLE,
                                        crosshairMarkerVisible = style == "line_markers",
                                        crosshairMarkerRadius = if (style == "line_markers") 4f else null,
                                        crosshairMarkerBorderColor = if (style == "line_markers") chartSettings.symbol.upColor.toIntColor() else null,
                                        crosshairMarkerBackgroundColor = if (style == "line_markers") IntColor(chartBgColor) else null,
                                        priceFormat = mainPriceFormat,
                                        priceLineVisible = priceLineVisible,
                                        lastValueVisible = lastValueVisible
                                    ),
                                    onSeriesCreated = { createdSeries ->
                                        seriesApi = createdSeries
                                        createdSeries.priceScale().applyOptions(
                                            PriceScaleOptions(
                                                autoScale = true,
                                                scaleMargins = mainScaleMargins
                                            )
                                        )
                                    }
                                )
                            }
                            MainSeriesKind.AREA -> {
                                api.addAreaSeries(
                                    options = AreaSeriesOptions(
                                        lineColor = chartSettings.symbol.upColor.toIntColor(),
                                        topColor = IntColor(applyOpacity(upColorRaw, 28)),
                                        bottomColor = IntColor(applyOpacity(upColorRaw, 4)),
                                        priceFormat = mainPriceFormat,
                                        priceLineVisible = priceLineVisible,
                                        lastValueVisible = lastValueVisible
                                    ),
                                    onSeriesCreated = { createdSeries ->
                                        seriesApi = createdSeries
                                        createdSeries.priceScale().applyOptions(
                                            PriceScaleOptions(
                                                autoScale = true,
                                                scaleMargins = mainScaleMargins
                                            )
                                        )
                                    }
                                )
                            }
                            MainSeriesKind.BASELINE -> {
                                api.addBaselineSeries(
                                    options = BaselineSeriesOptions(
                                        topLineColor = IntColor(upColorRaw),
                                        topFillColor1 = IntColor(applyOpacity(upColorRaw, 24)),
                                        topFillColor2 = IntColor(applyOpacity(upColorRaw, 6)),
                                        bottomLineColor = IntColor(downColorRaw),
                                        bottomFillColor1 = IntColor(applyOpacity(downColorRaw, 24)),
                                        bottomFillColor2 = IntColor(applyOpacity(downColorRaw, 6)),
                                        priceFormat = mainPriceFormat,
                                        priceLineVisible = priceLineVisible,
                                        lastValueVisible = lastValueVisible
                                    ),
                                    onSeriesCreated = { createdSeries ->
                                        seriesApi = createdSeries
                                        createdSeries.priceScale().applyOptions(
                                            PriceScaleOptions(
                                                autoScale = true,
                                                scaleMargins = mainScaleMargins
                                            )
                                        )
                                    }
                                )
                            }
                            MainSeriesKind.CANDLESTICK -> {
                                val isHollowCandles = style == "hollow_candles"
                                api.addCandlestickSeries(
                                    options = CandlestickSeriesOptions(
                                        upColor = if (isHollowCandles) IntColor(chartBgColor) else chartSettings.symbol.upColor.toIntColor(),
                                        downColor = chartSettings.symbol.downColor.toIntColor(),
                                        borderVisible = if (isHollowCandles) true else chartSettings.symbol.borderVisible,
                                        borderUpColor = chartSettings.symbol.borderColorUp.toIntColor(),
                                        borderDownColor = chartSettings.symbol.borderColorDown.toIntColor(),
                                        wickVisible = if (isHollowCandles) true else chartSettings.symbol.wickVisible,
                                        wickUpColor = chartSettings.symbol.wickColorUp.toIntColor(),
                                        wickDownColor = chartSettings.symbol.wickColorDown.toIntColor(),
                                        priceFormat = mainPriceFormat,
                                        priceLineVisible = priceLineVisible,
                                        lastValueVisible = lastValueVisible
                                    ),
                                    onSeriesCreated = { createdSeries ->
                                        seriesApi = createdSeries
                                        createdSeries.priceScale().applyOptions(
                                            PriceScaleOptions(
                                                autoScale = true,
                                                scaleMargins = mainScaleMargins
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(ComposeColor.White.toArgb()),
                                lineWidth = LineWidth.ONE
                            ),
                            onSeriesCreated = { ema10SeriesApi = it }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(ComposeColor.White.toArgb()),
                                lineWidth = LineWidth.ONE
                            ),
                            onSeriesCreated = { ema20SeriesApi = it }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(ComposeColor.White.toArgb()),
                                lineWidth = LineWidth.ONE
                            ),
                            onSeriesCreated = { sma1SeriesApi = it }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(ComposeColor.White.toArgb()),
                                lineWidth = LineWidth.ONE
                            ),
                            onSeriesCreated = { sma2SeriesApi = it }
                        )

                        api.addAreaSeries(
                            options = AreaSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                                topColor = IntColor(applyOpacity(AndroidColor.parseColor("#2B4B60"), 18)),
                                bottomColor = IntColor(applyOpacity(AndroidColor.parseColor("#2B4B60"), 18)),
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { vwapBandFillSeriesApi = it }
                        )
                        api.addAreaSeries(
                            options = AreaSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                                topColor = IntColor(chartBgColor),
                                bottomColor = IntColor(chartBgColor),
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { vwapBandMaskSeriesApi = it }
                        )
                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#4CAF50")),
                                lineWidth = LineWidth.ONE,
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { vwapUpperSeriesApi = it }
                        )
                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#2962FF")),
                                lineWidth = LineWidth.ONE,
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { vwapSeriesApi = it }
                        )
                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#4CAF50")),
                                lineWidth = LineWidth.ONE,
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { vwapLowerSeriesApi = it }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#F44336")),
                                lineWidth = LineWidth.ONE,
                                priceScaleId = PriceScaleId(ATR_SCALE_KEY)
                            ),
                            onSeriesCreated = { api ->
                                atrSeriesApi = api
                                api.priceScale().applyOptions(
                                    PriceScaleOptions(
                                        autoScale = true,
                                        scaleMargins = atrScaleMargins,
                                        visible = showAtr,
                                        borderVisible = false,
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        entireTextOnly = true,
                                        alignLabels = true,
                                        ticksVisible = false
                                    )
                                )
                            }
                        )

                        api.addAreaSeries(
                            options = AreaSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                                topColor = IntColor(applyOpacity(AndroidColor.parseColor("#2B4B60"), 18)),
                                bottomColor = IntColor(applyOpacity(AndroidColor.parseColor("#2B4B60"), 18)),
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { bbBandFillSeriesApi = it }
                        )
                        api.addAreaSeries(
                            options = AreaSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                lineColor = IntColor(applyOpacity(AndroidColor.WHITE, 0)),
                                topColor = IntColor(chartBgColor),
                                bottomColor = IntColor(chartBgColor),
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { bbBandMaskSeriesApi = it }
                        )
                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#F23645")),
                                lineWidth = LineWidth.ONE,
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { bbUpperSeriesApi = it }
                        )
                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#2196F3")),
                                lineWidth = LineWidth.ONE,
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { bbMiddleSeriesApi = it }
                        )
                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#00BFA5")),
                                lineWidth = LineWidth.ONE,
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { bbLowerSeriesApi = it }
                        )

                        // RSI series live in the dedicated ChartsView below - nothing to create here

                        api.addHistogramSeries(
                            options = HistogramSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                base = 0f,
                                priceFormat = PriceFormat.priceFormatBuiltIn(type = PriceFormat.Type.PRICE, precision = 4, minMove = 0.0001f),
                                priceScaleId = PriceScaleId(MACD_SCALE_KEY)
                            ),
                            onSeriesCreated = { macdHistogramSeriesApi = it }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#2962FF")),
                                lineWidth = LineWidth.ONE,
                                priceScaleId = PriceScaleId(MACD_SCALE_KEY),
                                lastValueVisible = true,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = {
                                macdLineSeriesApi = it
                                it.priceScale().applyOptions(
                                    PriceScaleOptions(
                                        autoScale = true,
                                        scaleMargins = macdScaleMargins,
                                        visible = showMacd,
                                        borderVisible = false,
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        entireTextOnly = true,
                                        alignLabels = true,
                                        ticksVisible = false
                                    )
                                )
                            }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(AndroidColor.parseColor("#FF6D00")),
                                lineWidth = LineWidth.ONE,
                                priceScaleId = PriceScaleId(MACD_SCALE_KEY),
                                lastValueVisible = false,
                                priceLineVisible = false
                            ),
                            onSeriesCreated = { macdSignalSeriesApi = it }
                        )

                        api.addHistogramSeries(
                            options = HistogramSeriesOptions(
                                lastValueVisible = false,
                                priceLineVisible = false,
                                base = 0f,
                                priceFormat = PriceFormat.priceFormatBuiltIn(type = PriceFormat.Type.VOLUME, precision = 0, minMove = 1f),
                                priceScaleId = PriceScaleId(VOLUME_SCALE_KEY)
                            ),
                            onSeriesCreated = {
                                volumeSeriesApi = it
                                it.priceScale().applyOptions(
                                    PriceScaleOptions(
                                        autoScale = true,
                                        scaleMargins = volumeScaleMargins,
                                        visible = false,
                                        borderVisible = false
                                    )
                                )
                            }
                        )

                        api.addLineSeries(
                            options = LineSeriesOptions(
                                color = IntColor(volumeMaColor.toArgb()),
                                lineWidth = LineWidth.ONE,
                                priceScaleId = PriceScaleId(VOLUME_SCALE_KEY),
                                priceLineVisible = false,
                                lastValueVisible = false,
                                crosshairMarkerVisible = false
                            ),
                            onSeriesCreated = { volumeMaSeriesApi = it }
                        )

                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { chartsView ->
                    chartsViewApi = null
                    resetChartSeriesHandles()
                    (chartsView as? android.webkit.WebView)?.destroy()
                },
                update = { chartsView ->
                    val scales = chartSettings.scales
                    val activeScaleMode = toPriceScaleMode(scales.scaleType)
                    val autoScaleEnabled = scales.autoScale && !scales.lockRatio
                    val useLeftPriceScale = scales.scalesPlacement == "Left"

                    chartsView.api.applyOptions {
                        layout = LayoutOptions(
                            background = SolidColor(color = IntColor(AndroidColor.TRANSPARENT)),
                            textColor = chartSettings.canvas.scaleTextColor.toIntColor(),
                            fontSize = chartSettings.canvas.scaleFontSize
                        )
                        grid = GridOptions(
                            vertLines = GridLineOptions(
                                color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.gridColor), chartSettings.canvas.gridOpacity)),
                                visible = chartSettings.canvas.gridVisible && chartSettings.canvas.gridType in listOf("Vert and horz", "Vert")
                            ),
                            horzLines = GridLineOptions(
                                color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.horzGridColor), chartSettings.canvas.gridOpacity)),
                                visible = chartSettings.canvas.gridVisible && chartSettings.canvas.gridType in listOf("Vert and horz", "Horz")
                            )
                        )
                        crosshair = CrosshairOptions(
                            vertLine = CrosshairLineOptions(
                                color = chartSettings.canvas.crosshairColor.toIntColor(),
                                width = chartSettings.canvas.crosshairThickness.toLineWidth(),
                                style = chartSettings.canvas.crosshairLineStyle.toLineStyle()
                            ),
                            horzLine = CrosshairLineOptions(
                                color = chartSettings.canvas.crosshairColor.toIntColor(),
                                width = chartSettings.canvas.crosshairThickness.toLineWidth(),
                                style = chartSettings.canvas.crosshairLineStyle.toLineStyle()
                            )
                        )
                        rightPriceScale = PriceScaleOptions(
                            borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                            entireTextOnly = false,
                            autoScale = autoScaleEnabled,
                            mode = activeScaleMode,
                            invertScale = scales.invertScale,
                            position = PriceAxisPosition.RIGHT,
                            visible = !useLeftPriceScale
                        )
                        leftPriceScale = PriceScaleOptions(
                            borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                            entireTextOnly = false,
                            autoScale = autoScaleEnabled,
                            mode = activeScaleMode,
                            invertScale = scales.invertScale,
                            position = PriceAxisPosition.LEFT,
                            visible = useLeftPriceScale
                        )
                        timeScale = TimeScaleOptions(
                            borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                            // Hide the main time axis when the dedicated RSI pane owns it (TV-style)
                            visible = !showInlineRsiPane,
                            timeVisible = true,
                            rightOffset = 15f,
                            barSpacing = 6f
                        )
                        handleScroll = HandleScrollOptions(
                            pressedMouseMove = true,
                            horzTouchDrag = true,
                            vertTouchDrag = true
                        )
                        handleScale = HandleScaleOptions(
                            mouseWheel = true,
                            pinch = true,
                            axisPressedMouseMove = AxisPressedMouseMoveOptions(
                                time = !scales.scalePriceChartOnly,
                                price = true
                            )
                        )
                        kineticScroll = KineticScrollOptions(
                            touch = true,
                            mouse = true
                        )
                    }

                    // Apply series-specific options
                    val priceLineVisible = chartSettings.scales.symbolLastPriceLine
                    val lastValueVisible = chartSettings.scales.symbolLastPriceLabel

                    val uppercaseSymbol = symbol.uppercase()
                    val isBitcoin = uppercaseSymbol.contains("BTC") || uppercaseSymbol.contains("BITCOIN")
                    val isForex = uppercaseSymbol.length == 6 || uppercaseSymbol.contains("/")
                    val precision = when {
                        isBitcoin -> 0
                        isForex -> 5
                        else -> 2
                    }
                    val minMove = when {
                        isBitcoin -> 1f
                        isForex -> 0.00001f
                        else -> 0.01f
                    }
                    val mainPriceFormat = PriceFormat.priceFormatBuiltIn(
                        type = PriceFormat.Type.PRICE,
                        precision = precision,
                        minMove = minMove.toFloat()
                    )

                    seriesApi?.let { api ->
                        when (style) {
                            "bars" -> api.applyOptions(BarSeriesOptions(priceFormat = mainPriceFormat, priceLineVisible = priceLineVisible, lastValueVisible = lastValueVisible))
                            "line" -> api.applyOptions(LineSeriesOptions(priceFormat = mainPriceFormat, priceLineVisible = priceLineVisible, lastValueVisible = lastValueVisible))
                            "area" -> api.applyOptions(AreaSeriesOptions(priceFormat = mainPriceFormat, priceLineVisible = priceLineVisible, lastValueVisible = lastValueVisible))
                            "heikin_ashi", "candles" -> api.applyOptions(CandlestickSeriesOptions(priceFormat = mainPriceFormat, priceLineVisible = priceLineVisible, lastValueVisible = lastValueVisible))
                            else -> api.applyOptions(CandlestickSeriesOptions(priceFormat = mainPriceFormat, priceLineVisible = priceLineVisible, lastValueVisible = lastValueVisible))
                        }
                    }
                }
            )
                }
                }

                if (showInlineRsiPane) {
                    // Divider line between price chart and RSI pane (TV-style pane separation)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ComposeColor(AndroidColor.parseColor(chartSettings.canvas.scaleLineColor)))
                    )
                    // Dedicated RSI pane - a physically separate ChartsView with its own
                    // canvas, its own 0-100 price scale and the time axis at the bottom.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(RSI_PANE_SPLIT_FRACTION)
                    ) {
                    AndroidView(
                        factory = { context ->
                            ChartsView(context).apply {
                                rsiChartsViewApi = this
                                api.applyOptions {
                                    layout = LayoutOptions(
                                        background = SolidColor(color = IntColor(chartBgColor)),
                                        textColor = chartSettings.canvas.scaleTextColor.toIntColor(),
                                        fontSize = chartSettings.canvas.scaleFontSize
                                    )
                                    grid = GridOptions(
                                        vertLines = GridLineOptions(
                                            color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.gridColor), chartSettings.canvas.gridOpacity)),
                                            visible = chartSettings.canvas.gridVisible && chartSettings.canvas.gridType in listOf("Vert and horz", "Vert")
                                        ),
                                        horzLines = GridLineOptions(
                                            color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.horzGridColor), chartSettings.canvas.gridOpacity)),
                                            visible = chartSettings.canvas.gridVisible && chartSettings.canvas.gridType in listOf("Vert and horz", "Horz")
                                        )
                                    )
                                    crosshair = CrosshairOptions(
                                        mode = CrosshairMode.NORMAL,
                                        vertLine = CrosshairLineOptions(
                                            color = chartSettings.canvas.crosshairColor.toIntColor(),
                                            width = chartSettings.canvas.crosshairThickness.toLineWidth(),
                                            style = chartSettings.canvas.crosshairLineStyle.toLineStyle()
                                        ),
                                        horzLine = CrosshairLineOptions(
                                            color = IntColor(applyOpacity(AndroidColor.parseColor(chartSettings.canvas.crosshairColor), 40)),
                                            labelVisible = false
                                        )
                                    )
                                    rightPriceScale = PriceScaleOptions(
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        visible = chartSettings.scales.scalesPlacement != "Left",
                                        entireTextOnly = true,
                                        alignLabels = true
                                    )
                                    leftPriceScale = PriceScaleOptions(
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        visible = chartSettings.scales.scalesPlacement == "Left"
                                    )
                                    timeScale = TimeScaleOptions(
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        visible = true,
                                        timeVisible = true,
                                        rightOffset = 15f,
                                        barSpacing = 6f
                                    )
                                    handleScroll = HandleScrollOptions(
                                        pressedMouseMove = true,
                                        horzTouchDrag = true,
                                        vertTouchDrag = false
                                    )
                                    handleScale = HandleScaleOptions(
                                        mouseWheel = true,
                                        pinch = true,
                                        axisPressedMouseMove = AxisPressedMouseMoveOptions(
                                            time = !chartSettings.scales.scalePriceChartOnly,
                                            price = false
                                        )
                                    )
                                    kineticScroll = KineticScrollOptions(
                                        touch = true,
                                        mouse = true
                                    )
                                }
                                createInlineRsiPaneSeries(
                                    chartsView = this,
                                    refs = rsiPaneRefs,
                                    scaleMargins = PriceScaleMargins(top = 0.08f, bottom = 0.08f),
                                    borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                    visible = true,
                                    maskColor = IntColor(chartBgColor)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { view ->
                            if (rsiChartsViewApi == view) {
                                rsiChartsViewApi = null
                                rsiPaneRefs.clear()
                            }
                            (view as? android.webkit.WebView)?.destroy()
                        },
                        update = { view ->
                            view.api.applyOptions {
                                layout = LayoutOptions(
                                    background = SolidColor(color = IntColor(chartBgColor)),
                                    textColor = chartSettings.canvas.scaleTextColor.toIntColor(),
                                    fontSize = chartSettings.canvas.scaleFontSize
                                )
                                rightPriceScale = rightPriceScale?.copy(borderColor = chartSettings.canvas.scaleLineColor.toIntColor())
                                    ?: PriceScaleOptions(
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        visible = chartSettings.scales.scalesPlacement != "Left"
                                    )
                                leftPriceScale = leftPriceScale?.copy(borderColor = chartSettings.canvas.scaleLineColor.toIntColor())
                                    ?: PriceScaleOptions(
                                        borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                        visible = chartSettings.scales.scalesPlacement == "Left"
                                    )
                                timeScale = timeScale?.copy(borderColor = chartSettings.canvas.scaleLineColor.toIntColor())
                                    ?: TimeScaleOptions(borderColor = chartSettings.canvas.scaleLineColor.toIntColor())
                            }
                            rsiPaneRefs.priceScaleOwner()?.priceScale()?.applyOptions(
                                PriceScaleOptions(
                                    autoScale = true,
                                    scaleMargins = PriceScaleMargins(top = 0.08f, bottom = 0.08f)
                                )
                            )
                        }
                    )

                    RsiPaneOverlay(
                        visible = true,
                        data = rsiDataState,
                        rsiPeriod = rsiPeriod,
                        scaleTextColor = chartSettings.canvas.scaleTextColor,
                        scaleBorderColor = chartSettings.canvas.scaleLineColor,
                        scaleFontSize = chartSettings.canvas.scaleFontSize,
                        axisWidthPx = mainPriceScaleWidthPx,
                        crosshairRsiValue = rsiPaneRefs.crosshairRsiValue,
                        crosshairMaValue = rsiPaneRefs.crosshairMaValue,
                        scalesPlacement = chartSettings.scales.scalesPlacement
                    )
                    }
                }
            }
            }
        if (showCurrencySelector) {
            Box(Modifier.align(Alignment.TopEnd)) {
                CurrencySelectorChip(
                    showCurrencySelector = showCurrencySelector,
                    selectedCurrency = selectedCurrency,
                    onCurrencyClick = onCurrencyClick
                )
            }
        }

        if (chartSettings.scales.countdown && currentQuoteState != null) {
            Box(Modifier.align(Alignment.CenterEnd)) {
                ChartCountdownOverlay(
                    visible = chartSettings.scales.countdown,
                    currentQuoteState = currentQuoteState,
                    timeframe = timeframe,
                    symbol = symbol
                )
            }
        }

        // Overlay UI (Top Left Status Line)
        Column(
            modifier = Modifier
                .padding(
                    start = 4.dp,
                    top = 4.dp,
                    end = chartSettings.canvas.marginRight.dp,
                    bottom = chartSettings.canvas.marginBottom.dp
                )
                .align(Alignment.TopStart)
        ) {
            StatusLineHeader(
                chartSettings = chartSettings,
                symbol = symbol,
                currentQuoteState = currentQuoteState,
                onMarketStatusClick = { showMarketStatus = true }
            )
                
                val indicatorRows: @Composable () -> Unit = {
                    if (showVolume) {
                        IndicatorStatusItem(
                            label = "Vol Â· Ticks",
                            color = ComposeColor(0xFF787B86),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "Volume",
                            isMoreSelected = indicatorMoreMenuTarget == "Volume",
                            isHidden = "Volume" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "Volume") null else "Volume") },
                            onHide = { onIndicatorHide("Volume") },
                            onSettings = { onIndicatorSettingsClick("Volume") },
                            onRemove = { onVolumeToggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "Volume"
                                showIndicatorMoreMenu = true
                            },
                            extraContent = if (showVolumeMa) {
                                {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = volumeMaDataState.lastOrNull()?.let { String.format("%,.0f", it) } ?: "",
                                        color = volumeMaColor,
                                        fontSize = 13.sp
                                    )
                                }
} else null
                        )
                    }

                    if (showEma10) {
                        IndicatorStatusItem(
                            label = "EMA $ema10Period close",
                            color = ComposeColor.White,
                            value = ohlcData.lastOrNull()?.close,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "EMA 10",
                            isMoreSelected = indicatorMoreMenuTarget == "EMA 10",
                            isHidden = "EMA 10" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "EMA 10") null else "EMA 10") },
                            onHide = { onIndicatorHide("EMA 10") },
                            onSettings = { onIndicatorSettingsClick("EMA 10") },
                            onRemove = { onEma10Toggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "EMA 10"
                                showIndicatorMoreMenu = true
                            }
                        )
                    }
        if (showEma20 && "EMA 20" !in hiddenIndicators) {
                        IndicatorStatusItem(
                            label = "EMA $ema20Period close",
                            color = ComposeColor.White,
                            value = ohlcData.lastOrNull()?.close,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "EMA 20",
                            isMoreSelected = indicatorMoreMenuTarget == "EMA 20",
                            isHidden = "EMA 20" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "EMA 20") null else "EMA 20") },
                            onHide = { onIndicatorHide("EMA 20") },
                            onSettings = { onIndicatorSettingsClick("EMA 20") },
                            onRemove = { onEma20Toggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "EMA 20"
                                showIndicatorMoreMenu = true
                            }
                        )
                    }
                    if (showSma1) {
                        IndicatorStatusItem(
                            label = "SMA $sma1Period close",
                            color = ComposeColor.White,
                            value = ohlcData.lastOrNull()?.close,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "SMA 1",
                            isMoreSelected = indicatorMoreMenuTarget == "SMA 1",
                            isHidden = "SMA 1" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "SMA 1") null else "SMA 1") },
                            onHide = { onIndicatorHide("SMA 1") },
                            onSettings = { onIndicatorSettingsClick("SMA 1") },
                            onRemove = { onSma1Toggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "SMA 1"
                                showIndicatorMoreMenu = true
                            }
                        )
                    }
                    if (showSma2) {
                        IndicatorStatusItem(
                            label = "SMA $sma2Period close",
                            color = ComposeColor.White,
                            value = ohlcData.lastOrNull()?.close,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "SMA 2",
                            isMoreSelected = indicatorMoreMenuTarget == "SMA 2",
                            isHidden = "SMA 2" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "SMA 2") null else "SMA 2") },
                            onHide = { onIndicatorHide("SMA 2") },
                            onSettings = { onIndicatorSettingsClick("SMA 2") },
                            onRemove = { onSma2Toggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "SMA 2"
                                showIndicatorMoreMenu = true
                            }
                        )
                    }

                    if (showBb) {
                        IndicatorStatusItem(
                            label = "BB $bbPeriod SMA close ${formatBandMultiplier(bbStdDev)}",
                            color = ComposeColor(0xFF787B86),
                            value = null, // Custom Row logic below for BB values
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "BB",
                            isMoreSelected = indicatorMoreMenuTarget == "BB",
                            isHidden = "BB" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "BB") null else "BB") },
                            onHide = { onIndicatorHide("BB") },
                            onSettings = { onIndicatorSettingsClick("BB") },
                            onRemove = { onBbToggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "BB"
                                showIndicatorMoreMenu = true
                            },
                            extraContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    bbDataState.latestMiddleBand?.let { middleBand ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = formatPrice(middleBand, symbol), color = ComposeColor(0xFF2196F3), fontSize = 13.sp)
                                    }
                                    bbDataState.latestUpperBand?.let { upperBand ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = formatPrice(upperBand, symbol), color = ComposeColor(0xFFF23645), fontSize = 13.sp)
                                    }
                                    bbDataState.latestLowerBand?.let { lowerBand ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = formatPrice(lowerBand, symbol), color = ComposeColor(0xFF00BFA5), fontSize = 13.sp)
                                    }
                                }
                            }
                        )
                    }

                    if (showVwap) {
                        IndicatorStatusItem(
                            label = "VWAP hlc3 Session",
                            color = ComposeColor(0xFF787B86),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "VWAP",
                            isMoreSelected = indicatorMoreMenuTarget == "VWAP",
                            isHidden = "VWAP" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "VWAP") null else "VWAP") },
                            onHide = { onIndicatorHide("VWAP") },
                            onSettings = { onIndicatorSettingsClick("VWAP") },
                            onRemove = { onVwapToggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "VWAP"
                                showIndicatorMoreMenu = true
                            },
                            extraContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    vwapDataState.latestVwap?.let { latestVwap ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = formatPrice(latestVwap, symbol), color = ComposeColor(0xFF2962FF), fontSize = 13.sp)
                                    }
                                    vwapDataState.latestUpperBand?.let { upperBand ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = formatPrice(upperBand, symbol), color = ComposeColor(0xFF4CAF50), fontSize = 13.sp)
                                    }
                                    vwapDataState.latestLowerBand?.let { lowerBand ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = formatPrice(lowerBand, symbol), color = ComposeColor(0xFF4CAF50), fontSize = 13.sp)
                                    }
                                }
                            }
                        )
                    }

                    if (showRsi) {
                        IndicatorStatusItem(
                            label = "RSI $rsiPeriod close",
                            color = ComposeColor(0xFF7E57C2),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "RSI",
                            isMoreSelected = indicatorMoreMenuTarget == "RSI",
                            isHidden = "RSI" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "RSI") null else "RSI") },
                            onHide = { onIndicatorHide("RSI") },
                            onSettings = { onIndicatorSettingsClick("RSI") },
                            onRemove = { onRsiToggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "RSI"
                                showIndicatorMoreMenu = true
                            },
                            extraContent = {
                                rsiDataState.latestValue?.let { latestRsi ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = String.format("%.2f", latestRsi), color = ComposeColor(0xFF7E57C2), fontSize = 13.sp)
                                }
                            }
                        )
                    }

                    if (showAtr) {
                        IndicatorStatusItem(
                            label = "ATR $atrPeriod",
                            color = ComposeColor(0xFFF44336),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "ATR",
                            isMoreSelected = indicatorMoreMenuTarget == "ATR",
                            isHidden = "ATR" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "ATR") null else "ATR") },
                            onHide = { onIndicatorHide("ATR") },
                            onSettings = { onIndicatorSettingsClick("ATR") },
                            onRemove = { onAtrToggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "ATR"
                                showIndicatorMoreMenu = true
                            },
                            extraContent = {
                                atrDataState.lastOrNull()?.let { latestAtr ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = String.format("%.4f", latestAtr), color = ComposeColor(0xFFF44336), fontSize = 13.sp)
                                }
                            }
                        )
                    }

                    if (showMacd) {
                        IndicatorStatusItem(
                            label = "MACD $macdFast $macdSlow close EMA $macdSignal",
                            color = ComposeColor(0xFF787B86),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "MACD",
                            isMoreSelected = indicatorMoreMenuTarget == "MACD",
                            isHidden = "MACD" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "MACD") null else "MACD") },
                            onHide = { onIndicatorHide("MACD") },
                            onSettings = { onIndicatorSettingsClick("MACD") },
                            onRemove = { onMacdToggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "MACD"
                                showIndicatorMoreMenu = true
                            },
                            extraContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    macdDataState.first.lastOrNull()?.let { latestMacd ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = String.format("%.4f", latestMacd), color = ComposeColor(0xFF2962FF), fontSize = 13.sp)
                                    }
                                    macdDataState.second.lastOrNull()?.let { latestSignal ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = String.format("%.4f", latestSignal), color = ComposeColor(0xFFFF6D00), fontSize = 13.sp)
                                    }
                                    macdDataState.third.lastOrNull()?.let { latestHistogram ->
                                        val color = if (latestHistogram >= 0f) ComposeColor(0xFF26A69A) else ComposeColor(0xFFEF5350)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = String.format("%.4f", latestHistogram), color = color, fontSize = 13.sp)
                                    }
                                }
                            }
                        )
                    }

                    // Editors' picks status chips (extracted into OverlayStatusChips to keep TradingChart under the JVM 64KB method limit)
                    OverlayStatusChips(
                        symbol = symbol,
                        selectedIndicatorId = selectedIndicatorId,
                        indicatorMoreMenuTarget = indicatorMoreMenuTarget,
                        hiddenIndicators = hiddenIndicators,
                        showPremiumDiscount = showPremiumDiscount,
                        showFairValueGap = showFairValueGap,
                        showSupplyDemandDaily = showSupplyDemandDaily,
                        showOteVisibleChart = showOteVisibleChart,
                        showLiquidityDeltaProfiler = showLiquidityDeltaProfiler,
                        showEqhEqlLiquidityZones = showEqhEqlLiquidityZones,
                        showPowerHourBreakout = showPowerHourBreakout,
                        showTrendlineBreakouts = showTrendlineBreakouts,
                        showTrendlineNavigator = showTrendlineNavigator,
                        showLiquidityPools = showLiquidityPools,
                        showOrderBlockBreaker = showOrderBlockBreaker,
                        autoFibEnabled = autoFibEnabled,
                        showAutoFib = showAutoFib,
                        confluenceFvgEnabled = confluenceFvgEnabled,
                        showConfluenceFvg = showConfluenceFvg,
                        onSelectedIndicatorIdChange = onSelectedIndicatorIdChange,
                        onIndicatorHide = onIndicatorHide,
                        onPremiumDiscountToggle = onPremiumDiscountToggle,
                        onFairValueGapToggle = onFairValueGapToggle,
                        onFvgSettingsClick = onFvgSettingsClick,
                        onSupplyDemandDailyToggle = onSupplyDemandDailyToggle,
                        onSdVrSettingsClick = onSdVrSettingsClick,
                        onOteVisibleChartToggle = onOteVisibleChartToggle,
                        onLdpSettingsClick = onLdpSettingsClick,
                        onLiquidityDeltaProfilerToggle = onLiquidityDeltaProfilerToggle,
                        onEqhEqlSettingsClick = onEqhEqlSettingsClick,
                        onEqhEqlLiquidityZonesToggle = onEqhEqlLiquidityZonesToggle,
                        onPowerHourSettingsClick = onPowerHourSettingsClick,
                        onPowerHourBreakoutToggle = onPowerHourBreakoutToggle,
                        onTrendlineSettingsClick = onTrendlineSettingsClick,
                        onTrendlineBreakoutsToggle = onTrendlineBreakoutsToggle,
                        onNavigatorSettingsClick = onNavigatorSettingsClick,
                        onTrendlineNavigatorToggle = onTrendlineNavigatorToggle,
                        onLiquidityPoolsSettingsClick = onLiquidityPoolsSettingsClick,
                        onLiquidityPoolsToggle = onLiquidityPoolsToggle,
                        onObbSettingsClick = onObbSettingsClick,
                        onOrderBlockBreakerToggle = onOrderBlockBreakerToggle,
                        showVolumaticFvg = showVolumaticFvg,
                        vfvgBullCount = vfvgBullCount,
                        vfvgBearCount = vfvgBearCount,
                        onVolumaticFvgSettingsClick = onVolumaticFvgSettingsClick,
                        onVolumaticFvgToggle = onVolumaticFvgToggle,
                        onAutoFibHide = onAutoFibHide,
                        onAutoFibSettingsClick = onAutoFibSettingsClick,
                        onAutoFibToggle = onAutoFibToggle,
                        onConfluenceFvgHide = onConfluenceFvgHide,
                        onCfvgSettingsClick = onCfvgSettingsClick,
                        onConfluenceFvgToggle = onConfluenceFvgToggle,
                        onShowMoreMenu = { indicatorMoreMenuTarget = it; showIndicatorMoreMenu = true }
                    )
                }
                if (showIndicatorsList) {
                    indicatorRows()
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ComposeColor(0xFF131722))
                        .border(1.dp, ComposeColor(0xFF363A45), RoundedCornerShape(4.dp))
                        .clickable { showIndicatorsList = !showIndicatorsList },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showIndicatorsList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Indicators",
                        tint = ComposeColor(0xFFD1D4DC),
                        modifier = Modifier.size(16.dp)
                    )
                }

                }

        if (showIndicatorMoreMenu && indicatorMoreMenuTarget != null) {
            val moreTarget = indicatorMoreMenuTarget!!
            IndicatorMoreMenu(
                label = moreTarget,
                onDismiss = {
                    showIndicatorMoreMenu = false
                    indicatorMoreMenuTarget = null
                },
                onRemove = {
                    when (moreTarget) {
                        "Volume" -> onVolumeToggle(false)
                        "EMA 10" -> onEma10Toggle(false)
                        "EMA 20" -> onEma20Toggle(false)
                        "SMA 1" -> onSma1Toggle(false)
                        "SMA 2" -> onSma2Toggle(false)
                        "BB" -> onBbToggle(false)
                        "VWAP" -> onVwapToggle(false)
                        "RSI" -> onRsiToggle(false)
                        "ATR" -> onAtrToggle(false)
                        "MACD" -> onMacdToggle(false)
                        "PREMIUM_DISCOUNT" -> onPremiumDiscountToggle(false)
                        "FAIR_VALUE_GAP" -> onFairValueGapToggle(false)
                        "SUPPLY_DEMAND_DAILY" -> onSupplyDemandDailyToggle(false)
                        "OTE_VISIBLE_CHART" -> onOteVisibleChartToggle(false)
                        "LIQUIDITY_DELTA_PROFILER" -> onLiquidityDeltaProfilerToggle(false)
                        "EQH_EQL_LIQUIDITY_ZONES" -> onEqhEqlLiquidityZonesToggle(false)
                        "POWER_HOUR_BREAKOUT" -> onPowerHourBreakoutToggle(false)
                        "TRENDLINE_BREAKOUTS" -> onTrendlineBreakoutsToggle(false)
                        "TRENDLINE_NAVIGATOR" -> onTrendlineNavigatorToggle(false)
                        "LIQUIDITY_POOLS" -> onLiquidityPoolsToggle(false)
                        "ORDER_BLOCK_BREAKER" -> onOrderBlockBreakerToggle(false)
                        "VOLUMATIC_FVG" -> onVolumaticFvgToggle(false)
                        "AUTO_FIB_RETRACEMENT" -> onAutoFibToggle(false)
                        "CONFLUENCE_FVG" -> onConfluenceFvgToggle(false)
                    }
                    if (selectedIndicatorId == moreTarget) onSelectedIndicatorIdChange(null)
                }
            )
        }

        if (showSettingsButton) {
            // Settings Button (Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = 60.dp, height = 34.dp)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Chart Settings",
                    tint = ComposeColor(0xFFD1D4DC),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showMarketStatus) {
            MarketStatusModal(
                symbol = symbol,
                selectedTimeZone = selectedTimeZone,
                onDismiss = { showMarketStatus = false }
            )
        }
    }
}

private fun trimRatio(r: Float): String =
    if (r == kotlin.math.floor(r)) r.toLong().toString() else r.toString()

private fun formatPrice(price: Float, symbol: String = ""): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    symbols.groupingSeparator = ','
    val uppercaseSymbol = symbol.uppercase()
    val group = com.asc.markets.data.trainedAssetGroup(symbol)
    val isBitcoin = (group == null) && (uppercaseSymbol.contains("BTC") || uppercaseSymbol.contains("BITCOIN"))
    val isForex = (group == null) && (uppercaseSymbol.length == 6 || uppercaseSymbol.contains("/"))

    val pattern = when {
        group == "crypto" || isBitcoin -> "#,##0.00"
        group == "forex" || isForex -> "#,##0.00000"
        else -> "#,##0.##"
    }

    val df = DecimalFormat(pattern, symbols)
    return df.format(price)
}

private fun parseAlertColor(hex: String): ComposeColor {
    return try {
        ComposeColor(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        ComposeColor.Gray
    }
}

private fun formatBandMultiplier(multiplier: Float): String {
    return if (multiplier % 1f == 0f) {
        multiplier.toInt().toString()
    } else {
        DecimalFormat("#.##", DecimalFormatSymbols(Locale.US)).format(multiplier)
    }
}

@Composable
fun OhlcItem(label: String, value: Float, symbol: String) {
    Row(modifier = Modifier.padding(end = 8.dp)) {
        Text(text = "$label ", color = ComposeColor(0xFF787B86), fontSize = 13.sp)
        Text(text = formatPrice(value, symbol), color = ComposeColor(0xFFD1D4DC), fontSize = 13.sp)
    }
}

@Composable
fun IndicatorStatusItem(
    label: String,
    color: ComposeColor,
    value: Float?,
    symbol: String,
    isSelected: Boolean = false,
    isMoreSelected: Boolean = false,
    isHidden: Boolean = false,
    onClick: () -> Unit = {},
    onHide: () -> Unit = {},
    onSettings: () -> Unit = {},
    onRemove: () -> Unit = {},
    onMore: () -> Unit = {},
    extraContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .padding(top = 1.dp)
            .then(
                if (isSelected) Modifier
                    .border(1.dp, ComposeColor(0xFF2962FF), RoundedCornerShape(4.dp))
                    .background(ComposeColor(0x1A2962FF), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 1.dp)
                else Modifier.clickable { onClick() }
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = ComposeColor(0xFF787B86),
                fontSize = 13.sp
            )
            
            if (!isSelected) {
                value?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatPrice(it, symbol),
                        color = color,
                        fontSize = 13.sp
                    )
                }
                extraContent?.invoke()
            } else {
                val iconColor = ComposeColor(0xFFD1D4DC)
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isHidden) "Show" else "Hide",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp).clickable { onHide() }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp).clickable { onSettings() }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp).clickable { onRemove() }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Box(
                        modifier = Modifier
                            .size(height = 24.dp, width = 36.dp)
                            .then(
                                if (isMoreSelected) Modifier
                                    .background(ComposeColor(0xFF363A45), RoundedCornerShape(4.dp))
                                else Modifier
                            )
                            .clickable { onMore() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartNotificationItem(
    label: String,
    value: String? = null,
    backgroundColor: ComposeColor = ComposeColor(0xFF131722),
    valueColor: ComposeColor = ComposeColor(0xFFD1D4DC)
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = ComposeColor(0xFF787B86),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            if (value != null) {
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorMoreMenu(
    label: String,
    onDismiss: () -> Unit,
    onRemove: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ComposeColor(0xFF212121),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(ComposeColor(0xFF363A45), RoundedCornerShape(2.dp))
            )
        },
        contentWindowInsets = { WindowInsets(0) },
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            MoreMenuItem(
                icon = Icons.Default.AddAlert,
                text = "Add alert on $label...",
                onClick = onDismiss
            )
            MoreMenuItem(
                icon = Icons.Default.BarChart,
                text = "Add indicator/strategy on $label...",
                onClick = onDismiss
            )
            MoreMenuItem(
                icon = Icons.Outlined.StarBorder,
                text = "Add this indicator to favorites",
                onClick = onDismiss
            )
            
            Divider(color = ComposeColor(0xFF363A45), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            
            MoreMenuItem(
                icon = Icons.Default.Layers,
                text = "Visual order",
                showArrow = true,
                onClick = onDismiss
            )
            MoreMenuItem(
                text = "Visibility on intervals",
                showArrow = true,
                onClick = onDismiss
            )
            MoreMenuItem(
                icon = Icons.Default.UnfoldMore,
                text = "Move to",
                showArrow = true,
                onClick = onDismiss
            )
            MoreMenuItem(
                icon = Icons.Default.AlignHorizontalRight,
                text = "Pin to scale (now right)",
                showArrow = true,
                onClick = onDismiss
            )
            
            Divider(color = ComposeColor(0xFF363A45), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            MoreMenuItem(
                icon = Icons.Default.Delete,
                text = "Remove indicator",
                onClick = {
                    onDismiss()
                    onRemove()
                }
            )
            MoreMenuItem(
                text = "About this script...",
                onClick = onDismiss
            )
            MoreMenuItem(
                text = "Copy",
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ComposeColor(0xFFD1D4DC),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        
        Text(
            text = text,
            color = ComposeColor(0xFFD1D4DC),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ComposeColor(0xFF787B86),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OverlayStatusChips(
    symbol: String,
    selectedIndicatorId: String?,
    indicatorMoreMenuTarget: String?,
    hiddenIndicators: Set<String>,
    showPremiumDiscount: Boolean,
    showFairValueGap: Boolean,
    showSupplyDemandDaily: Boolean,
    showOteVisibleChart: Boolean,
    showLiquidityDeltaProfiler: Boolean,
    showEqhEqlLiquidityZones: Boolean,
    showPowerHourBreakout: Boolean,
    showTrendlineBreakouts: Boolean,
    showTrendlineNavigator: Boolean,
    showLiquidityPools: Boolean,
    showOrderBlockBreaker: Boolean,
    autoFibEnabled: Boolean,
    showAutoFib: Boolean,
    confluenceFvgEnabled: Boolean,
    showConfluenceFvg: Boolean,
    onSelectedIndicatorIdChange: (String?) -> Unit,
    onIndicatorHide: (String) -> Unit,
    onPremiumDiscountToggle: (Boolean) -> Unit,
    onFairValueGapToggle: (Boolean) -> Unit,
    onFvgSettingsClick: () -> Unit,
    onSupplyDemandDailyToggle: (Boolean) -> Unit,
    onSdVrSettingsClick: () -> Unit,
    onOteVisibleChartToggle: (Boolean) -> Unit,
    onLdpSettingsClick: () -> Unit,
    onLiquidityDeltaProfilerToggle: (Boolean) -> Unit,
    onEqhEqlSettingsClick: () -> Unit,
    onEqhEqlLiquidityZonesToggle: (Boolean) -> Unit,
    onPowerHourSettingsClick: () -> Unit,
    onPowerHourBreakoutToggle: (Boolean) -> Unit,
    onTrendlineSettingsClick: () -> Unit,
    onTrendlineBreakoutsToggle: (Boolean) -> Unit,
    onNavigatorSettingsClick: () -> Unit,
    onTrendlineNavigatorToggle: (Boolean) -> Unit,
    onLiquidityPoolsSettingsClick: () -> Unit,
    onLiquidityPoolsToggle: (Boolean) -> Unit,
    onObbSettingsClick: () -> Unit,
    onOrderBlockBreakerToggle: (Boolean) -> Unit,
    showVolumaticFvg: Boolean,
    vfvgBullCount: Int,
    vfvgBearCount: Int,
    onVolumaticFvgSettingsClick: () -> Unit,
    onVolumaticFvgToggle: (Boolean) -> Unit,
    onAutoFibHide: (Boolean) -> Unit,
    onAutoFibSettingsClick: () -> Unit,
    onAutoFibToggle: (Boolean) -> Unit,
    onConfluenceFvgHide: (Boolean) -> Unit,
    onCfvgSettingsClick: () -> Unit,
    onConfluenceFvgToggle: (Boolean) -> Unit,
    onShowMoreMenu: (String) -> Unit
) {
                    // Editors' picks overlays in the indicator-name legend
                    if (showPremiumDiscount) {
                        IndicatorStatusItem(
                            label = "Premium & Discount Delta Volume [BigBeluga]",
                            color = ComposeColor(0xFF79C1F1),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "PREMIUM_DISCOUNT",
                            isMoreSelected = indicatorMoreMenuTarget == "PREMIUM_DISCOUNT",
                            isHidden = "PREMIUM_DISCOUNT" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "PREMIUM_DISCOUNT") null else "PREMIUM_DISCOUNT") },
                            onHide = { onIndicatorHide("PREMIUM_DISCOUNT") },
                            onSettings = { },
                            onRemove = { onPremiumDiscountToggle(false) },
                            onMore = {
                                onShowMoreMenu("PREMIUM_DISCOUNT")
                            },
                            extraContent = {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "ΔVol boxes", color = ComposeColor(0xFF79C1F1), fontSize = 13.sp)
                            }
                        )
                    }
                    if (showFairValueGap) {
                        IndicatorStatusItem(
                            label = "Fair Value Gap [LuxAlgo]",
                            color = ComposeColor(0xFF089981),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "FAIR_VALUE_GAP",
                            isMoreSelected = indicatorMoreMenuTarget == "FAIR_VALUE_GAP",
                            isHidden = "FAIR_VALUE_GAP" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "FAIR_VALUE_GAP") null else "FAIR_VALUE_GAP") },
                            onHide = { onIndicatorHide("FAIR_VALUE_GAP") },
                            onSettings = { onFvgSettingsClick() },
                            onRemove = { onFairValueGapToggle(false) },
                            onMore = {
                                onShowMoreMenu("FAIR_VALUE_GAP")
                            }
                        )
                    }
                    if (showSupplyDemandDaily) {
                        IndicatorStatusItem(
                            label = "Supply & Demand VR [LuxAlgo]",
                            color = ComposeColor(0xFFFF5D00),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "SUPPLY_DEMAND_DAILY",
                            isMoreSelected = indicatorMoreMenuTarget == "SUPPLY_DEMAND_DAILY",
                            isHidden = "SUPPLY_DEMAND_DAILY" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "SUPPLY_DEMAND_DAILY") null else "SUPPLY_DEMAND_DAILY") },
                            onHide = { onIndicatorHide("SUPPLY_DEMAND_DAILY") },
                            onSettings = { onSdVrSettingsClick() },
                            onRemove = { onSupplyDemandDailyToggle(false) },
                            onMore = {
                                onShowMoreMenu("SUPPLY_DEMAND_DAILY")
                            }
                        )
                    }
                    if (showOteVisibleChart) {
                        IndicatorStatusItem(
                            label = "OTE visible chart [twingall]",
                            color = ComposeColor(0xFFF0B90B),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "OTE_VISIBLE_CHART",
                            isMoreSelected = indicatorMoreMenuTarget == "OTE_VISIBLE_CHART",
                            isHidden = "OTE_VISIBLE_CHART" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "OTE_VISIBLE_CHART") null else "OTE_VISIBLE_CHART") },
                            onHide = { onIndicatorHide("OTE_VISIBLE_CHART") },
                            onSettings = { },
                            onRemove = { onOteVisibleChartToggle(false) },
                            onMore = {
                                onShowMoreMenu("OTE_VISIBLE_CHART")
                            }
                        )
                    }
                    if (showLiquidityDeltaProfiler) {
                        IndicatorStatusItem(
                            label = "Liquidity Delta [LuxAlgo]",
                            color = ComposeColor(0xFF2962FF),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "LIQUIDITY_DELTA_PROFILER",
                            isMoreSelected = indicatorMoreMenuTarget == "LIQUIDITY_DELTA_PROFILER",
                            isHidden = "LIQUIDITY_DELTA_PROFILER" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "LIQUIDITY_DELTA_PROFILER") null else "LIQUIDITY_DELTA_PROFILER") },
                            onHide = { onIndicatorHide("LIQUIDITY_DELTA_PROFILER") },
                            onSettings = { onLdpSettingsClick() },
                            onRemove = { onLiquidityDeltaProfilerToggle(false) },
                            onMore = {
                                onShowMoreMenu("LIQUIDITY_DELTA_PROFILER")
                            }
                        )
                    }
                    if (showEqhEqlLiquidityZones) {
                        IndicatorStatusItem(
                            label = "EQH/EQL [LuxAlgo]",
                            color = ComposeColor(0xFF089981),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "EQH_EQL_LIQUIDITY_ZONES",
                            isMoreSelected = indicatorMoreMenuTarget == "EQH_EQL_LIQUIDITY_ZONES",
                            isHidden = "EQH_EQL_LIQUIDITY_ZONES" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "EQH_EQL_LIQUIDITY_ZONES") null else "EQH_EQL_LIQUIDITY_ZONES") },
                            onHide = { onIndicatorHide("EQH_EQL_LIQUIDITY_ZONES") },
                            onSettings = { onEqhEqlSettingsClick() },
                            onRemove = { onEqhEqlLiquidityZonesToggle(false) },
                            onMore = {
                                onShowMoreMenu("EQH_EQL_LIQUIDITY_ZONES")
                            }
                        )
                    }
                    if (showPowerHourBreakout) {
                        IndicatorStatusItem(
                            label = "Power Hour [LuxAlgo]",
                            color = ComposeColor(0xFFE91E63),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "POWER_HOUR_BREAKOUT",
                            isMoreSelected = indicatorMoreMenuTarget == "POWER_HOUR_BREAKOUT",
                            isHidden = "POWER_HOUR_BREAKOUT" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "POWER_HOUR_BREAKOUT") null else "POWER_HOUR_BREAKOUT") },
                            onHide = { onIndicatorHide("POWER_HOUR_BREAKOUT") },
                            onSettings = { onPowerHourSettingsClick() },
                            onRemove = { onPowerHourBreakoutToggle(false) },
                            onMore = {
                                onShowMoreMenu("POWER_HOUR_BREAKOUT")
                            }
                        )
                    }
                    if (showTrendlineBreakouts) {
                        IndicatorStatusItem(
                            label = "TBT [ChartPrime]",
                            color = ComposeColor(0xFF4CAF50),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "TRENDLINE_BREAKOUTS",
                            isMoreSelected = indicatorMoreMenuTarget == "TRENDLINE_BREAKOUTS",
                            isHidden = "TRENDLINE_BREAKOUTS" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "TRENDLINE_BREAKOUTS") null else "TRENDLINE_BREAKOUTS") },
                            onHide = { onIndicatorHide("TRENDLINE_BREAKOUTS") },
                            onSettings = { onTrendlineSettingsClick() },
                            onRemove = { onTrendlineBreakoutsToggle(false) },
                            onMore = {
                                onShowMoreMenu("TRENDLINE_BREAKOUTS")
                            }
                        )
                    }
                    if (showTrendlineNavigator) {
                        IndicatorStatusItem(
                            label = "Trendline Navigator [LuxAlgo]",
                            color = ComposeColor(0xFF085DEF),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "TRENDLINE_NAVIGATOR",
                            isMoreSelected = indicatorMoreMenuTarget == "TRENDLINE_NAVIGATOR",
                            isHidden = "TRENDLINE_NAVIGATOR" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "TRENDLINE_NAVIGATOR") null else "TRENDLINE_NAVIGATOR") },
                            onHide = { onIndicatorHide("TRENDLINE_NAVIGATOR") },
                            onSettings = { onNavigatorSettingsClick() },
                            onRemove = { onTrendlineNavigatorToggle(false) },
                            onMore = {
                                onShowMoreMenu("TRENDLINE_NAVIGATOR")
                            }
                        )
                    }
                    if (showLiquidityPools) {
                        IndicatorStatusItem(
                            label = "Liquidity Pools [LuxAlgo]",
                            color = ComposeColor(0xFF089981),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "LIQUIDITY_POOLS",
                            isMoreSelected = indicatorMoreMenuTarget == "LIQUIDITY_POOLS",
                            isHidden = "LIQUIDITY_POOLS" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "LIQUIDITY_POOLS") null else "LIQUIDITY_POOLS") },
                            onHide = { onIndicatorHide("LIQUIDITY_POOLS") },
                            onSettings = { onLiquidityPoolsSettingsClick() },
                            onRemove = { onLiquidityPoolsToggle(false) },
                            onMore = {
                                onShowMoreMenu("LIQUIDITY_POOLS")
                            }
                        )
                    }
                    if (showOrderBlockBreaker) {
                        IndicatorStatusItem(
                            label = "Order Blocks & Breakers [LuxAlgo]",
                            color = ComposeColor(0xFF2157F3),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "ORDER_BLOCK_BREAKER",
                            isMoreSelected = indicatorMoreMenuTarget == "ORDER_BLOCK_BREAKER",
                            isHidden = "ORDER_BLOCK_BREAKER" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "ORDER_BLOCK_BREAKER") null else "ORDER_BLOCK_BREAKER") },
                            onHide = { onIndicatorHide("ORDER_BLOCK_BREAKER") },
                            onSettings = { onObbSettingsClick() },
                            onRemove = { onOrderBlockBreakerToggle(false) },
                            onMore = {
                                onShowMoreMenu("ORDER_BLOCK_BREAKER")
                            }
                        )
                    }
                    if (showVolumaticFvg) {
                        IndicatorStatusItem(
                            label = "Volumatic FVG [BigBeluga]",
                            color = ComposeColor(0xFF26C6DA),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "VOLUMATIC_FVG",
                            isMoreSelected = indicatorMoreMenuTarget == "VOLUMATIC_FVG",
                            isHidden = "VOLUMATIC_FVG" in hiddenIndicators,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "VOLUMATIC_FVG") null else "VOLUMATIC_FVG") },
                            onHide = { onIndicatorHide("VOLUMATIC_FVG") },
                            onSettings = { onVolumaticFvgSettingsClick() },
                            onRemove = { onVolumaticFvgToggle(false) },
                            onMore = {
                                onShowMoreMenu("VOLUMATIC_FVG")
                            },
                            extraContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${vfvgBullCount}↑", color = ComposeColor(0xFF1AC2D8), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "${vfvgBearCount}↓", color = ComposeColor(0xFFD8761A), fontSize = 13.sp)
                                }
                            }
                        )
                    }
                    if (autoFibEnabled) {
                        IndicatorStatusItem(
                            label = "Auto Fib Retracement",
                            color = ComposeColor(0xFF787B86),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "AUTO_FIB_RETRACEMENT",
                            isMoreSelected = indicatorMoreMenuTarget == "AUTO_FIB_RETRACEMENT",
                            isHidden = !showAutoFib,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "AUTO_FIB_RETRACEMENT") null else "AUTO_FIB_RETRACEMENT") },
                            onHide = { onAutoFibHide(!showAutoFib) },
                            onSettings = { onAutoFibSettingsClick() },
                            onRemove = { onAutoFibToggle(false) },
                            onMore = {
                                onShowMoreMenu("AUTO_FIB_RETRACEMENT")
                            }
                        )
                    }
                    if (confluenceFvgEnabled) {
                        IndicatorStatusItem(
                            label = "Confluence FVG Finder",
                            color = ComposeColor(0xFF089981),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "CONFLUENCE_FVG",
                            isMoreSelected = indicatorMoreMenuTarget == "CONFLUENCE_FVG",
                            isHidden = !showConfluenceFvg,
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "CONFLUENCE_FVG") null else "CONFLUENCE_FVG") },
                            onHide = { onConfluenceFvgHide(!showConfluenceFvg) },
                            onSettings = { onCfvgSettingsClick() },
                            onRemove = { onConfluenceFvgToggle(false) },
                            onMore = {
                                onShowMoreMenu("CONFLUENCE_FVG")
                            }
                        )
                    }
}

@Composable
private fun CurrencySelectorChip(
    showCurrencySelector: Boolean,
    selectedCurrency: String,
    onCurrencyClick: () -> Unit
) {
        if (showCurrencySelector) {
            // Top Right Currency Selector
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, end = 2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ComposeColor(0xFF131722))
                    .border(1.dp, ComposeColor(0xFF363A45), RoundedCornerShape(3.dp))
                    .clickable { onCurrencyClick() }
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedCurrency,
                        color = ComposeColor(0xFFD1D4DC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        null,
                        tint = ComposeColor(0xFF787B86),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
}

@Composable
private fun ChartCountdownOverlay(
    visible: Boolean,
    currentQuoteState: SymbolQuote?,
    timeframe: String,
    symbol: String
) {
        // Countdown Timer Overlay (positioned on right side near current price)
        if (visible && currentQuoteState != null) {
            val countdown by produceState(initialValue = "", currentQuoteState, timeframe) {
                while (true) {
                    val now = System.currentTimeMillis() / 1000
                    val timeframeSeconds = timeframeToSeconds(timeframe)
                    val elapsed = now % timeframeSeconds
                    val remaining = timeframeSeconds - elapsed
                    
                    val hours = remaining / 3600
                    val minutes = (remaining % 3600) / 60
                    val seconds = remaining % 60
                    
                    value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    delay(1000)
                }
            }
            
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (currentQuoteState!!.change >= 0) ComposeColor(0xCC089981) else ComposeColor(0xCCF05252))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatPrice(currentQuoteState!!.lastPrice, symbol),
                        color = ComposeColor.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = countdown,
                        color = ComposeColor.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
}

@Composable
private fun StatusLineHeader(
    chartSettings: ChartSettings,
    symbol: String,
    currentQuoteState: SymbolQuote?,
    onMarketStatusClick: () -> Unit
) {
    if (chartSettings.statusLine.symbol) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chartSettings.statusLine.logo) {
                        val symbolInfo = remember(symbol) {
                            val type = when {
                                symbol.startsWith("BTC") || symbol.startsWith("ETH") || symbol.startsWith("SOL") -> "Crypto"
                                symbol.length == 6 && (symbol.contains("USD") || symbol.contains("EUR") || symbol.contains("JPY") || symbol.contains("GBP")) -> "Forex"
                                symbol == "SPX" || symbol == "DJI" || symbol == "IXIC" || symbol == "NIFTY" -> "Index"
                                else -> "Stock"
                            }
                            SymbolInfo(ticker = symbol, name = "", type = type)
                        }
                        AssetIcon(symbolInfo, size = 24)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (chartSettings.statusLine.titleMode == "Description") getFullSymbolName(symbol) else symbol,
                        color = ComposeColor(0xFFB2B5BE),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (chartSettings.statusLine.openMarketStatus) {
                        val isCrypto = symbol.uppercase().contains("BTC") || symbol.uppercase().contains("ETH")
                        val calendar = Calendar.getInstance()
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        val isOpen = isCrypto || (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY)
                        val dotColor = if (isOpen) ComposeColor(0xFF089981) else ComposeColor(0xFF787B86)

                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable { onMarketStatusClick() }
                        ) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.15f)))
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.35f)))
                            Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(dotColor))
                        }
                    }
                }
            }

            currentQuoteState?.let { quote ->
                val color = if (quote.change >= 0) ComposeColor(0xFF089981) else ComposeColor(0xFFF05252)
                val statusFontSize = 12.8.sp  // Reduced by 20% from 16sp
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 0.dp)) {
                    Text(
                        text = formatPrice(quote.lastPrice, symbol),
                        color = color,
                        fontSize = statusFontSize,
                        fontWeight = FontWeight.Medium
                    )
                    if (chartSettings.statusLine.barChangeValues) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val sign = if (quote.change >= 0) "+" else ""
                        Text(
                            text = String.format("%s%s (%+.2f%%)", sign, formatPrice(quote.change, symbol), quote.changePercent),
                            color = color,
                            fontSize = statusFontSize,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (chartSettings.statusLine.ohlc) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        if (chartSettings.symbol.openVisible) OhlcItem("O", quote.open, symbol)
                        if (chartSettings.symbol.highVisible) OhlcItem("H", quote.high, symbol)
                        if (chartSettings.symbol.lowVisible) OhlcItem("L", quote.low, symbol)
                        if (chartSettings.symbol.closeVisible) OhlcItem("C", quote.lastPrice, symbol)
                    }
                }
            }
}
