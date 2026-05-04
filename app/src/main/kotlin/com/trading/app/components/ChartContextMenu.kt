package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.trading.app.models.ChartSettings

@Composable
fun ChartContextMenu(
    settings: ChartSettings,
    onToggleHeader: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(220.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color(0xFF434651))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                val isHeaderVisible = settings.canvas.headerVisible
                
                ContextMenuItem(
                    icon = if (isHeaderVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    text = if (isHeaderVisible) "Hide Header" else "Show Header",
                    onClick = {
                        onToggleHeader(!isHeaderVisible)
                        onDismiss()
                    }
                )
                
                Divider(color = Color(0xFF2A2E39), modifier = Modifier.padding(vertical = 4.dp))
                
                ContextMenuItem(
                    icon = Icons.Default.Settings,
                    text = "Settings...",
                    onClick = {
                        onOpenSettings()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFD1D4DC),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
