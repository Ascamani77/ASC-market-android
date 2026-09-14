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
import com.trading.app.indicators.LiquidityDeltaProfilerSettings

private val LDP_TV_BG = Color(0xFF000000)
private val LDP_TV_DIVIDER = Color(0xFF2A2E39)
private val LDP_TV_FIELD_BORDER = Color(0xFF363A45)
private val LDP_TV_TEXT_PRIMARY = Color.White
private val LDP_TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val LDP_TV_TEXT_MUTED = Color(0xFF868993)
private val LDP_TV_SECTION = Color(0xFF868993)

private val LDP_PALETTE = listOf(
    "#f23645", "#089981", "#2157f3", "#787b86", "#f44336", "#81c784",
    "#4caf50", "#009688", "#64b5f6", "#2962ff", "#9c27b0", "#e91e63",
    "#ff5d00", "#ff9800", "#ffffff"
)

private val LDP_POSITIONS = listOf("Top Right", "Bottom Right", "Bottom Left")
private val LDP_SIZES = listOf("Tiny", "Small", "Normal", "Large", "Huge")

@Composable
fun LiquidityDeltaProfilerSettingsModal(
    settings: LiquidityDeltaProfilerSettings,
    onChange: (LiquidityDeltaProfilerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = LDP_TV_BG) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LuxAlgo - Liquidity Delta Profiler", color = LDP_TV_TEXT_PRIMARY, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = LDP_TV_TEXT_PRIMARY)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    LdpTvTab("Inputs", selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                    LdpTvTab("Style", selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                    LdpTvTab("Visibility", selected = tab == 2, modifier = Modifier.weight(1f)) { tab = 2 }
                }
                HorizontalDivider(color = LDP_TV_DIVIDER, thickness = 1.dp)
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> LdpInputsTab(settings, onChange)
                        1 -> LdpStyleTab(settings, onChange)
                        2 -> LdpVisibilityTab(settings, onChange)
                    }
                }
                HorizontalDivider(color = LDP_TV_DIVIDER, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(LiquidityDeltaProfilerSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LDP_TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LDP_TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("···", fontSize = 16.sp, letterSpacing = 2.sp) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LDP_TV_TEXT_PRIMARY),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LDP_TV_TEXT_PRIMARY),
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
private fun LdpTvTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected) LDP_TV_TEXT_PRIMARY else LDP_TV_TEXT_MUTED, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (selected) LDP_TV_TEXT_PRIMARY else Color.Transparent))
    }
}

@Composable
private fun LdpInputsTab(s: LiquidityDeltaProfilerSettings, onChange: (LiquidityDeltaProfilerSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        LdpFieldRow("Pivot Length", s.pivotLength.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 2) onChange(s.copy(pivotLength = v.coerceIn(2, 200))) }
        }
        LdpFieldRow("Max Zones per Type", s.maxZones.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(s.copy(maxZones = v.coerceIn(1, 40))) }
        }
        LdpCheckboxRow("Filter Overlapping Zones", s.filterOverlaps) { onChange(s.copy(filterOverlaps = it)) }

        LdpSection("ZONES")
        LdpFieldRow("Zone Volume Capacity", s.zoneCapacity.toString().let { if (it.endsWith(".0")) it.dropLast(2) else it }) {
            it.toFloatOrNull()?.let { v -> if (v >= 1f) onChange(s.copy(zoneCapacity = v.coerceIn(1f, 100f))) }
        }

        LdpSection("REVERSALS")
        LdpCheckboxRow("Enable Reversal Detection", s.enableReversals) { onChange(s.copy(enableReversals = it)) }
        LdpFieldRow("Eval Window (Bars)", s.dashboardWindow.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(s.copy(dashboardWindow = v.coerceIn(1, 500))) }
        }
        LdpFieldRow("Hold Time (Bars)", s.dashboardHold.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(s.copy(dashboardHold = v.coerceIn(1, 100))) }
        }

        LdpSection("DASHBOARD")
        LdpCheckboxRow("Show Dashboard", s.showDashboard) { onChange(s.copy(showDashboard = it)) }
        LdpDropdownRow("Position", s.dashboardPosition, LDP_POSITIONS) { sel -> onChange(s.copy(dashboardPosition = sel)) }
        LdpDropdownRow("Size", s.dashboardSize, LDP_SIZES) { sel -> onChange(s.copy(dashboardSize = sel)) }
        LdpCheckboxRow("Highlight Eval Bars", s.dashboardHighlight) { onChange(s.copy(dashboardHighlight = it)) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LdpStyleTab(s: LiquidityDeltaProfilerSettings, onChange: (LiquidityDeltaProfilerSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        LdpColorRow("BSL Outline", s.bslColorHex) { hex -> onChange(s.copy(bslColorHex = hex)) }
        LdpColorRow("SSL Outline", s.sslColorHex) { hex -> onChange(s.copy(sslColorHex = hex)) }
        LdpColorRow("Buy Delta Fill", s.buyDeltaColorHex) { hex -> onChange(s.copy(buyDeltaColorHex = hex)) }
        LdpColorRow("Sell Delta Fill", s.sellDeltaColorHex) { hex -> onChange(s.copy(sellDeltaColorHex = hex)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun LdpVisibilityTab(s: LiquidityDeltaProfilerSettings, onChange: (LiquidityDeltaProfilerSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        LdpSection("ZONES")
        LdpCheckboxRow("Show Swept Zones", s.showSwept) { onChange(s.copy(showSwept = it)) }
        LdpCheckboxRow("Show Zone Decay", s.showDecay) { onChange(s.copy(showDecay = it)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun LdpSection(text: String) {
    Text(text, color = LDP_TV_SECTION, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun LdpCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onChecked(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = LDP_TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun LdpFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = LDP_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = LDP_TV_TEXT_PRIMARY, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(140.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, LDP_TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun LdpDropdownRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = LDP_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.width(140.dp).height(36.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black).border(1.dp, LDP_TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable { expanded = true }.padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(value, color = LDP_TV_TEXT_PRIMARY, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = LDP_TV_TEXT_MUTED, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1E222D))) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt, color = if (opt == value) Color.White else LDP_TV_TEXT_SECONDARY, fontSize = 13.sp) }, onClick = { expanded = false; onSelect(opt) }, modifier = Modifier.background(if (opt == value) Color(0xFF2A2E39) else Color.Transparent))
                }
            }
        }
    }
}

@Composable
private fun LdpColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = LDP_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(4.dp)).background(ldpChecker()).border(1.dp, LDP_TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable {
            val idx = LDP_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
            onPick(LDP_PALETTE[(idx + 1) % LDP_PALETTE.size])
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }))
        }
    }
}

private fun ldpChecker(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39)))
