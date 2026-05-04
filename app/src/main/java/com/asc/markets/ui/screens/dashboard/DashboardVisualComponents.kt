package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import java.util.Locale
import kotlin.random.Random

// ===== Mini Sparkline (Canvas-based, no WebView) =====
@Composable
fun MiniSparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = EmeraldSuccess,
    fillColor: Color = color.copy(alpha = 0.12f),
    strokeWidth: Float = 2.5f
) {
    if (points.size < 2) {
        Box(modifier = modifier.background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp)))
        return
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = (maxVal - minVal).coerceAtLeast(0.001f)

        fun yAt(index: Int): Float {
            val normalized = ((points[index] - minVal) / range).coerceIn(0f, 1f)
            return h - (normalized * h * 0.82f) - (h * 0.09f)
        }

        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val linePath = Path().apply {
            moveTo(0f, yAt(0))
            for (i in 1 until points.size) {
                val prevX = (i - 1) * stepX
                val currX = i * stepX
                val c1x = prevX + stepX * 0.45f
                val c2x = currX - stepX * 0.45f
                cubicTo(c1x, yAt(i - 1), c2x, yAt(i), currX, yAt(i))
            }
        }

        val fillPath = Path().apply {
            moveTo(0f, h)
            lineTo(0f, yAt(0))
            for (i in 1 until points.size) {
                val prevX = (i - 1) * stepX
                val currX = i * stepX
                val c1x = prevX + stepX * 0.45f
                val c2x = currX - stepX * 0.45f
                cubicTo(c1x, yAt(i - 1), c2x, yAt(i), currX, yAt(i))
            }
            lineTo(w, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // last point dot
        val lastX = w
        val lastY = yAt(points.lastIndex)
        drawCircle(color = color, radius = 3.5f, center = Offset(lastX, lastY))
    }
}

// Generate demo sparkline points for visual previews
fun demoSparkline(count: Int = 24, seed: Int = 42, trendBias: Float = 0f): List<Float> {
    val random = Random(seed)
    val values = MutableList(count) { 0f }
    var current = 0.5f
    repeat(count) { i ->
        val noise = (random.nextFloat() - 0.5f) * 0.14f
        val drift = (random.nextFloat() - 0.5f) * 0.04f + trendBias * 0.01f
        current = (current + noise + drift).coerceIn(0.08f, 0.92f)
        values[i] = current
    }
    return values
}

// ===== Donut / Ring Indicator =====
@Composable
fun DonutIndicator(
    segments: List<Triple<String, Float, Color>>,
    modifier: Modifier = Modifier,
    centerText: String = "",
    centerSubtext: String = "",
    strokeWidth: Float = 18f
) {
    val total = segments.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.001f)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "donut"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension * 0.82f
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            // background ring
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            segments.forEach { (_, value, color) ->
                val sweep = (value / total) * 360f * animatedProgress
                if (sweep > 1f) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                startAngle += sweep
            }
        }

        if (centerText.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (centerSubtext.isNotEmpty()) {
                    Text(centerSubtext, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===== Segmented Status Bar (WAIT / OBSERVE / FOCUS) =====
@Composable
fun SegmentedStatusBar(
    segments: List<Pair<String, Color>>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEachIndexed { i, (label, color) ->
            val isActive = i == activeIndex
            val bg = if (isActive) color.copy(alpha = 0.18f) else Color.Transparent
            val textColor = if (isActive) color else SlateText.copy(alpha = 0.6f)
            Surface(
                color = bg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ===== Compact Radial Gauge (0-100) =====
@Composable
fun CompactGauge(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = EmeraldSuccess,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    label: String = ""
) {
    val clamped = value.coerceIn(0, 100)
    val animated by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "gauge"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 7f, cap = StrokeCap.Round)
            val diameter = this.size.minDimension * 0.78f
            val topLeft = Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // track
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            // fill
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$clamped", color = Color.White, fontSize = (size.value / 3.5f).sp, fontWeight = FontWeight.Black)
            if (label.isNotEmpty()) {
                Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ===== Horizontal Dot Indicator =====
@Composable
fun HorizontalDotIndicator(
    labels: List<String>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = EmeraldSuccess
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { i, label ->
            val isActive = i == activeIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(if (isActive) 10.dp else 6.dp)
                        .background(
                            if (isActive) activeColor else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(999.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    label,
                    color = if (isActive) Color.White else SlateText.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium
                )
            }
        }
    }
}

// ===== Win/Loss Mini Donut =====
@Composable
fun WinLossDonut(
    won: Int,
    lost: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp
) {
    val total = (won + lost).coerceAtLeast(1)
    val winPct = won.toFloat() / total
    val segments = listOf(
        Triple("Won", winPct, EmeraldSuccess),
        Triple("Lost", 1f - winPct, RoseError)
    )
    DonutIndicator(
        segments = segments,
        centerText = "${(winPct * 100).toInt()}%",
        centerSubtext = "WIN RATE",
        modifier = modifier.size(size)
    )
}

// ===== Mini Bar Chart (horizontal bars) =====
@Composable
fun MiniBarChart(
    items: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = IndigoAccent,
    maxValue: Float? = null
) {
    val max = maxValue ?: items.maxOfOrNull { it.second }?.coerceAtLeast(0.01f) ?: 1f
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            val fraction = (value / max).coerceIn(0f, 1f)
            val animated by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "bar"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animated)
                            .background(barColor, RoundedCornerShape(999.dp))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    String.format(Locale.US, "%.0f%%", value * 100f),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}
