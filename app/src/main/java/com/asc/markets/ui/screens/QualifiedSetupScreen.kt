package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ASCSignalData
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.ForexPair
import com.asc.markets.data.ScannerSignal
import com.asc.markets.data.ScannerSignalsStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.PriceStreamManager
import com.asc.markets.ui.components.ConfidenceGauge
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import java.util.Locale

@Composable
fun QualifiedSetupScreen(viewModel: ForexViewModel) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val signalsByAsset by EASignalLiveStore.signalsByAsset.collectAsState()
    val scannerSignals by ScannerSignalsStore.signals.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedPair.symbol) {
        EASignalLiveStore.requestSignal(selectedPair.symbol)
    }

    val signal = remember(selectedPair, signalsByAsset) { signalForSetup(selectedPair, signalsByAsset) }
    val scanner = remember(selectedPair, scannerSignals) { scannerForSetup(selectedPair.symbol, scannerSignals) }
    val livePrice = PriceStreamManager.prices[selectedPair.symbol]
        ?: PriceStreamManager.prices[selectedPair.symbol.replace("/", "")]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(scrollState)
    ) {
        SetupHeader(viewModel, selectedPair, livePrice)
        Spacer(Modifier.height(16.dp))

        if (signal == null) {
            NoSignalState(selectedPair)
        } else {
            VerdictDashboard(signal, scanner, selectedPair)
            Spacer(Modifier.height(12.dp))
            VoteIntelligenceBox(signal)
            Spacer(Modifier.height(12.dp))
            RegimeStructureBox(signal)
            Spacer(Modifier.height(12.dp))
            LiquidityRadarBox(signal)
            Spacer(Modifier.height(12.dp))
            ValidationQualityBox(signal)
            Spacer(Modifier.height(12.dp))
            EntryTradePlanBox(signal)
            Spacer(Modifier.height(12.dp))
            ContextBox(signal)
        }
        Spacer(Modifier.height(96.dp))
    }
}

// ── Signal resolution helpers ──────────────────────────────────────────────
private fun signalForSetup(pair: ForexPair, signalsByAsset: Map<String, ASCSignalData>): ASCSignalData? {
    if (signalsByAsset.isEmpty()) return null
    val key = pair.symbol.uppercase(Locale.US)
        .replace("/", "").replace("-", "").replace("_", "")
        .replace(" ", "").replace(".", "").removeSuffix("M")
    return signalsByAsset[key]
}

private fun scannerForSetup(symbol: String, scannerSignals: List<ScannerSignal>): ScannerSignal? {
    val key = symbol.uppercase(Locale.US)
        .replace("/", "").replace("-", "").replace("_", "")
        .replace(" ", "").replace(".", "").removeSuffix("M")
    return scannerSignals.firstOrNull { it.asset.uppercase(Locale.US).replace("/", "").removeSuffix("M") == key }
}

// ── Header ────────────────────────────────────────────────────────────────
@Composable
private fun SetupHeader(viewModel: ForexViewModel, pair: ForexPair, livePrice: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepBlack)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.navigateTo(com.asc.markets.data.AppView.WATCHLIST) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        PairFlags(symbol = pair.symbol, size = 34)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(pair.symbol, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(pair.name, color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val price = livePrice ?: pair.price
        if (price > 0.0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(formatSetupPrice(price), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                val dir = if (pair.changePercent >= 0) "▲" else "▼"
                val col = if (pair.changePercent >= 0) EmeraldSuccess else RoseError
                Text("$dir ${String.format(Locale.US, "%.2f", pair.changePercent)}%", color = col, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
            }
        }
    }
}

// ── Verdict dashboard ─────────────────────────────────────────────────────
@Composable
private fun VerdictDashboard(signal: ASCSignalData, scanner: ScannerSignal?, pair: ForexPair) {
    val votes = signal.chart_panel?.votes
    // signal.confidence is 0-1 (e.g. 0.981 = 98%). votes.win_pct from the EA
    // is already 0-100 (e.g. 98.1), so normalize both before display.
    val gaugeScore = if (signal.confidence > 0.0) {
        (signal.confidence * 100).toInt().coerceIn(0, 100)
    } else {
        pctAny(votes?.win_pct)
    }
    val direction = signal.direction
    val dirLabel = when (direction.uppercase()) {
        "BUY" -> "LONG"
        "SELL" -> "SHORT"
        else -> "WAIT"
    }
    val dirColor = when (dirLabel) {
        "LONG" -> EmeraldSuccess
        "SHORT" -> RoseError
        else -> SlateText
    }
    val validator = signal.chart_panel
    val validatorDir = validator?.validator_direction?.uppercase() ?: ""
    val quality = signal.chart_panel?.quality_tier ?: signal.quality?.grade ?: "NONE"

    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 150.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConfidenceGauge(gaugeScore)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("QUALIFIED SETUP", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontFamily = InterFontFamily)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dirLabel, color = dirColor, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    QualityBadge(quality)
                }
                if (scanner != null) {
                    MetricRow("SCANNER", "${pretty(scanner.direction.uppercase())} • P ${pctAny(scanner.confidence)}% • ${scanner.age}s")
                } else {
                    MetricRow("DIRECTION", pretty(signal.direction.uppercase()))
                }
                MetricRow("CONFIDENCE", "${pct01(signal.confidence)}%")
                val valActive = validator?.validator_active == true
                MetricRow("VALIDATOR", if (valActive) "${pretty(validatorDir)} @ ${pctAny(validator?.validator_pwin)}% win" else "INACTIVE")
            }
        }
    }
}

// ── Vote intelligence ─────────────────────────────────────────────────────
@Composable
private fun VoteIntelligenceBox(signal: ASCSignalData) {
    val votes = signal.chart_panel?.votes
    if (votes == null) return
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 120.dp) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("VOTE INTELLIGENCE")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VoteStat("SMC BULL", votes.smc_bull, EmeraldSuccess)
                VoteStat("SMC BEAR", votes.smc_bear, RoseError)
                VoteStat("AI BULL", votes.ai_bull, EmeraldSuccess)
                VoteStat("AI BEAR", votes.ai_bear, RoseError)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VoteTotalStat("TOTAL BULL", "${votes.total_bull}", Modifier.weight(1f))
                VoteTotalStat("TOTAL BEAR", "${votes.total_bear}", Modifier.weight(1f))
                VoteTotalStat("WIN %", "${pctAny(votes.win_pct)}%", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VoteStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = color, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}

@Composable
private fun VoteTotalStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(pretty(value), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(Modifier.height(2.dp))
        Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}

// ── Regime & structure ────────────────────────────────────────────────────
@Composable
private fun RegimeStructureBox(signal: ASCSignalData) {
    val regime = signal.regime
    val structure = signal.structure
    val ind = signal.indicators
    val vol = signal.volatility
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 120.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("REGIME & STRUCTURE")
            MetricRow("REGIME", "${regime?.state ?: "UNKNOWN"} • ${regime?.trend ?: "NEUTRAL"} (${ScorePct(regime?.score)})")
            MetricRow("CONFIDENCE", regime?.confidence ?: "NONE")
            MetricRow("STRUCTURE", "${structure?.bias ?: "NEUTRAL"} • ${ScorePct(structure?.score)}")
            MetricRow("VOLATILITY", "${vol?.state ?: "NORMAL"} ${ScorePct(vol?.score)} • ATR ${fmt1(vol?.atr_ratio ?: 0.0)}")
            MetricRow("INDICATORS", "${ind?.bias ?: "NEUTRAL"}  RSI ${fmt1(ind?.rsi ?: 50.0)} ADX ${fmt1(ind?.adx ?: 0.0)}")
            MetricRow("ALIGNMENT", "${pctAny(signal.alignment_percentage)}%")
        }
    }
}

// ── Liquidity radar ───────────────────────────────────────────────────────
@Composable
private fun LiquidityRadarBox(signal: ASCSignalData) {
    val liq = signal.liquidity
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 120.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("LIQUIDITY RADAR")
            MetricRow("STATE", "${liq?.state ?: "NEUTRAL"} • ${liq?.bias ?: "NEUTRAL"} ${ScorePct(liq?.score)}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiqChip("FVG", liq?.fvg_bull == true || liq?.fvg_bear == true)
                LiqChip("SWEEP", liq?.sweep_high == true || liq?.sweep_low == true)
                LiqChip("BOS", liq?.bos_bull == true || liq?.bos_bear == true)
                if (signal.zone_context_valid) LiqChip("ZONE", true)
                if (signal.exhaustion_detected) LiqChip("EXHAUSTION", true, RoseError)
            }
        }
    }
}

@Composable
private fun LiqChip(label: String, active: Boolean, color: Color = EmeraldSuccess) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (active) color.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (active) color else SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}

// ── Validation & quality ──────────────────────────────────────────────────
@Composable
private fun ValidationQualityBox(signal: ASCSignalData) {
    val vald = signal.validation
    val q = signal.quality
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 120.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("VALIDATION & QUALITY")
            MetricRow("VALIDATION", "${vald?.state ?: "UNKNOWN"} ${ScorePct(vald?.score)}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValChip("SPREAD", vald?.spread_ok == true)
                ValChip("TIMING", vald?.timing_ok == true)
                ValChip("R:R", vald?.rr_ok == true)
                ValChip("MARKET", vald?.market_ok == true)
                ValChip("MTF", vald?.mtf_ok == true)
            }
            Spacer(Modifier.height(4.dp))
            MetricRow("QUALITY GRADE", "${q?.grade ?: "F"} ${ScorePct(q?.score)} • ${q?.strengths ?: 0} strengths / ${q?.weaknesses ?: 0} weaknesses")
            if (!q?.top_strength.isNullOrBlank()) MetricRow("TOP STRENGTH", q?.top_strength ?: "")
        }
    }
}

@Composable
private fun ValChip(label: String, ok: Boolean) {
    val color = if (ok) EmeraldSuccess else SlateText
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (ok) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}

// ── Entry & trade plan ────────────────────────────────────────────────────
@Composable
private fun EntryTradePlanBox(signal: ASCSignalData) {
    val entry = signal.entry
    val tp = signal.trade_params
    val pattern = signal.pattern_detection
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 120.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("ENTRY & TRADE PLAN")
            MetricRow("ENTRY", "${entry?.state ?: "NO_ENTRY"} • ${entry?.style ?: "NO_TRADE"} ${ScorePct(entry?.score)}")
            MetricRow("ENTRY CONF.", "${ScorePct(entry?.confidence)} • ${entry?.quality ?: "LOW"}")
            if (tp != null && (tp.stop_loss > 0 || tp.take_profit > 0)) {
                MetricRow("STOP LOSS", fmtPrice(tp.stop_loss))
                MetricRow("TAKE PROFIT", fmtPrice(tp.take_profit))
                val riskPct = if (tp.risk_pct in 0.0..1.0) tp.risk_pct * 100 else tp.risk_pct
                MetricRow("RISK", String.format(Locale.US, "%.2f%%", riskPct))
            }
            if (!pattern?.detected_pattern.isNullOrBlank()) {
                MetricRow("PATTERN", "${pattern?.detected_pattern} ${ScorePct(pattern?.pattern_confidence)}")
            }
            if (signal.zone_context_valid) {
                MetricRow("ZONE", "${signal.zone_context_type} • ${signal.target_zone}")
                MetricRow("ZONE DIST", String.format(Locale.US, "%.1f pips", signal.zone_distance_pips))
            }
            if (!entry?.reason.isNullOrBlank()) {
                ReasonLine(entry?.reason ?: "")
            }
        }
    }
}

@Composable
private fun ReasonLine(reason: String) {
    Text(pretty(reason), color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 14.sp, fontFamily = InterFontFamily)
}

// ── Context ───────────────────────────────────────────────────────────────
@Composable
private fun ContextBox(signal: ASCSignalData) {
    val session = signal.session
    val macro = signal.macro
    val htf = signal.htf_context
    val conf = signal.confluence
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), minHeight = 120.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("CONTEXT & CONFLUENCE")
            MetricRow("SESSION", "${session?.name ?: "OFF_HOURS"} ${ScorePct(session?.score)} ${if (session?.high_liquidity == true) "• HIGH LIQ" else ""}")
            MetricRow("MACRO", "${macro?.trend ?: "UNKNOWN"} ${ScorePct(macro?.score)} ${if (macro?.bullish_flag == true) "▲" else if (macro?.bearish_flag == true) "▼" else ""}")
            MetricRow("HTF BIAS", "${htf?.bias ?: "NEUTRAL"} ${ScorePct(htf?.score)} ${if (htf?.near_support == true) "• NEAR SUPPORT" else if (htf?.near_resistance == true) "• NEAR RESIST" else ""}")
            MetricRow("CONFLUENCE", "${conf?.state ?: "NO_CONFLUENCE"} ${ScorePct(conf?.score)}")
            if (!conf?.reason.isNullOrBlank()) ReasonLine(conf?.reason ?: "")
            if (!macro?.reason.isNullOrBlank()) ReasonLine("Macro: ${macro?.reason}")
        }
    }
}

// ── Shared UI ─────────────────────────────────────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(text, color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontFamily = InterFontFamily)
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(Modifier.width(12.dp))
        Text(
            pretty(value),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun QualityBadge(quality: String) {
    val color = when (quality.uppercase()) {
        "ELITE" -> Color(0xFFF59E0B)
        "STRONG" -> EmeraldSuccess
        "VALID", "GOOD" -> Color(0xFF60A5FA)
        "FILTERED", "LOW" -> RoseError
        else -> SlateText
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(pretty(quality), color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
}

@Composable
private fun NoSignalState(pair: ForexPair) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InfoBox(modifier = Modifier.fillMaxWidth(), minHeight = 140.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NO LIVE SETUP", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Spacer(Modifier.height(8.dp))
                Text("No EA signal captured yet for ${pair.symbol}.", color = SlateText.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = InterFontFamily)
            }
        }
    }
}

// ── Formatters ────────────────────────────────────────────────────────────
// EA sends most scores 0-1 (0.85 = 85%) but votes.win_pct is already 0-100.
private fun pct01(value: Double?): Int = (((value ?: 0.0) * 100).toInt()).coerceIn(0, 100)
private fun pctAny(value: Double?): Int {
    val v = value ?: 0.0
    return if (v > 1.0) v.toInt().coerceIn(0, 100) else (v * 100).toInt().coerceIn(0, 100)
}
// Strip underscores from every EA writeup so the UI never shows RAW_KEYS.
private fun pretty(raw: String?): String =
    (raw ?: "").replace("_", " ").replace("  ", " ").trim()
private fun ScorePct(value: Double?): String = if (value != null) "${pct01(value)}%" else ""
private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun fmtPrice(value: Double): String =
    if (value >= 1000) String.format(Locale.US, "%,.2f", value)
    else if (value >= 1) String.format(Locale.US, "%.3f", value)
    else String.format(Locale.US, "%.5f", value)
private fun formatSetupPrice(price: Double): String {
    val decimals = if (price >= 1000) 2 else if (price >= 10) 3 else 4
    return String.format(Locale.US, "%,.${decimals}f", price)
}
