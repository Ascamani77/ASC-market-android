package com.trading.app.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import com.trading.app.models.Drawing
import com.trading.app.models.UserAlert
import java.text.SimpleDateFormat
import java.util.*

// ── Colors — sheet matches AnalysisHub (0xFF121212), only inputs pure black ──
private val BgSheet = Color(0xFF121212)
private val BgField = Color.Black
private val BorderField = Color(0xFF2A2E39)
private val TextMuted = Color(0xFF787B86)
private val BlueAccent = Color.White

// ══════════════════════════════════════════════════════════
//  ROOT — single ModalBottomSheet, internal navigation
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertModal(
    symbol: String,
    currentPrice: Float? = null,
    alerts: List<UserAlert> = emptyList(),
    onAlertCreate: (UserAlert) -> Unit,
    onAlertUpdate: (UserAlert) -> Unit = {},
    onAlertDelete: (String) -> Unit = {},
    onDrawingCreate: (Drawing) -> Unit = {},
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var screen by remember { mutableStateOf<AlertScreen>(AlertScreen.List) }
    var selectedAlert by remember { mutableStateOf<UserAlert?>(null) }

    var conditionKind by remember { mutableStateOf("Price") }
    var crossingKind by remember { mutableStateOf("Crossing") }
    var priceStr by remember { mutableStateOf(currentPrice?.let { formatPriceDigits(it, symbol) } ?: currentPriceDefault(symbol)) }
    var triggerMode by remember { mutableStateOf("Once only") }
    var expirationStr by remember { mutableStateOf(defaultExpiration()) }
    var alertName by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("$symbol Crossing $priceStr") }
    var notifState by remember { mutableStateOf(NotifState(push = true, toast = true)) }
    var msgEditedManually by remember { mutableStateOf(false) }
    var smcZonesState by remember { mutableStateOf<Set<String>>(emptySet()) }
    var smcMin by remember { mutableStateOf(1) }
    LaunchedEffect(priceStr, crossingKind, conditionKind, smcZonesState, smcMin) {
        if (msgEditedManually) return@LaunchedEffect
        alertMessage = if (conditionKind == "SMC") {
            val ordered = com.trading.app.indicators.SmcZoneAlerts.zoneKeys.filter { it in smcZonesState }
            if (ordered.isEmpty()) "$symbol SMC alert" else {
                val verb = when {
                    smcMin >= ordered.size -> "all of"
                    smcMin <= 1 -> "any of"
                    else -> "$smcMin+ of"
                }
                "$symbol touches $verb [${ordered.joinToString(", ")}]"
            }
        } else "$symbol $crossingKind $priceStr"
    }

    val visibleAlerts = remember(alerts, symbol) { alerts.filter { it.symbol == symbol } }

    // Collect triggered (inactive with triggeredAt) for Log
    val logAlerts = remember(alerts) { alerts.filter { it.triggeredAt != null }.sortedByDescending { it.triggeredAt } }

    var pickerKind by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var listTab by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = BgSheet,
        dragHandle = { SheetDragHandle() },
        contentWindowInsets = { WindowInsets(0) },
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                slideInHorizontally(animationSpec = tween(180), initialOffsetX = { it / 3 }) + fadeIn(tween(180)) togetherWith
                        slideOutHorizontally(animationSpec = tween(180), targetOffsetX = { -it / 3 }) + fadeOut(tween(180))
            },
            label = "alertScreen"
        ) { target ->
            when (target) {
                AlertScreen.List -> AlertsListContent(
                    symbol = symbol, alerts = visibleAlerts, logAlerts = logAlerts,
                    searchQuery = searchQuery, onSearchChange = { searchQuery = it },
                    listTab = listTab, onListTabChange = { listTab = it },
                    onCreateClick = { screen = AlertScreen.Create },
                    onAlertClick = { a -> selectedAlert = a; screen = AlertScreen.Details }
                )
                AlertScreen.Create -> CreateAlertContent(
                    symbol = symbol,
                    conditionKind = conditionKind, onConditionClick = { pickerKind = "condition" },
                    crossingKind = crossingKind, onCrossingClick = { pickerKind = "crossing" },
                    priceStr = priceStr, onPriceStrChange = { priceStr = it },
                    smcZones = smcZonesState,
                    onSmcZoneToggle = { z ->
                        val next = if (z in smcZonesState) smcZonesState - z else smcZonesState + z
                        smcZonesState = next
                        if (smcMin > next.size) smcMin = next.size.coerceAtLeast(1)
                    },
                    smcMin = smcMin, onSmcMinChange = { smcMin = it },
                    triggerMode = triggerMode, onTriggerClick = { pickerKind = "trigger" },
                    expirationStr = expirationStr,
                    alertMessage = alertMessage, notifSummary = notifState.summary(),
                    onEditMessageClick = { screen = AlertScreen.EditMessage },
                    onNotificationsClick = { screen = AlertScreen.Notifications },
                    onClose = onClose,
                    onCreate = {
                        if (conditionKind == "SMC") {
                            val zones = com.trading.app.indicators.SmcZoneAlerts.zoneKeys.filter { it in smcZonesState }
                            if (zones.isNotEmpty()) {
                                onAlertCreate(UserAlert(
                                    id = System.currentTimeMillis().toString(), symbol = symbol, condition = "SMC",
                                    price = 0f, message = alertMessage, isActive = true, createdAt = System.currentTimeMillis(),
                                    triggerMode = triggerMode,
                                    smcZones = zones, smcMin = if (smcMin in 1..zones.size) smcMin else zones.size
                                ))
                            }
                        } else {
                            val p = priceStr.toFloatOrNull() ?: 0f
                            onAlertCreate(UserAlert(
                                id = System.currentTimeMillis().toString(), symbol = symbol, condition = crossingKind,
                                price = p, message = alertMessage, isActive = true, createdAt = System.currentTimeMillis(),
                                triggerMode = triggerMode
                            ))
                        }
                        priceStr = currentPriceDefault(symbol)
                        triggerMode = "Once only"
                        smcZonesState = emptySet()
                        smcMin = 1
                        screen = AlertScreen.List
                    }
                )
                AlertScreen.EditMessage -> EditMessageContent(
                    alertName = alertName, alertMessage = alertMessage,
                    onApply = { n, m -> alertName = n; alertMessage = m; msgEditedManually = true; screen = AlertScreen.Create },
                    onBack = { screen = AlertScreen.Create }
                )
                AlertScreen.Notifications -> NotificationsContent(
                    state = notifState, onStateChange = { notifState = it },
                    onApply = { screen = AlertScreen.Create }, onBack = { screen = AlertScreen.Create }
                )
                AlertScreen.Details -> {
                    val a = selectedAlert
                    if (a != null) {
                        val current = alerts.find { it.id == a.id } ?: a
                        DetailsContent(
                            alert = current,
                            onBack = { screen = AlertScreen.List },
                            onStop = {
                                val toggled = current.copy(isActive = false)
                                onAlertUpdate(toggled); screen = AlertScreen.List
                            },
                            onResume = {
                                val toggled = current.copy(isActive = true, triggeredAt = null)
                                onAlertUpdate(toggled); screen = AlertScreen.List
                            },
                            onDelete = { onAlertDelete(current.id); screen = AlertScreen.List },
                            onClone = {
                                val clone = current.copy(id = System.currentTimeMillis().toString(), createdAt = System.currentTimeMillis(), isActive = true, triggeredAt = null, lastTriggeredAt = null)
                                onAlertCreate(clone); screen = AlertScreen.List
                            },
                            onEdit = {
                                // Pre-fill create form with this alert's values for editing
                                if (current.condition == "SMC") {
                                    conditionKind = "SMC"
                                    crossingKind = "SMC"
                                    smcZonesState = current.smcZones.toSet()
                                    smcMin = current.smcMin
                                } else {
                                    conditionKind = "Price"
                                    crossingKind = current.condition
                                }
                                priceStr = current.price.toString()
                                triggerMode = current.triggerMode
                                alertMessage = current.message
                                msgEditedManually = true
                                // Delete old, user will re-create
                                onAlertDelete(current.id)
                                screen = AlertScreen.Create
                            }
                        )
                    }
                }
            }
        }

        val pk = pickerKind
        if (pk != null && screen == AlertScreen.Create) {
            Spacer(modifier = Modifier.height(8.dp))
            when (pk) {
                "condition" -> InlinePicker(
                    options = listOf("Price", "SMC", "Indicator", "Drawing"),
                    selected = conditionKind,
                    onSelect = { conditionKind = it; pickerKind = null }
                )
                "crossing" -> InlinePicker(
                    options = listOf("Crossing", "Crossing Up", "Crossing Down"),
                    selected = crossingKind, showMore = true,
                    onSelect = { crossingKind = it; pickerKind = null }
                )
                "trigger" -> InlineTriggerPicker(
                    selected = triggerMode,
                    onSelect = { triggerMode = it; pickerKind = null }
                )
            }
        }
    }
}

private enum class AlertScreen { List, Create, EditMessage, Notifications, Details }

private data class NotifState(
    val push: Boolean = true, val toast: Boolean = true,
    val email: Boolean = false, val webhook: Boolean = false,
    val sound: Boolean = false, val plainText: Boolean = false,
    val schedule: String = "24/7"
) {
    fun summary(): String {
        val parts = mutableListOf<String>()
        if (push) parts += "Push"
        if (toast) parts += "Pop-up"
        if (email) parts += "Email"
        if (webhook) parts += "Webhook"
        return if (parts.isEmpty()) "None" else parts.joinToString(", ")
    }
}

// ══════════════════════════════════════════════════════════
//  ALERTS LIST
// ══════════════════════════════════════════════════════════
@Composable
private fun AlertsListContent(
    symbol: String,
    alerts: List<UserAlert>,
    logAlerts: List<UserAlert>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    listTab: Int,
    onListTabChange: (Int) -> Unit,
    onCreateClick: () -> Unit,
    onAlertClick: (UserAlert) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(bottom = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Alerts", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Edit, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.Tune, null, tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = searchQuery, onValueChange = onSearchChange,
            placeholder = { Text(if (listTab == 0) "Search for alerts" else "Search for logs", color = TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black,
                focusedBorderColor = BorderField, unfocusedBorderColor = BorderField,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
            ),
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A)).padding(4.dp)) {
            listOf("List", "Log").forEachIndexed { idx, title ->
                val selected = idx == listTab
                Box(
                    modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF2A2E39) else Color.Transparent).clickable { onListTabChange(idx) },
                    contentAlignment = Alignment.Center
                ) { Text(title, color = if (selected) Color.White else TextMuted, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (listTab == 0) {
            val filtered = if (searchQuery.isBlank()) alerts else alerts.filter { it.message.contains(searchQuery, true) || it.condition.contains(searchQuery, true) }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { Text("No alerts", color = TextMuted, fontSize = 13.sp) }
            } else {
                filtered.forEach { alert ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black)
                            .clickable { onAlertClick(alert) }.padding(12.dp)
                    ) {
                        Text(alert.message.ifEmpty { "${alert.symbol} ${alert.condition} ${alert.price}" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("◉", color = Color(0xFF2962FF), fontSize = 10.sp)
                            Text(alert.symbol, color = Color.White, fontSize = 11.sp)
                            Text("•", color = TextMuted, fontSize = 11.sp)
                            if (!alert.isActive && alert.triggeredAt != null) {
                                Text("Stopped — Triggered", color = Color(0xFFF23645), fontSize = 11.sp)
                                Text("•", color = TextMuted, fontSize = 11.sp)
                                Text(formatLogTime(alert.triggeredAt), color = TextMuted, fontSize = 11.sp)
                            } else {
                                Text(if (alert.isActive) "Active" else "Stopped", color = if (alert.isActive) Color(0xFF26A69A) else Color(0xFFF23645), fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            // Log tab — triggered alerts grouped by TODAY
            val filteredLog = if (searchQuery.isBlank()) logAlerts else logAlerts.filter { it.message.contains(searchQuery, true) || it.symbol.contains(searchQuery, true) }
            if (filteredLog.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { Text("No logs yet", color = TextMuted, fontSize = 13.sp) }
            } else {
                Text("TODAY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                filteredLog.forEach { alert ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text("${alert.symbol} ${alert.condition} ${alert.price}", color = Color.White, fontSize = 13.sp, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("◉", color = Color(0xFFFFC107), fontSize = 10.sp)
                            Text(alert.symbol, color = TextMuted, fontSize = 11.sp)
                            Text("•", color = TextMuted, fontSize = 11.sp)
                            Text(formatLogTime(alert.triggeredAt ?: alert.createdAt), color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) { Text("Create alert", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

// ══════════════════════════════════════════════════════════
//  DETAILS
// ══════════════════════════════════════════════════════════
@Composable
private fun DetailsContent(
    alert: UserAlert,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    onEdit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Delete, null, tint = Color(0xFFF23645), modifier = Modifier.size(22.dp).clickable { onDelete() })
        }
        Text(alert.message.ifEmpty { "${alert.symbol} ${alert.condition} ${alert.price}" }, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { if (alert.isActive) onStop() else onResume() },
                modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (alert.isActive) Icons.Default.Stop else Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Text(if (alert.isActive) "Stop" else "Resume", fontSize = 11.sp)
                }
            }
            Button(onClick = onEdit, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Text("Edit", fontSize = 11.sp)
                }
            }
            Button(onClick = onClone, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)); Text("Clone", fontSize = 11.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow("Status", if (alert.isActive) "Active" else "Stopped", valueColor = if (alert.isActive) Color(0xFF26A69A) else Color(0xFFF23645))
        DetailRow("Symbol", "OANDA:${alert.symbol}")
        DetailRow("Type", if (alert.condition == "SMC") "SMC" else alert.condition)
        if (alert.condition == "SMC") {
            DetailRow("Zones", if (alert.smcZones.isEmpty()) "—" else alert.smcZones.joinToString(", "))
            DetailRow("Match", if (alert.smcZones.isNotEmpty() && alert.smcMin >= alert.smcZones.size) "All selected" else "${alert.smcMin.coerceAtLeast(1)}+ selected")
        } else {
            DetailRow("Price", alert.price.toString())
        }
        DetailRow("Trigger", alert.triggerMode)
        DetailRow("Created", formatDetailTime(alert.createdAt))
        alert.triggeredAt?.let { DetailRow("Triggered", formatDetailTime(it)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack, modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White)
        ) { Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Open ${alert.symbol} Chart", fontSize = 13.sp) }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back", color = TextMuted) }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ══════════════════════════════════════════════════════════
//  CREATE ALERT
// ══════════════════════════════════════════════════════════
@Composable
private fun CreateAlertContent(
    symbol: String,
    conditionKind: String, onConditionClick: () -> Unit,
    crossingKind: String, onCrossingClick: () -> Unit,
    priceStr: String, onPriceStrChange: (String) -> Unit,
    smcZones: Set<String>, onSmcZoneToggle: (String) -> Unit,
    smcMin: Int, onSmcMinChange: (Int) -> Unit,
    triggerMode: String, onTriggerClick: () -> Unit,
    expirationStr: String,
    alertMessage: String, notifSummary: String,
    onEditMessageClick: () -> Unit, onNotificationsClick: () -> Unit,
    onClose: () -> Unit, onCreate: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Create alert on", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFA500).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFA500), modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(24.dp).clickable { onClose() })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("◉", color = Color(0xFF2962FF), fontSize = 12.sp)
            Text(symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Condition", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TvDropdownField(value = conditionKind, onClick = onConditionClick)
        Spacer(modifier = Modifier.height(8.dp))
        if (conditionKind == "SMC") {
            Text("Zones (price must touch)", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            SmcZoneChips(zones = smcZones, onToggle = onSmcZoneToggle)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Match", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            SmcMatchSelector(zoneCount = smcZones.size, selected = smcMin, onSelect = onSmcMinChange)
        } else {
            TvCrossingField(value = crossingKind, onClick = onCrossingClick)
            Spacer(modifier = Modifier.height(8.dp))
            TvTextField(value = priceStr, onValueChange = onPriceStrChange, keyboardType = KeyboardType.Number)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clickable { }) {
            Text("+ Add condition", color = TextMuted, fontSize = 13.sp)
            Icon(Icons.Default.HelpOutline, null, tint = TextMuted, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Trigger", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().clickable { onTriggerClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(triggerMode, color = Color.White, fontSize = 14.sp)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Expiration", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(expirationStr, color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { })
        Spacer(modifier = Modifier.height(12.dp))
        Text("Message", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().clickable { onEditMessageClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(alertMessage, color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Text("›", color = TextMuted, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Notifications", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().clickable { onNotificationsClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(notifSummary, color = Color.White, fontSize = 14.sp)
            Text("›", color = TextMuted, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onClose, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderField), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.height(38.dp)) {
                Text("Cancel", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = onCreate, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), modifier = Modifier.height(38.dp), enabled = if (conditionKind == "SMC") smcZones.isNotEmpty() else priceStr.toFloatOrNull() != null) {
                Text("Create", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EditMessageContent(alertName: String, alertMessage: String, onApply: (String, String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(alertName) }
    var message by remember { mutableStateOf(alertMessage) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp).clickable { onBack() })
                Text("Edit message", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(22.dp).clickable { onBack() })
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Alert name", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TvPlainField(value = name, onValueChange = { name = it })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Message", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = message, onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = BgField, unfocusedContainerColor = BgField, focusedBorderColor = BlueAccent, unfocusedBorderColor = BorderField, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White),
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {}, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderField), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                Text("Add placeholder", fontSize = 12.sp)
            }
            Icon(Icons.Default.HelpOutline, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderField), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.height(38.dp)) { Text("Cancel", fontSize = 14.sp) }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = { onApply(name, message) }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), modifier = Modifier.height(38.dp)) { Text("Apply", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun NotificationsContent(state: NotifState, onStateChange: (NotifState) -> Unit, onApply: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp).clickable { onBack() })
                Text("Notifications", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(22.dp).clickable { onBack() })
        }
        Spacer(modifier = Modifier.height(16.dp))
        NotifCheckRow(checked = state.push, label = "Push notifications", desc = "Provides a push notification in the mobile app.", onChecked = { onStateChange(state.copy(push = it)) })
        NotifCheckRow(checked = state.toast, label = "Show toast notification on desktop", desc = "Displays an onsite notification in the page corner.", onChecked = { onStateChange(state.copy(toast = it)) })
        NotifCheckRow(checked = state.email, label = "Send email", desc = "Provides an email notification to the address specified in your account settings.", onChecked = { onStateChange(state.copy(email = it)) })
        NotifCheckRow(checked = state.webhook, label = "Webhook URL", desc = "Sends a POST request to your specified URL when your alert triggers.", help = true, onChecked = { onStateChange(state.copy(webhook = it)) })
        NotifCheckRow(checked = state.sound, label = "Play sound on desktop", desc = "Plays an audio cue when your alert triggers.", onChecked = { onStateChange(state.copy(sound = it)) })
        NotifCheckRow(checked = state.plainText, label = "Send plain text", desc = "Sends plain text to an alternative email.", help = true, onChecked = { onStateChange(state.copy(plainText = it)) })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Notification schedule", color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        TvDropdownField(value = state.schedule, onClick = {})
        Text("Notifications are muted outside this schedule.", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderField), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.height(38.dp)) { Text("Cancel", fontSize = 14.sp) }
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = onApply, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), modifier = Modifier.height(38.dp)) { Text("Apply", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ── Shared small composables ──
@Composable private fun SheetDragHandle() {
    Box(modifier = Modifier.padding(vertical = 12.dp).width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF363A45)))
}
@Composable private fun TvDropdownField(value: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(6.dp)).background(BgField).border(1.dp, BorderField, RoundedCornerShape(6.dp)).clickable { onClick() }.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = Color.White, fontSize = 14.sp); Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
@Composable private fun TvCrossingField(value: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(6.dp)).background(BgField).border(1.dp, BorderField, RoundedCornerShape(6.dp)).clickable { onClick() }.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⤢", color = Color.White, fontSize = 14.sp); Text(value, color = Color.White, fontSize = 14.sp)
            }
            Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
@Composable private fun TvTextField(value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Number) {
    OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = BgField, unfocusedContainerColor = BgField, focusedBorderColor = BlueAccent, unfocusedBorderColor = BorderField, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White), textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp))
}
@Composable private fun TvPlainField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = BgField, unfocusedContainerColor = BgField, focusedBorderColor = BlueAccent, unfocusedBorderColor = BorderField, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White), textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp))
}
@Composable private fun InlinePicker(options: List<String>, selected: String, showMore: Boolean = false, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, BorderField, RoundedCornerShape(10.dp)).padding(vertical = 4.dp, horizontal = 4.dp)) {
        options.forEach { opt ->
            val isSel = opt == selected
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (isSel) Color.White else Color.Transparent).clickable { onSelect(opt) }.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⤢", color = if (isSel) Color.Black else Color.White, fontSize = 13.sp)
                    Text(opt, color = if (isSel) Color.Black else Color.White, fontSize = 14.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                }
                if (isSel) Box(modifier = Modifier.size(8.dp).background(Color.Black, RoundedCornerShape(4.dp)))
            }
        }
        if (showMore) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clickable { }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SHOW MORE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
@Composable private fun SmcZoneChips(zones: Set<String>, onToggle: (String) -> Unit) {
    val options = com.trading.app.indicators.SmcZoneAlerts.zoneKeys.map { k -> k to (com.trading.app.indicators.SmcZoneAlerts.zoneLabels[k] ?: k) }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, BorderField, RoundedCornerShape(10.dp)).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (key, label) ->
            val selected = key in zones
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Color(0xFF2A2E39) else Color.Transparent)
                    .clickable { onToggle(key) }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(if (selected) Color.White else Color.Transparent).border(1.dp, if (selected) Color.White else TextMuted, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        if (selected) Text("✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(key, color = Color.White, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
                Text(label, color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable private fun SmcMatchSelector(zoneCount: Int, selected: Int, onSelect: (Int) -> Unit) {
    val options = buildList {
        if (zoneCount >= 1) add(1 to if (zoneCount == 1) "Any (1)" else "Any (1+)")
        if (zoneCount >= 2) add(2 to "2 or more")
        if (zoneCount >= 3) add(3 to "3 or more")
        if (zoneCount >= 2) add(zoneCount to "All ($zoneCount)")
    }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, BorderField, RoundedCornerShape(10.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Box(
                modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) Color.White else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) { Text(label, color = if (isSel) Color.Black else TextMuted, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, maxLines = 1) }
        }
    }
}

@Composable private fun InlineTriggerPicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("Once only" to "Triggers once when condition is met", "Every time" to "Triggers once per minute while condition remains met")
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black).border(1.dp, BorderField, RoundedCornerShape(10.dp)).padding(4.dp)) {
        options.forEach { (title, desc) ->
            val isSel = title == selected
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isSel) Color.White else Color.Transparent).clickable { onSelect(title) }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(if (isSel) Color.Black else Color.Transparent).border(1.dp, if (isSel) Color.Black else TextMuted, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    if (isSel) Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column { Text(title, color = if (isSel) Color.Black else Color.White, fontSize = 14.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal); Text(desc, color = if (isSel) Color.Black.copy(alpha = 0.6f) else TextMuted, fontSize = 11.sp) }
            }
        }
    }
}
@Composable private fun NotifCheckRow(checked: Boolean, label: String, desc: String, help: Boolean = false, onChecked: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Checkbox(checked = checked, onCheckedChange = onChecked, colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = BorderField, checkmarkColor = Color.Black), modifier = Modifier.size(20.dp))
            Text(label, color = Color.White, fontSize = 14.sp)
            if (help) Icon(Icons.Default.HelpOutline, null, tint = TextMuted, modifier = Modifier.size(14.dp))
        }
        Text(desc, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 30.dp, top = 2.dp))
    }
}

// ── Helpers ──
private fun currentPriceDefault(symbol: String): String {
    return when (symbol.uppercase()) {
        "XAUUSD", "XAGUSD" -> "0.00000"
        "BTCUSD", "BTCUSDT", "ETHUSD", "ETHUSDT" -> "0.0"
        else -> "0.00000"
    }
}

private fun formatPriceDigits(price: Float, symbol: String): String {
    val digits = when (symbol.uppercase()) {
        "XAUUSD", "XAGUSD" -> 2
        "BTCUSD", "BTCUSDT", "ETHUSD", "ETHUSDT" -> 2
        else -> 5
    }
    return String.format(java.util.Locale.US, "%.${digits}f", price)
}
private fun formatLogTime(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getDefault() }.format(Date(millis))
private fun formatDetailTime(millis: Long): String = SimpleDateFormat("dd MMM HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getDefault() }.format(Date(millis))
private fun defaultExpiration(): String {
    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
    return SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.ENGLISH).format(cal.time)
}
