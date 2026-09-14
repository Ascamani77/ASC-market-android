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
import com.trading.app.indicators.OrderBlockBreakerSettings

private val OBB_TV_BG = Color(0xFF000000)
private val OBB_TV_DIVIDER = Color(0xFF2A2E39)
private val OBB_TV_FIELD_BORDER = Color(0xFF363A45)
private val OBB_TV_TEXT_PRIMARY = Color.White
private val OBB_TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val OBB_TV_SECTION = Color(0xFF868993)

private val OBB_DETECTIONS = listOf("Short Term", "Intermediate Term", "Long Term")

private val OBB_PALETTE = listOf(
    "#2157f3", "#ff1100", "#ff5d00", "#0cb51a", "#089981", "#f23645", "#085def",
    "#787b86", "#f44336", "#81c784", "#4caf50", "#009688", "#64b5f6", "#2962ff",
    "#9c27b0", "#e91e63", "#ff9800", "#c0c0c0", "#ffffff"
)

@Composable
fun OrderBlockBreakerSettingsModal(
    settings: OrderBlockBreakerSettings,
    onChange: (OrderBlockBreakerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = OBB_TV_BG) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LuxAlgo - Pure Price Action OB/BB", color = OBB_TV_TEXT_PRIMARY, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = OBB_TV_TEXT_PRIMARY)
                    }
                }
                HorizontalDivider(color = OBB_TV_DIVIDER, thickness = 1.dp)
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Column {
                        ObbSection("DETECTION")
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Text("Detection", color = OBB_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                            Row {
                                OBB_DETECTIONS.forEach { d ->
                                    val selected = OrderBlockBreakerSettings.normalizeDetection(settings.detection) == d
                                    Button(
                                        onClick = { onChange(settings.copy(detection = d)) },
                                        shape = RoundedCornerShape(4.dp),
                                        colors = if (selected) ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                        else ButtonDefaults.outlinedButtonColors(contentColor = OBB_TV_TEXT_PRIMARY),
                                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, OBB_TV_FIELD_BORDER),
                                        modifier = Modifier.height(34.dp).padding(end = 6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) { Text(d, fontSize = 12.sp) }
                                }
                            }
                        }

                        ObbSection("VISIBILITY")
                        ObbFieldRow("Show Last Bullish OB", settings.showBull.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(showBull = v.coerceAtLeast(0))) }
                        }
                        ObbFieldRow("Show Last Bearish OB", settings.showBear.toString()) {
                            it.toIntOrNull()?.let { v -> onChange(settings.copy(showBear = v.coerceAtLeast(0))) }
                        }

                        ObbSection("DETECTION SETTINGS")
                        ObbCheckboxRow("Use Candle Body", settings.useBody) { onChange(settings.copy(useBody = it)) }
                        ObbCheckboxRow("Show Historical Polarity Changes", settings.showLabels) { onChange(settings.copy(showLabels = it)) }

                        ObbSection("STYLE")
                        ObbColorRow("Bullish OB", settings.bullCssHex) { hex -> onChange(settings.copy(bullCssHex = hex)) }
                        ObbColorRow("Bullish Break", settings.bullBreakCssHex) { hex -> onChange(settings.copy(bullBreakCssHex = hex)) }
                        ObbColorRow("Bearish OB", settings.bearCssHex) { hex -> onChange(settings.copy(bearCssHex = hex)) }
                        ObbColorRow("Bearish Break", settings.bearBreakCssHex) { hex -> onChange(settings.copy(bearBreakCssHex = hex)) }
                        Text(
                            "Colors store pure RGB. Renderer applies Pine transparencies automatically (OB: 20% opacity, Break: 60% opacity); the dotted average line and polarity labels use full color.",
                            color = OBB_TV_SECTION,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
                HorizontalDivider(color = OBB_TV_DIVIDER, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(OrderBlockBreakerSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OBB_TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OBB_TV_TEXT_PRIMARY),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("···", fontSize = 16.sp, letterSpacing = 2.sp) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OBB_TV_TEXT_PRIMARY),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OBB_TV_TEXT_PRIMARY),
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
private fun ObbSection(text: String) {
    Text(text, color = OBB_TV_SECTION, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun ObbCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onChecked(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChecked, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = OBB_TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun ObbFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = OBB_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = OBB_TV_TEXT_PRIMARY, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.width(140.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, OBB_TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun ObbColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = OBB_TV_TEXT_SECONDARY, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(modifier = Modifier.size(width = 36.dp, height = 28.dp).clip(RoundedCornerShape(4.dp)).background(obbChecker()).border(1.dp, OBB_TV_FIELD_BORDER, RoundedCornerShape(4.dp)).clickable {
            val idx = OBB_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
            onPick(OBB_PALETTE[(idx + 1) % OBB_PALETTE.size])
        }, contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().background(try { Color(android.graphics.Color.parseColor("#FF" + hex.removePrefix("#"))) } catch (_: Exception) { Color.Gray }))
        }
    }
}

private fun obbChecker(): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39)))