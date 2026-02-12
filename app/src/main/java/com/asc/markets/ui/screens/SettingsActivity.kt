package com.asc.markets.ui.screens

import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.asc.markets.R
import com.asc.markets.ui.theme.PureBlack
import kotlinx.coroutines.delay

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force status bar color to black at runtime to avoid OEM/theming inconsistencies
        try {
            window.statusBarColor = ContextCompat.getColor(this, com.asc.markets.R.color.black)
        } catch (_: Exception) { }
        // Read deep-link query parameter 'section' (e.g. asc://settings?section=post_move_audit)
        val section = intent?.data?.getQueryParameter("section")
        setContent {
            SettingsScreen(targetSection = section)
        }
    }
}

@Composable
fun SettingsScreen(targetSection: String? = null) {
    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // navigation state for opening detailed panels
            val currentScreen = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            val userSettings = remember { com.asc.markets.data.UserSettings() }

            // Header: keep the back arrow at the left, center the title + writeup in the header
            val activity = LocalContext.current as? Activity
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(4.dp)) {
                // Back arrow remains pinned to the left
                Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                    if (currentScreen.value == null) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier
                            .size(36.dp)
                            .clickable { activity?.finish() }
                            .padding(6.dp))
                    } else {
                        Spacer(modifier = Modifier.width(36.dp))
                    }
                }

                // Centered title
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Add rows with explicit icons matching the screenshot
            val rows = listOf(
                Pair("Workspace Interface", Icons.Filled.DarkMode),
                Pair("Analytical Canvas & Focus", Icons.Filled.Tune),
                Pair("Intelligence Filtering Logic", Icons.Filled.CenterFocusStrong),
                Pair("Security Protocol", Icons.Filled.Fingerprint),
                Pair("Risk Governance & Execution", Icons.Filled.Gavel),
                Pair("Post-Move Audit", Icons.Filled.List),
                Pair("Intelligence Dispatch", Icons.Filled.Notifications),
                Pair("Asset Universe Filtering", Icons.Filled.ListAlt),
                Pair("Strategy Calibration", Icons.Filled.ShowChart),
                Pair("AI Analytical Tuning", Icons.Filled.Storage),
                Pair("Export Analysis Logs", Icons.Filled.CloudDownload)
            )

            val listState = rememberLazyListState()
            val highlightedIndex = remember { androidx.compose.runtime.mutableStateOf(-1) }

            // If a deep-link target was provided, scroll to and briefly highlight that row
            LaunchedEffect(targetSection) {
                if (targetSection != null) {
                    val key = targetSection.lowercase()
                    val idx = when (key) {
                        "post_move_audit" -> rows.indexOfFirst { it.first == "Post-Move Audit" }
                        "system_configuration" -> rows.indexOfFirst { it.first == "System Configuration" }
                        else -> rows.indexOfFirst { it.first.replace(" ", "_").lowercase().contains(key) }
                    }
                    if (idx >= 0) {
                        // small delay to allow layout to settle
                        delay(120)
                        listState.animateScrollToItem(idx)
                        highlightedIndex.value = idx
                        // keep highlighted for 1.8s
                        delay(1800)
                        highlightedIndex.value = -1
                        // if deep-link targets a specific panel, open it
                        val label = rows.getOrNull(idx)?.first
                        if (label != null) {
                            // open the panel matching the label (Intelligence Dispatch opens its own panel)
                            currentScreen.value = label
                        }
                    }
                }
            }

            // If a detailed screen is active, render it; otherwise show main list
            if (currentScreen.value != null) {
                when (currentScreen.value) {
                    "notifications" -> NotificationSettingsPanel(userSettings = userSettings, onBack = { currentScreen.value = null })
                    else -> SettingsPanel(userSettings = userSettings, section = currentScreen.value ?: "", onBack = { currentScreen.value = null })
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(rows) { pair ->
                        val label = pair.first
                        val icon = pair.second
                        val index = rows.indexOf(pair)
                        SettingRow(label = label, highlighted = (highlightedIndex.value == index), icon = icon) {
                            // navigate on click
                            currentScreen.value = label
                        }
                        Divider(color = Color.White.copy(alpha = 0.03f))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    label: String,
    highlighted: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(horizontal = 16.dp, vertical = 18.dp)
        .background(if (highlighted) Color(0xFF0B2130) else Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.SansSerif)
    }
}
