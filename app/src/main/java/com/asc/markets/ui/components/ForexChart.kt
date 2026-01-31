package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import androidx.compose.foundation.Canvas

@Composable
fun ForexChart(data: List<Double>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF030303), RoundedCornerShape(12.dp))
            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Watermark Parity
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.04f)) {
            Text("ASC", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("market", color = IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 40.dp)) {
            if (data.isEmpty()) return@Canvas
            
            val width = size.width
            val height = size.height
            val min = data.minOrNull() ?: 0.0
            val max = data.maxOrNull() ?: 1.0
            val range = max - min
            val step = if (data.size > 1) width / (data.size - 1) else 0f

            // Liquidity Zones parity
            val bslY = height - ((max * 0.9995 - min) / range * height).toFloat()
            val sslY = height - ((min * 1.0005 - min) / range * height).toFloat()

            drawRect(
                color = RoseError.copy(alpha = 0.08f),
                topLeft = Offset(0f, bslY - 15f),
                size = Size(width, 30f)
            )
            drawRect(
                color = EmeraldSuccess.copy(alpha = 0.08f),
                topLeft = Offset(0f, sslY - 15f),
                size = Size(width, 30f)
            )

            // Price Trace
            data.forEachIndexed { i, price ->
                if (i > 0) {
                    val startX = (i - 1) * step
                    val startY = height - ((data[i - 1] - min) / range * height).toFloat()
                    val endX = i * step
                    val endY = height - ((price - min) / range * height).toFloat()
                    
                    drawLine(
                        color = Color.White,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
    }
}