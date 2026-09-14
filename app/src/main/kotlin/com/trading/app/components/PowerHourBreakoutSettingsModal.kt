package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.indicators.FiboLevelInput
import com.trading.app.indicators.PowerHourBreakoutSettings

private val PB_TV_BG = Color(0xFF000000)
private val PB_TV_DIVIDER = Color(0xFF2A2E39)
private val PB_TV_FIELD_BORDER = Color(0xFF363A45)
private val PB_TV_TEXT_PRIMARY = Color.White
private val PB_TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val PB_TV_SECTION = Color(0xFF868993)

private val PB_PALETTE = listOf(
    "#089981", "#f23645", "#787b86", "#2157f3", "#f44336", "#81c784",
    "#4caf50", "#009688", "#64b5f6", "#2962ff", "#9c27b0", "#e91e63",
    "#ff5d00", "#ff9800", "#c0c0c0", "#ffffff"
)

@Composable
fun PowerHourBreakoutSettingsModal(
    settings: PowerHourBreakoutSettings,
    onChange: (PowerHourBreakoutSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PB_TV_BG) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LuxAlgo - Power Hour Breakout", color = PB_TV_TEXT_PRIMARY, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = PB_TV_TEXT_PRIMARY)
                    }
                }
                HorizontalDivider(color = PB_TV_DIVIDER, thickness = 1.dp)
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Column {
                        PbSection("POWER HOUR")
                        PbFieldRow("Display Last X Power Hours", settings.powerHoursLength.toString()) {
                            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(settings.copy(powerHoursLength = v.coerceIn(1, 365))) }
                        }
                        PbCheckboxRow("Display All", settings.displayAll) { onChange(settings.copy(displayAll = it)) }
                        PbFieldRow("Power Hour (NY Time)", settings.session) {
                            if (it.matches(Regex("\\d{4}-\\d{4}"))) onChange(settings.copy(session = it))
                        }

                        PbSection("BREAKOUTS")
                        PbCheckboxRow("Show Breakouts", settings.showBreakouts) { onChange(settings.copy(showBreakouts = it)) }
                        PbColorRow("Bullish Breakout", settings.bullBreakColorHex) { hex -> onChange(settings.copy(bullBreakColorHex = hex)) }
                        PbColorRow("Bearish Breakout", settings.bearBreakColorHex) { hex -> onChange(settings.copy(bearBreakColorHex = hex)) }

                        PbSection("EXTENSIONS")
                        PbCheckboxRow("Top Extension %", settings.topExtension) { onChange(settings.copy(topExtension = it)) }
                        PbFieldRow("  Top Multiplier %", settings.topMultiplierPct.toString()) {
                            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(settings.copy(topMultiplierPct = v.coerceIn(1, 5000))) }
                        }
                        PbCheckboxRow("Bottom Extension %", settings.bottomExtension) { onChange(settings.copy(bottomExtension = it)) }
                        PbFieldRow("  Bottom Multiplier %", settings.bottomMultiplierPct.toString()) {
                            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(settings.copy(bottomMultiplierPct = v.coerceIn(1, 5000))) }
                        }

                        PbSection("FIBONACCI LEVELS")
                        PbCheckboxRow("Display Fibonacci", settings.showFibonacci) { onChange(settings.copy(showFibonacci = it)) }
                        PbCheckboxRow("Reverse", settings.fiboReverse) { onChange(settings.copy(fiboReverse = it)) }
                        settings.fiboLevels.forEachIndexed { idx, fl ->
                            PbCheckboxRow("Level ${idx + 1}", fl.display) {
                                pbSetLevel(settings, idx, fl.copy(display = it), onChange)
                            }
                            if (fl.display) {
                                PbFieldRow("  Level %", fl.level.toString()) {
                                    it.toFloatOrNull()?.let { v -> pbSetLevel(settings, idx, fl.copy(level = v.coerceIn(0f, 1f)), onChange) }
                                }
                                PbColorRow("  Color", fl.colorHex) { hex -> pbSetLevel(settings, idx, fl.copy(colorHex = hex), onChange) }
                                PbFieldRow("  Style", fl.style) {
                                    if (it in listOf("Dotted", "Dashed", "Solid")) pbSetLevel(settings, idx, fl.copy(style = it), onChange)
                                }
                            }
                        }
                        PbCheckboxRow("Display Labels", settings.fibosLabels) { onChange(settings.copy(fibosLabels = it)) }
                        PbFieldRow("Text Size", settings.fibosLabelSize.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(fibosLabelSize = v.coerceIn(1, 50))) }
                        }

                        PbSection("STYLE")
                        PbColorRow("Top Color", settings.topColorHex) { hex -> onChange(settings.copy(topColorHex = hex)) }
                        PbColorRow("Bottom Color", settings.bottomColorHex) { hex -> onChange(settings.copy(bottomColorHex = hex)) }
                        PbFieldRow("Extension Transparency", settings.transparency.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(transparency = v.coerceIn(0, 100))) }
                        }
                        PbCheckboxRow("Session Breaks", settings.sessionStartMarkers) { onChange(settings.copy(sessionStartMarkers = it)) }
                        PbColorRow(" Background Color", settings.backgroundColorHex) { hex -> onChange(settings.copy(backgroundColorHex = hex)) }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                HorizontalDivider(color = PB_TV_DIVIDER, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(PowerHourBreakoutSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PB_TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PB_TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("···", fontSize = 16.sp, letterSpacing = 2.sp) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PB_TV_TEXT_PRIMARY),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PB_TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) { Text("Cancel", fontSize = 14.sp) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp)
                    ) { Text("Ok", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

private fun pbSetLevel(
    settings: PowerHourBreakoutSettings,
    idx: Int,
    updated: FiboLevelInput,
    onChange: (PowerHourBreakoutSettings) -> Unit
) {
    val l = settings.fiboLevels.toMutableList()
    if (idx in l.indices) {
        l[idx] = updated
        onChange(settings.copy(fiboLevels = l))
    }
}

@Composable
private fun PbSection(text: String) {
    Text(text, color = PB_TV_SECTION, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun PbCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onChecked(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = PB_TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun PbFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = PB_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = PB_TV_TEXT_PRIMARY, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(140.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, PB_TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun PbColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = PB_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(4.dp)).background(pbChecker()).border(1.dp, PB_TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable {
            val idx = PB_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
            onPick(PB_PALETTE[(idx + 1) % PB_PALETTE.size])
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }))
        }
    }
}

private fun pbChecker(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39)))