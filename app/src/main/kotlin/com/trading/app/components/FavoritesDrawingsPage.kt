package com.trading.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val favoriteDrawingTools = listOf(
    DrawingToolUiItem(id = "horizontal_line", name = "Horizontal Line", icon = Icons.Default.HorizontalRule, isFavorite = true),
    DrawingToolUiItem(id = "trendline", name = "Trend Line", icon = Icons.Default.Timeline),
    DrawingToolUiItem(id = "measure", name = "Measure", icon = Icons.Default.Straighten),
    DrawingToolUiItem(id = "fib_retracement", name = "Fib Retracement", icon = Icons.Default.Notes)
)

@Composable
fun FavoritesDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = favoriteDrawingTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
