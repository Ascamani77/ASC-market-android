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
import com.trading.app.indicators.ConfluenceFvgSettings

private val TV_BG = Color(0xFF000000)
private val TV_CARD_BG = Color(0xFF000000)
private val TV_DIVIDER = Color(0xFF2A2E39)
private val TV_FIELD_BG = Color(0xFF1E222D)
private val TV_FIELD_BORDER = Color(0xFF363A45)
private val TV_TEXT_PRIMARY = Color.White
private val TV_TEXT_SECONDARY = Color(0xFFD1D4DC)
private val TV_TEXT_MUTED = Color(0xFF868993)
private val TV_SECTION_HEADER = Color(0xFF868993)
private val TV_CHECKBOX_BORDER = Color(0xFF6A6D78)

private val CFVG_PALETTE = listOf(
    "#089981", "#f23645", "#2157f3", "#ff5d00", "#787b86", "#4caf50",
    "#f44336", "#009688", "#64b5f6", "#2962ff", "#9c27b0", "#e91e63", "#ffffff", "#000000"
)

@Composable
fun ConfluenceFvgSettingsModal(
    settings: ConfluenceFvgSettings,
    onChange: (ConfluenceFvgSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = TV_BG
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Confluence FVG Finder",
                        color = TV_TEXT_PRIMARY,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TV_TEXT_PRIMARY)
                    }
                }
                // Tabs — horizontal header as in TradingView
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TvTab("Inputs", selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                    TvTab("Style", selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                    TvTab("Visibility", selected = tab == 2, modifier = Modifier.weight(1f)) { tab = 2 }
                }
                HorizontalDivider(color = TV_DIVIDER, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (tab) {
                        0 -> InputsTab(settings, onChange)
                        1 -> StyleTab(settings, onChange)
                        2 -> VisibilityTab(settings, onChange)
                    }
                }
                HorizontalDivider(color = TV_DIVIDER, thickness = 1.dp)
                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onChange(ConfluenceFvgSettings()) },
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TV_FIELD_BORDER),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TV_TEXT_PRIMARY),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("···", fontSize = 16.sp, letterSpacing = 2.sp)
                    }
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
private fun TvTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) TV_TEXT_PRIMARY else TV_TEXT_MUTED,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) TV_TEXT_PRIMARY else Color.Transparent)
        )
    }
}

@Composable
private fun InputsTab(settings: ConfluenceFvgSettings, onChange: (ConfluenceFvgSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        TvSectionHeader("UNIVERSAL ZONE SETTINGS")
        TvCheckboxRow("Normalize All Zone Heights", settings.useNormalizedZones) { onChange(settings.copy(useNormalizedZones = it)) }
        TvDropdownRow(
            label = "Zone Height Method",
            value = if (settings.zoneHeightMethod == "atr") "ATR Based" else "Fixed Percentage",
            options = listOf("ATR Based", "Fixed Percentage")
        ) { sel -> onChange(settings.copy(zoneHeightMethod = if (sel == "ATR Based") "atr" else "percent")) }
        TvTextFieldRow("Zone Height (ATR Multiplier)", settings.zoneHeightAtrMult.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(zoneHeightAtrMult = v.coerceIn(0.1f, 3f))) }
        }
        TvTextFieldRow("Zone Height (% of Price)", settings.zoneHeightPercent.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(zoneHeightPercent = v.coerceIn(0.05f, 2f))) }
        }

        TvSectionHeader("FVG DETECTION")
        TvCheckboxRow("Show Bullish FVG", settings.showBullish) { onChange(settings.copy(showBullish = it)) }
        TvCheckboxRow("Show Bearish FVG", settings.showBearish) { onChange(settings.copy(showBearish = it)) }
        TvTextFieldRow("Max Zones Per Side", settings.maxZonesPerSide.toString()) {
            it.toIntOrNull()?.let { v -> onChange(settings.copy(maxZonesPerSide = v.coerceIn(1, 50))) }
        }

        TvSectionHeader("MULTI-TIMEFRAME CONFLUENCE")
        TvTimeframeRow("Timeframe 1 (anchor / lowest)", settings.tf1Min) { v -> onChange(settings.copy(tf1Min = v)) }
        TvTimeframeRow("Timeframe 2", settings.tf2Min) { v -> onChange(settings.copy(tf2Min = v)) }
        TvTimeframeRow("Timeframe 3", settings.tf3Min) { v -> onChange(settings.copy(tf3Min = v)) }
        TvTextFieldRow("Minimum Timeframe Confluence", settings.minConfluence.toString()) {
            it.toIntOrNull()?.let { v -> onChange(settings.copy(minConfluence = v.coerceIn(2, 3))) }
        }
        TvTextFieldRow("Proximity Tolerance (x ATR)", settings.proximityAtrMult.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(proximityAtrMult = v.coerceIn(0.5f, 10f))) }
        }

        TvSectionHeader("STRENGTH RATING")
        TvCheckboxRow("Enable Strength Rating", settings.enableStrengthRating) { onChange(settings.copy(enableStrengthRating = it)) }
        TvTextFieldRow("Minimum Strength Filter", settings.minStrengthFilter.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(minStrengthFilter = v.coerceIn(0f, 10f))) }
        }
        TvTextFieldRow("Strength Bonus Per Extra Timeframe", settings.confluenceBonus.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(confluenceBonus = v.coerceIn(0f, 2f))) }
        }

        TvSectionHeader("FVG STYLING")
        TvColorRow("Bullish FVG Color", settings.bullishColorHex) { hex -> onChange(settings.copy(bullishColorHex = hex)) }
        TvColorRow("Bearish FVG Color", settings.bearishColorHex) { hex -> onChange(settings.copy(bearishColorHex = hex)) }
        TvColorRow("Bullish Border", settings.bullishBorderHex) { hex -> onChange(settings.copy(bullishBorderHex = hex)) }
        TvColorRow("Bearish Border", settings.bearishBorderHex) { hex -> onChange(settings.copy(bearishBorderHex = hex)) }
        TvTextFieldRow("Border Width", settings.borderWidth.toString()) {
            it.toIntOrNull()?.let { v -> onChange(settings.copy(borderWidth = v.coerceIn(1, 5))) }
        }
        TvCheckboxRow("Show Direction Labels", settings.showDirectionLabels) { onChange(settings.copy(showDirectionLabels = it)) }

        TvSectionHeader("EXTENDED INFO LABELS")
        TvCheckboxRow("Show Extended Info Labels", settings.showExtendedInfo) { onChange(settings.copy(showExtendedInfo = it)) }
        TvDropdownRow(
            label = "Info Text Size",
            value = settings.infoTextSize,
            options = listOf("Tiny", "Small", "Normal", "Large", "Huge")
        ) { sel -> onChange(settings.copy(infoTextSize = sel)) }

        TvSectionHeader("MITIGATION SETTINGS")
        TvDropdownRow(
            label = "Mitigation Type",
            value = when (settings.mitigationType) { "touch" -> "Touch"; "full" -> "Full Fill"; else -> "50% Fill" },
            options = listOf("Touch", "Full Fill", "50% Fill")
        ) { sel ->
            onChange(settings.copy(mitigationType = when (sel) { "Touch" -> "touch"; "Full Fill" -> "full"; else -> "fifty" }))
        }
        TvCheckboxRow("Show Mitigated FVG", settings.showMitigated) { onChange(settings.copy(showMitigated = it)) }
        TvColorRow("Mitigated FVG Color", settings.mitigatedColorHex) { hex -> onChange(settings.copy(mitigatedColorHex = hex)) }

        TvSectionHeader("FILTER SETTINGS")
        TvTextFieldRow("Minimum Gap Size (0 = No Filter)", if (settings.minGapSize == 0f) "0" else settings.minGapSize.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(minGapSize = v.coerceAtLeast(0f))) }
        }
        TvCheckboxRow("Use ATR Filter", settings.useAtrFilter) { onChange(settings.copy(useAtrFilter = it)) }
        TvTextFieldRow("Minimum Gap Size (ATR Multiplier)", settings.atrMultiplier.toString()) {
            it.toFloatOrNull()?.let { v -> onChange(settings.copy(atrMultiplier = v.coerceAtLeast(0.1f))) }
        }
        TvTextFieldRow("ATR Length", settings.atrLength.toString()) {
            it.toIntOrNull()?.let { v -> if (v >= 1) onChange(settings.copy(atrLength = v)) }
        }
        Spacer(Modifier.height(8.dp))
        TvCheckboxRow("Zone entry alarms (toast)", settings.alertsEnabled) { onChange(settings.copy(alertsEnabled = it)) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StyleTab(settings: ConfluenceFvgSettings, onChange: (ConfluenceFvgSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        TvSectionHeader("GRAPHIC OBJECTS")
        TvCheckboxRow("Boxes", settings.styleBoxes) { onChange(settings.copy(styleBoxes = it)) }
        TvCheckboxRow("Pane labels", settings.stylePaneLabels) { onChange(settings.copy(stylePaneLabels = it)) }
        TvSectionHeader("OUTPUT VALUES")
        TvDropdownRow("Precision", settings.stylePrecision, listOf("Default", "0", "1", "2", "3", "4", "5")) { sel ->
            onChange(settings.copy(stylePrecision = sel))
        }
        TvSectionHeader("INPUT VALUES")
        TvCheckboxRow("Inputs in status line", settings.styleInputsInStatusLine) { onChange(settings.copy(styleInputsInStatusLine = it)) }
        Spacer(Modifier.height(200.dp))
    }
}

@Composable
private fun VisibilityTab(settings: ConfluenceFvgSettings, onChange: (ConfluenceFvgSettings) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        TvCheckboxRow("Ticks", settings.visTicks) { onChange(settings.copy(visTicks = it)) }
        TvVisibilityIntervalRow("Seconds", settings.visSeconds, { onChange(settings.copy(visSeconds = it)) }, "1", "59")
        TvVisibilityIntervalRow("Minutes", settings.visMinutes, { onChange(settings.copy(visMinutes = it)) }, "1", "59")
        TvVisibilityIntervalRow("Hours", settings.visHours, { onChange(settings.copy(visHours = it)) }, "1", "24")
        TvVisibilityIntervalRow("Days", settings.visDays, { onChange(settings.copy(visDays = it)) }, "1", "366")
        TvVisibilityIntervalRow("Weeks", settings.visWeeks, { onChange(settings.copy(visWeeks = it)) }, "1", "52")
        TvVisibilityIntervalRow("Months", settings.visMonths, { onChange(settings.copy(visMonths = it)) }, "1", "12")
        TvCheckboxRow("Ranges", settings.visRanges) { onChange(settings.copy(visRanges = it)) }
        Spacer(Modifier.height(200.dp))
    }
}

// ---- Shared TradingView-styled components ----

@Composable
private fun TvSectionHeader(text: String) {
    Text(
        text,
        color = TV_SECTION_HEADER,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun TvCheckboxRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onChecked(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = TV_TEXT_PRIMARY, fontSize = 14.sp)
    }
}

@Composable
private fun TvTextFieldRow(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (text != value) text = value }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            label,
            color = TV_TEXT_SECONDARY,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
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
private fun TvDropdownRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            label,
            color = TV_TEXT_SECONDARY,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
                .border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    value,
                    color = TV_TEXT_PRIMARY,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TV_TEXT_MUTED, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E222D))
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, color = if (opt == value) Color.White else TV_TEXT_SECONDARY, fontSize = 13.sp) },
                        onClick = {
                            expanded = false
                            onSelect(opt)
                        },
                        modifier = Modifier.background(if (opt == value) Color(0xFF2A2E39) else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun TvTimeframeRow(label: String, minutes: Int, onPick: (Int) -> Unit) {
    val options = listOf(1 to "1 minute", 5 to "5 minutes", 15 to "15 minutes", 30 to "30 minutes", 60 to "1 hour", 120 to "2 hours", 240 to "4 hours", 1440 to "1 day")
    val display = options.firstOrNull { it.first == minutes }?.second ?: "$minutes minutes"
    TvDropdownRow(label, display, options.map { it.second }) { sel ->
        options.firstOrNull { it.second == sel }?.let { onPick(it.first) }
    }
}

@Composable
private fun TvColorRow(label: String, hex: String, onPick: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            label,
            color = TV_TEXT_SECONDARY,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(checkerBrush())
                .border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp))
                .clickable {
                    val idx = CFVG_PALETTE.indexOfFirst { it.equals(hex, ignoreCase = true) }.coerceAtLeast(0)
                    onPick(CFVG_PALETTE[(idx + 1) % CFVG_PALETTE.size])
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray })
            )
        }
    }
}

@Composable
private fun TvVisibilityIntervalRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    fromDefault: String,
    toDefault: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = TV_TEXT_PRIMARY, fontSize = 14.sp, modifier = Modifier.width(80.dp))
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
                .border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(fromDefault, color = TV_TEXT_SECONDARY, fontSize = 13.sp)
        }
        Text("  –  ", color = TV_TEXT_MUTED, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
                .border(1.dp, TV_FIELD_BORDER, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(toDefault, color = TV_TEXT_SECONDARY, fontSize = 13.sp)
        }
    }
}

private fun checkerBrush(): androidx.compose.ui.graphics.Brush {
    return androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(Color(0xFF3A3E4A), Color(0xFF2A2E39))
    )
}
