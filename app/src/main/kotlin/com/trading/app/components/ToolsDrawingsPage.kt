package com.trading.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val toolsPageTools = listOf(
    DrawingToolUiItem(id = "measure", name = "Measure", icon = DrawingIcons.Measure),
    DrawingToolUiItem(id = "eraser", name = "Eraser", icon = DrawingIcons.Eraser),
    DrawingToolUiItem(id = "keep_drawing", name = "Keep drawing", icon = DrawingIcons.KeepDrawing),
    DrawingToolUiItem(id = "hide_drawings", name = "Hide drawings", icon = DrawingIcons.HideDrawings),
    DrawingToolUiItem(id = "lock_all_drawings", name = "Lock all drawings", icon = DrawingIcons.LockAllDrawings),
    DrawingToolUiItem(id = "weak_magnet", name = "Weak Magnet", icon = DrawingIcons.Magnet),
    DrawingToolUiItem(id = "remove_all_drawings", name = "Remove all drawings", icon = DrawingIcons.RemoveAll),
    DrawingToolUiItem(id = "zoom", name = "Zoom In", icon = DrawingIcons.ZoomIn),
    DrawingToolUiItem(id = "zoom_out", name = "Zoom Out", icon = DrawingIcons.ZoomOut, isEnabled = false)
)

@Composable
fun ToolsDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = toolsPageTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
