package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AppView
import com.asc.markets.data.NotificationModel
import com.asc.markets.ui.theme.*

@Composable
fun NotificationsScreen(viewModel: com.asc.markets.logic.ForexViewModel) {
    val notifications by viewModel.inAppNotifications.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val tabs = listOf("All", "Volatility", "AI", "Orderbook", "Liquidations")
    val filteredNotifications = remember(notifications, selectedFilter) {
        notifications.filter { notification -> matchesInboxFilter(notification, selectedFilter) }
    }
    val unreadCount = notifications.count { !it.seen }
    val actionableCount = notifications.count { it.symbol != null || it.targetView != null }

    fun openNotification(notification: NotificationModel) {
        viewModel.markNotificationSeen(notification.id)
        notification.symbol?.let { viewModel.selectPairBySymbolNoNavigate(it) }
        val targetView = notification.targetView?.let { route ->
            runCatching { enumValueOf<AppView>(route) }.getOrNull()
        } ?: notification.symbol?.let { AppView.TRADING_ASSISTANT }
        targetView?.let { viewModel.navigateTo(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(vertical = 8.dp)
    ) {
        // Professional Top Bar with back arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "NOTIFICATIONS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(32.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsActive, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                color = RoseError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
                Text(
                    "MARK ALL READ",
                    color = IndigoAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.clickable { viewModel.markAllNotificationsSeen() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(tabs) { tab ->
                val selected = selectedFilter == tab
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) IndigoAccent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.clickable { selectedFilter = tab }
                ) {
                    Text(
                        tab,
                        color = if (selected) Color.White else SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredNotifications.isEmpty()) {
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = SlateText, modifier = Modifier.size(28.dp))
                    Text("NO EVENTS IN THIS FILTER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("Triggered alerts and routed notification events will appear here with restore context.", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp, fontFamily = InterFontFamily)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotifications, key = { it.id }) { item ->
                    AlertInboxCard(item = item, onOpen = { openNotification(item) })
                }
            }
        }
    }
}

@Composable
fun AlertInboxCard(item: NotificationModel, onOpen: () -> Unit) {
    val indicatorColor = when (item.severity.uppercase()) {
        "CRITICAL" -> RoseError
        "WARNING" -> Color(0xFFF59E0B)
        "HIGH" -> IndigoAccent
        else -> EmeraldSuccess
    }
    val actionable = item.symbol != null || item.targetView != null

    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(indicatorColor, CircleShape)
                    )
                    Text(item.type.uppercase(), color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text(item.severity.uppercase(), color = indicatorColor, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    if (!item.seen) {
                        Text("UNREAD", color = IndigoAccent, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                Text(item.time, color = Color.Gray, fontSize = 10.sp, fontFamily = InterFontFamily)
            }

            Text(item.msg, color = if (item.seen) Color.White.copy(alpha = 0.72f) else Color.White, fontSize = 13.sp, fontWeight = if (item.seen) FontWeight.Medium else FontWeight.Black, lineHeight = 19.sp, fontFamily = InterFontFamily)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item.symbol?.let {
                    Surface(color = Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp)) {
                        Text(it, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
                item.timeframe?.let {
                    Surface(color = Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp)) {
                        Text(it, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
                if (actionable) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Visibility, null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                        Text("RESTORE WORKSPACE", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

private fun matchesInboxFilter(item: NotificationModel, filter: String): Boolean {
    if (filter == "All") return true
    val haystack = "${item.type} ${item.msg}".uppercase()
    return when (filter) {
        "Volatility" -> haystack.contains("VOLATILITY") || haystack.contains("REGIME") || haystack.contains("BURST")
        "AI" -> haystack.contains("AI") || haystack.contains("PRE-MOVE") || haystack.contains("PROGRESS")
        "Orderbook" -> haystack.contains("ORDER") || haystack.contains("BOOK") || haystack.contains("EXECUTION")
        "Liquidations" -> haystack.contains("LIQUIDATION") || haystack.contains("RISK")
        else -> true
    }
}
