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
import com.trading.app.indicators.TrendlineNavigatorSettings

private val TN_TV_BG = Color(0xFF000000)
private val TN_TV_DIVIDER = Color(0xFF2A2E39)
private val TN_TV_FIELD_BORDER = Color(0xFF363A45)
private val TN_TV_TEXT_PRIMARY = Color.White
private val TN_TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val TN_TV_SECTION = Color(0xFF868993)

private val TN_PALETTE = listOf(
    "#089981", "#f23645", "#085def", "#ff5d00", "#787b86", "#2157f3",
    "#f44336", "#81c784", "#4caf50", "#009688", "#64b5f6", "#2962ff",
    "#9c27b0", "#e91e63", "#ff5d00", "#ff9800", "#c0c0c0", "#ffffff"
)

@Composable
fun TrendlineNavigatorSettingsModal(
    settings: TrendlineNavigatorSettings,
    onChange: (TrendlineNavigatorSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = TN_TV_BG) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LuxAlgo - Trendline Breakout Navigator", color = TN_TV_TEXT_PRIMARY, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TN_TV_TEXT_PRIMARY)
                    }
                }
                HorizontalDivider(color = TN_TV_DIVIDER, thickness = 1.dp)
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Column {
                        TnSection("PERIOD")
                        TnFieldRow("Period (timeframe)", settings.res) {
                            onChange(settings.copy(res = it.trim()))
                        }

                        TnSection("SWING LENGTH")
                        TnCheckboxRow("Long swings", settings.showLong) { onChange(settings.copy(showLong = it)) }
                        TnFieldRow("  Long length", settings.longLen.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(longLen = v.coerceIn(2, 500))) }
                        }
                        TnCheckboxRow("Medium swings", settings.showMedium) { onChange(settings.copy(showMedium = it)) }
                        TnFieldRow("  Medium length", settings.mediumLen.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(mediumLen = v.coerceIn(2, 500))) }
                        }
                        TnCheckboxRow("Short swings", settings.showShort) { onChange(settings.copy(showShort = it)) }
                        TnFieldRow("  Short length", settings.shortLen.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(shortLen = v.coerceIn(2, 500))) }
                        }

                        TnSection("STYLE")
                        TnColorRow("Bullish Trendline", settings.bullColorHex) { hex -> onChange(settings.copy(bullColorHex = hex)) }
                        TnColorRow("Bearish Trendline", settings.bearColorHex) { hex -> onChange(settings.copy(bearColorHex = hex)) }
                        TnColorRow("Bullish Wick Dot", settings.wickBullColorHex) { hex -> onChange(settings.copy(wickBullColorHex = hex)) }
                        TnColorRow("Bearish Wick Dot", settings.wickBearColorHex) { hex -> onChange(settings.copy(wickBearColorHex = hex)) }
                        TnFieldRow("Term (Long/Medium/Short)", settings.term) {
                            val t = it.trim()
                            if (t.equals("Long", ignoreCase = true) || t.equals("Medium", ignoreCase = true) || t.equals("Short", ignoreCase = true)) {
                                onChange(settings.copy(term = TrendlineNavigatorSettings.normalizeTerm(t)))
                            }
                        }
                        TnFieldRow("HH/LL", settings.hhll) {
                            val t = it.trim()
                            if (t.equals("None", ignoreCase = true) || t.equals("Only HH/LL", ignoreCase = true) || t.startsWith("HH/LL", ignoreCase = true)) {
                                onChange(settings.copy(hhll = TrendlineNavigatorSettings.normalizeHhll(t)))
                            }
                        }
                        TnCheckboxRow("Background Color", settings.background) { onChange(settings.copy(background = it)) }
                        TnCheckboxRow("Bar Color", settings.barColor) { onChange(settings.copy(barColor = it)) }
                        Text(
                            "Background/Bar Color are stored but not rendered: the chart library has no per-bar background or candle recolor primitive.",
                            color = TN_TV_SECTION,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
                HorizontalDivider(color = TN_TV_DIVIDER, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(TrendlineNavigatorSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TN_TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TN_TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("···", fontSize = 16.sp, letterSpacing = 2.sp) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TN_TV_TEXT_PRIMARY),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TN_TV_TEXT_PRIMARY),
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
private fun TnSection(text: String) {
    Text(text, color = TN_TV_SECTION, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun TnCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onChecked(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TN_TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun TnFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = TN_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = TN_TV_TEXT_PRIMARY, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.width(140.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, TN_TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun TnColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = TN_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(4.dp)).background(tnChecker()).border(1.dp, TN_TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable {
            val idx = TN_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
            onPick(TN_PALETTE[(idx + 1) % TN_PALETTE.size])
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }))
        }
    }
}

private fun tnChecker(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39)))