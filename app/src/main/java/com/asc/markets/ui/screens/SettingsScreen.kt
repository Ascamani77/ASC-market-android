@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")
package com.asc.markets.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.asc.markets.data.CombinedFallbackDecision
import com.asc.markets.data.CombinedFallbackStore
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import com.asc.markets.state.AssetContextStore
import com.asc.markets.ui.screens.dashboard.getExploreItemsForContext
import com.trading.app.data.BinanceTradingMode
import com.trading.app.data.ChartFeedType
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeSection != null) {
                IconButton(onClick = { activeSection = null }) {
                    Icon(androidx.compose.material.icons.autoMirrored.outlined.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = activeSection?.title ?: "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontSize = if (activeSection == null) 36.sp else 24.sp
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (activeSection == null) {
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    val sections = listOf(
                        SettingsSection.Workspace, SettingsSection.Analytical, 
                        SettingsSection.Intelligence, SettingsSection.Security,
                        SettingsSection.Risk, SettingsSection.Dispatch,
                        SettingsSection.Asset, SettingsSection.ChartType, SettingsSection.Calibration,
                        SettingsSection.Engine
                    )
                    
                    sections.forEach { section ->
                        SettingsMenuRow(section) { activeSection = section }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = HairlineBorder)
                    
                    SettingsMenuRow("Export Analysis Logs", androidx.compose.material.icons.autoMirrored.outlined.Download) { /* Export */ }
                    SettingsMenuRow("Delete all history", androidx.compose.material.icons.autoMirrored.outlined.Delete, isError = true) { /* Purge */ }
                }
            } else {
                SettingsDetailContent(activeSection!!, _viewModel)
            }
        }

        if (activeSection == null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { /* Save */ },
                    modifier = Modifier.width(200.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B))
                ) {
                    Icon(androidx.compose.material.icons.autoMirrored.outlined.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE CONFIG", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                InfoBox(minHeight = 60.dp) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(androidx.compose.material.icons.autoMirrored.outlined.Info, contentDescription = null, tint = SlateText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Parameters influence the internal node's local weighting. Changes are committed to secure hardware storage.".uppercase(),
                                color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Medium
                        )
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
            .height(64.dp)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isError) RoseError else SlateText, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = if (isError) RoseError else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, color = SlateMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(androidx.compose.material.icons.autoMirrored.outlined.ChevronRight, contentDescription = null, tint = Color.DarkGray)
        }
    }
}

@Composable
fun SettingsDetailContent(section: SettingsSection, viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 100.dp)) {
        when (section) {
            SettingsSection.Workspace -> {
                ToggleRow("Zen Mode", "Hide sidebar and distractions", false)
                ToggleRow("Price Ticker", "Show scrolling bottom ticker", true)
                ToggleRow("Critical Impact Ticker", "High-volatility alerts", false, isHighImpact = true)
                Spacer(modifier = Modifier.height(24.dp))
                Text("APPEARANCE LOGIC", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().background(GhostWhite, RoundedCornerShape(12.dp)).padding(4.dp)) {
                    listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp).clickable { },
                            color = if (mode == "DARK") Color.White else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(mode, color = if (mode == "DARK") Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
            SettingsSection.Risk -> {
                SliderRow("Max Session Risk", 1.0f, 0.1f, 5.0f, "%")
                SliderRow("Min Risk Reward", 2.0f, 1.0f, 5.0f, " RR")
                Spacer(modifier = Modifier.height(24.dp))
                Text("RISK MODE", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().background(GhostWhite, RoundedCornerShape(12.dp)).padding(4.dp)) {
                    listOf("CONS", "BAL", "AGG").forEach { mode ->
                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp).clickable { },
                            color = if (mode == "BAL") Color.White else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(mode, color = if (mode == "BAL") Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
                    placeholder = { Text("Search Universe...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(androidx.compose.material.icons.autoMirrored.outlined.Search, null, tint = SlateText) },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = HairlineBorder, focusedBorderColor = Color.White)
                )
                val exploreItems = getExploreItemsForContext(AssetContextStore.get())
                exploreItems.forEach { pair ->
                    AssetSettingRow(pair)
                }
            }
            SettingsSection.Engine -> {
                SliderRow("Node Lookback Depth", 500f, 100f, 5000f, " Bars")
                ToggleRow("HTF Context Aggregator", "Deep structural scanning", true)
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

                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Stream Chart Source", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select which independent source powers StreamScreen charts and the quote page.", color = SlateMuted, fontSize = 10.sp)
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
                            color = if (selected) Color.White else GhostWhite,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(feedType.displayName, color = if (selected) Color.Black else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        when (feedType) {
                                            ChartFeedType.EXNESS -> "MT5 bridge chart and Exness symbols."
                                            ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader bridge chart with dedicated connection."
                                            ChartFeedType.BINANCE -> "Binance futures chart and USDT crypto assets."
                                            ChartFeedType.BINANCE_CONNECT -> "Public Binance WebSocket for real-time crypto charts (view-only)."
                                        },
                                        color = if (selected) Color.DarkGray else SlateMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Binance Trading Mode", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose whether Binance live trading uses your real account or your demo/testnet keys from env.demo.", color = SlateMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GhostWhite, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        BinanceTradingMode.values().forEach { mode ->
                            val selected = selectedBinanceMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable {
                                        selectedBinanceMode = mode
                                        prefs.edit().putString(BinanceTradingMode.PREF_KEY, mode.prefValue).apply()
                                    },
                                color = if (selected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        mode.displayName,
                                        color = if (selected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                    Text("Pattern Sensitivity", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Controls how aggressively the vigilance engine treats pattern matches (0 = permissive, 100 = strict)", color = SlateMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = sensitivity,
                            onValueChange = { viewModel.setPatternSensitivity(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = IndigoAccent, inactiveTrackColor = GhostWhite)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${sensitivity.toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("NODE CONFIG READY", color = Color.DarkGray, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ToggleRowControlled(label: String, sub: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, isHighImpact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isHighImpact) RoseError else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub.uppercase(), color = SlateMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = if (isHighImpact) RoseError else IndigoAccent)
        )
    }
}

@Composable
fun ToggleRow(label: String, sub: String, checked: Boolean, isHighImpact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isHighImpact) RoseError else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub.uppercase(), color = SlateMuted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = {},
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = if (isHighImpact) RoseError else IndigoAccent)
        )
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
