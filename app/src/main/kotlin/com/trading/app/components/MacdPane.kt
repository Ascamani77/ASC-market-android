package com.trading.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun MacdPane(
    macdValues: List<Float?>,
    signalValues: List<Float?>,
    histogramValues: List<Float?>,
    timestamps: List<Long>,
    visibleRange: Pair<Long, Long>?,
    modifier: Modifier = Modifier,
    fastPeriod: Int = 12,
    slowPeriod: Int = 26,
    signalPeriod: Int = 9
) {
    val gutterWidth = 60.dp
    val backgroundColor = Color.Black
    val borderColor = Color(0xFF2A2E39)

    // Filter data based on visible range
    val (filteredMacd, filteredSignal, filteredHist) = remember(macdValues, signalValues, histogramValues, timestamps, visibleRange) {
        if (visibleRange == null || timestamps.isEmpty()) {
            Triple(macdValues, signalValues, histogramValues)
        } else {
            val startIndex = timestamps.indexOfFirst { it >= visibleRange.first }.coerceAtLeast(0)
            val endIndex = timestamps.indexOfLast { it <= visibleRange.second }.coerceAtMost(timestamps.size - 1)
            
            if (startIndex <= endIndex && startIndex < macdValues.size) {
                val endLimit = (endIndex + 1).coerceAtMost(macdValues.size)
                Triple(
                    macdValues.subList(startIndex, endLimit),
                    if (signalValues.size >= endLimit) signalValues.subList(startIndex, endLimit) else emptyList(),
                    if (histogramValues.size >= endLimit) histogramValues.subList(startIndex, endLimit) else emptyList()
                )
            } else {
                Triple(emptyList<Float?>(), emptyList<Float?>(), emptyList<Float?>())
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(0.5.dp, borderColor)
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val gutterWidthPx = with(density) { gutterWidth.toPx() }
        val chartWidth = totalWidth - gutterWidthPx
        val height = constraints.maxHeight.toFloat()
        val scopeMaxHeight = maxHeight

        Row(modifier = Modifier.fillMaxSize()) {
            // Main Chart Area
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Find absolute max for scaling (to center zero line)
                    val allVisibleValues = (filteredMacd + filteredSignal + filteredHist).filterNotNull()
                    val maxAbs = if (allVisibleValues.isNotEmpty()) {
                        allVisibleValues.maxOf { kotlin.math.abs(it) } * 1.1f
                    } else 1f

                    val zeroY = height / 2f
                    val scaleY = (height / 2f) / maxAbs

                    // Zero line
                    drawLine(borderColor, Offset(0f, zeroY), Offset(chartWidth, zeroY), strokeWidth = 1f)

                    val stepX = if (filteredMacd.size > 1) chartWidth / (filteredMacd.size - 1) else chartWidth

                    // Draw Histogram
                    filteredHist.forEachIndexed { index, value ->
                        if (value != null) {
                            val x = index * stepX
                            val hHeight = value * scaleY
                            val barWidth = (stepX * 0.8f).coerceAtLeast(1f)
                            
                            val color = if (value >= 0) {
                                val prevValue = if (index > 0) filteredHist[index - 1] else null
                                if (prevValue != null && value >= prevValue) Color(0xFF26A69A) else Color(0xFF26A69A).copy(alpha = 0.5f)
                            } else {
                                val prevValue = if (index > 0) filteredHist[index - 1] else null
                                if (prevValue != null && value <= prevValue) Color(0xFFEF5350) else Color(0xFFEF5350).copy(alpha = 0.5f)
                            }
                            
                            drawRect(
                                color = color,
                                topLeft = Offset(x - barWidth / 2f, zeroY - (if (value >= 0) hHeight else 0f)),
                                size = Size(barWidth, kotlin.math.abs(hHeight))
                            )
                        }
                    }

                    // Draw MACD Line (Blue)
                    if (filteredMacd.isNotEmpty()) {
                        val macdPath = Path()
                        var firstPoint = true
                        filteredMacd.forEachIndexed { index, value ->
                            if (value != null) {
                                val x = index * stepX
                                val y = zeroY - (value * scaleY)
                                if (firstPoint) {
                                    macdPath.moveTo(x, y)
                                    firstPoint = false
                                } else {
                                    macdPath.lineTo(x, y)
                                }
                            }
                        }
                        drawPath(
                            path = macdPath,
                            color = Color(0xFF2962FF),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Draw Signal Line (Orange)
                    if (filteredSignal.isNotEmpty()) {
                        val signalPath = Path()
                        var firstPoint = true
                        filteredSignal.forEachIndexed { index, value ->
                            if (value != null) {
                                val x = index * stepX
                                val y = zeroY - (value * scaleY)
                                if (firstPoint) {
                                    signalPath.moveTo(x, y)
                                    firstPoint = false
                                } else {
                                    signalPath.lineTo(x, y)
                                }
                            }
                        }
                        drawPath(
                            path = signalPath,
                            color = Color(0xFFFF9800),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // Legends
                val latestMacd = macdValues.lastOrNull()
                val latestSignal = signalValues.lastOrNull()
                val latestHist = histogramValues.lastOrNull()

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MACD $fastPeriod $slowPeriod close $signalPeriod", color = Color(0xFFD1D4DC), fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (latestMacd != null) {
                        Text(String.format(Locale.US, "%.2f", latestMacd), color = Color(0xFF2962FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (latestSignal != null) {
                        Text(String.format(Locale.US, "%.2f", latestSignal), color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (latestHist != null) {
                        Text(String.format(Locale.US, "%.2f", latestHist), color = if (latestHist >= 0) Color(0xFF26A69A) else Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Right Gutter
            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(backgroundColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(borderColor)
                        .align(Alignment.CenterStart)
                )

                val allVisibleValues = (filteredMacd + filteredSignal + filteredHist).filterNotNull()
                val maxAbs = if (allVisibleValues.isNotEmpty()) {
                    allVisibleValues.maxOf { kotlin.math.abs(it) } * 1.1f
                } else 1f
                
                val latestMacd = macdValues.lastOrNull()
                val latestSignal = signalValues.lastOrNull()

                // Y-Axis Labels (Top and Bottom only for simplicity)
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelStyle = Color(0xFFD1D4DC).copy(alpha = 0.5f)
                    Text(String.format(Locale.US, "%.1f", maxAbs), color = labelStyle, fontSize = 10.sp)
                    Text("0.0", color = labelStyle, fontSize = 10.sp)
                    Text(String.format(Locale.US, "%.1f", -maxAbs), color = labelStyle, fontSize = 10.sp)
                }

                if (latestMacd != null) {
                    val yMacd = 0.5f - (latestMacd / (2 * maxAbs))
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = scopeMaxHeight * yMacd - 8.dp)
                            .background(Color(0xFF2962FF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(String.format(Locale.US, "%.1f", latestMacd), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (latestSignal != null) {
                    val ySignal = 0.5f - (latestSignal / (2 * maxAbs))
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = scopeMaxHeight * ySignal - 8.dp)
                            .background(Color(0xFFFF9800))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(String.format(Locale.US, "%.1f", latestSignal), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(borderColor)
                .align(Alignment.TopCenter)
        )
    }
}
