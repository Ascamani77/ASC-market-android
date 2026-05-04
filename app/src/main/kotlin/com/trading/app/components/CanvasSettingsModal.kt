package com.trading.app.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.ChartSettings
import com.trading.app.models.ColorPickerState

@Composable
fun CanvasSettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onClose: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings.canvas) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerState?>(null) }
    val sizeOptions = remember { (1..40).map { it.toString() } }

    // Apply changes in real-time to the chart
    LaunchedEffect(tempSettings) {
        onUpdate(settings.copy(canvas = tempSettings))
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
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
                        "Canvas",
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
                    // CHART BASIC STYLES
                    CanvasSectionHeader("CHART BASIC STYLES")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Background", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = tempSettings.backgroundType,
                                options = listOf("Solid", "Gradient"),
                                onValueChange = { tempSettings = tempSettings.copy(backgroundType = it) },
                                modifier = Modifier.width(130.dp)
                            )
                            CanvasColorBox(tempSettings.background) {
                                colorPickerTarget = ColorPickerState("Background Color", tempSettings.background) { 
                                    tempSettings = tempSettings.copy(background = it) 
                                }
                            }
                            if (tempSettings.backgroundType == "Gradient") {
                                CanvasColorBox(tempSettings.backgroundGradientEnd) {
                                    colorPickerTarget = ColorPickerState("Gradient End Color", tempSettings.backgroundGradientEnd) { 
                                        tempSettings = tempSettings.copy(backgroundGradientEnd = it) 
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid lines", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = tempSettings.gridType,
                                options = listOf("Vert and horz", "Vert", "Horz", "None"),
                                onValueChange = { tempSettings = tempSettings.copy(gridType = it) },
                                modifier = Modifier.width(130.dp)
                            )
                            CanvasColorBox(tempSettings.gridColor) {
                                colorPickerTarget = ColorPickerState("Grid Color", tempSettings.gridColor) { 
                                    tempSettings = tempSettings.copy(gridColor = it) 
                                }
                            }
                            CanvasColorBox(tempSettings.horzGridColor) {
                                colorPickerTarget = ColorPickerState("Grid Color", tempSettings.horzGridColor) { 
                                    tempSettings = tempSettings.copy(horzGridColor = it) 
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Opacity", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Slider(
                            value = tempSettings.gridOpacity.toFloat(),
                            onValueChange = { tempSettings = tempSettings.copy(gridOpacity = it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF2962FF),
                                inactiveTrackColor = Color(0xFF363A45)
                            )
                        )
                        Text("${tempSettings.gridOpacity}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(40.dp).padding(start = 8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Crosshair", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        CrosshairPreviewBox(
                            color = tempSettings.crosshairColor,
                            thickness = tempSettings.crosshairThickness,
                            style = tempSettings.crosshairLineStyle,
                            onClick = {
                                colorPickerTarget = ColorPickerState(
                                    title = "Crosshair",
                                    initialHex = tempSettings.crosshairColor,
                                    isCrosshair = true,
                                    initialThickness = tempSettings.crosshairThickness,
                                    initialLineStyle = getLineStyleIndex(tempSettings.crosshairLineStyle),
                                    onCrosshairUpdate = { color, thickness, styleIndex ->
                                        tempSettings = tempSettings.copy(
                                            crosshairColor = color,
                                            crosshairThickness = thickness,
                                            crosshairLineStyle = getLineStyleText(styleIndex)
                                        )
                                    },
                                    onColorSelect = { color ->
                                        tempSettings = tempSettings.copy(crosshairColor = color)
                                    }
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Watermark", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = tempSettings.watermarkType,
                                options = listOf("Symbol", "Replay mode", "None"),
                                onValueChange = { tempSettings = tempSettings.copy(watermarkType = it) },
                                modifier = Modifier.width(130.dp)
                            )
                            CanvasColorBox(tempSettings.watermarkColor) {
                                colorPickerTarget = ColorPickerState("Watermark Color", tempSettings.watermarkColor) { 
                                    tempSettings = tempSettings.copy(watermarkColor = it) 
                                }
                            }
                        }
                    }

                    // SCALES
                    CanvasSectionHeader("SCALES")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Text", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasColorBox(tempSettings.scaleTextColor) {
                                colorPickerTarget = ColorPickerState("Scale Text Color", tempSettings.scaleTextColor) { 
                                    tempSettings = tempSettings.copy(scaleTextColor = it) 
                                }
                            }
                            CanvasDropdown(
                                value = "${tempSettings.scaleFontSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(scaleFontSize = it.toIntOrNull() ?: 11) },
                                modifier = Modifier.width(80.dp)
                            )
                            IconButton(
                                onClick = { tempSettings = tempSettings.copy(scaleFontBold = !tempSettings.scaleFontBold) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatBold,
                                    null,
                                    tint = if (tempSettings.scaleFontBold) Color(0xFF2962FF) else Color.White
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lines", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        CanvasColorBox(tempSettings.scaleLineColor) {
                            colorPickerTarget = ColorPickerState("Scale Line Color", tempSettings.scaleLineColor) { 
                                tempSettings = tempSettings.copy(scaleLineColor = it) 
                                }
                        }
                    }

                    // UI ELEMENTS TEXT SETTINGS
                    CanvasSectionHeader("UI ELEMENTS TEXT & SIZE")
                    
                    // Chart Items Font Size
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chart Items", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = "${tempSettings.chartItemFontSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(chartItemFontSize = it.toIntOrNull() ?: 12) },
                                modifier = Modifier.width(80.dp)
                            )
                            Text("px", color = Color(0xFF787B86), fontSize = 14.sp)
                        }
                    }

                    // Asset Pair Font Size
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Asset Pair", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = "${tempSettings.symbolFontSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(symbolFontSize = it.toIntOrNull() ?: 14) },
                                modifier = Modifier.width(80.dp)
                            )
                            Text("px", color = Color(0xFF787B86), fontSize = 14.sp)
                        }
                    }

                    // Header Settings
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Header", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = tempSettings.headerVisibility,
                                options = listOf("Always visible", "Auto-hide"),
                                onValueChange = { tempSettings = tempSettings.copy(headerVisibility = it) },
                                modifier = Modifier.width(130.dp)
                            )
                            CanvasDropdown(
                                value = "${tempSettings.headerFontSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(headerFontSize = it.toIntOrNull() ?: 14) },
                                modifier = Modifier.width(80.dp)
                            )
                            IconButton(
                                onClick = { tempSettings = tempSettings.copy(headerFontBold = !tempSettings.headerFontBold) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatBold,
                                    null,
                                    tint = if (tempSettings.headerFontBold) Color(0xFF2962FF) else Color.White
                                )
                            }
                        }
                    }

                    // Bottom Bar Settings
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bottom Bar", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = "${tempSettings.bottomFontSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(bottomFontSize = it.toIntOrNull() ?: 13) },
                                modifier = Modifier.width(80.dp)
                            )
                            IconButton(
                                onClick = { tempSettings = tempSettings.copy(bottomFontBold = !tempSettings.bottomFontBold) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatBold,
                                    null,
                                    tint = if (tempSettings.bottomFontBold) Color(0xFF2962FF) else Color.White
                                )
                            }
                        }
                    }

                    // Sidebar Settings
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sidebar Icon", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = "${tempSettings.sidebarIconSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(sidebarIconSize = it.toIntOrNull() ?: 24) },
                                modifier = Modifier.width(80.dp)
                            )
                            Text("px", color = Color(0xFF787B86), fontSize = 14.sp)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sidebar Font", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CanvasDropdown(
                                value = "${tempSettings.sidebarFontSize}",
                                options = sizeOptions,
                                onValueChange = { tempSettings = tempSettings.copy(sidebarFontSize = it.toIntOrNull() ?: 15) },
                                modifier = Modifier.width(80.dp)
                            )
                            IconButton(
                                onClick = { tempSettings = tempSettings.copy(sidebarFontBold = !tempSettings.sidebarFontBold) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.FormatBold,
                                    null,
                                    tint = if (tempSettings.sidebarFontBold) Color(0xFF2962FF) else Color.White
                                )
                            }
                        }
                    }

                    // Swap Settings
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Swap Header/Footer", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = tempSettings.swapHeaderAndFooter,
                            onCheckedChange = { tempSettings = tempSettings.copy(swapHeaderAndFooter = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2962FF),
                                uncheckedThumbColor = Color(0xFF787B86),
                                uncheckedTrackColor = Color(0xFF121212)
                            )
                        )
                    }

                    // BUTTONS
                    CanvasSectionHeader("BUTTONS VISIBILITY")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Navigation", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        CanvasDropdown(
                            value = tempSettings.navigationButtons,
                            options = listOf("Always visible", "Visible on tap", "Always invisible"),
                            onValueChange = { tempSettings = tempSettings.copy(navigationButtons = it) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pane", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                        CanvasDropdown(
                            value = tempSettings.paneButtons,
                            options = listOf("Always visible", "Visible on tap", "Always invisible"),
                            onValueChange = { tempSettings = tempSettings.copy(paneButtons = it) },
                            modifier = Modifier.width(160.dp)
                        )
                    }

                    // MARGINS
                    CanvasSectionHeader("MARGINS")
                    
                    MarginInputRow("Top", tempSettings.marginTop) { tempSettings = tempSettings.copy(marginTop = it) }
                    MarginInputRow("Bottom", tempSettings.marginBottom) { tempSettings = tempSettings.copy(marginBottom = it) }
                    MarginInputRow("Right", tempSettings.marginRight) { tempSettings = tempSettings.copy(marginRight = it) }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Footer
                Surface(
                    color = Color(0xFF131722),
                    border = BorderStroke(1.dp, Color(0xFF2A2E39))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp, 36.dp)
                                .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp))
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color(0xFF2A2E39)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Cancel", color = Color.White, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { 
                                    onUpdate(settings.copy(canvas = tempSettings))
                                    onClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Ok", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    colorPickerTarget?.let { state ->
        ColorPickerDialog(
            state = state,
            onClose = { colorPickerTarget = null }
        )
    }
}

@Composable
private fun MarginInputRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var displayValue by remember(value) { mutableStateOf("$value") }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.width(100.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = displayValue,
                onValueChange = { newInput ->
                    displayValue = newInput
                    val filtered = newInput.filter { char -> char.isDigit() }
                    if (filtered.isNotEmpty()) {
                        val newVal = filtered.toInt()
                        onValueChange(newVal)
                    }
                },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier
                    .width(60.dp)
                    .height(36.dp)
                    .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("%", color = Color(0xFF787B86), fontSize = 14.sp)
        }
    }
}

@Composable
private fun CanvasSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF787B86),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun CanvasDropdown(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF121212), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, color = Color.White, fontSize = 13.sp, maxLines = 1)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF121212)).border(1.dp, Color(0xFF434651))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White, fontSize = 14.sp) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CanvasColorBox(hex: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(parseCanvasColor(hex))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
    )
}

@Composable
private fun CrosshairPreviewBox(color: String, thickness: Int, style: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(32.dp)
            .background(Color(0xFF121212), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Color box
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(parseCanvasColor(color))
            )
            // Line preview
            androidx.compose.foundation.Canvas(modifier = Modifier.weight(1f).height(1.dp)) {
                val effect = when (style) {
                    "Dashed" -> PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    "Dotted" -> PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)
                    else -> null
                }
                drawLine(
                    color = Color.White,
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = thickness.toFloat(),
                    pathEffect = effect
                )
            }
        }
    }
}

private fun getLineStyleText(style: Int): String {
    return when (style) {
        0 -> "Solid"
        1 -> "Dashed"
        2 -> "Dotted"
        else -> "Solid"
    }
}

private fun getLineStyleIndex(style: String): Int {
    return when (style) {
        "Solid" -> 0
        "Dashed" -> 1
        "Dotted" -> 2
        else -> 0
    }
}

private fun parseCanvasColor(hex: String): Color {
    return try {
        if (hex.startsWith("#")) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
