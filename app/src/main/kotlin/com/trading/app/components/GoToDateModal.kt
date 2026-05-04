package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GoToDateModal(
    onClose: () -> Unit,
    onGoTo: (Long) -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var viewingMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var activeTab by remember { mutableStateOf("Date") }
    
    // Time states
    var hour by remember { mutableStateOf("00") }
    var minute by remember { mutableStateOf("00") }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF121212)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Go to", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF787B86))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                Row(modifier = Modifier.fillMaxWidth()) {
                    val tabs = listOf("Date", "Custom range")
                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        Column(
                            modifier = Modifier
                                .weight(if (tab == "Date") 1f else 2f)
                                .clickable { activeTab = tab },
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                tab,
                                color = if (isSelected) Color.White else Color(0xFF787B86),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isSelected) 2.dp else 1.dp)
                                    .background(if (isSelected) Color.White else Color(0xFF363A45))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (activeTab == "Date") {
                    // Date and Time inputs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Date input
                        Row(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp)
                                .border(1.5.dp, Color(0xFF2962FF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time),
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF787B86), modifier = Modifier.size(18.dp))
                        }

                        // Time input
                        Row(
                            modifier = Modifier
                                .weight(0.8f)
                                .height(40.dp)
                                .background(Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF363A45), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = "$hour:$minute",
                                onValueChange = { /* Handle time change */ },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color(0xFF787B86), fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                decorationBox = { innerTextField ->
                                    if (hour.isEmpty() && minute.isEmpty()) {
                                        Text("00:00", color = Color(0xFF787B86), fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF787B86), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Calendar Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewingMonth = (viewingMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        }) {
                            Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                        }
                        Text(
                            SimpleDateFormat("MMMM yyyy", Locale.US).format(viewingMonth.time),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            viewingMonth = (viewingMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        }) {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days of week
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                            Text(
                                day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = Color(0xFF787B86),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    val daysInMonth = getDaysInMonth(viewingMonth)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(daysInMonth) { date ->
                            if (date == null) {
                                Box(modifier = Modifier.aspectRatio(1f))
                            } else {
                                val isSelected = isSameDay(date, selectedDate)
                                val isCurrentMonth = date.get(Calendar.MONTH) == viewingMonth.get(Calendar.MONTH)
                                
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                        color = if (isSelected) Color.Black else if (isCurrentMonth) Color.White else Color(0xFF363A45),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .width(12.dp)
                                                .height(2.dp)
                                                .background(Color.Black)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom Range UI
                    Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        Text("Custom Range selection would go here", color = Color(0xFF787B86), fontSize = 14.sp)
                        // This could be implemented as two calendars or a range picker
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.height(40.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF363A45)),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val calendar = selectedDate.clone() as Calendar
                            calendar.set(Calendar.HOUR_OF_DAY, hour.toIntOrNull() ?: 0)
                            calendar.set(Calendar.MINUTE, minute.toIntOrNull() ?: 0)
                            onGoTo(calendar.timeInMillis)
                            onClose()
                        },
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Go to", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getDaysInMonth(month: Calendar): List<Calendar?> {
    val days = mutableListOf<Calendar?>()
    val firstDayOfMonth = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    
    // Day of week starts at 1 (Sunday) in Calendar, we want Monday (2) as first
    var firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 2
    if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Sunday becomes 6
    
    for (i in 0 until firstDayOfWeek) {
        days.add(null)
    }
    
    val maxDays = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..maxDays) {
        days.add((firstDayOfMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) })
    }
    
    return days
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
