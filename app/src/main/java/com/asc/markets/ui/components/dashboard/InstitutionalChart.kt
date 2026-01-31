package com.asc.markets.ui.components.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun InstitutionalChart(symbol: String, price: Double, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(450.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 5 horizontal grid lines at 5% opacity
            for (i in 0..4) {
                val y = h * (i / 4f)
                drawLine(color = Color.White.copy(alpha = 0.05f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1f)
            }

            // Mock price series (random walk centered on price)
            val points = mutableListOf<Double>()
            var p = price
            repeat(200) {
                p += kotlin.random.Random.nextDouble(-0.0015, 0.0015)
                points.add(p)
            }
            val maxPrice = points.maxOrNull() ?: price
            val minPrice = points.minOrNull() ?: price

            // Liquidity bands (BSL / SSL)
            val liquidityTop = maxPrice - (maxPrice - minPrice) * 0.12
            val liquidityBottom = minPrice + (maxPrice - minPrice) * 0.12
            val yOfPrice: (Double) -> Float = { v -> ((maxPrice - v) / (maxPrice - minPrice) * h).toFloat() }

            val topRectTop = yOfPrice(maxPrice)
            val topRectBottom = yOfPrice(liquidityTop)
            drawRect(color = Color(0xFFF43F5E).copy(alpha = 0.10f), topLeft = androidx.compose.ui.geometry.Offset(0f, topRectTop), size = androidx.compose.ui.geometry.Size(w, kotlin.math.max(2f, topRectBottom - topRectTop)))

            val botRectTop = yOfPrice(liquidityBottom)
            val botRectBottom = yOfPrice(minPrice)
            drawRect(color = Color(0xFF10B981).copy(alpha = 0.10f), topLeft = androidx.compose.ui.geometry.Offset(0f, botRectTop), size = androidx.compose.ui.geometry.Size(w, kotlin.math.max(2f, botRectBottom - botRectTop)))

            // Price trace
            val step = w / (points.size - 1)
            val path = Path()
            points.forEachIndexed { idx, v ->
                val x = idx * step
                val y = yOfPrice(v)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Color.White, style = Stroke(width = 2.5f))

            // Watermark: ASC MARKET TERMINAL
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    alpha = (255 * 0.04).toInt()
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = 48f
                    isFakeBoldText = true
                }
                canvas.nativeCanvas.drawText("ASC MARKET TERMINAL", 12f, h / 2f, paint)
            }

            // Right-aligned price scale (8 labels, 5-decimal, monospace)
            val labelCount = 8
            for (i in 0 until labelCount) {
                val ratio = i / (labelCount - 1f)
                val v = maxPrice - (maxPrice - minPrice) * ratio
                val y = yOfPrice(v)
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.LTGRAY
                        textSize = 12f
                        typeface = android.graphics.Typeface.MONOSPACE
                        alpha = (255 * 0.7).toInt()
                    }
                    canvas.nativeCanvas.drawText(String.format("%.5f", v), w - 6f - paint.measureText("000.00000"), y - 4f, paint)
                }
            }
        }
    }
}
