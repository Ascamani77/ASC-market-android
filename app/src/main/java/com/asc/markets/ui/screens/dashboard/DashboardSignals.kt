package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt

data class Signal(
    val symbol: String,
    val pairFlag: Int? = null, // optional drawable resource id
    val timeframe: String,
    val technicalScore: Int,
    val rationale: List<String>,
    val entry: String,
    val invalidation: String
)

@Composable
fun DashboardSignals() {
    // sample signals — 25 items: 5 Forex majors, 5 Commodities, 5 Crypto, 5 Stocks, 5 Indexes
    val signals = remember {
        listOf(
            // Forex majors (5)
            Signal("EUR/USD", timeframe = "H1", technicalScore = 88, rationale = listOf("Order Block Validated", "Asian Low Sweep", "Liquidity Grab"), entry = "1.0840", invalidation = "1.0790"),
            Signal("GBP/USD", timeframe = "H4", technicalScore = 72, rationale = listOf("MSS Break", "OB Confluence"), entry = "1.2660", invalidation = "1.2580"),
            Signal("USD/JPY", timeframe = "M30", technicalScore = 64, rationale = listOf("POC Rejection", "VWAP Deceleration"), entry = "149.20", invalidation = "150.10"),
            Signal("AUD/USD", timeframe = "H1", technicalScore = 55, rationale = listOf("Asian Range", "Liquidity Node"), entry = "0.6450", invalidation = "0.6410"),
            Signal("USD/CHF", timeframe = "H1", technicalScore = 60, rationale = listOf("Pivot Rejection", "Symmetry Level"), entry = "0.9120", invalidation = "0.9060"),

            // Commodities (5)
            Signal("XAU/USD", timeframe = "D1", technicalScore = 70, rationale = listOf("Long-term OB", "Inflation Flows"), entry = "1938", invalidation = "1910"),
            Signal("XAG/USD", timeframe = "H4", technicalScore = 58, rationale = listOf("Volatility Spike", "Speculative Flow"), entry = "23.10", invalidation = "22.40"),
            Signal("WTI/USD", timeframe = "H4", technicalScore = 66, rationale = listOf("Inventory Drawdown", "Momentum"), entry = "72.40", invalidation = "70.20"),
            Signal("BRENT/USD", timeframe = "H4", technicalScore = 62, rationale = listOf("Refinery Cycle", "Supply Risk"), entry = "76.80", invalidation = "74.50"),
            Signal("COPPER", timeframe = "D1", technicalScore = 54, rationale = listOf("Manufacturing PMI", "Seasonal Demand"), entry = "3.92", invalidation = "3.70"),

            // Crypto majors (5)
            Signal("BTC/USD", timeframe = "H4", technicalScore = 77, rationale = listOf("On-chain Accumulation", "Futures Roll"), entry = "48200", invalidation = "46800"),
            Signal("ETH/USD", timeframe = "H4", technicalScore = 74, rationale = listOf("Options Flow", "DeFi Activity"), entry = "3200", invalidation = "3050"),
            Signal("SOL/USD", timeframe = "H1", technicalScore = 61, rationale = listOf("Network Activity", "Short Squeeze"), entry = "120", invalidation = "110"),
            Signal("ADA/USD", timeframe = "H4", technicalScore = 52, rationale = listOf("Sentiment Lag", "Developer Signals"), entry = "0.95", invalidation = "0.88"),
            Signal("XRP/USD", timeframe = "H4", technicalScore = 50, rationale = listOf("Regulatory Watch", "On-Chain Liquidity"), entry = "0.65", invalidation = "0.60"),

            // Stocks (5)
            Signal("AAPL", timeframe = "D1", technicalScore = 79, rationale = listOf("Earnings Beat", "Option Sweep"), entry = "169.50", invalidation = "165.00"),
            Signal("MSFT", timeframe = "D1", technicalScore = 75, rationale = listOf("Cloud Momentum", "Institutional Buying"), entry = "320.10", invalidation = "310.00"),
            Signal("AMZN", timeframe = "H4", technicalScore = 68, rationale = listOf("Retail Strength", "Prime Cycle"), entry = "138.40", invalidation = "134.00"),
            Signal("GOOGL", timeframe = "D1", technicalScore = 71, rationale = listOf("Ad Rev Upside", "POC Acceptance"), entry = "115.20", invalidation = "112.00"),
            Signal("TSLA", timeframe = "H4", technicalScore = 63, rationale = listOf("Delivery Guidance", "Short Interest"), entry = "240.00", invalidation = "230.00"),

            // Indexes (5)
            Signal("SPX", timeframe = "H1", technicalScore = 69, rationale = listOf("Sector Breadth", "Macro Data"), entry = "5050", invalidation = "4980"),
            Signal("NDX", timeframe = "H1", technicalScore = 72, rationale = listOf("Tech Strength", "Futures Premium"), entry = "17320", invalidation = "17100"),
            Signal("DJI", timeframe = "D1", technicalScore = 58, rationale = listOf("Blue-chip Rotation", "Dividend Flow"), entry = "35500", invalidation = "35100"),
            Signal("FTSE", timeframe = "H4", technicalScore = 51, rationale = listOf("Brexit Watch", "Commodity Link"), entry = "7700", invalidation = "7600"),
            Signal("NIKKEI", timeframe = "H4", technicalScore = 56, rationale = listOf("Yen FX Influence", "Export Data"), entry = "35200", invalidation = "34800")
        )
    }

    var engineRefreshSeconds by remember { mutableStateOf(300) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            engineRefreshSeconds = (engineRefreshSeconds - 1).coerceAtLeast(0)
            if (engineRefreshSeconds == 0) engineRefreshSeconds = 300
        }
    }

    var selected by remember { mutableStateOf<Signal?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0D0D0D))
        .verticalScroll(rememberScrollState())
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Global Matrix Header (auto-height)
        InfoBox {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // pulse icon
                Box(modifier = Modifier.size(14.dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Opportunity Awareness Matrix", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.weight(1f))
                Text("Engine refresh: ${timeStr(engineRefreshSeconds)}", color = SlateText, fontSize = 12.sp)
            }
        }

        // Group signals into categories of 5 and label each group on the deep-black background
        val groupLabels = listOf("Forex Majors", "Commodities", "Crypto Majors", "Stocks", "Indexes")
        val groups = signals.chunked(5)
        var openGroup by remember { mutableStateOf<Int?>(null) }

        groups.forEachIndexed { gi, group ->
            // category label (on deep black background, not inside InfoBox)
            Row(modifier = Modifier.fillMaxWidth().clickable { openGroup = gi }.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("›", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Text(groupLabels.getOrNull(gi) ?: "Group ${gi + 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Responsive rows for this group
            val cfg = androidx.compose.ui.platform.LocalConfiguration.current
            val cols = if (cfg.screenWidthDp < 720) 1 else 3
            val chunked = group.chunked(cols)
            chunked.forEach { rowSignals ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (s in rowSignals) {
                        SignalCard(signal = s, modifier = Modifier.weight(1f)) { selected = it }
                    }
                    // fill remaining columns if needed
                    if (rowSignals.size < cols) {
                        repeat(cols - rowSignals.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Group modal: show full list for the opened category in InfoBox style
        if (openGroup != null) {
            val gi = openGroup!!
            Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(groupLabels.getOrNull(gi) ?: "Group ${gi + 1}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("Close", color = SlateText, modifier = Modifier.clickable { openGroup = null })
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // show each pair as an InfoBox within this modal
                            val list = groups.getOrNull(gi) ?: emptyList()
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                list.forEach { s ->
                                    InfoBox {
                                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(s.symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                                Text(s.rationale.joinToString(", "), color = SlateText, fontSize = 11.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(s.timeframe, color = SlateText, fontSize = 11.sp)
                                                Text(s.entry, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Footer disclosure (auto-height)
        InfoBox {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(16.dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Surveillance Protocol Disclosure", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("This node provides situational awareness only — not dispatch commands. Beta feature.", color = SlateText, fontSize = 11.sp)
                }
            }
        }
    }

    // Modal overlay
    if (selected != null) {
        SignalModal(signal = selected!!) { selected = null }
    }
}

@Composable
private fun SignalCard(signal: Signal, modifier: Modifier = Modifier, onTap: (Signal) -> Unit) {
    // Deterministic weighing engine
    val safetyClosed = isSafetyGateClosed()
    val safetyScore = if (safetyClosed) 0 else 100
    val combined = ((signal.technicalScore * 0.6) + (safetyScore * 0.4)).roundToInt()
    val state = when {
        safetyClosed -> "WAIT"
        combined >= 75 -> "FOCUS"
        combined >= 50 -> "OBSERVE"
        else -> "WAIT"
    }

    InfoBox(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onTap(signal) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stateColor = when (state) { "FOCUS" -> IndigoAccent; "OBSERVE" -> IndigoAccent.copy(alpha = 0.7f); else -> RoseError }
                    Text(state, color = stateColor, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                    Text(signal.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text(signal.timeframe, color = SlateText, fontSize = 11.sp)
            }

            // Zones (A / B / C) — condensed institutional view
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Zone A: Validation header
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone A", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Validated 04:12 UTC", color = SlateText, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✓ Order Block", color = SlateText, fontSize = 10.sp)
                    Text("✓ Liquidity Sweep", color = SlateText, fontSize = 10.sp)
                }

                // Zone B: Performance & levels
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone B", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ATR: 8 pips", color = SlateText, fontSize = 10.sp)
                    Text("VOLUME: ↑ 1.3x", color = SlateText, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Support: 1.0835 — Resist: 1.0865", color = SlateText, fontSize = 10.sp)
                }

                // Zone C: Risk & audits
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone C", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(28.dp).background(RoseError.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            Text("🛡️", fontSize = 14.sp)
                        }
                        Column { Text("Shield: OK", color = SlateText, fontSize = 10.sp); Text("BrainCircuit: Stable", color = SlateText, fontSize = 10.sp) }
                    }
                }
            }

            // Middle: bias & confidence
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Direction text (no background; color indicates direction)
                val dirIsBuy = signal.technicalScore >= 60
                Text(if (dirIsBuy) "BUY" else "SELL", color = if (dirIsBuy) EmeraldSuccess else RoseError, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Confidence meter
                    val confidence = combined / 100f
                    val animated = animateFloatAsState(targetValue = confidence, animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = FastOutSlowInEasing)).value
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().width((animated * 100).dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Confidence: ${ (confidence * 100).roundToInt()}% — 60% Tech / 40% Safety", color = SlateText, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        signal.rationale.forEach { r -> Text("• $r", color = SlateText, fontSize = 11.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom: action levels and audit
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Entry", color = SlateText, fontSize = 10.sp); Text(signal.entry, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }
                Column { Text("Invalidation", color = SlateText, fontSize = 10.sp); Text(signal.invalidation, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }
            }

            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Prop Guard • Intelligence Audit: Monitoring ${signal.timeframe}", color = SlateText, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(20.dp).background(IndigoAccent.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🧠", fontSize = 10.sp) }
                    Box(modifier = Modifier.size(20.dp).background(IndigoAccent.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🛡️", fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun SignalModal(signal: Signal, onClose: () -> Unit) {
    // Full-screen overlay with dim and a soft glass watermark
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle watermark behind content
                    Text(
                        "ASC",
                        color = Color.White.copy(alpha = 0.04f),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${signal.symbol} — Tactical Overlay", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                        }

                        Text("The Why:", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("Institutional logic: ${signal.rationale.joinToString(", ")}. Model weighs technical confluence and safety clearing to produce actionable awareness.", color = Color.White, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Post-Analysis Data", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Column { Text("• VIX Input: 19.8", color = SlateText); Text("• DXY Beta: +0.42%", color = SlateText); Text("• Retail Alignment: Neutral", color = SlateText) }

                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text("ASC — Intelligence Audit", color = SlateText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun timeStr(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
