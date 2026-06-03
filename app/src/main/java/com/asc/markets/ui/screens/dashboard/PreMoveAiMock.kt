package com.asc.markets.ui.screens.dashboard

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.ForexPair
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.TimedPrice
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.PureBlack
import com.trading.app.data.BinanceService
import com.trading.app.data.DerivService
import com.trading.app.data.FredService
import com.asc.markets.network.TiingoIexRestClient
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

val DarkSurface = Color(0xFF0D0D0D)
val DarkSurfaceElevated = Color(0xFF141414)
val TextGray = Color(0xFF9E9E9E)
val PreMoveGreen = Color(0xFF4CAF50)
val ExpansionBlue = Color(0xFF2196F3)
val CompressionYellow = Color(0xFFDCEB3A)
val StructureOrange = Color(0xFFFFA726)
val NoiseRed = Color(0xFFEF5350)

private data class PhaseStep(val label: String, val caption: String, val start: Float, val end: Float, val color: Color)
private data class ProgressDetailCard(val title: String, val metric: String, val value: String, val status: String, val icon: String, val color: Color, val progress: Float?, val active: Boolean)
private data class EntryStyleCard(val title: String, val match: String, val detail: String, val icon: String, val color: Color, val active: Boolean)
private data class PreMovePriceSeries(
    val values: List<Double>,
    val timestamps: List<Long>,
    val axisLabels: List<Double>,
    val windowMillis: Long
)
private data class PreMoveTimeframe(val label: String, val intervalMillis: Long, val windowMillis: Long)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreMoveAiMockImage(
    selectedPair: ForexPair,
    livePairs: List<ForexPair>,
    priceHistory: Map<String, List<Double>>,
    timedPriceHistory: Map<String, List<TimedPrice>> = emptyMap(),
    aiDecisions: List<FinalDecisionItem> = emptyList(),
    onAssetSelected: (ForexPair) -> Unit = {}
) {
    var selectedTimeframe by remember { mutableStateOf("1H") }
    var chartExpanded by remember { mutableStateOf(false) }
    val assetRowScrollState = rememberScrollState()
    val selectedDecision = findAiDecision(selectedPair, aiDecisions)
    val aiCurve = preMoveAiCurve(selectedPair, selectedDecision)
    val aiScore = preMoveScore(selectedPair, selectedDecision)
    
    // ensure chartValues ends with aiScore
    val chartValues = if (aiCurve.isNotEmpty()) {
        aiCurve.dropLast(1) + aiScore
    } else {
        listOf(aiScore)
    }
    
    val priceSeries = preMovePriceSeries(selectedPair, priceHistory, timedPriceHistory, selectedTimeframe)
    val hasChartData = priceSeries.values.size >= 2
    val chartCurrentValue = aiScore
    val currentPhase = selectedDecision?.pre_move_ai_phase?.uppercase(Locale.US) ?: phaseFromScore(chartCurrentValue)
    val scoreFraction = (aiScore / 100f).coerceIn(0f, 1f)
    val scoreState = preMoveState(aiScore, selectedDecision)
    val scoreColor = preMoveColor(aiScore)
    val changeColor = if (selectedPair.change >= 0.0) PreMoveGreen else NoiseRed
    val displayPairs = livePairs
        .filter { it.category == selectedPair.category }
        .takeIf { it.isNotEmpty() }
        ?: livePairs
    val assetRows = displayPairs
        .filter { it.price.isFinite() && it.price > 0.0 }
        .distinctBy { it.symbol }
        .take(5)
        .ifEmpty { listOf(selectedPair) }

    PreMoveMarketHistoryLoader(selectedPair, selectedTimeframe)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // 1. Header (Apple)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PairFlags(symbol = selectedPair.symbol, size = 40)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(assetAlias(selectedPair), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(assetSubtitle(selectedPair), color = TextGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Price and Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatPreMovePrice(selectedPair), color = Color.White, fontSize = 28.8f.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(" ${preMovePriceUnit(selectedPair)}", color = TextGray, fontSize = 12.8f.sp, modifier = Modifier.padding(bottom = 5.dp, start = 4.dp))
                }
                Text(formatPreMoveChange(selectedPair), color = changeColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Box(
                modifier = Modifier
                    .width(206.dp)
                    .height(102.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureBlack)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AI PRE-MOVE SCORE", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(11.dp))
                        }
                        Text("${String.format("%.1f", aiScore)}%", color = scoreColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(scoreState, color = scoreColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Visible)
                    }
                    Box(modifier = Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0xFF2A2A2A),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(StructureOrange, PreMoveGreen, ExpansionBlue)),
                                startAngle = 135f,
                                sweepAngle = 270f * scoreFraction,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Box(modifier = Modifier.size(8.dp).offset(x = 21.dp, y = (-9).dp).clip(CircleShape).background(Color.White))
                        MiniSparkline(values = aiCurve, color = scoreColor, modifier = Modifier.size(34.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Chart and Asset Selector Container with Horizontal Pager
        val pagerState = rememberPagerState(pageCount = { 2 }) // 2 pages: AI Pre-Move Chart and another chart
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PureBlack)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Only the chart scrolls horizontally
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> {
                            // First page: AI Pre-Move Chart
                            PreMoveCombinedChart(
                                selectedPair = selectedPair,
                                priceSeries = priceSeries,
                                aiValues = chartValues,
                                currentAiValue = chartCurrentValue,
                                selectedTimeframe = selectedTimeframe,
                                hasChartData = hasChartData,
                                onTimeframeSelected = { selectedTimeframe = it },
                                onExpandClick = { chartExpanded = true }
                            )
                        }
                        1 -> {
                            // Second page: Live Volatility Chart
                            VolatilityChart(
                                selectedPair = selectedPair,
                                selectedTimeframe = selectedTimeframe,
                                timedPriceHistory = timedPriceHistory,
                                priceHistory = priceHistory,
                                onTimeframeSelected = { selectedTimeframe = it }
                            )
                        }
                    }
                }
                
                // Page indicator dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        Color.White.copy(alpha = 0.8f)
                                    else
                                        Color.White.copy(alpha = 0.3f)
                                )
                        )
                        if (index < 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                // Asset Selector stays fixed (doesn't scroll)
                assetRows.forEachIndexed { index, pair ->
                    val rowDecision = findAiDecision(pair, aiDecisions)
                    val rowCurve = preMoveAiCurve(pair, rowDecision)
                    val rowScore = preMoveScore(pair, rowDecision)
                    val rowColor = preMoveColor(rowScore)
                    StockListItem(
                        name = assetAlias(pair),
                        subtitle = assetSubtitle(pair),
                        score = "${String.format("%.1f", rowScore)}%",
                        price = formatPreMovePrice(pair),
                        priceChange = formatPreMoveRowChange(pair),
                        state = preMoveState(rowScore, rowDecision),
                        color = rowColor,
                        symbol = pair.symbol,
                        values = rowCurve,
                        isHighlighted = normalizeAssetKey(pair.symbol) == normalizeAssetKey(selectedPair.symbol),
                        metricsScrollState = assetRowScrollState,
                        onClick = { onAssetSelected(pair) }
                    )
                    // Add divider between assets (not after the last one)
                    if (index < assetRows.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        }

        if (chartExpanded) {
            ExpandedPreMoveChartDialog(
                selectedPair = selectedPair,
                priceSeries = priceSeries,
                aiValues = chartValues,
                currentAiValue = chartCurrentValue,
                selectedTimeframe = selectedTimeframe,
                hasChartData = hasChartData,
                onTimeframeSelected = { selectedTimeframe = it },
                onDismiss = { chartExpanded = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AiProgressionSection(currentPhase, aiScore)

        Spacer(modifier = Modifier.height(12.dp))
        
        VolatilityRegimeScaleSection(selectedDecision)

        Spacer(modifier = Modifier.height(12.dp))
        
        CurrentProgressDetailsSection(selectedDecision, currentPhase, aiScore)

        Spacer(modifier = Modifier.height(12.dp))

        KeyVolatilityStatsSection(selectedDecision)

        Spacer(modifier = Modifier.height(12.dp))

        EntryStyleSection(selectedDecision, currentPhase, aiScore)
        
        Spacer(modifier = Modifier.height(12.dp))

        FinalTradingDecisionSection(selectedDecision, aiScore)

        Spacer(modifier = Modifier.height(12.dp))
        
        AiInterpretationSection(selectedDecision, currentPhase, aiScore)

        }
    }
}

@Composable
private fun PreMoveMarketHistoryLoader(pair: ForexPair, selectedTimeframe: String) {
    val context = LocalContext.current
    val normalizedPairKey = remember(pair.symbol) { normalizeAssetKey(pair.symbol) }
    val historyInterval = remember(selectedTimeframe) { preMoveHistoryInterval(selectedTimeframe) }
    val binanceSymbol = remember(pair.symbol) { binanceHistorySymbol(pair.symbol) }
    val useBinanceHistory = pair.category == MarketCategory.CRYPTO && binanceSymbol != null
    val useDerivHistory = shouldUseDerivPreMoveHistory(pair)
    val useFredHistory = pair.category == MarketCategory.BONDS
    val fredSeriesId = remember(pair.symbol) { fredSeriesIdForPair(pair.symbol) }
    val useTiingoStockHistory = pair.category == MarketCategory.STOCK
    val tiingoStockSymbol = remember(pair.symbol) { tiingoStockSymbolForPair(pair.symbol) }

    val binanceService = remember(normalizedPairKey, historyInterval) {
        if (!useBinanceHistory || binanceSymbol == null) {
            null
        } else {
            BinanceService(
                onQuoteUpdate = {},
                onHistoryUpdate = { receivedSymbol, history ->
                    if (receivedSymbol.equals(binanceSymbol, ignoreCase = true) && history.isNotEmpty()) {
                        BinanceDataStore.replaceTimedHistory(
                            pair.symbol,
                            history.map { candle ->
                                TimedPrice(candle.time * 1000L, candle.close.toDouble())
                            }
                        )
                    }
                }
            )
        }
    }
    val derivService = remember(normalizedPairKey, historyInterval) {
        if (!useDerivHistory) {
            null
        } else {
            DerivService(
                onQuoteUpdate = {},
                onHistoryUpdate = { receivedSymbol, history ->
                    if (normalizeAssetKey(receivedSymbol) == normalizedPairKey && history.isNotEmpty()) {
                        com.asc.markets.data.CombinedFallbackDataStore.replaceTimedHistory(
                            pair.symbol,
                            history.map { candle ->
                                TimedPrice(candle.time * 1000L, candle.close.toDouble())
                            }
                        )
                    }
                }
            )
        }
    }
    val fredService = remember(normalizedPairKey, historyInterval) {
        if (!useFredHistory || fredSeriesId == null) {
            null
        } else {
            FredService()
        }
    }
    val tiingoIexClient = remember(normalizedPairKey, historyInterval) {
        if (!useTiingoStockHistory || tiingoStockSymbol == null) {
            null
        } else {
            TiingoIexRestClient(com.asc.markets.BuildConfig.TIINGO_API_KEY, context)
        }
    }

    LaunchedEffect(binanceService, derivService, fredService, tiingoIexClient, binanceSymbol, historyInterval, normalizedPairKey, fredSeriesId, tiingoStockSymbol) {
        when {
            binanceService != null && binanceSymbol != null -> {
                binanceService.fetchHistory(binanceSymbol, historyInterval, null)
            }
            derivService != null -> {
                derivService.connect()
                repeat(6) {
                    if (derivService.isConnected()) {
                        derivService.fetchHistory(pair.symbol, historyInterval, null)
                        return@LaunchedEffect
                    }
                    delay(500L)
                }
                derivService.fetchHistory(pair.symbol, historyInterval, null)
            }
            fredService != null && fredSeriesId != null -> {
                fredService.fetchSeriesObservations(fredSeriesId, object : FredService.FredCallback {
                    override fun onSuccess(data: org.json.JSONObject) {
                        val observations = data.optJSONArray("observations")
                        if (observations != null && observations.length() > 0) {
                            val timedPrices = mutableListOf<TimedPrice>()
                            for (i in 0 until observations.length()) {
                                val obs = observations.optJSONObject(i) ?: continue
                                val date = obs.optString("date")
                                val value = obs.optDouble("value", Double.NaN)
                                if (date.isNotBlank() && value.isFinite()) {
                                    val timestamp = parseFredDate(date)
                                    if (timestamp > 0) {
                                        timedPrices.add(TimedPrice(timestamp, value))
                                    }
                                }
                            }
                            if (timedPrices.isNotEmpty()) {
                                com.asc.markets.data.CombinedFallbackDataStore.replaceTimedHistory(pair.symbol, timedPrices.sortedBy { it.timestampMillis })
                            }
                        }
                    }
                    override fun onError(error: String) {
                        android.util.Log.e("PreMoveFred", "FRED fetch error: $error")
                    }
                })
            }
            tiingoIexClient != null && tiingoStockSymbol != null -> {
                fetchTiingoStockHistory(tiingoIexClient, tiingoStockSymbol, historyInterval, pair.symbol)
            }
        }
    }

    DisposableEffect(binanceService, derivService, fredService, tiingoIexClient) {
        onDispose {
            binanceService?.disconnect()
            derivService?.disconnect()
        }
    }
}

@Composable
private fun PreMoveCombinedChart(
    selectedPair: ForexPair,
    priceSeries: PreMovePriceSeries,
    aiValues: List<Float>,
    currentAiValue: Float,
    selectedTimeframe: String,
    hasChartData: Boolean,
    onTimeframeSelected: (String) -> Unit,
    onExpandClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PreMoveChartHeader(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = onTimeframeSelected,
            onExpandClick = onExpandClick
        )
        Spacer(modifier = Modifier.height(8.dp))
        PreMoveCombinedChartBox(
            selectedPair = selectedPair,
            priceSeries = priceSeries,
            aiValues = aiValues,
            currentAiValue = currentAiValue,
            selectedTimeframe = selectedTimeframe,
            hasChartData = hasChartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(283.dp)
        )
    }
}

@Composable
private fun ExpandedPreMoveChartDialog(
    selectedPair: ForexPair,
    priceSeries: PreMovePriceSeries,
    aiValues: List<Float>,
    currentAiValue: Float,
    selectedTimeframe: String,
    hasChartData: Boolean,
    onTimeframeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI PRICE & PRE-MOVE CHART", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(13.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeframeDropdown(selectedTimeframe, onTimeframeSelected)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(PureBlack)
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(9.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            PreMoveCombinedChartBox(
                selectedPair = selectedPair,
                priceSeries = priceSeries,
                aiValues = aiValues,
                currentAiValue = currentAiValue,
                selectedTimeframe = selectedTimeframe,
                hasChartData = hasChartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun PreMoveChartHeader(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI PRICE & PRE-MOVE CHART", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeframeDropdown(selectedTimeframe, onTimeframeSelected)
            Spacer(modifier = Modifier.width(8.dp))
            ExpandChartButton(onExpandClick)
        }
    }
}

@Composable
private fun TimeframeDropdown(selectedTimeframe: String, onTimeframeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(PureBlack)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedTimeframe, color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(5.dp))
                Canvas(modifier = Modifier.width(7.dp).height(5.dp)) {
                    val iconColor = Color.White.copy(alpha = 0.70f)
                    drawLine(iconColor, Offset(0f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(iconColor, Offset(size.width, 0f), Offset(size.width / 2f, size.height), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PureBlack)
        ) {
            preMoveTimeframes().forEach { timeframe ->
                DropdownMenuItem(
                    text = {
                        Text(
                            timeframe.label,
                            color = if (timeframe.label == selectedTimeframe) PreMoveGreen else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (timeframe.label == selectedTimeframe) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    onClick = {
                        expanded = false
                        onTimeframeSelected(timeframe.label)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpandChartButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(15.dp)) {
            val iconColor = Color.White.copy(alpha = 0.86f)
            val stroke = 1.4.dp.toPx()
            drawLine(iconColor, Offset(size.width * 0.58f, size.height * 0.42f), Offset(size.width, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(size.width * 0.72f, 0f), Offset(size.width, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(size.width, 0f), Offset(size.width, size.height * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(size.width * 0.42f, size.height * 0.58f), Offset(0f, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(0f, size.height * 0.72f), Offset(0f, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(0f, size.height), Offset(size.width * 0.28f, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun PreMoveCombinedChartBox(
    selectedPair: ForexPair,
    priceSeries: PreMovePriceSeries,
    aiValues: List<Float>,
    currentAiValue: Float,
    selectedTimeframe: String,
    hasChartData: Boolean,
    modifier: Modifier = Modifier
) {
    val priceMin = priceSeries.values.minOrNull() ?: selectedPair.price
    val priceMax = priceSeries.values.maxOrNull() ?: selectedPair.price
    val priceRange = (priceMax - priceMin).takeIf { it > 0.0 } ?: 1.0
    
    // Update interval based on timeframe (not every second)
    val updateIntervalMillis = when (selectedTimeframe) {
        "5m" -> 5_000L      // Update every 5 seconds for 5m chart
        "15m" -> 15_000L    // Update every 15 seconds for 15m chart
        "30m" -> 30_000L    // Update every 30 seconds for 30m chart
        "1H" -> 60_000L     // Update every 1 minute for 1H chart
        "4H" -> 240_000L    // Update every 4 minutes for 4H chart
        "1D" -> 300_000L    // Update every 5 minutes for 1D chart
        "1W" -> 600_000L    // Update every 10 minutes for 1W chart
        else -> 60_000L
    }
    
    val currentTime by produceState(initialValue = System.currentTimeMillis(), selectedTimeframe) {
        while (true) {
            value = System.currentTimeMillis()
            delay(updateIntervalMillis)
        }
    }
    
    // Use the AI values passed from parent (from backend decision data)
    val aiScoreToDisplay = currentAiValue
    
    // Blinking animation for the tip dot
    val infiniteTransition = rememberInfiniteTransition(label = "chart_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tip_alpha"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PureBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 0.dp, top = 28.dp, end = 28.dp, bottom = 30.dp)) {
            val w = size.width
            val h = size.height
            
            // Draw AI phase reference lines (0-100 scale on left Y-axis)
            val aiPhases = listOf(
                Triple(0f, NoiseRed.copy(alpha = 0.75f), ""),
                Triple(20f, NoiseRed, "NOISE"),
                Triple(40f, StructureOrange, "STRUCTURE"), 
                Triple(60f, CompressionYellow, "COMPRESSION"),
                Triple(80f, PreMoveGreen, "PRE-MOVE"),
                Triple(100f, ExpansionBlue, "EXPANSION")
            )
            
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val textPaint = android.graphics.Paint().apply {
                textSize = 10.sp.toPx()
                isFakeBoldText = true
                isAntiAlias = true
            }
            
            aiPhases.forEach { (level, color, label) ->
                val yPos = h - (level / 100f * h)
                
                // Draw horizontal reference line
                drawLine(
                    color = color.copy(alpha = 0.15f),
                    start = Offset(0f, yPos),
                    end = Offset(w, yPos),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
                
                // Draw level number on left using native canvas
                textPaint.color = color.toArgb()
                val levelText = level.toInt().toString()
                nativeCanvas.drawText(
                    levelText,
                    8.dp.toPx(),
                    yPos + 4.dp.toPx(),
                    textPaint
                )
                
                if (label.isNotBlank()) {
                    textPaint.textSize = 8.sp.toPx()
                    textPaint.color = color.copy(alpha = 0.7f).toArgb()
                    nativeCanvas.drawText(
                        label,
                        8.dp.toPx(),
                        yPos - 8.dp.toPx(),
                        textPaint
                    )
                }
                textPaint.textSize = 10.sp.toPx() // Reset for next iteration
            }
            
            if (priceSeries.values.size >= 2) {
                // Keep right edge fixed at current time, window moves left as time progresses (like market overview chart)
                val windowEnd = currentTime
                val windowStart = currentTime - priceSeries.windowMillis
                val liveEntries = priceSeries.values.zip(priceSeries.timestamps).map { (value, timestamp) ->
                    TimedPrice(timestamp, value)
                }.toMutableList().apply {
                    add(TimedPrice(currentTime, selectedPair.price))
                }.dedupeConsecutiveTimedPrices()
                // Use ALL available data points within the window (no takeLast limit)
                val pricePoints = liveEntries.map { point ->
                    // Map timestamp to X position: right edge = current time, left edge = windowStart
                    val xFraction = if (priceSeries.windowMillis <= 0L) {
                        1f
                    } else {
                        ((point.timestampMillis - windowStart).toDouble() / priceSeries.windowMillis.toDouble())
                            .toFloat()
                            .coerceIn(0f, 1f)
                    }
                    val y = h - (((point.price - priceMin) / priceRange).toFloat().coerceIn(0f, 1f) * h)
                    Offset(xFraction * w, y)
                }
                val linePath = Path().apply {
                    moveTo(pricePoints.first().x, pricePoints.first().y)
                    pricePoints.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                val lineColor = Color(0xFF6FCBC0)
                
                // Draw only the line (no fill)
                drawPath(
                    path = linePath,
                    color = lineColor.copy(alpha = 0.94f),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw blinking dot at the tip (current price)
                if (pricePoints.isNotEmpty()) {
                    val tipPoint = pricePoints.last()
                    // Outer glow ring
                    drawCircle(
                        lineColor.copy(alpha = alpha * 0.5f),
                        radius = 8.dp.toPx(),
                        center = tipPoint
                    )
                    // Middle ring
                    drawCircle(
                        lineColor.copy(alpha = alpha * 0.7f),
                        radius = 5.dp.toPx(),
                        center = tipPoint
                    )
                    // Main dot
                    drawCircle(
                        lineColor.copy(alpha = alpha),
                        radius = 3.dp.toPx(),
                        center = tipPoint
                    )
                    // Inner bright core
                    drawCircle(
                        Color.White.copy(alpha = alpha),
                        radius = 1.5.dp.toPx(),
                        center = tipPoint
                    )
                }
                
                // Draw AI Pre-Move Sparkline (Market Readiness Engine)
                // Use AI values from backend decision data
                if (aiValues.isNotEmpty()) {
                    val aiPoints = liveEntries.map { point ->
                        val xFraction = if (priceSeries.windowMillis <= 0L) {
                            1f
                        } else {
                            ((point.timestampMillis - windowStart).toDouble() / priceSeries.windowMillis.toDouble())
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                        val score = samplePreMoveAiValue(aiValues, xFraction)
                        val yPos = h - (score / 100f * h)
                        Offset(xFraction * w, yPos)
                    }
                    
                    val aiPath = Path().apply {
                        if (aiPoints.isNotEmpty()) {
                            moveTo(aiPoints.first().x, aiPoints.first().y)
                            if (aiPoints.size == 2) {
                                lineTo(aiPoints.last().x, aiPoints.last().y)
                            } else {
                                for (index in 1 until aiPoints.size) {
                                    val previous = aiPoints[index - 1]
                                    val current = aiPoints[index]
                                    val midpoint = Offset(
                                        x = (previous.x + current.x) / 2f,
                                        y = (previous.y + current.y) / 2f
                                    )
                                    quadraticBezierTo(previous.x, previous.y, midpoint.x, midpoint.y)
                                }
                                lineTo(aiPoints.last().x, aiPoints.last().y)
                            }
                        }
                    }
                    
                    // Draw AI sparkline with dashed style and glow
                    val aiColor = Color(0xFFFFFFFF).copy(alpha = 0.6f)
                    drawPath(
                        path = aiPath,
                        color = aiColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )
                    )
                    
                    // Draw current AI score percentage on right side
                    if (aiPoints.isNotEmpty()) {
                        val lastAiPoint = aiPoints.last()
                        val lastScore = aiScoreToDisplay // Use the actual AI score from backend
                        
                        val boxWidth = 52.dp.toPx()
                        val boxHeight = 20.dp.toPx()
                        val boxX = w - boxWidth - 16.dp.toPx()
                        val boxY = (lastAiPoint.y - boxHeight / 2).coerceIn(6.dp.toPx(), h - boxHeight - 6.dp.toPx())
                        
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            topLeft = Offset(boxX, boxY),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        
                        // Draw percentage text
                        textPaint.color = aiColor.toArgb()
                        textPaint.textSize = 11.sp.toPx()
                        textPaint.textAlign = android.graphics.Paint.Align.CENTER
                        nativeCanvas.drawText(
                            "${lastScore.toInt()}%",
                            boxX + boxWidth / 2,
                            boxY + boxHeight / 2 + 4.dp.toPx(),
                            textPaint
                        )
                        textPaint.textAlign = android.graphics.Paint.Align.LEFT // Reset
                    }
                }
            }
        }
        if (!hasChartData) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AiProgressionSection(currentPhase: String, currentValue: Float) {
    val phases = superPhaseSteps()
    val value = currentValue.coerceIn(0f, 100f)
    val activeColor = preMoveColor(value)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("AI PROGRESSION SCALE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(progressionSubtitle(currentPhase), color = TextGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(154.dp)
        ) {
            val sidePadding = 0.dp  // Removed all side padding - full screen width
            val usableWidth = maxWidth - (sidePadding * 2f)
            val barY = 54.dp
            val tickY = 77.dp
            val bubbleWidth = 85.dp
            val currentX = sidePadding + (usableWidth * (value / 100f))
            val bubbleX = when {
                currentX < bubbleWidth / 2f -> 0.dp
                currentX > maxWidth - bubbleWidth / 2f -> maxWidth - bubbleWidth
                else -> currentX - bubbleWidth / 2f
            }
            phases.forEach { phase ->
                val labelWidth = 70.dp  // Reduced from 90.dp to prevent overlap
                val phaseCenter = (phase.start + phase.end) / 200f
                val rawX = sidePadding + (usableWidth * phaseCenter) - (labelWidth / 2f)
                val labelX = when {
                    rawX < 0.dp -> 0.dp
                    rawX > maxWidth - labelWidth -> maxWidth - labelWidth
                    else -> rawX
                }
                Column(
                    modifier = Modifier
                        .width(labelWidth)
                        .offset(x = labelX, y = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(phase.label, color = phase.color, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(phase.caption, color = TextGray, fontSize = 7.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = sidePadding.toPx()
                val endX = size.width - sidePadding.toPx()
                val width = endX - startX
                val centerY = barY.toPx()
                phases.forEach { phase ->
                    val x1 = startX + width * (phase.start / 100f)
                    val x2 = startX + width * (phase.end / 100f)
                    drawLine(phase.color.copy(alpha = 0.24f), Offset(x1, centerY), Offset(x2, centerY), strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(phase.color, Offset(x1, centerY), Offset(x2, centerY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                }
                val dashed = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f)
                val currentPx = startX + width * (value / 100f)
                drawLine(activeColor.copy(alpha = 0.72f), Offset(currentPx, 6.dp.toPx()), Offset(currentPx, 118.dp.toPx()), strokeWidth = 1.2.dp.toPx(), pathEffect = dashed)
                drawCircle(activeColor.copy(alpha = 0.18f), radius = 12.dp.toPx(), center = Offset(currentPx, centerY))
                drawCircle(activeColor, radius = 5.dp.toPx(), center = Offset(currentPx, centerY))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(currentPx, centerY))
            }
            listOf(0, 30, 45, 60, 80, 100).forEach { tick ->
                val tickWidth = 34.dp
                val rawX = sidePadding + (usableWidth * (tick / 100f)) - (tickWidth / 2f)
                val tickX = when {
                    rawX < 0.dp -> 0.dp
                    rawX > maxWidth - tickWidth -> maxWidth - tickWidth
                    else -> rawX
                }
                Text(
                    tick.toString(),
                    color = TextGray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(tickWidth)
                        .offset(x = tickX, y = tickY)
                )
            }
            Box(
                modifier = Modifier
                    .width(bubbleWidth)
                    .height(32.dp)
                    .offset(x = bubbleX, y = 98.dp)
                    .background(PureBlack, RoundedCornerShape(4.dp))
                    .border(1.dp, activeColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${value.toInt()}%  $currentPhase", color = activeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        Text("Macro phase scale: AI moves from no-trade conditions into execution readiness", color = TextGray, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun CurrentProgressDetailsSection(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float) {
    val phaseColor = preMoveColor(aiScore)
    val cards = progressDetailCards(decision, currentPhase, aiScore, phaseColor)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$currentPhase DETAILS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.background(phaseColor.copy(alpha = 0.18f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("ACTIVE", color = phaseColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Swipe horizontally", color = TextGray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cards.forEachIndexed { index, card ->
                ProgressDetailCardView(card)
                if (index < cards.lastIndex) {
                    Box(modifier = Modifier.width(10.dp).height(2.dp).background(phaseColor.copy(alpha = 0.65f), RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
private fun ProgressDetailCardView(card: ProgressDetailCard) {
    Column(
        modifier = Modifier
            .width(146.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(9.dp)
    ) {
        Text(card.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(6.dp))
        Text(card.metric, color = TextGray, fontSize = 8.sp, maxLines = 1)
        Text(card.value, color = card.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (card.progress != null) {
            Spacer(modifier = Modifier.height(5.dp))
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
                Box(modifier = Modifier.fillMaxWidth(card.progress.coerceIn(0f, 1f)).fillMaxHeight().background(card.color, RoundedCornerShape(3.dp)))
            }
        } else {
            Spacer(modifier = Modifier.height(9.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("Status", color = TextGray, fontSize = 8.sp)
        Text(card.status, color = Color.White, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EntryStyleSection(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float) {
    val activeColor = preMoveColor(aiScore)
    val cards = entryStyleCards(decision, currentPhase, aiScore, activeColor)
    val active = cards.firstOrNull { it.active }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ENTRY STYLE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("($currentPhase)", color = activeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.forEach { card ->
                    EntryStyleCardView(card)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Box(
                modifier = Modifier
                    .width(166.dp)
                    .height(118.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureBlack)
                    .border(1.dp, activeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(active?.detail ?: "Waiting for clearer phase confirmation.", color = Color.White, fontSize = 10.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${currentPhase} CONFIDENCE", color = activeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", aiScore)}%", color = activeColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EntryStyleCardView(card: EntryStyleCard) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(card.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.weight(1f))
        Text(if (card.active) "Best For Current" else "Match Quality", color = TextGray, fontSize = 9.sp)
        Text(card.match, color = card.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VolatilityRegimeScaleSection(decision: FinalDecisionItem?) {
    val volScore = ((decision?.feeder_volatility_score ?: 0.0) * 100.0).toFloat().coerceIn(0f, 100f)
    val state = decision?.feeder_volatility_state?.uppercase(Locale.US) ?: "NORMAL"
    val confidence = decision?.feeder_volatility_confidence?.uppercase(Locale.US) ?: "LOW"
    val reason = formatVolatilityReason(decision?.feeder_volatility_reason)
    
    val phases = ascVolatilityPhases()

    val activePhase = phases.firstOrNull { it.label == state }
        ?: phases.find { volScore >= it.start && volScore <= it.end }
        ?: phases.first { it.label == "NORMAL" }
    val activeColor = activePhase.color
    val stateColor = activeColor
    val markerScore = ascVolatilityMarkerScore(activePhase, volScore)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("VOLATILITY REGIME SCALE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF50C878)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val usableWidth = maxWidth
            val barY = 54.dp
            val tickY = 77.dp
            
            // Draw Phase Labels
            phases.forEach { phase ->
                val labelWidth = 64.dp
                val phaseCenter = (phase.start + phase.end) / 200f
                val rawX = (usableWidth * phaseCenter) - (labelWidth / 2f)
                val labelX = rawX.coerceIn(0.dp, maxWidth - labelWidth)
                
                Column(
                    modifier = Modifier
                        .width(labelWidth)
                        .offset(x = labelX, y = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(phase.label, color = phase.color, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Visible)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(phase.caption, color = phase.color, fontSize = 14.sp)
                }
            }
            
            // Draw Bar
            Canvas(modifier = Modifier.fillMaxSize()) {
                val startX = 0f
                val endX = size.width
                val width = endX - startX
                val centerY = barY.toPx()
                
                // Draw track
                phases.forEach { phase ->
                    val x1 = startX + width * (phase.start / 100f)
                    val x2 = startX + width * (phase.end / 100f)
                    drawLine(phase.color.copy(alpha = 0.3f), Offset(x1, centerY), Offset(x2, centerY), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(phase.color, Offset(x1, centerY), Offset(x2, centerY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                }
                
                // Draw current marker
                val currentPx = startX + width * (markerScore / 100f)
                val dashed = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(currentPx, centerY), Offset(currentPx, 100.dp.toPx()), strokeWidth = 1.dp.toPx(), pathEffect = dashed)
                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(currentPx, centerY))
            }
            
            // Draw Ticks
            (phases.map { it.start.toInt() } + 100).distinct().forEach { tick ->
                val tickWidth = 24.dp
                val rawX = (usableWidth * (tick / 100f)) - (tickWidth / 2f)
                val tickX = rawX.coerceIn(0.dp, maxWidth - tickWidth)
                Text(
                    tick.toString(),
                    color = TextGray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(tickWidth)
                        .offset(x = tickX, y = tickY)
                )
            }
            
            // Current Value Text
            val valWidth = 60.dp
            val valRawX = (usableWidth * (markerScore / 100f)) - (valWidth / 2f)
            val valX = valRawX.coerceIn(0.dp, maxWidth - valWidth)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(valWidth).offset(x = valX, y = 92.dp)
            ) {
                Text(String.format(Locale.US, "%.1f", volScore), color = activeColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.background(activeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(ascVolatilityDisplayLabel(confidence), color = activeColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp).border(1.dp, stateColor, CircleShape), contentAlignment = Alignment.Center) {
                Text(activePhase.caption, color = stateColor, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Feeder volatility is ${ascVolatilityDisplayLabel(state)}.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Confidence: ${ascVolatilityDisplayLabel(confidence)} • $reason", color = TextGray, fontSize = 10.sp)
            }
            val nextPhase = phases.getOrNull(phases.indexOf(activePhase) + 1) ?: phases.last()
            Text("Next State: ", color = TextGray, fontSize = 10.sp)
            Text(ascVolatilityDisplayLabel(nextPhase.label), color = nextPhase.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun ascVolatilityPhases(): List<PhaseStep> = listOf(
    PhaseStep("DEAD", "·", 0f, 12f, Color(0xFF64748B)),
    PhaseStep("COMPRESSED", "⇥", 12f, 35f, CompressionYellow),
    PhaseStep("NORMAL", "∿", 35f, 55f, PreMoveGreen),
    PhaseStep("EXPANDING", "↗", 55f, 75f, StructureOrange),
    PhaseStep("BURST", "⚡", 75f, 88f, NoiseRed),
    PhaseStep("EXPLOSIVE", "✦", 88f, 100f, Color(0xFFE91E63))
)

private fun ascVolatilityPhaseForScore(score: Float): PhaseStep {
    val clampedScore = score.coerceIn(0f, 100f)
    return ascVolatilityPhases().firstOrNull { phase ->
        clampedScore >= phase.start && (clampedScore < phase.end || phase.end >= 100f)
    } ?: ascVolatilityPhases().last()
}

private fun ascVolatilityMarkerScore(activePhase: PhaseStep, score: Float): Float {
    val clampedScore = score.coerceIn(0f, 100f)
    val lowerBound = activePhase.start
    val upperBound = if (activePhase.end >= 100f) 100f else activePhase.end

    if (clampedScore <= 0f) {
        return (lowerBound + upperBound) / 2f
    }

    return clampedScore.coerceIn(lowerBound, upperBound)
}

private fun ascVolatilityStateColor(state: String): Color {
    return ascVolatilityPhases().firstOrNull { it.label == state.uppercase(Locale.US) }?.color ?: PreMoveGreen
}

private fun ascVolatilityDisplayLabel(value: String): String {
    return value.uppercase(Locale.US).replace("_", " ")
}

private fun formatVolatilityReason(reason: String?): String {
    return reason
        ?.replace("_", " ")
        ?.replace("|", "•")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "NO VOLATILITY REASON"
}

@Composable
private fun KeyVolatilityStatsSection(decision: FinalDecisionItem?) {
    val volScore = ((decision?.feeder_volatility_score ?: 0.0) * 100.0).toFloat()
    val volRatio = decision?.vol_ratio ?: 0.0
    val atrRatio = decision?.atr_ratio ?: 0.0
    val burstRatio = decision?.burst_ratio ?: 0.0
    val confidence = ascVolatilityDisplayLabel(decision?.feeder_volatility_confidence ?: "LOW")
    val state = ascVolatilityDisplayLabel(decision?.feeder_volatility_state ?: "NORMAL")
    val scoreColor = ascVolatilityPhaseForScore(volScore).color
    val stateColor = scoreColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("KEY VOLATILITY STATS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VolStatItem("VOL SCORE", String.format(Locale.US, "%.1f", volScore), confidence, scoreColor)
            VolStatDivider()
            VolStatItem("VOL RATIO", String.format(Locale.US, "%.2fx", volRatio), "Relative Vol", Color(0xFFDCEB3A))
            VolStatDivider()
            VolStatItem("ATR RATIO", String.format(Locale.US, "%.2fx", atrRatio), "True Range", Color(0xFFFF6B6B))
            VolStatDivider()
            VolStatItem("BURST RATIO", String.format(Locale.US, "%.2fx", burstRatio), "Max Move", Color(0xFFFFA726))
            VolStatDivider()
            VolStatItem("STATE", state, "Regime", stateColor)
        }
    }
}

@Composable
private fun VolStatItem(title: String, value: String, subtitle: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, color = TextGray, fontSize = 8.sp)
    }
}

@Composable
private fun VolStatDivider() {
    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.1f)))
}

@Composable
private fun FinalTradingDecisionSection(decision: FinalDecisionItem?, aiScore: Float) {
    val resolvedDecision = resolveFinalTradingDecision(decision, aiScore)
    val finalState = resolvedDecision.state
    val finalDirection = resolvedDecision.direction
    val finalConfidence = resolvedDecision.confidence
    val finalScore = resolvedDecision.score
    val finalPriority = resolvedDecision.priority
    val finalReason = resolvedDecision.reason
    
    val stateColor = when (finalState) {
        "TRADE_CANDIDATE" -> when (finalDirection) {
            "LONG" -> PreMoveGreen
            "SHORT" -> NoiseRed
            else -> TextGray
        }
        "MANUAL_REVIEW" -> Color(0xFFFFAA00)
        else -> TextGray
    }
    
    val stateLabel = when (finalState) {
        "TRADE_CANDIDATE" -> "✓ TRADE CANDIDATE"
        "MANUAL_REVIEW" -> "⚠ MANUAL REVIEW"
        "REJECTED" -> "✗ NO TRADE"
        else -> finalState
    }
    
    val priorityLabel = finalDecisionPriorityLabel(finalPriority)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack, RoundedCornerShape(8.dp))
            .border(1.dp, stateColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "FINAL TRADING DECISION",
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                stateLabel,
                color = stateColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DIRECTION", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (finalDirection == "NONE") "—" else finalDirection,
                    color = if (finalDirection == "NONE") TextGray else stateColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SCORE", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${finalScore.toInt()}%",
                    color = stateColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("PRIORITY", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    priorityLabel,
                    color = stateColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
        }
        
        if (finalState == "TRADE_CANDIDATE") {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(stateColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    finalReason,
                    color = stateColor,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        } else if (finalState == "MANUAL_REVIEW") {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(stateColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    finalReason,
                    color = stateColor,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TextGray.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    finalReason,
                    color = TextGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private data class ResolvedFinalTradingDecision(
    val state: String,
    val direction: String,
    val score: Float,
    val priority: String,
    val confidence: String,
    val reason: String
)

private fun resolveFinalTradingDecision(decision: FinalDecisionItem?, aiScore: Float): ResolvedFinalTradingDecision {
    if (decision == null) {
        return ResolvedFinalTradingDecision(
            state = "REJECTED",
            direction = "NONE",
            score = aiScore.coerceIn(0f, 100f),
            priority = "NO_TRADE",
            confidence = confidenceFromDecisionScore(aiScore),
            reason = "No decision data available yet for the selected asset."
        )
    }

    val resolvedScore = decision.final_trade_score
        ?.takeIf { it.isFinite() }
        ?.let { (it * 100f).toFloat() }
        ?.coerceIn(0f, 100f)
        ?: decision.pre_move_ai_score
            ?.takeIf { it.isFinite() }
            ?.let { (it * 100f).toFloat() }
            ?.coerceIn(0f, 100f)
        ?: aiScore.coerceIn(0f, 100f)

    val explicitState = decision.final_trade_state
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.US)
    val portfolioLabel = decision.portfolio_decision_label
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.US)
    val explicitPriority = decision.final_trade_priority
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.US)
    val labelPriority = decision.final_trade_label
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.US)
    val journalPriority = decision.journal_priority
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.US)

    val resolvedState = explicitState ?: when {
        explicitPriority == "REVIEW" || labelPriority == "REVIEW" -> "MANUAL_REVIEW"
        portfolioLabel == "PRIMARY_DEPLOYMENT" -> "TRADE_CANDIDATE"
        resolvedScore >= 60f && (
            decision.confluence_state.equals("TRADEABLE_SETUP", ignoreCase = true) ||
                decision.entry_state.equals("READY", ignoreCase = true) ||
                decision.plan_state.equals("PLAN_READY", ignoreCase = true)
            ) -> "MANUAL_REVIEW"
        else -> "REJECTED"
    }

    val resolvedDirection = decision.final_trade_direction
        ?.takeIf { it.isNotBlank() && !it.equals("NONE", ignoreCase = true) }
        ?.uppercase(Locale.US)
        ?: decision.journal_direction
            ?.takeIf { it.isNotBlank() && !it.equals("NONE", ignoreCase = true) }
            ?.uppercase(Locale.US)
        ?: "NONE"

    val resolvedPriority = explicitPriority
        ?: labelPriority
        ?: journalPriority
        ?: when {
            resolvedState == "TRADE_CANDIDATE" && resolvedScore >= 80f -> "PRIORITY_A"
            resolvedState == "TRADE_CANDIDATE" -> "PRIORITY_B"
            resolvedState == "MANUAL_REVIEW" -> "REVIEW"
            else -> "NO_TRADE"
        }

    val resolvedConfidence = decision.final_trade_confidence
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.US)
        ?: decision.confluence_confidence
            ?.takeIf { it.isNotBlank() }
            ?.uppercase(Locale.US)
        ?: confidenceFromDecisionScore(resolvedScore)

    val resolvedReason = formatFinalDecisionReason(
        decision.final_trade_reason,
        decision.portfolio_decision_reason,
        resolvedState,
        resolvedConfidence,
        decision.confluence_state,
        decision.entry_state
    )

    return ResolvedFinalTradingDecision(
        state = resolvedState,
        direction = resolvedDirection,
        score = resolvedScore,
        priority = resolvedPriority,
        confidence = resolvedConfidence,
        reason = resolvedReason
    )
}

private fun finalDecisionPriorityLabel(priority: String): String {
    return when (priority.uppercase(Locale.US)) {
        "PRIORITY_A", "HIGH", "HIGH_PRIORITY", "PRIMARY_DEPLOYMENT" -> "HIGH PRIORITY"
        "PRIORITY_B", "NORMAL", "NORMAL_PRIORITY" -> "NORMAL PRIORITY"
        "REVIEW", "MANUAL_REVIEW" -> "REVIEW REQUIRED"
        else -> "NO TRADE"
    }
}

private fun confidenceFromDecisionScore(score: Float): String {
    return when {
        score >= 78f -> "VERY_HIGH"
        score >= 58f -> "HIGH"
        score >= 35f -> "MEDIUM"
        else -> "LOW"
    }
}

private fun formatFinalDecisionReason(
    finalReason: String?,
    portfolioReason: String?,
    resolvedState: String,
    resolvedConfidence: String,
    confluenceState: String?,
    entryState: String?
): String {
    val rawReason = finalReason
        ?.takeIf { it.isNotBlank() }
        ?: portfolioReason?.takeIf { it.isNotBlank() }

    if (rawReason != null) {
        return rawReason
            .replace("_", " ")
            .replace("|", "•")
            .trim()
    }

    return when (resolvedState) {
        "TRADE_CANDIDATE" -> "Selected asset is aligned for deployment with ${resolvedConfidence.replace("_", " ")} confidence."
        "MANUAL_REVIEW" -> "Selected asset has a developing setup but still needs manual confirmation before execution."
        else -> when {
            confluenceState.equals("TRADEABLE_SETUP", ignoreCase = true) && !entryState.equals("READY", ignoreCase = true) ->
                "Confluence is forming, but the entry gate is not ready yet for the selected asset."
            else -> "No pre-move setup detected for the selected asset. Waiting for confluence and entry conditions."
        }
    }
}

@Composable
private fun AiReasoningCollapsibleSection(decision: FinalDecisionItem?) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val finalState = decision?.final_trade_state?.uppercase(Locale.US) ?: "REJECTED"
    val reason = decision?.final_trade_reason ?: "NO_DECISION_DATA"
    val reasons = reason.split(" | ").filter { it.isNotBlank() }
    
    val headerColor = when (finalState) {
        "TRADE_CANDIDATE" -> PreMoveGreen
        "MANUAL_REVIEW" -> Color(0xFFFFAA00)
        else -> TextGray
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack, RoundedCornerShape(8.dp))
            .border(1.dp, headerColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        // Header - Always visible, clickable to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🧠",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "AI REASONING",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "40 Feeder Pipeline Analysis",
                        color = headerColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isExpanded) "COLLAPSE" else "EXPAND",
                    color = headerColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isExpanded) "▲" else "▼",
                    color = headerColor,
                    fontSize = 10.sp
                )
            }
        }
        
        // Expandable content
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = headerColor.copy(alpha = 0.2f), thickness = 1.dp)
                
                // Feeder Gates Status
                FeederGatesSection(decision)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Rejection/Approval Reasons
                RejectionReasonsSection(reasons, finalState, headerColor)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Key Feeder States
                KeyFeederStatesSection(decision)
            }
        }
    }
}

@Composable
private fun FeederGatesSection(decision: FinalDecisionItem?) {
    val gates = listOf(
        Triple("ENTRY", decision?.entry_state ?: "NO_ENTRY", decision?.entry_state == "READY"),
        Triple("CONFLUENCE", decision?.confluence_state ?: "NO_CONFLUENCE", decision?.confluence_state == "TRADEABLE_SETUP"),
        Triple("PLAN", decision?.plan_state ?: "NO_PLAN", decision?.plan_state == "PLAN_READY"),
        Triple("EXECUTION", decision?.execution_status ?: "BLOCKED", decision?.execution_status == "READY"),
        Triple("SIGNAL QUALITY", decision?.signal_quality_state ?: "NO_SIGNAL_QUALITY", decision?.signal_quality_state in listOf("STRONG_SIGNAL", "ELITE_SIGNAL")),
        Triple("RISK", decision?.feeder_risk_state ?: "RISK_OFF", decision?.feeder_risk_state != "RISK_OFF")
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "CRITICAL GATES",
            color = TextGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        gates.forEach { (name, state, passed) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (passed) PreMoveGreen.copy(alpha = 0.08f) else NoiseRed.copy(alpha = 0.08f),
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (passed) PreMoveGreen.copy(alpha = 0.2f) else NoiseRed.copy(alpha = 0.2f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (passed) "✓" else "✗",
                        color = if (passed) PreMoveGreen else NoiseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        name,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    state,
                    color = if (passed) PreMoveGreen else NoiseRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RejectionReasonsSection(reasons: List<String>, finalState: String, headerColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (finalState == "TRADE_CANDIDATE") "APPROVAL FACTORS" else "BLOCKING FACTORS",
            color = TextGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        if (reasons.isEmpty()) {
            Text(
                "No reasoning data available",
                color = TextGray.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        } else {
            reasons.take(8).forEach { reasonItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .border(1.dp, headerColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▸", color = headerColor, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        reasonItem,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
            
            if (reasons.size > 8) {
                Text(
                    "+ ${reasons.size - 8} more factors",
                    color = TextGray.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun KeyFeederStatesSection(decision: FinalDecisionItem?) {
    val feederStates = listOf(
        "REGIME" to (decision?.regime_state ?: "UNKNOWN"),
        "VOLATILITY" to (decision?.feeder_volatility_state ?: "UNKNOWN"),
        "STRUCTURE" to (decision?.structure_state ?: "UNKNOWN"),
        "TREND" to (decision?.trend_state ?: "UNKNOWN"),
        "LIQUIDITY" to (decision?.feeder_liquidity_state ?: "UNKNOWN"),
        "INDICATOR" to (decision?.feeder_indicator_state ?: "UNKNOWN")
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "KEY FEEDER STATES",
            color = TextGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                .border(1.dp, TextGray.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            feederStates.forEach { (name, state) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        name,
                        color = TextGray.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AiInterpretationSection(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float) {
    val state = decision?.feeder_volatility_state?.uppercase(Locale.US) ?: "NORMAL"
    val stateLabel = ascVolatilityDisplayLabel(state)
    val stateColor = ascVolatilityStateColor(state)
    val confidence = ascVolatilityDisplayLabel(decision?.feeder_volatility_confidence ?: "LOW")
    val reason = formatVolatilityReason(decision?.feeder_volatility_reason)
    val atrRatio = decision?.atr_ratio ?: 0.0
    val volRatio = decision?.vol_ratio ?: 0.0
    val burstRatio = decision?.burst_ratio ?: 0.0
    val ignitionPercent = normalizeAiPercentValue(decision?.ignition_probability) ?: aiScore.coerceIn(0f, 100f)
    val primaryMessage = when (state) {
        "DEAD" -> "Volatility is in DEAD state with suppressed range and body activity."
        "COMPRESSED" -> "Volatility is COMPRESSED and the market is coiling before expansion."
        "NORMAL" -> "Volatility is NORMAL and the market is balanced, not yet displaced."
        "EXPANDING" -> "Volatility is EXPANDING as range and dispersion start lifting."
        "BURST" -> "Volatility is in BURST mode with elevated short-window move pressure."
        "EXPLOSIVE" -> "Volatility is EXPLOSIVE with strong expansion and body strength alignment."
        else -> "Volatility is in $stateLabel state."
    }
    val secondaryMessage = when (state) {
        "DEAD" -> "Wait for real expansion before anticipating ignition."
        "COMPRESSED" -> "Watch for ignition only after expansion confirmation appears."
        "NORMAL" -> "Let structure, context, and ignition probability improve before commitment."
        "EXPANDING" -> "Monitor for displacement continuation or pullback execution."
        "BURST" -> "Expect faster moves and tighter execution timing if signals align."
        "EXPLOSIVE" -> "Momentum is already active, so risk needs to stay controlled."
        else -> "Follow the feeder volatility and ignition signals for confirmation."
    }
    val detailMessage = "Confidence $confidence • ATR ${String.format(Locale.US, "%.2fx", atrRatio)} • Vol ${String.format(Locale.US, "%.2fx", volRatio)} • Burst ${String.format(Locale.US, "%.2fx", burstRatio)} • $reason"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureBlack)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("AI INTERPRETATION", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6D28D9).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(primaryMessage, color = Color.White, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(secondaryMessage, color = Color.White, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(detailMessage, color = TextGray, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = stateColor,
                        startAngle = 135f,
                        sweepAngle = 270f * (ignitionPercent / 100f),
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Draw dot at end of arc
                    val angleInDegrees = 135f + (270f * (ignitionPercent / 100f))
                    val angleInRadians = Math.toRadians(angleInDegrees.toDouble())
                    val radius = size.minDimension / 2
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val dotX = (centerX + radius * Math.cos(angleInRadians)).toFloat()
                    val dotY = (centerY + radius * Math.sin(angleInRadians)).toFloat()
                    
                    drawCircle(stateColor.copy(alpha = 0.75f), radius = 3.dp.toPx(), center = Offset(dotX, dotY))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${ignitionPercent.toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
             Text("Ignition Probability", color = TextGray, fontSize = 8.sp, modifier = Modifier.padding(end = 6.dp))
        }
    }
}

@Composable
private fun PhaseBandLabel(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StockListItem(
    name: String,
    subtitle: String,
    score: String,
    price: String,
    priceChange: String,
    state: String,
    color: Color,
    symbol: String,
    values: List<Float>,
    isHighlighted: Boolean,
    metricsScrollState: ScrollState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(if (isHighlighted) Color.White.copy(alpha = 0.035f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(144.dp)
        ) {
            PairFlags(symbol = symbol, size = 36)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(metricsScrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.widthIn(min = 72.dp)
                ) {
                    Text(score, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 148.dp)
                ) {
                    Text(
                        text = price,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = priceChange,
                        color = if (priceChange.startsWith("-")) NoiseRed else PreMoveGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                MiniSparkline(
                    values = values,
                    color = color,
                    modifier = Modifier
                        .width(112.dp)
                        .height(34.dp)
                )

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.widthIn(min = 152.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            rowPreparationLabel(state),
                            color = color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        rowIgnitionLabel(state),
                        color = TextGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun MiniSparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    // Blinking animation for the tip dot
    val infiniteTransition = rememberInfiniteTransition(label = "sparkline_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tip_alpha"
    )
    
    Canvas(modifier = modifier) {
        val points = preMoveSparkPoints(values)
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val step = if (points.size <= 1) 0f else w / (points.size - 1)
        // Use the same adaptive window as preMoveSparkPoints
        val windowSize = minOf(40, values.size)
        val window = values.takeLast(windowSize).map { it.coerceIn(0f, 100f) }
        val offsets = points.mapIndexed { index, point ->
            Offset(index * step, point * h)
        }
        offsets.zipWithNext().forEachIndexed { index, pair ->
            val segmentColor = preMoveColor((window[index] + window[index + 1]) / 2f)
            val area = Path().apply {
                moveTo(pair.first.x, h)
                lineTo(pair.first.x, pair.first.y)
                lineTo(pair.second.x, pair.second.y)
                lineTo(pair.second.x, h)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(
                    colors = listOf(segmentColor.copy(alpha = 0.32f), segmentColor.copy(alpha = 0.03f)),
                    startY = minOf(pair.first.y, pair.second.y),
                    endY = h
                )
            )
        }
        offsets.zipWithNext().forEachIndexed { index, pair ->
            val segmentColor = preMoveColor((window[index] + window[index + 1]) / 2f)
            drawLine(segmentColor, pair.first, pair.second, strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        }
        
        // Draw blinking dot at the tip
        val tipColor = preMoveColor(window.lastOrNull() ?: 0f)
        // Outer glow ring
        drawCircle(
            tipColor.copy(alpha = alpha * 0.4f), 
            radius = 4.dp.toPx(), 
            center = offsets.last()
        )
        // Main dot
        drawCircle(
            tipColor.copy(alpha = alpha), 
            radius = 2.2.dp.toPx(), 
            center = offsets.last()
        )
        // Inner bright core
        drawCircle(
            Color.White.copy(alpha = alpha), 
            radius = 1.dp.toPx(), 
            center = offsets.last()
        )
    }
}

private fun findAiDecision(pair: ForexPair, aiDecisions: List<FinalDecisionItem>): FinalDecisionItem? {
    val key = normalizeAssetKey(pair.symbol)
    return aiDecisions
        .asSequence()
        .filter { decision -> normalizeAssetKey(decision.asset_1.orEmpty()) == key }
        .maxByOrNull { decision -> preMoveDecisionTimestampMillis(decision) }
}

private fun normalizeAssetKey(value: String): String {
    return value.uppercase(Locale.US)
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
}

private fun preMoveDecisionTimestampMillis(decision: FinalDecisionItem?): Long {
    val rawTimestamp = decision?.journal_timestamp?.takeIf { it.isNotBlank() } ?: return 0L
    return try {
        OffsetDateTime.parse(rawTimestamp).toInstant().toEpochMilli()
    } catch (_: Exception) {
        0L
    }
}

private fun preMovePriceSeries(
    pair: ForexPair,
    priceHistory: Map<String, List<Double>>,
    timedPriceHistory: Map<String, List<TimedPrice>>,
    timeframeLabel: String
): PreMovePriceSeries {
    val key = normalizeAssetKey(pair.symbol)
    val timeframe = preMoveTimeframes().firstOrNull { it.label == timeframeLabel } ?: preMoveTimeframes().first { it.label == "1D" }
    val now = System.currentTimeMillis()
    val timedHistory = timedPriceHistory.entries
        .firstOrNull { normalizeAssetKey(it.key) == key }
        ?.value
        .orEmpty()
        .filter { it.price.isFinite() && it.price > 0.0 }
    val bucketedPoints = timedHistory
        .filter { it.timestampMillis >= now - timeframe.windowMillis }
        .plus(TimedPrice(now, pair.price))
        .groupBy { it.timestampMillis / timeframe.intervalMillis }
        .toSortedMap()
        .values
        .mapNotNull { bucket -> bucket.maxByOrNull { it.timestampMillis } }

    val sourcePoints = bucketedPoints
    val points = densifyTimedPrices(
        points = sourcePoints,
        timeframe = timeframe,
        now = now,
        currentPrice = pair.price
    )
        .filter { it.price.isFinite() && it.price > 0.0 }
        .dedupeConsecutiveTimedPrices()
        .takeLast(200)  // Increased from 60 to 200 for better coverage across all timeframes
    val safePoints = points.takeIf { it.size >= 2 }.orEmpty()
    val safeValues = safePoints.map { it.price }
    val safeTimestamps = safePoints.map { it.timestampMillis }
    return PreMovePriceSeries(
        values = safeValues,
        timestamps = safeTimestamps,
        axisLabels = priceAxisLabels(pair, safeValues),
        windowMillis = timeframe.windowMillis
    )
}

private fun binanceHistorySymbol(symbol: String): String? {
    val normalized = normalizeAssetKey(symbol)
    return when {
        normalized.endsWith("USDT") -> normalized
        else -> null
    }
}

private fun binanceIntervalForPreMove(timeframeLabel: String): String {
    return when (timeframeLabel) {
        "5m" -> "5m"
        "15m" -> "15m"
        "30m" -> "30m"
        "1H" -> "1h"
        "4H" -> "4h"
        "1D" -> "1d"
        "1W" -> "1w"
        else -> "1h"
    }
}

private fun preMoveHistoryInterval(timeframeLabel: String): String {
    return when (timeframeLabel) {
        "5m" -> "5m"
        "15m" -> "15m"
        "30m" -> "30m"
        "1H" -> "1h"
        "4H" -> "4h"
        "1D" -> "1d"
        "1W" -> "1d"
        else -> "1h"
    }
}

private fun shouldUseDerivPreMoveHistory(pair: ForexPair): Boolean {
    return when (pair.category) {
        MarketCategory.FOREX,
        MarketCategory.COMMODITIES,
        MarketCategory.INDICES -> true
        else -> false
    }
}

private fun fredSeriesIdForPair(symbol: String): String? {
    val key = normalizeAssetKey(symbol)
    return when (key) {
        "US10Y" -> "DGS10"
        "US02Y" -> "DGS2"
        "US30Y" -> "DGS30"
        "US05Y" -> "DGS5"
        else -> null
    }
}

private fun parseFredDate(dateStr: String): Long {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val date = format.parse(dateStr)
        date?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

private fun tiingoStockSymbolForPair(symbol: String): String? {
    val key = normalizeAssetKey(symbol)
    return when (key) {
        "AAPL" -> "aapl"
        "INTC" -> "intc"
        "NVDA" -> "nvda"
        "MSFT" -> "msft"
        "GOOGL" -> "googl"
        "AMZN" -> "amzn"
        "META" -> "meta"
        "TSLA" -> "tsla"
        else -> key.lowercase(Locale.US).takeIf { it.isNotBlank() }
    }
}

private suspend fun fetchTiingoStockHistory(
    client: TiingoIexRestClient,
    symbol: String,
    interval: String,
    appSymbol: String
) {
    try {
        val days = when (interval) {
            "5m", "15m", "30m", "1H" -> 5
            "4H" -> 10
            "1D" -> 30
            "1W" -> 90
            else -> 30
        }
        
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("api.tiingo.com")
            .addPathSegments("tiingo/daily/$symbol/prices")
            .addQueryParameter("token", com.asc.markets.BuildConfig.TIINGO_API_KEY)
            .addQueryParameter("startDate", java.time.LocalDate.now().minusDays(days.toLong()).toString())
            .addQueryParameter("endDate", java.time.LocalDate.now().toString())
            .addQueryParameter("resampleFreq", "daily")
            .build()
        
        val request = okhttp3.Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = okhttp3.OkHttpClient().newCall(request).execute()
        val body = response.body?.string().orEmpty()
        
        if (response.isSuccessful && body.isNotBlank()) {
            val json = org.json.JSONArray(body)
            val timedPrices = mutableListOf<TimedPrice>()
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                val date = item.optString("date")
                val close = item.optDouble("close", Double.NaN)
                if (date.isNotBlank() && close.isFinite()) {
                    val timestamp = parseFredDate(date)
                    if (timestamp > 0) {
                        timedPrices.add(TimedPrice(timestamp, close))
                    }
                }
            }
            if (timedPrices.isNotEmpty()) {
                com.asc.markets.data.CombinedFallbackDataStore.replaceTimedHistory(appSymbol, timedPrices.sortedBy { it.timestampMillis })
            }
        } else {
            android.util.Log.e("PreMoveTiingo", "Tiingo EOD fetch failed: ${response.code} ${body.take(240)}")
        }
    } catch (e: Exception) {
        android.util.Log.e("PreMoveTiingo", "Tiingo EOD fetch error: ${e.message}")
    }
}

private fun preMoveTimeframes(): List<PreMoveTimeframe> = listOf(
    PreMoveTimeframe("5m", 5L * 60_000L, 5L * 60_000L * 60L),
    PreMoveTimeframe("15m", 15L * 60_000L, 15L * 60_000L * 60L),
    PreMoveTimeframe("30m", 30L * 60_000L, 30L * 60_000L * 60L),
    PreMoveTimeframe("1H", 60L * 60_000L, 60L * 60_000L * 72L),
    PreMoveTimeframe("4H", 4L * 60L * 60_000L, 4L * 60L * 60_000L * 72L),
    PreMoveTimeframe("1D", 24L * 60L * 60_000L, 24L * 60L * 60_000L * 120L),
    PreMoveTimeframe("1W", 7L * 24L * 60L * 60_000L, 7L * 24L * 60L * 60_000L * 80L)
)

private fun sampleFallbackTimedPrices(
    values: List<Double>,
    timeframe: PreMoveTimeframe,
    now: Long
): List<TimedPrice> {
    val filtered = values.filter { it.isFinite() && it > 0.0 }
    if (filtered.isEmpty()) return emptyList()
    val stride = when (timeframe.label) {
        "5m" -> 1
        "15m" -> 2
        "30m" -> 3
        "1H" -> 5
        "4H" -> 8
        "1D" -> 12
        "1W" -> 20
        else -> 1
    }
    val sampled = filtered.filterIndexed { index, _ -> index == filtered.lastIndex || index % stride == 0 }
    if (sampled.isEmpty()) return emptyList()
    if (sampled.size == 1) return listOf(TimedPrice(now, sampled.first()))
    val start = now - timeframe.windowMillis
    val stepMillis = timeframe.windowMillis / (sampled.lastIndex.toLong().coerceAtLeast(1L))
    return sampled.mapIndexed { index, price ->
        TimedPrice(start + (stepMillis * index), price)
    }
}

private fun densifyTimedPrices(
    points: List<TimedPrice>,
    timeframe: PreMoveTimeframe,
    now: Long,
    currentPrice: Double
): List<TimedPrice> {
    val sanitized = (points + TimedPrice(now, currentPrice))
        .filter { it.timestampMillis > 0L && it.price.isFinite() && it.price > 0.0 }
        .sortedBy { it.timestampMillis }
        .dedupeConsecutiveTimedPrices()
    if (sanitized.size < 2) return sanitized

    val targetSize = when (timeframe.label) {
        "5m" -> 60
        "15m" -> 72
        "30m" -> 84
        "1H" -> 96
        "4H" -> 108
        "1D" -> 120
        "1W" -> 140
        else -> 96
    }
    if (sanitized.size >= targetSize / 2) {
        return sanitized.takeLast(targetSize)
    }

    val start = (now - timeframe.windowMillis).coerceAtLeast(sanitized.first().timestampMillis)
    val end = now.coerceAtLeast(sanitized.last().timestampMillis)
    val span = (end - start).coerceAtLeast(1L)

    return List(targetSize) { index ->
        val fraction = index.toDouble() / (targetSize - 1).coerceAtLeast(1)
        val timestamp = start + (span * fraction).toLong()
        TimedPrice(timestamp, interpolateTimedPrice(sanitized, timestamp))
    }
}

private fun interpolateTimedPrice(points: List<TimedPrice>, timestamp: Long): Double {
    if (points.isEmpty()) return 0.0
    val first = points.first()
    val last = points.last()
    if (timestamp <= first.timestampMillis) return first.price
    if (timestamp >= last.timestampMillis) return last.price

    val upperIndex = points.indexOfFirst { it.timestampMillis >= timestamp }
    if (upperIndex <= 0) return points.first().price

    val lower = points[upperIndex - 1]
    val upper = points[upperIndex]
    val window = (upper.timestampMillis - lower.timestampMillis).coerceAtLeast(1L).toDouble()
    val progress = ((timestamp - lower.timestampMillis).toDouble() / window).coerceIn(0.0, 1.0)
    return lower.price + ((upper.price - lower.price) * progress)
}

private fun List<TimedPrice>.dedupeConsecutiveTimedPrices(): List<TimedPrice> {
    return fold(emptyList()) { acc, value ->
        if (acc.lastOrNull()?.price == value.price) acc else acc + value
    }
}

private fun priceAxisLabels(pair: ForexPair, values: List<Double>): List<Double> {
    val safeValues = (values + pair.price).filter { it.isFinite() && it > 0.0 }
    if (safeValues.isEmpty()) return emptyList()
    val min = safeValues.minOrNull() ?: pair.price
    val max = safeValues.maxOrNull() ?: pair.price
    val baseRange = (max - min).takeIf { it > 0.0 } ?: (pair.price * 0.002).coerceAtLeast(0.0001)
    val paddedMin = min - (baseRange * 0.15)
    val paddedMax = max + (baseRange * 0.15)
    val step = (paddedMax - paddedMin) / 4.0
    return List(5) { index -> paddedMax - (step * index) }
}

private fun timeAxisLabels(timeframeLabel: String): List<String> {
    return when (timeframeLabel) {
        "5m" -> listOf("-25m", "-20m", "-15m", "-10m", "-5m", "NOW")
        "15m" -> listOf("-75m", "-60m", "-45m", "-30m", "-15m", "NOW")
        "30m" -> listOf("-150m", "-120m", "-90m", "-60m", "-30m", "NOW")
        "1H" -> listOf("-5h", "-4h", "-3h", "-2h", "-1h", "NOW")
        "4H" -> listOf("-20h", "-16h", "-12h", "-8h", "-4h", "NOW")
        "1D" -> listOf("-5D", "-4D", "-3D", "-2D", "-1D", "NOW")
        "1W" -> listOf("-5w", "-4w", "-3w", "-2w", "-1w", "NOW")
        else -> listOf("-5D", "-4D", "-3D", "-2D", "-1D", "NOW")
    }
}

private fun formatPriceAxisLabel(pair: ForexPair, value: Double): String {
    return when (pair.category) {
        MarketCategory.FOREX -> if (value >= 100.0) String.format(Locale.US, "%.2f", value) else String.format(Locale.US, "%.4f", value)
        MarketCategory.BONDS -> String.format(Locale.US, "%.3f", value)
        else -> if (value >= 1000.0) String.format(Locale.US, "%,.0f", value) else String.format(Locale.US, "%.2f", value)
    }
}

private fun preMoveAiCurve(pair: ForexPair, decision: FinalDecisionItem?): List<Float> {
    if (decision == null) return emptyList()
    val directScore = normalizeAiPercentValue(decision.pre_move_ai_score)
    val backendCurve = decision?.pre_move_ai_curve
        ?.mapNotNull { normalizeAiPercentValue(it) }
        ?.takeIf { it.isNotEmpty() }
    if (backendCurve != null) {
        return anchoredPreMoveCurve(
            values = backendCurve,
            tailScore = directScore ?: backendCurve.lastOrNull()
        )
    }

    val journal = normalizeAi01(decision.journal_score)
    val ignition = normalizeAi01(decision.ignition_probability, journal)
    val expansion = normalizeAi01(decision.expansion_probability, journal)
    val confluence = normalizeAi01(decision.confluence_score, journal)
    val entry = normalizeAi01(decision.entry_quality_score, journal)
    val structure = normalizeAi01(decision.structure_score ?: decision.structural_pressure_score, 0f)
    val chartContext = normalizeAi01(decision.chart_context_score ?: decision.mtf_alignment_score, structure)
    val volatility = normalizeAi01(decision.feeder_volatility_score, 0f)
    val phaseBase = volatilityPhaseBase(decision.feeder_volatility_state)
    val tailScore = directScore ?: aiPercent(decision.journal_score).takeIf { it > 0f }

    val hasRealAiInputs = listOf(
        decision.pre_move_ai_score,
        decision.journal_score,
        decision.ignition_probability,
        decision.expansion_probability,
        decision.confluence_score,
        decision.entry_quality_score,
        decision.structure_score,
        decision.structural_pressure_score,
        decision.chart_context_score,
        decision.mtf_alignment_score,
        decision.feeder_volatility_score
    ).any { it?.isFinite() == true }
    if (!hasRealAiInputs) return emptyList()

    return anchoredPreMoveCurve(
        values = listOf(
        8f + volatility * 12f,
        phaseBase,
        24f + structure * 26f,
        30f + chartContext * 30f,
        38f + confluence * 30f,
        48f + ignition * 28f,
        52f + entry * 28f,
        56f + expansion * 30f,
        tailScore ?: (40f + journal * 55f)
        ),
        tailScore = tailScore
    )
}

private fun anchoredPreMoveCurve(values: List<Float>, tailScore: Float? = null): List<Float> {
    val sanitized = values
        .map { it.coerceIn(0f, 100f) }
    if (sanitized.isEmpty()) return emptyList()

    val anchored = mutableListOf<Float>()
    anchored += 0f
    anchored += sanitized
    tailScore?.coerceIn(0f, 100f)?.let { score ->
        if (anchored.isEmpty() || kotlin.math.abs((anchored.lastOrNull() ?: 0f) - score) > 0.1f) {
            anchored += score
        }
    }

    return anchored.fold(mutableListOf<Float>()) { acc, value ->
        if (acc.isEmpty() || kotlin.math.abs(acc.last() - value) > 0.1f) {
            acc += value
        }
        acc
    }
}

private fun samplePreMoveAiValue(values: List<Float>, fraction: Float): Float {
    if (values.isEmpty()) return 0f
    if (values.size == 1) return values.first().coerceIn(0f, 100f)

    val clampedFraction = fraction.coerceIn(0f, 1f)
    val scaledIndex = clampedFraction * values.lastIndex.toFloat()
    val lowerIndex = scaledIndex.toInt().coerceIn(0, values.lastIndex)
    val upperIndex = (lowerIndex + 1).coerceAtMost(values.lastIndex)
    val localProgress = (scaledIndex - lowerIndex).coerceIn(0f, 1f)
    val start = values[lowerIndex].coerceIn(0f, 100f)
    val end = values[upperIndex].coerceIn(0f, 100f)
    val baseline = start + ((end - start) * localProgress)

    if (lowerIndex == upperIndex) return baseline.coerceIn(0f, 100f)

    val waveAmplitude = maxOf(1.4f, kotlin.math.abs(end - start) * 0.18f)
    val waveDirection = if (lowerIndex % 2 == 0) 1f else -1f
    val wave = Math.sin(localProgress * Math.PI).toFloat() * waveAmplitude * waveDirection
    return (baseline + wave).coerceIn(0f, 100f)
}

private fun volatilityPhaseBase(state: String?): Float {
    return when (state?.uppercase(Locale.US)) {
        "EXPLOSIVE" -> 92f
        "BURST" -> 84f
        "EXPANDING" -> 72f
        "NORMAL" -> 50f
        "COMPRESSED" -> 38f
        "DEAD" -> 16f
        else -> 30f
    }
}

private fun preMoveScore(pair: ForexPair, decision: FinalDecisionItem?): Float {
    if (decision == null) return 0f
    
    // For TRADE_CANDIDATE state, use final_trade_score (68%+)
    // For REJECTED state, use pre_move_ai_score to show progress (0-25%)
    val finalState = decision.final_trade_state?.uppercase() ?: "REJECTED"
    
    if (finalState == "TRADE_CANDIDATE") {
        // Use final_trade_score for trade candidates (0.0 to 1.0)
        val finalScore = decision.final_trade_score?.takeIf { it.isFinite() }?.let { (it * 100f).toFloat() }
        if (finalScore != null && finalScore > 0f) return finalScore.coerceIn(0f, 100f)
    }
    
    // For rejected/other states, use pre_move_ai_score to show incremental progress
    val preMoveScore = decision.pre_move_ai_score?.takeIf { it.isFinite() }?.let { (it * 100f).toFloat() }
    if (preMoveScore != null && preMoveScore > 0f) return preMoveScore.coerceIn(0f, 100f)
    
    // Fallback to curve if available
    val curve = decision.pre_move_ai_curve.orEmpty()
    if (curve.isNotEmpty()) return curve.last().toFloat().coerceIn(0f, 100f)
    
    return 0f
}

private fun noiseBandCurve(pair: ForexPair): List<Float> {
    val drift = pair.changePercent.toFloat().coerceIn(-2f, 2f)
    val base = (9f + kotlin.math.abs(drift) * 2f).coerceIn(7f, 13f)
    return listOf(
        base - 3f,
        base - 1f,
        base + 2f,
        base + 1f,
        base + 4f,
        base + 2f,
        base + 5f,
        base + 3f,
        base + 6f
    ).map { it.coerceIn(4f, 19f) }
}

private fun normalizeAi01(value: Double?, fallback: Float = 0f): Float {
    val safe = value?.takeIf { it.isFinite() }?.toFloat() ?: fallback
    return if (safe > 1f) (safe / 100f).coerceIn(0f, 1f) else safe.coerceIn(0f, 1f)
}

private fun normalizeAiPercentValue(value: Double?): Float? {
    val safe = value?.takeIf { it.isFinite() } ?: return null
    val normalized = if (safe > 1.0) safe.toFloat() else (safe * 100.0).toFloat()
    return normalized.coerceIn(0f, 100f)
}

private fun aiPercent(value: Double?, fallback: Float = 0f): Float {
    return normalizeAi01(value, fallback) * 100f
}

private fun preMoveSparkPoints(values: List<Float>): List<Float> {
    if (values.size < 2) return emptyList()
    // Adaptive window: use more points for better visualization across all timeframes
    // Take up to 40 points for smooth curves, or all available if less
    val windowSize = minOf(40, values.size)
    val window = values.takeLast(windowSize).map { it.coerceIn(0f, 100f) }
    return window.map { (1f - (it / 100f) * 0.8f - 0.1f).coerceIn(0.05f, 0.95f) }
}

private fun phaseY(value: Float, height: Float): Float {
    return height - (value.coerceIn(0f, 100f) / 100f) * height
}

private fun superPhaseSteps(): List<PhaseStep> = listOf(
    PhaseStep("NOISE", "No trade", 0f, 30f, NoiseRed),
    PhaseStep("STRUCTURE", "Forming", 30f, 45f, StructureOrange),
    PhaseStep("COMPRESSION", "Tightening", 45f, 60f, CompressionYellow),
    PhaseStep("PRE-MOVE", "Preparing", 60f, 80f, PreMoveGreen),
    PhaseStep("EXPANSION", "Execution", 80f, 100f, ExpansionBlue)
)

private fun progressionSubtitle(phase: String): String = when (phase.uppercase(Locale.US)) {
    "NOISE" -> "No trade → Waiting for structure"
    "STRUCTURE" -> "Structure forming → Compression pending"
    "COMPRESSION" -> "Compression tightening → Pre-move pending"
    "PRE-MOVE" -> "Confluence building → Ignition pending"
    else -> "Expansion active → Execution window"
}

private fun phaseColor(phase: String): Color = when (phase.uppercase(Locale.US)) {
    "EXPANSION" -> ExpansionBlue
    "PRE-MOVE" -> PreMoveGreen
    "COMPRESSION" -> CompressionYellow
    "STRUCTURE" -> StructureOrange
    else -> NoiseRed
}

private fun phaseBoundaryColor(level: Float): Color = when {
    level >= 80f -> ExpansionBlue
    level >= 60f -> PreMoveGreen
    level >= 45f -> CompressionYellow
    level >= 30f -> StructureOrange
    else -> NoiseRed
}

private fun preMoveColor(score: Float): Color = when {
    score >= 80f -> ExpansionBlue
    score >= 60f -> PreMoveGreen
    score >= 45f -> CompressionYellow
    score >= 30f -> StructureOrange
    else -> NoiseRed
}

private fun preMoveState(score: Float, decision: FinalDecisionItem? = null): String {
    // Use ONLY final_trade_state from FINAL_TRADING_AI
    decision?.final_trade_state?.let { state ->
        return when (state.uppercase(Locale.US)) {
            "TRADE_CANDIDATE" -> when (decision.final_trade_label?.uppercase(Locale.US)) {
                "PRIORITY_A" -> "EXPANSION" // High priority = expansion phase
                "PRIORITY_B" -> "PRE-MOVE"  // Normal priority = pre-move phase
                else -> "PRE-MOVE"
            }
            "MANUAL_REVIEW" -> "COMPRESSION" // Review = compression/waiting
            "REJECTED" -> when {
                score >= 45f -> "COMPRESSION" // Some setup but not ready
                score >= 30f -> "STRUCTURE"   // Structure forming
                else -> "NOISE"                // No setup
            }
            else -> "NOISE"
        }
    }
    
    // If no final_trade_state, derive from score
    return when {
        score >= 80f -> "EXPANSION"
        score >= 60f -> "PRE-MOVE"
        score >= 45f -> "COMPRESSION"
        score >= 30f -> "STRUCTURE"
        else -> "NOISE"
    }
}

private fun phaseFromScore(score: Float): String = when {
    score >= 80f -> "EXPANSION"
    score >= 60f -> "PRE-MOVE"
    score >= 45f -> "COMPRESSION"
    score >= 30f -> "STRUCTURE"
    else -> "NOISE"
}

private fun progressDetailCards(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float, displayColor: Color): List<ProgressDetailCard> {
    val phase = currentPhase.uppercase(Locale.US)
    val volatilityState = textOrDefault(decision?.feeder_volatility_state, if (decision == null) "UNKNOWN" else "NORMAL")
    val confluence = normalizeAi01(decision?.confluence_score, 0f)
    val ignition = normalizeAi01(decision?.ignition_probability, 0f)
    val entry = normalizeAi01(decision?.entry_quality_score, 0f)
    val structure = normalizeAi01(decision?.structure_score ?: decision?.structural_pressure_score, 0f)
    val chartContext = normalizeAi01(decision?.chart_context_score ?: decision?.mtf_alignment_score, structure)
    val expansion = normalizeAi01(decision?.expansion_probability, 0f)
    return when (phase) {
        "NOISE" -> listOf(
            ProgressDetailCard("NOISE", "Volatility State", volatilityState, "Weak or empty volatility state", "⌁", displayColor, null, true),
            ProgressDetailCard("NO TRADE", "Decision State", textOrDefault(decision?.portfolio_decision_label, "NO_TRADE"), "Context not aligned", "×", displayColor, null, false),
            ProgressDetailCard("QUIET", "Structure Label", textOrDefault(decision?.structure_label ?: decision?.structural_pressure_label, "QUIET"), "Structure below active threshold", "□", displayColor, structure, false)
        )
        "STRUCTURE" -> listOf(
            ProgressDetailCard("STRUCTURE", "Structure Score", formatAiScore(structure), "Structure forming", "▦", displayColor, structure, structure >= 0.35f),
            ProgressDetailCard("PRESSURE", "Pressure Label", textOrDefault(decision?.structural_pressure_label, "PRESSURE_BUILD"), "Pressure build check", "≋", displayColor, structure, structure < 0.35f),
            ProgressDetailCard("CONTEXT", "Chart Context", formatAiScore(chartContext), "Context beginning to align", "◎", displayColor, chartContext, false)
        )
        "COMPRESSION" -> listOf(
            ProgressDetailCard("COMPRESSED", "Volatility State", volatilityState, "Compression before ignition", "⇥", displayColor, normalizeAi01(decision?.feeder_volatility_score, 0f), true),
            ProgressDetailCard("WAIT", "Readiness Label", "WAIT_FOR_EXPANSION", "Low-volatility setup waiting", "⏱", displayColor, null, false),
            ProgressDetailCard("BUILD", "ASC Readiness", "${String.format("%.1f", aiScore)}%", "Expansion pressure loading", "↯", displayColor, aiScore / 100f, false)
        )
        "PRE-MOVE" -> {
            val activeTitle = when {
                confluence >= 0.65f -> "CONFLUENCE"
                ignition >= 0.55f -> "IGNITION BUILD"
                entry >= 0.55f -> "READY TO IGNITE"
                else -> "EXPANDING"
            }
            listOf(
                ProgressDetailCard("EXPANDING", "Volatility State", volatilityState, "Early expansion", "↗", displayColor, normalizeAi01(decision?.feeder_volatility_score, 0f), activeTitle == "EXPANDING"),
                ProgressDetailCard("CONFLUENCE", "Confluence Score", formatAiScore(confluence), "Confluence building", "⊙", displayColor, confluence, activeTitle == "CONFLUENCE"),
                ProgressDetailCard("IGNITION BUILD", "Ignition Probability", "${(ignition * 100f).toInt()}%", "Ignition probability building", "⚡", displayColor, ignition, activeTitle == "IGNITION BUILD"),
                ProgressDetailCard("READY TO IGNITE", "Entry Quality", formatAiScore(entry), "Entry quality improving", "▣", displayColor, entry, activeTitle == "READY TO IGNITE")
            )
        }
        else -> listOf(
            ProgressDetailCard("BURST", "Volatility State", volatilityState, "Expansion underway", "✦", displayColor, normalizeAi01(decision?.feeder_volatility_score, 0f), true),
            ProgressDetailCard("EXPANSION", "Expansion Probability", "${(expansion * 100f).toInt()}%", "Expansion probability confirmed", "↟", displayColor, expansion, expansion >= 0.78f),
            ProgressDetailCard("MOMENTUM", "ASC Readiness", "${String.format("%.1f", aiScore)}%", "Momentum execution window", "→", displayColor, aiScore / 100f, false)
        )
    }
}

private fun entryStyleCards(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float, displayColor: Color): List<EntryStyleCard> {
    val phase = currentPhase.uppercase(Locale.US)
    val volatilityState = decision?.feeder_volatility_state?.uppercase(Locale.US).orEmpty()
    val regimeState = decision?.regime_state?.uppercase(Locale.US).orEmpty()
    val confluence = normalizeAi01(decision?.confluence_score, 0f)
    val ignition = normalizeAi01(decision?.ignition_probability, 0f)
    return when (phase) {
        "NOISE" -> listOf(
            EntryStyleCard("NO TRADE", "ACTIVE BLOCK", "Noise phase is active, so execution stays blocked until structure appears.", "×", displayColor, true),
            EntryStyleCard("DEAD MARKET BLOCK", "WATCH", "Dead or unknown volatility means there is no clean deterministic move yet.", "⌁", displayColor, false),
            EntryStyleCard("CONTEXT NOT ALIGNED", "WAIT", "Context is not aligned enough for pre-move entry selection.", "□", displayColor, false)
        )
        "STRUCTURE" -> listOf(
            EntryStyleCard("STRUCTURE FORMING", "HIGH MATCH", "Structure is forming; wait for pressure build or compression confirmation.", "▦", displayColor, true),
            EntryStyleCard("PRESSURE BUILD", "MEDIUM", "Structural pressure is building but has not moved into compression/pre-move yet.", "≋", displayColor, false),
            EntryStyleCard("CONTEXT ALIGNMENT", "WATCH", "Chart context needs more confluence before entry style selection.", "◎", displayColor, false)
        )
        "COMPRESSION" -> listOf(
            EntryStyleCard("WAIT FOR EXPANSION", "HIGH MATCH", "Compression is active; best action is waiting for expansion/ignition confirmation.", "⇥", displayColor, true),
            EntryStyleCard("LOW VOL SETUP", "MEDIUM", "Low volatility can precede the move, but entry is not ready yet.", "⏱", displayColor, false),
            EntryStyleCard("IGNITION WATCH", "WATCH", "Track ignition probability for transition into pre-move.", "↯", displayColor, false)
        )
        "PRE-MOVE" -> {
            val active = when {
                regimeState == "RANGING" -> "RANGE EDGE CONFIRMATION"
                volatilityState == "EXPANDING" -> "MOMENTUM PULLBACK"
                confluence >= 0.65f && ignition >= 0.50f -> "DISPLACEMENT PULLBACK"
                else -> "MOMENTUM PULLBACK"
            }
            listOf(
                EntryStyleCard("MOMENTUM PULLBACK", if (active == "MOMENTUM PULLBACK") "HIGH MATCH" else "MEDIUM", "Confluence is strong and ignition probability is building. Good setup developing.", "↗", displayColor, active == "MOMENTUM PULLBACK"),
                EntryStyleCard("DISPLACEMENT PULLBACK", if (active == "DISPLACEMENT PULLBACK") "HIGH MATCH" else "MEDIUM", "Use when confluence and ignition align with expansion pressure.", "↗", displayColor, active == "DISPLACEMENT PULLBACK"),
                EntryStyleCard("RANGE EDGE CONFIRMATION", if (active == "RANGE EDGE CONFIRMATION") "HIGH MATCH" else "LOW", "Use when the regime is ranging and confirmation appears at the edge.", "⊚", displayColor, active == "RANGE EDGE CONFIRMATION")
            )
        }
        else -> listOf(
            EntryStyleCard("BREAKOUT CONTINUATION", "HIGH MATCH", "Expansion is active; continuation is preferred when burst conditions persist.", "↟", displayColor, true),
            EntryStyleCard("STRONG MOMENTUM EXECUTION", if (aiScore >= 85f) "HIGH MATCH" else "MEDIUM", "Use when explosive momentum confirms continuation pressure.", "✦", displayColor, aiScore >= 85f),
            EntryStyleCard("PULLBACK WAIT", "WATCH", "If expansion is extended, wait for a cleaner continuation pullback.", "↘", displayColor, false)
        )
    }
}

private fun textOrDefault(value: String?, fallback: String): String {
    return value?.takeIf { it.isNotBlank() } ?: fallback
}

private fun formatAiScore(value: Float): String {
    return String.format(Locale.US, "%.2f", value.coerceIn(0f, 1f))
}

private fun formatPreMovePrice(pair: ForexPair): String {
    return when (pair.category) {
        MarketCategory.FOREX -> if (pair.price >= 100.0) String.format(Locale.US, "%.2f", pair.price) else String.format(Locale.US, "%.5f", pair.price) // 5 decimals for forex (like MT5)
        MarketCategory.BONDS -> String.format(Locale.US, "%.3f", pair.price)
        else -> String.format(Locale.US, "%,.2f", pair.price)
    }
}

private fun preMovePriceUnit(pair: ForexPair): String {
    val key = normalizeAssetKey(pair.symbol)
    return when {
        pair.category == MarketCategory.CRYPTO && key.endsWith("USDT") -> "USDT"
        pair.category == MarketCategory.CRYPTO && key.endsWith("USD") -> "USD"
        pair.symbol.contains("/") -> pair.symbol.substringAfter("/").uppercase(Locale.US)
        pair.category == MarketCategory.BONDS -> "%"
        pair.category == MarketCategory.INDICES -> "PTS"
        pair.category == MarketCategory.STOCK -> "USD"
        else -> "USD"
    }
}

private fun assetAlias(pair: ForexPair): String {
    val key = normalizeAssetKey(pair.symbol)
    return when (key) {
        "BTCUSDT" -> "BTCUSDT"
        "BTCUSD" -> "BTCUSD"
        "ETHUSDT" -> "ETHUSDT"
        "ETHUSD" -> "ETHUSD"
        "SPX500", "SP500" -> "SP500"
        "NAS100", "NASDAQ", "NDX", "IXIC" -> "NASDAQ"
        else -> key.ifBlank { pair.symbol.uppercase(Locale.US) }
    }
}

private fun assetSubtitle(pair: ForexPair): String {
    val key = normalizeAssetKey(pair.symbol)
    return when (key) {
        "BTCUSDT" -> "Bitcoin / Tether"
        "BTCUSD" -> "Bitcoin / US Dollar"
        "ETHUSDT" -> "Ethereum / Tether"
        "ETHUSD" -> "Ethereum / US Dollar"
        else -> pair.name.takeIf { it.isNotBlank() && it != pair.symbol } ?: pair.symbol
    }
}

private fun rowPreparationLabel(state: String): String {
    return when (state.uppercase(Locale.US)) {
        "EXPANSION" -> "Execution Active"
        "PRE-MOVE" -> "Preparation Active"
        "COMPRESSION" -> "Compression Active"
        "STRUCTURE" -> "Structure Active"
        else -> "No Trade Active"
    }
}

private fun rowIgnitionLabel(state: String): String {
    return when (state.uppercase(Locale.US)) {
        "EXPANSION" -> "Continuation active"
        "PRE-MOVE" -> "Ignition pending"
        "COMPRESSION" -> "Expansion pending"
        "STRUCTURE" -> "Compression pending"
        else -> "Structure pending"
    }
}

private fun formatPreMoveChange(pair: ForexPair, includeLabel: Boolean = true): String {
    val sign = if (pair.change >= 0.0) "+" else ""
    val label = if (includeLabel) " Today" else ""
    return "$sign${String.format(Locale.US, "%.2f", pair.change)} (${sign}${String.format(Locale.US, "%.2f", pair.changePercent)}%)$label"
}

private fun formatPreMoveRowChange(pair: ForexPair): String {
    val amountSign = if (pair.change >= 0.0) "+" else ""
    val percentSign = if (pair.changePercent >= 0.0) "+" else ""
    val amount = String.format(Locale.US, "%,.2f", pair.change)
    val percent = String.format(Locale.US, "%.2f", pair.changePercent)
    return "$amountSign$amount  $percentSign$percent%"
}

/**
 * AI Pre-Move Readiness Engine
 * Calculates market readiness score (0-100) based on hidden market preparation
 * NOT price direction - measures how close market is to expansion
 */
private fun calculateAiReadinessScores(priceData: List<TimedPrice>, windowMillis: Long): List<Float> {
    if (priceData.size < 10) return emptyList()
    
    val scores = mutableListOf<Float>()
    var previousSmoothed = 0f
    
    for (i in priceData.indices) {
        val windowSize = minOf(20, i + 1)
        val window = priceData.subList(maxOf(0, i - windowSize + 1), i + 1)
        
        // Component 1: Structure Quality (20%)
        val structureScore = calculateStructureScore(window)
        
        // Component 2: Compression Quality (20%)
        val compressionScore = calculateCompressionScore(window)
        
        // Component 3: Volatility Alignment (15%)
        val volatilityScore = calculateVolatilityScore(window)
        
        // Component 4: Momentum Buildup (15%)
        val momentumScore = calculateMomentumScore(window)
        
        // Component 5: Liquidity Pressure (10%)
        val liquidityScore = calculateLiquidityScore(window)
        
        // Component 6: Confluence Score (10%)
        val confluenceScore = calculateConfluenceScore(window)
        
        // Component 7: Expansion Probability (10%)
        val expansionScore = calculateExpansionProbability(window, structureScore, compressionScore)
        
        // Weighted AI Score
        val rawScore = (
            structureScore * 0.20f +
            compressionScore * 0.20f +
            volatilityScore * 0.15f +
            momentumScore * 0.15f +
            liquidityScore * 0.10f +
            confluenceScore * 0.10f +
            expansionScore * 0.10f
        ).coerceIn(0f, 100f)
        
        // Apply momentum memory (smoothing) - prevents instant jumps
        val smoothed = previousSmoothed * 0.8f + rawScore * 0.2f
        previousSmoothed = smoothed
        
        scores.add(smoothed)
    }
    
    return scores
}

// Structure Quality: measures trend formation and higher lows/highs
private fun calculateStructureScore(window: List<TimedPrice>): Float {
    if (window.size < 3) return 5f
    
    val prices = window.map { it.price }
    val highs = mutableListOf<Double>()
    val lows = mutableListOf<Double>()
    
    for (i in 1 until prices.size - 1) {
        if (prices[i] > prices[i-1] && prices[i] > prices[i+1]) highs.add(prices[i])
        if (prices[i] < prices[i-1] && prices[i] < prices[i+1]) lows.add(prices[i])
    }
    
    // Check for higher highs and higher lows (uptrend structure)
    val higherHighs = if (highs.size >= 2) highs.zipWithNext().count { it.second > it.first } else 0
    val higherLows = if (lows.size >= 2) lows.zipWithNext().count { it.second > it.first } else 0
    
    val structureQuality = ((higherHighs + higherLows).toFloat() / maxOf(1, highs.size + lows.size - 2)) * 100f
    
    return (structureQuality * 0.4f + 20f).coerceIn(0f, 40f) // NOISE to STRUCTURE range
}

// Compression Quality: measures range tightening and volatility shrinking
private fun calculateCompressionScore(window: List<TimedPrice>): Float {
    if (window.size < 5) return 30f
    
    val prices = window.map { it.price }
    val recentRange = prices.takeLast(5).let { it.maxOrNull()!! - it.minOrNull()!! }
    val fullRange = prices.let { it.maxOrNull()!! - it.minOrNull()!! }
    
    // Compression = recent range is smaller than full range
    val compressionRatio = if (fullRange > 0) (1.0 - (recentRange / fullRange)).toFloat() else 0f
    
    // Oscillation tightness
    val recentStdDev = calculateStdDev(prices.takeLast(5))
    val fullStdDev = calculateStdDev(prices)
    val tightness = if (fullStdDev > 0) (1.0 - (recentStdDev / fullStdDev)).toFloat() else 0f
    
    val compressionScore = (compressionRatio * 0.6f + tightness * 0.4f) * 100f
    
    return (compressionScore * 0.4f + 40f).coerceIn(30f, 60f) // STRUCTURE to COMPRESSION range
}

// Volatility Alignment: measures volatility patterns
private fun calculateVolatilityScore(window: List<TimedPrice>): Float {
    if (window.size < 5) return 20f
    
    val prices = window.map { it.price }
    val volatility = calculateStdDev(prices)
    val avgPrice = prices.average()
    
    val volatilityPercent = if (avgPrice > 0) ((volatility / avgPrice) * 100.0).toFloat() else 0f
    
    // Moderate volatility is good (not too low, not too high)
    val idealVolatility = 0.5f
    val alignment = 100f - minOf(100f, kotlin.math.abs(volatilityPercent - idealVolatility) * 20f)
    
    return alignment.coerceIn(0f, 100f)
}

// Momentum Buildup: measures acceleration
private fun calculateMomentumScore(window: List<TimedPrice>): Float {
    if (window.size < 3) return 20f
    
    val prices = window.map { it.price }
    val changes = prices.zipWithNext { a, b -> b - a }
    
    // Positive momentum = more positive changes
    val positiveChanges = changes.count { it > 0 }
    val momentumRatio = positiveChanges.toFloat() / changes.size.toFloat()
    
    // Acceleration = changes are getting larger
    val recentChanges = changes.takeLast(3).map { kotlin.math.abs(it) }.average()
    val earlierChanges = changes.take(maxOf(1, changes.size - 3)).map { kotlin.math.abs(it) }.average()
    val acceleration = if (earlierChanges > 0) (recentChanges / earlierChanges).toFloat() else 1f
    
    val momentumScore = (momentumRatio * 0.6f + acceleration.coerceIn(0f, 2f) * 0.2f) * 100f
    
    return momentumScore.coerceIn(0f, 100f)
}

// Liquidity Pressure: measures volume-like pressure (simulated from price action)
private fun calculateLiquidityScore(window: List<TimedPrice>): Float {
    if (window.size < 5) return 30f
    
    val prices = window.map { it.price }
    val changes = prices.zipWithNext { a, b -> kotlin.math.abs(b - a) }
    
    // Larger price moves = more liquidity
    val avgChange = changes.average()
    val recentAvgChange = changes.takeLast(3).average()
    
    val liquidityIncrease = if (avgChange > 0) ((recentAvgChange / avgChange) - 1.0).toFloat() else 0f
    
    val liquidityScore = (50f + liquidityIncrease * 50f).coerceIn(0f, 100f)
    
    return liquidityScore
}

// Confluence Score: measures alignment of multiple factors
private fun calculateConfluenceScore(window: List<TimedPrice>): Float {
    if (window.size < 5) return 30f
    
    val prices = window.map { it.price }
    
    // Factor 1: Trend consistency
    val changes = prices.zipWithNext { a, b -> b - a }
    val trendConsistency = kotlin.math.abs(changes.count { it > 0 }.toFloat() / changes.size - 0.5f) * 2f
    
    // Factor 2: Price position in range
    val currentPrice = prices.last()
    val rangeMin = prices.minOrNull()!!
    val rangeMax = prices.maxOrNull()!!
    val rangePosition = if (rangeMax > rangeMin) ((currentPrice - rangeMin) / (rangeMax - rangeMin)).toFloat() else 0.5f
    
    // Factor 3: Recent strength
    val recentStrength = if (prices.size >= 3) {
        val recent = prices.takeLast(3).average()
        val earlier = prices.take(prices.size - 3).average()
        if (earlier > 0) ((recent / earlier) - 1.0).toFloat() else 0f
    } else 0f
    
    val confluenceScore = (trendConsistency * 0.4f + rangePosition * 0.3f + (recentStrength + 1f) * 0.3f) * 100f
    
    return confluenceScore.coerceIn(0f, 100f)
}

// Expansion Probability: measures likelihood of breakout
private fun calculateExpansionProbability(window: List<TimedPrice>, structureScore: Float, compressionScore: Float): Float {
    if (window.size < 5) return 20f
    
    val prices = window.map { it.price }
    
    // High structure + high compression = high expansion probability
    val setupQuality = (structureScore + compressionScore) / 2f
    
    // Recent momentum
    val recentPrices = prices.takeLast(3)
    val momentum = if (recentPrices.size >= 2) {
        val change = recentPrices.last() - recentPrices.first()
        val range = prices.maxOrNull()!! - prices.minOrNull()!!
        if (range > 0) (kotlin.math.abs(change) / range * 100.0).toFloat() else 0f
    } else 0f
    
    // Breakout proximity (is price near range extremes?)
    val currentPrice = prices.last()
    val rangeMin = prices.minOrNull()!!
    val rangeMax = prices.maxOrNull()!!
    val distanceToExtreme = minOf(
        kotlin.math.abs(currentPrice - rangeMax),
        kotlin.math.abs(currentPrice - rangeMin)
    )
    val range = rangeMax - rangeMin
    val proximityScore = if (range > 0) (1.0 - (distanceToExtreme / range)).toFloat() * 100f else 0f
    
    val expansionProb = (setupQuality * 0.5f + momentum * 0.3f + proximityScore * 0.2f).coerceIn(0f, 100f)
    
    return expansionProb
}

// Helper: Calculate standard deviation
private fun calculateStdDev(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val mean = values.average()
    val variance = values.map { (it - mean) * (it - mean) }.average()
    return kotlin.math.sqrt(variance)
}


