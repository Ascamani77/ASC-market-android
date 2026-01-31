package com.asc.markets.ui.screens

import androidx.compose.foundation.background
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
fun NotificationsScreen() {
    var activeCategory by remember { mutableStateOf("ALL") }
    val categories = listOf("ALL", "ALERTS", "NEWS", "STRATEGY", "SYSTEM")
    
    val logs = listOf(
        NotificationItem("SIGNAL", "High confidence BUY setup detected on EUR/USD.", "2m ago", "CRITICAL"),
        NotificationItem("NEWS", "NFP data release in 30 minutes. USD VOLATILITY EXPECTED.", "15m ago", "WARNING"),
        NotificationItem("SYSTEM", "Local node identity verified via biometrics.", "1h ago", "INFO"),
        NotificationItem("ALERT", "Target level 1.0850 breached on H1.", "3h ago", "INFO")
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Category Pill Bar Parity
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                Surface(
                    color = if (activeCategory == cat) IndigoAccent else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(32.dp).clickable { activeCategory = cat },
                    border = if (activeCategory != cat) androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder) else null
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(cat, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(logs.filter { activeCategory == "ALL" || it.type.contains(activeCategory.take(3)) }) { log ->
                NotificationCard(log)
            }
        }
    }
}

data class NotificationItem(val type: String, val msg: String, val time: String, val severity: String)

@Composable
fun NotificationCard(item: NotificationItem) {
    val indicatorColor = when(item.severity) {
        "CRITICAL" -> RoseError
        "WARNING" -> Color(0xFFF59E0B)
        else -> IndigoAccent
    }

    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
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
                Text(item.msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(10.dp))
                Text(item.time, color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}