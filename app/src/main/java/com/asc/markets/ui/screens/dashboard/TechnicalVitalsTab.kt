package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.CandlestickChart
import androidx.compose.foundation.lazy.LazyColumn
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.random.Random

@Composable
fun TechnicalVitalsScreen() {
    val vitals = rememberTechnicalVitals()
    
    Column(modifier = Modifier
        .fillMaxSize()
        .background(DeepBlack)
        .padding(vertical = 12.dp)
    ) {
        // Primary Node: Session Progress (full width)
        Box(modifier = Modifier.fillMaxWidth()) {
            SessionProgressNode()
        }

        Spacer(modifier = Modifier.height(12.dp))

        // KPI Grid: 4 square cards in a single row (scrollable if needed)
        Box(modifier = Modifier.fillMaxWidth()) {
            val scroll = rememberScrollState()
            Row(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val cfg = LocalConfiguration.current
                val screenDp = cfg.screenWidthDp
                val padding = 32 // 16 left + 16 right approximate
                val gap = 12 * 3 // three gaps between 4 cards
                val cardDp = ((screenDp - padding - gap) / 4f).coerceAtLeast(72f)
                val cardSize = with(LocalDensity.current) { cardDp.dp }

                VitalsKpiCard("NODE_HEALTH", String.format("%.2f", vitals.nodeHealth), "Institutional Feed", cardSize, status = VitalsStatus.Active)
                VitalsKpiCard("AVG_SPREAD", String.format("%.2f pips", vitals.avgSpread), "Bid/Ask spread", cardSize, status = VitalsStatus.Active)
                VitalsKpiCard("VOL_P/H", String.format("%.1f P/H", vitals.volatilityPerHour), "20-candle range", cardSize, status = VitalsStatus.Processing)
                VitalsKpiCard("LATENCY", String.format("%.2f ms", vitals.latencyMs), "Direct LMAX Uplink", cardSize, status = VitalsStatus.Active)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contextual Nodes: Global Regime (narrow) + Macro Intelligence Stream (wide)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Global Regime (narrow)
            InfoBox(modifier = Modifier.width(160.dp), minHeight = 160.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header
                    Text("GLOBAL REGIME", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(vitals.globalRegime, color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
                    Text("VIX: ${String.format("%.1f", vitals.vixValue)} • DXY: ${String.format("+%.2f%%", vitals.dxyChange)}", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    Text("Audit • updated ${nowUtcFormatted()}", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
                }
            }

            // Macro Intelligence Stream (wide)
            InfoBox(modifier = Modifier.weight(1f), minHeight = 160.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("MACRO INTELLIGENCE STREAM", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(if (vitals.safetyGateClosed) "BLOCKED" else "ARMED", color = if (vitals.safetyGateClosed) RoseError else EmeraldSuccess, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
                    }

                    // Display macro comment
                    Column(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text(vitals.macroComment, color = Color.White, fontSize = DashboardFontSizes.labelMedium, lineHeight = 16.sp)
                    }

                    Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    Text("Audit • direct uplink: TL-01 • ${nowUtcFormatted()}", color = SlateText, fontSize = DashboardFontSizes.labelSmall)
                }
            }
        }
    }
}

private fun nowUtcFormatted(): String {
    return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm'Z'"))
}

private fun avgSpreadSample(): Double = (Random.nextDouble(0.05, 0.35))
private fun volatilitySample(): Double = (Random.nextDouble(3.0, 18.0))
private fun nodeLatencySample(): Double = (Random.nextDouble(0.5, 12.0))

data class StreamEvent(val time: String, val text: String)
private fun sampleStreamEvents(): List<StreamEvent> = listOf(
    StreamEvent("08:41", "RSI enters Overbought (>70) on M5"),
    StreamEvent("08:38", "Volume spike 2.3x 10-period MA"),
    StreamEvent("08:35", "Price touches R1 level 1.0892"),
    StreamEvent("08:32", "Higher High / Higher Low confirmed")
)

enum class VitalsStatus { Active, Blocked, Processing }

@Composable
private fun VitalsKpiCard(label: String, value: String, sub: String, size: androidx.compose.ui.unit.Dp, status: VitalsStatus) {
    InfoBox(modifier = Modifier.size(size), minHeight = size) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = SlateText, fontSize = DashboardFontSizes.vitalsKpiLabel, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box(modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (status == VitalsStatus.Blocked) RoseError else if (status == VitalsStatus.Processing) IndigoAccent else EmeraldSuccess)
                ) {}
            }

            // Primary value
            Text(value, color = Color.White, fontSize = DashboardFontSizes.vitalsKpiValue, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(sub, color = SlateText, fontSize = DashboardFontSizes.labelMedium)

            Spacer(modifier = Modifier.weight(1f))

            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Text("Audit • ${nowUtcFormatted()}", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
        }
    }
}

@Composable
private fun SessionProgressNode() {
    // compute session info based on UTC
    var now by remember { mutableStateOf(ZonedDateTime.now(ZoneOffset.UTC)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(ZoneOffset.UTC)
            delay(1000L)
        }
    }

    val session = sessionFor(now)
    val start = session.first
    val end = session.second
    val total = Duration.between(start, end).toMillis().coerceAtLeast(1)
    val elapsed = Duration.between(start, now).toMillis().coerceAtLeast(0).coerceAtMost(total)
    val pct = elapsed.toFloat() / total.toFloat()
    val remaining = Duration.between(now, end).seconds.coerceAtLeast(0)

    InfoBox(modifier = Modifier.fillMaxWidth(), minHeight = 140.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Gauge
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
                        val stroke = Stroke(width = 10f, cap = StrokeCap.Round)
                        val startAngle = -90f
                        drawArc(Color.White.copy(alpha = 0.06f), startAngle, 360f, false, topLeft = Offset.Zero, size = size, style = stroke)
                        drawArc(IndigoAccent, startAngle, 360f * pct, false, topLeft = Offset.Zero, size = size, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format("%d%%", (pct * 100).toInt()), color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black)
                    Text(timeStrFromSeconds(remaining), color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                }
            }

            // Narrative & context
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Session Progress", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderLarge, fontWeight = FontWeight.Black)
                    Text(sessionNameFor(now), color = IndigoAccent, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
                }

                Text("${sessionNameFor(now)} ${start.toLocalTime()}–${end.toLocalTime()} UTC", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                Text("Global Regime: ${regimeSummary()}", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun timeStrFromSeconds(sec: Long): String {
    val s = sec % 60
    val m = (sec / 60) % 60
    val h = sec / 3600
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun sessionNameFor(now: ZonedDateTime): String {
    val utc = now.toLocalTime()
    return when {
        utc.hour in 8..15 -> "LONDON"
        utc.hour in 13..20 -> "NEW YORK"
        utc.hour in 0..7 -> "TOKYO"
        else -> "GLOBAL"
    }
}

private fun sessionFor(now: ZonedDateTime): Pair<ZonedDateTime, ZonedDateTime> {
    val date = now.toLocalDate()
    val utc = now.toLocalTime()
    return when {
        utc.hour in 8..15 -> Pair(ZonedDateTime.of(date, java.time.LocalTime.of(8,0), ZoneOffset.UTC), ZonedDateTime.of(date, java.time.LocalTime.of(16,0), ZoneOffset.UTC))
        utc.hour in 13..20 -> Pair(ZonedDateTime.of(date, java.time.LocalTime.of(13,0), ZoneOffset.UTC), ZonedDateTime.of(date, java.time.LocalTime.of(21,0), ZoneOffset.UTC))
        utc.hour in 0..7 -> Pair(ZonedDateTime.of(date, java.time.LocalTime.of(0,0), ZoneOffset.UTC), ZonedDateTime.of(date, java.time.LocalTime.of(8,0), ZoneOffset.UTC))
        else -> Pair(ZonedDateTime.of(date, java.time.LocalTime.of(0,0), ZoneOffset.UTC), ZonedDateTime.of(date, java.time.LocalTime.of(23,59,59), ZoneOffset.UTC))
    }
}

private fun regimeSummary(): String = "Risk-Off: Equities under pressure due to hawkish FED tone"

fun isSafetyGateClosed(): Boolean {
    // mock: check if any hard-coded high-impact event is within ±30 minutes of now UTC
    val now = Instant.now()
    val events = listOf(
        Instant.now().plusSeconds(60 * 25)
    )
    return events.any { ev -> kotlin.math.abs(Duration.between(now, ev).toMinutes()) <= 30 }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TechnicalVitalsTab() {
    val scrollState = rememberScrollState()
    val tapeRequester = remember { BringIntoViewRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(DeepBlack)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Node: full-width session progress
        SessionProgressNode()

        // KPI Grid: 2x2 square cards (two columns, two rows)
        val cfg = LocalConfiguration.current
        val screenDp = cfg.screenWidthDp
        val padding = 32 // 16 left + 16 right
        val gap = 12 // one gap between two columns
        val cardDp = ((screenDp - padding - gap) / 2f).coerceAtLeast(72f)
        val cardSize = with(LocalDensity.current) { cardDp.dp }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalsKpiCard(label = "AVG_SPREAD", value = String.format("%.2f pips", avgSpreadSample()), sub = "Institutional Bid/Ask", size = cardSize, status = VitalsStatus.Active)
            VitalsKpiCard(label = "VOL_P/H", value = String.format("%.1f P/H", volatilitySample()), sub = "20-candle range", size = cardSize, status = VitalsStatus.Processing)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalsKpiCard(label = "SAFETY_GATE", value = if (isSafetyGateClosed()) "BLOCKED" else "ARMED", sub = "High-impact window", size = cardSize, status = if (isSafetyGateClosed()) VitalsStatus.Blocked else VitalsStatus.Active)
            VitalsKpiCard(label = "NODE_LATENCY", value = String.format("%.2f ms", nodeLatencySample()), sub = "Direct LMAX Uplink", size = cardSize, status = VitalsStatus.Active)
        }

        // Contextual Nodes: stack Global Regime above Macro Intelligence Stream
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBox(modifier = Modifier.fillMaxWidth(), minHeight = 200.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GLOBAL REGIME", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(regimeSummary(), color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
                    Text("VIX: 19.8 • DXY: +0.42%", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
                    Spacer(modifier = Modifier.weight(1f))
                    Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    Text("Audit • updated ${nowUtcFormatted()}", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
                }
            }

            InfoBox(modifier = Modifier.fillMaxWidth().bringIntoViewRequester(tapeRequester), minHeight = 260.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MACRO INTELLIGENCE STREAM", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        val safety = isSafetyGateClosed()
                        Box(modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (safety) RoseError else EmeraldSuccess)
                        ) {}
                    }

                    val events = remember { sampleStreamEvents() }
                    Column(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        events.forEach { ev ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(ev.time, color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                                Text(ev.text, color = Color.White, fontSize = DashboardFontSizes.labelMedium)
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                    Text("Audit • direct uplink: TL-01 • ${nowUtcFormatted()}", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
                }
            }
        }
        LaunchedEffect(Unit) {
            // scroll to top when page opens
            scrollState.scrollTo(0)
        }
    }
}

@Composable
fun MarketDepthLadder() {
    val showMicro = com.asc.markets.ui.components.LocalShowMicrostructure.current
    if (!showMicro) return

    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("MARKET DEPTH (L2)", color = Color.Gray, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            repeat(5) { i ->
                DepthRow(price = "1.084${50-i}", size = "${100 + i * 20}", color = RoseError.copy(alpha = 0.6f - (i * 0.1f)))
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)).padding(vertical = 4.dp))
            repeat(5) { i ->
                DepthRow(price = "1.084${40-i}", size = "${150 - i * 15}", color = EmeraldSuccess.copy(alpha = 0.1f + (i * 0.1f)))
            }
        }
    }
}

@Composable
private fun DepthRow(price: String, size: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(size, color = Color.Gray, fontSize = DashboardFontSizes.gridLabelTiny, modifier = Modifier.width(40.dp))
        Box(modifier = Modifier.weight(1f).height(16.dp).background(color, RoundedCornerShape(2.dp)))
        Text(price, color = Color.White, fontSize = DashboardFontSizes.vitalsKpiLabel, modifier = Modifier.padding(start = 12.dp), fontFamily = InterFontFamily)
    }
}

@Composable
private fun VitalsBox(label: String, value: String, color: Color, modifier: Modifier) {
    InfoBox(modifier = modifier, height = 100.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black)
            Text(value, color = color, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black)
        }
    }
}