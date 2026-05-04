package com.trading.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val annotationTools = listOf(
    DrawingToolUiItem(id = "text", name = "Text", icon = Icons.Default.TextFields),
    DrawingToolUiItem(id = "note", name = "Note", icon = Icons.Default.Notes),
    DrawingToolUiItem(id = "callout", name = "Callout", icon = Icons.Outlined.Chat),
    DrawingToolUiItem(id = "comment", name = "Comment", icon = Icons.Default.FormatListBulleted),
    DrawingToolUiItem(id = "balloon", name = "Balloon", icon = Icons.Default.NotificationsNone),
    DrawingToolUiItem(id = "info_label", name = "Info Label", icon = Icons.Default.Info)
)

@Composable
fun AnnotationDrawingsPage(
    searchQuery: String,
    onToolSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DrawingsToolGrid(
        items = annotationTools,
        searchQuery = searchQuery,
        onToolSelect = onToolSelect,
        modifier = modifier
    )
}
