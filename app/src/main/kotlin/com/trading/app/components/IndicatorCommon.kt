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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorOpacityBox(color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(2.dp)
                .background(color.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun IconBox(icon: ImageVector, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 34.dp)
            .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF787B86),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFD1D4DC), fontSize = 16.sp, modifier = Modifier.width(100.dp))
        Box(
            modifier = Modifier
                .width(120.dp)
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected, color = Color(0xFFD1D4DC), fontSize = 16.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF787B86))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E222D))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VisibilityTab() {
    var ticksVisible by rememberSaveable { mutableStateOf(true) }
    var secondsVisible by rememberSaveable { mutableStateOf(true) }
    var secondsFrom by rememberSaveable { mutableStateOf("1") }
    var secondsTo by rememberSaveable { mutableStateOf("59") }
    var minutesVisible by rememberSaveable { mutableStateOf(true) }
    var minutesFrom by rememberSaveable { mutableStateOf("1") }
    var minutesTo by rememberSaveable { mutableStateOf("59") }
    var hoursVisible by rememberSaveable { mutableStateOf(true) }
    var hoursFrom by rememberSaveable { mutableStateOf("1") }
    var hoursTo by rememberSaveable { mutableStateOf("24") }
    var daysVisible by rememberSaveable { mutableStateOf(true) }
    var daysFrom by rememberSaveable { mutableStateOf("1") }
    var daysTo by rememberSaveable { mutableStateOf("366") }
    var weeksVisible by rememberSaveable { mutableStateOf(true) }
    var weeksFrom by rememberSaveable { mutableStateOf("1") }
    var weeksTo by rememberSaveable { mutableStateOf("52") }
    var monthsVisible by rememberSaveable { mutableStateOf(true) }
    var monthsFrom by rememberSaveable { mutableStateOf("1") }
    var monthsTo by rememberSaveable { mutableStateOf("12") }
    var rangesVisible by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        VisibilitySettingRow(
            label = "Ticks",
            checked = ticksVisible,
            onCheckedChange = { ticksVisible = it }
        )
        VisibilitySettingRow(
            label = "Seconds",
            checked = secondsVisible,
            onCheckedChange = { secondsVisible = it },
            rangeStart = secondsFrom,
            onRangeStartChange = { secondsFrom = it },
            rangeEnd = secondsTo,
            onRangeEndChange = { secondsTo = it }
        )
        VisibilitySettingRow(
            label = "Minutes",
            checked = minutesVisible,
            onCheckedChange = { minutesVisible = it },
            rangeStart = minutesFrom,
            onRangeStartChange = { minutesFrom = it },
            rangeEnd = minutesTo,
            onRangeEndChange = { minutesTo = it }
        )
        VisibilitySettingRow(
            label = "Hours",
            checked = hoursVisible,
            onCheckedChange = { hoursVisible = it },
            rangeStart = hoursFrom,
            onRangeStartChange = { hoursFrom = it },
            rangeEnd = hoursTo,
            onRangeEndChange = { hoursTo = it }
        )
        VisibilitySettingRow(
            label = "Days",
            checked = daysVisible,
            onCheckedChange = { daysVisible = it },
            rangeStart = daysFrom,
            onRangeStartChange = { daysFrom = it },
            rangeEnd = daysTo,
            onRangeEndChange = { daysTo = it }
        )
        VisibilitySettingRow(
            label = "Weeks",
            checked = weeksVisible,
            onCheckedChange = { weeksVisible = it },
            rangeStart = weeksFrom,
            onRangeStartChange = { weeksFrom = it },
            rangeEnd = weeksTo,
            onRangeEndChange = { weeksTo = it }
        )
        VisibilitySettingRow(
            label = "Months",
            checked = monthsVisible,
            onCheckedChange = { monthsVisible = it },
            rangeStart = monthsFrom,
            onRangeStartChange = { monthsFrom = it },
            rangeEnd = monthsTo,
            onRangeEndChange = { monthsTo = it }
        )
        VisibilitySettingRow(
            label = "Ranges",
            checked = rangesVisible,
            onCheckedChange = { rangesVisible = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun VisibilitySettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    rangeStart: String? = null,
    onRangeStartChange: ((String) -> Unit)? = null,
    rangeEnd: String? = null,
    onRangeEndChange: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, Color(0xFF434651), RoundedCornerShape(2.dp))
                    .background(if (checked) Color(0xFFD1D4DC) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color(0xFFD1D4DC),
                fontSize = 16.sp,
                modifier = Modifier.width(60.dp)
            )
        }

        if (
            rangeStart != null &&
            onRangeStartChange != null &&
            rangeEnd != null &&
            onRangeEndChange != null
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            VisibilityNumberField(
                value = rangeStart,
                onValueChange = onRangeStartChange
            )
            Text(
                text = "-",
                color = Color(0xFF787B86),
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            VisibilityNumberField(
                value = rangeEnd,
                onValueChange = onRangeEndChange
            )
        }
    }
}

@Composable
fun VisibilityNumberField(
    value: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue.filter { it.isDigit() })
        },
        modifier = Modifier
            .width(84.dp)
            .height(36.dp)
            .border(1.dp, Color(0xFF434651), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        textStyle = TextStyle(
            color = Color(0xFFD1D4DC),
            fontSize = 15.sp
        ),
        cursorBrush = SolidColor(Color.White),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
fun SettingsNumericInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFD1D4DC), fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value.toString(),
            onValueChange = { onValueChange(it.toIntOrNull() ?: value) },
            modifier = Modifier
                .width(70.dp)
                .background(Color.Transparent, RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            textStyle = TextStyle(color = Color(0xFFD1D4DC), fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun SettingsFloatInput(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFD1D4DC), fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value.toString(),
            onValueChange = { onValueChange(it.toFloatOrNull() ?: value) },
            modifier = Modifier
                .width(70.dp)
                .background(Color.Transparent, RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            textStyle = TextStyle(color = Color(0xFFD1D4DC), fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun SettingsCheckbox(
    label: String, 
    checked: Boolean, 
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(1.dp, Color(0xFF434651), RoundedCornerShape(2.dp))
                .background(if (checked) Color(0xFFD1D4DC) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = Color(0xFFD1D4DC), fontSize = 16.sp)
    }
}
