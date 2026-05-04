package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.ColorPickerState

@Composable
fun BBIndicatorSettingsModal(
    period: Int,
    onPeriodChange: (Int) -> Unit,
    stdDev: Float,
    onStdDevChange: (Float) -> Unit,
    lineColor: Color,
    onLineColorChange: (Color) -> Unit,
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    showLines: Boolean,
    onShowLinesChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inputs", "Style", "Visibility")

    var currentPeriod by remember { mutableIntStateOf(period) }
    var currentStdDev by remember { mutableFloatStateOf(stdDev) }
    var currentLineColor by remember { mutableStateOf(lineColor) }
    var currentShowLabels by remember { mutableStateOf(showLabels) }
    var currentShowLines by remember { mutableStateOf(showLines) }
    var colorPickerTarget by remember { mutableStateOf<ColorPickerState?>(null) }

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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bollinger Bands".uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF787B86),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    edgePadding = 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            val currentTab = tabPositions[selectedTab]
                            TabRowDefaults.Indicator(
                                Modifier
                                    .tabIndicatorOffset(currentTab)
                                    .padding(end = 24.dp),
                                height = 2.dp,
                                color = Color.White
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clickable { selectedTab = index }
                                .padding(end = 24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                title,
                                color = if (selectedTab == index) Color.White else Color(0xFF787B86),
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Divider(
                    color = Color(0xFF2A2E39),
                    thickness = 3.0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (selectedTab) {
                        0 -> BBInputsTab(
                            period = currentPeriod,
                            onPeriodChange = { currentPeriod = it },
                            stdDev = currentStdDev,
                            onStdDevChange = { currentStdDev = it }
                        )
                        1 -> BBStyleTab(
                            lineColor = currentLineColor,
                            onLineColorChange = {
                                colorPickerTarget = ColorPickerState(
                                    title = "Bollinger Bands",
                                    initialHex = String.format("#%06X", (0xFFFFFF and currentLineColor.toArgb())),
                                    onColorSelect = { currentLineColor = parseComposeColor(it) }
                                )
                            },
                            showLabels = currentShowLabels,
                            onShowLabelsChange = { currentShowLabels = it },
                            showLines = currentShowLines,
                            onShowLinesChange = { currentShowLines = it }
                        )
                        2 -> VisibilityTab()
                    }
                }

                Divider(color = Color(0xFF2A2E39))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 38.dp)
                            .border(1.dp, Color(0xFF434651), RoundedCornerShape(8.dp))
                            .clickable { /* More options */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = Color(0xFFD1D4DC),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onClose,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(1.dp, Color(0xFF434651), RoundedCornerShape(8.dp))
                                .height(38.dp)
                        ) {
                            Text("Cancel", color = Color(0xFFD1D4DC), fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                onPeriodChange(currentPeriod)
                                onStdDevChange(currentStdDev)
                                onLineColorChange(currentLineColor)
                                onShowLabelsChange(currentShowLabels)
                                onShowLinesChange(currentShowLines)
                                onClose()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                        ) {
                            Text("Ok", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
    }
}

@Composable
fun BBInputsTab(
    period: Int, 
    onPeriodChange: (Int) -> Unit,
    stdDev: Float,
    onStdDevChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsNumericInput(label = "Length", value = period, onValueChange = onPeriodChange)
        SettingsDropdown(label = "Source", selected = "Close", options = listOf("Open", "High", "Low", "Close"), onSelect = {})
        SettingsFloatInput(label = "StdDev", value = stdDev, onValueChange = onStdDevChange)
        SettingsNumericInput(label = "Offset", value = 0, onValueChange = {})
    }
}

@Composable
fun BBStyleTab(
    lineColor: Color,
    onLineColorChange: () -> Unit,
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    showLines: Boolean,
    onShowLinesChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsCheckbox(label = "Basis", checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
            ColorOpacityBox(color = lineColor, onClick = onLineColorChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsCheckbox(label = "Upper", checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
            ColorOpacityBox(color = lineColor, onClick = onLineColorChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsCheckbox(label = "Lower", checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
            ColorOpacityBox(color = lineColor, onClick = onLineColorChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsCheckbox(label = "Background", checked = true, onCheckedChange = {}, modifier = Modifier.weight(1f))
            ColorOpacityBox(color = lineColor.copy(alpha = 0.1f), onClick = onLineColorChange)
        }
        SettingsCheckbox(label = "Labels", checked = showLabels, onCheckedChange = onShowLabelsChange)
        SettingsCheckbox(label = "Lines", checked = showLines, onCheckedChange = onShowLinesChange)
    }
}
