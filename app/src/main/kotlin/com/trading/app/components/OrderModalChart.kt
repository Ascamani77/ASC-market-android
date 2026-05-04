package com.trading.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.OHLCData
import java.util.Locale

@Composable
fun PendingOrderLineChartPanel(
    symbol: String,
    candles: List<OHLCData>,
    lineColor: Color,
    marketPrice: Float?,
    entryPreview: Float?,
    stopLimitPreview: Float?,
    tpPreview: Float?,
    slPreview: Float?,
    isEntryValid: Boolean,
    modifier: Modifier = Modifier
) {
    val closes = remember(candles) {
        candles.takeLast(240).map { it.close }
    }
    val lastPrice = closes.lastOrNull()
    val plottedValues = remember(closes, marketPrice, entryPreview, stopLimitPreview, tpPreview, slPreview) {
        buildList {
            addAll(closes)
            marketPrice?.takeIf { it > 0f }?.let { add(it) }
            entryPreview?.takeIf { it > 0f }?.let { add(it) }
            stopLimitPreview?.takeIf { it > 0f }?.let { add(it) }
            tpPreview?.takeIf { it > 0f }?.let { add(it) }
            slPreview?.takeIf { it > 0f }?.let { add(it) }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF11151E))
            .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp))
    ) {
        if (closes.size < 2) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Waiting for line chart...",
                    color = Color(0xFF787B86),
                    fontSize = 13.sp
                )
            }
            return
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 40.dp, bottom = 12.dp)
        ) {
            val minPrice = plottedValues.minOrNull() ?: return@Canvas
            val maxPrice = plottedValues.maxOrNull() ?: return@Canvas
            val buffer = ((maxPrice - minPrice) * 0.08f).coerceAtLeast(0.0001f)
            val scaledMin = minPrice - buffer
            val scaledMax = maxPrice + buffer
            val range = (scaledMax - scaledMin).coerceAtLeast(0.000001f)
            val pointCount = closes.size
            val stepX = if (pointCount > 1) size.width / (pointCount - 1) else size.width
            val dashedStroke = 1.dp.toPx()

            fun toY(price: Float): Float {
                val normalized = (price - scaledMin) / range
                return size.height - (normalized * size.height)
            }

            for (gridIndex in 1..3) {
                val y = size.height * (gridIndex / 4f)
                drawLine(
                    color = Color(0xFF2A2E39),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            closes.forEachIndexed { index, close ->
                val x = index * stepX
                val y = toY(close)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            fun drawSolidLevel(price: Float, color: Color) {
                val y = toY(price)
                drawLine(
                    color = color,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.4.dp.toPx()
                )
                drawCircle(
                    color = color,
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width, y)
                )
            }

            fun drawDashedLevel(price: Float, color: Color) {
                val y = toY(price)
                val dash = 8.dp.toPx()
                val gap = 5.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    val end = (x + dash).coerceAtMost(size.width)
                    drawLine(
                        color = color,
                        start = Offset(x, y),
                        end = Offset(end, y),
                        strokeWidth = dashedStroke
                    )
                    x += dash + gap
                }
            }

            marketPrice?.takeIf { it > 0f }?.let {
                drawDashedLevel(it, Color(0xFFB0BEC5))
            }
            entryPreview?.takeIf { it > 0f }?.let {
                drawSolidLevel(it, if (isEntryValid) Color(0xFFFFA726) else Color(0xFFF23645))
            }
            stopLimitPreview?.takeIf { it > 0f }?.let {
                drawSolidLevel(it, Color(0xFFAB47BC))
            }
            tpPreview?.takeIf { it > 0f }?.let {
                drawSolidLevel(it, Color(0xFF26A69A))
            }
            slPreview?.takeIf { it > 0f }?.let {
                drawSolidLevel(it, Color(0xFFF4511E))
            }

            val lastY = toY(closes.last())
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(size.width, lastY)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$symbol - Line",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(horizontalAlignment = Alignment.End) {
                if (lastPrice != null) {
                    Text(
                        text = "Latest: ${formatPriceForOrderChart(lastPrice, symbol)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                entryPreview?.takeIf { it > 0f }?.let {
                    Text(
                        text = "Entry: ${formatPriceForOrderChart(it, symbol)}",
                        color = if (isEntryValid) Color(0xFFFFA726) else Color(0xFFF23645),
                        fontSize = 11.sp
                    )
                }
                stopLimitPreview?.takeIf { it > 0f }?.let {
                    Text(
                        text = "Stop-Limit: ${formatPriceForOrderChart(it, symbol)}",
                        color = Color(0xFFAB47BC),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatPriceForOrderChart(price: Float, symbol: String): String {
    val uppercaseSymbol = symbol.uppercase(Locale.US)
    val isBitcoin = uppercaseSymbol.contains("BTC")
    val isForex = uppercaseSymbol.length == 6 || uppercaseSymbol.contains("/")
    val pattern = when {
        isBitcoin -> "%,.0f"
        isForex -> "%,.5f"
        else -> "%,.2f"
    }
    return String.format(Locale.US, pattern, price)
}
