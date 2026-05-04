package com.trading.app.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectingToServerOverlay(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF131722),
    onRetryBridge: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Ghost Candlestick Chart Skeleton
        GhostChartSkeleton(
            modifier = Modifier.fillMaxSize(),
            color = Color.White.copy(alpha = 0.05f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF2962FF),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connecting to Server...",
                color = Color.White,
                fontSize = 17.6.sp,
                fontWeight = FontWeight.Medium
            )
            if (onRetryBridge != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRetryBridge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2962FF),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Text(text = "Retry Bridge")
                }
            }
        }
    }
}

@Composable
private fun GhostChartSkeleton(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_chart")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val candleCount = 20
        val candleWidth = width / candleCount
        val spacing = candleWidth * 0.2f
        val bodyWidth = candleWidth - spacing

        // Simple deterministic pseudo-random heights for the "ghost" candles
        val heights = listOf(
            0.4f, 0.45f, 0.35f, 0.5f, 0.55f, 0.48f, 0.6f, 0.65f, 0.58f, 0.7f,
            0.65f, 0.6f, 0.55f, 0.5f, 0.45f, 0.4f, 0.35f, 0.3f, 0.25f, 0.2f
        )

        for (i in 0 until candleCount) {
            val x = i * candleWidth + spacing / 2
            val hFactor = heights[i % heights.size]
            val candleHeight = height * 0.2f
            val y = height * hFactor

            // Draw wick
            drawLine(
                color = color.copy(alpha = color.alpha * alpha),
                start = Offset(x + bodyWidth / 2, y - candleHeight * 0.5f),
                end = Offset(x + bodyWidth / 2, y + candleHeight * 1.5f),
                strokeWidth = 1.dp.toPx()
            )

            // Draw body
            drawRect(
                color = color.copy(alpha = color.alpha * alpha),
                topLeft = Offset(x, y),
                size = Size(bodyWidth, candleHeight)
            )
        }
    }
}
