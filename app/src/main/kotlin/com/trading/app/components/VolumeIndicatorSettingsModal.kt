package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.toArgb

import com.trading.app.models.ColorPickerState

@Composable
fun VolumeIndicatorSettingsModal(
    maLength: Int,
    onMaLengthChange: (Int) -> Unit,
    showMa: Boolean,
    onShowMaChange: (Boolean) -> Unit,
    colorBasedOnPreviousClose: Boolean,
    onColorBasedOnPreviousCloseChange: (Boolean) -> Unit,
    maColor: Color,
    onMaColorChange: (Color) -> Unit,
    growingColor: Color,
    onGrowingColorChange: (Color) -> Unit,
    fallingColor: Color,
    onFallingColorChange: (Color) -> Unit,
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    showLines: Boolean,
    onShowLinesChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inputs", "Style", "Visibility")

    var currentMaLength by remember { mutableIntStateOf(maLength) }
    var currentShowMa by remember { mutableStateOf(showMa) }
    var currentColorBasedOnPrevClose by remember { mutableStateOf(colorBasedOnPreviousClose) }

    var currentMaColor by remember { mutableStateOf(maColor) }
    var currentGrowingColor by remember { mutableStateOf(growingColor) }
    var currentFallingColor by remember { mutableStateOf(fallingColor) }

    var currentShowLabels by remember { mutableStateOf(showLabels) }
    var currentShowLines by remember { mutableStateOf(showLines) }

    var currentMaThickness by remember { mutableIntStateOf(1) }
    var currentMaLineStyle by remember { mutableIntStateOf(0) }
    var currentGrowingThickness by remember { mutableIntStateOf(1) }
    var currentFallingThickness by remember { mutableIntStateOf(1) }

    var currentVolumeStyle by remember { mutableStateOf("columns") }
    var currentVolumeMaStyle by remember { mutableStateOf("line") }
    var showVolumeStyleModal by remember { mutableStateOf(false) }
    var currentStyleTarget by remember { mutableStateOf<String?>(null) } // "volume" or "volumeMa"

    // Style tab states
    var showVolume by remember { mutableStateOf(true) }
    var showValuesInStatusLine by remember { mutableStateOf(true) }
    var showInputsInStatusLine by remember { mutableStateOf(true) }
    var precision by remember { mutableStateOf("Default") }

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
                        text = "VOLUME",
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
                        0 -> VolumeInputsTab(
                            maLength = currentMaLength,
                            onMaLengthChange = { currentMaLength = it },
                            showMa = currentShowMa,
                            onShowMaChange = { currentShowMa = it },
                            colorBasedOnPreviousClose = currentColorBasedOnPrevClose,
                            onColorBasedOnPreviousCloseChange = { currentColorBasedOnPrevClose = it }
                        )
                        1 -> VolumeStyleTab(
                            maColor = currentMaColor,
                            growingColor = currentGrowingColor,
                            fallingColor = currentFallingColor,
                            onMaColorChange = {
                                colorPickerTarget = ColorPickerState(
                                    title = "Volume MA",
                                    initialHex = String.format("#%06X", (0xFFFFFF and currentMaColor.toArgb())),
                                    initialThickness = currentMaThickness,
                                    initialLineStyle = currentMaLineStyle,
                                    showLineStyle = true,
                                    onColorSelect = { currentMaColor = parseComposeColor(it) },
                                    onThicknessChange = { currentMaThickness = it },
                                    onLineStyleChange = { currentMaLineStyle = it }
                                )
                            },
                            onGrowingColorChange = {
                                colorPickerTarget = ColorPickerState(
                                    title = "Growing",
                                    initialHex = String.format("#%06X", (0xFFFFFF and currentGrowingColor.toArgb())),
                                    initialThickness = currentGrowingThickness,
                                    onColorSelect = { currentGrowingColor = parseComposeColor(it) },
                                    onThicknessChange = { currentGrowingThickness = it }
                                )
                            },
                            onFallingColorChange = {
                                colorPickerTarget = ColorPickerState(
                                    title = "Falling",
                                    initialHex = String.format("#%06X", (0xFFFFFF and currentFallingColor.toArgb())),
                                    initialThickness = currentFallingThickness,
                                    onColorSelect = { currentFallingColor = parseComposeColor(it) },
                                    onThicknessChange = { currentFallingThickness = it }
                                )
                            },
                            showVolume = showVolume,
                            onShowVolumeChange = { showVolume = it },
                            showMa = currentShowMa,
                            onShowMaChange = { currentShowMa = it },
                            showLabels = currentShowLabels,
                            onShowLabelsChange = { currentShowLabels = it },
                            showLines = currentShowLines,
                            onShowLinesChange = { currentShowLines = it },
                            showValuesInStatusLine = showValuesInStatusLine,
                            onShowValuesInStatusLineChange = { showValuesInStatusLine = it },
                            showInputsInStatusLine = showInputsInStatusLine,
                            onShowInputsInStatusLineChange = { showInputsInStatusLine = it },
                            precision = precision,
                            onPrecisionChange = { precision = it },
                            volumeStyle = currentVolumeStyle,
                            onVolumeStyleClick = {
                                currentStyleTarget = "volume"
                                showVolumeStyleModal = true
                            },
                            volumeMaStyle = currentVolumeMaStyle,
                            onVolumeMaStyleClick = {
                                currentStyleTarget = "volumeMa"
                                showVolumeStyleModal = true
                            },
                            showVolumeStyleModal = showVolumeStyleModal,
                            currentStyleTarget = currentStyleTarget,
                            currentVolumeStyle = currentVolumeStyle,
                            currentVolumeMaStyle = currentVolumeMaStyle,
                            onVolumeStyleChange = { currentVolumeStyle = it },
                            onVolumeMaStyleChange = { currentVolumeMaStyle = it },
                            onCloseVolumeStyleModal = { showVolumeStyleModal = false }
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
                                onMaLengthChange(currentMaLength)
                                onShowMaChange(currentShowMa)
                                onColorBasedOnPreviousCloseChange(currentColorBasedOnPrevClose)
                                onMaColorChange(currentMaColor)
                                onGrowingColorChange(currentGrowingColor)
                                onFallingColorChange(currentFallingColor)
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
fun VolumeInputsTab(
    maLength: Int,
    onMaLengthChange: (Int) -> Unit,
    showMa: Boolean,
    onShowMaChange: (Boolean) -> Unit,
    colorBasedOnPreviousClose: Boolean,
    onColorBasedOnPreviousCloseChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingsNumericInput("MA Length", maLength, onMaLengthChange)
        
        SettingsCheckbox(
            label = "Color based on previous close",
            checked = colorBasedOnPreviousClose,
            onCheckedChange = onColorBasedOnPreviousCloseChange
        )

        SettingsCheckbox(
            label = "Volume MA",
            checked = showMa,
            onCheckedChange = onShowMaChange
        )
    }
}

@Composable
fun VolumeStyleTab(
    maColor: Color,
    growingColor: Color,
    fallingColor: Color,
    onMaColorChange: () -> Unit,
    onGrowingColorChange: () -> Unit,
    onFallingColorChange: () -> Unit,
    showVolume: Boolean,
    onShowVolumeChange: (Boolean) -> Unit,
    showMa: Boolean,
    onShowMaChange: (Boolean) -> Unit,
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    showLines: Boolean,
    onShowLinesChange: (Boolean) -> Unit,
    showValuesInStatusLine: Boolean,
    onShowValuesInStatusLineChange: (Boolean) -> Unit,
    showInputsInStatusLine: Boolean,
    onShowInputsInStatusLineChange: (Boolean) -> Unit,
    precision: String,
    onPrecisionChange: (String) -> Unit,
    volumeStyle: String,
    onVolumeStyleClick: () -> Unit,
    volumeMaStyle: String,
    onVolumeMaStyleClick: () -> Unit,
    showVolumeStyleModal: Boolean,
    currentStyleTarget: String?,
    currentVolumeStyle: String,
    currentVolumeMaStyle: String,
    onVolumeStyleChange: (String) -> Unit,
    onVolumeMaStyleChange: (String) -> Unit,
    onCloseVolumeStyleModal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCheckbox("Volume", showVolume, onShowVolumeChange)
        
        Column(modifier = Modifier.padding(start = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Growing", color = Color(0xFFD1D4DC), fontSize = 16.sp, modifier = Modifier.width(100.dp))
                ColorOpacityBox(growingColor, onClick = onGrowingColorChange)
                Spacer(modifier = Modifier.width(8.dp))
                IconBox(getStyleIcon(volumeStyle), onClick = onVolumeStyleClick)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Falling", color = Color(0xFFD1D4DC), fontSize = 16.sp, modifier = Modifier.width(100.dp))
                ColorOpacityBox(fallingColor, onClick = onFallingColorChange)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsCheckbox("Volume MA", showMa, onShowMaChange, modifier = Modifier.width(128.dp))
            ColorOpacityBox(maColor, onClick = onMaColorChange)
            Spacer(modifier = Modifier.width(8.dp))
            IconBox(getStyleIcon(volumeMaStyle), onClick = onVolumeMaStyleClick)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OUTPUT VALUES", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsDropdown("Precision", precision, listOf("Default", "1", "2", "3", "4"), onPrecisionChange)
        
        SettingsCheckbox("Labels", showLabels, onShowLabelsChange)
        SettingsCheckbox("Lines", showLines, onShowLinesChange)
        SettingsCheckbox("Values in status line", showValuesInStatusLine, onShowValuesInStatusLineChange)

        Spacer(modifier = Modifier.height(24.dp))
        Text("INPUT VALUES", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCheckbox("Inputs in status line", showInputsInStatusLine, onShowInputsInStatusLineChange)
    }

    if (showVolumeStyleModal) {
        val currentStyle = if (currentStyleTarget == "volume") currentVolumeStyle else currentVolumeMaStyle
        ChartTypeModal(
            currentStyle = currentStyle,
            onStyleChange = { newStyle ->
                if (currentStyleTarget == "volume") {
                    onVolumeStyleChange(newStyle)
                } else {
                    onVolumeMaStyleChange(newStyle)
                }
            },
            onClose = onCloseVolumeStyleModal
        )
    }
}
