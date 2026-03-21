package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.DeepBlack

@Composable
fun StatusBar(
    selectedTimezone: String,
    onTimezoneClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        color = Color(0xFF1E222D),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Market is Open",
                    color = Color(0xFF089981),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "UTC: 12:45:30",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            
            Text(
                text = selectedTimezone,
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.clickable { onTimezoneClick() }
            )
        }
    }
}
