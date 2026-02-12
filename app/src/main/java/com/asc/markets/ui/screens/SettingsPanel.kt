package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.asc.markets.data.UserSettings
import com.asc.markets.ui.theme.PureBlack
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Info
import com.asc.markets.ui.theme.HairlineBorder
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.SlateText
import com.asc.markets.ui.theme.InterFontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    userSettings: UserSettings,
    section: String,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopAppBar(title = { Text(section) }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }, colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = PureBlack))

            Spacer(modifier = Modifier.height(12.dp))

            when (section) {
                "Workspace Interface" -> WorkspaceInterfacePanel(userSettings)
                "Analytical Canvas & Focus", "AI Analytical Tuning" -> AnalyticalFocusPanel(userSettings)
                "Intelligence Filtering Logic", "Strategy Calibration" -> IntelligenceCalibrationPanel(userSettings)
                "Intelligence Dispatch" -> IntelligenceDispatchPanel(userSettings)
                "Security Protocol" -> SecurityProtocolPanel(userSettings)
                "Risk Governance & Execution" -> RiskPanel(userSettings)
                "Asset Universe Filtering" -> AssetUniversePanel(userSettings)
                "Post Move Audit", "Post-Move Audit" -> PostMoveAuditSettingsPanel(userSettings)
                else -> DefaultSettingsPanel(section)
            }
        }
    }
}

@Composable
private fun WorkspaceInterfacePanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        // Large page title like the screenshot
        Text("Workspace Interface", fontSize = 32.sp, color = Color.White)
        Spacer(Modifier.height(18.dp))

        Text("APPEARANCE LOGIC", color = IndigoAccent, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))

        // Segmented theme selector
        val theme = remember { mutableStateOf(settings.themeMode) }
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF0A0A0A), RoundedCornerShape(24.dp)), verticalAlignment = Alignment.CenterVertically) {
            val options = listOf("LIGHT", "DARK", "SYSTEM")
            options.forEach { opt ->
                val selected = theme.value == opt
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(6.dp)
                    .background(if (selected) Color.White else Color(0x00121212), RoundedCornerShape(20.dp))
                    .fillMaxHeight()
                    .then(Modifier), contentAlignment = Alignment.Center) {
                    Text(opt, color = if (selected) Color.Black else Color.White)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Toggle rows
        var materialYou by remember { mutableStateOf(true) }
        var zenMode by remember { mutableStateOf(false) }
        var priceTicker by remember { mutableStateOf(false) }
        var criticalImpact by remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Material You Theme", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = materialYou, onCheckedChange = { materialYou = it })
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Zen Mode", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = zenMode, onCheckedChange = { zenMode = it })
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Price Ticker", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = priceTicker, onCheckedChange = { priceTicker = it })
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Critical Impact Ticker", color = RoseError, modifier = Modifier.weight(1f))
            Switch(checked = criticalImpact, onCheckedChange = { criticalImpact = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RoseError))
        }

        Spacer(Modifier.height(28.dp))

        // Save button
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* persist to settings */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B)), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save", color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Info box
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                Spacer(Modifier.width(12.dp))
                Text("PARAMETERS INFLUENCE THE AI NODE'S LOCAL WEIGHTING. CHANGES ARE COMMITTED TO SECURE HARDWARE STORAGE.", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AnalyticalFocusPanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("AI Analytical Tuning", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
        Spacer(Modifier.height(18.dp))

        // NODE LOOKBACK DEPTH slider with right-aligned value
        var lookback by remember { mutableStateOf(500f) }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("NODE LOOKBACK DEPTH", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${lookback.toInt()}", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = lookback, onValueChange = { lookback = it }, valueRange = 1f..500f, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(18.dp))

        // HTF Context Aggregator toggle on the right
        var htf by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("HTF Context Aggregator", color = Color.White, modifier = Modifier.weight(1f), fontSize = 18.sp)
            Switch(checked = htf, onCheckedChange = { htf = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Centered Save pill (dark navy)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* persist */ }, modifier = Modifier.width(200.dp).height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2036))) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Large rounded info panel
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0B0B), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                Spacer(Modifier.width(12.dp))
                Text(
                    "PARAMETERS INFLUENCE THE AI NODE'S LOCAL WEIGHTING. CHANGES ARE COMMITTED TO SECURE HARDWARE STORAGE.",
                    color = SlateText,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun IntelligenceCalibrationPanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Strategy Calibration", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
        Spacer(Modifier.height(18.dp))

        // Manual override toggle aligned to the right like the screenshot
        var manualOverride by remember { mutableStateOf(true) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Manual Override Strategy", color = Color.White, modifier = Modifier.weight(1f), fontSize = 18.sp)
            Switch(checked = manualOverride, onCheckedChange = { manualOverride = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
        }

        Spacer(Modifier.height(12.dp))

        // MA FAST slider row
        var maFast by remember { mutableStateOf(50f) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("MA FAST", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${maFast.toInt()}", color = Color.White)
        }
        Slider(value = maFast, onValueChange = { maFast = it }, valueRange = 1f..200f, modifier = Modifier.fillMaxWidth(), steps = 0)

        Spacer(Modifier.height(18.dp))

        // MA SLOW slider row
        var maSlow by remember { mutableStateOf(200f) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("MA SLOW", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${maSlow.toInt()}", color = Color.White)
        }
        Slider(value = maSlow, onValueChange = { maSlow = it }, valueRange = 1f..400f, modifier = Modifier.fillMaxWidth(), steps = 0)

        Spacer(modifier = Modifier.height(28.dp))

        // Centered Save pill (dark navy)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* persist */ }, modifier = Modifier.width(200.dp).height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2036))) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Large rounded info panel matching screenshot
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0B0B), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                Spacer(Modifier.width(12.dp))
                Text(
                    "PARAMETERS INFLUENCE THE AI NODE'S LOCAL WEIGHTING. CHANGES ARE COMMITTED TO SECURE HARDWARE STORAGE.",
                    color = SlateText,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun RiskPanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Risk Governance & Execution", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
        Spacer(Modifier.height(18.dp))

        // Max session risk
        // default to 1% if UserSettings doesn't contain a value
        var maxRisk by remember { mutableStateOf(1f) }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MAX SESSION RISK", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${maxRisk.toInt()}%", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = maxRisk, onValueChange = { maxRisk = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(18.dp))

        // Minimum risk reward
        // default to 2 RR if UserSettings doesn't contain a value
        var rr by remember { mutableStateOf(2f) }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MINIMUM RISK REWARD", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${rr.toInt()} RR", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = rr, onValueChange = { rr = it }, valueRange = 1f..5f, steps = 3, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        // Save button centered
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* persist */ }, modifier = Modifier.width(220.dp).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E4854))) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Info box
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                Spacer(Modifier.width(12.dp))
                Text(
                    "PARAMETERS INFLUENCE THE AI NODE'S LOCAL WEIGHTING. CHANGES ARE COMMITTED TO SECURE HARDWARE STORAGE.",
                    color = SlateText,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun SecurityProtocolPanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Access Governance", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)

        // ACCESS GOVERNANCE box
        Text("ACCESS GOVERNANCE", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var biometricGuard by remember { mutableStateOf(true) }
                var pinBackup by remember { mutableStateOf(true) }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Guard", color = Color.White, fontSize = 16.sp)
                        Text("Require fingerprint or face ID on app launch", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = biometricGuard, onCheckedChange = { biometricGuard = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }

                Divider(color = Color.White.copy(alpha = 0.03f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pin Code Backup", color = Color.White, fontSize = 16.sp)
                        Text("Fallback authentication for secondary nodes", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = pinBackup, onCheckedChange = { pinBackup = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }

                Divider(color = Color.White.copy(alpha = 0.03f))

                // Auto-lock slider
                var autoLock by remember { mutableStateOf(15f) }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("AUTO-LOCK NODE INTERVAL", color = IndigoAccent, modifier = Modifier.weight(1f))
                    Text("${autoLock.toInt()} MINS", color = Color.White)
                }
                Slider(value = autoLock, onValueChange = { autoLock = it }, valueRange = 1f..60f, modifier = Modifier.fillMaxWidth())
            }
        }

        // CRYPTOGRAPHY LAYER
        Text("CRYPTOGRAPHY LAYER", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var hardwareBacked by remember { mutableStateOf(true) }
                var sandbox by remember { mutableStateOf(false) }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hardware-Backed Storage", color = Color.White, fontSize = 16.sp)
                        Text("AES-GCM encryption for settings at rest", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = hardwareBacked, onCheckedChange = { hardwareBacked = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }

                Divider(color = Color.White.copy(alpha = 0.03f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Operational Sandbox", color = Color.White, fontSize = 16.sp)
                        Text("Isolate analytical processes from background tasks", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = sandbox, onCheckedChange = { sandbox = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }
            }
        }

        // INTEGRITY AUDITING
        Text("INTEGRITY AUDITING", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var contScan by remember { mutableStateOf(true) }
                var attestation by remember { mutableStateOf(true) }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Binary Scan", color = Color.White, fontSize = 16.sp)
                        Text("Detect logic corruption in real-time", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = contScan, onCheckedChange = { contScan = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }

                Divider(color = Color.White.copy(alpha = 0.03f))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Device Attestation", color = Color.White, fontSize = 16.sp)
                        Text("Verify hardware-integrity status", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = attestation, onCheckedChange = { attestation = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Emergency wipe button
                Button(onClick = { /* destructive action */ }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = RoseError), shape = RoundedCornerShape(12.dp)) {
                    Text("FORCE EMERGENCY DATA WIPE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Save button and info box
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* persist */ }, modifier = Modifier.width(220.dp).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E4854))) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HairlineBorder)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                Spacer(Modifier.width(12.dp))
                Text(
                    "PARAMETERS INFLUENCE THE AI NODE'S LOCAL WEIGHTING. CHANGES ARE COMMITTED TO SECURE HARDWARE STORAGE.",
                    color = SlateText,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun IntelligenceDispatchPanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Intelligence Dispatch", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
        Spacer(Modifier.height(18.dp))

        // Confidence filter label + value
        var confidence by remember { mutableStateOf(70f) }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("CONFIDENCE FILTER", color = IndigoAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("${confidence.toInt()}%", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = confidence, onValueChange = { confidence = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(18.dp))

        // Aural feedback toggle
        var aural by remember { mutableStateOf(true) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Aural Feedback", color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = InterFontFamily)
            Switch(checked = aural, onCheckedChange = { aural = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Centered Save pill
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* persist */ }, modifier = Modifier.width(220.dp).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2036))) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AssetUniversePanel(settings: UserSettings) {
    // Static visual-only implementation to match design mockups. No actions wired.
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header row: Market Universe + Add Filter
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF22303A)) {
                Text("  MARKET UNIVERSE  ", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF0A0A0A)) {
                Text("+ ADD FILTER", color = SlateText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }

        // Search bar mock
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0B0B), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = SlateText)
                Spacer(Modifier.width(12.dp))
                Text("Quick Search Symbol...", color = SlateText)
            }
        }

        // Helper to render an asset row
        @Composable
        fun AssetRow(symbol: String, name: String, price: String, change: String, changePositive: Boolean) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // avatar placeholder
                Box(modifier = Modifier.size(36.dp).background(Color(0xFF0E0E0E), shape = RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Text(symbol.take(2), color = Color.White, fontSize = 12.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(symbol, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(name, color = SlateText, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(price, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(change, color = if (changePositive) EmeraldSuccess else RoseError, fontSize = 12.sp)
                }
                Spacer(Modifier.width(12.dp))
                // star + eye icons as static visuals
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF172028)) { Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp)) }
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF172028)) { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp)) }
            }
            Divider(color = Color.White.copy(alpha = 0.03f))
        }

        // FOREX section
        Text("FOREX", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("EUR/USD", "EURO / US DOLLAR", "1.0845", "+0.11%", true)
                AssetRow("GBP/USD", "BRITISH POUND / US DOLLAR", "1.2634", "-0.17%", false)
                AssetRow("USD/JPY", "US DOLLAR / JAPANESE YEN", "151.4200", "+0.23%", true)
            }
        }

        // STOCKS
        Text("STOCKS", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("NVDA", "NVIDIA CORP", "890.15", "+2.83%", true)
                AssetRow("TSLA", "TESLA INC", "172.40", "-2.38%", false)
                AssetRow("AAPL", "APPLE INC", "185.12", "+0.62%", true)
                AssetRow("MSFT", "MICROSOFT CORP", "425.40", "+0.73%", true)
                AssetRow("AMZN", "AMAZON.COM INC", "180.15", "+1.38%", true)
            }
        }

        // COMMODITIES
        Text("COMMODITIES", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("XAU/USD", "GOLD / US DOLLAR", "2,342.50", "+0.53%", true)
                AssetRow("USOIL", "WTI CRUDE OIL", "82.14", "-1.44%", false)
            }
        }

        // CRYPTO
        Text("CRYPTO", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("BTC/USDT", "BITCOIN / TETHER", "67,432.50", "+1.87%", true)
                AssetRow("ETH/USDT", "ETHEREUM / TETHER", "3,452.15", "-1.29%", false)
            }
        }

        // INDICES
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("INDICES", color = IndigoAccent, fontSize = 12.sp)
            Text("3 Assets", color = SlateText, fontSize = 12.sp)
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("NAS100", "NASDAQ 100", "18,240.50", "+0.79%", true)
                AssetRow("US30", "DOW JONES 30", "39,120.00", "+0.22%", true)
                AssetRow("SPX500", "S&P 500", "5,210.45", "+0.23%", true)
            }
        }

        // BONDS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("BONDS", color = IndigoAccent, fontSize = 12.sp)
            Text("2 Assets", color = SlateText, fontSize = 12.sp)
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("US10Y", "US 10Y TREASURY YIELD", "4.256", "+0.28%", true)
                AssetRow("US02Y", "US 2Y TREASURY YIELD", "4.624", "-0.11%", false)
            }
        }

        // FUTURES
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("FUTURES", color = IndigoAccent, fontSize = 12.sp)
            Text("2 Assets", color = SlateText, fontSize = 12.sp)
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                AssetRow("ES1!", "S&P 500 FUTURES", "5,245.25", "+0.30%", true)
                AssetRow("NQ1!", "NASDAQ 100 FUTURES", "18,450.75", "+0.65%", true)
            }
        }

        // Active Matrix State boxes
        Text("ACTIVE MATRIX STATE", color = IndigoAccent, fontSize = 12.sp)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.01f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WHITELISTED ASSETS", color = SlateText, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("25 INSTRUMENTS MONITORING", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(Icons.Default.Star, contentDescription = null, tint = SlateText)
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1C0A0E), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SUPPRESSED ASSETS", color = Color(0xFF8A0011), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("0 RESTRICTED NODES", color = Color(0xFFFF2D55), fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color(0xFF8A0011))
                }
            }
        }

        // Save button and disclaimer at the bottom
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { /* disabled for design-only */ }, modifier = Modifier.width(220.dp).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D2036))) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoAccent)
                Spacer(Modifier.width(12.dp))
                Text("PARAMETERS INFLUENCE THE AI NODE'S LOCAL WEIGHTING. CHANGES ARE COMMITTED TO SECURE HARDWARE STORAGE.", color = SlateText)
            }
        }
    }
}

@Composable
private fun DefaultSettingsPanel(section: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(section, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("No detail view implemented yet")
    }
}

@Composable
private fun PostMoveAuditSettingsPanel(settings: UserSettings) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Governance parameters box
        Text("GOVERNANCE PARAMETERS", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                var maxRisk by remember { mutableStateOf(1f) }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("MAX SESSION RISK", color = Color.White, modifier = Modifier.weight(1f))
                    Text("${maxRisk.toInt()}%", color = Color.White)
                }
                Slider(value = maxRisk, onValueChange = { maxRisk = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())

                var rr by remember { mutableStateOf(2f) }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("MINIMUM RISK REWARD", color = Color.White, modifier = Modifier.weight(1f))
                    Text("${rr.toInt()} RR", color = Color.White)
                }
                Slider(value = rr, onValueChange = { rr = it }, valueRange = 1f..5f, steps = 3, modifier = Modifier.fillMaxWidth())
            }
        }

        // Execution audit maintenance
        Text("EXECUTION AUDIT MAINTENANCE", color = IndigoAccent, fontSize = 12.sp)
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                var autoPurge by remember { mutableStateOf(true) }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Purge Ledger", color = Color.White, fontSize = 18.sp)
                        Text("Automatically wipe audit logs on session end", color = SlateText, fontSize = 12.sp)
                    }
                    Switch(checked = autoPurge, onCheckedChange = { autoPurge = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent))
                }

                Divider(color = Color.White.copy(alpha = 0.03f))

                Button(onClick = { /* export */ }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("BATCH EXPORT AUDIT HISTORY", color = Color.White)
                }

                Button(onClick = { /* purge */ }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = RoseError), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("PURGE ACTIVE EXECUTION LEDGER", color = Color.White)
                }
            }
        }

        // Warning/disclaimer box
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0B0B), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF7A5800))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF2C94C))
                Spacer(Modifier.width(12.dp))
                Text("MANUAL LEDGER INTERVENTION IS RECORDED IN THE HIGHER-LEVEL SECURITY BUFFER. PURGING CLEARS VISIBILITY BUT DOES NOT DELETE PRIMARY TRANSACTION IDS FROM THE SERVER NODE.", color = SlateText, fontSize = 12.sp)
            }
        }
    }
}
