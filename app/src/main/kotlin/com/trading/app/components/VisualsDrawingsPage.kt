package com.trading.app.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val visualTools = listOf(
    DrawingToolUiItem(id = "emojis", name = "Emojis", icon = DrawingIcons.Emojis),
    DrawingToolUiItem(id = "stickers", name = "Stickers", icon = DrawingIcons.Stickers),
    DrawingToolUiItem(id = "icons", name = "Icons", icon = DrawingIcons.IconsVisuals)
)

@Composable
fun VisualsDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = visualTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
