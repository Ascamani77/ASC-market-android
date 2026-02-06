package com.asc.markets.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AppView
import com.asc.markets.ui.theme.*

import android.util.Log

data class NavItem(val view: AppView, val label: String, val icon: ImageVector)

@Composable
fun AscSidebar(
    currentView: AppView,
    isCollapsed: Boolean,
    onViewChange: (AppView) -> Unit,
    onClose: () -> Unit = {}
) {
    Log.d("ASC", "AscSidebar composed: isCollapsed=$isCollapsed, currentView=$currentView")
    val targetWidth = if (isCollapsed) 80.dp else 260.dp
    val sidebarWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "sidebarWidth"
    )

    Surface(
        color = PureBlack,
        modifier = Modifier.width(sidebarWidth).fillMaxHeight()
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            // measure footer height and reserve that space at the bottom of the scrollable area
            val footerHeightPx = remember { mutableStateOf(0) }
            val density = LocalDensity.current
            val footerPaddingDp = with(density) { footerHeightPx.value.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = footerPaddingDp + if (isCollapsed) 20.dp else 28.dp)
            ) {
            // Brand / Header with close action
            Box(modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .padding(horizontal = if (isCollapsed) 0.dp else 16.dp), contentAlignment = Alignment.CenterStart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("∧", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ASC", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MARKET", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }

                // Close button at top-right when expanded
                if (!isCollapsed) {
                    IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Groups
            // Pre-Move Surveillance group (arranged per design)
            SidebarGroup("PRE-MOVE SURVEILLANCE", isCollapsed, listOf(
                NavItem(AppView.ANALYSIS_RESULTS, "Analysis Node", Icons.Default.LineAxis),
                NavItem(AppView.MACRO_STREAM, "Macro Intelligence Stream", Icons.Default.Public),
                NavItem(AppView.LIQUIDITY_HUB, "Liquidity Maps", Icons.Default.AccountTree),
                NavItem(AppView.MULTI_TIMEFRAME, "Order Flow Delta", Icons.Default.GridView),
                NavItem(AppView.MARKETS, "Markets Scanner", Icons.Default.BarChart),
                NavItem(AppView.DIAGNOSTICS, "Micro-Jitter Monitor", Icons.Default.Shield)
            ), currentView, onViewChange)

            SidebarGroup("INTELLIGENCE", isCollapsed, listOf(
                NavItem(AppView.CHAT, "AI Intel", Icons.Default.Memory),
                NavItem(AppView.ALERTS, "Vigilance Nodes", Icons.Default.Notifications),
                NavItem(AppView.BACKTEST, "Logic Simulation", Icons.Default.History)
            ), currentView, onViewChange)

            SidebarGroup("POST-MOVE AUDIT", isCollapsed, listOf(
                NavItem(AppView.TRADE, "Trade Ledger", Icons.Default.ReceiptLong),
                NavItem(AppView.DIAGNOSTICS, "Execution Audit", Icons.Default.List),
                NavItem(AppView.TRADING_ASSISTANT, "Terminal Desk", Icons.Default.Terminal)
            ), currentView, onViewChange)

            SidebarGroup("KNOWLEDGE", isCollapsed, listOf(
                NavItem(AppView.NEWS, "Macro Intel", Icons.Default.Public),
                NavItem(AppView.CALENDAR, "Scheduling", Icons.Default.CalendarToday)
            ), currentView, onViewChange)

                Spacer(modifier = Modifier.weight(1f))
            }

            // Static footer region (never scrolls)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(PureBlack)
                    .padding(horizontal = if (isCollapsed) 12.dp else 16.dp, vertical = if (isCollapsed) 12.dp else 16.dp)
                    .onGloballyPositioned { footerHeightPx.value = it.size.height }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top
                ) {
                    if (!isCollapsed) {
                            Column {
                                FooterRow("RISK DISCLOSURE", Icons.Default.Security) { }
                                Spacer(modifier = Modifier.height(8.dp))
                                FooterRow("SYSTEM CONFIGURATION", Icons.Default.Settings) { }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FooterProfileBox(
                                name = "JOHN DOE",
                                node = "L14-UK",
                                initials = "JD",
                                version = "V0.9.0-BETA",
                                onClick = { onViewChange(AppView.PROFILE) }
                            )

                            // (removed duplicate small avatar for expanded state)
                    } else {
                        // Collapsed: only the small avatar centered
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(36.dp).background(GhostWhite, RoundedCornerShape(18.dp))) { }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarGroup(title: String, isCollapsed: Boolean, items: List<NavItem>, currentView: AppView, onViewChange: (AppView) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        if (!isCollapsed) {
            Text(title, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), letterSpacing = 2.sp, fontFamily = InterFontFamily)
        }
        items.forEach { item ->
            val active = currentView == item.view
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(44.dp)
                    .clickable { onViewChange(item.view) },
                color = if (active) ActiveHighlight else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(item.label.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SlateText, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FooterProfileBox(name: String, node: String, initials: String, version: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxWidth(),
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).background(GhostWhite, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(initials, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LIVE", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("NODE: $node", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(version, color = Color(0xFF3EA6FF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun FooterUserCard(name: String, node: String, initials: String, version: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(GhostWhite, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text(initials, color = Color.Black, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(EmeraldSuccess, RoundedCornerShape(4.dp))) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("NODE: $node", color = SlateText, fontSize = 11.sp)
            }

            Surface(
                color = Color(0xFF081A2B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(version, color = Color(0xFF3EA6FF), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}