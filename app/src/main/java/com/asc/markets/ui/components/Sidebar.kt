@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.asc.markets.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AppView
import com.asc.markets.ui.theme.*

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.delay
import android.content.Context
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.asc.markets.data.RemoteConfigManager

data class NavItem(val view: AppView, val label: String, val icon: ImageVector)

@Composable
fun AscSidebar(
    currentView: AppView,
    isCollapsed: Boolean,
    promoteMacro: Boolean = false,
    onViewChange: (AppView) -> Unit,
    onClose: () -> Unit = {}
) {
    Log.d("ASC", "AscSidebar composed: isCollapsed=$isCollapsed, currentView=$currentView")
    val context = LocalContext.current
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
                    Text("∧", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ASC", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MARKET", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, fontFamily = InterFontFamily)
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
                // Small PRE-EVENT MODE badge under header when promoted
                if (!isCollapsed && promoteMacro) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp)) {
                        Surface(color = Color(0xFF081A2B), shape = RoundedCornerShape(8.dp)) {
                            Text("PRE-EVENT MODE", color = IndigoAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            

            // Groups
            // Map of bring-into-view requesters for each nav item so we can auto-scroll
            val bringMap = remember { mutableMapOf<com.asc.markets.data.AppView, BringIntoViewRequester>() }

            // When the currentView changes (or the sidebar composes), bring the active item into view
            LaunchedEffect(currentView) {
                // small delay to allow layout
                delay(80)
                try {
                    bringMap[currentView]?.bringIntoView()
                } catch (_: Exception) { }
            }
            // Pre-Move Surveillance group (arranged per design)
            SidebarGroup("PRE-MOVE SURVEILLANCE", isCollapsed, listOf(
                NavItem(AppView.MACRO_STREAM, "Macro Intelligence Stream", Icons.Default.Public),
                NavItem(AppView.MARKET_WATCH, "Market Watch", Icons.Default.Visibility),
                NavItem(AppView.ANALYSIS_RESULTS, "Analysis Node", Icons.Default.LineAxis),
                NavItem(AppView.LIQUIDITY_HUB, "Liquidity Maps", Icons.Default.AccountTree),
                NavItem(AppView.MULTI_TIMEFRAME, "Order Flow Delta", Icons.Default.GridView),
                NavItem(AppView.MARKETS, "Markets Scanner", Icons.Default.BarChart),
                NavItem(AppView.DIAGNOSTICS, "Micro-Jitter Monitor", Icons.Default.Shield),
                NavItem(AppView.DATA_HUB, "Market Data Bus", Icons.Default.Storage)
            ), bringMap, currentView, onViewChange)

            SidebarGroup("INTELLIGENCE", isCollapsed, listOf(
                NavItem(AppView.CHAT, "AI Intel", Icons.Default.Memory),
                NavItem(AppView.ALERTS, "Vigilance Nodes", Icons.Default.Notifications),
                NavItem(AppView.BACKTEST, "Logic Simulation", Icons.Default.History),
                NavItem(AppView.INTELLIGENCE_STREAM, "Intelligence Stream", Icons.Default.Public),
                NavItem(AppView.DATA_VAULT, "Node Data Vault", Icons.Default.Lock)
            ), bringMap, currentView, onViewChange)

            SidebarGroup("OPERATIONS", isCollapsed, listOf(
                NavItem(AppView.PORTFOLIO_MANAGER, "Active Inventory", Icons.Default.AttachMoney)
            ), bringMap, currentView, onViewChange)

            SidebarGroup("POST-MOVE AUDIT", isCollapsed, listOf(
                NavItem(AppView.TRADE, "Trade Ledger", Icons.Default.ReceiptLong),
                NavItem(AppView.POST_MOVE_AUDIT, "Post-Move Audit", Icons.Default.List),
                NavItem(AppView.TRADING_ASSISTANT, "Terminal Desk", Icons.Default.Terminal),
                NavItem(AppView.TRADE_RECONSTRUCTION, "Deep Audit", Icons.Default.AssignmentReturned)
            ), bringMap, currentView, onViewChange)

            SidebarGroup("KNOWLEDGE", isCollapsed, listOf(
                NavItem(AppView.NEWS, "Macro Intel", Icons.Default.Public),
                NavItem(AppView.CALENDAR, "Scheduling", Icons.Default.CalendarToday)
            ), bringMap, currentView, onViewChange)

            // Insert LEGAL section directly below Scheduling to keep legal items near knowledge
            if (!isCollapsed) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("LEGAL", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), letterSpacing = 2.sp, fontFamily = InterFontFamily)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .height(44.dp)
                        .clickable { onViewChange(AppView.NEWS) },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Persistent active indicator (left edge) for alignment consistency
                        Box(modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(Color.Transparent, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("RISK DISCLOSURE".uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    }
                }
            }

                Spacer(modifier = Modifier.weight(1f))
            }

            // Static footer region (never scrolls)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = if (isCollapsed) 12.dp else 16.dp, vertical = 0.dp)
                    .onGloballyPositioned { footerHeightPx.value = it.size.height }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (!isCollapsed) {
                        Box(modifier = Modifier.padding(start = 0.dp)) {
                            FooterRow("SYSTEM CONFIGURATION", Icons.Default.Settings) {
                                val uri = Uri.parse("asc://settings?section=system_configuration")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                    .setPackage(context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        SidebarRemoteStatus(isCollapsed = isCollapsed, promoteMacro = promoteMacro, onViewChange = onViewChange)
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
private fun SidebarGroup(
    title: String,
    isCollapsed: Boolean,
    items: List<NavItem>,
    bringMap: MutableMap<AppView, BringIntoViewRequester>,
    currentView: AppView,
    onViewChange: (AppView) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        if (!isCollapsed) {
            Text(title, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), letterSpacing = 2.sp, fontFamily = InterFontFamily)
        }
        items.forEach { item ->
            val active = (currentView == item.view) || (currentView == AppView.DASHBOARD && item.view == AppView.MACRO_STREAM)
            val requester = remember { BringIntoViewRequester() }
            // register requester so parent can bring active item into view
            bringMap[item.view] = requester
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(44.dp)
                    .bringIntoViewRequester(requester)
                    .clickable { onViewChange(item.view) },
                color = if (active) ActiveHighlight else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Persistent active indicator (left edge) so user always sees current page
                    Box(modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(if (active) ActiveHighlight else Color.Transparent, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    if (!isCollapsed) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(item.label.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
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
        .padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SlateText, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
    }
}

// FooterProfileBox removed — avatar displayed inline in `SidebarRemoteStatus`

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
                Text(initials, color = Color.Black, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(EmeraldSuccess, RoundedCornerShape(4.dp))) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIVE", color = EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("NODE: $node", color = SlateText, fontSize = 13.sp, fontFamily = InterFontFamily)
            }

            Surface(
                color = Color(0xFF081A2B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(version, color = Color(0xFF3EA6FF), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
fun SidebarRemoteStatus(isCollapsed: Boolean, promoteMacro: Boolean, onViewChange: (AppView) -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    if (isCollapsed) return

    // Read persisted prefs for remote mode and interval
    val prefs = context.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
    val force = try { prefs.getBoolean("force_remote_override", false) } catch (_: Exception) { false }
    val interval = try { prefs.getLong("remote_poll_ms", 10000L) } catch (_: Exception) { 10000L }
    val lastFetch = RemoteConfigManager.lastFetchMillis
    val lastOk = RemoteConfigManager.lastFetchSuccess

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text("REMOTE STATUS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(3.dp))

        // First row: remote mode and interval on left (avatar moved to Macro row)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (force) "Remote: FORCE" else "Remote: RESPECT",
                    color = if (force) RoseError else Color(0xFFF59E0B),
                    modifier = Modifier.clickable {
                        val txt = if (force) "remote_mode:FORCE" else "remote_mode:RESPECT"
                        clipboard.setText(AnnotatedString(txt))
                        Toast.makeText(context, "Copied: $txt", Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 3.dp, vertical = 2.dp),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${interval}ms",
                    color = SlateText,
                    modifier = Modifier.clickable {
                        val remoteUrl = try {
                            val bcClass = com.asc.markets.BuildConfig::class.java
                            val f = bcClass.getDeclaredField("REMOTE_CONFIG_URL")
                            val v = f.get(null)
                            if (v is String) v else ""
                        } catch (_: Exception) { "" }
                        val toCopy = if (!remoteUrl.isNullOrBlank()) remoteUrl else "poll_interval_ms:${interval}"
                        clipboard.setText(AnnotatedString(toCopy))
                        Toast.makeText(context, "Copied: ${if (toCopy.length > 80) toCopy.take(80) + "..." else toCopy}", Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 3.dp, vertical = 2.dp),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 0.dp)) {
            Text(
                text = if (promoteMacro) "Macro Promoted" else "Macro Normal",
                color = if (promoteMacro) Color(0xFF10B981) else SlateText,
                modifier = Modifier
                    .clickable {
                        val txt = "promote_macro_stream:${promoteMacro}"
                        clipboard.setText(AnnotatedString(txt))
                        Toast.makeText(context, "Copied: $txt", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = InterFontFamily
            )

            Spacer(modifier = Modifier.width(12.dp))

            // last fetch small
            val now = System.currentTimeMillis()
            val ageMs = if (lastFetch > 0) now - lastFetch else Long.MAX_VALUE
            val ageText = when {
                lastFetch == 0L -> "never"
                ageMs < 60_000L -> "${ageMs / 1000}s"
                ageMs < 3_600_000L -> "${ageMs / 60_000L}m"
                else -> "${ageMs / 3_600_000L}h"
            }
            val dot = if (lastOk) Color(0xFF10B981) else Color(0xFFEF4444)
            Box(modifier = Modifier.size(8.dp).align(Alignment.CenterVertically).background(dot, shape = RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.width(6.dp))
            Text("last: $ageText", color = SlateText, fontSize = 13.sp, fontFamily = InterFontFamily, modifier = Modifier.align(Alignment.CenterVertically))

            Spacer(modifier = Modifier.weight(1f))

            // Avatar moved here: right aligned on same row as Macro Normal
            Box(modifier = Modifier.size(36.dp).align(Alignment.CenterVertically).padding(end = 4.dp, top = 4.dp, bottom = 8.dp).clickable { onViewChange(AppView.PROFILE) }.background(IndigoAccent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("JD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = InterFontFamily)
            }
        }
    }
}