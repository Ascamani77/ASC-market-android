package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.TimedPrice
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// Volatility phase colors
private val QuietColor = Color(0xFF4A90E2)
private val BalancedColor = Color(0xFF50C878)
private val BuildingColor = Color(0xFFDCEB3A)
private val CompressedColor = Color(0xFFFFA726)
private val TensionColor = Color(0xFFFF6B6B)
private val IgnitionColor = Color(0xFFE91E63)
private val ExpansionColor = Color(0xFF9C27B0)

private data class VolatilityPhase(
    val label: String,
    val start: Float,
    val end: Float,
    val color: Color
)

private data class VolatilityPoint(
    val timestamp: Long,
    val value: Float,
    val rawValue: Float,
    val isProjected: Boolean = false
)

private data class HourlyVolatilityBar(
    val bucketStart: Long,
    val point: VolatilityPoint,
    val isCurrentHour: Boolean
)

@Composable
fun VolatilityChart(
    selectedPair: ForexPair,
    selectedTimeframe: String,
    timedPriceHistory: Map<String, List<TimedPrice>> = emptyMap(),
    priceHistory: Map<String, List<Double>> = emptyMap(),
    onTimeframeSelected: (String) -> Unit
) {
    val pairHistory = remember(selectedPair.symbol, timedPriceHistory, priceHistory, selectedTimeframe) {
        resolveVolatilityHistory(
            pair = selectedPair,
            timeframe = selectedTimeframe,
            timedPriceHistory = timedPriceHistory,
            priceHistory = priceHistory
        )
    }
    
    // Calculate volatility points from price history
    val volatilityPoints = remember(pairHistory, selectedTimeframe) {
        calculateVolatilityPoints(pairHistory, selectedTimeframe)
    }
    
    // Update current time for live updates
    val updateIntervalMillis = when (selectedTimeframe) {
        "1m" -> 1_000L
        "5m" -> 5_000L
        "15m" -> 15_000L
        "30m" -> 30_000L
        "1H" -> 60_000L
        else -> 60_000L
    }
    
    val currentTime by produceState(initialValue = System.currentTimeMillis(), selectedTimeframe) {
        while (true) {
            value = System.currentTimeMillis()
            delay(updateIntervalMillis)
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        VolatilityChartHeader(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = onTimeframeSelected
        )
        Spacer(modifier = Modifier.height(8.dp))
        VolatilityChartBox(
            volatilityPoints = volatilityPoints,
            currentTime = currentTime,
            selectedTimeframe = selectedTimeframe,
            modifier = Modifier
                .fillMaxWidth()
                .height(283.dp)
        )
    }
}

@Composable
private fun VolatilityChartHeader(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "LIVE VOLATILITY LINE",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Timeframe",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                VolatilityTimeframeDropdown(selectedTimeframe, onTimeframeSelected)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Realized Volatility
            Canvas(modifier = Modifier.width(30.dp).height(2.dp)) {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Realized Volatility",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Projected
            Canvas(modifier = Modifier.width(30.dp).height(2.dp)) {
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Projected",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun VolatilityTimeframeDropdown(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timeframes = listOf("1m", "5m", "15m", "30m", "1H")
    
    Box {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedTimeframe,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(5.dp))
                Canvas(modifier = Modifier.width(7.dp).height(5.dp)) {
                    val iconColor = Color.White.copy(alpha = 0.70f)
                    drawLine(
                        iconColor,
                        Offset(0f, 0f),
                        Offset(size.width / 2f, size.height),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        iconColor,
                        Offset(size.width, 0f),
                        Offset(size.width / 2f, size.height),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.Black)
        ) {
            timeframes.forEach { timeframe ->
                DropdownMenuItem(
                    text = {
                        Text(
                            timeframe,
                            color = if (timeframe == selectedTimeframe) Color(0xFF4CAF50) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (timeframe == selectedTimeframe) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    onClick = {
                        expanded = false
                        onTimeframeSelected(timeframe)
                    }
                )
            }
        }
    }
}

@Composable
private fun VolatilityChartBox(
    volatilityPoints: List<VolatilityPoint>,
    currentTime: Long,
    selectedTimeframe: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "volatility_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tip_alpha"
    )
    val bucketMillis = remember(selectedTimeframe) {
        getTimeframeBucketMillis(selectedTimeframe)
    }
    val liveBars = remember(volatilityPoints, currentTime, selectedTimeframe) {
        buildHourlyVolatilityBars(
            volatilityPoints = volatilityPoints,
            currentTime = currentTime,
            timeframe = selectedTimeframe,
            maxBars = 17
        )
    }
    val lockedBars = remember(selectedTimeframe) {
        mutableStateMapOf<Long, VolatilityPoint>()
    }
    var lastLiveCurrentBar by remember(selectedTimeframe) {
        mutableStateOf<HourlyVolatilityBar?>(null)
    }
    val currentBucketStart = remember(currentTime, bucketMillis) {
        alignToTimeframeBucket(currentTime, bucketMillis)
    }

    LaunchedEffect(liveBars, currentBucketStart, bucketMillis, selectedTimeframe) {
        val visibleWindowStart = currentBucketStart - ((17 - 1) * bucketMillis)

        lastLiveCurrentBar
            ?.takeIf { it.bucketStart < currentBucketStart }
            ?.let { snapshot ->
                if (lockedBars[snapshot.bucketStart] == null) {
                    lockedBars[snapshot.bucketStart] = snapshot.point
                }
            }

        liveBars.forEach { bar ->
            if (bar.bucketStart < currentBucketStart && lockedBars[bar.bucketStart] == null) {
                lockedBars[bar.bucketStart] = bar.point
            }
        }

        lockedBars.keys.toList().forEach { bucketStart ->
            if (bucketStart < visibleWindowStart || bucketStart > currentBucketStart) {
                lockedBars.remove(bucketStart)
            }
        }

        lastLiveCurrentBar = liveBars.lastOrNull { it.bucketStart == currentBucketStart }
    }

    val visibleBars = candleLockVolatilityBars(
        liveBars = liveBars,
        lockedPoints = lockedBars,
        currentBucketStart = currentBucketStart,
        lastLiveCurrentBar = lastLiveCurrentBar
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF000000))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val chartLeft = 32.dp.toPx()
            val rightGutter = 58.dp.toPx()
            val chartRight = (w - rightGutter).coerceAtLeast(chartLeft + 1f)
            val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
            val chartTop = 8.dp.toPx()
            val chartBottom = (h - 24.dp.toPx()).coerceAtLeast(chartTop + 1f)
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val textPaint = android.graphics.Paint().apply {
                textSize = 10.sp.toPx()
                isAntiAlias = true
            }
            
            // Draw horizontal grid lines and 0-100 axis labels
            val yLevels = listOf(0f, 10f, 20f, 30f, 40f, 50f, 60f, 70f, 80f, 90f, 100f)
            yLevels.forEach { level ->
                val fraction = level / 100f
                val y = chartBottom - (fraction * chartHeight)
                
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                textPaint.color = Color.White.copy(alpha = 0.55f).toArgb()
                textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                nativeCanvas.drawText(
                    level.toInt().toString(),
                    chartLeft - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    textPaint
                )
            }

            if (visibleBars.isNotEmpty()) {
                if (visibleBars.isNotEmpty()) {
                    val barCount = 17
                    val barSpacing = 4.dp.toPx()
                    val totalSpacing = barSpacing * (barCount - 1)
                    val barWidth = ((chartWidth - totalSpacing) / barCount).coerceAtLeast(4.dp.toPx())
                    
                    val timeLabels = mutableListOf<Pair<Float, Long>>()
                    val currentBar = visibleBars.lastOrNull { it.isCurrentHour }

                    visibleBars.forEachIndexed { index, bar ->
                        val point = bar.point
                        val isCurrentHour = bar.isCurrentHour
                        
                        // Each bar has a fixed position based on its index
                        val xCenter = chartLeft + (barWidth / 2f) + (index * (barWidth + barSpacing))
                        val barHeightPx = (point.value / 100f * chartHeight).coerceIn(0f, chartHeight)
                        val barTop = chartBottom - barHeightPx
                        val barColor = volatilityBarColor(point.value)

                        drawRect(
                            color = barColor,
                            topLeft = Offset(xCenter - barWidth / 2f, barTop),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeightPx)
                        )
                        
                        if (index == 0 || index % 4 == 0 || isCurrentHour) {
                            val labelTimestamp = if (isCurrentHour) point.timestamp else bar.bucketStart
                            timeLabels.add(xCenter to labelTimestamp)
                        }

                        if (isCurrentHour) {
                            drawCircle(
                                color = barColor.copy(alpha = alpha * 0.4f),
                                radius = barWidth * 0.8f,
                                center = Offset(xCenter, barTop + barWidth / 2f)
                            )
                            drawCircle(
                                color = barColor,
                                radius = barWidth * 0.6f,
                                center = Offset(xCenter, barTop + barWidth / 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    currentBar?.let { bar ->
                        val barColor = volatilityBarColor(bar.point.value)
                        val labelX = chartRight + 8.dp.toPx()
                        val labelY = (chartBottom - ((bar.point.value / 100f) * chartHeight)).coerceIn(chartTop + 14.dp.toPx(), chartBottom - 18.dp.toPx())
                        textPaint.color = barColor.toArgb()
                        textPaint.textAlign = android.graphics.Paint.Align.LEFT
                        textPaint.isFakeBoldText = true
                        nativeCanvas.drawText(
                            String.format(Locale.US, "%.1f%%", bar.point.value),
                            labelX,
                            labelY,
                            textPaint
                        )
                        textPaint.textSize = 8.sp.toPx()
                        nativeCanvas.drawText(
                            volatilityBarLabel(bar.point.value),
                            labelX,
                            labelY + 12.dp.toPx(),
                            textPaint
                        )
                        textPaint.textSize = 10.sp.toPx()
                    }
                    
                    textPaint.color = Color.White.copy(alpha = 0.55f).toArgb()
                    textPaint.textAlign = android.graphics.Paint.Align.CENTER
                    textPaint.isFakeBoldText = false
                    timeLabels.forEach { (x, timestamp) ->
                        val timeStr = formatVolatilityTimeLabel(timestamp, selectedTimeframe)
                        nativeCanvas.drawText(
                            timeStr,
                            x,
                            h - 4.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

private fun buildHourlyVolatilityBars(
    volatilityPoints: List<VolatilityPoint>,
    currentTime: Long,
    timeframe: String,
    maxBars: Int
): List<HourlyVolatilityBar> {
    val bucketMillis = getTimeframeBucketMillis(timeframe)
    val realizedPoints = volatilityPoints
        .filter { !it.isProjected }
        .sortedBy { it.timestamp }
    if (realizedPoints.isEmpty()) return emptyList()

    val groupedByHour = realizedPoints.groupBy { point ->
        alignToTimeframeBucket(point.timestamp, bucketMillis)
    }.toSortedMap()
    
    val currentHourBucket = alignToTimeframeBucket(currentTime, bucketMillis)
    
    // Get the last maxBars hours including current hour
    val endHourBucket = currentHourBucket
    val startHourBucket = endHourBucket - ((maxBars - 1) * bucketMillis)
    
    // Build bars for the fixed time window
    val bars = mutableListOf<HourlyVolatilityBar>()
    
    for (i in 0 until maxBars) {
        val bucketStart = startHourBucket + (i * bucketMillis)
        val isCurrentHour = bucketStart == currentHourBucket
        
        // Get the latest point in this hour bucket
        val pointsInBucket = groupedByHour[bucketStart]
        val point = pointsInBucket?.maxByOrNull { it.timestamp }
        
        if (point != null) {
            bars.add(
                HourlyVolatilityBar(
                    bucketStart = bucketStart,
                    point = point,
                    isCurrentHour = isCurrentHour
                )
            )
        }
    }

    return bars
}

private fun candleLockVolatilityBars(
    liveBars: List<HourlyVolatilityBar>,
    lockedPoints: Map<Long, VolatilityPoint>,
    currentBucketStart: Long,
    lastLiveCurrentBar: HourlyVolatilityBar?
): List<HourlyVolatilityBar> {
    val barsByBucket = liveBars.associateBy { it.bucketStart }.toMutableMap()

    lastLiveCurrentBar
        ?.takeIf { it.bucketStart == currentBucketStart && barsByBucket[currentBucketStart] == null }
        ?.let { snapshot ->
            barsByBucket[currentBucketStart] = snapshot
        }

    return barsByBucket.values
        .sortedBy { it.bucketStart }
        .map { bar ->
            val displayPoint = when {
                bar.bucketStart < currentBucketStart -> lockedPoints[bar.bucketStart] ?: bar.point
                bar.bucketStart == currentBucketStart && lastLiveCurrentBar?.bucketStart == currentBucketStart -> lastLiveCurrentBar.point
                else -> bar.point
            }

            HourlyVolatilityBar(
                bucketStart = bar.bucketStart,
                point = displayPoint,
                isCurrentHour = bar.bucketStart == currentBucketStart
            )
        }
}

private fun volatilityBarColor(value: Float): Color {
    return when {
        value < 25f -> Color(0xFF64748B)
        value < 45f -> Color(0xFFDCEB3A)
        value < 60f -> Color(0xFF50C878)
        value < 75f -> Color(0xFFFFA726)
        value < 90f -> Color(0xFFFF6B6B)
        else -> Color(0xFFE91E63)
    }
}

private fun volatilityBarLabel(value: Float): String {
    return when {
        value < 25f -> "DEAD"
        value < 45f -> "COMPRESSED"
        value < 60f -> "NORMAL"
        value < 75f -> "EXPANDING"
        value < 90f -> "BURST"
        else -> "EXPLOSIVE"
    }
}

private fun volatilityPhases(): List<VolatilityPhase> = listOf(
    VolatilityPhase("QUIET", 0f, 0.14f, QuietColor),
    VolatilityPhase("BALANCED", 0.14f, 0.29f, BalancedColor),
    VolatilityPhase("BUILDING", 0.29f, 0.43f, BuildingColor),
    VolatilityPhase("COMPRESSED", 0.43f, 0.57f, CompressedColor),
    VolatilityPhase("TENSION", 0.57f, 0.71f, TensionColor),
    VolatilityPhase("IGNITION", 0.71f, 0.86f, IgnitionColor),
    VolatilityPhase("EXPANSION", 0.86f, 1f, ExpansionColor)
)

private fun getVolatilityColor(value: Float): Color {
    return when {
        value < 14f -> QuietColor
        value < 29f -> BalancedColor
        value < 43f -> BuildingColor
        value < 57f -> CompressedColor
        value < 71f -> TensionColor
        value < 86f -> IgnitionColor
        else -> ExpansionColor
    }
}

private fun volatilityAxisLabels(minRaw: Float, maxRaw: Float): List<Float> {
    val step = ((maxRaw - minRaw).coerceAtLeast(0.000001f)) / 4f
    return List(5) { index -> maxRaw - (step * index) }
}

private fun formatVolatilityAxisLabel(rawValue: Float): String {
    val percentValue = rawValue * 100f
    return when {
        percentValue >= 100f -> String.format(Locale.US, "%.0f%%", percentValue)
        percentValue >= 10f -> String.format(Locale.US, "%.1f%%", percentValue)
        percentValue >= 1f -> String.format(Locale.US, "%.2f%%", percentValue)
        else -> String.format(Locale.US, "%.3f%%", percentValue)
    }
}

private fun formatVolatilityTimeLabel(timestamp: Long, timeframe: String): String {
    val pattern = when (timeframe) {
        "1m", "5m", "15m", "30m" -> "HH:mm"
        "1H" -> "MM/dd HH:mm"
        else -> "HH:mm"
    }
    return SimpleDateFormat(pattern, Locale.US).format(Date(timestamp))
}

private fun calculateVolatilityPoints(
    priceHistory: List<TimedPrice>,
    timeframe: String
): List<VolatilityPoint> {
    if (priceHistory.size < 5) return emptyList()
    
    val targetWindowSize = when (timeframe) {
        "1m" -> 20
        "5m" -> 30
        "15m" -> 40
        "30m" -> 50
        "1H" -> 60
        else -> 60
    }
    
    // Adapt window size to not exceed available history
    val windowSize = targetWindowSize.coerceAtMost((priceHistory.size - 1).coerceAtLeast(5))
    val normalizationWindow = maxOf(20, windowSize * 2)
    
    val points = mutableListOf<VolatilityPoint>()
    val rawScores = mutableListOf<Float>()
    
    // Calculate multi-factor volatility scores
    for (i in windowSize until priceHistory.size) {
        val window = priceHistory.subList(i - windowSize, i)
        val currentCandle = priceHistory[i]
        val prevCandle = if (i > 0) priceHistory[i - 1] else currentCandle
        
        // Multi-factor volatility scoring
        val score = calculateMultiFactorVolatility(
            window = window,
            currentCandle = currentCandle,
            prevCandle = prevCandle,
            allHistory = priceHistory.subList(0, i + 1)
        )
        rawScores.add(score)
    }
    
    if (rawScores.isEmpty()) return emptyList()
    
    for (i in rawScores.indices) {
        val raw = rawScores[i]
        val referenceValues = rawScores.subList((i - normalizationWindow + 1).coerceAtLeast(0), i + 1)
        val normalized = normalizeVolatilityRawValue(raw, referenceValues)
        
        points.add(
            VolatilityPoint(
                timestamp = priceHistory[i + windowSize].timestampMillis,
                value = normalized.coerceIn(0f, 100f),
                rawValue = raw,
                isProjected = false
            )
        )
    }
    
    // Add projected points with acceleration awareness
    if (points.size >= 5) {
        val lastPoints = points.takeLast(5)
        val acceleration = (lastPoints.last().rawValue - lastPoints.first().rawValue) / 5f
        val lastTimestamp = points.last().timestamp
        val projectionReferenceValues = rawScores.takeLast(normalizationWindow)
        val timeStep = if (points.size >= 2) {
            (points.last().timestamp - points[points.size - 2].timestamp)
        } else {
            60_000L
        }
        
        // Detect compression state for projection
        val recentValues = points.takeLast(10).map { it.value }
        val isCompressed = recentValues.average() < 40f && recentValues.maxOrNull()!! - recentValues.minOrNull()!! < 15f
        
        for (i in 1..10) {
            val decayFactor = 1f - (i * 0.08f)
            val projectedRaw = if (isCompressed) {
                // Compression release projection
                points.last().rawValue + (acceleration * i * 1.5f * decayFactor)
            } else {
                // Normal trend projection with decay
                points.last().rawValue + (acceleration * i * decayFactor)
            }
            
            val projectedValue = normalizeVolatilityRawValue(projectedRaw, projectionReferenceValues)
            
            points.add(
                VolatilityPoint(
                    timestamp = lastTimestamp + (timeStep * i),
                    value = projectedValue.coerceIn(0f, 100f),
                    rawValue = projectedRaw,
                    isProjected = true
                )
            )
        }
    }
    
    return points
}

/**
 * Multi-factor volatility scoring system
 * Combines: ATR expansion, body expansion, compression, acceleration, and momentum
 */
private fun calculateMultiFactorVolatility(
    window: List<TimedPrice>,
    currentCandle: TimedPrice,
    prevCandle: TimedPrice,
    allHistory: List<TimedPrice>
): Float {
    if (window.size < 3) return 0f
    
    // Factor 1: True Range Expansion (30% weight)
    val atr = calculateATR(window)
    val avgATR = if (allHistory.size > window.size) {
        calculateATR(allHistory.takeLast(window.size * 2))
    } else atr
    val atrExpansion = if (avgATR > 0) (atr / avgATR).coerceIn(0f, 3f) / 3f else 0f
    val recentRangeWindow = window.takeLast(minOf(5, window.size))
    val recentRange = (recentRangeWindow.maxOf { it.price } - recentRangeWindow.minOf { it.price }).toFloat()
    val rollingRanges = window.windowed(size = minOf(5, window.size), step = 1, partialWindows = false)
        .map { slice -> (slice.maxOf { it.price } - slice.minOf { it.price }).toFloat() }
    val averageRange = rollingRanges.average().toFloat().takeIf { it.isFinite() && it > 0f } ?: recentRange
    val rangeExpansion = if (averageRange > 0f) (recentRange / averageRange).coerceIn(0f, 3f) / 3f else 0f
    
    // Factor 2: Body Expansion (20% weight)
    val currentBody = abs(currentCandle.price - prevCandle.price).toFloat()
    val avgBody = window.zipWithNext { a, b -> abs(b.price - a.price) }.average().toFloat()
    val bodyExpansion = if (avgBody > 0) (currentBody / avgBody).coerceIn(0f, 3f) / 3f else 0f
    
    // Factor 3: Compression Detection (15% weight)
    val rangeWidth = (window.maxOf { it.price } - window.minOf { it.price }).toFloat()
    val avgPrice = window.map { it.price }.average().toFloat()
    val compressionRatio = if (avgPrice > 0) (rangeWidth / avgPrice) else 0f
    val compressionScore = (1f - compressionRatio.coerceIn(0f, 0.1f) / 0.1f) // Inverted: high compression = high score
    
    // Factor 4: Volatility Acceleration (15% weight)
    val recentATR = if (window.size >= 10) calculateATR(window.takeLast(5)) else atr
    val olderATR = if (window.size >= 10) calculateATR(window.take(5)) else atr
    val acceleration = if (olderATR > 0) ((recentATR - olderATR) / olderATR).coerceIn(-1f, 2f) else 0f
    val accelerationScore = ((acceleration + 1f) / 3f).coerceIn(0f, 1f)
    
    // Factor 5: Momentum Imbalance (10% weight)
    val returns = window.zipWithNext { a, b -> 
        if (a.price > 0) ((b.price - a.price) / a.price).toFloat() else 0f
    }
    val momentum = returns.sum() / returns.size.coerceAtLeast(1)
    val momentumScore = abs(momentum).coerceIn(0f, 0.05f) / 0.05f
    
    // Factor 6: Tick Density (10% weight)
    val timeSpan = if (window.size >= 2) {
        (window.last().timestampMillis - window.first().timestampMillis).toFloat()
    } else 1f
    val tickDensity = if (timeSpan > 0) (window.size / (timeSpan / 60000f)).coerceIn(0f, 100f) / 100f else 0f
    val rangeFloor = recentRangeWindow.minOf { it.price }
    val rangeCeiling = recentRangeWindow.maxOf { it.price }
    val sweepRange = (rangeCeiling - rangeFloor).toFloat().coerceAtLeast(0.000001f)
    val upperWickProxy = ((rangeCeiling - maxOf(currentCandle.price, prevCandle.price)) / sweepRange).toFloat().coerceIn(0f, 1f)
    val lowerWickProxy = ((minOf(currentCandle.price, prevCandle.price) - rangeFloor) / sweepRange).toFloat().coerceIn(0f, 1f)
    val closeLocation = ((currentCandle.price - rangeFloor) / sweepRange).toFloat().coerceIn(0f, 1f)
    val rejectionStrength = maxOf(
        lowerWickProxy * closeLocation,
        upperWickProxy * (1f - closeLocation)
    )
    val reversalStrength = if (atr > 0f) (currentBody / atr).coerceIn(0f, 1f) else 0f
    val sweepScore = (rejectionStrength * reversalStrength).coerceIn(0f, 1f)
    
    // Weighted combination
    val volatilityScore = (
        atrExpansion * 0.24f +
        rangeExpansion * 0.16f +
        bodyExpansion * 0.18f +
        compressionScore * 0.14f +
        accelerationScore * 0.12f +
        tickDensity * 0.10f +
        sweepScore * 0.10f +
        momentumScore * 0.06f
    )
    
    return volatilityScore.coerceIn(0f, 1f)
}

/**
 * Calculate Average True Range
 */
private fun calculateATR(prices: List<TimedPrice>): Float {
    if (prices.size < 2) return 0f
    
    val trueRanges = prices.zipWithNext { prev, curr ->
        val high = maxOf(curr.price, prev.price)
        val low = minOf(curr.price, prev.price)
        val range = abs(curr.price - prev.price)
        maxOf(high - low, range).toFloat()
    }
    
    return if (trueRanges.isNotEmpty()) {
        trueRanges.average().toFloat()
    } else 0f
}

private fun normalizeVolatilityRawValue(rawValue: Float, referenceValues: List<Float>): Float {
    if (referenceValues.isEmpty()) return 0f

    val mean = referenceValues.average().toFloat()
    val variance = referenceValues.map { (it - mean).toDouble().pow(2.0) }.average()
    val stdDev = sqrt(variance).toFloat().coerceAtLeast(0.0001f)
    val zScore = (rawValue - mean) / stdDev
    val minValue = referenceValues.minOrNull() ?: rawValue
    val maxValue = referenceValues.maxOrNull() ?: rawValue
    val minMaxScore = if (maxValue > minValue) {
        ((rawValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val percentile = referenceValues.count { it <= rawValue }.toFloat() / referenceValues.size.coerceAtLeast(1).toFloat()
    val adaptiveScore = (0.55f * minMaxScore + 0.45f * percentile).coerceIn(0f, 1f)

    return when {
        zScore < -1.2f -> interpolateRange(4f, 25f, adaptiveScore)
        zScore < -0.4f -> interpolateRange(25f, 45f, adaptiveScore)
        zScore < 0.7f -> interpolateRange(45f, 60f, adaptiveScore)
        zScore < 1.5f -> interpolateRange(60f, 75f, adaptiveScore)
        zScore < 2.5f -> interpolateRange(75f, 90f, adaptiveScore)
        else -> interpolateRange(90f, 100f, adaptiveScore)
    }
}

private fun interpolateRange(start: Float, end: Float, fraction: Float): Float {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return start + ((end - start) * clampedFraction)
}

private fun getTimeframeBucketMillis(timeframe: String): Long {
    return when (timeframe) {
        "1m" -> 60_000L
        "5m" -> 5L * 60_000L
        "15m" -> 15L * 60_000L
        "30m" -> 30L * 60_000L
        "1H" -> 60L * 60_000L
        else -> 60L * 60_000L
    }
}

private fun alignToTimeframeBucket(timestamp: Long, bucketMillis: Long): Long {
    return (timestamp / bucketMillis) * bucketMillis
}

private fun getWindowMillis(timeframe: String): Long {
    return when (timeframe) {
        "1m" -> 60L * 60_000L // 1 hour window
        "5m" -> 5L * 60_000L * 60L // 5 hours window
        "15m" -> 15L * 60_000L * 60L // 15 hours window
        "30m" -> 30L * 60_000L * 60L // 30 hours window
        "1H" -> 60L * 60_000L * 72L // 72 hours window
        else -> 60L * 60_000L * 72L
    }
}

private fun resolveVolatilityHistory(
    pair: ForexPair,
    timeframe: String,
    timedPriceHistory: Map<String, List<TimedPrice>>,
    priceHistory: Map<String, List<Double>>
): List<TimedPrice> {
    val windowMillis = getWindowMillis(timeframe)
    val timed = timedPriceHistory.entries
        .firstOrNull { MarketDataStore.matchesSymbol(it.key, pair.symbol) }
        ?.value
        .orEmpty()
        .filter { it.timestampMillis > 0L && it.price.isFinite() && it.price > 0.0 }
        .sortedBy { it.timestampMillis }
    val timedWindow = timed.takeLast(240)
    val timedCoverage = if (timedWindow.size >= 2) {
        timedWindow.last().timestampMillis - timedWindow.first().timestampMillis
    } else {
        0L
    }
    if (timedWindow.size >= 24 && timedCoverage >= (windowMillis / 3L)) {
        return timedWindow
    }

    val rawValues = priceHistory.entries
        .firstOrNull { MarketDataStore.matchesSymbol(it.key, pair.symbol) }
        ?.value
        .orEmpty()
        .filter { it.isFinite() && it > 0.0 }
        .takeLast(240)
    if (rawValues.size < 6) {
        return timedWindow
    }

    val now = System.currentTimeMillis()
    val stepMillis = (windowMillis / rawValues.size.coerceAtLeast(1)).coerceAtLeast(1L)
    val start = now - (stepMillis * (rawValues.size - 1))
    return rawValues.mapIndexed { index, value ->
        TimedPrice(
            timestampMillis = start + (stepMillis * index),
            price = value
        )
    }
}
