package com.asc.markets.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.logic.MT5BridgeClient
import com.asc.markets.ui.components.OrderFlowMiniChart
import com.asc.markets.ui.screens.dashboard.OrderBookLadder
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.HairlineBorder
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import com.trading.app.data.BinanceService
import com.trading.app.models.OHLCData
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private enum class OrderFlowAssetMode {
    LINKED,
    CUSTOM
}

private data class OrderFlowTimeframe(
    val code: String,
    val seconds: Long,
    val candleCount: Int,
    val smoothingWindow: Int,
    val amplitudeMultiplier: Double,
    val waveWeight: Double
)

private val orderFlowTimeframes = listOf(
    OrderFlowTimeframe(code = "D1", seconds = 86_400L, candleCount = 84, smoothingWindow = 5, amplitudeMultiplier = 1.8, waveWeight = 9.0),
    OrderFlowTimeframe(code = "H4", seconds = 14_400L, candleCount = 92, smoothingWindow = 4, amplitudeMultiplier = 1.45, waveWeight = 7.0),
    OrderFlowTimeframe(code = "H1", seconds = 3_600L, candleCount = 100, smoothingWindow = 3, amplitudeMultiplier = 1.2, waveWeight = 5.5),
    OrderFlowTimeframe(code = "M30", seconds = 1_800L, candleCount = 108, smoothingWindow = 2, amplitudeMultiplier = 1.0, waveWeight = 4.0),
    OrderFlowTimeframe(code = "M15", seconds = 900L, candleCount = 114, smoothingWindow = 2, amplitudeMultiplier = 0.82, waveWeight = 3.2),
    OrderFlowTimeframe(code = "M5", seconds = 300L, candleCount = 120, smoothingWindow = 1, amplitudeMultiplier = 0.68, waveWeight = 2.2)
)

@Composable
fun MultiTimeframeScreen(symbol: String) {
    val allPairs by MarketDataStore.allPairs.collectAsState()
    val priceHistory by MarketDataStore.priceHistory.collectAsState()
    val assetOptions = remember(allPairs) { allPairs.distinctBy(ForexPair::symbol).sortedBy(ForexPair::symbol) }

    var assetModeName by rememberSaveable { mutableStateOf(OrderFlowAssetMode.LINKED.name) }
    val assetMode = remember(assetModeName) { OrderFlowAssetMode.valueOf(assetModeName) }

    var customSymbols by rememberSaveable {
        mutableStateOf(orderFlowTimeframes.associate { it.code to symbol })
    }

    LaunchedEffect(symbol) {
        if (customSymbols.isEmpty()) {
            customSymbols = orderFlowTimeframes.associate { it.code to symbol }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        OrderFlowModePanel(
            currentSymbol = symbol,
            assetMode = assetMode,
            onModeChange = { assetModeName = it.name }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orderFlowTimeframes, key = { it.code }) { timeframe ->
                val resolvedSymbol = if (assetMode == OrderFlowAssetMode.LINKED) {
                    symbol
                } else {
                    customSymbols[timeframe.code] ?: symbol
                }
                val pair = remember(allPairs, resolvedSymbol) { resolvePair(allPairs, resolvedSymbol) }
                val baseHistory = remember(priceHistory, allPairs, resolvedSymbol) {
                    resolveHistory(priceHistory, allPairs, resolvedSymbol)
                }
                val binanceHistory = rememberBinanceOrderFlowHistory(
                    symbol = resolvedSymbol,
                    timeframe = timeframe
                )
                val mt5History = rememberMt5OrderFlowHistory(
                    symbol = resolvedSymbol,
                    timeframe = timeframe
                )
                val preferredHistory = remember(resolvedSymbol, binanceHistory, mt5History) {
                    when {
                        shouldUseBinanceOrderFlowHistory(resolvedSymbol) && binanceHistory.isNotEmpty() -> binanceHistory
                        mt5History.isNotEmpty() -> mt5History
                        else -> emptyList()
                    }
                }
                val fallbackPrice = pair?.price
                    ?: preferredHistory.lastOrNull()?.close?.toDouble()
                    ?: baseHistory.lastOrNull()
                    ?: 0.0
                val candles = remember(resolvedSymbol, timeframe, preferredHistory, baseHistory, fallbackPrice) {
                    if (preferredHistory.isNotEmpty()) {
                        selectCardCandles(preferredHistory, timeframe)
                    } else {
                        buildTimeframeCandles(
                            symbol = resolvedSymbol,
                            timeframe = timeframe,
                            baseHistory = baseHistory,
                            lastPrice = fallbackPrice
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = timeframe.code,
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(start = 12.dp)
                    )

                    MiniChartContainer(
                        symbol = resolvedSymbol,
                        pair = pair,
                        candles = candles,
                        showAssetPicker = assetMode == OrderFlowAssetMode.CUSTOM,
                        assetOptions = assetOptions,
                        onAssetSelected = { selectedSymbol ->
                            customSymbols = customSymbols + (timeframe.code to selectedSymbol)
                        }
                    )
                    
                    // Order Book under each timeframe box
                    if (pair != null) {
                        OrderBookLadder(
                            selectedPair = pair,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderFlowModePanel(
    currentSymbol: String,
    assetMode: OrderFlowAssetMode,
    onModeChange: (OrderFlowAssetMode) -> Unit
) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Asset source",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderFlowModeChip(
                    label = "Linked",
                    selected = assetMode == OrderFlowAssetMode.LINKED,
                    onClick = { onModeChange(OrderFlowAssetMode.LINKED) }
                )
                OrderFlowModeChip(
                    label = "Custom",
                    selected = assetMode == OrderFlowAssetMode.CUSTOM,
                    onClick = { onModeChange(OrderFlowAssetMode.CUSTOM) }
                )
            }
            Text(
                text = if (assetMode == OrderFlowAssetMode.LINKED) {
                    "All timeframe boxes follow the asset you are currently viewing: $currentSymbol"
                } else {
                    "Each timeframe box can track an independent asset of your choice."
                },
                color = SlateText,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun OrderFlowModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) IndigoAccent.copy(alpha = 0.16f) else DeepBlack,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) IndigoAccent else HairlineBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else SlateText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MiniChartContainer(
    symbol: String,
    pair: ForexPair?,
    candles: List<OHLCData>,
    showAssetPicker: Boolean,
    assetOptions: List<ForexPair>,
    onAssetSelected: (String) -> Unit
) {
    val compactSymbol = remember(symbol) { displaySymbol(symbol) }
    val quoteAsset = remember(symbol) { extractQuoteAsset(symbol) }
    val livePrice = pair?.price?.toFloat()?.takeIf { it.isFinite() && it > 0f }
    val displayCandles = remember(candles, livePrice) {
        applyLivePriceToCandles(candles, livePrice)
    }
    val lastPrice = livePrice ?: displayCandles.lastOrNull()?.close ?: 0f
    val changePercent = pair?.changePercent?.toFloat()?.takeIf { it.isFinite() }
        ?: remember(displayCandles, pair) {
            calculateChangePercent(displayCandles, pair?.changePercent?.toFloat())
        }
    val changeColor = if (changePercent >= 0f) EmeraldSuccess else RoseError
    val cardHeight = if (showAssetPicker) 396.dp else 364.dp

    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OrderFlowSymbolBadge(compactSymbol)
                    if (showAssetPicker) {
                        AssetPicker(
                            selectedSymbol = symbol,
                            assetOptions = assetOptions,
                            onAssetSelected = onAssetSelected
                        )
                    } else {
                        Text(
                            text = compactSymbol,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatPrice(lastPrice),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
                if (quoteAsset != null) {
                    Text(
                        text = quoteAsset,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = formatSignedPercent(changePercent),
                    color = changeColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini Chart (Full Width)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Transparent)
            ) {
                OrderFlowMiniChart(
                    candles = displayCandles,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun OrderFlowSymbolBadge(symbol: String) {
    val (label, background, foreground) = when {
        symbol.startsWith("BTC") -> Triple("B", Color(0xFFF59E38), Color.White)
        symbol.startsWith("ETH") -> Triple("E", Color(0xFF5C6BC0), Color.White)
        symbol.startsWith("XAU") -> Triple("AU", Color(0xFFD4AF37), Color.Black)
        else -> Triple(symbol.take(2), Color(0xFF1C2630), Color.White)
    }

    Surface(
        color = background,
        shape = CircleShape,
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun AssetPicker(
    selectedSymbol: String,
    assetOptions: List<ForexPair>,
    onAssetSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            color = DeepBlack,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedSymbol,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "Change",
                    color = IndigoAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 320.dp)
                .background(PureBlack)
        ) {
            assetOptions.forEach { pair ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = pair.symbol,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = pair.name,
                                color = SlateText,
                                fontSize = 10.sp,
                                fontFamily = InterFontFamily
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onAssetSelected(pair.symbol)
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberBinanceOrderFlowHistory(
    symbol: String,
    timeframe: OrderFlowTimeframe
): List<OHLCData> {
    val normalizedSymbol = remember(symbol) { displaySymbol(symbol) }
    val useBinanceHistory = remember(symbol) { shouldUseBinanceOrderFlowHistory(symbol) }
    val binanceTimeframe = remember(timeframe.code) { toBinanceTimeframe(timeframe.code) }
    var history by remember(symbol, timeframe.code) { mutableStateOf<List<OHLCData>>(emptyList()) }

    DisposableEffect(normalizedSymbol, binanceTimeframe, useBinanceHistory) {
        history = emptyList()
        if (!useBinanceHistory) {
            return@DisposableEffect onDispose {}
        }

        val service = BinanceService(
            onQuoteUpdate = {},
            onHistoryUpdate = { receivedSymbol, incomingHistory ->
                if (receivedSymbol.equals(normalizedSymbol, ignoreCase = true) && incomingHistory.isNotEmpty()) {
                    history = sanitizeHistoricalCandles(incomingHistory)
                }
            }
        )
        service.fetchHistory(normalizedSymbol, binanceTimeframe, null)

        onDispose {
            service.disconnect()
        }
    }

    return history
}

@Composable
private fun rememberMt5OrderFlowHistory(
    symbol: String,
    timeframe: OrderFlowTimeframe
): List<OHLCData> {
    val context = LocalContext.current
    val normalizedSymbol = remember(symbol) { normalizeMt5OrderFlowSymbol(symbol) }
    val mt5Timeframe = remember(timeframe.code) { toMt5Timeframe(timeframe.code) }
    val useMt5History = remember(symbol) { shouldUseMt5OrderFlowHistory(symbol) }
    var history by remember(symbol, timeframe.code) { mutableStateOf<List<OHLCData>>(emptyList()) }

    LaunchedEffect(context.applicationContext, normalizedSymbol, mt5Timeframe, useMt5History) {
        history = emptyList()
        if (!useMt5History) {
            return@LaunchedEffect
        }

        val prefs = context.applicationContext.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        val bridgeUrl = prefs.getString("mt5_bridge_url", "192.168.1.100:62100") ?: "192.168.1.100:62100"
        val client = MT5BridgeClient(bridgeUrl = bridgeUrl, brokerSuffix = "m")
        val bars = client.getHistoricalBars(
            symbol = normalizedSymbol,
            timeframe = mt5Timeframe,
            limit = requestedHistoryCount(timeframe)
        )
        if (bars.isNotEmpty()) {
            history = sanitizeHistoricalCandles(
                bars.map { bar ->
                    OHLCData(
                        time = bar.time,
                        open = bar.open.toFloat(),
                        high = bar.high.toFloat(),
                        low = bar.low.toFloat(),
                        close = bar.close.toFloat(),
                        volume = bar.volume.toFloat()
                    )
                }
            )
        }
    }

    return history
}

private fun resolvePair(pairs: List<ForexPair>, symbol: String): ForexPair? {
    return pairs.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
        ?: pairs.firstOrNull { MarketDataStore.matchesSymbol(it.symbol, symbol) }
}

private fun resolveHistory(
    historyMap: Map<String, List<Double>>,
    pairs: List<ForexPair>,
    symbol: String
): List<Double> {
    val canonicalSymbol = resolvePair(pairs, symbol)?.symbol
    if (canonicalSymbol != null) {
        return historyMap[canonicalSymbol] ?: emptyList()
    }
    return historyMap.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, symbol) }?.value
        ?: emptyList()
}

private fun buildTimeframeCandles(
    symbol: String,
    timeframe: OrderFlowTimeframe,
    baseHistory: List<Double>,
    lastPrice: Double
): List<OHLCData> {
    val sourceSeries = if (baseHistory.size >= 8) {
        baseHistory
    } else {
        generateFallbackSeries(symbol, timeframe, lastPrice)
    }
    val resampled = resampleSeries(sourceSeries, timeframe.candleCount)
    val smoothed = smoothSeries(resampled, timeframe.smoothingWindow)
    val pivot = smoothed.average().takeIf { it.isFinite() } ?: lastPrice.takeIf { it > 0.0 } ?: 1.0
    val tickSize = estimateTickSize(lastPrice.takeIf { it > 0.0 } ?: pivot)
    val shapedSeries = smoothed.mapIndexed { index, price ->
        val centered = pivot + ((price - pivot) * timeframe.amplitudeMultiplier)
        val wave = sin(index * 0.6 + timeframe.seconds.toDouble() / 7_200.0) * tickSize * timeframe.waveWeight
        (centered + wave).coerceAtLeast(tickSize)
    }

    val now = System.currentTimeMillis() / 1000L
    return shapedSeries.mapIndexed { index, closeValue ->
        val previousClose = shapedSeries.getOrElse(index - 1) { closeValue }
        val bodyRange = max(tickSize * 2.0, abs(closeValue - previousClose) * 0.85)
        val openValue = previousClose
        val close = closeValue.toFloat()
        val high = (max(openValue, closeValue) + bodyRange * 0.55).toFloat()
        val low = (min(openValue, closeValue) - bodyRange * 0.55).toFloat()
        OHLCData(
            time = now - ((shapedSeries.size - index).toLong() * timeframe.seconds),
            open = openValue.toFloat(),
            high = high,
            low = low,
            close = close,
            volume = 0f
        )
    }
}

private fun generateFallbackSeries(
    symbol: String,
    timeframe: OrderFlowTimeframe,
    lastPrice: Double
): List<Double> {
    val basePrice = lastPrice.takeIf { it > 0.0 } ?: 100.0
    val seed = symbol.uppercase(Locale.US).hashCode().toLong() * 31L + timeframe.code.hashCode().toLong()
    val random = Random(seed)
    var price = basePrice
    return List(max(timeframe.candleCount, 40)) { index ->
        val tickSize = estimateTickSize(price)
        val volatility = max(price * 0.0022 * timeframe.amplitudeMultiplier, tickSize * 4.0)
        val drift = sin(index * 0.42 + timeframe.seconds.toDouble() / 5_400.0) * volatility * 0.18
        price = (price + random.nextDouble(-volatility, volatility) + drift).coerceAtLeast(tickSize)
        price
    }
}

private fun selectCardCandles(
    candles: List<OHLCData>,
    timeframe: OrderFlowTimeframe
): List<OHLCData> {
    val ordered = candles.sortedBy(OHLCData::time)
    val preferredWindow = when (timeframe.code) {
        "D1" -> 180
        "H4" -> 180
        "H1" -> 220
        "M30" -> 240
        "M15" -> 260
        "M5" -> 288
        else -> 220
    }
    return ordered.takeLast(preferredWindow)
}

private fun requestedHistoryCount(timeframe: OrderFlowTimeframe): Int {
    return when (timeframe.code) {
        "D1" -> 220
        "H4" -> 240
        "H1" -> 260
        "M30" -> 300
        "M15" -> 320
        "M5" -> 360
        else -> 260
    }
}

private fun sanitizeHistoricalCandles(candles: List<OHLCData>): List<OHLCData> {
    return candles
        .asSequence()
        .filter { candle ->
            candle.time > 0L &&
                candle.open.isFinite() &&
                candle.high.isFinite() &&
                candle.low.isFinite() &&
                candle.close.isFinite()
        }
        .map { candle ->
            val high = max(candle.high, max(candle.open, candle.close))
            val low = min(candle.low, min(candle.open, candle.close))
            candle.copy(
                high = high,
                low = low,
                volume = candle.volume.coerceAtLeast(0f)
            )
        }
        .sortedBy(OHLCData::time)
        .distinctBy(OHLCData::time)
        .toList()
}

private fun resampleSeries(values: List<Double>, targetSize: Int): List<Double> {
    if (values.isEmpty()) return List(targetSize) { 0.0 }
    if (values.size == targetSize) return values
    if (targetSize <= 1) return listOf(values.last())

    return List(targetSize) { index ->
        val position = index.toDouble() * (values.lastIndex.toDouble() / (targetSize - 1).toDouble())
        val lowerIndex = position.toInt()
        val upperIndex = min(lowerIndex + 1, values.lastIndex)
        val fraction = position - lowerIndex
        val lower = values[lowerIndex]
        val upper = values[upperIndex]
        lower + ((upper - lower) * fraction)
    }
}

private fun smoothSeries(values: List<Double>, windowSize: Int): List<Double> {
    if (windowSize <= 1) return values
    return values.indices.map { index ->
        val start = max(0, index - windowSize + 1)
        val slice = values.subList(start, index + 1)
        slice.average()
    }
}

private fun estimateTickSize(price: Double): Double {
    return when {
        price >= 10_000 -> 5.0
        price >= 1_000 -> 1.0
        price >= 100 -> 0.1
        price >= 10 -> 0.01
        price >= 1 -> 0.0001
        else -> 0.00001
    }
}

private fun shouldUseMt5OrderFlowHistory(symbol: String): Boolean {
    return symbol.isNotBlank()
}

private fun shouldUseBinanceOrderFlowHistory(symbol: String): Boolean {
    val compactSymbol = displaySymbol(symbol)
    return compactSymbol.endsWith("USDT")
}

private fun normalizeMt5OrderFlowSymbol(symbol: String): String {
    return displaySymbol(symbol)
}

private fun toMt5Timeframe(code: String): String {
    return when (code.uppercase(Locale.US)) {
        "D1" -> "D"
        "H4" -> "4h"
        "H1" -> "1h"
        "M30" -> "30m"
        "M15" -> "15m"
        "M5" -> "5m"
        else -> "1h"
    }
}

private fun toBinanceTimeframe(code: String): String {
    return when (code.uppercase(Locale.US)) {
        "D1" -> "d"
        "H4" -> "4h"
        "H1" -> "1h"
        "M30" -> "30m"
        "M15" -> "15m"
        "M5" -> "5m"
        else -> "1h"
    }
}

private fun displaySymbol(symbol: String): String {
    return symbol.replace("/", "").uppercase(Locale.US)
}

private fun extractQuoteAsset(symbol: String): String? {
    val compactSymbol = displaySymbol(symbol)
    return when {
        symbol.contains("/") -> symbol.substringAfter("/").uppercase(Locale.US)
        compactSymbol.endsWith("USDT") -> "USDT"
        compactSymbol.endsWith("USDC") -> "USDC"
        compactSymbol.endsWith("BUSD") -> "BUSD"
        compactSymbol.endsWith("USD") -> "USD"
        compactSymbol.endsWith("JPY") -> "JPY"
        compactSymbol.endsWith("EUR") -> "EUR"
        compactSymbol.endsWith("GBP") -> "GBP"
        compactSymbol.endsWith("CHF") -> "CHF"
        else -> null
    }
}

private fun calculateChangePercent(candles: List<OHLCData>, fallback: Float?): Float {
    if (candles.size < 2) return fallback ?: 0f
    val firstClose = candles.first().close
    if (!firstClose.isFinite() || firstClose == 0f) return fallback ?: 0f
    return ((candles.last().close - firstClose) / firstClose) * 100f
}

private fun formatSignedPercent(value: Float): String {
    val sign = if (value >= 0f) "+" else ""
    return "$sign${String.format(Locale.US, "%.2f", value)}%"
}

private fun formatPrice(value: Float): String {
    val absolute = abs(value)
    return when {
        absolute >= 1_000f -> String.format(Locale.US, "%.2f", value)
        absolute >= 10f -> String.format(Locale.US, "%.3f", value)
        absolute >= 1f -> String.format(Locale.US, "%.4f", value)
        else -> String.format(Locale.US, "%.5f", value)
    }
}

private fun applyLivePriceToCandles(
    candles: List<OHLCData>,
    livePrice: Float?
): List<OHLCData> {
    if (candles.isEmpty() || livePrice == null || !livePrice.isFinite() || livePrice <= 0f) {
        return candles
    }

    val updated = candles.toMutableList()
    val last = updated.last()
    updated[updated.lastIndex] = last.copy(
        high = max(last.high, livePrice),
        low = min(last.low, livePrice),
        close = livePrice
    )
    return updated
}
