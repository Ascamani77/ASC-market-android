package com.trading.app.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp

@Composable
fun TradingIcon(type: String, tint: ComposeColor) {
    when (type) {
        "trendline" -> Icon(Icons.Default.TrendingUp, null, tint = tint, modifier = Modifier.size(20.dp))
        "fib" -> Icon(Icons.Default.Layers, null, tint = tint, modifier = Modifier.size(20.dp))
        "brush" -> Icon(Icons.Default.Brush, null, tint = tint, modifier = Modifier.size(20.dp))
        "long_position" -> Icon(Icons.Default.ArrowUpward, null, tint = tint, modifier = Modifier.size(20.dp))
        "short_position" -> Icon(Icons.Default.ArrowDownward, null, tint = tint, modifier = Modifier.size(20.dp))
        "text" -> Icon(Icons.Default.TextFields, null, tint = tint, modifier = Modifier.size(20.dp))
        "measure" -> Icon(Icons.Default.Straighten, null, tint = tint, modifier = Modifier.size(20.dp))
        "zoom" -> Icon(Icons.Default.ZoomIn, null, tint = tint, modifier = Modifier.size(20.dp))
        "magnet" -> Icon(Icons.Default.FlashOn, null, tint = tint, modifier = Modifier.size(20.dp))
        "stay" -> Icon(Icons.Default.Lock, null, tint = tint, modifier = Modifier.size(20.dp))
        "hide" -> Icon(Icons.Default.VisibilityOff, null, tint = tint, modifier = Modifier.size(20.dp))
        "remove" -> Icon(Icons.Default.Delete, null, tint = tint, modifier = Modifier.size(20.dp))
        "star" -> Icon(Icons.Default.Star, null, tint = tint, modifier = Modifier.size(20.dp))
        else -> Icon(Icons.Default.Help, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
