package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.trading.app.models.ColorPickerState

private val TRADING_VIEW_COLORS = listOf(
    "#ffffff", "#d1d4dc", "#b2b5be", "#868993", "#6a6d78", "#434651", "#363a45", "#2a2e39", "#131722", "#000000",
    "#ff5252", "#ff9800", "#ffeb3b", "#4caf50", "#00bcd4", "#2196f3", "#673ab7", "#9c27b0", "#e91e63", "#795548",
    "#ffcdd2", "#ffe0b2", "#fff9c4", "#c8e6c9", "#b2ebf2", "#bbdefb", "#d1c4e9", "#e1bee7", "#f8bbd0", "#d7ccc8",
    "#ef9a9a", "#ffcc80", "#fff59d", "#a5d6a7", "#80deea", "#90caf9", "#b39ddb", "#ce93d8", "#f48fb1", "#bcaaa4",
    "#e57373", "#ffb74d", "#fff176", "#81c784", "#4dd0e1", "#64b5f6", "#9575cd", "#ba68c8", "#f06292", "#a1887f",
    "#f44336", "#ffa726", "#ffee58", "#66bb6a", "#26c6da", "#42a5f5", "#7e57c2", "#ab47bc", "#ec407a", "#8d6e63",
    "#d32f2f", "#f57c00", "#fbc02d", "#388e3c", "#0097a7", "#1976d2", "#512da8", "#7b1fa2", "#c2185b", "#5d4037",
    "#b71c1c", "#e65100", "#f57f17", "#1b5e20", "#006064", "#0d47a1", "#311b92", "#4a148c", "#880e4f", "#3e2723"
)

@Composable
fun ColorPickerDialog(
    state: ColorPickerState,
    onClose: () -> Unit
) {
    var showMixer by remember { mutableStateOf(false) }
    var currentHex by remember { mutableStateOf(state.initialHex) }
    var currentThickness by remember { mutableStateOf(state.initialThickness) }
    var currentLineStyle by remember { mutableStateOf(state.initialLineStyle) }
    var opacity by remember { mutableFloatStateOf(100f) }

    // Update real-time when hex, thickness or line style changes
    LaunchedEffect(currentHex, currentThickness, currentLineStyle) {
        if (state.isCrosshair) {
            state.onCrosshairUpdate?.invoke(currentHex, currentThickness, currentLineStyle)
        } else {
            state.onColorSelect(currentHex)
            state.onThicknessChange?.invoke(currentThickness)
            state.onLineStyleChange?.invoke(currentLineStyle)
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF121212),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF363A45))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (showMixer) {
                    CustomColorMixer(
                        initialColor = parseComposeColor(currentHex),
                        onColorChange = { color ->
                            currentHex = String.format("#%02x%02x%02x", (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                        },
                        onBack = { showMixer = false },
                        onDone = { onClose() }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF787B86))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(10),
                        modifier = Modifier.height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(TRADING_VIEW_COLORS) { colorHex ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(parseComposeColor(colorHex))
                                    .border(
                                        width = if (currentHex.lowercase().takeLast(6) == colorHex.lowercase().removePrefix("#")) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .clickable {
                                        currentHex = colorHex
                                    }
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFF363A45),
                        thickness = 1.dp
                    )

                    // Add color button (Plus icon)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFF363A45), RoundedCornerShape(8.dp))
                            .clickable { showMixer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add color",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Opacity", color = Color(0xFF787B86), fontSize = 12.sp)
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF2962FF),
                            inactiveTrackColor = Color(0xFF363A45)
                        )
                    )

                    if (state.isCrosshair) {
                        CrosshairControls(
                            currentThickness = currentThickness,
                            onThicknessChange = { currentThickness = it },
                            currentLineStyle = currentLineStyle,
                            onLineStyleChange = { currentLineStyle = it }
                        )
                    } else {
                        if (state.showThickness) {
                            ThicknessControls(
                                currentThickness = currentThickness,
                                onThicknessChange = { currentThickness = it }
                            )
                        }
                        if (state.showLineStyle) {
                            LineStyleControls(
                                currentLineStyle = currentLineStyle,
                                onLineStyleChange = { currentLineStyle = it }
                            )
                        }
                    }

                    // Buttons are removed as per user request
                }
            }
        }
    }
}

@Composable
fun CustomColorMixer(
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    var hsv by remember { 
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (initialColor.red * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue * 255).toInt()
            ),
            hsvArr
        )
        mutableStateOf(Triple(hsvArr[0], hsvArr[1], hsvArr[2])) 
    }

    val currentColor = remember(hsv) {
        Color.hsv(hsv.first, hsv.second, hsv.third)
    }

    val hexString = remember(currentColor) {
        String.format("#%02x%02x%02x", (currentColor.red * 255).toInt(), (currentColor.green * 255).toInt(), (currentColor.blue * 255).toInt())
    }

    val density = LocalDensity.current

    Column {
        // Top Bar: Color Preview, Hex Input, Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(currentColor)
                    .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
            )

            BasicTextField(
                value = hexString,
                onValueChange = { },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                readOnly = true,
                cursorBrush = SolidColor(Color.White)
            )

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Add", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            val totalWidth = constraints.maxWidth.toFloat()
            val totalHeight = constraints.maxHeight.toFloat()

            Row(modifier = Modifier.fillMaxSize()) {
                // Saturation-Value Mixer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val s = (offset.x / totalWidth).coerceIn(0f, 1f)
                                val v = (1f - (offset.y / totalHeight)).coerceIn(0f, 1f)
                                hsv = Triple(hsv.first, s, v)
                                onColorChange(Color.hsv(hsv.first, s, v))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val s = (change.position.x / totalWidth).coerceIn(0f, 1f)
                                val v = (1f - (change.position.y / totalHeight)).coerceIn(0f, 1f)
                                hsv = Triple(hsv.first, s, v)
                                onColorChange(Color.hsv(hsv.first, s, v))
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(SolidColor(Color.hsv(hsv.first, 1f, 1f)))
                        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    }

                    // Selector (Round ball)
                    val selectorX = hsv.second
                    val selectorY = 1f - hsv.third
                    
                    Box(
                        modifier = Modifier
                            .offset { 
                                IntOffset(
                                    (selectorX * totalWidth).toInt() - density.run { 10.dp.roundToPx() },
                                    (selectorY * totalHeight).toInt() - density.run { 10.dp.roundToPx() }
                                )
                            }
                            .size(20.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Hue Slider
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                            )
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val h = (offset.y / totalHeight).coerceIn(0f, 1f) * 360f
                                hsv = Triple(h, hsv.second, hsv.third)
                                onColorChange(Color.hsv(h, hsv.second, hsv.third))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val h = (change.position.y / totalHeight).coerceIn(0f, 1f) * 360f
                                hsv = Triple(h, hsv.second, hsv.third)
                                onColorChange(Color.hsv(h, hsv.second, hsv.third))
                            }
                        }
                ) {
                    val hueY = hsv.first / 360f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, (hueY * totalHeight).toInt() - density.run { 2.dp.roundToPx() }) }
                            .height(4.dp)
                            .border(1.dp, Color.White, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("Back to palette", color = Color(0xFF2962FF))
        }
    }
}

@Composable
private fun ThicknessControls(
    currentThickness: Int,
    onThicknessChange: (Int) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text("Thickness", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(1, 2, 3, 4).forEach { t ->
            val selected = currentThickness == t
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) Color.White else Color(0xFF2A2E39))
                    .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
                    .clickable { onThicknessChange(t) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth(0.6f).height(t.dp)) {
                    drawRect(color = if (selected) Color.Black else Color.White)
                }
            }
        }
    }
}

@Composable
private fun LineStyleControls(
    currentLineStyle: Int,
    onLineStyleChange: (Int) -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Text("Line style", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(0, 1, 2).forEach { style ->
            val selected = currentLineStyle == style
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) Color.White else Color(0xFF2A2E39))
                    .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
                    .clickable { onLineStyleChange(style) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth(0.6f)) {
                    val centerY = size.height / 2f
                    val lineColor = if (selected) Color.Black else Color.White
                    val stroke = 2.dp.toPx()
                    when (style) {
                        0 -> drawLine(lineColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = stroke)
                        1 -> drawLine(lineColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                        2 -> drawLine(lineColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f), 0f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosshairControls(
    currentThickness: Int,
    onThicknessChange: (Int) -> Unit,
    currentLineStyle: Int,
    onLineStyleChange: (Int) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text("Thickness", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(1, 2, 3, 4).forEach { t ->
            val selected = currentThickness == t
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Color.White else Color(0xFF2A2E39))
                    .border(1.dp, Color(0xFF363A45), RoundedCornerShape(6.dp))
                    .clickable { onThicknessChange(t) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerY = size.height / 2f
                    val lineColor = if (selected) Color.Black else Color.White
                    drawLine(
                        color = lineColor,
                        start = Offset(16.dp.toPx(), centerY),
                        end = Offset(size.width - 16.dp.toPx(), centerY),
                        strokeWidth = t.dp.toPx()
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text("Line style", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(0, 1, 2).forEach { style ->
            val selected = currentLineStyle == style
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) Color.White else Color(0xFF2A2E39))
                    .border(1.dp, Color(0xFF363A45), RoundedCornerShape(6.dp))
                    .clickable { onLineStyleChange(style) },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerY = size.height / 2f
                    val lineColor = if (selected) Color.Black else Color.White
                    val stroke = 2.2f.dp.toPx()
                    when (style) {
                        0 -> drawLine(lineColor, Offset(16.dp.toPx(), centerY), Offset(size.width - 16.dp.toPx(), centerY), strokeWidth = stroke)
                        1 -> drawLine(lineColor, Offset(16.dp.toPx(), centerY), Offset(size.width - 16.dp.toPx(), centerY), strokeWidth = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                        2 -> drawLine(lineColor, Offset(16.dp.toPx(), centerY), Offset(size.width - 16.dp.toPx(), centerY), strokeWidth = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 6f), 0f))
                    }
                }
            }
        }
    }
}

fun parseComposeColor(hex: String): Color {
    return try {
        if (hex.startsWith("#")) {
            val colorString = if (hex.length == 7) "#FF" + hex.removePrefix("#") else hex
            Color(android.graphics.Color.parseColor(colorString))
        } else if (hex.startsWith("rgba")) {
             val parts = hex.substringAfter("(").substringBefore(")").split(",")
            val r = parts[0].trim().toInt()
            val g = parts[1].trim().toInt()
            val b = parts[2].trim().toInt()
            val a = parts.getOrNull(3)?.trim()?.toFloat() ?: 1f
            Color(r / 255f, g / 255f, b / 255f, a)
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
