package com.asc.markets.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.logic.ConnectivityManager
import com.asc.markets.logic.ConnectionState
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Composable
fun AiSettingsScreen(viewModel: ForexViewModel) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE) }
    val aiServerUrl = NetworkConfig.DEFAULT_SCANNER_URL

    val connectionState by ConnectivityManager.state.collectAsState()
    val eaConnected by EASignalLiveStore.isConnected.collectAsState()

    var backendUrl by remember { mutableStateOf(prefs.getString("backend_url", aiServerUrl) ?: aiServerUrl) }
    var mt5Host by remember { mutableStateOf(NetworkConfig.normalizedHost(prefs.getString("mt5_host", NetworkConfig.DEFAULT_HOST) ?: NetworkConfig.DEFAULT_HOST)) }
    var mt5PortText by remember { mutableStateOf(prefs.getInt("mt5_port", NetworkConfig.DEFAULT_MT5_PORT).toString()) }

    var analyticalFocus by remember { mutableStateOf(prefs.getString("analytical_focus", "H1") ?: "H1") }
    var intelligenceThreshold by remember { mutableStateOf(prefs.getInt("intelligence_threshold", 50)) }

    var backendTestState by remember { mutableStateOf<Boolean?>(null) }
    var mt5TestState by remember { mutableStateOf<Boolean?>(null) }
    var isTestingBackend by remember { mutableStateOf(false) }
    var isTestingMt5 by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var aiServerOk by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(aiServerUrl) {
        while (true) {
            val base = aiServerUrl.removeSuffix("/")
            val ok = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder().connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS).readTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
                    val request = Request.Builder().url("$base/signals").get().build()
                    client.newCall(request).execute().use { it.isSuccessful }
                } catch (_: Exception) { false }
            }
            aiServerOk = ok
            kotlinx.coroutines.delay(5000)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 36.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI & CONNECTION", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // ─── CONNECTION STATUS OVERVIEW ───
                InfoBox(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SYSTEM STATUS", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(12.dp))

                        ConnectionStatusRow("Data Feed", when (connectionState) {
                            ConnectionState.LIVE -> "Live"
                            ConnectionState.STALE -> "Stale"
                            ConnectionState.DEGRADED -> "Degraded"
                            ConnectionState.DISCONNECTED -> "Offline"
                            ConnectionState.PAUSED -> "Paused"
                        }, when (connectionState) {
                            ConnectionState.LIVE -> EmeraldSuccess
                            ConnectionState.STALE -> Color(0xFFF59E0B)
                            ConnectionState.DEGRADED -> Color(0xFFF59E0B)
                            ConnectionState.DISCONNECTED -> RoseError
                            ConnectionState.PAUSED -> RoseError
                        })

                        ConnectionStatusRow("EA Bridge", if (eaConnected) "Connected" else "Disconnected", if (eaConnected) EmeraldSuccess else RoseError)

                        val aiText = when (aiServerOk) {
                            null -> "Checking…"
                            true -> "Reachable"
                            false -> "Unreachable"
                        }
                        val aiColor: Color = when (aiServerOk) {
                            null -> SlateText
                            true -> EmeraldSuccess
                            false -> RoseError
                        }
                        ConnectionStatusRow("AI Server", aiText, aiColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── EA BRIDGE CONNECTION ───
                AiSettingsSectionCard(title = "EA BRIDGE", subtitle = "MT5 WebSocket bridge for live EA signals", icon = Icons.Default.SmartToy) {
                    AiSettingsInput("MT5 Host", mt5Host, "e.g. 192.168.1.198", onValueChange = { mt5Host = it })
                    AiSettingsInput("MT5 Port", mt5PortText, "8081", keyboardType = KeyboardType.Number, onValueChange = { mt5PortText = it.filter { ch -> ch.isDigit() } })

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiTestButton("Test Bridge", isTestingMt5) {
                            val host = mt5Host.trim()
                            val port = mt5PortText.toIntOrNull()
                            if (host.isBlank() || port == null) {
                                saveMessage = "Invalid host/port"
                                return@AiTestButton
                            }
                            isTestingMt5 = true
                            saveMessage = null
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient.Builder().connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS).build()
                                        val request = Request.Builder().url("ws://$host:$port").build()
                                        val socket = client.newWebSocket(request, object : WebSocketListener() {
                                            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { webSocket.close(1000, "Test") }
                                        })
                                        kotlinx.coroutines.delay(2000)
                                        true
                                    } catch (_: Exception) { false }
                                }
                                mt5TestState = ok
                                isTestingMt5 = false
                                saveMessage = if (ok) "MT5 Bridge reachable" else "MT5 Bridge unreachable"
                            }
                        }
                    }
                    mt5TestState?.let {
                        ConnectionIndicator(it, if (it) "Bridge connected" else "Bridge failed")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── AI SERVER CONNECTION ───
                AiSettingsSectionCard(title = "AI SERVER (HYBRID)", subtitle = "hybrid_ai_server.py — live AI signals + health", icon = Icons.Default.Dns) {
                    AiSettingsInput("AI Server URL", backendUrl, aiServerUrl, onValueChange = { backendUrl = it })

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiTestButton("Test Backend", isTestingBackend) {
                            val base = backendUrl.trim().removeSuffix("/")
                            if (base.isBlank()) { saveMessage = "URL is empty"; return@AiTestButton }
                            isTestingBackend = true
                            saveMessage = null
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient.Builder().connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS).build()
                                        val request = Request.Builder().url("$base/signals").get().build()
                                        client.newCall(request).execute().use { it.isSuccessful }
                                    } catch (_: Exception) { false }
                                }
                                backendTestState = ok
                                isTestingBackend = false
                                saveMessage = if (ok) "AI server connected" else "AI server unreachable"
                            }
                        }
                    }
                    backendTestState?.let {
                        ConnectionIndicator(it, if (it) "AI server connected" else "AI server failed")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── AI INTELLIGENCE ───
                AiSettingsSectionCard(title = "AI INTELLIGENCE", subtitle = "Analysis parameters and signal thresholds", icon = Icons.Default.Psychology) {
                    Text("Analytical Focus", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Primary timeframe for AI analysis", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF17171D), RoundedCornerShape(8.dp)).padding(3.dp)) {
                        listOf("M1", "M5", "M15", "H1", "H4", "D1").forEach { focus ->
                            val selected = analyticalFocus == focus
                            Box(
                                modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) IndigoAccent else Color.Transparent).clickable {
                                    analyticalFocus = focus
                                    prefs.edit().putString("analytical_focus", focus).apply()
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(focus, color = if (selected) Color.White else SlateText, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontFamily = InterFontFamily)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Signal Threshold", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Minimum confidence for AI signals", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = intelligenceThreshold.toFloat(),
                            onValueChange = {
                                intelligenceThreshold = it.toInt()
                                prefs.edit().putInt("intelligence_threshold", intelligenceThreshold).apply()
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = IndigoAccent, inactiveTrackColor = Color(0xFF17171D))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("$intelligenceThreshold%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, modifier = Modifier.width(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── SAVE BUTTON ───
                Button(
                    onClick = {
                        val mt5Port = mt5PortText.toIntOrNull()
                        if (mt5Port == null) {
                            saveMessage = "Invalid port number"
                        } else {
                            val cleanMt5Host = mt5Host.trim()
                            prefs.edit()
                                .putString("backend_url", NetworkConfig.normalizedBackendUrl(backendUrl))
                                .putString("mt5_host", cleanMt5Host)
                                .putInt("mt5_port", mt5Port)
                                .putString("mt5_bridge_url", "$cleanMt5Host:$mt5Port")
                                .apply()
                            AiRetrofitClient.configure(backendUrl.trim().ifBlank { aiServerUrl })
                            saveMessage = "Settings saved"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE CONNECTION SETTINGS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = InterFontFamily)
                }

                saveMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = IndigoAccent, fontSize = 11.sp, fontFamily = InterFontFamily)
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun AiSettingsSectionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp), color = IndigoAccent.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Text(subtitle, color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun AiSettingsInput(label: String, value: String, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            placeholder = { Text(placeholder, color = Color(0xFF333340), fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedBorderColor = IndigoAccent,
                unfocusedContainerColor = Color(0xFF111115),
                focusedContainerColor = Color(0xFF111115),
                cursorColor = IndigoAccent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = InterFontFamily)
        )
    }
}

@Composable
private fun AiTestButton(label: String, isLoading: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF17171D),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.height(36.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = IndigoAccent, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.WifiFind, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun ConnectionIndicator(connected: Boolean, label: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(if (connected) EmeraldSuccess else RoseError, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
    }
}

@Composable
private fun ConnectionStatusRow(label: String, status: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
            Text(status, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}
