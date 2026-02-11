package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.asc.markets.ui.theme.SlateText
import com.asc.markets.ui.theme.InterFontFamily
import androidx.compose.foundation.background
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
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text(section) }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }, colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = PureBlack))

            Spacer(modifier = Modifier.height(12.dp))

            when (section) {
                "Workspace Interface" -> WorkspaceInterfacePanel(userSettings)
                "Analytical Canvas & Focus" -> AnalyticalFocusPanel(userSettings)
                "Intelligence Filtering Logic" -> IntelligenceCalibrationPanel(userSettings)
                "Risk Governance & Execution" -> RiskPanel(userSettings)
                "Asset Universe Filtering" -> AssetUniversePanel(userSettings)
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
        Text("Analytical Canvas & Focus", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
        Spacer(Modifier.height(18.dp))

        Text("PRIMARY OPERATIONAL TIMEFRAME", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))

        // Segmented timeframe selector
        val timeframes = listOf("M15", "H1", "H4", "D1")
        var selectedTf by remember { mutableStateOf("H1") }
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF0A0A0A), RoundedCornerShape(24.dp)), verticalAlignment = Alignment.CenterVertically) {
            timeframes.forEach { tf ->
                val selected = selectedTf == tf
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(6.dp)
                    .background(if (selected) Color.White else Color(0x00121212), RoundedCornerShape(20.dp))
                    .fillMaxHeight()
                    .clickable { selectedTf = tf }, contentAlignment = Alignment.Center) {
                    Text(tf, color = if (selected) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Hub toggles in two rows
        var london by remember { mutableStateOf(false) }
        var newYork by remember { mutableStateOf(false) }
        var tokyo by remember { mutableStateOf(false) }
        var sydney by remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("London Hub", color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = InterFontFamily)
            Switch(checked = london, onCheckedChange = { london = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF2B2B2B)))
            Spacer(modifier = Modifier.width(24.dp))
            Text("New York Hub", color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = InterFontFamily)
            Switch(checked = newYork, onCheckedChange = { newYork = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF2B2B2B)))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tokyo Hub", color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = InterFontFamily)
            Switch(checked = tokyo, onCheckedChange = { tokyo = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF2B2B2B)))
            Spacer(modifier = Modifier.width(24.dp))
            Text("Sydney Hub", color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = InterFontFamily)
            Switch(checked = sydney, onCheckedChange = { sydney = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoAccent, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF2B2B2B)))
        }

        Spacer(Modifier.height(24.dp))

        // Save button centered (matches screenshot: white pill)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { /* persist */ }, modifier = Modifier.width(180.dp).height(48.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
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
private fun IntelligenceCalibrationPanel(settings: UserSettings) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Intelligence Calibration", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Threshold: ${settings.intelligenceThreshold}")
    }
}

@Composable
private fun RiskPanel(settings: UserSettings) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Risk Governance & Execution", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Level: ${settings.riskLevel}")
    }
}

@Composable
private fun AssetUniversePanel(settings: UserSettings) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Asset Universe Filtering", fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Whitelisted: ${settings.assetWhitelist.size} items")
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
