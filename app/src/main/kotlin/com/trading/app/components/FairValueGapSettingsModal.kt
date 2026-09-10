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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.trading.app.indicators.FairValueGapSettings

private val TV_BG = Color(0xFF000000)
private val TV_DIVIDER = Color(0xFF2A2E39)
private val TV_FIELD_BORDER = Color(0xFF363A45)
private val TV_TEXT_PRIMARY = Color.White
private val TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val TV_TEXT_MUTED = Color(0xFF868993)
private val TV_SECTION = Color(0xFF868993)

private val FVG_PALETTE = listOf(
    "#089981", "#f23645", "#2157f3", "#ff5d00", "#787b86", "#4caf50",
    "#f44336", "#009688", "#64b5f6", "#2962ff", "#9c27b0", "#e91e63", "#ffffff"
)

@Composable
fun FairValueGapSettingsModal(
    settings: FairValueGapSettings,
    onChange: (FairValueGapSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = TV_BG) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LuxAlgo - Fair Value Gap", color = TV_TEXT_PRIMARY, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TV_TEXT_PRIMARY)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    FvTab("Inputs", selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                    FvTab("Style", selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                    FvTab("Visibility", selected = tab == 2, modifier = Modifier.weight(1f)) { tab = 2 }
                }
                HorizontalDivider(color = TV_DIVIDER, thickness = 1.dp)
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> FvInputsTab(settings, onChange)
                        1 -> FvStyleTab(settings, onChange)
                        2 -> FvVisibilityTab(settings, onChange)
                    }
                }
                HorizontalDivider(color = TV_DIVIDER, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(FairValueGapSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("···", fontSize = 16.sp, letterSpacing = 2.sp) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TV_TEXT_PRIMARY),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TV_TEXT_PRIMARY),
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

@Composable
private fun FvTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected) TV_TEXT_PRIMARY else TV_TEXT_MUTED, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (selected) TV_TEXT_PRIMARY else Color.Transparent))
    }
}

@Composable
private fun FvInputsTab(s: FairValueGapSettings, onChange: (FairValueGapSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Threshold % + Auto inline
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text("Threshold %", color = TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
            FvField(s.thresholdPer.toString().let { if (it.endsWith(".0")) it.dropLast(2) else it }, "0") { it.toFloatOrNull()?.let { v -> onChange(s.copy(thresholdPer = v.coerceIn(0f, 100f))) } }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onChange(s.copy(auto = !s.auto)) }) {
                Checkbox(checked = s.auto, onCheckedChange = { onChange(s.copy(auto = it)) }, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auto", color = TV_TEXT_PRIMARY, fontSize = 14.sp)
            }
        }
        FvFieldRow("Unmitigated Levels", s.showLast.toString()) { it.toIntOrNull()?.let { v -> onChange(s.copy(showLast = v.coerceAtLeast(0))) } }
        FvCheckboxRow("Mitigation Levels", s.mitigationLevels) { onChange(s.copy(mitigationLevels = it)) }
        FvTimeframeDropdown("Timeframe", s.timeframeMinutes) { v -> onChange(s.copy(timeframeMinutes = v)) }

        FvSection("STYLE")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text("Extend", color = TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
            FvField(s.extend.toString(), "20") { it.toIntOrNull()?.let { v -> onChange(s.copy(extend = v.coerceAtLeast(0))) } }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onChange(s.copy(dynamic = !s.dynamic)) }) {
                Checkbox(checked = s.dynamic, onCheckedChange = { onChange(s.copy(dynamic = it)) }, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dynamic", color = TV_TEXT_PRIMARY, fontSize = 14.sp)
            }
        }
        FvColorRow("Bullish FVG", s.bullColorHex) { hex -> onChange(s.copy(bullColorHex = hex)) }
        FvColorRow("Bearish FVG", s.bearColorHex) { hex -> onChange(s.copy(bearColorHex = hex)) }

        FvSection("DASHBOARD")
        FvCheckboxRow("Show Dashboard", s.showDashboard) { onChange(s.copy(showDashboard = it)) }
        FvDropdownRow("Location", s.dashboardLocation, listOf("Top Right", "Bottom Right", "Bottom Left")) { sel -> onChange(s.copy(dashboardLocation = sel)) }
        FvDropdownRow("Size", s.dashboardSize, listOf("Tiny", "Small", "Normal")) { sel -> onChange(s.copy(dashboardSize = sel)) }
        Spacer(Modifier.height(8.dp))
        FvCheckboxRow("Zone entry alarms (toast)", s.alertsEnabled) { onChange(s.copy(alertsEnabled = it)) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FvStyleTab(s: FairValueGapSettings, onChange: (FairValueGapSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // 4 Plot rows
        repeat(4) { idx ->
            val enabled = when (idx) { 0 -> s.plot1Enabled; 1 -> s.plot2Enabled; 2 -> s.plot3Enabled; else -> s.plot4Enabled }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Checkbox(checked = enabled, onCheckedChange = {
                    onChange(when (idx) {
                        0 -> s.copy(plot1Enabled = it); 1 -> s.copy(plot2Enabled = it)
                        2 -> s.copy(plot3Enabled = it); else -> s.copy(plot4Enabled = it)
                    })
                }, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Plot", color = TV_TEXT_PRIMARY, fontSize = 14.sp, modifier = Modifier.width(40.dp))
                Spacer(Modifier.weight(1f))
                // style placeholders: line style boxes
                Box(modifier = Modifier.width(72.dp).height(28.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(40.dp).height(2.dp).background(TV_TEXT_SECONDARY))
                }
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.width(36.dp).height(28.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(16.dp).height(2.dp).background(TV_TEXT_SECONDARY))
                }
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.width(36.dp).height(28.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("~", color = TV_TEXT_SECONDARY, fontSize = 12.sp)
                }
            }
        }
        FvSection("GRAPHIC OBJECTS")
        FvCheckboxRow("Boxes", s.styleBoxes) { onChange(s.copy(styleBoxes = it)) }
        FvCheckboxRow("Lines", s.styleLines) { onChange(s.copy(styleLines = it)) }
        FvCheckboxRow("Tables", s.styleTables) { onChange(s.copy(styleTables = it)) }
        FvSection("OUTPUT VALUES")
        FvDropdownRow("Precision", s.stylePrecision, listOf("Default", "0", "1", "2", "3", "4", "5")) { sel -> onChange(s.copy(stylePrecision = sel)) }
        FvCheckboxRow("Labels on price scale", s.styleLabelsOnPriceScale) { onChange(s.copy(styleLabelsOnPriceScale = it)) }
        FvCheckboxRow("Values in status line", s.styleValuesInStatusLine) { onChange(s.copy(styleValuesInStatusLine = it)) }
        FvSection("INPUT VALUES")
        FvCheckboxRow("Inputs in status line", s.styleInputsInStatusLine) { onChange(s.copy(styleInputsInStatusLine = it)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun FvVisibilityTab(s: FairValueGapSettings, onChange: (FairValueGapSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        FvCheckboxRow("Ticks", s.visTicks) { onChange(s.copy(visTicks = it)) }
        FvVisInterval("Seconds", s.visSeconds, { onChange(s.copy(visSeconds = it)) }, "1", "59")
        FvVisInterval("Minutes", s.visMinutes, { onChange(s.copy(visMinutes = it)) }, "1", "59")
        FvVisInterval("Hours", s.visHours, { onChange(s.copy(visHours = it)) }, "1", "24")
        FvVisInterval("Days", s.visDays, { onChange(s.copy(visDays = it)) }, "1", "366")
        FvVisInterval("Weeks", s.visWeeks, { onChange(s.copy(visWeeks = it)) }, "1", "52")
        FvVisInterval("Months", s.visMonths, { onChange(s.copy(visMonths = it)) }, "1", "12")
        FvCheckboxRow("Ranges", s.visRanges) { onChange(s.copy(visRanges = it)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun FvSection(text: String) {
    Text(text, color = TV_SECTION, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun FvCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onChecked(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun FvFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = TV_TEXT_PRIMARY, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(140.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun FvField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    BasicTextField(
        value = text,
        onValueChange = { text = it; onValueChange(it) },
        singleLine = true,
        textStyle = TextStyle(color = TV_TEXT_PRIMARY, fontSize = 13.sp),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(110.dp).height(44.dp)
            .background(Color.Black, RoundedCornerShape(4.dp))
            .border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp),
        decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
    )
}

@Composable
private fun FvDropdownRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.width(140.dp).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable { expanded = true }.padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(value, color = TV_TEXT_PRIMARY, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TV_TEXT_MUTED, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1E222D))) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt, color = if (opt == value) Color.White else TV_TEXT_SECONDARY, fontSize = 13.sp) }, onClick = { expanded = false; onSelect(opt) }, modifier = Modifier.background(if (opt == value) Color(0xFF2A2E39) else Color.Transparent))
                }
            }
        }
    }
}

@Composable
private fun FvTimeframeDropdown(label: String, minutes: Int, onPick: (Int) -> Unit) {
    val opts = listOf(0 to "Chart", 1 to "1 minute", 5 to "5 minutes", 15 to "15 minutes", 30 to "30 minutes", 60 to "1 hour", 120 to "2 hours", 240 to "4 hours", 1440 to "1 day")
    val display = opts.firstOrNull { it.first == minutes }?.second ?: "Chart"
    FvDropdownRow(label, display, opts.map { it.second }) { sel -> opts.firstOrNull { it.second == sel }?.let { onPick(it.first) } }
}

@Composable
private fun FvColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(4.dp)).background(checker()).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable {
            val idx = FVG_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
            onPick(FVG_PALETTE[(idx + 1) % FVG_PALETTE.size])
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }))
        }
    }
}

@Composable
private fun FvVisInterval(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, fromDefault: String, toDefault: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TV_TEXT_PRIMARY, fontSize = 14.sp, modifier = Modifier.width(80.dp))
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.width(86.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text(fromDefault, color = TV_TEXT_SECONDARY, fontSize = 13.sp) }
        Text("  –  ", color = TV_TEXT_MUTED, fontSize = 13.sp)
        Box(modifier = Modifier.width(86.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text(toDefault, color = TV_TEXT_SECONDARY, fontSize = 13.sp) }
    }
}

private fun checker(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39)))
