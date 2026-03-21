package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
fun RightToolPanel(
    isCrosshairEnabled: Boolean = true,
    isLocked: Boolean = false,
    isMagnetMode: Boolean = false,
    onCrosshairToggle: (Boolean) -> Unit = {},
    onLockToggle: (Boolean) -> Unit = {},
    onMagnetToggle: (Boolean) -> Unit = {},
    onResetZoom: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(Color.Transparent)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToolIconButton(
            icon = if (isCrosshairEnabled) Icons.Default.FilterCenterFocus else Icons.Outlined.FilterCenterFocus,
            active = isCrosshairEnabled,
            onClick = { onCrosshairToggle(!isCrosshairEnabled) },
            label = "Crosshair"
        )
        
        ToolIconButton(
            icon = Icons.Default.FlashOn,
            active = isMagnetMode,
            onClick = { onMagnetToggle(!isMagnetMode) },
            label = "Magnet"
        )

        ToolIconButton(
            icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            active = isLocked,
            onClick = { onLockToggle(!isLocked) },
            label = "Lock"
        )

        Spacer(modifier = Modifier.weight(1f))

        ToolIconButton(
            icon = Icons.Default.CenterFocusStrong,
            onClick = onResetZoom,
            label = "Reset Zoom"
        )
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit = {},
    label: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .background(
                if (active) AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) AccentBlue else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
