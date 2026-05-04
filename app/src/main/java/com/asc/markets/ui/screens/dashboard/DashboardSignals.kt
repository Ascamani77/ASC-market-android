package com.asc.markets.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.state.AssetContext
import com.asc.markets.state.AssetContextStore
import com.asc.markets.logic.ForexViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

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
fun DashboardSignals(viewModel: ForexViewModel = viewModel()) {
    val ctx by AssetContextStore.context.collectAsState()
    val aiResponse by viewModel.aiDeployments.collectAsState()
    
    val allSignals = aiResponse?.final_decision ?: emptyList()

    // Helper: map AssetContext -> Keywords for filtering (simplified mapping)
    fun isAssetInContext(asset: String, context: AssetContext): Boolean {
        val a = asset.uppercase()
        return when (context) {
            AssetContext.FOREX -> a.contains("/") || a.length == 6 // e.g. EURUSD or EUR/USD
            AssetContext.COMMODITIES -> a.contains("XAU") || a.contains("XAG") || a.contains("WTI") || a.contains("BRENT") || a.contains("COPPER")
            AssetContext.CRYPTO -> a.contains("BTC") || a.contains("ETH") || a.contains("SOL") || a.contains("ADA") || a.contains("XRP")
            AssetContext.STOCKS -> a.length < 6 && !a.contains("/") // simple heuristic
            AssetContext.INDICES, AssetContext.FUTURES -> a.contains("SPX") || a.contains("NDX") || a.contains("DJI") || a.contains("FTSE") || a.contains("NIKKEI")
            AssetContext.ALL -> true
            else -> true
        }
    }

    val visibleSignals = remember(allSignals, ctx) {
        if (ctx == AssetContext.ALL) allSignals else allSignals.filter { isAssetInContext(it.asset_1 ?: "", ctx) }
    }

    var engineRefreshSeconds by remember { mutableStateOf(300) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            engineRefreshSeconds = (engineRefreshSeconds - 1).coerceAtLeast(0)
            if (engineRefreshSeconds == 0) engineRefreshSeconds = 300
        }
    }

    var selected by remember { mutableStateOf<FinalDecisionItem?>(null) }
    val scrollState = rememberScrollState()

    // Watch scroll and animate header collapse smoothly
    val collapseRange = 150f
    val collapseProgress by remember {
        derivedStateOf {
            (scrollState.value.toFloat() / collapseRange).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(collapseProgress) {
        viewModel.setGlobalHeaderCollapse(collapseProgress)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(PureBlack)
        .verticalScroll(scrollState)
        .padding(top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Global Matrix Header (auto-height)
        InfoBox {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // pulse icon
                Box(modifier = Modifier.size(14.dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Opportunity Awareness Matrix", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
                    if (aiResponse != null) {
                        Text("Last updated: ${aiResponse?.last_updated ?: "N/A"}", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Engine refresh: ${timeStr(engineRefreshSeconds)}", color = SlateText, fontSize = DashboardFontSizes.valueMedium)
            }
        }

        // Group signals into categories based on visible signals
        val categories = listOf("Forex Majors", "Commodities", "Crypto Majors", "Stocks", "Indexes")
        val groups = listOf(
            visibleSignals.filter { isAssetInContext(it.asset_1 ?: "", AssetContext.FOREX) },
            visibleSignals.filter { isAssetInContext(it.asset_1 ?: "", AssetContext.COMMODITIES) },
            visibleSignals.filter { isAssetInContext(it.asset_1 ?: "", AssetContext.CRYPTO) },
            visibleSignals.filter { isAssetInContext(it.asset_1 ?: "", AssetContext.STOCKS) },
            visibleSignals.filter { isAssetInContext(it.asset_1 ?: "", AssetContext.INDICES) }
        )
        
        var openGroup by remember { mutableStateOf<Int?>(null) }

        groups.forEachIndexed { gi, group ->
            if (group.isEmpty()) return@forEachIndexed

            // category label
            Row(modifier = Modifier.fillMaxWidth().clickable { openGroup = gi }.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("›", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, modifier = Modifier.padding(end = 8.dp))
                Text(categories.getOrNull(gi) ?: "Group ${gi + 1}", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Responsive rows for this group
            val cfg = androidx.compose.ui.platform.LocalConfiguration.current
            val cols = if (cfg.screenWidthDp < 720) 1 else 3
            val chunked = group.chunked(cols)
            chunked.forEach { rowSignals ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (s in rowSignals) {
                        SignalCard(item = s, modifier = Modifier.weight(1f)) { selected = it }
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
                                Text(categories.getOrNull(gi) ?: "Group ${gi + 1}", color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black)
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
                                                Text(s.asset_1 ?: "UNKNOWN", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
                                                Text(s.portfolio_decision_reason ?: "No reason provided", color = SlateText, fontSize = DashboardFontSizes.labelLarge)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(s.journal_timestamp?.takeLast(8) ?: "N/A", color = SlateText, fontSize = DashboardFontSizes.labelLarge)
                                                Text("${s.journal_score?.roundToInt() ?: 0}%", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
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
                    Text("Surveillance Protocol Disclosure", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
                    Text("This node provides situational awareness only — not dispatch commands. Beta feature.", color = SlateText, fontSize = DashboardFontSizes.labelLarge)
                }
            }
        }
    }

    // Modal overlay
    if (selected != null) {
        SignalModal(item = selected!!) { selected = null }
    }
}

@Composable
private fun SignalCard(item: FinalDecisionItem, modifier: Modifier = Modifier, onTap: (FinalDecisionItem) -> Unit) {
    // Deterministic weighing engine
    val safetyClosed = isSafetyGateClosed()
    val safetyScore = if (safetyClosed) 0 else 100
    val technicalScore = (item.journal_score ?: 0.0).toInt()
    val combined = ((technicalScore * 0.6) + (safetyScore * 0.4)).roundToInt()
    val state = when {
        safetyClosed -> "WAIT"
        combined >= 75 -> "FOCUS"
        combined >= 50 -> "OBSERVE"
        else -> "WAIT"
    }

    InfoBox(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().clickable { onTap(item) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val stateColor = when (state) { "FOCUS" -> IndigoAccent; "OBSERVE" -> IndigoAccent.copy(alpha = 0.7f); else -> RoseError }
                    Text(state, color = stateColor, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                    Text(item.asset_1 ?: "UNKNOWN", color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black)
                }
                Text(item.journal_label ?: "H1", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel)
            }

            // Trend sparkline preview
            val dirIsBuy = (item.journal_direction?.uppercase() ?: "BUY") == "BUY"
            MiniSparkline(
                points = demoSparkline(count = 18, seed = (item.asset_1 ?: "BTC").hashCode(), trendBias = if (dirIsBuy) 0.02f else -0.015f),
                modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp)),
                color = if (dirIsBuy) EmeraldSuccess else RoseError,
                fillColor = if (dirIsBuy) EmeraldSuccess.copy(alpha = 0.08f) else RoseError.copy(alpha = 0.08f)
            )

            // Zones (A / B / C) — condensed institutional view
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Zone A: Validation header
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone A", color = IndigoAccent, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Validated ${item.journal_timestamp?.takeLast(8) ?: "N/A"}", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✓ ${item.portfolio_deployment_bucket ?: "Surveillance"}", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                    Text("✓ ${item.journal_priority ?: "NORMAL"} Priority", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                }

                // Zone B: Performance & levels
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone B", color = IndigoAccent, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("RISK: ${item.final_risk_pct ?: 0.0}%", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                    Text("SCALE: ${item.final_position_scale ?: 0.0}x", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Allocation: $${item.final_risk_amount ?: 0.0}", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny)
                }

                // Zone C: Risk & audits
                Column(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("Zone C", color = IndigoAccent, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(28.dp).background(RoseError.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            Text("🛡️", fontSize = DashboardFontSizes.emojiIcon)
                        }
                        Column { Text("Shield: OK", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny); Text("Circuit: ${item.portfolio_decision_label ?: "STABLE"}", color = SlateText, fontSize = DashboardFontSizes.gridLabelTiny) }
                    }
                }
            }

            // Middle: bias & confidence
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Direction text (no background; color indicates direction)
                val dirIsBuy = (item.journal_direction?.uppercase() ?: "BUY") == "BUY"
                val confidence = combined / 100f
                Text(if (dirIsBuy) "BUY" else "SELL", color = if (dirIsBuy) EmeraldSuccess else RoseError, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Confidence meter
                    val animated = animateFloatAsState(targetValue = confidence, animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = FastOutSlowInEasing)).value
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().width((animated * 100).dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Confidence: ${ (confidence * 100).roundToInt()}% — 60% Tech / 40% Safety", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.portfolio_decision_reason ?: "Analyzing market structure confluence...", color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel)
                }
                CompactGauge(
                    value = (confidence * 100).roundToInt(),
                    color = if (confidence >= 0.7f) EmeraldSuccess else if (confidence >= 0.5f) Color(0xFFF59E0B) else RoseError,
                    size = 62.dp,
                    label = "CONF"
                )
                CompactGauge(
                    value = ((1f - confidence) * 100).roundToInt(),
                    color = if (confidence >= 0.7f) EmeraldSuccess else RoseError,
                    size = 62.dp,
                    label = "RISK"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom: action levels and audit
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Decision", color = SlateText, fontSize = DashboardFontSizes.labelMedium); Text(item.portfolio_decision_label ?: "WATCH", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black) }
                Column { Text("Weight", color = SlateText, fontSize = DashboardFontSizes.labelMedium); Text("${item.final_position_scale ?: 1.0}x", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black) }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Prop Guard • Intelligence Audit: Monitoring ${item.asset_1}", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(20.dp).background(IndigoAccent.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🧠", fontSize = DashboardFontSizes.labelSmall) }
                    Box(modifier = Modifier.size(20.dp).background(IndigoAccent.copy(alpha = 0.12f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🛡️", fontSize = DashboardFontSizes.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun SignalModal(item: FinalDecisionItem, onClose: () -> Unit) {
    // Full-screen overlay with dim and a soft glass watermark
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle watermark behind content
                    Text(
                        "ASC",
                        color = Color.White.copy(alpha = 0.04f),
                        fontSize = DashboardFontSizes.signalZoneEmoji,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.asset_1} — Tactical Overlay", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
                            Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                        }

                        Text("The Why:", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
                        Text("Institutional logic: ${item.portfolio_decision_reason}. Model weighs technical confluence and safety clearing to produce actionable awareness.", color = Color.White, fontSize = DashboardFontSizes.labelLarge)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Post-Analysis Data", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
                        Column { 
                            Text("• Direction: ${item.journal_direction}", color = SlateText)
                            Text("• Score: ${item.journal_score}", color = SlateText)
                            Text("• Priority: ${item.journal_priority}", color = SlateText)
                            Text("• Bucket: ${item.portfolio_deployment_bucket}", color = SlateText)
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text("ASC — Intelligence Audit", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
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
