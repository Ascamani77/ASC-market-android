package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.trading.app.indicators.AutoFibSettings
import com.trading.app.indicators.AutoFibLevelState

private val AUTO_FIB_PALETTE = listOf(
    "#787b86", "#f44336", "#81c784", "#4caf50", "#009688", "#64b5f6",
    "#2962ff", "#9c27b0", "#e91e63", "#ff9800", "#ffc107", "#ffffff"
)

@Composable
fun AutoFibSettingsModal(
    settings: AutoFibSettings,
    onChange: (AutoFibSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var deviationText by remember(settings) { mutableStateOf(settings.deviation.toString()) }
    var depthText by remember(settings) { mutableStateOf(settings.depth.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            color = Color(0xFF1E222D),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Auto Fib Retracement", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))

                SettingsSectionTitle("Calculation")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsNumberField(
                        value = deviationText,
                        onValueChange = {
                            deviationText = it
                            it.toFloatOrNull()?.let { v -> onChange(settings.copy(deviation = v)) }
                        },
                        label = "Deviation"
                    )
                    Spacer(Modifier.width(12.dp))
                    SettingsNumberField(
                        value = depthText,
                        onValueChange = {
                            depthText = it
                            it.toIntOrNull()?.let { v -> if (v >= 2) onChange(settings.copy(depth = v)) }
                        },
                        label = "Depth"
                    )
                    Spacer(Modifier.width(12.dp))
                    SettingsSwitchRow("Reverse", settings.reverse) { onChange(settings.copy(reverse = it)) }
                }
                Spacer(Modifier.height(12.dp))

                SettingsSectionTitle("Lines")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsSwitchRow("Extend L", settings.extendLeft) { onChange(settings.copy(extendLeft = it)) }
                    Spacer(Modifier.width(10.dp))
                    SettingsSwitchRow("Extend R", settings.extendRight) { onChange(settings.copy(extendRight = it)) }
                    Spacer(Modifier.width(10.dp))
                    SettingsSwitchRow("Prices", settings.showPrices) { onChange(settings.copy(showPrices = it)) }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsSwitchRow("Levels", settings.showLevels) { onChange(settings.copy(showLevels = it)) }
                    Spacer(Modifier.width(10.dp))
                    SegmentedChoice(
                        options = listOf("Values", "Percent"),
                        selectedIndex = if (settings.levelsFormatValues) 0 else 1
                    ) { idx -> onChange(settings.copy(levelsFormatValues = idx == 0)) }
                    Spacer(Modifier.width(10.dp))
                    SegmentedChoice(
                        options = listOf("Left", "Right"),
                        selectedIndex = if (settings.labelsPositionLeft) 0 else 1
                    ) { idx -> onChange(settings.copy(labelsPositionLeft = idx == 0)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fill opacity", color = Color(0xFF787B86), fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = settings.backgroundTransparency.toFloat(),
                        onValueChange = { onChange(settings.copy(backgroundTransparency = it.toInt())) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF2962FF), activeTrackColor = Color(0xFF2962FF))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${settings.backgroundTransparency}", color = Color.White, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))

                SettingsSectionTitle("Levels")
                settings.levels.forEachIndexed { index, lvl ->
                    LevelRow(index, lvl, settings, onChange)
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onChange(AutoFibSettings()) }) { Text("Reset", color = Color(0xFFF23645)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                    ) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun LevelRow(index: Int, lvl: AutoFibLevelState, settings: AutoFibSettings, onChange: (AutoFibSettings) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = lvl.shown,
            onCheckedChange = { shown ->
                val updated = settings.levels.mapIndexed { i, l ->
                    if (i == index) AutoFibLevelState(shown, l.ratio, l.colorInt) else l
                }
                onChange(settings.copy(levels = updated))
            },
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2962FF))
        )
        Text("${lvl.ratio}", color = Color(0xFFD1D4DC), fontSize = 13.sp, modifier = Modifier.width(64.dp))
        Spacer(Modifier.weight(1f))
        val paletteIdx = AUTO_FIB_PALETTE.indexOfFirst { android.graphics.Color.parseColor(it) == lvl.colorInt }
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color(android.graphics.Color.parseColor(AUTO_FIB_PALETTE[(paletteIdx.coerceAtLeast(0))])), CircleShape)
                .border(1.dp, Color(0xFF363A45), CircleShape)
                .clickable {
                    val next = ((paletteIdx.coerceAtLeast(0)) + 1) % AUTO_FIB_PALETTE.size
                    val newColor = android.graphics.Color.parseColor(AUTO_FIB_PALETTE[next])
                    val updated = settings.levels.mapIndexed { i, l ->
                        if (i == index) AutoFibLevelState(l.shown, l.ratio, newColor) else l
                    }
                    onChange(settings.copy(levels = updated))
                }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(text, color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2962FF))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color(0xFFD1D4DC), fontSize = 13.sp)
    }
}

@Composable
private fun SettingsNumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    Column {
        Text(label, color = Color(0xFFD1D4DC), fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(96.dp).height(44.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            decorationBox = { innerTextField -> Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) { innerTextField() } }
        )
    }
}

@Composable
private fun SegmentedChoice(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row {
        options.forEachIndexed { idx, opt ->
            val selected = idx == selectedIndex
            Text(
                opt,
                color = if (selected) Color.White else Color(0xFF787B86),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) Color(0xFF2962FF) else Color(0xFF2A2E39))
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            if (idx < options.lastIndex) Spacer(Modifier.width(4.dp))
        }
    }
}
