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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.trading.app.data.BinanceService
import com.trading.app.data.Mt5Service
import com.trading.app.data.Mt5ReverseBridge
import com.trading.app.models.ChartSettings
import com.trading.app.models.Drawing
import com.trading.app.models.Position
import com.trading.app.models.Order
import com.trading.app.models.BalanceRecord
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
        "USOIL" -> "WTI Crude Oil"
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

private fun binanceStreamSymbolFor(symbol: String): String {
    return when (val normalized = normalizeChartSymbol(symbol)) {
        "BTCUSD" -> "BTCUSDT"
        "ETHUSD" -> "ETHUSDT"
        else -> normalized
    }
}

private fun chartSymbolsMatch(left: String, right: String): Boolean {
    return binanceStreamSymbolFor(left) == binanceStreamSymbolFor(right)
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
    showAtr: Boolean = false
): Map<String, PriceScaleMargins> {
    val margins = mutableMapOf<String, PriceScaleMargins>()

    val activePanes = buildList {
        if (showAtr) add(ATR_SCALE_KEY)
        if (showMacd) add(MACD_SCALE_KEY)
        if (showRsi) add(RSI_SCALE_KEY)
        // Volume is handled as an overlay on the main candle area
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
    selectedIndicatorId: String? = null,
    onSelectedIndicatorIdChange: (String?) -> Unit = {},
    onIndicatorDataUpdate: (IndicatorData) -> Unit = {}
) {
    var ohlcData by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMoreHistory by remember { mutableStateOf(true) }
    var useMt5FallbackForCrypto by remember { mutableStateOf(false) }
    
    val candlestickData by remember {
        derivedStateOf { ohlcData.map(OHLCData::toCandlestickData) }
    }
    var currentQuoteState by remember { mutableStateOf<SymbolQuote?>(null) }
    var mainPriceScaleWidthPx by remember { mutableFloatStateOf(0f) }
    var seriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var chartsViewApi by remember { mutableStateOf<ChartsView?>(null) }
    var showMarketStatus by remember { mutableStateOf(false) }
    var showIndicatorsList by remember { mutableStateOf(true) }
    var showIndicatorMoreMenu by remember { mutableStateOf(false) }
    var indicatorMoreMenuTarget by remember { mutableStateOf<String?>(null) }

    // Indicator series state
    val rsiPaneRefs = rememberRsiPaneRefs()
    var ema10SeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var ema20SeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var sma1SeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var sma2SeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var vwapBandFillSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var vwapBandMaskSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var vwapUpperSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var vwapSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var vwapLowerSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var atrSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var bbBandFillSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var bbBandMaskSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var bbUpperSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var bbMiddleSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var bbLowerSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var macdLineSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var macdSignalSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var macdHistogramSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var volumeSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }
    var volumeMaSeriesApi by remember { mutableStateOf<SeriesApi?>(null) }

    fun resetChartSeriesHandles() {
        chartsViewApi = null
        seriesApi = null
        rsiPaneRefs.clear()
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
    val showInlineRsiPane = false
    val mainSeriesKind = resolveMainSeriesKind(style)

    fun applyQuoteToChart(quote: SymbolQuote) {
        currentQuoteState = quote
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
        if (now - lastChartQuoteAppliedAt < CHART_TICK_THROTTLE_MS) {
            pendingChartQuote = quote
            return
        }
        lastChartQuoteAppliedAt = now
        pendingChartQuote = null
        applyQuoteToChart(quote)
    }

    LaunchedEffect(pendingChartQuote) {
        val quote = pendingChartQuote ?: return@LaunchedEffect
        val waitMs = (CHART_TICK_THROTTLE_MS - (System.currentTimeMillis() - lastChartQuoteAppliedAt)).coerceAtLeast(0L)
        kotlinx.coroutines.delay(waitMs)
        if (pendingChartQuote == quote) {
            lastChartQuoteAppliedAt = System.currentTimeMillis()
            pendingChartQuote = null
            applyQuoteToChart(quote)
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
        resolvePaneMargins(showVolume, showInlineRsiPane, showMacd, showAtr)
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

    val binanceService = remember {
        BinanceService(
            onQuoteUpdate = { quote: SymbolQuote ->
                val isTarget = chartSymbolsMatch(quote.name, currentSymbol.value)
                
                if (isTarget) {
                    val prevClose = ohlcData.getOrNull(ohlcData.size - 2)?.close ?: quote.lastPrice
                    val change = quote.lastPrice - prevClose
                    val changePercent = if (prevClose != 0f) (change / prevClose) * 100f else 0f
                    
                    val updatedQuote = quote.copy(
                        name = currentSymbol.value,
                        change = change,
                        changePercent = changePercent
                    )
                    
                    scheduleChartQuote(updatedQuote)
                }
            },
            onHistoryUpdate = { receivedSymbol: String, history: List<OHLCData> ->
                val isTarget = chartSymbolsMatch(receivedSymbol, currentSymbol.value)
                
                if (isTarget) {
                    if (history.isEmpty()) {
                        // Binance history can be blocked/unavailable in some regions.
                        // Mark fallback so MT5 can provide candles for USDT symbols.
                        if (binanceStreamSymbolFor(currentSymbol.value).endsWith("USDT", ignoreCase = true)) {
                            useMt5FallbackForCrypto = true
                        }
                        return@BinanceService
                    }
                    useMt5FallbackForCrypto = false
                    if (isLoadingMore) {
                        val combined = (history + ohlcData)
                            .distinctBy { it.time }
                            .sortedBy { it.time }
                            .takeLast(10000)
                        ohlcData = combined
                        isLoadingMore = false
                        if (history.size < MT5_HISTORY_PAGE_SIZE) hasMoreHistory = false
                    } else {
                        ohlcData = history
                        if (history.size < MT5_HISTORY_PAGE_SIZE) hasMoreHistory = false
                        updatedOnDataLoaded.value(history)
                    }
                }
            }
        )
    }

    val mt5Service = remember {
        Mt5Service(
            pcIpAddress = "10.95.77.133",
            port = 8081,
            onHistoryUpdate = { receivedSymbol: String, history: List<OHLCData> ->
                if (receivedSymbol.isEmpty() || chartSymbolsMatch(receivedSymbol, currentSymbol.value)) {
                    val isCryptoBinance = normalizeChartSymbol(currentSymbol.value).endsWith("USDT", ignoreCase = true)
                    if (isCryptoBinance && !useMt5FallbackForCrypto) {
                        return@Mt5Service
                    }
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
                    Log.d(LOG_TAG, "onHistoryUpdate (MT5): received ${processedHistory.size} candles for $receivedSymbol")
                }
            },
            onQuoteUpdate = { quote: SymbolQuote ->
                var outgoingQuote = quote
                if (chartSymbolsMatch(quote.name, currentSymbol.value)) {
                    // Symbol Routing: Only symbols ending in USDT use BinanceService for main chart data
                    val isCryptoBinance = normalizeChartSymbol(currentSymbol.value).endsWith("USDT", ignoreCase = true)
                    
                    if (!isCryptoBinance || useMt5FallbackForCrypto) {
                        val prevClose = ohlcData.getOrNull(ohlcData.size - 2)?.close ?: quote.lastPrice
                        val change = quote.lastPrice - prevClose
                        val changePercent = if (prevClose != 0f) (change / prevClose) * 100f else 0f
                        
                        val updatedQuote = quote.copy(
                            change = change,
                            changePercent = changePercent
                        )
                        outgoingQuote = updatedQuote
                        scheduleChartQuote(updatedQuote)
                    }
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

    LaunchedEffect(Unit) {
        mt5Service.connect()
        mt5Service.requestSymbols()
        reverseBridge?.connect()
    }

    LaunchedEffect(symbol, timeframe) {
        mt5Service.stopActiveStream()
        binanceService.stopActiveStream()
        ohlcData = emptyList()
        currentQuoteState = null
        pendingChartQuote = null
        lastChartQuoteAppliedAt = 0L
        isLoadingMore = false
        hasMoreHistory = true
        useMt5FallbackForCrypto = false
        val streamSymbol = binanceStreamSymbolFor(symbol)
        if (streamSymbol.endsWith("USDT", ignoreCase = true)) {
            binanceService.streamActiveSymbol(streamSymbol)
            binanceService.fetchHistory(streamSymbol, timeframe, null) // Explicit null and it uses limit=500
            // If Binance history does not arrive quickly, auto-fallback to MT5 history.
            kotlinx.coroutines.delay(3500)
            if (
                ohlcData.isEmpty() &&
                chartSymbolsMatch(currentSymbol.value, symbol) &&
                currentTimeframe.value.equals(timeframe, ignoreCase = true)
            ) {
                useMt5FallbackForCrypto = true
                mt5Service.streamActiveSymbol(streamSymbol, timeframe, 500)
            }
        } else {
            mt5Service.streamActiveSymbol(streamSymbol, timeframe, 500)
        }
    }

    LaunchedEffect(isCalendarVisible, calendarRequestDateIso, calendarRequestVersion) {
        if (isCalendarVisible) {
            mt5Service.requestCalendar(calendarRequestDateIso)
        }
    }

    LaunchedEffect(isNewsVisible) {
        if (isNewsVisible) {
            mt5Service.requestNews()
        }
    }

    LaunchedEffect(ohlcData, seriesApi, style, chartBgColor,
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
        volumeMaSeriesApi) {
        val mainSeriesApi = seriesApi
        val ohlcList = ohlcData
        
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

            updateInlineRsiPaneData(
                refs = rsiPaneRefs,
                candles = ohlcData,
                data = rsiDataState,
                enabled = true, // Force enabled for now as showInlineRsiPane is false
                showLabels = rsiShowLabels,
                showLines = rsiShowLines
            )
            
            if (showEma10) {
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

            if (showSma1) {
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

            if (showSma2) {
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

            if (showVwap) {
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

            if (showAtr) {
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

            if (showBb) {
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

            if (showMacd) {
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

            if (showVolume) {
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
                volumeMaSeriesApi?.setData(emptyList())
                safelyRemovePriceLine(volumeSeriesApi, volumeLineState.value)
                safelyRemovePriceLine(volumeMaSeriesApi, volumeMaLineState.value)
                volumeLineState.value = null
                volumeMaLineState.value = null
            }
        } else {
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
        val rsiScaleMargins = paneMargins[RSI_SCALE_KEY] ?: PriceScaleMargins(
            top = 1f - RSI_PANE_HEIGHT - 0.04f,
            bottom = 0.04f
        )
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
            scaleMargins = rsiScaleMargins,
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

        val updatedCandle = lastCandle.copy(
            high = maxOf(lastCandle.high, quote.lastPrice),
            low = minOf(lastCandle.low, quote.lastPrice),
            close = quote.lastPrice
        )

        when (mainSeriesKind) {
            MainSeriesKind.BAR -> api.update(updatedCandle.toBarSeriesData())
            MainSeriesKind.LINE -> api.update(updatedCandle.toLineSeriesData())
            MainSeriesKind.AREA -> api.update(
                updatedCandle.toAreaSeriesData(areaValueForStyle(style, updatedCandle))
            )
            MainSeriesKind.BASELINE -> api.update(updatedCandle.toBaselineSeriesData())
            MainSeriesKind.CANDLESTICK -> api.update(updatedCandle.toCandlestickData())
        }
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
            val s1 = symbol.uppercase()
            val s2 = pos.symbol.uppercase()
            s1 == s2 || 
            (s1 == "BTCUSDT" && s2 == "BTCUSD") || 
            (s1 == "BTCUSD" && s2 == "BTCUSDT") ||
            (s1 == "ETHUSDT" && s2 == "ETHUSD") || 
            (s1 == "ETHUSD" && s2 == "ETHUSDT")
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
            val s1 = symbol.uppercase()
            val s2 = ord.symbol.uppercase()
            s1 == s2 || 
            (s1 == "BTCUSDT" && s2 == "BTCUSD") || 
            (s1 == "BTCUSD" && s2 == "BTCUSDT") ||
            (s1 == "ETHUSDT" && s2 == "ETHUSD") || 
            (s1 == "ETHUSD" && s2 == "ETHUSDT")
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
            mt5Service.disconnect()
            binanceService.disconnect()
            reverseBridge?.disconnect()
        }
    }

    DisposableEffect(style, symbol) {
        onDispose {
            resetChartSeriesHandles()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                key(style) {
                    AndroidView(
                        factory = { context ->
                            resetChartSeriesHandles()
                            ChartsView(context).apply {
                                chartsViewApi = this
                                rsiPaneRefs.clear()
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
                                rightOffset = 15f
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
                            if (range != null && candlestickData.isNotEmpty()) {
                                // Lazy loading logic
                                if (!isLoadingMore && hasMoreHistory && ohlcData.size < 10000) {
                                    val firstCandleTime = ohlcData.first().time
                                    val visibleStart = (range.from as? Time.Utc)?.timestamp ?: 0L
                                    val threshold = timeframeToSeconds(timeframe) * 50 // 50 candles buffer
                                    
                                    if (visibleStart <= firstCandleTime + threshold) {
                                        isLoadingMore = true
                                        val endTime = firstCandleTime - 1
                                        val streamSymbol = binanceStreamSymbolFor(symbol)
                                        if (streamSymbol.endsWith("USDT", ignoreCase = true)) {
                                            binanceService.fetchHistory(streamSymbol, timeframe, endTime)
                                        } else {
                                            mt5Service.subscribe(streamSymbol, timeframe, endTime, 500)
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

                        if (false) { // Force RSI pane to be hidden from chart
                            val rsiScaleMargins = paneMargins[RSI_SCALE_KEY] ?: PriceScaleMargins(
                                top = 1f - RSI_PANE_HEIGHT - 0.04f,
                                bottom = 0.04f
                            )
                            createInlineRsiPaneSeries(
                                chartsView = this,
                                refs = rsiPaneRefs,
                                scaleMargins = rsiScaleMargins,
                                borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
                                visible = true
                            )
                        }

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
                            rightOffset = 15f
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



                RsiPaneOverlay(
                    visible = showInlineRsiPane,
                    scaleMargins = paneMargins[RSI_SCALE_KEY] ?: PriceScaleMargins(
                        top = 1f - RSI_PANE_HEIGHT - 0.04f,
                        bottom = 0.04f
                    ),
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
        // Top Right Currency Selector
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
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

        // Overlay UI (Top Left Status Line)
        Column(
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    top = chartSettings.canvas.marginTop.dp,
                    end = chartSettings.canvas.marginRight.dp,
                    bottom = chartSettings.canvas.marginBottom.dp
                )
                .align(Alignment.TopStart)
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
                        Spacer(modifier = Modifier.width(8.dp))
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
                            modifier = Modifier.clickable { showMarketStatus = true }
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
                val statusFontSize = 16.sp
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
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
                
                if (showIndicatorsList) {
                    if (showVolume) {
                        IndicatorStatusItem(
                            label = "Vol · Ticks",
                            color = ComposeColor(0xFF787B86),
                            value = null,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "Volume",
                            isMoreSelected = indicatorMoreMenuTarget == "Volume",
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "Volume") null else "Volume") },
                            onHide = { onVolumeToggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "EMA 10") null else "EMA 10") },
                            onHide = { onEma10Toggle(false) },
                            onSettings = { onIndicatorSettingsClick("EMA 10") },
                            onRemove = { onEma10Toggle(false) },
                            onMore = { 
                                indicatorMoreMenuTarget = "EMA 10"
                                showIndicatorMoreMenu = true
                            }
                        )
                    }
                    if (showEma20) {
                        IndicatorStatusItem(
                            label = "EMA $ema20Period close",
                            color = ComposeColor.White,
                            value = ohlcData.lastOrNull()?.close,
                            symbol = symbol,
                            isSelected = selectedIndicatorId == "EMA 20",
                            isMoreSelected = indicatorMoreMenuTarget == "EMA 20",
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "EMA 20") null else "EMA 20") },
                            onHide = { onEma20Toggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "SMA 1") null else "SMA 1") },
                            onHide = { onSma1Toggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "SMA 2") null else "SMA 2") },
                            onHide = { onSma2Toggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "BB") null else "BB") },
                            onHide = { onBbToggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "VWAP") null else "VWAP") },
                            onHide = { onVwapToggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "RSI") null else "RSI") },
                            onHide = { onRsiToggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "ATR") null else "ATR") },
                            onHide = { onAtrToggle(false) },
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
                            onClick = { onSelectedIndicatorIdChange(if (selectedIndicatorId == "MACD") null else "MACD") },
                            onHide = { onMacdToggle(false) },
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

        if (showIndicatorMoreMenu && indicatorMoreMenuTarget != null) {
            IndicatorMoreMenu(
                label = indicatorMoreMenuTarget!!,
                onDismiss = { 
                    showIndicatorMoreMenu = false
                    indicatorMoreMenuTarget = null
                }
            )
        }

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

        if (showMarketStatus) {
            MarketStatusModal(
                symbol = symbol,
                selectedTimeZone = selectedTimeZone,
                onDismiss = { showMarketStatus = false }
            )
        }
    }
}

private fun formatPrice(price: Float, symbol: String = ""): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    symbols.groupingSeparator = ','
    val uppercaseSymbol = symbol.uppercase()
    val isBitcoin = uppercaseSymbol.contains("BTC") || uppercaseSymbol.contains("BITCOIN")
    val isForex = uppercaseSymbol.length == 6 || uppercaseSymbol.contains("/")
    
    val pattern = when {
        isBitcoin -> "#,##0"
        isForex -> "#,##0.00000"
        else -> "#,##0.##"
    }

    val df = DecimalFormat(pattern, symbols)
    return df.format(price)
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
    onClick: () -> Unit = {},
    onHide: () -> Unit = {},
    onSettings: () -> Unit = {},
    onRemove: () -> Unit = {},
    onMore: () -> Unit = {},
    extraContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
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
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "Hide",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorMoreMenu(
    label: String,
    onDismiss: () -> Unit
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
        windowInsets = WindowInsets(0),
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
