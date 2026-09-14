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
import com.trading.app.indicators.EqhEqlLiquidityZonesSettings

private val EQH_TV_BG = Color(0xFF000000)
private val EQH_TV_DIVIDER = Color(0xFF2A2E39)
private val EQH_TV_FIELD_BORDER = Color(0xFF363A45)
private val EQH_TV_TEXT_PRIMARY = Color.White
private val EQH_TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val EQH_TV_TEXT_MUTED = Color(0xFF868993)
private val EQH_TV_SECTION = Color(0xFF868993)

private val EQH_PALETTE = listOf(
    "#089981", "#f23645", "#787b86", "#2157f3", "#f44336", "#81c784",
    "#4caf50", "#009688", "#64b5f6", "#2962ff", "#9c27b0", "#e91e63",
    "#ff5d00", "#ff9800", "#ffffff"
)

@Composable
fun EqhEqlLiquidityZonesSettingsModal(
    settings: EqhEqlLiquidityZonesSettings,
    onChange: (EqhEqlLiquidityZonesSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = EQH_TV_BG) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LuxAlgo - EQH/EQL Liquidity Zones", color = EQH_TV_TEXT_PRIMARY, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = EQH_TV_TEXT_PRIMARY)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    EqhTvTab("Inputs", selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                    EqhTvTab("Style", selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                    EqhTvTab("Visibility", selected = tab == 2, modifier = Modifier.weight(1f)) { tab = 2 }
                }
                HorizontalDivider(color = EQH_TV_DIVIDER, thickness = 1.dp)
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (tab) {
                        0 -> EqhInputsTab(settings, onChange)
                        1 -> EqhStyleTab(settings, onChange)
                        2 -> EqhVisibilityTab(settings, onChange)
                    }
                }
                HorizontalDivider(color = EQH_TV_DIVIDER, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(EqhEqlLiquidityZonesSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EQH_TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EQH_TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("···", fontSize = 16.sp, letterSpacing = 2.sp) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EQH_TV_TEXT_PRIMARY),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EQH_TV_TEXT_PRIMARY),
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
private fun EqhTvTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected) EQH_TV_TEXT_PRIMARY else EQH_TV_TEXT_MUTED, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(vertical = 10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (selected) EQH_TV_TEXT_PRIMARY else Color.Transparent))
    }
}

@Composable
private fun EqhInputsTab(s: EqhEqlLiquidityZonesSettings, onChange: (EqhEqlLiquidityZonesSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        EqhFieldRow("Pivot Left Length", s.pivotLeft.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(s.copy(pivotLeft = v.coerceIn(1, 200))) }
        }
        EqhFieldRow("Pivot Right Length", s.pivotRight.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(s.copy(pivotRight = v.coerceIn(1, 200))) }
        }
        EqhFieldRow("Equality Threshold (%)", s.thresholdPct.toString()) {
            it.toFloatOrNull()?.let { v -> if (v >= 0f) onChange(s.copy(thresholdPct = v.coerceIn(0f, 100f))) }
        }
        EqhFieldRow("Max Active Zones", s.maxZones.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(s.copy(maxZones = v.coerceIn(1, 100))) }
        }

        EqhSection("VISUALS")
        EqhCheckboxRow("Delete on Sweep", s.deleteOnSweep) { onChange(s.copy(deleteOnSweep = it)) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EqhStyleTab(s: EqhEqlLiquidityZonesSettings, onChange: (EqhEqlLiquidityZonesSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        EqhColorRow("Bullish Zone Color", s.bullColorHex) { hex -> onChange(s.copy(bullColorHex = hex)) }
        EqhColorRow("Bearish Zone Color", s.bearColorHex) { hex -> onChange(s.copy(bearColorHex = hex)) }
        EqhFieldRow("Zone Transparency", s.zoneTransp.toString()) {
            it.toIntOrNull()?.let { v -> onChange(s.copy(zoneTransp = v.coerceIn(0, 100))) }
        }
        EqhColorRow("Midline Color", s.midlineColorHex) { hex -> onChange(s.copy(midlineColorHex = hex)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun EqhVisibilityTab(s: EqhEqlLiquidityZonesSettings, onChange: (EqhEqlLiquidityZonesSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        EqhSection("ZONES")
        EqhCheckboxRow("Show Midline", s.showMidline) { onChange(s.copy(showMidline = it)) }
        EqhCheckboxRow("Show Labels", s.showLabels) { onChange(s.copy(showLabels = it)) }
        EqhCheckboxRow("Show Volume", s.showVolume) { onChange(s.copy(showVolume = it)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun EqhSection(text: String) {
    Text(text, color = EQH_TV_SECTION, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun EqhCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onChecked(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = EQH_TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun EqhFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = EQH_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = EQH_TV_TEXT_PRIMARY, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(140.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, EQH_TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun EqhColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = EQH_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(4.dp)).background(eqhChecker()).border(1.dp, EQH_TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable {
            val idx = EQH_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
            onPick(EQH_PALETTE[(idx + 1) % EQH_PALETTE.size])
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }))
        }
    }
}

private fun eqhChecker(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39)))
