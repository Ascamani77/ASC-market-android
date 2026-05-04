package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.ChartSettings
import com.trading.app.models.ColorPickerState
import com.trading.app.models.SymbolSettings
import java.util.*

private val TRADING_VIEW_COLORS = listOf(
    "#ffffff", "#d1d4dc", "#b2b5be", "#868993", "#5d606b", "#434651", "#363a45", "#2a2e39", "#131722", "#000000",
    "#ff5252", "#ff9800", "#ffeb3b", "#4caf50", "#00bcd4", "#2196f3", "#3f51b5", "#9c27b0", "#e91e63", "#795548",
    "#ffebee", "#fff3e0", "#fffde7", "#e8f5e9", "#e0f7fa", "#e3f2fd", "#e8eaf6", "#f3e5f5", "#fce4ec", "#efebe9",
    "#ffcdd2", "#ffe0b2", "#fff9c4", "#c8e6c9", "#b2ebf2", "#bbdefb", "#c5cae9", "#e1bee7", "#f8bbd0", "#d7ccc8",
    "#ef9a9a", "#ffcc80", "#fff59d", "#a5d6a7", "#80deea", "#90caf9", "#9fa8da", "#ce93d8", "#f48fb1", "#bcaaa4",
    "#e57373", "#ffb74d", "#fff176", "#81c784", "#4dd0e1", "#64b5f6", "#7986cb", "#ba68c8", "#f06292", "#a1887f",
    "#ef5350", "#ffa726", "#ffee58", "#66bb6a", "#26c6da", "#42a5f5", "#5c6bc0", "#ab47bc", "#ec407a", "#8d6e63",
    "#f44336", "#fb8c00", "#fdd835", "#43a047", "#00acc1", "#1e88e5", "#3949ab", "#8e24aa", "#d81b60", "#6d4c41",
    "#d32f2f", "#f57c00", "#fbc02d", "#388e3c", "#0097a7", "#1976d2", "#303f9f", "#7b1fa2", "#c2185b", "#5d4037",
    "#c62828", "#ef6c00", "#f9a825", "#2e7d32", "#00838f", "#1565c0", "#283593", "#6a1b9a", "#ad1457", "#4e342e",
    "#b71c1c", "#e65100", "#f57f17", "#1b5e20", "#006064", "#0d47a1", "#1a237e", "#4a148c", "#880e4f", "#3e2723"
)

private fun parseColor(colorString: String?, defaultColor: Color = Color.Gray): Color {
    if (colorString.isNullOrBlank()) return defaultColor
    return try {
        if (colorString.startsWith("rgba", ignoreCase = true)) {
            val parts = colorString.substringAfter("(").substringBefore(")").split(",")
            val r = parts[0].trim().toInt() / 255f
            val g = parts[1].trim().toInt() / 255f
            val b = parts[2].trim().toInt() / 255f
            val a = parts.getOrNull(3)?.trim()?.toFloat() ?: 1f
            Color(r, g, b, a)
        } else if (colorString.startsWith("#")) {
            Color(android.graphics.Color.parseColor(colorString))
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        defaultColor
    }
}

private fun colorToHex(color: Color): String {
    return String.format("#%02x%02x%02x", 
        (color.red * 255).toInt(), 
        (color.green * 255).toInt(), 
        (color.blue * 255).toInt()
    )
}

@Composable
fun SymbolSettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onTimeZoneClick: () -> Unit,
    onClose: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings.symbol) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerState?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            "Symbol",
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
                            "BARS",
                            color = Color(0xFF787B86),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        SimpleCheckbox(
                            label = "Color bars based on previous close",
                            checked = tempSettings.barColorer,
                            onCheckedChange = { tempSettings = tempSettings.copy(barColorer = it) }
                        )

                        SimpleCheckbox(
                            label = "HLC bars",
                            checked = tempSettings.hlcBars,
                            onCheckedChange = { tempSettings = tempSettings.copy(hlcBars = it) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ColorSettingRow(
                            label = "Up color",
                            color = tempSettings.upColor,
                            onColorClick = {
                                colorPickerTarget = ColorPickerState("Up color", tempSettings.upColor) {
                                    tempSettings = tempSettings.copy(upColor = it)
                                }
                            }
                        )

                        ColorSettingRow(
                            label = "Down color",
                            color = tempSettings.downColor,
                            onColorClick = {
                                colorPickerTarget = ColorPickerState("Down color", tempSettings.downColor) {
                                    tempSettings = tempSettings.copy(downColor = it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SimpleCheckbox(
                            label = "Thin bars",
                            checked = tempSettings.thinBars,
                            onCheckedChange = { tempSettings = tempSettings.copy(thinBars = it) }
                        )

                        Text(
                            "DATA MODIFICATION",
                            color = Color(0xFF787B86),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                        )

                        SettingsDropdown(
                            label = "Precision",
                            value = tempSettings.precision,
                            onClick = { /* Open precision dropdown */ }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsDropdown(
                            label = "Timezone",
                            value = tempSettings.timezone,
                            onClick = onTimeZoneClick
                        )
                    }

                    // Footer
                    Surface(
                        color = Color.Black,
                        border = BorderStroke(0.5.dp, Color(0xFF2A2E39))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF2A2E39), RoundedCornerShape(8.dp))
                                    .clickable { /* More options */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
                            }

                            Row {
                                Button(
                                    onClick = onClose,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E39)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp)
                                ) {
                                    Text("Cancel", color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        onUpdate(settings.copy(symbol = tempSettings))
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp)
                                ) {
                                    Text("Ok", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    colorPickerTarget?.let { target ->
        ColorPickerModal(
            title = target.title,
            initialColor = target.initialHex,
            onColorSelected = {
                target.onColorSelect(it)
                colorPickerTarget = null
            },
            onClose = { colorPickerTarget = null }
        )
    }
}

@Composable
private fun ColorSettingRow(
    label: String,
    color: String,
    onColorClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.width(100.dp)
        )
        ColorBox(color = color, onClick = onColorClick)
    }
}

@Composable
private fun SimpleCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = Color(0xFF434651),
                checkmarkColor = Color.Black
            )
        )
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ColorBox(color: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(parseColor(color), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF434651), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsDropdown(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.width(100.dp)
        )
        Box(
            modifier = Modifier
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(8.dp))
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value, 
                    color = Color.White, 
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF787B86))
            }
        }
    }
}

@Composable
fun ColorPickerModal(
    title: String,
    initialColor: String,
    onColorSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var opacity by remember { mutableStateOf(100) }
    var showMixer by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            color = Color(0xFF121212),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (showMixer) {
                ColorMixer(
                    initialColor = parseColor(selectedColor),
                    onColorChange = { selectedColor = colorToHex(it) },
                    onAdd = { onColorSelected(selectedColor) },
                    onBack = { showMixer = false }
                )
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(10),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(TRADING_VIEW_COLORS) { color ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(parseColor(color))
                                    .border(
                                        if (selectedColor.lowercase() == color.lowercase()) 2.dp else 0.dp,
                                        Color.White
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Plus Icon for Mixer
                    IconButton(
                        onClick = { showMixer = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Opacity", color = Color(0xFF787B86), fontSize = 12.sp)
                    Slider(
                        value = opacity.toFloat(),
                        onValueChange = { opacity = it.toInt() },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF2196F3)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onClose) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = { onColorSelected(selectedColor) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Apply", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorMixer(
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    var hsv by remember { 
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.argb(
            (initialColor.alpha * 255).toInt(),
            (initialColor.red * 255).toInt(),
            (initialColor.green * 255).toInt(),
            (initialColor.blue * 255).toInt()
        ), hsvArr)
        mutableStateOf(Triple(hsvArr[0], hsvArr[1], hsvArr[2])) 
    }

    val currentColor = Color.hsv(hsv.first, hsv.second, hsv.third)

    Column(modifier = Modifier.padding(16.dp)) {
        // Mixer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(currentColor, RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFF2A2E39),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(32.dp).width(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(colorToHex(currentColor), color = Color.White, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Add", color = Color.Black, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saturation-Value Picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newS = (hsv.second + dragAmount.x / size.width).coerceIn(0f, 1f)
                        val newV = (hsv.third - dragAmount.y / size.height).coerceIn(0f, 1f)
                        hsv = Triple(hsv.first, newS, newV)
                        onColorChange(Color.hsv(hsv.first, newS, newV))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val saturationGradient = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.hsv(hsv.first, 1f, 1f))
                )
                val valueGradient = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
                drawRect(saturationGradient)
                drawRect(valueGradient)

                // Cursor
                val cursorX = hsv.second * size.width
                val cursorY = (1f - hsv.third) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(cursorX, cursorY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hue Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newH = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            hsv = Triple(newH, hsv.second, hsv.third)
                            onColorChange(Color.hsv(newH, hsv.second, hsv.third))
                        }
                    }
            ) {
                val hueColors = List(361) { Color.hsv(it.toFloat(), 1f, 1f) }
                drawRect(Brush.horizontalGradient(hueColors))
                
                // Slider Handle
                val handleX = (hsv.first / 360f) * size.width
                drawRect(
                    color = Color.White,
                    topLeft = Offset(handleX - 2.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Back", color = Color(0xFF787B86))
        }
    }
}
