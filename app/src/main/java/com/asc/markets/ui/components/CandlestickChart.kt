package com.asc.markets.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.RoseError

@Composable
fun CandlestickChart(data: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (data.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val minPrice = data.minOrNull() ?: 0.0
        val maxPrice = data.maxOrNull() ?: 1.0
        val range = maxPrice - minPrice
        val step = width / (data.size - 1)

        // Draw Liquidity Bands (Parity with FullLightweightChart.tsx)
        val bslPrice = maxPrice * 0.999f
        val sslPrice = minPrice * 1.001f
        
        val bslY = height - ((bslPrice - minPrice) / range * height).toFloat()
        val sslY = height - ((sslPrice - minPrice) / range * height).toFloat()

        // Buy Side Liquidity (Red Band)
        drawRect(
            color = RoseError.copy(alpha = 0.1f),
            topLeft = Offset(0f, bslY - 20f),
            size = Size(width, 40f)
        )

        // Sell Side Liquidity (Green Band)
        drawRect(
            color = EmeraldSuccess.copy(alpha = 0.1f),
            topLeft = Offset(0f, sslY - 20f),
            size = Size(width, 40f)
        )

        // Price Line
        data.forEachIndexed { index, price ->
            if (index > 0) {
                val startX = (index - 1) * step
                val startY = height - ((data[index - 1] - minPrice) / range * height).toFloat()
                val endX = index * step
                val endY = height - ((price - minPrice) / range * height).toFloat()
                
                drawLine(
                    color = Color.White,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}