package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.terminal.theme.*

@Composable
fun TimeframeRow(
    currentTimeframe: String = "5m",
    onTimeframeChange: (String) -> Unit = {}
) {
    val timeframes = listOf("1m", "5m", "15m", "30m", "1h", "4h", "D", "W")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(DarkSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        timeframes.forEach { tf ->
            val isSelected = currentTimeframe == tf
            Surface(
                onClick = { onTimeframeChange(tf) },
                color = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
            ) {
                Text(
                    text = tf,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
