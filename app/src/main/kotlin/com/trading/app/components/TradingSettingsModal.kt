package com.trading.app.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.ChartSettings
import com.trading.app.models.TradingSettings

@Composable
fun TradingSettingsModal(
    settings: ChartSettings,
    onUpdate: (ChartSettings) -> Unit,
    onClose: () -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings.trading) }

    // Apply changes in real-time to the chart
    LaunchedEffect(tempSettings) {
        onUpdate(settings.copy(trading = tempSettings))
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
                        "Trading",
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
                    // GENERAL
                    SectionHeader("GENERAL")
                    
                    TradingCheckboxWithDescription(
                        label = "One-tap trading",
                        description = "Instantly place, edit, cancel orders or close positions without confirmation",
                        checked = tempSettings.oneClickTrading,
                        onCheckedChange = { tempSettings = tempSettings.copy(oneClickTrading = it) },
                        showHelp = true
                    )
                    
                    TradingCheckboxRow(
                        label = "Show only rejection notifications",
                        checked = tempSettings.rejectionNotifications,
                        onCheckedChange = { tempSettings = tempSettings.copy(rejectionNotifications = it) }
                    )

                    // APPEARANCE
                    SectionHeader("APPEARANCE")
                    
                    TradingCheckboxRow(
                        label = "Buy/Sell buttons",
                        checked = tempSettings.buySellButtons,
                        onCheckedChange = { tempSettings = tempSettings.copy(buySellButtons = it) }
                    )

                    TradingCheckboxRow(
                        label = "Buy/Sell labels on chart",
                        checked = tempSettings.showBuySellLabels,
                        onCheckedChange = { tempSettings = tempSettings.copy(showBuySellLabels = it) }
                    )

                    TradingCheckboxRow(
                        label = "Positions and orders",
                        checked = tempSettings.positionsAndOrders,
                        onCheckedChange = { tempSettings = tempSettings.copy(positionsAndOrders = it) },
                        showInfo = true
                    )
                    
                    if (tempSettings.positionsAndOrders) {
                        Column(modifier = Modifier.padding(start = 32.dp)) {
                            TradingCheckboxRow(
                                label = "Reverse position button",
                                checked = tempSettings.reversePositionButton,
                                onCheckedChange = { tempSettings = tempSettings.copy(reversePositionButton = it) }
                            )
                            Text(
                                "Adds the reverse button next to the open position on the chart",
                                color = Color(0xFF787B86),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    
                    TradingCheckboxWithDescription(
                        label = "Project order for market orders",
                        description = "Shows a project order on the chart before sending a market order",
                        checked = tempSettings.projectOrder,
                        onCheckedChange = { tempSettings = tempSettings.copy(projectOrder = it) }
                    )
                    
                    TradingCheckboxRow(
                        label = "Profit and loss value",
                        checked = tempSettings.profitLossValue,
                        onCheckedChange = { tempSettings = tempSettings.copy(profitLossValue = it) },
                        showHelp = true
                    )
                    
                    if (tempSettings.profitLossValue) {
                        Column(modifier = Modifier.padding(start = 32.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Checkbox(checked = true, onCheckedChange = {}, colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black))
                                Text("Positions", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                TradingSmallDropdown(tempSettings.positionsMode, listOf("Money", "Percentage", "Ticks")) { tempSettings = tempSettings.copy(positionsMode = it) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Checkbox(checked = true, onCheckedChange = {}, colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black))
                                Text("Brackets", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                TradingSmallDropdown(tempSettings.bracketsMode, listOf("Money", "Percentage", "Ticks")) { tempSettings = tempSettings.copy(bracketsMode = it) }
                            }
                        }
                    }
                    
                    TradingCheckboxRow(
                        label = "Execution marks",
                        checked = tempSettings.executionMarks,
                        onCheckedChange = { tempSettings = tempSettings.copy(executionMarks = it) },
                        showInfo = true
                    )
                    
                    if (tempSettings.executionMarks) {
                        TradingCheckboxRow(
                            label = "Execution labels",
                            checked = tempSettings.executionLabels,
                            onCheckedChange = { tempSettings = tempSettings.copy(executionLabels = it) },
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                    
                    TradingCheckboxRow(
                        label = "Extended price lines across the entire chart width",
                        checked = tempSettings.extendedPriceLines,
                        onCheckedChange = { tempSettings = tempSettings.copy(extendedPriceLines = it) }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Order and position alignment", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TradingSmallDropdown(tempSettings.alignment, listOf("Right", "Left")) { tempSettings = tempSettings.copy(alignment = it) }
                    }
                    
                    TradingCheckboxWithDescription(
                        label = "Orders, executions and positions in screenshots",
                        description = "Shows your trades on the chart in screenshots",
                        checked = tempSettings.screenshotVisibility,
                        onCheckedChange = { tempSettings = tempSettings.copy(screenshotVisibility = it) }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(44.dp, 36.dp).border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp)).clickable { },
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
                                try {
                                    val updatedSettings = settings.copy(trading = tempSettings)
                                    onUpdate(updatedSettings)
                                    onClose()
                                } catch (e: Exception) {
                                    android.util.Log.e("TradingSettings", "Failed to apply settings changes", e)
                                    onClose()
                                }
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

@Composable
fun TradingCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showHelp: Boolean = false,
    showInfo: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color(0xFF434651), checkmarkColor = Color.Black)
        )
        Text(label, color = Color.White, fontSize = 14.sp)
        if (showHelp) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Outlined.HelpOutline, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
        }
        if (showInfo) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Outlined.Info, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun TradingCheckboxWithDescription(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showHelp: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        TradingCheckboxRow(label, checked, onCheckedChange, showHelp)
        Text(
            description,
            color = Color(0xFF787B86),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun TradingSmallDropdown(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(36.dp)
                .border(1.dp, Color(0xFF363A45), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.White, fontSize = 13.sp)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF787B86), modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF121212)).border(1.dp, Color(0xFF363A45))
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
