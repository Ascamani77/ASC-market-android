package com.trading.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val gannAndFibonacciTools = listOf(
    DrawingToolUiItem(id = "fib_retracement", name = "Fib Retracement", icon = DrawingIcons.FibRetracement),
    DrawingToolUiItem(id = "trend_based_fib_extension", name = "Trend-Based Fib Ex...", icon = DrawingIcons.TrendBasedFibExtension),
    DrawingToolUiItem(id = "fib_channel", name = "Fib Channel", icon = DrawingIcons.FibChannel),
    DrawingToolUiItem(id = "fib_time_zone", name = "Fib Time Zone", icon = DrawingIcons.FibTimeZone),
    DrawingToolUiItem(id = "fib_speed_resistance_fan", name = "Fib Speed Resistan...", icon = DrawingIcons.FibSpeedResistanceFan),
    DrawingToolUiItem(id = "trend_based_fib_time", name = "Trend-Based Fib Ti...", icon = DrawingIcons.TrendBasedFibTime),
    DrawingToolUiItem(id = "fib_circles", name = "Fib Circles", icon = DrawingIcons.FibCircles),
    DrawingToolUiItem(id = "fib_spiral", name = "Fib Spiral", icon = DrawingIcons.FibSpiral),
    DrawingToolUiItem(id = "fib_speed_resistance_arcs", name = "Fib Speed Resistan...", icon = DrawingIcons.FibSpeedResistanceArcs),
    DrawingToolUiItem(id = "fib_wedge", name = "Fib Wedge", icon = DrawingIcons.FibWedge),
    DrawingToolUiItem(id = "pitchfan", name = "Pitchfan", icon = DrawingIcons.Pitchfan),
    DrawingToolUiItem(id = "gann_box", name = "Gann Box", icon = DrawingIcons.GannBox),
    DrawingToolUiItem(id = "gann_square_fixed", name = "Gann Square Fixed", icon = DrawingIcons.GannSquareFixed),
    DrawingToolUiItem(id = "gann_square", name = "Gann Square", icon = DrawingIcons.GannSquare),
    DrawingToolUiItem(id = "gann_fan", name = "Gann Fan", icon = DrawingIcons.GannFan)
)

@Composable
fun GannAndFibonacciDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = gannAndFibonacciTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
