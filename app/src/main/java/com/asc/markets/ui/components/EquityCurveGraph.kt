package com.asc.markets.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.asc.markets.ui.theme.EmeraldSuccess

@Composable
fun EquityCurveGraph(modifier: Modifier = Modifier) {
    val points = listOf(0.2f, 0.35f, 0.3f, 0.55f, 0.5f, 0.75f, 0.8f, 1f)
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val spacing = width / (points.size - 1)
        
        val path = Path().apply {
            moveTo(0f, height * (1 - points[0]))
            points.forEachIndexed { index, value ->
                if (index > 0) {
                    lineTo(index * spacing, height * (1 - value))
                }
            }
        }

        // Fill Gradient Parity
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(EmeraldSuccess.copy(alpha = 0.2f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Line Glow Parity
        drawPath(
            path = path,
            color = EmeraldSuccess,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}