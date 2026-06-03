@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.asc.markets.ui.components

import androidx.compose.animation.*
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
import com.asc.markets.ui.theme.*

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
    
    // Initialize QuickAccessManager
    LaunchedEffect(Unit) {
        QuickAccessManager.initialize(context)
    }
    
    val quickAccessItems by QuickAccessManager.quickAccessItems.collectAsState()
    var showRemoveDialog by remember { mutableStateOf<QuickAccessItem?>(null) }

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
                                // Avatar "E" Orange
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFFFF6A00), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "E",
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
                                    "El Jeffe",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                // Offline badge with red dot
                                Surface(
                                    color = Color(0xFF2C0B0B),
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
                                                .background(Color(0xFFEF4444), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            "Offline",
                                            color = Color(0xFFEF4444),
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
                // QUICK ACCESS Section
                SectionHeader("Quick Access")
                
                // Show remove dialog
                if (showRemoveDialog != null) {
                    AlertDialog(
                        onDismissRequest = { showRemoveDialog = null },
                        title = { Text("Remove from Quick Access?", color = Color.White) },
                        text = { Text("Remove \"${showRemoveDialog?.label}\" from Quick Access? You can add it back later from the menu sections below.", color = Color.White.copy(alpha = 0.7f)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showRemoveDialog?.let { item ->
                                    QuickAccessManager.removeFromQuickAccess(context, item.id)
                                }
                                showRemoveDialog = null
                            }) {
                                Text("Remove", color = Color(0xFFEF4444))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRemoveDialog = null }) {
                                Text("Cancel", color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF121212),
                        tonalElevation = 0.dp
                    )
                }
                
                // Render Quick Access items dynamically
                if (quickAccessItems.isEmpty()) {
                    // Show empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Quick Access items",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                "Long press any menu item below to add",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Render items in rows of 2
                        quickAccessItems.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    QuickAccessCard(
                                        modifier = Modifier.weight(1f),
                                        icon = item.icon,
                                        label = item.label,
                                        badgeText = if (item.appView == AppView.ALERTS || item.appView == AppView.MY_ALERTS) 
                                            alertBadgeCount.takeIf { it > 0 }?.toString() else null,
                                        onClick = { onViewChange(item.appView) },
                                        onLongClick = { showRemoveDialog = item }
                                    )
                                }
                                // Add spacer if odd number of items in row
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // TRACK Section
                SectionHeader("TRACK")
                MenuGroupContainer {
                    MenuItem(
                        icon = Icons.Default.Notifications,
                        label = "Vigilance Setup",
                        badgeText = alertBadgeCount.takeIf { it > 0 }?.toString(),
                        appView = AppView.ALERTS
                    ) { onViewChange(AppView.ALERTS) }
                    MenuDivider()
                    MenuItem(
                        icon = Icons.Default.List,
                        label = "My Alerts",
                        badgeText = alertBadgeCount.takeIf { it > 0 }?.toString(),
                        appView = AppView.MY_ALERTS
                    ) { onViewChange(AppView.MY_ALERTS) }
                    MenuDivider()
                    MenuItem(
                        icon = Icons.Default.NotificationsActive,
                        label = "Notification",
                        badgeText = alertBadgeCount.takeIf { it > 0 }?.toString(),
                        appView = AppView.NOTIFICATIONS
                    ) { onViewChange(AppView.NOTIFICATIONS) }
                    MenuDivider()
                    MenuItem(Icons.Default.PlayCircleOutline, "AI Simulation", appView = AppView.SIMULATION) { onViewChange(AppView.SIMULATION) }
                    MenuDivider()
                    MenuItem(Icons.Default.Timeline, "Backtest", appView = AppView.MY_SIMULATION) { onViewChange(AppView.MY_SIMULATION) }
                    MenuDivider()
                    MenuItem(Icons.AutoMirrored.Filled.ShowChart, "AI Sentiment", appView = AppView.SENTIMENT) { onViewChange(AppView.SENTIMENT) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // LIVE MARKETS Section
                SectionHeader("LIVE MARKETS")
                MenuGroupContainer {
                    MenuItem(Icons.Default.BarChart, "Markets Overview", appView = AppView.MARKETS) { onViewChange(AppView.MARKETS) }
                    MenuDivider()
                    MenuItem(Icons.Default.List, "Quotes Feed", appView = AppView.QUOTES) { onViewChange(AppView.QUOTES) }
                    MenuDivider()
                    MenuItem(Icons.Default.Schedule, "Market Status", appView = AppView.MARKET_STATUS) { onViewChange(AppView.MARKET_STATUS) }
                    MenuDivider()
                    MenuItem(Icons.Outlined.MenuBook, "Analysis & Opinion", appView = AppView.ANALYSIS_OPINION) { onViewChange(AppView.ANALYSIS_OPINION) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // MARKET INTELLIGENCE Section
                SectionHeader("MARKET INTELLIGENCE")
                MenuGroupContainer {
                    MenuItem(Icons.Default.Visibility, "Market Watch", appView = AppView.MARKET_WATCH) { onViewChange(AppView.MARKET_WATCH) }
                    MenuDivider()
                    MenuItem(Icons.Default.AddPhotoAlternate, "Chart Analysis Node", appView = AppView.CHART_ANALYSIS) { onViewChange(AppView.CHART_ANALYSIS) }
                    MenuDivider()
                    MenuItem(Icons.Default.Layers, "Liquidity Maps", appView = AppView.LIQUIDITY_HUB) { onViewChange(AppView.LIQUIDITY_HUB) }
                    MenuDivider()
                    MenuItem(Icons.Default.GridView, "Multi-Timeframe Analysis", appView = AppView.MULTI_TIMEFRAME) { onViewChange(AppView.MULTI_TIMEFRAME) }
                    MenuDivider()
                    MenuItem(Icons.Default.Shield, "System Diagnostics", appView = AppView.DIAGNOSTICS) { onViewChange(AppView.DIAGNOSTICS) }
                    MenuDivider()
                    MenuItem(Icons.Default.List, "Market Data Bus", appView = AppView.DATA_HUB) { onViewChange(AppView.DATA_HUB) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // INTELLIGENCE & DECISION Section
                SectionHeader("INTELLIGENCE & DECISION")
                MenuGroupContainer {
                    MenuItem(Icons.Default.Memory, "AI Intel", appView = AppView.CHAT) { onViewChange(AppView.CHAT) }
                    MenuDivider()
                    MenuItem(Icons.Default.History, "Logic Simulation", appView = AppView.BACKTEST) { onViewChange(AppView.BACKTEST) }
                    MenuDivider()
                    MenuItem(Icons.Default.Language, "Event Stream", appView = AppView.INTELLIGENCE_STREAM) { onViewChange(AppView.INTELLIGENCE_STREAM) }
                    MenuDivider()
                    MenuItem(Icons.Default.Lock, "Node Data Vault", appView = AppView.DATA_VAULT) { onViewChange(AppView.DATA_VAULT) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PORTFOLIO & OPERATIONS Section
                SectionHeader("PORTFOLIO & OPERATIONS")
                MenuGroupContainer {
                    MenuItem(Icons.Default.AttachMoney, "Active Inventory", appView = AppView.PORTFOLIO_MANAGER) { onViewChange(AppView.PORTFOLIO_MANAGER) }
                    MenuDivider()
                    MenuItem(Icons.Default.CurrencyExchange, "Live Trade", appView = AppView.PAPER_TRADING) { onViewChange(AppView.PAPER_TRADING) }
                    MenuDivider()
                    MenuItem(Icons.Default.CalendarToday, "Event Calendar", appView = AppView.CALENDAR) { onViewChange(AppView.CALENDAR) }
                    MenuDivider()
                    MenuItem(Icons.Default.Smartphone, "Push Notification", appView = AppView.PUSH_SETTINGS) { onViewChange(AppView.PUSH_SETTINGS) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // EXECUTION POST REVIEW Section
                SectionHeader("EXECUTION POST REVIEW")
                MenuGroupContainer {
                    MenuItem(Icons.AutoMirrored.Filled.ReceiptLong, "Trade Ledger", appView = AppView.TRADE) { onViewChange(AppView.TRADE) }
                    MenuDivider()
                    MenuItem(Icons.Default.List, "Post-Move Audit", appView = AppView.POST_MOVE_AUDIT) { onViewChange(AppView.POST_MOVE_AUDIT) }
                    MenuDivider()
                    MenuItem(Icons.Default.AssignmentReturned, "Post-Move Reconstruction", appView = AppView.TRADE_RECONSTRUCTION) { onViewChange(AppView.TRADE_RECONSTRUCTION) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // LEGAL Section
                SectionHeader("LEGAL")
                MenuGroupContainer {
                    MenuItem(Icons.Default.Shield, "Risk Disclosure", appView = AppView.EDUCATION) { onViewChange(AppView.EDUCATION) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // REMOTE STATUS Section
                SectionHeader("REMOTE STATUS")
                Surface(
                    color = DeepBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Remote: RESPECT  10000ms",
                            color = Color(0xFFFF6A00),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Macro Normal",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "last: never",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
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
            // Show indicator if in Quick Access
            if (isInQuickAccess) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.Star,
                    contentDescription = "In Quick Access",
                    tint = Color(0xFFFFA500),
                    modifier = Modifier.size(14.dp)
                )
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
