package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.terminal.theme.*

@Composable
fun IndicatorRow(
    onIndicatorsClick: () -> Unit = {},
    onReplayClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    onRedoClick: () -> Unit = {},
    onQuickIndicatorToggle: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(DarkSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(
            onClick = onIndicatorsClick,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.Timeline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Indicators", color = Color.White, fontSize = 13.sp)
        }

        LocalVerticalDivider()

        listOf("Vol", "RSI", "EMA").forEach { ind ->
            Surface(
                onClick = { onQuickIndicatorToggle(ind) },
                color = Color.White.copy(alpha = 0.05f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text(ind, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        LocalVerticalDivider()

        TextButton(
            onClick = onReplayClick,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Replay", color = Color.White, fontSize = 13.sp)
        }

        LocalVerticalDivider()

        IconButton(onClick = onUndoClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onRedoClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LocalVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(BorderColor)
    )
}
