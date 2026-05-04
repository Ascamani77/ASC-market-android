package com.trading.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun SidebarToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    toolId: String,
    activeTool: String?,
    onClick: (String) -> Unit,
    hasDropdown: Boolean = false
) {
    val isActive = when (toolId) {
        "trendline" -> listOf("trendline", "ray", "info_line", "extended_line", "trend_angle", "horizontal_line", "horizontal_ray", "vertical_line", "cross_line").contains(activeTool)
        "fib" -> activeTool == "fib"
        "brush" -> activeTool == "brush"
        "long_short" -> listOf("long_position", "short_position", "projection").contains(activeTool)
        else -> activeTool == toolId
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) ComposeColor(0xFF2962FF) else ComposeColor.Transparent)
            .clickable { onClick(toolId) },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                null,
                tint = if (isActive) ComposeColor.White else ComposeColor(0xFF787B86),
                modifier = Modifier.size(24.dp)
            )
        } else {
            TradingIcon(
                type = when (toolId) {
                    "trendline" -> if (listOf("trendline", "ray", "info_line", "extended_line", "trend_angle", "horizontal_line", "horizontal_ray", "vertical_line", "cross_line").contains(activeTool)) activeTool!! else "trendline"
                    "fib" -> "fib"
                    "brush" -> "brush"
                    "long_short" -> if (listOf("long_position", "short_position", "projection").contains(activeTool)) activeTool!! else "long_position"
                    else -> toolId
                },
                tint = if (isActive) ComposeColor.White else ComposeColor(0xFFD1D4DC)
            )
        }

        if (hasDropdown) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val path = Path().apply {
                    moveTo(size.width - 2.dp.toPx(), size.height - 2.dp.toPx())
                    lineTo(size.width - 6.dp.toPx(), size.height - 2.dp.toPx())
                    lineTo(size.width - 2.dp.toPx(), size.height - 6.dp.toPx())
                    close()
                }
                drawPath(path, color = ComposeColor(0xFF787B86))
            }
        }
    }
}
