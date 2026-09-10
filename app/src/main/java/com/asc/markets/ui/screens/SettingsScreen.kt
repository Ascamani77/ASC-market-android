@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")
package com.asc.markets.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.BackHandler
import com.asc.markets.data.AccumulationRadarTimeframe
import com.asc.markets.data.NetworkConfig
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import com.google.gson.Gson
import com.trading.app.data.ChartFeedType
import com.trading.app.models.ChartSettings
import com.trading.app.models.IndicatorsSettings
import kotlinx.coroutines.Dispatchers

sealed class SettingsSection(val id: String, val title: String, val icon: ImageVector, val value: String? = null) {
    object Workspace : SettingsSection("workspace", "Interface & Display", androidx.compose.material.icons.autoMirrored.outlined.Settings, "DARK")
    object Analytical : SettingsSection("analytical", "Analytical Canvas & Focus", androidx.compose.material.icons.autoMirrored.outlined.Timeline, "H1")
    object Security : SettingsSection("security", "Security Protocol", androidx.compose.material.icons.autoMirrored.outlined.Lock, "ENABLED")
    object Asset : SettingsSection("asset", "Asset Universe Filtering", androidx.compose.material.icons.autoMirrored.outlined.List)
    object Engine : SettingsSection("engine", "Engine Diagnostics", androidx.compose.material.icons.autoMirrored.outlined.Memory)
}

@Composable
fun SettingsScreen(_viewModel: ForexViewModel) {
    var activeSection by remember { mutableStateOf<SettingsSection?>(null) }

    // Handle back button: if in detail section, go back to main list
    BackHandler(enabled = activeSection != null) {
        activeSection = null
    }

    if (activeSection == null) {
        // Main settings list
        SettingsMainList(onSectionClick = { activeSection = it })
    } else {
        // Detail page for selected section
        SettingsDetailPage(
            section = activeSection!!,
            viewModel = _viewModel,
            onBack = { activeSection = null },
            onClose = { activeSection = null }
        )
    }
}

@Composable
fun SettingsMainList(onSectionClick: (SettingsSection) -> Unit) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp)) // Space for back button alignment
                Text(
                    "Settings",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(48.dp)) // Space for close button alignment
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                val context = LocalContext.current
                val sections = listOf(
                    SettingsSection.Workspace, SettingsSection.Analytical,
                    SettingsSection.Asset, SettingsSection.Engine,
                    SettingsSection.Security
                )

                sections.forEach { section ->
                    SettingsMenuRow(section) { onSectionClick(section) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2A2E39), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingsMenuRow("Export Analysis Logs", androidx.compose.material.icons.autoMirrored.outlined.Download) {
                    val prefs = context.getSharedPreferences("asc_prefs", android.content.Context.MODE_PRIVATE)
                    val logLines = buildList {
                        add("=== ASC Analysis Log Export ===")
                        add("Theme: ${prefs.getString("theme_mode", "DARK")}")
                        add("Zen Mode: ${prefs.getBoolean("zen_mode", false)}")
                        add("Risk Level: ${prefs.getString("risk_level", "Balanced")}")
                        add("Max Session Risk: ${prefs.getFloat("max_session_risk", 1.0f)}")
                        add("Min Risk Reward: ${prefs.getFloat("min_risk_reward", 2.0f)}")
                        add("Analytical Focus: ${prefs.getString("analytical_focus", "H1")}")
                        add("Intelligence Threshold: ${prefs.getInt("intelligence_threshold", 50)}")
                        add("Export timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
                    }
                    val logText = logLines.joinToString("\n")
                    val file = java.io.File(context.cacheDir, "asc_settings_export_${System.currentTimeMillis()}.txt")
                    file.writeText(logText)
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Export Settings"))
                }
                var showDeleteDialog by remember { mutableStateOf(false) }
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete All History", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = { Text("This will clear all settings and reset to defaults. This cannot be undone.", color = Color(0xFF787B86)) },
                        confirmButton = {
                            TextButton(onClick = {
                                context.getSharedPreferences("asc_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                context.getSharedPreferences("trading_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                showDeleteDialog = false
                            }) {
                                Text("Delete All", color = Color(0xFFEF4444))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel", color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF1E222D)
                    )
                }
                SettingsMenuRow("Delete all history", androidx.compose.material.icons.autoMirrored.outlined.Delete, isError = true) { showDeleteDialog = true }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SettingsDetailPage(
    section: SettingsSection,
    viewModel: ForexViewModel,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    section.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF787B86))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                SettingsDetailContent(section, viewModel)
                Spacer(modifier = Modifier.height(80.dp))
            }

            // Footer with Cancel and Ok buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp, 36.dp)
                        .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(8.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color(0xFF2A2E39)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            // Save settings
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Ok", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMenuRow(section: SettingsSection, onClick: () -> Unit) {
    SettingsMenuRow(section.title, section.icon, section.value, false, onClick)
}

@Composable
fun SettingsMenuRow(label: String, icon: ImageVector, value: String? = null, isError: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = if (isError) Color(0xFFEF4444) else Color(0xFF787B86), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = if (isError) Color(0xFFEF4444) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, color = Color(0xFF787B86), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsDetailContent(section: SettingsSection, viewModel: ForexViewModel) {
    when (section) {
        SettingsSection.Workspace -> {
            val context = LocalContext.current
            val signalConnected by com.asc.markets.data.EASignalLiveStore.isConnected.collectAsState()
            val liveDataConnected by com.asc.markets.data.EALiveDataStore.isConnected.collectAsState()
            val links by com.asc.markets.data.SystemLinkMonitor.state.collectAsState()

            Text("DATA SOURCE", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val marketLive = signalConnected || liveDataConnected
                    SettingsStatusRow("Market Data", if (marketLive) "MT5 EA Bridge (live)" else "MT5 EA Bridge (offline)", if (marketLive) Color(0xFF22C55E) else Color(0xFFEF4444))
                    SettingsStatusRow("Backend", if (links.ai) "Python Hybrid AI Server" else "AI Server offline", if (links.ai) Color(0xFF22C55E) else Color(0xFFEF4444))
                    SettingsStatusRow("Indicators", "Configurable per chart profile", IndigoAccent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("CARD OUTLINES", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    val borderAlpha by com.asc.markets.data.UiAppearanceStore.borderAlpha.collectAsState()
                    SettingsSliderRow(
                        label = "Border brightness",
                        valueLabel = "${(borderAlpha * 100).toInt()}%",
                        value = borderAlpha,
                        range = 0f..1f,
                        onValueChange = { com.asc.markets.data.UiAppearanceStore.setBorderAlpha(it) }
                    )
                    Text(
                        "Controls the lines around every info box. 0% hides them, 100% is solid white.",
                        color = Color(0xFF787B86),
                        fontSize = 12.sp
                    )
                }
            }
        }
        SettingsSection.Security -> {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE) }
            
            fun saveBoolean(key: String, value: Boolean) {
                prefs.edit().putBoolean(key, value).apply()
            }
            
            var biometricGuard by remember { mutableStateOf(com.asc.markets.data.BiometricAuthManager.isEnabled(context)) }
            var sessionTimeout by remember { mutableStateOf(prefs.getInt("session_timeout_mins", 15)) }
            val showRiskDisclosure by viewModel.showRiskDisclosure.collectAsState()
            var currentRiskDisclosure by remember(showRiskDisclosure) { mutableStateOf(showRiskDisclosure) }
            
            Text("ACCESS GOVERNANCE", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            ToggleRowControlled(
                label = "Biometric Guard",
                sub = "Require fingerprint or face ID on app launch",
                checked = biometricGuard,
                onCheckedChange = {
                    biometricGuard = it
                    com.asc.markets.data.BiometricAuthManager.setEnabled(context, it)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("SESSION CONTROL", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-Lock After", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E222D),
                        onClick = { if (sessionTimeout > 1) { sessionTimeout--; prefs.edit().putInt("session_timeout_mins", sessionTimeout).apply() } }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("${sessionTimeout}m", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E222D),
                        onClick = { if (sessionTimeout < 60) { sessionTimeout++; prefs.edit().putInt("session_timeout_mins", sessionTimeout).apply() } }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("RISK DISCLOSURE", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            ToggleRowControlled(
                label = "Show Risk Disclosure",
                sub = "Display risk warning on app startup",
                checked = currentRiskDisclosure,
                onCheckedChange = {
                    currentRiskDisclosure = it
                    viewModel.setShowRiskDisclosure(it)
                }
            )
        }
        SettingsSection.Analytical -> {
            val context = LocalContext.current
            val networkPrefs = remember {
                context.getSharedPreferences(NetworkConfig.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            }
            val tradingPrefs = remember {
                context.getSharedPreferences("trading_prefs", Context.MODE_PRIVATE)
            }
            var selectedRadarTimeframe by remember {
                mutableStateOf(AccumulationRadarTimeframe.current(context))
            }
            var streamFeedType by remember {
                mutableStateOf(ChartFeedType.streamCurrent(context))
            }
            var indicatorSettings by remember {
                mutableStateOf(loadStreamIndicatorsSettings(context, streamFeedType))
            }

            DisposableEffect(networkPrefs, context) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == ChartFeedType.STREAM_PREF_KEY || key == ChartFeedType.PREF_KEY) {
                        val updatedFeed = ChartFeedType.streamCurrent(context)
                        streamFeedType = updatedFeed
                        indicatorSettings = loadStreamIndicatorsSettings(context, updatedFeed)
                    }
                }
                networkPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    networkPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            DisposableEffect(tradingPrefs, streamFeedType) {
                val chartSettingsKey = streamChartSettingsKey(streamFeedType)
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == chartSettingsKey) {
                        indicatorSettings = loadStreamIndicatorsSettings(context, streamFeedType)
                    }
                }
                tradingPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    tradingPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            fun persistIndicators(transform: (IndicatorsSettings) -> IndicatorsSettings) {
                indicatorSettings = updateStreamIndicatorsSettings(context, streamFeedType, transform)
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text("ACCUMULATION RADAR", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the homepage radar timeframe. The widget will sample and rank assets using the selected window.",
                    color = Color(0xFF787B86),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                AccumulationRadarTimeframe.values().forEach { timeframe ->
                    val selected = selectedRadarTimeframe == timeframe
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                selectedRadarTimeframe = timeframe
                                networkPrefs.edit().putString(AccumulationRadarTimeframe.PREF_KEY, timeframe.prefValue).apply()
                            },
                        color = if (selected) Color(0xFF1E222D) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (selected) Color(0xFF363A45) else Color(0xFF2A2E39))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(timeframe.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    when (timeframe) {
                                        AccumulationRadarTimeframe.MIN_5 -> "Ultra-fast pre-move view for the last 5 minutes."
                                        AccumulationRadarTimeframe.MIN_15 -> "Short-term compression and release over 15 minutes."
                                        AccumulationRadarTimeframe.MIN_30 -> "Broader intraday radar over the last 30 minutes."
                                        AccumulationRadarTimeframe.HOUR_1 -> "Intraday accumulation scan across the last hour."
                                        AccumulationRadarTimeframe.HOUR_4 -> "Swing setup build-up over the last 4 hours."
                                        AccumulationRadarTimeframe.HOUR_12 -> "Half-day accumulation structure and drift."
                                        AccumulationRadarTimeframe.DAY_1 -> "Full 1D accumulation structure and slower radar flow."
                                    },
                                    color = Color(0xFF787B86),
                                    fontSize = 12.sp
                                )
                            }
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("INDICATOR SETTINGS", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "These controls update the current Stream chart profile using the same indicator settings payload the chart already reads.",
                    color = Color(0xFF787B86),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chart Profile", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(streamFeedType.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                        Text(streamChartSettingsKey(streamFeedType), color = SlateMuted, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                IndicatorSettingsCard(
                    title = "RSI",
                    subtitle = "Momentum oscillator visibility, period, labels, and guide lines.",
                    enabled = indicatorSettings.showRsi,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showRsi = enabled) } }
                ) {
                    IndicatorNumberField(
                        label = "Period",
                        value = indicatorSettings.rsiPeriod.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(rsiPeriod = it.coerceIn(2, 200)) } } }
                    )
                    SettingsToggleRow("Show Labels", "Display RSI labels in the chart pane.", indicatorSettings.rsiShowLabels) {
                        persistIndicators { settings -> settings.copy(rsiShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render RSI guide lines and overlay line cues.", indicatorSettings.rsiShowLines) {
                        persistIndicators { settings -> settings.copy(rsiShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "EMA 10",
                    subtitle = "Fast trend response line with adjustable period and display helpers.",
                    enabled = indicatorSettings.showEma10,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showEma10 = enabled) } }
                ) {
                    IndicatorNumberField(
                        label = "Period",
                        value = indicatorSettings.ema10Period.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(ema10Period = it.coerceIn(2, 300)) } } }
                    )
                    SettingsToggleRow("Show Labels", "Display EMA 10 labels in the chart header and pane.", indicatorSettings.ema10ShowLabels) {
                        persistIndicators { settings -> settings.copy(ema10ShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render EMA 10 line extension helpers.", indicatorSettings.ema10ShowLines) {
                        persistIndicators { settings -> settings.copy(ema10ShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "EMA 20",
                    subtitle = "Medium-speed exponential average with the current chart styling preserved.",
                    enabled = indicatorSettings.showEma20,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showEma20 = enabled) } }
                ) {
                    IndicatorNumberField(
                        label = "Period",
                        value = indicatorSettings.ema20Period.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(ema20Period = it.coerceIn(2, 300)) } } }
                    )
                    SettingsToggleRow("Show Labels", "Display EMA 20 labels in the chart header and pane.", indicatorSettings.ema20ShowLabels) {
                        persistIndicators { settings -> settings.copy(ema20ShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render EMA 20 line extension helpers.", indicatorSettings.ema20ShowLines) {
                        persistIndicators { settings -> settings.copy(ema20ShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "SMA 21",
                    subtitle = "Primary simple moving average with persisted period and visibility settings.",
                    enabled = indicatorSettings.showSma1,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showSma1 = enabled) } }
                ) {
                    IndicatorNumberField(
                        label = "Period",
                        value = indicatorSettings.sma1Period.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(sma1Period = it.coerceIn(2, 400)) } } }
                    )
                    SettingsToggleRow("Show Labels", "Display SMA 21 labels in the chart header and pane.", indicatorSettings.sma1ShowLabels) {
                        persistIndicators { settings -> settings.copy(sma1ShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render SMA 21 line extension helpers.", indicatorSettings.sma1ShowLines) {
                        persistIndicators { settings -> settings.copy(sma1ShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "SMA 10",
                    subtitle = "Secondary simple moving average with the same current chart feature set.",
                    enabled = indicatorSettings.showSma2,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showSma2 = enabled) } }
                ) {
                    IndicatorNumberField(
                        label = "Period",
                        value = indicatorSettings.sma2Period.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(sma2Period = it.coerceIn(2, 400)) } } }
                    )
                    SettingsToggleRow("Show Labels", "Display SMA 10 labels in the chart header and pane.", indicatorSettings.sma2ShowLabels) {
                        persistIndicators { settings -> settings.copy(sma2ShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render SMA 10 line extension helpers.", indicatorSettings.sma2ShowLines) {
                        persistIndicators { settings -> settings.copy(sma2ShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "VWAP",
                    subtitle = "Volume-weighted average price visibility with label and line controls.",
                    enabled = indicatorSettings.showVwap,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showVwap = enabled) } }
                ) {
                    SettingsToggleRow("Show Labels", "Display VWAP labels in the chart header and pane.", indicatorSettings.vwapShowLabels) {
                        persistIndicators { settings -> settings.copy(vwapShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render VWAP overlay line helpers.", indicatorSettings.vwapShowLines) {
                        persistIndicators { settings -> settings.copy(vwapShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "Bollinger Bands",
                    subtitle = "Band visibility with period, deviation, labels, and line helpers.",
                    enabled = indicatorSettings.showBb,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showBb = enabled) } }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        IndicatorNumberField(
                            label = "Period",
                            value = indicatorSettings.bbPeriod.toString(),
                            modifier = Modifier.weight(1f),
                            onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(bbPeriod = it.coerceIn(2, 300)) } } }
                        )
                        IndicatorDecimalField(
                            label = "Std Dev",
                            value = String.format(java.util.Locale.US, "%.1f", indicatorSettings.bbStdDev),
                            modifier = Modifier.weight(1f),
                            onValueChange = { value -> value.toFloatOrNull()?.let { persistIndicators { settings -> settings.copy(bbStdDev = it.coerceIn(0.5f, 6f)) } } }
                        )
                    }
                    SettingsToggleRow("Show Labels", "Display Bollinger labels in the chart header and pane.", indicatorSettings.bbShowLabels) {
                        persistIndicators { settings -> settings.copy(bbShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render Bollinger overlay line helpers.", indicatorSettings.bbShowLines) {
                        persistIndicators { settings -> settings.copy(bbShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "ATR",
                    subtitle = "Volatility range indicator with period, labels, and guide lines.",
                    enabled = indicatorSettings.showAtr,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showAtr = enabled) } }
                ) {
                    IndicatorNumberField(
                        label = "Period",
                        value = indicatorSettings.atrPeriod.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(atrPeriod = it.coerceIn(2, 300)) } } }
                    )
                    SettingsToggleRow("Show Labels", "Display ATR labels in the chart header and pane.", indicatorSettings.atrShowLabels) {
                        persistIndicators { settings -> settings.copy(atrShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render ATR guide line helpers.", indicatorSettings.atrShowLines) {
                        persistIndicators { settings -> settings.copy(atrShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "MACD",
                    subtitle = "Trend/momentum composite with fast, slow, signal, labels, and line helpers.",
                    enabled = indicatorSettings.showMacd,
                    onEnabledChange = { enabled -> persistIndicators { settings -> settings.copy(showMacd = enabled) } }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        IndicatorNumberField(
                            label = "Fast",
                            value = indicatorSettings.macdFast.toString(),
                            modifier = Modifier.weight(1f),
                            onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(macdFast = it.coerceIn(2, 100)) } } }
                        )
                        IndicatorNumberField(
                            label = "Slow",
                            value = indicatorSettings.macdSlow.toString(),
                            modifier = Modifier.weight(1f),
                            onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(macdSlow = it.coerceIn(2, 200)) } } }
                        )
                        IndicatorNumberField(
                            label = "Signal",
                            value = indicatorSettings.macdSignal.toString(),
                            modifier = Modifier.weight(1f),
                            onValueChange = { value -> value.toIntOrNull()?.let { persistIndicators { settings -> settings.copy(macdSignal = it.coerceIn(2, 100)) } } }
                        )
                    }
                    SettingsToggleRow("Show Labels", "Display MACD labels in the chart header and pane.", indicatorSettings.macdShowLabels) {
                        persistIndicators { settings -> settings.copy(macdShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render MACD guide line helpers.", indicatorSettings.macdShowLines) {
                        persistIndicators { settings -> settings.copy(macdShowLines = it) }
                    }
                }

                IndicatorSettingsCard(
                    title = "Volume Panel",
                    subtitle = "Keep current volume annotations and line helper visibility in sync with the chart profile.",
                    enabled = true,
                    showToggle = false,
                    onEnabledChange = {}
                ) {
                    SettingsToggleRow("Show Labels", "Display volume labels in the chart header and volume pane.", indicatorSettings.volumeShowLabels) {
                        persistIndicators { settings -> settings.copy(volumeShowLabels = it) }
                    }
                    SettingsToggleRow("Show Lines", "Render volume panel line helpers.", indicatorSettings.volumeShowLines) {
                        persistIndicators { settings -> settings.copy(volumeShowLines = it) }
                    }
                }
            }
        }
        SettingsSection.Asset -> {
            val eaAssets by com.asc.markets.data.EALiveDataStore.liveAssets.collectAsState()
            val eaConnected by com.asc.markets.data.EASignalLiveStore.isConnected.collectAsState()
            var query by remember { mutableStateOf("") }
            
            Text("EA-LIVE ASSETS", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Assets are populated by the MT5 EA bridge. Connect the EA to see live assets.", color = Color(0xFF787B86), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(10.dp).background(
                        if (eaConnected) Color(0xFF22C55E) else Color(0xFFEF4444),
                        CircleShape
                    )
                )
                Text(
                    if (eaConnected) "EA Bridge Connected" else "EA Bridge Offline",
                    color = if (eaConnected) Color(0xFF22C55E) else Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Filter assets...", color = Color(0xFF787B86)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(androidx.compose.material.icons.autoMirrored.outlined.Search, null, tint = Color(0xFF787B86)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF2A2E39),
                    focusedBorderColor = Color.White,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val filteredAssets = eaAssets.filter {
                query.isBlank() || it.symbol.contains(query, ignoreCase = true)
            }
            
            if (filteredAssets.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    if (eaConnected) "No assets matching filter" else "EA offline — no assets available",
                    color = Color(0xFF787B86),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                filteredAssets.forEach { asset ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(asset.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val conf = ((asset.eaAi?.confidence ?: 0.0) * 100).toInt()
                                val dir = (asset.eaAi?.direction ?: "").ifBlank { "—" }
                                Text("Bid: ${String.format(java.util.Locale.US, "%.5f", asset.prices.bid)}", color = Color(0xFF787B86), fontSize = 11.sp)
                                Text("Spread: ${String.format(java.util.Locale.US, "%.2f", asset.prices.spreadPercent)}%", color = Color(0xFF787B86), fontSize = 11.sp)
                                Text("AI: $conf% $dir", color = if (conf >= 50) Color(0xFF22C55E) else Color(0xFF787B86), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                }
            }
        }
        SettingsSection.Engine -> {
            val eaConnected by com.asc.markets.data.EASignalLiveStore.isConnected.collectAsState()
            val eaLiveConnected by com.asc.markets.data.EALiveDataStore.isConnected.collectAsState()
            val activeNodes by com.asc.markets.logic.VigilanceNodeEngine.activeNodeCount.collectAsState()
            val latestSignal by com.asc.markets.data.EASignalLiveStore.signal.collectAsState()
            val eaLastUpdate by com.asc.markets.data.EALiveDataStore.lastUpdateTime.collectAsState()
            
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text("CONNECTION STATUS", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingsStatusRow("EA Signal WS", if (eaConnected) "Connected" else "Disconnected", if (eaConnected) EmeraldSuccess else RoseError)
                        SettingsStatusRow("EA Live Data", if (eaLiveConnected) "Connected" else "Disconnected", if (eaLiveConnected) EmeraldSuccess else RoseError)
                        SettingsStatusRow("Active Nodes", activeNodes.toString(), IndigoAccent)
                        if (eaLastUpdate != null) {
                            val elapsed = (System.currentTimeMillis() - eaLastUpdate!!) / 1000
                            SettingsStatusRow("Last Data", "${elapsed}s ago", if (elapsed < 30) EmeraldSuccess else Color(0xFFF59E0B))
                        }
                    }
                }
                
                if (latestSignal != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("LATEST SIGNAL", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SettingsStatusRow("Asset", latestSignal?.asset ?: "—", Color.White)
                            SettingsStatusRow("Direction", latestSignal?.direction?.ifBlank { "—" } ?: "—", IndigoAccent)
                            SettingsStatusRow("Confidence", "${((latestSignal?.confidence ?: 0.0) * 100).toInt()}%", EmeraldSuccess)
                            SettingsStatusRow("Regime", latestSignal?.regime?.state ?: "—", SlateText)
                            SettingsStatusRow("Volatility", latestSignal?.volatility?.state ?: "—", SlateText)
                            SettingsStatusRow("Quality", latestSignal?.quality?.grade ?: "—", SlateText)
                        }
                    }
                }
            }
        }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("NODE CONFIG READY", color = Color(0xFF787B86), fontWeight = FontWeight.Bold)
                }
            }
        }
}

@Composable
fun ToggleRowControlled(label: String, sub: String, checked: Boolean, isHighImpact: Boolean = false, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isHighImpact) Color(0xFFEF4444) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Color(0xFF787B86), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isHighImpact) Color(0xFFEF4444) else Color.White,
                checkedTrackColor = if (isHighImpact) Color(0xFFEF4444).copy(alpha = 0.5f) else IndigoAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun ToggleRow(label: String, sub: String, checked: Boolean, isHighImpact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isHighImpact) Color(0xFFEF4444) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Color(0xFF787B86), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isHighImpact) Color(0xFFEF4444) else Color.White,
                checkedTrackColor = if (isHighImpact) Color(0xFFEF4444).copy(alpha = 0.5f) else IndigoAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun SliderRow(label: String, value: Float, min: Float, max: Float, suffix: String) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${String.format(java.util.Locale.US, "%.1f", value)}$suffix", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
        Slider(
            value = value,
            onValueChange = {},
            valueRange = min..max,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = IndigoAccent, inactiveTrackColor = GhostWhite)
        )
    }
}

@Composable
private fun SliderSettingsRow(label: String, value: Float, min: Float, max: Float, suffix: String, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("${String.format(java.util.Locale.US, "%.1f", value)}$suffix", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = IndigoAccent, inactiveTrackColor = GhostWhite)
        )
    }
}

private fun streamChartSettingsKey(feedType: ChartFeedType): String = "chart_settings_stream_${feedType.prefValue}"

private fun loadStreamChartSettings(context: Context, feedType: ChartFeedType): ChartSettings {
    val prefs = context.applicationContext.getSharedPreferences("trading_prefs", Context.MODE_PRIVATE)
    val raw = prefs.getString(streamChartSettingsKey(feedType), null)
    return raw?.let {
        runCatching { Gson().fromJson(it, ChartSettings::class.java) }.getOrNull()
    } ?: ChartSettings()
}

private fun loadStreamIndicatorsSettings(context: Context, feedType: ChartFeedType): IndicatorsSettings {
    return loadStreamChartSettings(context, feedType).indicators
}

private fun updateStreamIndicatorsSettings(
    context: Context,
    feedType: ChartFeedType,
    transform: (IndicatorsSettings) -> IndicatorsSettings
): IndicatorsSettings {
    val prefs = context.applicationContext.getSharedPreferences("trading_prefs", Context.MODE_PRIVATE)
    val current = loadStreamChartSettings(context, feedType)
    val updatedIndicators = transform(current.indicators)
    val updatedSettings = current.copy(indicators = updatedIndicators)
    prefs.edit().putString(streamChartSettingsKey(feedType), Gson().toJson(updatedSettings)).apply()
    return updatedIndicators
}

@Composable
private fun IndicatorSettingsCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    showToggle: Boolean = true,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(subtitle, color = SlateText, fontSize = 12.sp, lineHeight = 14.sp, fontFamily = InterFontFamily)
                }
                if (showToggle) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = IndigoAccent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
            if (enabled || !showToggle) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, sub: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = SlateText, fontSize = 12.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IndigoAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Text(valueLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = IndigoAccent, inactiveTrackColor = GhostWhite)
        )
    }
}

@Composable
private fun SettingsStatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}

@Composable
private fun IndicatorNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { ch -> ch.isDigit() }) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = HairlineBorder,
                focusedBorderColor = Color.White,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun IndicatorDecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { ch -> ch.isDigit() || ch == '.' }) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = HairlineBorder,
                focusedBorderColor = Color.White,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}
