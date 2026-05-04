package com.trading.app.components

import androidx.compose.ui.graphics.vector.ImageVector

data class DrawingToolUiItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val isFavorite: Boolean = false,
    val isEnabled: Boolean = true
)
