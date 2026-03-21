package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.asc.markets.ui.terminal.theme.*

@Composable
fun LeftToolbar(
    activeTool: String? = null,
    onToolClick: (String?) -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(48.dp) // Adjusted width to match high-fidelity design
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ToolButton(Icons.Default.NearMe, active = activeTool == "cursor", onClick = { onToolClick("cursor") })
            ToolButton(Icons.Default.Timeline, active = activeTool == "trendline", onClick = { onToolClick("trendline") })
            ToolButton(Icons.Default.GridOn, active = activeTool == "fib", onClick = { onToolClick("fib") })
            ToolButton(Icons.Default.Category, active = activeTool == "shapes", onClick = { onToolClick("shapes") })
            ToolButton(Icons.Default.Title, active = activeTool == "text", onClick = { onToolClick("text") })
            ToolButton(Icons.AutoMirrored.Filled.List, active = activeTool == "patterns", onClick = { onToolClick("patterns") })
            ToolButton(Icons.Default.Straighten, active = activeTool == "prediction", onClick = { onToolClick("prediction") })
            ToolButton(Icons.Default.Face, active = activeTool == "icons", onClick = { onToolClick("icons") })
            ToolButton(Icons.Default.SquareFoot, active = activeTool == "measure", onClick = { onToolClick("measure") })
            ToolButton(Icons.Default.ZoomIn, active = activeTool == "zoom", onClick = { onToolClick("zoom") })
            
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.width(32.dp).height(1.dp).background(BorderColor.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(12.dp))
            
            ToolButton(Icons.Default.FlashOn, active = activeTool == "magnet", onClick = { onToolClick("magnet") })
            ToolButton(Icons.Default.Lock, active = activeTool == "lock", onClick = { onToolClick("lock") })
            ToolButton(Icons.Default.VisibilityOff, active = activeTool == "hide", onClick = { onToolClick("hide") })
            ToolButton(Icons.Default.Delete, onClick = { onToolClick("clear_drawings") })
        }
        // Right border line
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(BorderColor))
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(38.dp) // Adjusted size for better hit area and design parity
            .padding(2.dp)
            .background(
                if (active) AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) AccentBlue else TextSecondary,
            modifier = Modifier.size(22.dp) // Icon size matching high-fidelity 디자인
        )
    }
}
