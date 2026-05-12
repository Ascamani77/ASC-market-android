package com.asc.markets.ui.screens.dashboard

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    val chartValues = aiCurve
    val priceSeries = preMovePriceSeries(selectedPair, priceHistory, timedPriceHistory, selectedTimeframe)
    val hasChartData = priceSeries.values.size >= 2
    val aiScore = preMoveScore(selectedPair, selectedDecision)
    val chartCurrentValue = chartValues.lastOrNull() ?: aiScore
    val currentPhase = phaseFromScore(chartCurrentValue)
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
                    .height(92.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureBlack)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AI PRE-MOVE SCORE", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextGray, modifier = Modifier.size(11.dp))
                        }
                        Text("${aiScore.toInt()}%", color = scoreColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text(scoreState, color = scoreColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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

        CurrentProgressDetailsSection(selectedDecision, currentPhase, aiScore)

        Spacer(modifier = Modifier.height(12.dp))

        EntryStyleSection(selectedDecision, currentPhase, aiScore)

        Spacer(modifier = Modifier.height(32.dp))

        // 7. Stocks List
        assetRows.forEach { pair ->
            val rowDecision = findAiDecision(pair, aiDecisions)
            val rowCurve = preMoveAiCurve(pair, rowDecision)
            val rowScore = preMoveScore(pair, rowDecision)
            val rowColor = preMoveColor(rowScore)
            StockListItem(
                name = assetAlias(pair),
                subtitle = assetSubtitle(pair),
                score = "${rowScore.toInt()}%",
                price = formatPreMovePrice(pair),
                priceChange = formatPreMoveRowChange(pair),
                state = preMoveState(rowScore, rowDecision),
                color = rowColor,
                symbol = pair.symbol,
                values = rowCurve,
                isHighlighted = pair.symbol == selectedPair.symbol,
                metricsScrollState = assetRowScrollState,
                onClick = { onAssetSelected(pair) }
            )
        }
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
        modifier = Modifier.fillMaxWidth(),
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
    val currentTime by produceState(initialValue = System.currentTimeMillis(), selectedTimeframe) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L)
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
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(start = 0.dp, top = 28.dp, end = 28.dp, bottom = 30.dp)) {
            val w = size.width
            val h = size.height
            
            // Draw AI phase reference lines (0-100 scale on left Y-axis)
            val aiPhases = listOf(
                Triple(0f, Color.White.copy(alpha = 0.5f), "0"),
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
                
                // Draw phase label on left
                textPaint.textSize = 8.sp.toPx()
                textPaint.color = color.copy(alpha = 0.7f).toArgb()
                nativeCanvas.drawText(
                    label,
                    8.dp.toPx(),
                    yPos - 8.dp.toPx(),
                    textPaint
                )
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
                val fillPath = Path().apply {
                    moveTo(pricePoints.first().x, h)
                    lineTo(pricePoints.first().x, pricePoints.first().y)
                    pricePoints.drop(1).forEach { point -> lineTo(point.x, point.y) }
                    lineTo(pricePoints.last().x, h)
                    close()
                }
                val lineColor = Color(0xFF6FCBC0)
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.30f),
                            lineColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        startY = pricePoints.minOf { it.y },
                        endY = h
                    )
                )
                drawPath(
                    path = linePath,
                    color = lineColor.copy(alpha = 0.94f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
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
                    // Create AI points by interpolating aiValues across the time window
                    val aiPoints = liveEntries.mapIndexed { index, point ->
                        val xFraction = if (priceSeries.windowMillis <= 0L) {
                            1f
                        } else {
                            ((point.timestampMillis - windowStart).toDouble() / priceSeries.windowMillis.toDouble())
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                        
                        // Interpolate AI score from aiValues array
                        val aiIndex = ((index.toFloat() / liveEntries.size.toFloat()) * aiValues.size.toFloat()).toInt().coerceIn(0, aiValues.size - 1)
                        val score = aiValues[aiIndex]
                        
                        // Map AI score (0-100) to Y position
                        val yPos = h - (score / 100f * h)
                        Offset(xFraction * w, yPos)
                    }
                    
                    val aiPath = Path().apply {
                        if (aiPoints.isNotEmpty()) {
                            moveTo(aiPoints.first().x, aiPoints.first().y)
                            aiPoints.drop(1).forEach { point -> lineTo(point.x, point.y) }
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
                        
                        // Background box for percentage
                        val boxWidth = 45.dp.toPx()
                        val boxHeight = 20.dp.toPx()
                        val boxX = w - boxWidth - 8.dp.toPx()
                        val boxY = lastAiPoint.y - boxHeight / 2
                        
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
    val activeColor = phaseColor(currentPhase)
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
    val phaseColor = phaseColor(currentPhase)
    val cards = progressDetailCards(decision, currentPhase, aiScore)
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
            .border(if (card.active) 1.5f.dp else 1.dp, if (card.active) card.color else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
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
    val cards = entryStyleCards(decision, currentPhase, aiScore)
    val active = cards.firstOrNull { it.active }
    val activeColor = active?.color ?: phaseColor(currentPhase)
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
                    Text("${aiScore.toInt()}%", color = activeColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
            .border(if (card.active) 1.5f.dp else 1.dp, if (card.active) card.color else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(card.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.weight(1f))
        Text(if (card.active) "Best For Current" else "Match Quality", color = TextGray, fontSize = 9.sp)
        Text(card.match, color = card.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            .clip(RoundedCornerShape(10.dp))
            .background(PureBlack)
            .border(1.dp, if (isHighlighted) color.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
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
    return aiDecisions.firstOrNull { decision ->
        normalizeAssetKey(decision.asset_1.orEmpty()) == key
    }
}

private fun normalizeAssetKey(value: String): String {
    return value.uppercase(Locale.US)
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace(" ", "")
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
    val flatHistory = priceHistory.entries
        .firstOrNull { normalizeAssetKey(it.key) == key }
        ?.value
        .orEmpty()
        .filter { it.isFinite() && it > 0.0 }
    val fallbackPoints = sampleFallbackTimedPrices(flatHistory + pair.price, timeframe, now)
    val sourcePoints = if (bucketedPoints.size >= 8) bucketedPoints else fallbackPoints.ifEmpty { bucketedPoints }
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
        "1hr" -> "1h"
        "4hr" -> "4h"
        "1D" -> "1d"
        "1w" -> "1w"
        else -> "1h"
    }
}

private fun preMoveHistoryInterval(timeframeLabel: String): String {
    return when (timeframeLabel) {
        "5m" -> "5m"
        "15m" -> "15m"
        "30m" -> "30m"
        "1hr" -> "1h"
        "4hr" -> "4h"
        "1D" -> "1d"
        "1w" -> "1d"
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
            "5m", "15m", "30m", "1hr" -> 5
            "4hr" -> 10
            "1D" -> 30
            "1w" -> 90
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
    PreMoveTimeframe("1hr", 60L * 60_000L, 60L * 60_000L * 72L),
    PreMoveTimeframe("4hr", 4L * 60L * 60_000L, 4L * 60L * 60_000L * 72L),
    PreMoveTimeframe("1D", 24L * 60L * 60_000L, 24L * 60L * 60_000L * 120L),
    PreMoveTimeframe("1w", 7L * 24L * 60L * 60_000L, 7L * 24L * 60L * 60_000L * 80L)
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
        "1hr" -> 5
        "4hr" -> 8
        "1D" -> 12
        "1w" -> 20
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
        "1hr" -> 96
        "4hr" -> 108
        "1D" -> 120
        "1w" -> 140
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
        "1hr" -> listOf("-5h", "-4h", "-3h", "-2h", "-1h", "NOW")
        "4hr" -> listOf("-20h", "-16h", "-12h", "-8h", "-4h", "NOW")
        "1D" -> listOf("-5D", "-4D", "-3D", "-2D", "-1D", "NOW")
        "1w" -> listOf("-5w", "-4w", "-3w", "-2w", "-1w", "NOW")
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
    val phase = decision?.pre_move_ai_phase?.uppercase(Locale.US)
    if (decision == null || phase == "NOISE") {
        return noiseBandCurve(pair)
    }
    val backendCurve = decision?.pre_move_ai_curve
        ?.mapNotNull { it.takeIf { value -> value.isFinite() }?.toFloat()?.coerceIn(0f, 100f) }
        ?.takeIf { it.size >= 2 }
    if (backendCurve != null) return backendCurve

    val journal = normalizeAi01(decision.journal_score)
    val ignition = normalizeAi01(decision.ignition_probability, journal)
    val expansion = normalizeAi01(decision.expansion_probability, journal)
    val confluence = normalizeAi01(decision.confluence_score, journal)
    val entry = normalizeAi01(decision.entry_quality_score, journal)
    val structure = normalizeAi01(decision.structure_score ?: decision.structural_pressure_score, 0f)
    val chartContext = normalizeAi01(decision.chart_context_score ?: decision.mtf_alignment_score, structure)
    val volatility = normalizeAi01(decision.feeder_volatility_score, 0f)
    val phaseBase = volatilityPhaseBase(decision.feeder_volatility_state)

    return listOf(
        12f + volatility * 16f,
        phaseBase,
        30f + structure * 30f,
        36f + chartContext * 32f,
        42f + confluence * 34f,
        52f + ignition * 30f,
        56f + entry * 30f,
        58f + expansion * 34f,
        40f + journal * 55f
    ).map { it.coerceIn(0f, 100f) }
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
    if (decision == null) return noiseBandCurve(pair).last()
    if (decision.pre_move_ai_phase?.uppercase(Locale.US) == "NOISE") return noiseBandCurve(pair).last()
    val direct = decision?.pre_move_ai_score?.takeIf { it.isFinite() }?.let { normalizeAi01(it) * 100f }
    if (direct != null) return direct.coerceIn(0f, 100f)
    val curve = preMoveAiCurve(pair, decision)
    if (curve.isNotEmpty()) return curve.last().coerceIn(0f, 100f)
    return aiPercent(decision?.journal_score).takeIf { it > 0f } ?: 0f
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

private fun preMoveState(score: Float, decision: FinalDecisionItem? = null): String = when (decision?.pre_move_ai_phase?.uppercase(Locale.US)) {
    "COMPRESSION" -> "COMPRESSION"
    "EXPANSION" -> "EXPANSION"
    "PRE-MOVE" -> "PRE-MOVE"
    "STRUCTURE" -> "STRUCTURE"
    "NOISE" -> "NOISE"
    else -> when {
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

private fun progressDetailCards(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float): List<ProgressDetailCard> {
    val phase = currentPhase.uppercase(Locale.US)
    val phaseColor = phaseColor(currentPhase)
    val volatilityState = textOrDefault(decision?.feeder_volatility_state, if (decision == null) "UNKNOWN" else "NORMAL")
    val confluence = normalizeAi01(decision?.confluence_score, 0f)
    val ignition = normalizeAi01(decision?.ignition_probability, 0f)
    val entry = normalizeAi01(decision?.entry_quality_score, 0f)
    val structure = normalizeAi01(decision?.structure_score ?: decision?.structural_pressure_score, 0f)
    val chartContext = normalizeAi01(decision?.chart_context_score ?: decision?.mtf_alignment_score, structure)
    val expansion = normalizeAi01(decision?.expansion_probability, 0f)
    return when (phase) {
        "NOISE" -> listOf(
            ProgressDetailCard("NOISE", "Volatility State", volatilityState, "Weak or empty volatility state", "⌁", NoiseRed, null, true),
            ProgressDetailCard("NO TRADE", "Decision State", textOrDefault(decision?.portfolio_decision_label, "NO_TRADE"), "Context not aligned", "×", NoiseRed, null, false),
            ProgressDetailCard("QUIET", "Structure Label", textOrDefault(decision?.structure_label ?: decision?.structural_pressure_label, "QUIET"), "Structure below active threshold", "□", NoiseRed, structure, false)
        )
        "STRUCTURE" -> listOf(
            ProgressDetailCard("STRUCTURE", "Structure Score", formatAiScore(structure), "Structure forming", "▦", StructureOrange, structure, structure >= 0.35f),
            ProgressDetailCard("PRESSURE", "Pressure Label", textOrDefault(decision?.structural_pressure_label, "PRESSURE_BUILD"), "Pressure build check", "≋", StructureOrange, structure, structure < 0.35f),
            ProgressDetailCard("CONTEXT", "Chart Context", formatAiScore(chartContext), "Context beginning to align", "◎", StructureOrange, chartContext, false)
        )
        "COMPRESSION" -> listOf(
            ProgressDetailCard("COMPRESSED", "Volatility State", volatilityState, "Compression before ignition", "⇥", CompressionYellow, normalizeAi01(decision?.feeder_volatility_score, 0f), true),
            ProgressDetailCard("WAIT", "Readiness Label", "WAIT_FOR_EXPANSION", "Low-volatility setup waiting", "⏱", CompressionYellow, null, false),
            ProgressDetailCard("BUILD", "ASC Readiness", "${aiScore.toInt()}%", "Expansion pressure loading", "↯", CompressionYellow, aiScore / 100f, false)
        )
        "PRE-MOVE" -> {
            val activeTitle = when {
                confluence >= 0.65f -> "CONFLUENCE"
                ignition >= 0.55f -> "IGNITION BUILD"
                entry >= 0.55f -> "READY TO IGNITE"
                else -> "EXPANDING"
            }
            listOf(
                ProgressDetailCard("EXPANDING", "Volatility State", volatilityState, "Early expansion", "↗", PreMoveGreen, normalizeAi01(decision?.feeder_volatility_score, 0f), activeTitle == "EXPANDING"),
                ProgressDetailCard("CONFLUENCE", "Confluence Score", formatAiScore(confluence), "Confluence building", "⊙", PreMoveGreen, confluence, activeTitle == "CONFLUENCE"),
                ProgressDetailCard("IGNITION BUILD", "Ignition Probability", "${(ignition * 100f).toInt()}%", "Ignition probability building", "⚡", PreMoveGreen, ignition, activeTitle == "IGNITION BUILD"),
                ProgressDetailCard("READY TO IGNITE", "Entry Quality", formatAiScore(entry), "Entry quality improving", "▣", PreMoveGreen, entry, activeTitle == "READY TO IGNITE")
            )
        }
        else -> listOf(
            ProgressDetailCard("BURST", "Volatility State", volatilityState, "Expansion underway", "✦", ExpansionBlue, normalizeAi01(decision?.feeder_volatility_score, 0f), true),
            ProgressDetailCard("EXPANSION", "Expansion Probability", "${(expansion * 100f).toInt()}%", "Expansion probability confirmed", "↟", ExpansionBlue, expansion, expansion >= 0.78f),
            ProgressDetailCard("MOMENTUM", "ASC Readiness", "${aiScore.toInt()}%", "Momentum execution window", "→", ExpansionBlue, aiScore / 100f, false)
        )
    }.map { if (it.active) it else it.copy(color = if (it.color == phaseColor) it.color else it.color) }
}

private fun entryStyleCards(decision: FinalDecisionItem?, currentPhase: String, aiScore: Float): List<EntryStyleCard> {
    val phase = currentPhase.uppercase(Locale.US)
    val volatilityState = decision?.feeder_volatility_state?.uppercase(Locale.US).orEmpty()
    val regimeState = decision?.regime_state?.uppercase(Locale.US).orEmpty()
    val confluence = normalizeAi01(decision?.confluence_score, 0f)
    val ignition = normalizeAi01(decision?.ignition_probability, 0f)
    return when (phase) {
        "NOISE" -> listOf(
            EntryStyleCard("NO TRADE", "ACTIVE BLOCK", "Noise phase is active, so execution stays blocked until structure appears.", "×", NoiseRed, true),
            EntryStyleCard("DEAD MARKET BLOCK", "WATCH", "Dead or unknown volatility means there is no clean deterministic move yet.", "⌁", NoiseRed, false),
            EntryStyleCard("CONTEXT NOT ALIGNED", "WAIT", "Context is not aligned enough for pre-move entry selection.", "□", NoiseRed, false)
        )
        "STRUCTURE" -> listOf(
            EntryStyleCard("STRUCTURE FORMING", "HIGH MATCH", "Structure is forming; wait for pressure build or compression confirmation.", "▦", StructureOrange, true),
            EntryStyleCard("PRESSURE BUILD", "MEDIUM", "Structural pressure is building but has not moved into compression/pre-move yet.", "≋", StructureOrange, false),
            EntryStyleCard("CONTEXT ALIGNMENT", "WATCH", "Chart context needs more confluence before entry style selection.", "◎", StructureOrange, false)
        )
        "COMPRESSION" -> listOf(
            EntryStyleCard("WAIT FOR EXPANSION", "HIGH MATCH", "Compression is active; best action is waiting for expansion/ignition confirmation.", "⇥", CompressionYellow, true),
            EntryStyleCard("LOW VOL SETUP", "MEDIUM", "Low volatility can precede the move, but entry is not ready yet.", "⏱", CompressionYellow, false),
            EntryStyleCard("IGNITION WATCH", "WATCH", "Track ignition probability for transition into pre-move.", "↯", CompressionYellow, false)
        )
        "PRE-MOVE" -> {
            val active = when {
                regimeState == "RANGING" -> "RANGE EDGE CONFIRMATION"
                volatilityState == "EXPANDING" -> "MOMENTUM PULLBACK"
                confluence >= 0.65f && ignition >= 0.50f -> "DISPLACEMENT PULLBACK"
                else -> "MOMENTUM PULLBACK"
            }
            listOf(
                EntryStyleCard("MOMENTUM PULLBACK", if (active == "MOMENTUM PULLBACK") "HIGH MATCH" else "MEDIUM", "Confluence is strong and ignition probability is building. Good setup developing.", "↗", PreMoveGreen, active == "MOMENTUM PULLBACK"),
                EntryStyleCard("DISPLACEMENT PULLBACK", if (active == "DISPLACEMENT PULLBACK") "HIGH MATCH" else "MEDIUM", "Use when confluence and ignition align with expansion pressure.", "↗", StructureOrange, active == "DISPLACEMENT PULLBACK"),
                EntryStyleCard("RANGE EDGE CONFIRMATION", if (active == "RANGE EDGE CONFIRMATION") "HIGH MATCH" else "LOW", "Use when the regime is ranging and confirmation appears at the edge.", "⊚", NoiseRed, active == "RANGE EDGE CONFIRMATION")
            )
        }
        else -> listOf(
            EntryStyleCard("BREAKOUT CONTINUATION", "HIGH MATCH", "Expansion is active; continuation is preferred when burst conditions persist.", "↟", ExpansionBlue, true),
            EntryStyleCard("STRONG MOMENTUM EXECUTION", if (aiScore >= 85f) "HIGH MATCH" else "MEDIUM", "Use when explosive momentum confirms continuation pressure.", "✦", ExpansionBlue, aiScore >= 85f),
            EntryStyleCard("PULLBACK WAIT", "WATCH", "If expansion is extended, wait for a cleaner continuation pullback.", "↘", PreMoveGreen, false)
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
