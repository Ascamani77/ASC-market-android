package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.AISettings
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.*

@Composable
fun AISettingsPanel(
    settings: AISettings = AISettings(),
    onSettingsChanged: (AISettings) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenSettings() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open AI Settings",
                    tint = Emerald500,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI AUTOMATION",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Surface(
                color = Zinc900,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "STANDBY",
                    color = Zinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Auto-Adjust SL/TP
        AutomationItem(
            title = "Auto-Adjust SL/TP",
            description = "AI manages risk levels dynamically",
            checked = settings.autoAdjustSLTP,
            onCheckedChange = { onSettingsChanged(settings.copy(autoAdjustSLTP = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-Exit Market
        AutomationItem(
            title = "Auto-Exit Market",
            description = "Emergency liquidation on bias shift",
            checked = settings.autoExitMarket,
            onCheckedChange = { onSettingsChanged(settings.copy(autoExitMarket = it)) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Risk Threshold
        Text(
            text = "RISK THRESHOLD",
            color = Zinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(Zinc800, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((settings.maxRiskPerTrade.toFloat() / 5f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Emerald500, RoundedCornerShape(2.dp))
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${settings.maxRiskPerTrade.toInt()}%",
                color = Emerald500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AutomationItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = Zinc500, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Emerald500,
                uncheckedThumbColor = Zinc500,
                uncheckedTrackColor = Zinc800
            )
        )
    }
}
