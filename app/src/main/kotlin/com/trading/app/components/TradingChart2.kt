package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.IndicatorData
import com.trading.app.models.ChartSettings
import com.trading.app.models.Drawing
import com.trading.app.models.PartialOrder
import com.trading.app.models.Position
import com.trading.app.models.Order
import com.trading.app.models.BalanceRecord
import com.trading.app.models.EconomicCalendarPayload
import com.trading.app.models.OHLCData
import com.trading.app.models.SymbolInfo
import com.trading.app.data.Mt5Service
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

@Composable
fun TradingChart2(
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
    volumeMaColor: Color = Color(0xFF2962FF),
    volumeGrowingColor: Color = Color(0xFF089981),
    volumeFallingColor: Color = Color(0xFFF23645),
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
    positions: List<Position>,
    onPositionUpdate: (Position) -> Unit,
    onPositionDelete: (String) -> Unit,
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
    isTradingBarVisible: Boolean = false,
    reverseBridge: com.trading.app.data.Mt5ReverseBridge? = null,
    onTradeNotification: (com.trading.app.models.TradeNotification) -> Unit = {},
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
    var lotSize by remember { mutableStateOf("1.0") }
    var tpPrice by remember { mutableStateOf<Float?>(null) }
    var slPrice by remember { mutableStateOf<Float?>(null) }
    var pendingPartialOrders by remember { mutableStateOf<List<PartialOrder>>(emptyList()) }

    var currentLiveQuote by remember { mutableStateOf<SymbolQuote?>(null) }
    var showModifyModal by remember { mutableStateOf(false) }
    var selectedPositionToModify by remember { mutableStateOf<Position?>(null) }
    var offset by remember { mutableStateOf(IntOffset(100, 100)) }
    val tradeNotifications = remember { mutableStateListOf<com.trading.app.models.TradeNotification>() }
    val notificationsToDismiss = remember { mutableStateListOf<String>() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                bbStdDev = bbStdDev,
                showAtr = showAtr,
                atrPeriod = atrPeriod,
                showMacd = showMacd,
                macdFast = macdFast,
                macdSlow = macdSlow,
                macdSignal = macdSignal,
                showVolume = showVolume,
                showVolumeMa = showVolumeMa,
                volumeMaLength = volumeMaLength,
                volumeMaColor = volumeMaColor,
                volumeGrowingColor = volumeGrowingColor,
                volumeFallingColor = volumeFallingColor,
                volumeColorBasedOnPreviousClose = volumeColorBasedOnPreviousClose,
                isCrosshairActive = isCrosshairActive,
                onCrosshairToggle = onCrosshairToggle,
                onVolumeToggle = onVolumeToggle,
                onRsiToggle = onRsiToggle,
                onEma10Toggle = onEma10Toggle,
                onEma20Toggle = onEma20Toggle,
                onSma1Toggle = onSma1Toggle,
                onSma2Toggle = onSma2Toggle,
                onVwapToggle = onVwapToggle,
                onBbToggle = onBbToggle,
                onAtrToggle = onAtrToggle,
                onMacdToggle = onMacdToggle,
                rsiShowLabels = rsiShowLabels,
                rsiShowLines = rsiShowLines,
                ema10ShowLabels = ema10ShowLabels,
                ema10ShowLines = ema10ShowLines,
                ema20ShowLabels = ema20ShowLabels,
                ema20ShowLines = ema20ShowLines,
                sma1ShowLabels = sma1ShowLabels,
                sma1ShowLines = sma1ShowLines,
                sma2ShowLabels = sma2ShowLabels,
                sma2ShowLines = sma2ShowLines,
                vwapShowLabels = vwapShowLabels,
                vwapShowLines = vwapShowLines,
                bbShowLabels = bbShowLabels,
                bbShowLines = bbShowLines,
                atrShowLabels = atrShowLabels,
                atrShowLines = atrShowLines,
                macdShowLabels = macdShowLabels,
                macdShowLines = macdShowLines,
                volumeShowLabels = volumeShowLabels,
                volumeShowLines = volumeShowLines,
                selectedIndicatorId = selectedIndicatorId,
                onSelectedIndicatorIdChange = onSelectedIndicatorIdChange,
                onIndicatorDataUpdate = onIndicatorDataUpdate,
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
                onDataLoaded = onDataLoaded,
                selectedTimeZone = selectedTimeZone,
                onQuoteUpdate = {
                    currentLiveQuote = it
                    onQuoteUpdate(it)
                },
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
                isNewsVisible = isNewsVisible,
                onNewsUpdate = onNewsUpdate,
                onSymbolsUpdate = onSymbolsUpdate,
                positions = positions,
                onPositionUpdate = { pos ->
                    reverseBridge?.modifyPosition(pos, pos.tp, pos.sl)
                    onPositionUpdate(pos)
                },
                onPositionDelete = { id ->
                    onPositionDelete(id)
                },
                onDoubleClick = { price ->
                    val lastPrice = currentLiveQuote?.lastPrice ?: price
                    val tolerance = lastPrice * 0.02f
                    val targetPos = positions.find {
                        it.symbol.equals(symbol, ignoreCase = true) &&
                            abs(it.entryPrice - lastPrice) < tolerance
                    }
                    if (targetPos != null) {
                        selectedPositionToModify = targetPos
                    }
                },
                reverseBridge = reverseBridge
            )
        }

        // Floating Trading Bar
        if (isTradingBarVisible && chartSettings.trading.oneClickTrading) {
            Box(
                modifier = Modifier
                    .offset { offset }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offset = IntOffset(
                                (offset.x + dragAmount.x).roundToInt(),
                                (offset.y + dragAmount.y).roundToInt()
                            )
                        }
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF252525).copy(alpha = 0.95f))
                    .padding(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(40.dp)
                ) {
                    // Drag handle
                    Column(
                        modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(3) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(2) {
                                    Box(modifier = Modifier.size(2.5.dp).background(Color(0xFF434651), RoundedCornerShape(50)))
                                }
                            }
                        }
                    }

                    // Sell Button
                    Box(
                        modifier = Modifier
                            .width(82.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF23645))
                            .clickable {
                                currentLiveQuote?.let { quote ->
                                    val newPos = Position(
                                        id = "temp_${System.currentTimeMillis()}",
                                        symbol = symbol,
                                        type = "sell",
                                        entryPrice = quote.bid,
                                        volume = lotSize.toFloatOrNull() ?: 1f,
                                        time = System.currentTimeMillis(),
                                        tp = tpPrice,
                                        sl = slPrice,
                                        partialOrders = pendingPartialOrders
                                    )
                                    reverseBridge?.placePosition(newPos)
                                    onPositionUpdate(newPos)
                                    val notification = com.trading.app.models.TradeNotification(
                                        symbol = symbol,
                                        volume = newPos.volume,
                                        price = quote.bid,
                                        isBuy = false,
                                        type = "executed"
                                    )
                                    onTradeNotification(notification)
                                    tradeNotifications.add(notification)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentLiveQuote?.bid?.toString() ?: "0.00",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    // Lot Size Input
                    Column(
                        modifier = Modifier
                            .width(58.dp)
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("0.0", color = Color(0xFF787B86), fontSize = 9.sp)
                        BasicTextField(
                            value = lotSize,
                            onValueChange = { lotSize = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Buy Button
                    Box(
                        modifier = Modifier
                            .width(82.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF2962FF))
                            .clickable {
                                currentLiveQuote?.let { quote ->
                                    val newPos = Position(
                                        id = "temp_${System.currentTimeMillis()}",
                                        symbol = symbol,
                                        type = "buy",
                                        entryPrice = quote.ask,
                                        volume = lotSize.toFloatOrNull() ?: 1f,
                                        time = System.currentTimeMillis(),
                                        tp = tpPrice,
                                        sl = slPrice,
                                        partialOrders = pendingPartialOrders
                                    )
                                    reverseBridge?.placePosition(newPos)
                                    onPositionUpdate(newPos)
                                    val notification = com.trading.app.models.TradeNotification(
                                        symbol = symbol,
                                        volume = newPos.volume,
                                        price = quote.ask,
                                        isBuy = true,
                                        type = "executed"
                                    )
                                    onTradeNotification(notification)
                                    tradeNotifications.add(notification)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentLiveQuote?.ask?.toString() ?: "0.00",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    // More Button
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showModifyModal = true }
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = Color(0xFFD1D4DC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Modal for settings TP/SL for the next trade
        if (showModifyModal) {
            ModifyTpSlModal(
                symbol = symbol,
                qty = lotSize,
                entryPrice = currentLiveQuote?.lastPrice ?: 0f,
                lastTradedPrice = currentLiveQuote?.lastPrice ?: 0f,
                isBuy = true,
                initialTp = tpPrice,
                initialSl = slPrice,
                initialPartialOrders = pendingPartialOrders,
                currentPrice = currentLiveQuote?.lastPrice ?: 0f,
                onConfirm = { tp, sl, partials ->
                    tpPrice = tp
                    slPrice = sl
                    pendingPartialOrders = partials
                    showModifyModal = false
                },
                onCancel = { showModifyModal = false }
            )
        }

        // Modal for modifying an existing position
        if (selectedPositionToModify != null) {
            val pos = selectedPositionToModify!!
            ModifyTpSlModal(
                symbol = pos.symbol,
                qty = pos.volume.toString(),
                entryPrice = pos.entryPrice,
                lastTradedPrice = currentLiveQuote?.lastPrice ?: pos.entryPrice,
                isBuy = pos.type.equals("buy", ignoreCase = true),
                initialTp = pos.tp,
                initialSl = pos.sl,
                initialPartialOrders = pos.partialOrders,
                currentPrice = currentLiveQuote?.lastPrice ?: 0f,
                onConfirm = { tp, sl, partials ->
                    val oldTp = pos.tp
                    val oldSl = pos.sl
                    onPositionUpdate(pos.copy(tp = tp, sl = sl, partialOrders = partials))

                    if (tp != null && tp != oldTp) {
                        val notification = com.trading.app.models.TradeNotification(
                            symbol = pos.symbol,
                            volume = pos.volume,
                            price = tp,
                            isBuy = pos.type.equals("buy", ignoreCase = true),
                            type = "tp_placed"
                        )
                        onTradeNotification(notification)
                        tradeNotifications.add(notification)
                    }
                    if (sl != null && sl != oldSl) {
                        val notification = com.trading.app.models.TradeNotification(
                            symbol = pos.symbol,
                            volume = pos.volume,
                            price = sl,
                            isBuy = pos.type.equals("buy", ignoreCase = true),
                            type = "sl_placed"
                        )
                        onTradeNotification(notification)
                        tradeNotifications.add(notification)
                    }
                    selectedPositionToModify = null
                },
                onCancel = { selectedPositionToModify = null }
            )
        }

        // Trade Notifications Popups inside Chart Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (tradeNotifications.isNotEmpty()) {
                    // "Show less" header
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252525))
                            .clickable {
                                if (tradeNotifications.isNotEmpty()) {
                                    notificationsToDismiss.add(tradeNotifications.last().id)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show less",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF787B86)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tradeNotifications.size.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (tradeNotifications.isNotEmpty()) {
                                    notificationsToDismiss.add(tradeNotifications.last().id)
                                }
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Latest",
                                tint = Color(0xFF787B86),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                tradeNotifications.reversed().forEach { notification ->
                    key(notification.id) {
                        TradeNotificationPopup(
                            notification = notification,
                            dismissTrigger = notificationsToDismiss.contains(notification.id),
                            onDismiss = { 
                                tradeNotifications.remove(notification)
                                notificationsToDismiss.remove(notification.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyTpSlModal(
    symbol: String,
    qty: String,
    entryPrice: Float,
    lastTradedPrice: Float,
    isBuy: Boolean = true,
    initialTp: Float? = null,
    initialSl: Float? = null,
    initialPartialOrders: List<PartialOrder> = emptyList(),
    currentPrice: Float = 0f,
    onConfirm: (Float?, Float?, List<PartialOrder>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Modify Position") }
    
    val tickSize = if (symbol.uppercase().contains("BTC")) 0.1f else 0.00001f
    val precision = if (symbol.uppercase().contains("BTC")) 1 else 5
    fun formatPriceValue(price: Float): String = String.format("%.${precision}f", price).replace(",", ".")

    var tpTriggerPrice by remember { mutableStateOf(initialTp?.let { formatPriceValue(it) } ?: "") }
    var slTriggerPrice by remember { mutableStateOf(initialSl?.let { formatPriceValue(it) } ?: "") }
    var tpSecondaryMode by remember { mutableStateOf("ticks") }
    var slSecondaryMode by remember { mutableStateOf("ticks") }
    var showTpSecondaryOptions by remember { mutableStateOf(false) }
    var showSlSecondaryOptions by remember { mutableStateOf(false) }
    
    val partialOrders = remember { mutableStateListOf<PartialOrder>().apply { addAll(initialPartialOrders) } }
    
    // State for adding/modifying a partial order
    var isEditingPartial by remember { mutableStateOf(false) }
    var partialOrderId by remember { mutableStateOf<String?>(null) }
    var partialQty by remember { mutableStateOf("0.001") }
    var partialTpPrice by remember { mutableStateOf("") }
    var partialSlPrice by remember { mutableStateOf("") }

    val primaryColor = Color(0xFF2962FF)
    val backgroundColor = Color.Black
    val boxBorderColor = Color(0xFF2A2E39)
    val boxBackgroundColor = Color(0xFF121212)
    val topSectionBackground = Color(0xFF121212)
    val topSectionHeight = 8.dp

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val referencePrice = if (currentPrice > 0f) currentPrice else lastTradedPrice
    val quantityValue = qty.toFloatOrNull() ?: 1f
    val tpSecondaryOptions = listOf(
        "Ticks" to "ticks",
        "% price" to "percent",
        "Reward, USD" to "usd",
        "Reward, % balance" to "balance_percent"
    )
    val slSecondaryOptions = listOf(
        "Ticks" to "ticks",
        "% price" to "percent",
        "Risk, USD" to "usd",
        "Risk, % balance" to "balance_percent"
    )

    fun secondaryValueText(mode: String, targetPrice: String): String {
        val targetValue = targetPrice.toFloatOrNull() ?: referencePrice
        val distance = abs(targetValue - entryPrice)
        val ticks = if (tickSize > 0f) (distance / tickSize).roundToInt() else 0
        val percent = if (entryPrice > 0f) (distance / entryPrice) * 100f else 0f
        val usd = distance * quantityValue
        return when (mode) {
            "ticks" -> "${ticks} ticks"
            "percent" -> "${String.format(Locale.US, "%.2f", percent)}%"
            "usd" -> "${String.format(Locale.US, "%.2f", usd)} USD"
            "balance_percent" -> "${String.format(Locale.US, "%.2f", percent / 10f)}%"
            else -> "${ticks} ticks"
        }
    }

    val tpSecondaryText = secondaryValueText(tpSecondaryMode, tpTriggerPrice)
    val slSecondaryText = secondaryValueText(slSecondaryMode, slTriggerPrice)

    ModalBottomSheet(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppBottomNavHeight),
        onDismissRequest = onCancel,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topSectionBackground)
                    .padding(top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF787B86))
                )
            }
        },
        containerColor = backgroundColor,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topSectionBackground)
                        .height(topSectionHeight)
                )

                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    thickness = 1.dp
                )

                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp).padding(horizontal = 16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingPartial) {
                        Text("Modify Partial Order", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val ticker = symbol.uppercase()
                            if (ticker.contains("BTC")) {
                                CryptoLogo("BTC", size = 24)
                            } else if (ticker.contains("ETH")) {
                                CryptoLogo("ETH", size = 24)
                            } else {
                                val flagCode = if (ticker.length >= 3) ticker.take(3) else ticker
                                FlagImage(currency = flagCode, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = if (isEditingPartial) { { isEditingPartial = false } } else onCancel, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF787B86))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Entry Price", String.format("%,.2f", entryPrice))
                    StatItem("Qty", qty)
                    StatItem("Last Traded Price", String.format("%,.2f", lastTradedPrice))
                    val liqPrice = if (isBuy) entryPrice * 0.8f else entryPrice * 1.2f
                    StatItem("Liq. Price", String.format("%,.1f", liqPrice), color = Color(0xFFF2A52C))
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!isEditingPartial) {
                    // Tabs
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TabItem("Modify Position", selectedTab == "Modify Position", primaryColor) { selectedTab = "Modify Position" }
                        Spacer(modifier = Modifier.width(16.dp))
                        TabItem("Partial Position", selectedTab == "Partial Position", primaryColor, isNew = true) { selectedTab = "Partial Position" }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.HelpOutline, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when (selectedTab) {
                        "Modify Position" -> {
                            // Take Profit Section
                            val tpVal = tpTriggerPrice.toFloatOrNull()
                            val tpPnl = tpVal?.let { if (isBuy) (it - entryPrice) * (qty.toFloatOrNull() ?: 1f) else (entryPrice - it) * (qty.toFloatOrNull() ?: 1f) }
                            val tpPnlText = tpPnl?.let { " (${if(it >= 0) "+" else ""}${String.format("%.2f", it)})" } ?: ""

                            Text("Take profit, price$tpPnlText", color = Color(0xFF787B86), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            SimpleOrderStyleExitInputRow(
                                value = tpTriggerPrice,
                                onValueChange = { tpTriggerPrice = it },
                                secondaryText = tpSecondaryText,
                                dropdownExpanded = showTpSecondaryOptions,
                                onDropdownExpandedChange = { showTpSecondaryOptions = it },
                                dropdownOptions = tpSecondaryOptions,
                                onOptionSelected = { mode -> tpSecondaryMode = mode },
                                borderColor = boxBorderColor,
                                backgroundColor = boxBackgroundColor
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            PercentageSelector(
                                borderColor = boxBorderColor,
                                backgroundColor = boxBackgroundColor,
                                onPercentSelect = { pct ->
                                val target = if (isBuy) entryPrice * (1f + pct / 100f) else entryPrice * (1f - pct / 100f)
                                tpTriggerPrice = formatPriceValue(target)
                            })

                            Spacer(modifier = Modifier.height(12.dp))
                            SummaryText("Take Profit", tpTriggerPrice, entryPrice, qty.toFloatOrNull() ?: 1f, isBuy, isProfit = true)

                            Spacer(modifier = Modifier.height(24.dp))

                            // Stop Loss Section
                            val slVal = slTriggerPrice.toFloatOrNull()
                            val slPnl = slVal?.let { if (isBuy) (it - entryPrice) * (qty.toFloatOrNull() ?: 1f) else (entryPrice - it) * (qty.toFloatOrNull() ?: 1f) }
                            val slPnlText = slPnl?.let { " (${if(it >= 0) "+" else ""}${String.format("%.2f", it)})" } ?: ""

                            Text("Stop loss, price$slPnlText", color = Color(0xFF787B86), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            SimpleOrderStyleExitInputRow(
                                value = slTriggerPrice,
                                onValueChange = { slTriggerPrice = it },
                                secondaryText = slSecondaryText,
                                dropdownExpanded = showSlSecondaryOptions,
                                onDropdownExpandedChange = { showSlSecondaryOptions = it },
                                dropdownOptions = slSecondaryOptions,
                                onOptionSelected = { mode -> slSecondaryMode = mode },
                                borderColor = boxBorderColor,
                                backgroundColor = boxBackgroundColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            PercentageSelector(
                                borderColor = boxBorderColor,
                                backgroundColor = boxBackgroundColor,
                                onPercentSelect = { pct ->
                                val target = if (isBuy) entryPrice * (1f - pct / 100f) else entryPrice * (1f + pct / 100f)
                                slTriggerPrice = formatPriceValue(target)
                            })

                            Spacer(modifier = Modifier.height(12.dp))
                            SummaryText("Stop Loss", slTriggerPrice, entryPrice, qty.toFloatOrNull() ?: 1f, isBuy, isProfit = false)

                        }
                        "Partial Position" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Sort by Order Time", color = Color(0xFF787B86), fontSize = 14.sp)
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
                                    }
                                    Row {
                                        Button(onClick = { partialOrders.clear() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E39)), shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                            Text("Cancel All", color = Color.White, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { 
                                                partialOrderId = null
                                                partialQty = "0.001"
                                                partialTpPrice = formatPriceValue(if (isBuy) entryPrice * 1.05f else entryPrice * 0.95f)
                                                partialSlPrice = formatPriceValue(if (isBuy) entryPrice * 0.95f else entryPrice * 1.05f)
                                                isEditingPartial = true
                                            }, 
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor), 
                                            shape = RoundedCornerShape(4.dp), 
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Add", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text("Take Profit", color = Color(0xFF787B86), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Stop Loss", color = Color(0xFF787B86), fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Text("TP Order Price", color = Color(0xFF787B86), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text("Qty", color = Color(0xFF787B86), fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                    Text("SL Order Price", color = Color(0xFF787B86), fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                                
                                Divider(color = boxBorderColor)
                                
                                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                    items(partialOrders) { order ->
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                val tpPnl = order.tp?.let { if (isBuy) (it - entryPrice) * order.volume else (entryPrice - it) * order.volume }
                                                val slPnl = order.sl?.let { if (isBuy) (it - entryPrice) * order.volume else (entryPrice - it) * order.volume }

                                                Text(
                                                    text = order.tp?.let { String.format("%,.1f(Last)%s", it, tpPnl?.let { pnl -> " (${if(pnl>=0) "+" else ""}${String.format("%.2f", pnl)})" } ?: "") } ?: "--",
                                                    color = Color(0xFF089981),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = order.sl?.let { String.format("%s%,.1f(Last)", slPnl?.let { pnl -> "(${if(pnl>=0) "+" else ""}${String.format("%.2f", pnl)}) " } ?: "", it) } ?: "--",
                                                    color = Color(0xFFF23645),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text(order.tpOrderPrice, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                                Text(order.volume.toString(), color = Color.White, fontSize = 13.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                                Text(order.slOrderPrice, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { 
                                                        partialOrderId = order.id
                                                        partialQty = order.volume.toString()
                                                        partialTpPrice = order.tp?.let { formatPriceValue(it) } ?: ""
                                                        partialSlPrice = order.sl?.let { formatPriceValue(it) } ?: ""
                                                        isEditingPartial = true
                                                    }, 
                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E39)),
                                                    shape = RoundedCornerShape(4.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Modify", color = Color.White, fontSize = 12.sp)
                                                }
                                                Button(
                                                    onClick = { partialOrders.remove(order) }, 
                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E39)),
                                                    shape = RoundedCornerShape(4.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("Cancel", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                        Divider(color = boxBorderColor)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Qty", color = Color(0xFF787B86), fontSize = 14.sp)
                        Box(modifier = Modifier.fillMaxWidth().height(48.dp).padding(vertical = 4.dp).background(boxBackgroundColor, RoundedCornerShape(4.dp)).border(1.dp, boxBorderColor, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                            BasicTextField(value = partialQty, onValueChange = { partialQty = it }, textStyle = TextStyle(color = Color.White, fontSize = 16.sp), modifier = Modifier.fillMaxWidth())
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Take Profit Price", color = Color(0xFF787B86), fontSize = 14.sp)
                        PriceInputRow(value = partialTpPrice, onValueChange = { partialTpPrice = it }, onIncrement = { partialTpPrice = formatPriceValue((partialTpPrice.toFloatOrNull() ?: entryPrice) + tickSize) }, onDecrement = { partialTpPrice = formatPriceValue((partialTpPrice.toFloatOrNull() ?: entryPrice) - tickSize) }, selectedSource = "Last", onSourceChange = {}, primaryColor = primaryColor, borderColor = boxBorderColor, backgroundColor = boxBackgroundColor)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Stop Loss Price", color = Color(0xFF787B86), fontSize = 14.sp)
                        PriceInputRow(value = partialSlPrice, onValueChange = { partialSlPrice = it }, onIncrement = { partialSlPrice = formatPriceValue((partialSlPrice.toFloatOrNull() ?: entryPrice) + tickSize) }, onDecrement = { partialSlPrice = formatPriceValue((partialSlPrice.toFloatOrNull() ?: entryPrice) - tickSize) }, selectedSource = "Last", onSourceChange = {}, primaryColor = primaryColor, borderColor = boxBorderColor, backgroundColor = boxBackgroundColor)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = {
                                    val newOrder = PartialOrder(
                                        id = partialOrderId ?: java.util.UUID.randomUUID().toString(),
                                        volume = partialQty.toFloatOrNull() ?: 0.001f,
                                        tp = partialTpPrice.toFloatOrNull(),
                                        sl = partialSlPrice.toFloatOrNull()
                                    )
                                    val index = partialOrders.indexOfFirst { it.id == partialOrderId }
                                    if (index != -1) partialOrders[index] = newOrder else partialOrders.add(newOrder)
                                    isEditingPartial = false
                                },
                                modifier = Modifier.width(110.dp).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Save", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { isEditingPartial = false },
                                modifier = Modifier.width(110.dp).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E39)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Back", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!isEditingPartial) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, boxBorderColor),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 16.sp)
                        }
                        Button(
                            onClick = { onConfirm(tpTriggerPrice.toFloatOrNull(), slTriggerPrice.toFloatOrNull(), partialOrders.toList()) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Confirm", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color = Color.White) {
    Column {
        Text(label, color = Color(0xFF787B86), fontSize = 14.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TabItem(label: String, isSelected: Boolean, primaryColor: Color, isNew: Boolean = false, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (isSelected) Color.White else Color(0xFF787B86), fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isNew) {
                Spacer(modifier = Modifier.width(4.dp))
                Text("new", color = Color(0xFFF2A52C), fontSize = 10.sp)
            }
        }
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(40.dp).height(2.dp).background(primaryColor))
        }
    }
}

@Composable
fun SimpleOrderStyleExitInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    secondaryText: String,
    dropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    dropdownOptions: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit,
    borderColor: Color,
    backgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))

        Box(contentAlignment = Alignment.CenterEnd) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onDropdownExpandedChange(true) }
            ) {
                Text(
                    text = secondaryText,
                    color = Color(0xFF787B86),
                    fontSize = 14.sp
                )
                Icon(
                    if (dropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color(0xFF787B86),
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { onDropdownExpandedChange(false) },
                modifier = Modifier
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF2A2E39))
            ) {
                dropdownOptions.forEach { (label, mode) ->
                    DropdownMenuItem(
                        text = { Text(label, color = Color.White, fontSize = 14.sp) },
                        onClick = {
                            onOptionSelected(mode)
                            onDropdownExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PriceInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    selectedSource: String,
    onSourceChange: (String) -> Unit,
    primaryColor: Color,
    borderColor: Color,
    backgroundColor: Color
) {
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(backgroundColor, RoundedCornerShape(4.dp)).border(1.dp, borderColor, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                    cursorBrush = SolidColor(primaryColor),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Icon(Icons.Default.MoreHoriz, null, tint = Color(0xFFF2A52C), modifier = Modifier.size(18.dp).clickable { onValueChange("") })
                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("—", color = Color(0xFF787B86), fontSize = 18.sp, modifier = Modifier.clickable { onDecrement() })
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF2A2E39)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("+", color = Color(0xFF787B86), fontSize = 18.sp, modifier = Modifier.clickable { onIncrement() })
                }
            }
        }
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(backgroundColor, RoundedCornerShape(4.dp))
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable { sourceDropdownExpanded = true }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedSource, color = Color.White, fontSize = 14.sp)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = sourceDropdownExpanded,
                onDismissRequest = { sourceDropdownExpanded = false },
                modifier = Modifier.background(Color(0xFF121212)).border(1.dp, Color(0xFF363A45))
            ) {
                listOf("Last", "Bid", "Ask").forEach { source ->
                    DropdownMenuItem(
                        text = { Text(source, color = Color.White) },
                        onClick = {
                            onSourceChange(source)
                            sourceDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PercentageSelector(borderColor: Color, backgroundColor: Color, onPercentSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(5, 10, 15, 20, 25).forEach { pct ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .padding(horizontal = 2.dp)
                    .background(backgroundColor, RoundedCornerShape(4.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .clickable { onPercentSelect(pct) },
                contentAlignment = Alignment.Center
            ) {
                Text("$pct%", color = Color(0xFF787B86), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SummaryText(type: String, targetPrice: String, entryPrice: Float, volume: Float, isBuy: Boolean, isProfit: Boolean) {
    val target = targetPrice.toFloatOrNull() ?: entryPrice
    val diff = if (isBuy) {
        target - entryPrice
    } else {
        entryPrice - target
    }

    val actualIsProfit = diff >= 0
    val amount = abs(diff * volume)
    val roi = if (entryPrice != 0f) (abs(diff) / entryPrice) * 100f else 0f

    val profitLossText = if (actualIsProfit) "profit" else "loss"

    Text(
        text = "Last Traded Price to ${if (targetPrice.isEmpty()) "..." else targetPrice} will trigger market $type order; your expected $profitLossText will be ${String.format("%.4f", amount)} USD (ROI: ${String.format("%.2f", roi)}%)",
        color = Color(0xFF787B86),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    )
}
