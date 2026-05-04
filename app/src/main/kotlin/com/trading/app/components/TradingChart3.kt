package com.trading.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.EconomicCalendarPayload
import com.trading.app.models.OHLCData
import com.trading.app.utils.Indicators
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import java.util.Locale
import com.tradingview.lightweightcharts.api.series.models.BarData
import com.tradingview.lightweightcharts.api.series.models.LineData

@Composable
fun TradingChart3(
    symbol: String,
    timeframe: String,
    style: String,
    chartSettings: com.trading.app.models.ChartSettings,
    drawings: List<com.trading.app.models.Drawing>,
    onDrawingUpdate: (com.trading.app.models.Drawing) -> Unit,
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
    showAtr: Boolean = false,
    atrPeriod: Int = 14,
    showMacd: Boolean = false,
    macdFast: Int = 12,
    macdSlow: Int = 26,
    macdSignal: Int = 9,
    showVolume: Boolean = true,
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
    selectedTimeZone: String = "UTC",
    onQuoteUpdate: (SymbolQuote) -> Unit = {},
    onAnyQuoteUpdate: (SymbolQuote) -> Unit = {},
    watchlistSymbols: List<String> = emptyList(),
    positions: List<com.trading.app.models.Position>,
    onPositionUpdate: (com.trading.app.models.Position) -> Unit,
    onPositionDelete: (String) -> Unit,
    onAccountUpdate: (com.trading.app.data.Mt5Service.AccountInfo) -> Unit = {},
    onPositionsUpdate: (List<com.trading.app.models.Position>) -> Unit = {},
    orders: List<com.trading.app.models.Order> = emptyList(),
    onOrdersUpdate: (List<com.trading.app.models.Order>) -> Unit = {},
    onHistoryOrdersUpdate: (List<com.trading.app.models.Order>) -> Unit = {},
    onBalanceHistoryUpdate: (List<com.trading.app.models.BalanceRecord>) -> Unit = {},
    onCalendarUpdate: (EconomicCalendarPayload) -> Unit = {},
    isCalendarVisible: Boolean = false,
    calendarRequestDateIso: String? = null,
    calendarRequestVersion: Int = 0,
    isTradingBarVisible: Boolean = false,
    reverseBridge: com.trading.app.data.Mt5ReverseBridge? = null
) {
    // This will hold the OHLC data converted from whatever source TradingChart uses
    var ohlcData by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
    
    // Calculate RSI and RSI MA
    val rsiValues = remember(ohlcData, showRsi, rsiPeriod) {
        if (showRsi) Indicators.calculateRsi(ohlcData, rsiPeriod) else emptyList()
    }
    val rsiMaValues = remember(rsiValues) {
        if (rsiValues.isNotEmpty()) Indicators.calculateSma(rsiValues, 14) else emptyList()
    }

    // Use the internal pane management of TradingChart
    TradingChart(
        symbol = symbol,
        timeframe = timeframe,
        style = style,
        chartSettings = chartSettings,
        drawings = drawings,
        onDrawingUpdate = onDrawingUpdate,
        activeTool = activeTool,
        onToolReset = onToolReset,
        showRsi = showRsi,
        rsiPeriod = rsiPeriod,
        showEma10 = showEma10,
        ema10Period = ema10Period,
        showEma20 = showEma20,
        ema20Period = ema20Period,
        showSma1 = showSma1,
        sma1Period = sma1Period,
        showSma2 = showSma2,
        sma2Period = sma2Period,
        showVwap = showVwap,
        showBb = showBb,
        bbPeriod = bbPeriod,
        showAtr = showAtr,
        atrPeriod = atrPeriod,
        showMacd = showMacd,
        macdFast = macdFast,
        macdSlow = macdSlow,
        macdSignal = macdSignal,
        showVolume = showVolume,
        isCrosshairActive = isCrosshairActive,
        onCrosshairToggle = onCrosshairToggle,
        onVolumeToggle = onVolumeToggle,
        onIndicatorSettingsClick = onIndicatorSettingsClick,
        isMagnetEnabled = isMagnetEnabled,
        isLocked = isLocked,
        isVisible = isVisible,
        selectedCurrency = selectedCurrency,
        onCurrencyClick = onCurrencyClick,
        isFullscreen = isFullscreen,
        onFullscreenExit = onFullscreenExit,
        scrollToTimestamp = scrollToTimestamp,
        onScrollDone = onScrollDone,
        onLongPress = onLongPress,
        onSettingsClick = onSettingsClick,
        selectedTimeZone = selectedTimeZone,
        onQuoteUpdate = onQuoteUpdate,
        onAnyQuoteUpdate = onAnyQuoteUpdate,
        watchlistSymbols = watchlistSymbols,
        onAccountUpdate = onAccountUpdate,
        onPositionsUpdate = onPositionsUpdate,
        orders = orders,
        onOrdersUpdate = onOrdersUpdate,
        onHistoryOrdersUpdate = onHistoryOrdersUpdate,
        onBalanceHistoryUpdate = onBalanceHistoryUpdate,
        onCalendarUpdate = onCalendarUpdate,
        isCalendarVisible = isCalendarVisible,
        calendarRequestDateIso = calendarRequestDateIso,
        calendarRequestVersion = calendarRequestVersion,
        positions = positions,
        onPositionUpdate = onPositionUpdate,
        onPositionDelete = onPositionDelete,
        onDataLoaded = { data ->
            ohlcData = data
        },
        reverseBridge = reverseBridge
    )
}


