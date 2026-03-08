@file:OptIn(ExperimentalLayoutApi::class)

package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asc.markets.ui.screens.tradeDashboard.model.AISettings
import com.asc.markets.ui.theme.*

@Composable
fun AISettingsDialog(
    isOpen: Boolean = false,
    onClose: () -> Unit = {},
    settings: AISettings = AISettings(),
    onSettingsChanged: (AISettings) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isOpen) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
                color = Color(0xFF0A0A0A)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main Header with Close Icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI AUTOMATION",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Header Section: EXECUTION ZONES
                        SettingsSectionHeader(icon = Icons.Default.Adjust, title = "EXECUTION ZONES")
                        
                        var selectedZone by remember { mutableStateOf("TREND FOLLOWING") }
                        
                        ExecutionZoneItem(
                            title = "TREND FOLLOWING",
                            description = "AI acts on strong momentum",
                            isSelected = selectedZone == "TREND FOLLOWING",
                            onClick = { selectedZone = "TREND FOLLOWING" }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ExecutionZoneItem(
                            title = "COUNTER-TREND",
                            description = "AI acts on overextended levels",
                            isSelected = selectedZone == "COUNTER-TREND",
                            onClick = { selectedZone = "COUNTER-TREND" }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ExecutionZoneItem(
                            title = "NEWS TRADING",
                            description = "AI acts on high-impact events",
                            isSelected = selectedZone == "NEWS TRADING",
                            onClick = { selectedZone = "NEWS TRADING" }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Section: AUTHORIZED ASSETS
                        SettingsSectionHeader(icon = Icons.Default.Language, title = "AUTHORIZED ASSETS")
                        
                        val assets = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "BTCUSD", "ETHUSD", "US30", "NAS100")
                        var selectedAssets by remember { mutableStateOf(setOf("EURUSD", "GBPUSD", "XAUUSD")) }
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            assets.forEach { asset ->
                                AssetChip(
                                    text = asset,
                                    isSelected = selectedAssets.contains(asset),
                                    onClick = {
                                        selectedAssets = if (selectedAssets.contains(asset)) {
                                            selectedAssets - asset
                                        } else {
                                            selectedAssets + asset
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Section: OPERATIONAL WINDOW
                        SettingsSectionHeader(icon = Icons.Default.AccessTime, title = "OPERATIONAL WINDOW")
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TimeInputBox(label = "SESSION START", time = "08:00 AM", modifier = Modifier.weight(1f))
                            TimeInputBox(label = "SESSION END", time = "08:00 PM", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Section: RISK GUARDRAILS
                        SettingsSectionHeader(icon = Icons.Default.MonitorHeart, title = "RISK GUARDRAILS", iconColor = Color(0xFFF43F5E))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MAX DAILY LOSS",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-$1000",
                                color = Color(0xFFF43F5E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        var sliderValue by remember { mutableStateOf(0.3f) }
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF43F5E),
                                activeTrackColor = Color(0xFFF43F5E),
                                inactiveTrackColor = Color(0xFF1A1A1A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Deploy Button
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "DEPLOY PARAMETERS",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String, iconColor: Color = Color(0xFF10B981)) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ExecutionZoneItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF10B981).copy(alpha = 0.05f) else Color(0xFF0F0F0F))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = if (isSelected) Color(0xFF10B981) else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1A1A1A))
            )
        }
    }
}

@Composable
private fun AssetChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF10B981) else Color(0xFF0F0F0F))
            .border(1.dp, if (isSelected) Color(0xFF10B981) else Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TimeInputBox(label: String, time: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F0F0F))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = time,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
