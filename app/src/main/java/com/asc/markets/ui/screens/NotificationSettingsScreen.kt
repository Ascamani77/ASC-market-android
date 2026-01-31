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
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*

@Composable
fun NotificationSettingsScreen(viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "NOTIFICATIONS",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(24.dp)
        )

        SettingsSectionHeader("PUSH DELIVERY")
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ToggleRow("ENABLE PUSH", "GLOBAL MASTER SWITCH", true)
            ToggleRow("MARKET ALERTS", "STRUCTURE & TRIGGER NOTICES", true)
            ToggleRow("NEWS ALERTS", "HIGH-IMPACT FUNDAMENTAL EVENTS", true)
            ToggleRow("EXECUTION LOGS", "ORDER FILLS & TERMINAL UPDATES", true)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSectionHeader("QUIET MODE")
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ToggleRow("SLEEP MODE", "SILENCE NON-CRITICAL ALERTS", false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.padding(24.dp),
            color = Color.White.copy(alpha = 0.05f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Text("⚠", color = Color.Gray, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Identity verification and account security alerts bypass all quiet mode filters to ensure local node integrity.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.02f))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = IndigoAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun ToggleRow(label: String, sub: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(sub, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Switch(
            checked = checked,
            onCheckedChange = {},
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IndigoAccent,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}