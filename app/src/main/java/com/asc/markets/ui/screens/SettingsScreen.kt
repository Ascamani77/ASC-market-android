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
import com.asc.markets.data.AccumulationRadarTimeframe
import com.asc.markets.data.CombinedFallbackDecision
import com.asc.markets.data.CombinedFallbackStore
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import com.google.gson.Gson
import com.asc.markets.state.AssetContextStore
import com.asc.markets.ui.screens.dashboard.getExploreItemsForContext
import com.trading.app.data.BinanceTradingMode
import com.trading.app.data.ChartFeedType
import com.trading.app.models.ChartSettings
import com.trading.app.models.IndicatorsSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

sealed class SettingsSection(val id: String, val title: String, val icon: ImageVector, val value: String? = null) {
    object Workspace : SettingsSection("workspace", "Workspace Interface", androidx.compose.material.icons.autoMirrored.outlined.Settings, "DARK")
    object Analytical : SettingsSection("analytical", "Analytical Canvas & Focus", androidx.compose.material.icons.autoMirrored.outlined.Timeline, "H1")
    object Intelligence : SettingsSection("intelligence", "Intelligence Filtering Logic", androidx.compose.material.icons.autoMirrored.outlined.FilterList)
    object Security : SettingsSection("security", "Security Protocol", androidx.compose.material.icons.autoMirrored.outlined.Lock, "ENABLED")
    object Risk : SettingsSection("risk", "Risk Governance & Surveillance", androidx.compose.material.icons.autoMirrored.outlined.AccountBalance, "BALANCED")
    object Dispatch : SettingsSection("dispatch", "Intelligence Dispatch", androidx.compose.material.icons.autoMirrored.outlined.Notifications)
    object Asset : SettingsSection("asset", "Asset Universe Filtering", androidx.compose.material.icons.autoMirrored.outlined.List)
    object ChartType : SettingsSection("chart_type", "Chart Type", androidx.compose.material.icons.autoMirrored.outlined.Timeline, "STREAM")
    object Calibration : SettingsSection("calibration", "Strategy Calibration", androidx.compose.material.icons.autoMirrored.outlined.Tune)
    object Engine : SettingsSection("engine", "Core Engine Analytical Tuning", androidx.compose.material.icons.autoMirrored.outlined.Memory)
}

@Composable
fun SettingsScreen(_viewModel: ForexViewModel) {
    var activeSection by remember { mutableStateOf<SettingsSection?>(null) }

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
                    fontSize = 20.sp,
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
                val sections = listOf(
                    SettingsSection.Workspace, SettingsSection.Analytical,
                    SettingsSection.Intelligence, SettingsSection.Security,
                    SettingsSection.Risk, SettingsSection.Dispatch,
                    SettingsSection.Asset, SettingsSection.ChartType, SettingsSection.Calibration,
                    SettingsSection.Engine
                )

                sections.forEach { section ->
                    SettingsMenuRow(section) { onSectionClick(section) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2A2E39), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                SettingsMenuRow("Export Analysis Logs", androidx.compose.material.icons.autoMirrored.outlined.Download) { /* Export */ }
                SettingsMenuRow("Delete all history", androidx.compose.material.icons.autoMirrored.outlined.Delete, isError = true) { /* Purge */ }

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
                    fontSize = 20.sp,
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
                        Text("Cancel", color = Color.White, fontSize = 14.sp)
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
                        Text("Ok", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            Text(label, color = if (isError) Color(0xFFEF4444) else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Normal)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, color = Color(0xFF787B86), fontSize = 15.sp, fontWeight = FontWeight.Normal)
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
            ToggleRow("Zen Mode", "Hide sidebar and distractions", false)
            ToggleRow("Price Ticker", "Show scrolling bottom ticker", true)
            ToggleRow("Critical Impact Ticker", "High-volatility alerts", false, isHighImpact = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text("APPEARANCE LOGIC", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E222D), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp).clickable { },
                        color = if (mode == "DARK") Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(mode, color = if (mode == "DARK") Color.Black else Color(0xFF787B86), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        SettingsSection.Security -> {
            val showRiskDisclosure by viewModel.showRiskDisclosure.collectAsState()
            ToggleRow("Zen Mode", "Hide sidebar and distractions", false)
            ToggleRow("Price Ticker", "Show scrolling bottom ticker", true)
            ToggleRow("Critical Impact Ticker", "High-volatility alerts", false, isHighImpact = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text("RISK DISCLOSURE", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            ToggleRowControlled(
                label = "Show Risk Disclosure",
                sub = "Display risk warning on app startup",
                checked = showRiskDisclosure,
                onCheckedChange = { viewModel.setShowRiskDisclosure(it) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("APPEARANCE LOGIC", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E222D), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp).clickable { },
                        color = if (mode == "DARK") Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(mode, color = if (mode == "DARK") Color.Black else Color(0xFF787B86), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
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
                                Text(timeframe.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                            Text("Chart Profile", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(streamFeedType.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Text(streamChartSettingsKey(streamFeedType), color = SlateMuted, fontSize = 9.sp, fontFamily = InterFontFamily)
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
        SettingsSection.Dispatch -> {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE) }
            val firebaseConfigured = remember { context.resources.getIdentifier("google_app_id", "string", context.packageName) != 0 }

            var enablePush by remember { mutableStateOf(prefs.getBoolean("enable_push", false)) }
            var allowVolatilityPush by remember { mutableStateOf(prefs.getBoolean("allow_volatility_push", true)) }
            var allowAiPush by remember { mutableStateOf(prefs.getBoolean("allow_ai_push", true)) }
            var allowNewsPush by remember { mutableStateOf(prefs.getBoolean("allow_news_push", true)) }
            var allowExecutionPush by remember { mutableStateOf(prefs.getBoolean("allow_execution_push", true)) }
            var allowCriticalPush by remember { mutableStateOf(prefs.getBoolean("allow_critical_push", true)) }
            var alertSound by remember { mutableStateOf(prefs.getBoolean("push_sound_enabled", true)) }
            var vibration by remember { mutableStateOf(prefs.getBoolean("push_vibration_enabled", true)) }
            var showOnLockscreen by remember { mutableStateOf(prefs.getBoolean("push_lockscreen_enabled", true)) }
            var groupedNotifications by remember { mutableStateOf(prefs.getBoolean("push_grouped_notifications", true)) }
            var cooldownMinutes by remember { mutableStateOf(prefs.getInt("push_cooldown_minutes", 10).coerceIn(1, 60)) }
            var maxAlertsPerHour by remember { mutableStateOf(prefs.getInt("push_max_alerts_per_hour", 8).coerceIn(1, 50)) }

            fun saveBoolean(key: String, value: Boolean) {
                prefs.edit().putBoolean(key, value).apply()
            }

            fun saveInt(key: String, value: Int) {
                prefs.edit().putInt(key, value).apply()
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Push notification controls now live inside the main Settings page and use the same current fonts, card treatment, and saved flags.", color = SlateText, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = InterFontFamily)
                        Text("Saved to asc_prefs and applied across volatility, AI, news, execution, and critical alerts.", color = IndigoAccent.copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSectionCard(title = "MASTER TOGGLES", icon = Icons.Default.NotificationsActive) {
                    SettingsToggleRow("Enable Push", "Master permission gate for all device notifications", enablePush) {
                        enablePush = it
                        saveBoolean("enable_push", it)
                    }
                    SettingsToggleRow("Volatility Alerts", "Allow volatility score, regime, and burst notifications", allowVolatilityPush) {
                        allowVolatilityPush = it
                        saveBoolean("allow_volatility_push", it)
                    }
                    SettingsToggleRow("AI Signals", "Allow AI line, progression scale, and phase-state notifications", allowAiPush) {
                        allowAiPush = it
                        saveBoolean("allow_ai_push", it)
                    }
                    SettingsToggleRow("News Alerts", "Allow macro and asset-specific push notifications", allowNewsPush) {
                        allowNewsPush = it
                        saveBoolean("allow_news_push", it)
                    }
                    SettingsToggleRow("Execution Alerts", "Allow fills, risk, and execution-state notifications", allowExecutionPush) {
                        allowExecutionPush = it
                        saveBoolean("allow_execution_push", it)
                    }
                    SettingsToggleRow("Critical Alerts", "Security or liquidation-risk messages that may bypass quieter modes", allowCriticalPush) {
                        allowCriticalPush = it
                        saveBoolean("allow_critical_push", it)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsSectionCard(title = "DELIVERY CONTROLS", icon = Icons.Default.Smartphone) {
                    SettingsToggleRow("Alert Sound", "Play an audible tone for allowed push categories", alertSound) {
                        alertSound = it
                        saveBoolean("push_sound_enabled", it)
                    }
                    SettingsToggleRow("Vibration", "Use device vibration when alerts are delivered", vibration) {
                        vibration = it
                        saveBoolean("push_vibration_enabled", it)
                    }
                    SettingsToggleRow("Show on Lockscreen", "Allow notifications to appear before device unlock", showOnLockscreen) {
                        showOnLockscreen = it
                        saveBoolean("push_lockscreen_enabled", it)
                    }
                    SettingsToggleRow("Grouped Notifications", "Collapse repeated alerts into grouped device notifications", groupedNotifications) {
                        groupedNotifications = it
                        saveBoolean("push_grouped_notifications", it)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsSectionCard(title = "FREQUENCY CONTROLS", icon = Icons.Default.NotificationsActive) {
                    SettingsSliderRow("Cooldown", "${cooldownMinutes} min", cooldownMinutes.toFloat(), 1f..60f) {
                        cooldownMinutes = it.toInt()
                        saveInt("push_cooldown_minutes", cooldownMinutes)
                    }
                    SettingsSliderRow("Max Alerts Per Hour", maxAlertsPerHour.toString(), maxAlertsPerHour.toFloat(), 1f..50f) {
                        maxAlertsPerHour = it.toInt()
                        saveInt("push_max_alerts_per_hour", maxAlertsPerHour)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsSectionCard(title = "DEVICE TOKEN STATUS", icon = Icons.Default.Shield) {
                    SettingsStatusRow("Push Service", if (firebaseConfigured) "Configured" else "Firebase Config Missing", if (firebaseConfigured) EmeraldSuccess else RoseError)
                    SettingsStatusRow("FCM Token", if (firebaseConfigured) "Pending Runtime Registration" else "Unavailable", if (firebaseConfigured) IndigoAccent else SlateText)
                    SettingsStatusRow("Permission Gate", if (enablePush) "Enabled by app" else "Muted by app", if (enablePush) EmeraldSuccess else Color(0xFFF59E0B))
                    SettingsStatusRow("Backend Sync", "Preference flags stored locally in asc_prefs", SlateText)
                }
            }
        }
        SettingsSection.Risk -> {
            SliderRow("Max Session Risk", 1.0f, 0.1f, 5.0f, "%")
            SliderRow("Min Risk Reward", 2.0f, 1.0f, 5.0f, " RR")
            Spacer(modifier = Modifier.height(24.dp))
            Text("RISK MODE", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E222D), RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf("CONS", "BAL", "AGG").forEach { mode ->
                    Surface(
                        modifier = Modifier.weight(1f).height(36.dp).clickable { },
                        color = if (mode == "BAL") Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(mode, color = if (mode == "BAL") Color.Black else Color(0xFF787B86), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        SettingsSection.Asset -> {
            var query by remember { mutableStateOf("") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search Universe...", color = Color(0xFF787B86)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(androidx.compose.material.icons.autoMirrored.outlined.Search, null, tint = Color(0xFF787B86)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF2A2E39),
                    focusedBorderColor = Color.White,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )
            val exploreItems = getExploreItemsForContext(AssetContextStore.get())
            exploreItems.forEach { pair ->
                AssetSettingRow(pair)
            }
        }
        SettingsSection.Engine -> {
            val promoteMacroStream by viewModel.promoteMacroStream.collectAsState()

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("MACRO STREAM PROMOTION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Text("Controls whether Macro Intelligence Stream is promoted as a landing/highlight route. This is a real persisted engine-level toggle backed by promote_macro_stream.", color = SlateText, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = InterFontFamily)
                        SettingsStatusRow("Current State", if (promoteMacroStream) "Promoted" else "Standard routing", if (promoteMacroStream) EmeraldSuccess else SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsSectionCard(title = "ADVANCED DATA / ENGINE", icon = Icons.Default.Memory) {
                    SettingsToggleRow(
                        "Promote Macro Stream",
                        "Promote Macro Intelligence Stream as a preferred entry/highlight surface when enabled.",
                        promoteMacroStream
                    ) {
                        viewModel.setPromoteMacroStream(it)
                    }
                }
            }
        }
        SettingsSection.ChartType -> {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences(NetworkConfig.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            }
            var selectedFeed by remember {
                mutableStateOf(ChartFeedType.streamCurrent(context))
            }
            var selectedBinanceMode by remember {
                mutableStateOf(BinanceTradingMode.current(context))
            }

            Text("Stream Chart Source", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select which independent source powers StreamScreen charts and the quote page.", color = Color(0xFF787B86), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            ChartFeedType.values().forEach { feedType ->
                val selected = selectedFeed == feedType
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            selectedFeed = feedType
                            prefs.edit().putString(ChartFeedType.STREAM_PREF_KEY, feedType.prefValue).apply()
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
                            Text(feedType.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                when (feedType) {
                                    ChartFeedType.EXNESS -> "MT5 bridge chart and Exness symbols."
                                    ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader bridge chart with dedicated connection."
                                    ChartFeedType.PEPPERSTONE_DEMO -> "Pepperstone demo account for testing with $50,000 virtual balance."
                                    ChartFeedType.BINANCE -> "Binance futures chart and USDT crypto assets."
                                    ChartFeedType.BINANCE_CONNECT -> "Public Binance WebSocket for real-time crypto charts (view-only)."
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

            Spacer(modifier = Modifier.height(20.dp))
            Text("Binance Trading Mode", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Choose whether Binance live trading uses your real account or your demo/testnet keys from env.demo.", color = Color(0xFF787B86), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E222D), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                BinanceTradingMode.values().forEach { mode ->
                    val selected = selectedBinanceMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable {
                                selectedBinanceMode = mode
                                prefs.edit().putString(BinanceTradingMode.PREF_KEY, mode.prefValue).apply()
                            },
                        color = if (selected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                mode.displayName,
                                color = if (selected) Color.Black else Color(0xFF787B86),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
            SettingsSection.Intelligence -> {
                val force by viewModel.forceRemoteOverride.collectAsState()
                val interval by viewModel.remotePollIntervalMs.collectAsState()
                val context = LocalContext.current
                val prefs = remember {
                    context.getSharedPreferences("asc_prefs", android.content.Context.MODE_PRIVATE)
                }

                var backendUrl by remember {
                    mutableStateOf(prefs.getString("backend_url", NetworkConfig.DEFAULT_BACKEND_URL) ?: NetworkConfig.DEFAULT_BACKEND_URL)
                }
                var redisHost by remember {
                    mutableStateOf(prefs.getString("redis_host", NetworkConfig.DEFAULT_HOST) ?: NetworkConfig.DEFAULT_HOST)
                }
                var redisPortText by remember { mutableStateOf(prefs.getInt("redis_port", 6379).toString()) }
                var mt5Host by remember {
                    mutableStateOf(NetworkConfig.normalizedHost(prefs.getString("mt5_host", NetworkConfig.DEFAULT_HOST) ?: NetworkConfig.DEFAULT_HOST))
                }
                var mt5PortText by remember { mutableStateOf(prefs.getInt("mt5_port", NetworkConfig.DEFAULT_MT5_PORT).toString()) }
                var cTraderHost by remember {
                    mutableStateOf(NetworkConfig.normalizedHost(prefs.getString("ctrader_host", NetworkConfig.DEFAULT_HOST) ?: NetworkConfig.DEFAULT_HOST))
                }
                var cTraderPortText by remember { mutableStateOf(prefs.getInt("ctrader_port", NetworkConfig.DEFAULT_CTRADER_PORT).toString()) }
                var streamName by remember {
                    mutableStateOf(prefs.getString("stream_name", "market.ticks.stream") ?: "market.ticks.stream")
                }
                var publishApiKey by remember {
                    mutableStateOf(prefs.getString("publish_api_key", "") ?: "")
                }
                var saveMessage by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                var isCheckingConnection by remember { mutableStateOf(false) }
                var isConnected by remember { mutableStateOf<Boolean?>(null) }
                var isCheckingMt5 by remember { mutableStateOf(false) }
                var mt5Connected by remember { mutableStateOf<Boolean?>(null) }
                var isCheckingCTrader by remember { mutableStateOf(false) }
                var cTraderConnected by remember { mutableStateOf<Boolean?>(null) }
                val combinedFallbackState by CombinedFallbackStore.state.collectAsState()
                val combinedFallbackEnabled = combinedFallbackState.isActive &&
                    combinedFallbackState.decision == CombinedFallbackDecision.ACCEPTED

                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Network / Live Data", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Update these when your hotspot/Wi-Fi IP changes.", color = SlateMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    ToggleRowControlled(
                        label = "Combined fallback",
                        sub = "Manual only. Keep off for strict Pepperstone prices.",
                        checked = combinedFallbackEnabled,
                        onCheckedChange = { enabled ->
                            prefs.edit().putBoolean("combined_fallback_manual_enabled", enabled).apply()
                            CombinedFallbackStore.setManualEnabled(enabled)
                            saveMessage = if (enabled) {
                                "Combined fallback enabled manually."
                            } else {
                                "Combined fallback disabled. Pepperstone only."
                            }
                        },
                        isHighImpact = true
                    )

                    Text(
                        "Default live source: Pepperstone cTrader. Combined fallback will not auto-start or ask permission.",
                        color = SlateMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("Backend URL", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = redisHost,
                        onValueChange = { redisHost = it },
                        label = { Text("Redis Host", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = redisPortText,
                        onValueChange = { redisPortText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Redis Port", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = mt5Host,
                        onValueChange = { mt5Host = it },
                        label = { Text("MT5 Bridge Host", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = mt5PortText,
                        onValueChange = { mt5PortText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("MT5 Bridge Port", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = cTraderHost,
                        onValueChange = { cTraderHost = it },
                        label = { Text("Pepperstone cTrader Bridge Host", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = cTraderPortText,
                        onValueChange = { cTraderPortText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Pepperstone cTrader Bridge Port", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = streamName,
                        onValueChange = { streamName = it },
                        label = { Text("Stream Name", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    OutlinedTextField(
                        value = publishApiKey,
                        onValueChange = { publishApiKey = it },
                        label = { Text("Publish API Key (optional)", color = SlateMuted) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val port = redisPortText.toIntOrNull()
                            val mt5Port = mt5PortText.toIntOrNull()
                            val cTraderPort = cTraderPortText.toIntOrNull()
                            if (port == null) {
                                saveMessage = "Invalid Redis port"
                            } else if (mt5Port == null) {
                                saveMessage = "Invalid MT5 bridge port"
                            } else if (cTraderPort == null) {
                                saveMessage = "Invalid Pepperstone cTrader bridge port"
                            } else {
                                val cleanMt5Host = mt5Host.trim()
                                val cleanCTraderHost = cTraderHost.trim()
                                prefs.edit()
                                    .putString("backend_url", NetworkConfig.normalizedBackendUrl(backendUrl))
                                    .putString("redis_host", redisHost.trim())
                                    .putInt("redis_port", port)
                                    .putString("mt5_host", cleanMt5Host)
                                    .putInt("mt5_port", mt5Port)
                                    .putString("mt5_bridge_url", "$cleanMt5Host:$mt5Port")
                                    .putString("ctrader_host", cleanCTraderHost)
                                    .putInt("ctrader_port", cTraderPort)
                                    .putString("stream_name", streamName.trim())
                                    .putString("publish_api_key", publishApiKey.trim())
                                    .apply()
                                AiRetrofitClient.configure(NetworkConfig.normalizedBackendUrl(backendUrl))
                                saveMessage = "Network settings saved. Reopen app to reinitialize connections."
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                    ) {
                        Text("Save Network Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (saveMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(saveMessage ?: "", color = SlateText, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val base = backendUrl.trim().removeSuffix("/")
                            if (base.isBlank()) {
                                isConnected = false
                                saveMessage = "Backend URL is empty"
                                return@Button
                            }
                            isCheckingConnection = true
                            saveMessage = null
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient.Builder().build()
                                        val request = Request.Builder().url("$base/health").get().build()
                                        client.newCall(request).execute().use { it.isSuccessful }
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                isConnected = ok
                                isCheckingConnection = false
                                saveMessage = if (ok) {
                                    "Connection active"
                                } else {
                                    "Disconnected (failed to reach /health)"
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                    ) {
                        Text(if (isCheckingConnection) "Testing..." else "Test Connection", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val cleanMt5Host = mt5Host.trim()
                            val mt5Port = mt5PortText.toIntOrNull()
                            if (cleanMt5Host.isBlank() || mt5Port == null) {
                                saveMessage = "Invalid MT5 host/port"
                                return@Button
                            }
                            isCheckingMt5 = true
                            saveMessage = null
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient.Builder()
                                            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                                            .build()
                                        val request = Request.Builder()
                                            .url("ws://$cleanMt5Host:$mt5Port")
                                            .build()
                                        val socket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
                                            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                                                webSocket.close(1000, "Test complete")
                                            }
                                            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {}
                                            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {}
                                            override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) {}
                                            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {}
                                        })
                                        kotlinx.coroutines.delay(2000)
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                mt5Connected = ok
                                isCheckingMt5 = false
                                saveMessage = if (ok) {
                                    "MT5 Bridge reachable"
                                } else {
                                    "MT5 Bridge unreachable (check host/port/firewall)"
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                    ) {
                        Text(if (isCheckingMt5) "Testing MT5..." else "Test MT5 Bridge", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (mt5Connected != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mt5StatusColor = if (mt5Connected == true) Color(0xFF22C55E) else Color(0xFFEF4444)
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(mt5StatusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (mt5Connected == true) "MT5 Bridge connected" else "MT5 Bridge failed", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val cleanCTraderHost = cTraderHost.trim()
                            val cTraderPort = cTraderPortText.toIntOrNull()
                            if (cleanCTraderHost.isBlank() || cTraderPort == null) {
                                saveMessage = "Invalid Pepperstone cTrader host/port"
                                return@Button
                            }
                            isCheckingCTrader = true
                            saveMessage = null
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val connected = java.util.concurrent.atomic.AtomicBoolean(false)
                                        val completed = java.util.concurrent.CountDownLatch(1)
                                        val client = OkHttpClient.Builder()
                                            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                                            .build()
                                        val request = Request.Builder()
                                            .url("ws://$cleanCTraderHost:$cTraderPort")
                                            .build()
                                        val socket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
                                            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                                                connected.set(true)
                                                webSocket.close(1000, "Test complete")
                                                completed.countDown()
                                            }
                                            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                                                connected.set(false)
                                                completed.countDown()
                                            }
                                        })
                                        val finished = completed.await(3, java.util.concurrent.TimeUnit.SECONDS)
                                        if (!finished) {
                                            socket.cancel()
                                        }
                                        finished && connected.get()
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                cTraderConnected = ok
                                isCheckingCTrader = false
                                saveMessage = if (ok) {
                                    "Pepperstone cTrader Bridge reachable"
                                } else {
                                    "Pepperstone cTrader Bridge unreachable (check host/port/firewall)"
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                    ) {
                        Text(if (isCheckingCTrader) "Testing cTrader..." else "Test Pepperstone cTrader Bridge", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (cTraderConnected != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cTraderStatusColor = if (cTraderConnected == true) Color(0xFF22C55E) else Color(0xFFEF4444)
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(cTraderStatusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (cTraderConnected == true) "Pepperstone cTrader Bridge connected" else "Pepperstone cTrader Bridge failed", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = when {
                            isCheckingConnection -> Color(0xFFFFB020)
                            isConnected == true -> Color(0xFF22C55E)
                            isConnected == false -> Color(0xFFEF4444)
                            else -> Color(0xFF6B7280)
                        }
                        val statusText = when {
                            isCheckingConnection -> "Checking connection..."
                            isConnected == true -> "Connected"
                            isConnected == false -> "Disconnected"
                            else -> "Not tested yet"
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(statusText, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Remote Feature Flags", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    ToggleRowControlled(
                        label = "Force Remote Override",
                        sub = "When enabled, remote promote macro stream flag overrides local preference",
                        checked = force,
                        onCheckedChange = { viewModel.setForceRemoteOverride(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Poll Interval (ms)", color = SlateText, fontSize = 10.sp)
                    OutlinedTextField(
                        value = interval.toString(),
                        onValueChange = { v ->
                            val cleaned = v.filter { it.isDigit() }
                            val parsed = cleaned.toLongOrNull()
                            if (parsed != null) viewModel.setRemotePollIntervalMs(parsed)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                    )
                    Text("Minimum 2000ms — higher values reduce network use.", color = SlateMuted, fontSize = 10.sp)
                }
            }
            SettingsSection.Calibration -> {
                val sensitivity by viewModel.patternSensitivity.collectAsState()
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Pattern Sensitivity", color = Color(0xFF787B86), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Controls how aggressively the vigilance engine treats pattern matches (0 = permissive, 100 = strict)", color = Color(0xFF787B86), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = sensitivity,
                            onValueChange = { viewModel.setPatternSensitivity(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF2962FF), inactiveTrackColor = Color(0xFF1E222D))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${sensitivity.toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun ToggleRowControlled(label: String, sub: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, isHighImpact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = if (isHighImpact) Color(0xFFEF4444) else Color.White,
                uncheckedColor = Color(0xFF434651),
                checkmarkColor = Color.Black
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isHighImpact) Color(0xFFEF4444) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Normal)
            Text(sub, color = Color(0xFF787B86), fontSize = 12.sp)
        }
    }
}

@Composable
fun ToggleRow(label: String, sub: String, checked: Boolean, isHighImpact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = if (isHighImpact) Color(0xFFEF4444) else Color.White,
                uncheckedColor = Color(0xFF434651),
                checkmarkColor = Color.Black
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isHighImpact) Color(0xFFEF4444) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Normal)
            Text(sub, color = Color(0xFF787B86), fontSize = 12.sp)
        }
    }
}

@Composable
fun SliderRow(label: String, value: Float, min: Float, max: Float, suffix: String) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label.uppercase(), color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("${String.format(java.util.Locale.US, "%.1f", value)}$suffix", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
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
fun AssetSettingRow(pair: com.asc.markets.data.ForexPair) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PairFlags(pair.symbol, 24)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(pair.symbol, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(pair.name.uppercase(), color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Icon(androidx.compose.material.icons.autoMirrored.outlined.Visibility, null, tint = SlateText, modifier = Modifier.size(20.dp))
        }
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
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text(subtitle, color = SlateText, fontSize = 10.sp, lineHeight = 14.sp, fontFamily = InterFontFamily)
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
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
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
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = SlateText, fontSize = 11.sp, lineHeight = 15.sp, fontFamily = InterFontFamily)
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
            Text(label.uppercase(), color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Text(valueLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
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
        Text(label.uppercase(), color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
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
        Text(label.uppercase(), color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { ch -> ch.isDigit() }) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily),
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
        Text(label.uppercase(), color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(input.filter { ch -> ch.isDigit() || ch == '.' }) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily),
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
