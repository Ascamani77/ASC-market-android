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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.ChartSettings
import com.trading.app.models.ColorPickerState
import com.trading.app.models.StatusLineSettings

@Composable
fun StatusLineSettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onClose: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings.statusLine) }
    var titleDropdownExpanded by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerState?>(null) }

    // Apply changes in real-time
    LaunchedEffect(tempSettings) {
        val updatedSettings = settings.copy(statusLine = tempSettings)
        onUpdate(updatedSettings)
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
                        "Status line",
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
                    Text(
                        "INSTRUMENT",
                        color = Color(0xFF787B86),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    StatusSettingCheckbox("Logo", tempSettings.logo) { tempSettings = tempSettings.copy(logo = it) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = tempSettings.symbol,
                            onCheckedChange = { tempSettings = tempSettings.copy(symbol = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Title", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        
                        Box {
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(36.dp)
                                    .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                                    .clickable { titleDropdownExpanded = true }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(tempSettings.titleMode, color = Color.White, fontSize = 13.sp)
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = titleDropdownExpanded,
                                onDismissRequest = { titleDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF121212)).border(1.dp, Color(0xFF363A45))
                            ) {
                                listOf("Name", "Symbol", "Symbol and name").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = Color.White, fontSize = 14.sp) },
                                        onClick = {
                                            tempSettings = tempSettings.copy(titleMode = option)
                                            titleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    StatusSettingCheckbox("Open market status", tempSettings.openMarketStatus) { tempSettings = tempSettings.copy(openMarketStatus = it) }
                    StatusSettingCheckbox("OHLC", tempSettings.ohlc) { tempSettings = tempSettings.copy(ohlc = it) }
                    StatusSettingCheckbox("Bar change values", tempSettings.barChangeValues) { tempSettings = tempSettings.copy(barChangeValues = it) }
                    StatusSettingCheckbox("Volume", tempSettings.volume) { tempSettings = tempSettings.copy(volume = it) }
                    StatusSettingCheckbox("Last day change values", tempSettings.lastDayChange) { tempSettings = tempSettings.copy(lastDayChange = it) }
                    
                    Text(
                        "INDICATORS",
                        color = Color(0xFF787B86),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    StatusSettingCheckbox("Titles", tempSettings.indicatorTitles) { tempSettings = tempSettings.copy(indicatorTitles = it) }
                    StatusSettingCheckbox("Inputs", tempSettings.indicatorInputs) { tempSettings = tempSettings.copy(indicatorInputs = it) }
                    StatusSettingCheckbox("Values", tempSettings.indicatorValues) { tempSettings = tempSettings.copy(indicatorValues = it) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = tempSettings.indicatorBackground,
                            onCheckedChange = { tempSettings = tempSettings.copy(indicatorBackground = it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
                        )
                        Text("Background", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(try { Color(android.graphics.Color.parseColor(tempSettings.indicatorBackgroundColor)) } catch (e: Exception) { Color.Gray })
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                .clickable { 
                                    colorPickerTarget = ColorPickerState(
                                        title = "Background Color",
                                        initialHex = tempSettings.indicatorBackgroundColor,
                                        onColorSelect = { tempSettings = tempSettings.copy(indicatorBackgroundColor = it) }
                                    )
                                }
                        )

                        Slider(
                            value = tempSettings.indicatorBackgroundOpacity.toFloat(),
                            onValueChange = { tempSettings = tempSettings.copy(indicatorBackgroundOpacity = it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF2962FF),
                                inactiveTrackColor = Color(0xFF2A2E39)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Footer
                Surface(
                    color = Color(0xFF131722),
                    border = BorderStroke(1.dp, Color(0xFF2A2E39))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp, 36.dp)
                                .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color(0xFF434651)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Cancel", color = Color.White, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = onClose,
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
    }

    colorPickerTarget?.let { state ->
        ColorPickerDialog(
            state = state,
            onClose = { colorPickerTarget = null }
        )
    }
}

@Composable
fun StatusSettingCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
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
