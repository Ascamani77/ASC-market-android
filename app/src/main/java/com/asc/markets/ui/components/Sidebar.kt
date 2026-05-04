@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.asc.markets.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import com.asc.markets.data.AppView
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
    val scrollState = rememberScrollState()
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        color = PureBlack,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Avatar, Name, Offline status, Search, Settings (DeepBlack Background)
            Surface(
                color = DeepBlack,
                modifier = Modifier.fillMaxWidth()
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.GppGood,
                            label = "Deep Audit",
                            onClick = { onViewChange(AppView.ANALYSIS_RESULTS) }
                        )
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Memory,
                            label = "AI Intel",
                            onClick = { onViewChange(AppView.CHAT) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.GridView,
                            label = "Trade Dashboard",
                            onClick = { onViewChange(AppView.TRADE_DASHBOARD) }
                        )
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.CalendarMonth,
                            label = "Event Calendar",
                            onClick = { onViewChange(AppView.CALENDAR) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.NotificationsNone,
                            label = "Vigilance Setup",
                            badgeText = alertBadgeCount.takeIf { it > 0 }?.toString(),
                            onClick = { onViewChange(AppView.ALERTS) }
                        )
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.BookmarkBorder,
                            label = "Saved Items",
                            onClick = { }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.PlayCircleOutline,
                            label = "AI Simulation",
                            onClick = { onViewChange(AppView.SIMULATION) }
                        )
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.ShowChart,
                            label = "My Simulation",
                            onClick = { onViewChange(AppView.MY_SIMULATION) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Terminal,
                            label = "AI Terminal",
                            onClick = { onViewChange(AppView.AI_TERMINAL) }
                        )
                        QuickAccessCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            label = "AI Sentiment",
                            onClick = { onViewChange(AppView.SENTIMENT) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // TRACK Section
                SectionHeader("TRACK")
                MenuGroupContainer {
                    MenuItem(
                        icon = Icons.Default.Notifications,
                        label = "Vigilance Setup",
                        badgeText = alertBadgeCount.takeIf { it > 0 }?.toString()
                    ) { onViewChange(AppView.ALERTS) }
                    MenuDivider()
                    MenuItem(
                        icon = Icons.Default.List,
                        label = "My Alerts",
                        badgeText = alertBadgeCount.takeIf { it > 0 }?.toString()
                    ) { onViewChange(AppView.MY_ALERTS) }
                    MenuDivider()
                    MenuItem(Icons.Default.StarBorder, "Saved Items") { /* Placeholder */ }
                    MenuDivider()
                    MenuItem(Icons.Default.PlayCircleOutline, "AI Simulation") { onViewChange(AppView.SIMULATION) }
                    MenuDivider()
                    MenuItem(Icons.Default.Timeline, "My Simulation") { onViewChange(AppView.MY_SIMULATION) }
                    MenuDivider()
                    MenuItem(Icons.Default.Terminal, "AI Terminal") { onViewChange(AppView.AI_TERMINAL) }
                    MenuDivider()
                    MenuItem(Icons.AutoMirrored.Filled.ShowChart, "AI Sentiment") { onViewChange(AppView.SENTIMENT) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // LIVE MARKETS Section
                SectionHeader("LIVE MARKETS")
                MenuGroupContainer {
                    MenuItem(Icons.Default.BarChart, "Markets Overview") { onViewChange(AppView.MARKETS) }
                    MenuDivider()
                    MenuItem(Icons.Default.List, "Quotes Feed") { onViewChange(AppView.QUOTES) }
                    MenuDivider()
                    MenuItem(Icons.AutoMirrored.Outlined.MenuBook, "Analysis & Opinion") { onViewChange(AppView.NEWS) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // DEEP AUDIT Section
                SectionHeader("DEEP AUDIT")
                MenuGroupContainer {
                    MenuItem(Icons.Default.Language, "Macro Stream") { onViewChange(AppView.MACRO_STREAM) }
                    MenuDivider()
                    MenuItem(Icons.Default.Visibility, "Market Watch") { onViewChange(AppView.MARKET_WATCH) }
                    MenuDivider()
                    MenuItem(Icons.Default.Timeline, "Analysis Node") { onViewChange(AppView.ANALYSIS_RESULTS) }
                    MenuDivider()
                    MenuItem(Icons.Default.Layers, "Liquidity Maps") { onViewChange(AppView.LIQUIDITY_HUB) }
                    MenuDivider()
                    MenuItem(Icons.Default.GridView, "Order Flow Delta") { onViewChange(AppView.MULTI_TIMEFRAME) }
                    MenuDivider()
                    MenuItem(Icons.Default.Shield, "Micro-Jitter Monitor") { onViewChange(AppView.DIAGNOSTICS) }
                    MenuDivider()
                    MenuItem(Icons.Default.List, "Market Data Bus") { onViewChange(AppView.DATA_HUB) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // INTELLIGENCE & DECISION Section
                SectionHeader("INTELLIGENCE & DECISION")
                MenuGroupContainer {
                    MenuItem(Icons.Default.Memory, "AI Intel") { onViewChange(AppView.CHAT) }
                    MenuDivider()
                    MenuItem(Icons.Default.Notifications, "Vigilance Nodes") { onViewChange(AppView.ALERTS) }
                    MenuDivider()
                    MenuItem(Icons.Default.History, "Logic Simulation") { onViewChange(AppView.BACKTEST) }
                    MenuDivider()
                    MenuItem(Icons.Default.Language, "Intelligence Stream") { onViewChange(AppView.INTELLIGENCE_STREAM) }
                    MenuDivider()
                    MenuItem(Icons.Default.Lock, "Node Data Vault") { onViewChange(AppView.DATA_VAULT) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PORTFOLIO & OPERATIONS Section
                SectionHeader("PORTFOLIO & OPERATIONS")
                MenuGroupContainer {
                    MenuItem(Icons.Default.AttachMoney, "Active Inventory") { onViewChange(AppView.PORTFOLIO_MANAGER) }
                    MenuDivider()
                    MenuItem(Icons.Default.CurrencyExchange, "Paper Trading") { onViewChange(AppView.PAPER_TRADING) }
                    MenuDivider()
                    MenuItem(Icons.Default.Language, "Raw Feed") { onViewChange(AppView.NEWS) }
                    MenuDivider()
                    MenuItem(Icons.Default.CalendarToday, "Event Calendar") { onViewChange(AppView.CALENDAR) }
                    MenuDivider()
                    MenuItem(Icons.Default.GridView, "Trade Dashboard") { onViewChange(AppView.TRADE_DASHBOARD) }
                    MenuDivider()
                    MenuItem(Icons.Default.Terminal, "Terminal Desk") { onViewChange(AppView.TRADING_ASSISTANT) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // EXECUTION POST REVIEW Section
                SectionHeader("EXECUTION POST REVIEW")
                MenuGroupContainer {
                    MenuItem(Icons.AutoMirrored.Filled.ReceiptLong, "Trade Ledger") { onViewChange(AppView.TRADE) }
                    MenuDivider()
                    MenuItem(Icons.Default.List, "Post-Move Audit") { onViewChange(AppView.POST_MOVE_AUDIT) }
                    MenuDivider()
                    MenuItem(Icons.Default.AssignmentReturned, "Deep Audit") { onViewChange(AppView.TRADE_RECONSTRUCTION) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // LEGAL Section
                SectionHeader("LEGAL")
                MenuGroupContainer {
                    MenuItem(Icons.Default.Shield, "Risk Disclosure") { onViewChange(AppView.EDUCATION) }
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
    onClick: () -> Unit
) {
    Surface(
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() }
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
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
