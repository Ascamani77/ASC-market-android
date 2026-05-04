package com.trading.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getStyleIcon(style: String): ImageVector {
    return when (style) {
        "bars" -> Icons.Default.Reorder
        "candles", "hollow_candles", "volume_candles", "heikin_ashi" -> Icons.Default.BarChart
        "line", "line_markers", "kagi" -> Icons.Default.ShowChart
        "area", "hlc_area" -> Icons.Default.AreaChart
        "step_line" -> Icons.Default.StackedLineChart
        "baseline" -> Icons.Default.HorizontalRule
        "columns" -> Icons.Default.BarChart
        "high_low" -> Icons.Default.VerticalAlignBottom
        "renko", "tpo" -> Icons.Default.GridView
        "line_break", "volume_footprint" -> Icons.Default.FormatAlignLeft
        "point_figure" -> Icons.Default.Close
        "range" -> Icons.Default.Height
        "svp" -> Icons.Default.AlignHorizontalLeft
        else -> Icons.Default.BarChart
    }
}
