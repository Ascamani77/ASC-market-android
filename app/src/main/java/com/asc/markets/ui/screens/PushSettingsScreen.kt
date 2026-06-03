package com.asc.markets.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun PushSettingsScreen(viewModel: com.asc.markets.logic.ForexViewModel) {
    val currentView by viewModel.currentView.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("PUSH NOTIFICATION", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)

        Spacer(modifier = Modifier.height(24.dp))

        PushSettingsSection(title = "MASTER TOGGLES", icon = Icons.Default.NotificationsActive) {
            PushToggleRow(
                label = "Enable Push",
                sub = "Master permission gate for all device notifications",
                checked = enablePush,
                onCheckedChange = {
                    enablePush = it
                    saveBoolean("enable_push", it)
                }
            )
            PushToggleRow(
                label = "Volatility Alerts",
                sub = "Allow volatility score, regime, and burst notifications",
                checked = allowVolatilityPush,
                onCheckedChange = {
                    allowVolatilityPush = it
                    saveBoolean("allow_volatility_push", it)
                }
            )
            PushToggleRow(
                label = "AI Signals",
                sub = "Allow AI line, progression scale, and phase-state notifications",
                checked = allowAiPush,
                onCheckedChange = {
                    allowAiPush = it
                    saveBoolean("allow_ai_push", it)
                }
            )
            PushToggleRow(
                label = "News Alerts",
                sub = "Allow macro and asset-specific push notifications",
                checked = allowNewsPush,
                onCheckedChange = {
                    allowNewsPush = it
                    saveBoolean("allow_news_push", it)
                }
            )
            PushToggleRow(
                label = "Execution Alerts",
                sub = "Allow fills, risk, and execution-state notifications",
                checked = allowExecutionPush,
                onCheckedChange = {
                    allowExecutionPush = it
                    saveBoolean("allow_execution_push", it)
                }
            )
            PushToggleRow(
                label = "Critical Alerts",
                sub = "Security or liquidation-risk messages that may bypass quieter modes",
                checked = allowCriticalPush,
                onCheckedChange = {
                    allowCriticalPush = it
                    saveBoolean("allow_critical_push", it)
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        PushSettingsSection(title = "DELIVERY CONTROLS", icon = Icons.Default.Smartphone) {
            PushToggleRow(
                label = "Alert Sound",
                sub = "Play an audible tone for allowed push categories",
                checked = alertSound,
                onCheckedChange = {
                    alertSound = it
                    saveBoolean("push_sound_enabled", it)
                }
            )
            PushToggleRow(
                label = "Vibration",
                sub = "Use device vibration when alerts are delivered",
                checked = vibration,
                onCheckedChange = {
                    vibration = it
                    saveBoolean("push_vibration_enabled", it)
                }
            )
            PushToggleRow(
                label = "Show on Lockscreen",
                sub = "Allow notifications to appear before device unlock",
                checked = showOnLockscreen,
                onCheckedChange = {
                    showOnLockscreen = it
                    saveBoolean("push_lockscreen_enabled", it)
                }
            )
            PushToggleRow(
                label = "Grouped Notifications",
                sub = "Collapse repeated alerts into grouped device notifications",
                checked = groupedNotifications,
                onCheckedChange = {
                    groupedNotifications = it
                    saveBoolean("push_grouped_notifications", it)
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        PushSettingsSection(title = "FREQUENCY CONTROLS", icon = Icons.Default.NotificationsActive) {
            PushSliderRow(
                label = "Cooldown",
                valueLabel = "${cooldownMinutes} min",
                value = cooldownMinutes.toFloat(),
                range = 1f..60f,
                onValueChange = {
                    cooldownMinutes = it.toInt()
                    saveInt("push_cooldown_minutes", cooldownMinutes)
                }
            )
            PushSliderRow(
                label = "Max Alerts Per Hour",
                valueLabel = maxAlertsPerHour.toString(),
                value = maxAlertsPerHour.toFloat(),
                range = 1f..50f,
                onValueChange = {
                    maxAlertsPerHour = it.toInt()
                    saveInt("push_max_alerts_per_hour", maxAlertsPerHour)
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        PushSettingsSection(title = "DEVICE TOKEN STATUS", icon = Icons.Default.Shield) {
            PushStatusRow("Push Service", if (firebaseConfigured) "Configured" else "Firebase Config Missing", if (firebaseConfigured) EmeraldSuccess else RoseError)
            PushStatusRow("FCM Token", if (firebaseConfigured) "Pending Runtime Registration" else "Unavailable", if (firebaseConfigured) IndigoAccent else SlateText)
            PushStatusRow("Permission Gate", if (enablePush) "Enabled by app" else "Muted by app", if (enablePush) EmeraldSuccess else Color(0xFFF59E0B))
            PushStatusRow("Backend Sync", "Preference flags stored locally in asc_prefs", SlateText)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PushSettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            }
            content()
        }
    }
}

@Composable
fun PushToggleRow(label: String, sub: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(sub, color = SlateText, fontSize = 10.sp, lineHeight = 14.sp, fontFamily = InterFontFamily)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.White.copy(alpha = 0.2f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun PushSliderRow(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(valueLabel, color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
fun PushStatusRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}
