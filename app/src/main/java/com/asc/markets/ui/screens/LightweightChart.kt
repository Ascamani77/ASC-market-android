package com.asc.markets.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexDataPoint
import java.util.Locale
import kotlin.math.sin

@Composable
fun LightweightChart(
    symbol: String,
    price: Double,
    modifier: Modifier = Modifier,
    sparkline: List<ForexDataPoint> = emptyList()
) {
    // produce mock sparkline data if none provided; use Float for Canvas math
    val data: List<Float> = if (sparkline.isNotEmpty()) {
        sparkline.map { it.close.toFloat() }
    } else {
        val base = price.takeIf { it > 0.0 } ?: 1.0
        List(40) { i -> (base + sin(i * 0.25) * (base * 0.01) + (i % 3) * 0.005 * base).toFloat() }
    }

    val min = data.minOrNull() ?: 0f
    val max = data.maxOrNull() ?: 1f
    val range = (max - min).let { if (it <= 0f) 1f else it }

    Surface(modifier = modifier) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = String.format(Locale.US, "%.4f", price), fontSize = 13.sp, color = Color(0xFF94A3B8))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.Transparent)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (data.size >= 2) {
                        val w = size.width
                        val h = size.height
                        val sx = w / (data.size - 1)
                        val path = Path()
                        data.forEachIndexed { idx, v ->
                            val x = idx * sx
                            val y = ((v - min) / range)
                            val py = h - (y * h)
                            if (idx == 0) path.moveTo(x, py) else path.lineTo(x, py)
                        }

                        // shadow / area fill (subtle)
                        // draw line
                        drawPath(path = path, color = Color(0xFF60A5FA), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                        // tiny current-price marker
                        val lastX = (data.size - 1) * sx
                        val lastY = h - (((data.last() - min) / range) * h)
                        drawCircle(color = Color(0xFF60A5FA), radius = 3.dp.toPx(), center = Offset(lastX, lastY))
                    } else {
                        // draw flat line when not enough points
                        val y = size.height / 2f
                        drawLine(color = Color(0xFF60A5FA), strokeWidth = 2.dp.toPx(), start = Offset(0f, y), end = Offset(size.width, y), cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}
