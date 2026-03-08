package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.CustomAlert
import com.asc.markets.ui.screens.tradeDashboard.model.CustomAlertType

@Composable
fun CustomAlertsManager(
    symbol: String = "",
    alerts: List<CustomAlert> = emptyList(),
    onAddAlert: (CustomAlert) -> Unit = {},
    onRemoveAlert: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isAdding by remember { mutableStateOf(false) }
    val emeraldColor = Color(0xFF00C853)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = emeraldColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CUSTOM MT5 MONITORS",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .background(emeraldColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .clickable { isAdding = !isAdding }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAdding) "+ CANCEL" else "+ ADD MONITOR",
                    color = emeraldColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (isAdding) {
            AddMonitorForm(
                onInitialize = {
                    // Logic to create alert
                    isAdding = false
                },
                emeraldColor = emeraldColor
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Empty State / List
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color(0xFF1A1A1A),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO ACTIVE CUSTOM MONITORS FOR $symbol",
                    color = Color(0xFF404040),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Render list of alerts if needed
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMonitorForm(
    onInitialize: () -> Unit,
    emeraldColor: Color
) {
    var condition by remember { mutableStateOf(CustomAlertType.PRICE_ABOVE) }
    var thresholdValue by remember { mutableStateOf("") }
    var cooldown by remember { mutableStateOf("5") }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Condition Column
            Column(modifier = Modifier.weight(1f)) {
                Text("CONDITION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    Box(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F0F), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = condition.name.replace("_", " ").lowercase().capitalizeWords(),
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF0F0F0F))
                    ) {
                        CustomAlertType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ").lowercase().capitalizeWords(), color = Color.White) },
                                onClick = {
                                    condition = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Threshold Column
            Column(modifier = Modifier.weight(1f)) {
                Text("THRESHOLD VALUE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = thresholdValue,
                    onValueChange = { thresholdValue = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(emeraldColor),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F0F0F), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            if (thresholdValue.isEmpty()) {
                                Text("Enter value...", color = Color.DarkGray, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        // Cooldown Section
        Column {
            Text("COOLDOWN (MINUTES)", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = cooldown,
                        onValueChange = { cooldown = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Column {
                        Icon(
                            Icons.Default.ArrowDropUp, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(16.dp).clickable { 
                                val newVal = cooldown.toIntOrNull() ?: 0
                                cooldown = (newVal + 1).toString()
                            }
                        )
                        Icon(
                            Icons.Default.ArrowDropDown, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(16.dp).clickable { 
                                val newVal = cooldown.toIntOrNull() ?: 0
                                if (newVal > 0) cooldown = (newVal - 1).toString()
                            }
                        )
                    }
                }
            }
        }

        // Initialize Button
        Button(
            onClick = onInitialize,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                "INITIALIZE MONITOR",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

// Helper extension to capitalize words
private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
