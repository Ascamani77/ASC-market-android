package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asc.markets.ui.terminal.models.ChartSettings
import com.asc.markets.ui.terminal.theme.*
import java.util.Locale

@Composable
fun SettingsModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    settings: ChartSettings,
    onSave: (ChartSettings) -> Unit,
    chartType: String = "candles",
    onChartTypeChange: (String) -> Unit = {},
    theme: String = "dark",
    onThemeChange: (String) -> Unit = {},
    onResetDrawings: () -> Unit = {}
) {
    if (!isOpen) return

    var localSettings by remember { mutableStateOf(settings) }
    var localChartType by remember { mutableStateOf(chartType) }
    var localTheme by remember { mutableStateOf(theme) }
    var activeTab by remember { mutableStateOf("Symbol") }

    val tabs = listOf(
        "Symbol", "Status line", "Scales and lines", "Canvas", "Trading", "Alerts", "Events"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            color = Color(0xFF1C1C1C),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                    }
                }
                
                HorizontalDivider(color = BorderColor)

                Row(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                            .background(DarkSurface)
                            .verticalScroll(rememberScrollState())
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0xFF2D2D2D) else Color.Transparent)
                                    .noRippleClickable { activeTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(2.dp)
                                            .height(16.dp)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (activeTab) {
                            "Status line" -> StatusLineTab(localSettings, onUpdate = { localSettings = it })
                            "Canvas" -> CanvasTab(
                                theme = localTheme,
                                onThemeUpdate = { localTheme = it }
                            )
                            else -> Text("Tab $activeTab content coming soon", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131722))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onResetDrawings) {
                        Text("Reset Drawings", color = TradingRed, fontSize = 12.sp)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onClose) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { 
                                onSave(localSettings)
                                onChartTypeChange(localChartType)
                                onThemeChange(localTheme)
                                onClose() 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text("Ok", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasTab(
    theme: String,
    onThemeUpdate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Theme", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("dark", "light").forEach { t ->
                val isSelected = theme == t
                Surface(
                    onClick = { onThemeUpdate(t) },
                    color = if (isSelected) AccentBlue else Color(0xFF2D2D2D),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = t.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatusLineTab(settings: ChartSettings, onUpdate: (ChartSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingToggle("Logo", settings.statusLine.showTitle) {
            onUpdate(settings.copy(statusLine = settings.statusLine.copy(showTitle = it)))
        }
        SettingToggle("OHLC", settings.statusLine.showOhlc) {
            onUpdate(settings.copy(statusLine = settings.statusLine.copy(showOhlc = it)))
        }
    }
}

@Composable
fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
