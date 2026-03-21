package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import com.asc.markets.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import com.asc.markets.ui.screens.dashboard.*
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.LocalShowMicrostructure
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow

enum class DashboardTab { 
    MACRO_STREAM, 
    TECHNICAL_VITALS, 
    STRATEGY_SIGNALS, 
    ANALYTICAL_QUALITY, 
    MARKET_PSYCHOLOGY, 
    METHODOLOGY,
    EXECUTION_LEDGER
}

@Composable
fun DashboardScreen(viewModel: ForexViewModel) {
    var activeTab by remember { mutableStateOf(DashboardTab.MACRO_STREAM) }
    val selectedPair by viewModel.selectedPair.collectAsState()
    val promoteMacro by viewModel.promoteMacroStream.collectAsState()
    val dashboardTarget by viewModel.dashboardTabTarget.collectAsState()

    LaunchedEffect(dashboardTarget) {
        try {
            activeTab = DashboardTab.valueOf(dashboardTarget)
        } catch (_: Exception) { }
    }

    Column(modifier = Modifier.fillMaxSize().background(PureBlack)) {
        // 1. Top Navbar
        DashboardTopNavbar(
            activeTab = activeTab,
            onTabSelected = { tab ->
                activeTab = tab
                viewModel.setDashboardTab(tab.name)
            }
        )

        // Added space between navbar and content to prevent InfoBox from touching the navbar
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Content Area
        Box(modifier = Modifier.weight(1f)) {
            if (promoteMacro) {
                val events by viewModel.macroStreamEvents.collectAsState()
                CompositionLocalProvider(LocalShowMicrostructure provides false) {
                    MacroStreamView(events = events, viewModel = viewModel)
                }
            } else {
                androidx.compose.animation.Crossfade(targetState = activeTab, label = "TabTransition") { tab ->
                    when (tab) {
                        DashboardTab.MACRO_STREAM -> {
                            val events by viewModel.macroStreamEvents.collectAsState()
                            CompositionLocalProvider(LocalShowMicrostructure provides false) {
                                MacroStreamView(events = events, viewModel = viewModel)
                            }
                        }
                        DashboardTab.TECHNICAL_VITALS -> TechnicalVitalsTab(viewModel)
                        DashboardTab.STRATEGY_SIGNALS -> StrategySignalsTab(viewModel)
                        DashboardTab.ANALYTICAL_QUALITY -> AnalyticalQualityTab(viewModel)
                        DashboardTab.EXECUTION_LEDGER -> ExecutionLedgerTab(viewModel)
                        DashboardTab.MARKET_PSYCHOLOGY -> MarketPsychologyTab(viewModel)
                        DashboardTab.METHODOLOGY -> EducationTab(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTopNavbar(
    activeTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    val context = LocalContext.current
    Surface(
        color = Color(0xFF141414), // Dark gray background matching the image style
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(DashboardTab.entries.toTypedArray()) { tab ->
                    val active = activeTab == tab
                    val label = when (tab) {
                        DashboardTab.MACRO_STREAM -> "Top news"
                        DashboardTab.TECHNICAL_VITALS -> "Vitals"
                        DashboardTab.STRATEGY_SIGNALS -> "Signals"
                        DashboardTab.ANALYTICAL_QUALITY -> "Quality"
                        DashboardTab.MARKET_PSYCHOLOGY -> "Psychology"
                        DashboardTab.METHODOLOGY -> "Logic"
                        DashboardTab.EXECUTION_LEDGER -> "Audit"
                    }

                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .clickable {
                                val vib = context.getSystemService(Vibrator::class.java)
                                vib?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                onTabSelected(tab)
                            }
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.White else Color(0xFF8E8E8E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // The bold white line indicator below the active tab
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .fillMaxWidth()
                                .background(if (active) Color.White else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
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

        Surface(color = Color.DarkGray.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp), modifier = Modifier.wrapContentWidth().height(28.dp).clickable {
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
