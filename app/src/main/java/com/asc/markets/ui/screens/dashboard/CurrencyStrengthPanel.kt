package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketCategory
import com.asc.markets.data.MarketDataStore
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.Instrument
import com.asc.markets.ui.components.InstrumentIcon
import com.asc.markets.ui.components.classifyAsset
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import com.asc.markets.state.AssetContext
import com.asc.markets.state.AssetContextStore
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

enum class MarketCompareDensity { COMPACT, FULL }
private val BullColor = Color(0xFF43D17A)
private val BearColor = Color(0xFFE34C4C)
private val NeutralColor = Color(0xFFB6BBC6)
private val CardBg = Color.White.copy(alpha = 0.035f)
private val CardBorder = com.asc.markets.ui.theme.HairlineBorder
private val HeaderSize = 10.sp
private val ValueSize = 24.sp

@Composable
fun CurrencyStrengthPanel(density: MarketCompareDensity = MarketCompareDensity.FULL) {
    MarketCompareSection(density = density)
}

@Composable
fun MarketCompareSection(density: MarketCompareDensity = MarketCompareDensity.FULL) {
    val assetContext by AssetContextStore.context.collectAsState()
    val allPairs by MarketDataStore.allPairs.collectAsState()
    val priceHistory by MarketDataStore.priceHistory.collectAsState()

    val scopedPairs = allPairs
        .filter { pair -> pairInContext(pair.category, assetContext) }
        .sortedByDescending { abs(it.changePercent) }
    val leaders = scopedPairs.take(8)

    val breadth = rememberBreadth(scopedPairs)
    val buySell = rememberBuySellPressure(scopedPairs)
    val perf = rememberPerformanceRows(leaders)
    val positions = rememberPricePositions(leaders, priceHistory)
    val heatSymbols = leaders.take(6)
    val keyDrivers = rememberKeyDrivers(leaders)
    val pulseScore = rememberPulseScore(leaders)
    val volatilityScore = rememberVolatilityScore(leaders)
    val usdStrength = rememberUsdStrength(leaders)

    val panelTitle = when (assetContext) {
        AssetContext.ALL -> "MARKET COMPARE"
        else -> "MARKET COMPARE • ${assetContext.name}"
    }

    InfoBox(
        minHeight = if (density == MarketCompareDensity.FULL) 440.dp else 280.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(panelTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("Today", color = NeutralColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            // Accumulation Radar (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("ACCUMULATION RADAR (PRE-MOVE)")
                        TimeRangeChips()
                    }
                    Spacer8()
                    leaders.take(6).forEach { pair ->
                        TopMoverRow(pair, priceHistory) // Pass real price history
                    }
                }
            }

            // USD Dispatch Bias (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                val weakest = leaders.minByOrNull { it.changePercent }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. USD Strength Driver
                    StrengthDetailSection(
                        title = "USD DISPATCH BIAS",
                        value = usdStrength,
                        description = if (usdStrength >= 0.2f) "USD Accumulation complete. Bias: Dispatch Long" 
                                      else if (usdStrength <= -0.2f) "USD Liquidity sweep detected. Bias: Dispatch Short"
                                      else "USD in neutral accumulation phase"
                    )

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))

                    // 2. Timing Certainty
                    StrengthDetailSection(
                        title = "TIMING CONVERGENCE",
                        value = pulseScore,
                        description = "Convergence of macro and technical timing windows"
                    )
                }
            }

            // Market Pulse (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                MarketPulseWidget(bulls = pulseScore, bears = 1f - pulseScore)
            }

            // Volatility Dispatch Meter (Inner InfoBox, Transparent)
            InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("VOLATILITY DISPATCH METER")
                        Surface(
                            color = if (volatilityScore > 0.72f) RoseError.copy(alpha = 0.12f) else IndigoAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (volatilityScore > 0.72f) RoseError.copy(alpha = 0.3f) else IndigoAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                if (volatilityScore > 0.72f) "EXPANSION IMMINENT" else "ACCUMULATION",
                                color = if (volatilityScore > 0.72f) RoseError else IndigoAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    DispatchSegmentedBar(volatilityScore)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Readiness: ${(volatilityScore * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("System State: ${if (volatilityScore > 0.72f) "Dispatching" else "Coiling"}", color = SlateText, fontSize = 10.sp)
                    }
                }
            }

            if (density == MarketCompareDensity.FULL) {
                // Institutional Liquidity Gaps (Inner InfoBox, Transparent)
                InfoBox(modifier = Modifier.fillMaxWidth(), containerColor = Color.Transparent, contentPadding = PaddingValues(12.dp)) {
                    Column {
                        SectionHeader("INSTITUTIONAL LIQUIDITY GAPS")
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FILLED", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("PARTIAL", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("OPEN GAP", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        positions.take(5).forEach { row -> 
                            LiquidityGapRow(row) 
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = com.asc.markets.ui.theme.PureBlack,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .background(CardBg)
                .padding(10.dp),
            content = content
        )
    }
}

@Composable
private fun StrengthDetailSection(
    title: String,
    value: Float,
    description: String,
    pair: ForexPair? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            
            // Time range selector mock (dropdown style)
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val color = if (value >= 0) BullColor else BearColor
                    Text("1D", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("⌵", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Asset info (if weakest currency)
        if (pair != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                InstrumentIcon(
                    instrument = Instrument(pair.symbol, pair.name, classifyAsset(pair.symbol)),
                    size = 32
                )
                Column {
                    Text(pair.symbol.take(3), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(pair.name, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                
                // Labels above slider (right-aligned in image for weakest)
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Weak", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("Neutral", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("Strong", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Labels above slider for USD Strength
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("← Weak", color = BearColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Neutral", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Strong →", color = BullColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Slider
        StrengthSlider(value = value, modifier = Modifier.fillMaxWidth().height(10.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Value and Description
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val color = if (value >= 0) BullColor else BearColor
            val arrow = if (value >= 0) "↑" else "↓↓"
            Text(
                text = String.format(Locale.US, "%+.2f %s", value, arrow),
                color = color,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StrengthSlider(value: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val trackH = h * 0.6f
        val centerY = h / 2f
        
        // Track with gradient
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(BearColor, NeutralColor.copy(alpha = 0.5f), BullColor),
                startX = 0f,
                endX = w
            ),
            size = androidx.compose.ui.geometry.Size(w, trackH),
            topLeft = Offset(0f, centerY - trackH / 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f)
        )

        // Middle marker
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(w / 2f, centerY - trackH),
            end = Offset(w / 2f, centerY + trackH),
            strokeWidth = 1.dp.toPx()
        )

        // Thumb position (-1 to 1 maps to 0 to w)
        val thumbX = ((value + 1f) / 2f * w).coerceIn(0f, w)
        
        // Shadow/Glow for thumb
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = (trackH * 1.5f),
            center = Offset(thumbX, centerY)
        )
        
        // White Thumb
        drawCircle(
            color = Color.White,
            radius = trackH * 0.9f,
            center = Offset(thumbX, centerY)
        )
    }
}

@Composable
private fun TopMoverRow(pair: ForexPair, priceHistory: Map<String, List<Double>>) {
    val history = priceHistory[pair.symbol].orEmpty()
    val sparkPoints = if (history.isNotEmpty()) {
        val min = history.minOrNull() ?: 0.0
        val max = history.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        history.map { ((it - min) / range).toFloat() }
    } else {
        emptyList()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        InstrumentIcon(
            instrument = Instrument(
                symbol = pair.symbol,
                name = pair.name,
                type = classifyAsset(pair.symbol)
            ),
            size = 28
        )

        // Symbol
        Text(
            text = pair.symbol,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(90.dp)
        )

        // Change %
        Text(
            text = String.format("%+.2f%%", pair.changePercent),
            color = if (pair.changePercent >= 0) BullColor else BearColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )

        // Real Sparkline
        if (sparkPoints.isNotEmpty()) {
            MiniSparklineStrip(
                points = sparkPoints,
                color = if (pair.changePercent >= 0) BullColor else BearColor
            )
        } else {
            Box(modifier = Modifier.weight(1f).height(26.dp)) // Placeholder
        }
    }
    // Thin separator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
    )
}

@Composable private fun StrengthBarRow(symbol: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(symbol, color = SlateText, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
            Text(String.format("%+.2f", value), color = if (value >= 0f) BullColor else BearColor, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        }
        SegmentedTrack(value = value, positiveColor = BullColor, negativeColor = BearColor, showCenterMarker = true)
        Spacer8()
    }
}

@Composable private fun PerformanceBar(symbol: String, pct: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))) {
            val bar = (abs(normalizeChange(pct)) + 0.05f).coerceIn(0.04f, 1f)
            Box(modifier = Modifier.fillMaxWidth(bar).height(8.dp).background(if (pct >= 0) BullColor else BearColor, RoundedCornerShape(8.dp)))
        }
        Text(
            String.format("  %+.2f%%", pct),
            color = if (pct >= 0) BullColor else BearColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable private fun SplitPressureBar(buy: Float, sell: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(10.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))) {
            Box(modifier = Modifier.fillMaxWidth(max(sell, 0.04f)).height(10.dp).background(BearColor, RoundedCornerShape(10.dp)))
            Box(modifier = Modifier.fillMaxWidth(max(buy, 0.04f)).height(10.dp).background(BullColor, RoundedCornerShape(10.dp)))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(2.dp)
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
            )
        }
        Spacer8()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SELL ${(sell * 100).toInt()}%", color = BearColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("BUY ${(buy * 100).toInt()}%", color = BullColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PositionRow(row: PricePositionRow) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.symbol, color = SlateText, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
            Text("${(row.position * 100).toInt()}%", color = Color.White, fontSize = HeaderSize, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
        }
        SegmentedTrack(value = row.position.coerceIn(0.03f, 1f), positiveColor = Color(0xFF8B7CFF), negativeColor = Color(0xFF8B7CFF), showCenterMarker = true)
        Spacer8()
    }
}

@Composable
private fun DispatchSegmentedBar(value: Float) {
    val clamped = value.coerceIn(0f, 1f)
    val segments = 12
    val activeSegments = (clamped * segments).toInt().coerceAtLeast(1)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(segments) { index ->
            val isActive = index < activeSegments
            val color = when {
                !isActive -> Color.White.copy(alpha = 0.08f)
                clamped > 0.8f -> RoseError
                clamped > 0.6f -> Color(0xFFFFA940)
                else -> IndigoAccent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun LiquidityGapRow(row: PricePositionRow) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(row.symbol, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(if (row.position > 0.8f) "OPEN GAP" else if (row.position > 0.4f) "PARTIAL" else "FILLED", 
                 color = if (row.position > 0.8f) RoseError else if (row.position > 0.4f) Color(0xFFFFA940) else EmeraldSuccess, 
                 fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
            val color = if (row.position > 0.8f) RoseError else if (row.position > 0.4f) Color(0xFFFFA940) else EmeraldSuccess
            Box(
                modifier = Modifier
                    .fillMaxWidth(row.position.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
            // Visual markers for institutional levels
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(2) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.3f)))
                }
            }
        }
    }
}

@Composable private fun Spacer8() = Box(modifier = Modifier.height(8.dp))

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = Color.White, fontSize = HeaderSize, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
}

@Composable
private fun SegmentedTrack(
    value: Float,
    positiveColor: Color,
    negativeColor: Color,
    segments: Int = 20,
    showCenterMarker: Boolean = false
) {
    val clamped = value.coerceIn(-1f, 1f)
    val active = (abs(clamped) * segments).toInt().coerceAtLeast(1)
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(segments) { idx ->
                val enabled = idx < active
                val c = when {
                    !enabled -> Color.White.copy(alpha = 0.10f)
                    clamped >= 0f -> positiveColor
                    else -> negativeColor
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(c, RoundedCornerShape(2.dp))
                )
            }
        }
        if (showCenterMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun MeterScale() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("LOW", color = NeutralColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("MODERATE", color = Color(0xFFF0C14A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("HIGH", color = Color(0xFFFFA940), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("EXTREME", color = BearColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimeRangeChips() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("1D", "1W", "1M", "3M", "1Y").forEachIndexed { index, label ->
            val selected = index == 0
            Box(
                modifier = Modifier
                    .then(
                        if (selected) Modifier.background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MiniSparklineStrip(points: List<Float>, color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1)
        var prev = Offset(0f, size.height * (1f - points.first().coerceIn(0.02f, 0.98f)))
        for (i in 1 until points.size) {
            val next = Offset(i * step, size.height * (1f - points[i].coerceIn(0.02f, 0.98f)))
            drawLine(color = color, start = prev, end = next, strokeWidth = 2.2f)
            prev = next
        }
    }
}

@Composable
fun MarketPulseWidget(bulls: Float, bears: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MARKET PULSE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BULLS ${(bulls * 100).toInt()}%", color = BullColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("BEARS ${(bears * 100).toInt()}%", color = BearColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp) // Increased height from ~6dp to 12dp
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bulls)
                    .background(BullColor, RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(BearColor, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
            )
        }
    }
}

private fun pairInContext(category: MarketCategory, context: AssetContext): Boolean = when (context) {
    AssetContext.ALL -> true
    AssetContext.FOREX -> category == MarketCategory.FOREX
    AssetContext.CRYPTO -> category == MarketCategory.CRYPTO
    AssetContext.COMMODITIES -> category == MarketCategory.COMMODITIES
    AssetContext.INDICES -> category == MarketCategory.INDICES
    AssetContext.STOCKS -> category == MarketCategory.STOCK
    AssetContext.FUTURES -> category == MarketCategory.FUTURES
    AssetContext.BONDS -> category == MarketCategory.BONDS
}

private data class Breadth(val advancing: Int, val neutral: Int, val declining: Int)
private data class BuySell(val buyPressure: Float, val sellPressure: Float)
private data class PricePositionRow(val symbol: String, val position: Float)

private fun rememberBreadth(pairs: List<ForexPair>): Breadth {
    val adv = pairs.count { it.changePercent > 0.03 }
    val dec = pairs.count { it.changePercent < -0.03 }
    return Breadth(adv, (pairs.size - adv - dec).coerceAtLeast(0), dec)
}

private fun rememberBuySellPressure(pairs: List<ForexPair>): BuySell {
    val buy = pairs.filter { it.changePercent > 0.0 }.sumOf { abs(it.changePercent) }
    val sell = pairs.filter { it.changePercent < 0.0 }.sumOf { abs(it.changePercent) }
    val total = (buy + sell).takeIf { it > 0.0 } ?: 1.0
    return BuySell((buy / total).toFloat(), (sell / total).toFloat())
}

private fun rememberPerformanceRows(pairs: List<ForexPair>): List<Pair<String, Double>> {
    return pairs.take(6).map { it.symbol to it.changePercent }
}

private fun rememberPricePositions(
    pairs: List<ForexPair>,
    priceHistory: Map<String, List<Double>>
): List<PricePositionRow> {
    return pairs.map { pair ->
        val hist = priceHistory[pair.symbol].orEmpty()
        if (hist.isEmpty()) return@map PricePositionRow(pair.symbol, 0.5f)
        val low = hist.minOrNull() ?: pair.price
        val high = hist.maxOrNull() ?: pair.price
        val range = (high - low).takeIf { it > 0.0 } ?: 1.0
        PricePositionRow(pair.symbol, ((pair.price - low) / range).toFloat())
    }
}

private fun rememberKeyDrivers(pairs: List<ForexPair>): List<Pair<String, Float>> {
    val avgAbs = pairs.take(6).map { abs(it.changePercent) }.average().toFloat()
    val momentum = (avgAbs / 2.0f).coerceIn(0.05f, 1f)
    val trend = pairs.take(5).count { it.changePercent > 0 }.toFloat() / max(pairs.take(5).size, 1)
    val volatility = pairs.take(6).map { abs(it.change) }.average().toFloat().coerceIn(0f, 1000f) / 1000f
    val volume = (pairs.size.coerceAtMost(12) / 12f).coerceIn(0.1f, 1f)
    return listOf(
        "Trend" to trend,
        "Momentum" to momentum,
        "Volatility" to volatility,
        "Volume" to volume
    )
}

private fun rememberPulseScore(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0f
    val avg = pairs.take(8).map { it.changePercent }.average().toFloat()
    return (avg / 1.5f).coerceIn(-1f, 1f)
}

private fun rememberVolatilityScore(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0.2f
    val avgAbsChange = pairs.take(8).map { abs(it.changePercent) }.average().toFloat()
    return (avgAbsChange / 2.5f).coerceIn(0.08f, 1f)
}

private fun rememberUsdStrength(pairs: List<ForexPair>): Float {
    if (pairs.isEmpty()) return 0f
    val usdPairs = pairs.filter { it.symbol.contains("USD", true) }
    if (usdPairs.isEmpty()) return 0f
    val avg = usdPairs.map { it.changePercent }.average().toFloat()
    return (avg / 1.5f).coerceIn(-1f, 1f)
}

private fun normalizeChange(pct: Double): Float = (pct / 1.5).toFloat().coerceIn(-1f, 1f)

private fun sparklineFromScore(score: Float, count: Int = 20): List<Float> {
    val base = 0.5f + (score * 0.25f)
    return List(count) { idx ->
        (base + (sin(idx * 0.55f) * 0.12f) + (score * idx * 0.005f)).coerceIn(0.04f, 0.96f)
    }
}
