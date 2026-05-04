package com.trading.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val trendLineTools = listOf(
    DrawingToolUiItem(id = "trendline", name = "Trend Line", icon = DrawingIcons.TrendLine),
    DrawingToolUiItem(id = "ray", name = "Ray", icon = DrawingIcons.Ray),
    DrawingToolUiItem(id = "info_line", name = "Info Line", icon = DrawingIcons.InfoLine),
    DrawingToolUiItem(id = "extended_line", name = "Extended Line", icon = DrawingIcons.ExtendedLine),
    DrawingToolUiItem(id = "trend_angle", name = "Trend Angle", icon = DrawingIcons.TrendAngle),
    DrawingToolUiItem(id = "horizontal_line", name = "Horizontal Line", icon = DrawingIcons.HorizontalLine, isFavorite = true),
    DrawingToolUiItem(id = "horizontal_ray", name = "Horizontal Ray", icon = DrawingIcons.HorizontalRay),
    DrawingToolUiItem(id = "vertical_line", name = "Vertical Line", icon = DrawingIcons.VerticalLine),
    DrawingToolUiItem(id = "cross_line", name = "Cross Line", icon = DrawingIcons.CrossLine),
    DrawingToolUiItem(id = "parallel_channel", name = "Parallel Channel", icon = DrawingIcons.ParallelChannel),
    DrawingToolUiItem(id = "regression_trend", name = "Regression Trend", icon = DrawingIcons.RegressionTrend),
    DrawingToolUiItem(id = "flat_top_bottom", name = "Flat Top/Bottom", icon = DrawingIcons.FlatTopBottom),
    DrawingToolUiItem(id = "disjoint_channel", name = "Disjoint Channel", icon = DrawingIcons.ParallelChannel),
    DrawingToolUiItem(id = "pitchfork", name = "Pitchfork", icon = DrawingIcons.Pitchfork),
    DrawingToolUiItem(id = "schiff_pitchfork", name = "Schiff Pitchfork", icon = DrawingIcons.Pitchfork),
    DrawingToolUiItem(id = "modified_schiff_pitchfork", name = "Modified Schiff Pitchfork", icon = DrawingIcons.Pitchfork),
    DrawingToolUiItem(id = "inside_pitchfork", name = "Inside Pitchfork", icon = DrawingIcons.Pitchfork)
)

@Composable
fun TrendLinesDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = trendLineTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
