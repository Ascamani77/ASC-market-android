package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import com.asc.markets.R
import androidx.compose.ui.unit.em
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.text.style.TextOverflow
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import com.asc.markets.ui.screens.dashboard.*
import com.asc.markets.ui.theme.*
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.components.LocalShowMicrostructure
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.asc.markets.BuildConfig
import android.os.Vibrator
import android.os.VibrationEffect

enum class DashboardTab { MACRO_STREAM, MARKET_OVERVIEW, TECHNICAL_VITALS, STRATEGY_SIGNALS, ANALYTICAL_QUALITY, EXECUTION_LEDGER, MARKET_PSYCHOLOGY, METHODOLOGY }

@Composable
fun DashboardScreen(viewModel: ForexViewModel) {
    var activeTab by remember { mutableStateOf(DashboardTab.MACRO_STREAM) }
    val selectedPair by viewModel.selectedPair.collectAsState()
    val promoteMacro by viewModel.promoteMacroStream.collectAsState()
    val dashboardTarget by viewModel.dashboardTabTarget.collectAsState()

    // react to external requests to focus a specific dashboard tab
    LaunchedEffect(dashboardTarget) {
        try {
            activeTab = DashboardTab.valueOf(dashboardTarget)
        } catch (_: Exception) { /* ignore invalid names */ }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
                // Remote status moved to sidebar to avoid compressing dashboard content

            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (promoteMacro) {
                    // When promoted, show the Macro Intelligence Stream prominently (90/10 enforced by the view)
                    val events by viewModel.macroStreamEvents.collectAsState()
                    CompositionLocalProvider(LocalShowMicrostructure provides false) {
                        MacroStreamView(events = events)
                    }
                } else {
                    androidx.compose.animation.Crossfade(targetState = activeTab) { tab ->
                        when (tab) {
                            DashboardTab.MACRO_STREAM -> {
                                val events by viewModel.macroStreamEvents.collectAsState()
                                CompositionLocalProvider(LocalShowMicrostructure provides false) {
                                    MacroStreamView(events = events)
                                }
                            }
                            DashboardTab.MARKET_OVERVIEW -> MarketOverviewTab(selectedPair) { pair ->
                                viewModel.selectPair(pair)
                                viewModel.navigateTo(AppView.TRADING_ASSISTANT)
                            }
                            DashboardTab.TECHNICAL_VITALS -> TechnicalVitalsTab()
                            DashboardTab.STRATEGY_SIGNALS -> StrategySignalsTab()
                            DashboardTab.ANALYTICAL_QUALITY -> AnalyticalQualityTab()
                            DashboardTab.EXECUTION_LEDGER -> ExecutionLedgerTab()
                            DashboardTab.MARKET_PSYCHOLOGY -> MarketPsychologyTab()
                            DashboardTab.METHODOLOGY -> EducationTab()
                        }
                    }
                }
            }
            
            // Fixed Top Tab Switcher (Floating style)
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), contentAlignment = Alignment.Center) {
                val context = LocalContext.current
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.wrapContentWidth().height(44.dp).padding(horizontal = 16.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(DashboardTab.values()) { tab ->
                            val active = activeTab == tab
                            Surface(
                                color = if (active) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .padding(horizontal = 2.dp)
                                    .clickable {
                                        val vib = context.getSystemService(Vibrator::class.java)
                                        vib?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                        activeTab = tab
                                        // persist selection back to ViewModel so Home can reflect it
                                        viewModel.setDashboardTab(tab.name)
                                    }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val resId = when (tab) {
                                        DashboardTab.MACRO_STREAM -> R.drawable.lucide_line_chart
                                        DashboardTab.MARKET_OVERVIEW -> R.drawable.lucide_pie_chart
                                        DashboardTab.TECHNICAL_VITALS -> R.drawable.lucide_activity
                                        DashboardTab.STRATEGY_SIGNALS -> R.drawable.lucide_list_filter
                                        DashboardTab.ANALYTICAL_QUALITY -> R.drawable.lucide_pie_chart
                                        DashboardTab.EXECUTION_LEDGER -> R.drawable.lucide_arrow_left_right
                                        DashboardTab.MARKET_PSYCHOLOGY -> R.drawable.lucide_binary
                                        DashboardTab.METHODOLOGY -> R.drawable.lucide_book_open
                                    }
                                    Icon(
                                        painter = painterResource(id = resId), 
                                        contentDescription = null, 
                                        tint = if (active) Color.Black else Color.Gray, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (active) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when(tab) {
                                                DashboardTab.MACRO_STREAM -> "Macro Intelligence Stream"
                                                DashboardTab.EXECUTION_LEDGER -> "Post-Move Audit"
                                                else -> tab.name.replace("_", " ").toLowerCase().capitalize()
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteStatusChip(viewModel: ForexViewModel) {
    val force by viewModel.forceRemoteOverride.collectAsState()
    val promote by viewModel.promoteMacroStream.collectAsState()
    val interval by viewModel.remotePollIntervalMs.collectAsState()
    val lastFetch = com.asc.markets.data.RemoteConfigManager.lastFetchMillis
    val lastOk = com.asc.markets.data.RemoteConfigManager.lastFetchSuccess

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
        // Remote mode chip
        val remoteColor = if (force) RoseError else Color(0xFFF59E0B)
        Surface(color = remoteColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.wrapContentWidth().height(28.dp).clickable {
            val txt = if (force) "remote_mode:FORCE" else "remote_mode:RESPECT"
            clipboard.setText(AnnotatedString(txt))
            Toast.makeText(context, "Copied: $txt", Toast.LENGTH_SHORT).show()
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                Box(modifier = Modifier.size(8.dp).background(Color.White, shape = CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (force) "Remote: FORCE" else "Remote: RESPECT", color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Poll interval chip
        Surface(color = Color.DarkGray.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp), modifier = Modifier.wrapContentWidth().height(28.dp).clickable {
            // Prefer copying the configured remote URL when available, otherwise copy the numeric interval
            val remoteUrl = try {
                val bcClass = com.asc.markets.BuildConfig::class.java
                val f = bcClass.getDeclaredField("REMOTE_CONFIG_URL")
                val v = f.get(null)
                if (v is String) v else ""
            } catch (_: Exception) { "" }
            val toCopy = if (!remoteUrl.isNullOrBlank()) remoteUrl else "poll_interval_ms:${interval}"
            clipboard.setText(AnnotatedString(toCopy))
            Toast.makeText(context, "Copied: ${if (toCopy.length > 80) toCopy.take(80) + "..." else toCopy}", Toast.LENGTH_SHORT).show()
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${interval}ms", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Health / promote status
        val promoteColor = if (promote) Color(0xFF10B981) else Color.Gray
        Surface(color = promoteColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.wrapContentWidth().height(28.dp).clickable {
            val txt = "promote_macro_stream:${promote}"
            clipboard.setText(AnnotatedString(txt))
            Toast.makeText(context, "Copied: $txt", Toast.LENGTH_SHORT).show()
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                Icon(androidx.compose.material.icons.autoMirrored.outlined.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(if (promote) "Macro Promoted" else "Macro Normal", color = Color.White, fontSize = 12.sp)
                    val now = System.currentTimeMillis()
                    val ageMs = if (lastFetch > 0) now - lastFetch else Long.MAX_VALUE
                    val ageText = when {
                        lastFetch == 0L -> "never"
                        ageMs < 60_000L -> "${ageMs / 1000}s"
                        ageMs < 3_600_000L -> "${ageMs / 60_000L}m"
                        else -> "${ageMs / 3_600_000L}h"
                    }
                    val statusDot = if (lastOk) Color(0xFF10B981) else Color(0xFFEF4444)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(statusDot, shape = CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("last: $ageText", color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp)
                            if (lastFetch > 0L) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val ts = sdf.format(Date(lastFetch))
                                Text(ts, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}