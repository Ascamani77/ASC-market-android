package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.AlertsSettings
import com.trading.app.models.ChartSettings
import com.trading.app.models.ColorPickerState

@Composable
fun AlertsSettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onClose: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings.alerts) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerState?>(null) }

    // Apply changes in real-time
    LaunchedEffect(tempSettings) {
        onUpdate(settings.copy(alerts = tempSettings))
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text(
                        "Alerts",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF787B86))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // CHART LINE VISIBILITY
                    SectionHeader("CHART LINE VISIBILITY")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { tempSettings = tempSettings.copy(alertLines = !tempSettings.alertLines) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSettings.alertLines,
                            onCheckedChange = { tempSettings = tempSettings.copy(alertLines = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Alert lines", color = Color.White, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        // Color box that opens picker
                        Box(
                            modifier = Modifier
                                .size(36.dp, 32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(parseAlertColor(tempSettings.alertLinesColor))
                                .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
                                .clickable {
                                    colorPickerTarget = ColorPickerState(
                                        title = "Alert lines color",
                                        initialHex = tempSettings.alertLinesColor,
                                        onAddClick = { /* Handle custom color add if needed */ },
                                        onColorSelect = { tempSettings = tempSettings.copy(alertLinesColor = it) }
                                    )
                                }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { tempSettings = tempSettings.copy(onlyActiveAlerts = !tempSettings.onlyActiveAlerts) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSettings.onlyActiveAlerts,
                            onCheckedChange = { tempSettings = tempSettings.copy(onlyActiveAlerts = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Only active alerts", color = Color.White, fontSize = 14.sp)
                    }

                    // NOTIFICATIONS
                    SectionHeader("NOTIFICATIONS")

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { tempSettings = tempSettings.copy(hideToasts = !tempSettings.hideToasts) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSettings.hideToasts,
                            onCheckedChange = { tempSettings = tempSettings.copy(hideToasts = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Automatically hide toasts", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Outlined.HelpOutline, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(44.dp, 36.dp).border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp)).clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color(0xFF2A2E39)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { 
                                try {
                                    val updatedSettings = settings.copy(alerts = tempSettings)
                                    onUpdate(updatedSettings)
                                    onClose()
                                } catch (e: Exception) {
                                    android.util.Log.e("AlertsSettings", "Failed to apply settings changes", e)
                                    onClose()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Ok", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    colorPickerTarget?.let { state ->
        ColorPickerDialog(
            state = state,
            onClose = { colorPickerTarget = null }
        )
    }
}

private fun parseAlertColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
