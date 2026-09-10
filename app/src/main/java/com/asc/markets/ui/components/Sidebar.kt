package com.asc.markets.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.asc.markets.data.AppView
import com.asc.markets.data.QuickAccessManager
import com.asc.markets.data.QuickAccessItem
import com.asc.markets.data.SystemLinkMonitor
import com.asc.markets.data.UserProfileStore
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.HairlineBorder

private data class SidebarSearchEntry(
    val icon: ImageVector,
    val label: String,
    val keywords: String,
    val view: AppView
)

private val sidebarSearchEntries = listOf(
    SidebarSearchEntry(Icons.Default.BarChart, "Markets Overview", "markets dashboard", AppView.MARKETS),
    SidebarSearchEntry(Icons.Default.Visibility, "Watchlist", "watch assets", AppView.WATCHLIST),
    SidebarSearchEntry(Icons.Default.Schedule, "Event Calendar", "calendar events news", AppView.CALENDAR),
    SidebarSearchEntry(Icons.Default.Notifications, "Vigilance Setup", "alert monitoring deployment", AppView.ALERTS),
    SidebarSearchEntry(Icons.Default.List, "My Alerts", "triggered events alerts", AppView.MY_ALERTS),
    SidebarSearchEntry(Icons.Default.PlayCircleOutline, "AI Simulation", "simulate backtest analyze", AppView.SIMULATION),
    SidebarSearchEntry(Icons.Default.SmartToy, "Auto Trade", "automated trading", AppView.AUTO_TRADE),
    SidebarSearchEntry(Icons.Default.Settings, "AI & Connection", "settings ai connection server", AppView.AI_SETTINGS),
    SidebarSearchEntry(Icons.Default.Smartphone, "Push Notification", "push notifications settings", AppView.PUSH_SETTINGS),
    SidebarSearchEntry(Icons.AutoMirrored.Filled.ReceiptLong, "Trade Ledger", "trade closed positions book ledger", AppView.TRADE),
    SidebarSearchEntry(Icons.Default.List, "Post-Move Audit", "audit trade review outcomes", AppView.POST_MOVE_AUDIT),
    SidebarSearchEntry(Icons.Default.AssignmentReturned, "Post-Move Reconstruction", "reconstruction forensics trade", AppView.TRADE_RECONSTRUCTION)
)

@Composable
fun AscSidebar(
    currentView: AppView,
    isCollapsed: Boolean,
    promoteMacro: Boolean = false,
    alertBadgeCount: Int = 0,
    onViewChange: (AppView) -> Unit,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Observe real link status: APP + EA + AI all connected = Online
    val links by SystemLinkMonitor.state.collectAsState()

    // Observe real user profile from backend
    val userProfile by UserProfileStore.profile.collectAsState()

    Surface(
        color = PureBlack,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Avatar, Name, Offline status, Search, Settings
            Surface(
                color = Color.White.copy(alpha = 0.035f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "HeaderContent"
                    ) { active ->
                        if (active) {
                            // Search Box Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text("Search menu...", color = Color.Gray, fontSize = 16.sp)
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                IconButton(onClick = { 
                                    isSearchActive = false 
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        } else {
                            // Default Profile Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with user initials from profile
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFFFF6A00), CircleShape)
                                        .clickable { onViewChange(AppView.PROFILE) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        userProfile.initials,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    // Status dot (Green)
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                            .border(1.5.dp, DeepBlack, CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    userProfile.fullName,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                // Connection status badge (Online only when APP + EA + AI are all connected)
                                val online = links.allConnected
                                val (statusText, statusColor, dotColor) = if (online)
                                    Triple("Online", Color(0xFF0C3D2C), Color(0xFF10B981))
                                else
                                    Triple("Offline", Color(0xFF2C0B0B), Color(0xFFEF4444))
                                Surface(
                                    color = statusColor,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(dotColor, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            statusText,
                                            color = dotColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(onClick = { isSearchActive = true }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                IconButton(onClick = { onViewChange(AppView.SETTINGS) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Content Area (PureBlack Background)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 32.dp)
            ) {
                val trimmedQuery = searchQuery.trim()
                if (isSearchActive && trimmedQuery.isNotEmpty()) {
                    val term = trimmedQuery.lowercase()
                    val results = remember(term) {
                        sidebarSearchEntries.filter { entry ->
                            entry.label.lowercase().contains(term) || entry.keywords.contains(term)
                        }
                    }
                    SectionHeader("Search Results")
                    if (results.isEmpty()) {
                        Text(
                            text = "No pages match \"$trimmedQuery\"",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    } else {
                        MenuGroupContainer {
                            results.forEachIndexed { index, entry ->
                                val alertBadge = when (entry.view) {
                                    AppView.ALERTS, AppView.MY_ALERTS -> alertBadgeCount.takeIf { it > 0 }?.toString()
                                    else -> null
                                }
                                MenuItem(entry.icon, entry.label, badgeText = alertBadge, appView = entry.view) {
                                    onViewChange(entry.view)
                                    isSearchActive = false
                                    searchQuery = ""
                                }
                                if (index < results.lastIndex) MenuDivider()
                            }
                        }
                    }
                } else {
                    // ─── CORE WORKFLOW ───
                    SectionHeader("Workflow")
                    MenuGroupContainer {
                        MenuItem(Icons.Default.BarChart, "Markets Overview", appView = AppView.MARKETS) { onViewChange(AppView.MARKETS) }
                        MenuDivider()
                        MenuItem(Icons.Default.Visibility, "Watchlist", appView = AppView.WATCHLIST) { onViewChange(AppView.WATCHLIST) }
                        MenuDivider()
                        MenuItem(Icons.Default.Schedule, "Event Calendar", appView = AppView.CALENDAR) { onViewChange(AppView.CALENDAR) }
                        MenuDivider()
                        MenuItem(Icons.Default.Notifications, "Vigilance Setup", appView = AppView.ALERTS, badgeText = alertBadgeCount.takeIf { it > 0 }?.toString()) { onViewChange(AppView.ALERTS) }
                        MenuDivider()
                        MenuItem(Icons.Default.List, "My Alerts", appView = AppView.MY_ALERTS, badgeText = alertBadgeCount.takeIf { it > 0 }?.toString()) { onViewChange(AppView.MY_ALERTS) }
                        MenuDivider()
                        MenuItem(Icons.Default.PlayCircleOutline, "AI Simulation", appView = AppView.SIMULATION) { onViewChange(AppView.SIMULATION) }
                        MenuDivider()
                        MenuItem(Icons.Default.SmartToy, "Auto Trade", appView = AppView.AUTO_TRADE) { onViewChange(AppView.AUTO_TRADE) }
                        MenuDivider()
                        MenuItem(Icons.Default.Settings, "AI & Connection", appView = AppView.AI_SETTINGS) { onViewChange(AppView.AI_SETTINGS) }
                        MenuDivider()
                        MenuItem(Icons.Default.Smartphone, "Push Notification", appView = AppView.PUSH_SETTINGS) { onViewChange(AppView.PUSH_SETTINGS) }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ─── REVIEW / PORTFOLIO (collapsible) ───
                    val reviewPrefs = context.getSharedPreferences("sidebar_state", Context.MODE_PRIVATE)
                    var reviewExpanded by remember { mutableStateOf(reviewPrefs.getBoolean("review_portfolio_expanded", false)) }
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clickable {
                                    reviewExpanded = !reviewExpanded
                                    reviewPrefs.edit().putBoolean("review_portfolio_expanded", reviewExpanded).apply()
                                }
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Review / Portfolio",
                                color = Color(0xFF999999),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (reviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AnimatedVisibility(
                            visible = reviewExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            MenuGroupContainer {
                                MenuItem(Icons.AutoMirrored.Filled.ReceiptLong, "Trade Ledger", appView = AppView.TRADE) { onViewChange(AppView.TRADE) }
                                MenuDivider()
                                MenuItem(Icons.Default.List, "Post-Move Audit", appView = AppView.POST_MOVE_AUDIT) { onViewChange(AppView.POST_MOVE_AUDIT) }
                                MenuDivider()
                                MenuItem(Icons.Default.AssignmentReturned, "Post-Move Reconstruction", appView = AppView.TRADE_RECONSTRUCTION) { onViewChange(AppView.TRADE_RECONSTRUCTION) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF999999),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun QuickAccessCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    badgeText: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier
            .height(64.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    label,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!badgeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF23262F), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun MenuGroupContainer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    label: String,
    badgeText: String? = null,
    appView: AppView,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isInQuickAccess = QuickAccessManager.isInQuickAccess(appView)
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isInQuickAccess) "Remove from Quick Access?" else "Add to Quick Access?", color = Color.White) },
            text = { 
                Text(
                    if (isInQuickAccess) 
                        "Remove \"$label\" from Quick Access?" 
                    else 
                        "Add \"$label\" to Quick Access for faster navigation?",
                    color = Color.White.copy(alpha = 0.7f)
                ) 
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isInQuickAccess) {
                        val item = QuickAccessManager.getItemByAppView(appView)
                        item?.let { QuickAccessManager.removeFromQuickAccess(context, it.id) }
                    } else {
                        val item = QuickAccessManager.getItemByAppView(appView)
                        item?.let { QuickAccessManager.addToQuickAccess(context, it) }
                    }
                    showDialog = false
                }) {
                    Text(if (isInQuickAccess) "Remove" else "Add", 
                         color = if (isInQuickAccess) Color(0xFFEF4444) else Color(0xFF2962FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF121212),
            tonalElevation = 0.dp
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDialog = true }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (!badgeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF23262F), RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 0.dp),
        thickness = 0.8.dp,
        color = Color.White.copy(alpha = 0.15f)
    )
}
