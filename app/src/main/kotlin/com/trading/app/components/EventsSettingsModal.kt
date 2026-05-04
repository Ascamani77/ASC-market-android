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
import com.trading.app.models.ChartSettings
import com.trading.app.models.EventsSettings
import com.trading.app.models.ColorPickerState

@Composable
fun EventsSettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onClose: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings.events) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerState?>(null) }

    // Apply changes in real-time
    LaunchedEffect(tempSettings) {
        onUpdate(settings.copy(events = tempSettings))
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
                        "Events",
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
                    // EVENTS
                    SectionHeader("EVENTS")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSettings.ideas,
                            onCheckedChange = { tempSettings = tempSettings.copy(ideas = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Ideas", color = Color.White, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(36.dp)
                                .background(if (tempSettings.ideas) Color.Transparent else Color(0xFF121212), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(tempSettings.ideasMode, color = if (tempSettings.ideas) Color.White else Color(0xFF434651), fontSize = 13.sp)
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.HelpOutline, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
                    }

                    EventsCheckboxRow("Economic events", tempSettings.economicEvents) { tempSettings = tempSettings.copy(economicEvents = it) }
                    
                    if (tempSettings.economicEvents) {
                        EventsCheckboxRow("Only future events", tempSettings.onlyFutureEvents, modifier = Modifier.padding(start = 32.dp)) { 
                            tempSettings = tempSettings.copy(onlyFutureEvents = it) 
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSettings.eventsBreaks,
                            onCheckedChange = { tempSettings = tempSettings.copy(eventsBreaks = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Events breaks", color = Color.White, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .width(80.dp)
                                .height(36.dp)
                                .background(if (tempSettings.eventsBreaks) Color.Transparent else Color(0xFF121212), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp)
                                .clickable(enabled = tempSettings.eventsBreaks) {
                                    colorPickerTarget = ColorPickerState(
                                        title = "Events Breaks Color",
                                        initialHex = tempSettings.eventsBreaksColor,
                                        onColorSelect = { newColor ->
                                            tempSettings = tempSettings.copy(eventsBreaksColor = newColor)
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(safeParseEventsColor(tempSettings.eventsBreaksColor))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("----", color = Color(0xFF787B86), fontSize = 14.sp)
                        }
                    }

                    EventsCheckboxRow("Latest news", tempSettings.latestNews) { tempSettings = tempSettings.copy(latestNews = it) }
                    EventsCheckboxRow("News notification", tempSettings.newsNotification) { tempSettings = tempSettings.copy(newsNotification = it) }

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
                                    onUpdate(settings.copy(events = tempSettings))
                                    onClose()
                                } catch (e: Exception) {
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

private fun safeParseEventsColor(hex: String): Color {
    return try {
        if (hex.startsWith("#")) {
             val colorString = if (hex.length == 7) "#FF" + hex.removePrefix("#") else hex
             Color(android.graphics.Color.parseColor(colorString))
        }
        else if (hex.startsWith("rgba")) {
            val parts = hex.substringAfter("(").substringBefore(")").split(",")
            val r = parts[0].trim().toInt()
            val g = parts[1].trim().toInt()
            val b = parts[2].trim().toInt()
            val a = parts.getOrNull(3)?.trim()?.toFloat() ?: 1f
            Color(r / 255f, g / 255f, b / 255f, a)
        } else Color.Gray
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun EventsCheckboxRow(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
        )
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
