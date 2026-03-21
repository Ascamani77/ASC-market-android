package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.asc.markets.ui.terminal.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezoneModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    selectedTimezone: String,
    onSelect: (String) -> Unit
) {
    if (!isOpen) return

    val timezones = listOf(
        "UTC-12 (Baker Island)", "UTC-11 (Pago Pago)", "UTC-10 (Honolulu)",
        "UTC-9 (Anchorage)", "UTC-8 (Los Angeles)", "UTC-7 (Denver)",
        "UTC-6 (Chicago)", "UTC-5 (New York)", "UTC-4 (Santiago)",
        "UTC-3 (Buenos Aires)", "UTC-2 (South Georgia)", "UTC-1 (Azores)",
        "UTC+0 (London)", "UTC+1 (Paris)", "UTC+2 (Cairo)",
        "UTC+3 (Moscow)", "UTC+4 (Dubai)", "UTC+5 (Karachi)",
        "UTC+5:30 (Mumbai)", "UTC+6 (Dhaka)", "UTC+7 (Bangkok)",
        "UTC+8 (Singapore)", "UTC+9 (Tokyo)", "UTC+9:30 (Adelaide)",
        "UTC+10 (Sydney)", "UTC+11 (Noumea)", "UTC+12 (Auckland)",
        "UTC+13 (Nuku'alofa)", "UTC+14 (Kiritimati)"
    )

    var searchQuery by remember { mutableStateOf("") }
    val filteredTimezones = timezones.filter { it.contains(searchQuery, ignoreCase = true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            color = DarkSurface,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Timezone", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search timezone...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        cursorColor = AccentBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(filteredTimezones) { tz ->
                        val isSelected = selectedTimezone == tz
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onSelect(tz)
                                    onClose()
                                }
                                .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tz, color = if (isSelected) Color.White else TextSecondary, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
