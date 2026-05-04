package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UtilityIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    id: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) ComposeColor(0xFF2A2E39) else ComposeColor.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = if (isActive) ComposeColor.White else ComposeColor(0xFF787B86),
            modifier = Modifier.size(32.dp)
        )
        if (id == "alerts") {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(14.dp),
                shape = CircleShape,
                color = ComposeColor(0xFFFF5252),
                border = androidx.compose.foundation.BorderStroke(1.dp, ComposeColor(0xFF1E1E1E))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("16", color = ComposeColor.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
