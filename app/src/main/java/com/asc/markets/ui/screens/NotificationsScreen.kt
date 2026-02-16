package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun NotificationsScreen(viewModel: com.asc.markets.logic.ForexViewModel) {
    // Local toggle row composable for this screen
    @Composable
    fun ToggleSetting(label: String, sub: String = "", checkedInitial: Boolean = true) {
        var checked by remember { mutableStateOf(checkedInitial) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (sub.isNotEmpty()) Text(sub, color = SlateText, fontSize = 12.sp)
                }
                Switch(checked = checked, onCheckedChange = { checked = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
            }
            Divider(color = Color.White.copy(alpha = 0.03f))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack).verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Push Notifications Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Smartphone, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("PUSH NOTIFICATIONS", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                ToggleSetting("ENABLE PUSH", "GLOBAL MASTER TOGGLE FOR DEVICE ALERTS", true)
                ToggleSetting("MARKET ALERTS", "PRICE, STRUCTURE, AND TECHNICAL INDICATOR TRIGGERS", true)
                ToggleSetting("NEWS ALERTS", "HIGH-IMPACT MACRO AND ASSET-SPECIFIC NEWS", true)
                ToggleSetting("ORDER EXECUTION", "TRADE FILLS, STATUS CHANGES, AND EXECUTION LOGS", true)
                // Critical with red disclaimer
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CRITICAL RISK ALERTS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("SECURITY, SYSTEM STATUS, AND LIQUIDATION RISK", color = SlateText, fontSize = 12.sp)
                        }
                        var critical by remember { mutableStateOf(true) }
                        Switch(checked = critical, onCheckedChange = { critical = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                    }
                    Text("SYSTEM CRITICAL: MAY BYPASS QUIET MODE", color = RoseError, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                    Divider(color = Color.White.copy(alpha = 0.03f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Delivery Control: Quiet / Sleep Mode
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NightsStay, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("DELIVERY CONTROL", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                ToggleSetting("SLEEP MODE", "SILENCE NON-CRITICAL PUSH NOTIFICATIONS ON SCHEDULE", false)
            }
        }

        Spacer(Modifier.height(12.dp))

        // News Logic Filter
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Article, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("NEWS LOGIC FILTER", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text("AFFECTS BOTH PUSH NOTIFICATIONS AND IN-APP NEWS ALERTS", color = IndigoAccent.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                ToggleSetting("HIGH-IMPACT ONLY", "ONLY NOTIFY FOR HIGH-VOLATILITY MARKET EVENTS", true)
                ToggleSetting("ASSET-RELATED", "NEWS AFFECTING YOUR ACTIVE WATCHLIST ASSETS ONLY", true)
                ToggleSetting("MACRO SENTIMENT", "CENTRAL BANK POLICY, CPI, AND ECONOMIC FORECASTS", true)
            }
        }

        Spacer(Modifier.height(12.dp))

        // In-app messages
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("IN-APP MESSAGES", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                ToggleSetting("LATEST EVENTS", "SESSION OPENINGS, CLOSINGS, AND HOURLY RECAPS", true)
                ToggleSetting("ANNOUNCEMENTS", "PLATFORM UPDATES AND FEATURE DEPLOYMENTS", true)
                ToggleSetting("STRATEGY SIGNALS", "STANDARD ALGORITHMIC ENTRY AND EXIT ALERTS", true)
                ToggleSetting("SMART ALERTS", "HIGH-CONFLUENCE INSTITUTIONAL STRUCTURE MONITORING", true)
                ToggleSetting("SIMPLE ALERTS", "THRESHOLD-BASED PRICE AND RSI LEVEL TRIGGERS", true)
                ToggleSetting("SYSTEM NOTIFICATIONS", "LOCAL NODE STATUS AND ANALYTICAL ENGINE LOGS", true)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Use notifications from ViewModel; mark only when tapped
        val notifications by viewModel.inAppNotifications.collectAsState()

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("RECENT ALERTS", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            for (item in notifications) {
                NotificationCard(item = NotificationItem(item.type, item.msg, item.time, item.severity), onClick = {
                    viewModel.markNotificationSeen(item.id)
                }, seen = item.seen)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Security override disclaimer
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0B0B), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = SlateText)
                Spacer(Modifier.height(8.dp))
                Text("SECURITY OVERRIDES: IDENTITY VERIFICATION REQUESTS AND ACCOUNT SECURITY COMPROMISES WILL ALWAYS BYPASS NOTIFICATION LOGIC TO ENSURE SYSTEM INTEGRITY.", color = SlateText, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

data class NotificationItem(val type: String, val msg: String, val time: String, val severity: String)

@Composable
fun NotificationCard(item: NotificationItem, onClick: () -> Unit = {}, seen: Boolean = false) {
    val indicatorColor = when(item.severity) {
        "CRITICAL" -> RoseError
        "WARNING" -> Color(0xFFF59E0B)
        else -> IndigoAccent
    }

    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = 4.dp)
                    .background(indicatorColor, androidx.compose.foundation.shape.CircleShape)
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.type, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Box(modifier = Modifier.background(indicatorColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(item.severity, color = indicatorColor, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.msg, color = if (seen) Color.White.copy(alpha = 0.6f) else Color.White, fontSize = 13.sp, fontWeight = if (seen) FontWeight.Normal else FontWeight.Medium, lineHeight = 18.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(10.dp))
                Text(item.time, color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
            }
        }
    }
}