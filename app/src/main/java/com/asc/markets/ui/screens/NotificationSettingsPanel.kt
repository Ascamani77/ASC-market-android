package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun NotificationSettingsPanel() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text("NOTIFICATION SETTINGS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))

        SettingsSection("PUSH DELIVERY") {
            ToggleRow("Enable Push", true)
            ToggleRow("Market Alerts", true)
            ToggleRow("News Alerts", true)
            ToggleRow("Order Execution", true)
        }

        Spacer(modifier = Modifier.height(32.dp))

        SettingsSection("QUIET MODE") {
            ToggleRow("Sleep Mode", false)
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Text("⚠", color = IndigoAccent, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Security overrides: Identity verification requests and account security compromises will always bypass notification logic.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Switch(
            checked = checked,
            onCheckedChange = {},
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent)
        )
    }
}