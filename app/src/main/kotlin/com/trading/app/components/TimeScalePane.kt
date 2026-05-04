package com.trading.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Paint
import android.graphics.Typeface

import androidx.core.graphics.toColorInt

@Composable
fun TimeScalePane(
    timestamps: List<Long>,
    visibleRange: Pair<Long, Long>?,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    val gutterWidth = 60.dp
    val backgroundColor = Color.Black
    val borderColor = Color(0xFF2A2E39)
    val density = LocalDensity.current

    val filteredTimestamps = remember(timestamps, visibleRange) {
        if (visibleRange == null || timestamps.isEmpty()) {
            timestamps
        } else {
            val startIndex = timestamps.indexOfFirst { it >= visibleRange.first }.coerceAtLeast(0)
            val endIndex = timestamps.indexOfLast { it <= visibleRange.second }.coerceAtMost(timestamps.size - 1)
            
            if (startIndex <= endIndex && startIndex < timestamps.size) {
                timestamps.subList(startIndex, (endIndex + 1).coerceAtMost(timestamps.size))
            } else {
                emptyList()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val height = size.height
            val gutterWidthPx = gutterWidth.toPx()
            val chartWidth = totalWidth - gutterWidthPx
            
            // Top border
            // drawLine(borderColor, Offset(0f, 0f), Offset(totalWidth, 0f), strokeWidth = 1.dp.toPx())
            
            // Gutter separator
            // drawLine(borderColor, Offset(chartWidth, 0f), Offset(chartWidth, height), strokeWidth = 1.dp.toPx())

            // Draw "TV" Logo on the left
            val logoPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#2A2E39")
                textSize = with(density) { 14.sp.toPx() }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("17", 12.dp.toPx(), height * 0.65f, logoPaint)

            if (filteredTimestamps.isNotEmpty()) {
                val stepX = if (filteredTimestamps.size > 1) chartWidth / (filteredTimestamps.size - 1) else chartWidth
                
                val paint = Paint().apply {
                    color = "#787B86".toColorInt()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT
                    isAntiAlias = true
                }

                // Determine label density
                val labelWidthPx = 80.dp.toPx()
                val skip = (labelWidthPx / stepX).toInt().coerceAtLeast(filteredTimestamps.size / 6).coerceAtLeast(1)
                
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)

                filteredTimestamps.forEachIndexed { index, timestamp ->
                    if (index % skip == 0) {
                        val x = index * stepX
                        
                        // Don't draw too close to the right gutter or left logo
                        if (x > 40.dp.toPx() && x < chartWidth - 30.dp.toPx()) {
                            val date = Date(timestamp * 1000)
                            val timeStr = sdfTime.format(date)
                            
                            drawContext.canvas.nativeCanvas.drawText(timeStr, x, height * 0.65f, paint)
                            
                            // Tick mark
                            drawLine(borderColor, Offset(x, 0f), Offset(x, 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                        }
                    }
                }
            }
        }

        // Settings Icon in the gutter area
        if (onSettingsClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(gutterWidth, 40.dp)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Chart Settings",
                    tint = Color(0xFFD1D4DC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
