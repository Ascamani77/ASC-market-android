@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")
package com.asc.markets.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import com.asc.markets.data.FOREX_PAIRS

sealed class SettingsSection(val id: String, val title: String, val icon: ImageVector, val value: String? = null) {
    object Workspace : SettingsSection("workspace", "Workspace Interface", androidx.compose.material.icons.autoMirrored.outlined.Settings, "DARK")
    object Analytical : SettingsSection("analytical", "Analytical Canvas & Focus", androidx.compose.material.icons.autoMirrored.outlined.Timeline, "H1")
    object Intelligence : SettingsSection("intelligence", "Intelligence Filtering Logic", androidx.compose.material.icons.autoMirrored.outlined.FilterList)
    object Security : SettingsSection("security", "Security Protocol", androidx.compose.material.icons.autoMirrored.outlined.Lock, "ENABLED")
    object Risk : SettingsSection("risk", "Risk Governance & Surveillance", androidx.compose.material.icons.autoMirrored.outlined.AccountBalance, "BALANCED")
    object Dispatch : SettingsSection("dispatch", "Intelligence Dispatch", androidx.compose.material.icons.autoMirrored.outlined.Notifications)
    object Asset : SettingsSection("asset", "Asset Universe Filtering", androidx.compose.material.icons.autoMirrored.outlined.List)
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
                        SettingsSection.Asset, SettingsSection.Calibration,
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(androidx.compose.material.icons.autoMirrored.outlined.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE CONFIG", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
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
                FOREX_PAIRS.forEach { pair ->
                    AssetSettingRow(pair)
                }
            }
            SettingsSection.Engine -> {
                SliderRow("Node Lookback Depth", 500f, 100f, 5000f, " Bars")
                ToggleRow("HTF Context Aggregator", "Deep structural scanning", true)
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
            Text("${String.format(java.util.Locale.US, "%.1f", value)}$suffix", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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