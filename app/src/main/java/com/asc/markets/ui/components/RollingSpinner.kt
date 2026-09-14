package com.asc.markets.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gentle "rolling" loader used everywhere instead of CircularProgressIndicator:
 * a short 30° arc that advances 30° then holds briefly, stepping around the
 * dial like a calm tumble — no continuous full-speed spin.
 *
 * Signature mirrors CircularProgressIndicator so existing call sites only
 * need a class rename (modifier, color, strokeWidth).
 */
@Composable
fun AscRollingSpinner(
    modifier: Modifier = Modifier.size(40.dp),
    color: Color = Color.White,
    strokeWidth: Dp = 3.dp
) {
    val transition = rememberInfiniteTransition(label = "asc_rolling")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                // 12 steps x 700ms = 8.4s per revolution.
                // Each step: 30° across the first ~300ms, then a ~400ms hold.
                durationMillis = 12 * 700
                var t = 0
                for (i in 0..12) {
                    (i * 30f).toFloat() at t
                    t += 700
                }
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "roll_angle"
    )

    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val d = size.minDimension - stroke
        if (d <= 0f) return@Canvas
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 30f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(d, d),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}