package com.asc.markets.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ASCIndicatorsData
import com.asc.markets.data.ASCLiquidityData
import com.asc.markets.data.ASCSignalData
import com.asc.markets.data.EAAssetData
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.EATimeframeData
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.PriceStreamManager
import com.asc.markets.ui.components.AutoTradeStripForSymbol
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AssetDetailScreen(viewModel: ForexViewModel) {
    val pair by viewModel.selectedPair.collectAsState()
    val liveAssets by EALiveDataStore.liveAssets.collectAsState()
    val liveSignal by EASignalLiveStore.signal.collectAsState()
    val signalsByAsset by EASignalLiveStore.signalsByAsset.collectAsState()
    val signalConnected by EASignalLiveStore.isConnected.collectAsState()

    LaunchedEffect(pair.symbol) {
        EASignalLiveStore.requestSignal(pair.symbol)
        kotlinx.coroutines.delay(1200)
        if (signalsByAsset[normalizeSymbol(pair.symbol)] == null) {
            EASignalLiveStore.requestSignal(pair.symbol)
        }
    }

    val asset = remember(pair.symbol, liveAssets) {
        liveAssets.find { it.symbol == pair.symbol }
    }
    val targetNormalized = remember(pair.symbol) { normalizeSymbol(pair.symbol) }
    val matchingSignal = remember(liveSignal, signalsByAsset, targetNormalized) {
        signalsByAsset[targetNormalized]
            ?: liveSignal?.takeIf { normalizeSymbol(it.asset) == targetNormalized }
    }
    val otherSignalAsset = remember(liveSignal, matchingSignal) {
        val s = liveSignal
        if (s != null && matchingSignal == null) s.asset else ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(rememberScrollState())
    ) {
        DetailTopBar(
            pair = pair,
            live = signalConnected,
            onBack = { viewModel.onAssetDetailBack() }
        )

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PriceCard(pair = pair, asset = asset)

            AutoTradeStripForSymbol(pair.symbol)

            when {
                matchingSignal != null -> {
                    ChartPanelSection(matchingSignal)
                    ConfluenceSection(matchingSignal.confluence)
                    MacroSection(matchingSignal.macro)
                    HTFContextSection(matchingSignal.htf_context)
                    ValidationSection(matchingSignal.validation)
                    QualitySection(matchingSignal.quality)
                    AiWriteUpSection(matchingSignal)
                    ChartPanelsSection(matchingSignal)
                    VolatilitySection(matchingSignal.volatility)
                    LiquiditySection(matchingSignal.liquidity)
                    StructureSection(matchingSignal.structure)
                    IndicatorSection(matchingSignal.indicators)
                    SessionSection(matchingSignal.session)
                    EntrySection(matchingSignal.entry)
                    TradeParamsSection(matchingSignal.trade_params)
                    PatternSection(matchingSignal.pattern_detection)
                    ZoneSection(matchingSignal)
                    ExhaustionSection(matchingSignal)
                }
                otherSignalAsset.isNotBlank() -> {
                    NoticeCard(
                        title = "No cached write-up for ${pair.symbol} yet",
                        body = "Requested ${pair.symbol} from the bridge; the ASC EA cycles charts, so " +
                            "add ${pair.symbol} to an MT5 chart or wait a moment. Currently the live stream is on $otherSignalAsset. " +
                            "Prices/timeframes below are still live."
                    )
                }
                else -> {
                    NoticeCard(
                        title = "Waiting for live EA write-up...",
                        body = if (signalConnected) "Requested ${pair.symbol} — no write-up cached yet. Keep ${pair.symbol} on an MT5 EA chart."
                        else "Connecting to the ASC EA live bridge..."
                    )
                }
            }

            TimeframeCard(label = "M1", tf = asset?.m1)
            asset?.m5?.let { TimeframeCard(label = "M5", tf = it) }
            asset?.h1?.let { TimeframeCard(label = "H1", tf = it) }

            Text(
                text = "Write-ups pushed live from ASC EA on MT5" + if (signalConnected) "" else " · bridge offline",
                color = SlateText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
            )
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(pair: ForexPair, live: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Text(
                text = "\u2190",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = pair.symbol,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "ASC EA Intelligence",
                color = SlateText,
                fontSize = 11.sp
            )
        }
        StatusPill(
            label = if (live) "LIVE" else "OFFLINE",
            color = if (live) EmeraldSuccess else RoseError
        )
    }
}

// ─── Status Pill ─────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ─── Price Card (redesigned with gauges + mini bar chart) ────────────────────

@Composable
private fun PriceCard(pair: ForexPair, asset: EAAssetData?) {
    val pollPrice = asset?.prices?.last ?: pair.price
    val livePrice = PriceStreamManager.prices[pair.symbol]
        ?: PriceStreamManager.prices[pair.symbol.replace("/", "")]
        ?: MarketDataStore.pairSnapshot(pair.symbol)?.let { PriceStreamManager.prices[it.symbol] }
    val price = livePrice ?: pollPrice
    val changePercent = asset?.m1?.changePercent ?: pair.changePercent
    val positive = changePercent >= 0
    val bid = asset?.prices?.bid
    val ask = asset?.prices?.ask
    val spread = asset?.prices?.spread
    val spreadPct = asset?.prices?.spreadPercent

    InfoBox(minHeight = 180.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(pair.name.ifBlank { pair.symbol }, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(prettyPrice(price), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = (if (positive) EmeraldSuccess else RoseError).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = (if (positive) "+" else "") + String.format(Locale.US, "%.2f%%", changePercent),
                                color = if (positive) EmeraldSuccess else RoseError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                MiniTelemetryRing(
                    progress = (changePercent.coerceIn(-5.0, 5.0) + 5.0).toFloat() / 10f,
                    color = if (positive) EmeraldSuccess else RoseError,
                    modifier = Modifier.size(44.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile(
                    title = "BID",
                    value = asset?.prices?.let { prettyPrice(it.bid) } ?: "---",
                    caption = "buy",
                    meter = 0.5f,
                    color = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                IntelligenceTile(
                    title = "ASK",
                    value = asset?.prices?.let { prettyPrice(it.ask) } ?: "---",
                    caption = "sell",
                    meter = 0.5f,
                    color = RoseError,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile(
                    title = "SPREAD",
                    value = asset?.prices?.let { String.format(Locale.US, "%.2f", it.spread) } ?: "---",
                    caption = "pips",
                    meter = ((spread ?: 0.0) / 3.0).coerceIn(0.0, 1.0).toFloat(),
                    color = if ((spread ?: 0.0) < 1.5) EmeraldSuccess else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                IntelligenceTile(
                    title = "SPREAD %",
                    value = asset?.prices?.let { String.format(Locale.US, "%.2f%%", it.spreadPercent) } ?: "---",
                    caption = "of price",
                    meter = ((spreadPct ?: 0.0) / 0.05).coerceIn(0.0, 1.0).toFloat(),
                    color = if ((spreadPct ?: 0.0) < 0.03) EmeraldSuccess else Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Confidence Gauge (arc gauge) ────────────────────────────────────────────

@Composable
private fun ConfidenceGauge(value: Double, label: String, modifier: Modifier = Modifier) {
    val clamped = pctNumber(value).toFloat() / 100f
    val color = confColor(pctNumber(value))
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val diameter = minOf(size.width - stroke, size.height * 1.85f - stroke)
                val topLeft = Offset((size.width - diameter) / 2f, 6.dp.toPx())
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val angle = 180f + 180f * clamped
                val radians = Math.toRadians(angle.toDouble())
                val center = Offset(size.width / 2f, topLeft.y + diameter / 2f)
                val needleLength = diameter / 2f - 12.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(
                        center.x + cos(radians).toFloat() * needleLength,
                        center.y + sin(radians).toFloat() * needleLength
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
                Text(pct(value), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Mini Bar Chart ──────────────────────────────────────────────────────────

@Composable
private fun MiniBarChart(score: Float, color: Color, modifier: Modifier = Modifier) {
    val clamped = score.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val bars = 7
        val gap = 2.dp.toPx()
        val stroke = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(2.dp.toPx())
        repeat(bars) { index ->
            val x = index * (stroke + gap) + stroke / 2f
            val heightRatio = (0.28f + index * 0.11f).coerceIn(0.25f, 1f)
            val active = (index + 1).toFloat() / bars <= clamped + 0.06f
            val barHeight = size.height * heightRatio
            drawLine(
                color = if (active) color else Color.White.copy(alpha = 0.10f),
                start = Offset(x, size.height),
                end = Offset(x, size.height - barHeight),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

// ─── Mini Telemetry Ring ─────────────────────────────────────────────────────

@Composable
private fun MiniTelemetryRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val stroke = 4.dp.toPx()
        drawArc(
            color = Color.White.copy(alpha = 0.10f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * clamped,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

// ─── Intelligence Tile ───────────────────────────────────────────────────────

@Composable
private fun IntelligenceTile(
    title: String,
    value: String,
    caption: String,
    meter: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
            }
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(meter.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

// ─── Scale Bar ───────────────────────────────────────────────────────────────

@Composable
private fun ScaleBar(label: String, value: Float, left: String, right: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(pct(value * 100.0), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(left, color = SlateText, fontSize = 9.sp)
            Text(right, color = SlateText, fontSize = 9.sp)
        }
    }
}

// ─── Gate Tile (for validation) ──────────────────────────────────────────────

@Composable
private fun GateTile(title: String, state: String, passed: Boolean, modifier: Modifier = Modifier) {
    val color = if (passed) EmeraldSuccess else RoseError
    val meter = if (passed) 0.96f else 0.16f
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(state, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (passed) "Gate Passed" else "Gate Blocking", color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(meter)
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

// ─── Expandable Section Card ─────────────────────────────────────────────────

@Composable
private fun ExpandableSectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color = IndigoAccent,
    content: @Composable ColumnScope.() -> Unit
) {
    InfoBox(minHeight = if (expanded) null else 52.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = if (expanded) "COLLAPSE" else "EXPAND",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(color = accentColor.copy(alpha = 0.18f), thickness = 1.dp)
                content()
            }
        }
    }
}

// ─── EA write-up sections ────────────────────────────────────────────────────

@Composable
private fun AiWriteUpSection(signal: ASCSignalData) {
    var expanded by remember { mutableStateOf(true) }
    val direction = signal.direction.uppercase(Locale.US)
    val directionColor = when (direction) {
        "BUY", "LONG" -> EmeraldSuccess
        "SELL", "SHORT" -> RoseError
        else -> IndigoAccent
    }
    val regime = signal.regime

    ExpandableSectionCard(title = "AI WRITE-UP", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "DIRECTION",
                value = direction,
                caption = "signal",
                meter = 0.5f,
                color = directionColor,
                modifier = Modifier.weight(1f)
            )
            ConfidenceGauge(
                value = signal.confidence,
                label = "CONFIDENCE",
                modifier = Modifier.weight(1f)
            )
        }
        IntelligenceTile(
            title = "MTF ALIGNMENT",
            value = pct(signal.alignment_percentage),
            caption = "multi-timeframe",
            meter = (signal.alignment_percentage / 100.0).coerceIn(0.0, 1.0).toFloat(),
            color = alignColor(signal.alignment_percentage),
            modifier = Modifier.fillMaxWidth()
        )
        if (regime != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile(
                    title = "REGIME",
                    value = regime.state,
                    caption = "trend: ${regime.trend}",
                    meter = (pctNumber(regime.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                    color = confColor(pctNumber(regime.score)),
                    modifier = Modifier.weight(1f)
                )
                IntelligenceTile(
                    title = "REGIME CONF",
                    value = regime.confidence,
                    caption = "score ${pct(regime.score)}",
                    meter = (pctNumber(regime.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                    color = confColor(pctNumber(regime.score)),
                    modifier = Modifier.weight(1f)
                )
            }
            if (regime.reason.isNotBlank()) {
                Text("REASON: ${regime.reason}", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
    }
}

// ─── Chart Panel Section ─────────────────────────────────────────────────────

@Composable
private fun ChartPanelSection(signal: ASCSignalData) {
    var expanded by remember { mutableStateOf(true) }
    val panel = signal.chart_panel ?: return
    val tier = panel.quality_tier.uppercase(Locale.US)
    val tierColor = when (tier) {
        "ELITE" -> EmeraldSuccess
        "FILTERED" -> RoseError
        else -> Color.White
    }
    val votes = panel.votes
    val c = signal.confidence

    ExpandableSectionCard(title = "CHART PANEL", expanded = expanded, onToggle = { expanded = !expanded }) {
        if (panel.validator_active) {
            val vColor = if (panel.validator_allowed) EmeraldSuccess else RoseError
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GateTile(
                    title = "VALIDATOR",
                    state = panel.validator_direction,
                    passed = panel.validator_allowed,
                    modifier = Modifier.weight(1f)
                )
                IntelligenceTile(
                    title = "P(WIN)",
                    value = String.format(Locale.US, "%.1f%%", pctNumber(panel.validator_pwin)),
                    caption = "gate ${String.format(Locale.US, "%.0f%%", pctNumber(panel.validator_gate))}",
                    meter = (pctNumber(panel.validator_pwin) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                    color = vColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        IntelligenceTile(
            title = "QUALITY TIER",
            value = tier,
            caption = "signal quality",
            meter = when (tier) { "ELITE" -> 0.95f; "STRONG" -> 0.78f; "VALID" -> 0.55f; "FILTERED" -> 0.25f; else -> 0.5f },
            color = tierColor,
            modifier = Modifier.fillMaxWidth()
        )
        if (votes != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile(
                    title = "EA (SMC)",
                    value = "\u25b2${votes.smc_bull} / \u25bc${votes.smc_bear}",
                    caption = "votes",
                    meter = (votes.smc_bull.toFloat() / (votes.smc_bull + votes.smc_bear).coerceAtLeast(1)),
                    color = voteColor(votes.smc_bull, votes.smc_bear),
                    modifier = Modifier.weight(1f)
                )
                IntelligenceTile(
                    title = "AI",
                    value = "\u25b2${votes.ai_bull} / \u25bc${votes.ai_bear}",
                    caption = "votes",
                    meter = (votes.ai_bull.toFloat() / (votes.ai_bull + votes.ai_bear).coerceAtLeast(1)),
                    color = voteColor(votes.ai_bull, votes.ai_bear),
                    modifier = Modifier.weight(1f)
                )
            }
            IntelligenceTile(
                title = "TOTAL VOTES",
                value = "\u25b2${votes.total_bull} / \u25bc${votes.total_bear} (${String.format(Locale.US, "%.0f%%", votes.win_pct)})",
                caption = votes.direction,
                meter = (votes.win_pct / 100.0).toFloat(),
                color = when (votes.direction) { "BUY" -> EmeraldSuccess; "SELL" -> RoseError; else -> SlateText },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniBarChart(score = c.toFloat(), color = IndigoAccent, modifier = Modifier.width(72.dp).height(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CONFIDENCE BREAKDOWN", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    "T ${String.format(Locale.US, "%.0f%%", c * 0.9 * 100)} · S ${String.format(Locale.US, "%.0f%%", c * 0.85 * 100)} · L ${String.format(Locale.US, "%.0f%%", c * 0.8 * 100)}",
                    color = Color.White, fontSize = 10.sp, maxLines = 1
                )
            }
        }
        panel.confidence_trend?.let { trend ->
            val (arrow, trendColor) = when (trend.indicator) {
                "RISING" -> "\u2191" to EmeraldSuccess
                "FALLING" -> "\u2193" to RoseError
                "SLIGHT_UP" -> "\u2197" to EmeraldSuccess
                "SLIGHT_DOWN" -> "\u2198" to Color(0xFFF59E0B)
                else -> "\u2192" to Color(0xFFF59E0B)
            }
            val signed = if (trend.indicator == "FALLING") -trend.strength else trend.strength
            IntelligenceTile(
                title = "CONFIDENCE TREND",
                value = "$arrow ${trend.indicator.replace("_", " ")}",
                caption = "${String.format(Locale.US, "%+.1f%%", signed)}",
                meter = (trend.strength / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = trendColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (c >= 0.80) {
            StatusPill(label = "OVERRIDE MODE ACTIVE", color = EmeraldSuccess)
        }
    }
}

private fun voteColor(bull: Int, bear: Int): Color =
    when {
        bull > bear -> EmeraldSuccess
        bear > bull -> RoseError
        else -> SlateText
    }

// ─── Validation Section (gate grid) ──────────────────────────────────────────

@Composable
private fun ValidationSection(v: com.asc.markets.data.ASCValidationData?) {
    var expanded by remember { mutableStateOf(true) }
    if (v == null) return
    val passed = listOf(v.spread_ok, v.timing_ok, v.rr_ok, v.market_ok).count { it }
    val stateColor = when (v.state) {
        "PASSED" -> EmeraldSuccess
        "CONDITIONAL_PASS" -> Color(0xFFF59E0B)
        "REJECTED" -> RoseError
        else -> SlateText
    }

    ExpandableSectionCard(title = "VALIDATION", expanded = expanded, onToggle = { expanded = !expanded }, accentColor = stateColor) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "STATE",
                value = v.state,
                caption = "score ${pct(v.score)}",
                meter = (pctNumber(v.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = stateColor,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "CHECKS",
                value = "$passed/4",
                caption = "gates",
                meter = passed / 4f,
                color = if (passed == 4) EmeraldSuccess else Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GateTile(title = "SPREAD", state = if (v.spread_ok) "PASS" else "FAIL", passed = v.spread_ok, modifier = Modifier.weight(1f))
            GateTile(title = "TIMING", state = if (v.timing_ok) "PASS" else "FAIL", passed = v.timing_ok, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GateTile(title = "R:R", state = if (v.rr_ok) "PASS" else "FAIL", passed = v.rr_ok, modifier = Modifier.weight(1f))
            GateTile(title = "MARKET", state = if (v.market_ok) "PASS" else "FAIL", passed = v.market_ok, modifier = Modifier.weight(1f))
        }
    }
}

// ─── Quality Section ─────────────────────────────────────────────────────────

@Composable
private fun QualitySection(q: com.asc.markets.data.ASCQualityData?) {
    var expanded by remember { mutableStateOf(true) }
    if (q == null) return
    val gradeColor = when {
        q.grade.startsWith("A") -> EmeraldSuccess
        q.grade.startsWith("B") -> EmeraldSuccess
        q.grade.startsWith("C") -> Color(0xFFF59E0B)
        else -> RoseError
    }

    ExpandableSectionCard(title = "QUALITY", expanded = expanded, onToggle = { expanded = !expanded }, accentColor = gradeColor) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "GRADE",
                value = q.grade,
                caption = "quality score",
                meter = (pctNumber(q.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = gradeColor,
                modifier = Modifier.weight(1f)
            )
            ConfidenceGauge(value = q.confidence, label = "CONFIDENCE", modifier = Modifier.weight(1f))
        }
        IntelligenceTile(
            title = "FEEDERS",
            value = "\u2713${q.strengths} \u2717${q.weaknesses} (of ${q.total_feeders})",
            caption = "strengths vs weaknesses",
            meter = (q.strengths.toFloat() / q.total_feeders.coerceAtLeast(1)),
            color = gradeColor,
            modifier = Modifier.fillMaxWidth()
        )
        if (q.top_strength.isNotBlank()) {
            Surface(color = EmeraldSuccess.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u2713", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(q.top_strength, color = Color.White, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }
        if (q.top_weakness.isNotBlank()) {
            Surface(color = RoseError.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, RoseError.copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u2717", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(q.top_weakness, color = Color.White, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

// ─── Confluence Section ──────────────────────────────────────────────────────

@Composable
private fun ConfluenceSection(c: com.asc.markets.data.ASCConfluenceData?) {
    var expanded by remember { mutableStateOf(false) }
    if (c == null) return
    val stateColor = when (c.state) {
        "STRONG_BULLISH" -> EmeraldSuccess
        "STRONG_BEARISH" -> RoseError
        else -> SlateText
    }

    ExpandableSectionCard(title = "CONFLUENCE", expanded = expanded, onToggle = { expanded = !expanded }, accentColor = stateColor) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "STATE",
                value = c.state,
                caption = "confluence state",
                meter = (pctNumber(c.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = stateColor,
                modifier = Modifier.weight(1f)
            )
            ConfidenceGauge(value = c.confidence, label = "CONFIDENCE", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "BULL",
                value = c.bull_confluences.toString(),
                caption = "confluences",
                meter = c.bull_confluences.toFloat() / (c.bull_confluences + c.bear_confluences).coerceAtLeast(1),
                color = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "BEAR",
                value = c.bear_confluences.toString(),
                caption = "confluences",
                meter = c.bear_confluences.toFloat() / (c.bull_confluences + c.bear_confluences).coerceAtLeast(1),
                color = RoseError,
                modifier = Modifier.weight(1f)
            )
        }
        if (c.reason.isNotBlank()) {
            Text("REASON: ${c.reason}", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

// ─── Macro Section ───────────────────────────────────────────────────────────

@Composable
private fun MacroSection(m: com.asc.markets.data.ASCMacroData?) {
    var expanded by remember { mutableStateOf(false) }
    if (m == null) return
    val trendColor = when (m.trend) {
        "BULLISH" -> EmeraldSuccess
        "BEARISH" -> RoseError
        else -> SlateText
    }

    ExpandableSectionCard(title = "MACRO", expanded = expanded, onToggle = { expanded = !expanded }, accentColor = trendColor) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "TREND",
                value = m.trend,
                caption = "macro direction",
                meter = (pctNumber(m.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = trendColor,
                modifier = Modifier.weight(1f)
            )
            ConfidenceGauge(value = m.confidence, label = "CONFIDENCE", modifier = Modifier.weight(1f))
        }
        ScaleBar("D1 STRENGTH", (pctNumber(m.d1_trend_strength) / 100.0).toFloat(), "WEAK", "STRONG", confColor(pctNumber(m.d1_trend_strength)))
        ScaleBar("W1 STRENGTH", (pctNumber(m.w1_trend_strength) / 100.0).toFloat(), "WEAK", "STRONG", confColor(pctNumber(m.w1_trend_strength)))
        if (m.reason.isNotBlank()) {
            Text("REASON: ${m.reason}", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

// ─── HTF Context Section ─────────────────────────────────────────────────────

@Composable
private fun HTFContextSection(h: com.asc.markets.data.ASCHTFContextData?) {
    var expanded by remember { mutableStateOf(false) }
    if (h == null) return
    val ctxColor = when (h.context) {
        "AT_RESISTANCE" -> RoseError
        "AT_SUPPORT" -> EmeraldSuccess
        else -> SlateText
    }

    ExpandableSectionCard(title = "HTF CONTEXT", expanded = expanded, onToggle = { expanded = !expanded }, accentColor = ctxColor) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "CONTEXT",
                value = h.context,
                caption = "bias: ${h.bias}",
                meter = (pctNumber(h.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = ctxColor,
                modifier = Modifier.weight(1f)
            )
            ConfidenceGauge(value = h.confidence, label = "CONFIDENCE", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "SUPPORT",
                value = h.support.toString(),
                caption = if (h.near_support) "NEAR" else "far",
                meter = if (h.near_support) 0.9f else 0.3f,
                color = if (h.near_support) EmeraldSuccess else SlateText,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "RESISTANCE",
                value = h.resistance.toString(),
                caption = if (h.near_resistance) "NEAR" else "far",
                meter = if (h.near_resistance) 0.9f else 0.3f,
                color = if (h.near_resistance) RoseError else SlateText,
                modifier = Modifier.weight(1f)
            )
        }
        if (h.reason.isNotBlank()) {
            Text("REASON: ${h.reason}", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

// ─── Chart Panels Section ────────────────────────────────────────────────────

@Composable
private fun ChartPanelsSection(signal: ASCSignalData) {
    var expanded by remember { mutableStateOf(false) }
    val p = signal.chart_panels ?: return

    ExpandableSectionCard(title = "CHART PANELS", expanded = expanded, onToggle = { expanded = !expanded }) {
        val panels = listOf(
            "NEWS" to p.news_sentiment,
            "MARKET PHASE" to p.market_phase,
            "SMC DETAILS" to p.smc_details,
            "MOMENTUM" to p.momentum,
            "STRUCTURE" to p.market_structure,
            "TIME FILTER" to p.time_filter,
            "WIN PROB" to p.win_probability,
            "RISK:REWARD" to p.risk_reward,
            "DAILY PNL" to p.daily_pnl,
            "SESSION" to p.session_details,
            "SPREAD MON" to p.spread_monitor,
            "CORRELATION" to p.correlation_alert,
            "CROSS CORR" to p.cross_correlation,
            "SESSION INT" to p.session_intelligence,
            "PORTFOLIO" to p.portfolio_management,
            "CRITERIA" to p.criteria_score,
            "EXHAUSTION" to p.exhaustion_analysis,
            "ZONE" to p.zone_context,
            "EQUITY" to p.equity_info,
            "RISK MGMT" to p.risk_management,
            "SMC STATUS" to p.smc_status,
            "MODULE REL" to p.module_reliability
        )
        panels.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, text) ->
                    if (text.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.035f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(label, color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(text, color = SlateText, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─── Volatility Section ──────────────────────────────────────────────────────

@Composable
private fun VolatilitySection(v: com.asc.markets.data.ASCVolatilityData?) {
    var expanded by remember { mutableStateOf(false) }
    if (v == null) return

    ExpandableSectionCard(title = "VOLATILITY", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "STATE",
                value = v.state,
                caption = "volatility regime",
                meter = (pctNumber(v.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = confColor(pctNumber(v.score)),
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "ATR RATIO",
                value = String.format(Locale.US, "%.2f", v.atr_ratio),
                caption = "bias: ${v.bias}",
                meter = (v.atr_ratio / 3.0).coerceIn(0.0, 1.0).toFloat(),
                color = IndigoAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Liquidity Section ───────────────────────────────────────────────────────

@Composable
private fun LiquiditySection(l: ASCLiquidityData?) {
    var expanded by remember { mutableStateOf(false) }
    if (l == null) return

    ExpandableSectionCard(title = "LIQUIDITY", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "STATE",
                value = l.state,
                caption = "bias: ${l.bias}",
                meter = (pctNumber(l.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = confColor(pctNumber(l.score)),
                modifier = Modifier.weight(1f)
            )
        }
        val flags = buildList {
            if (l.fvg_bull) add("FVG BULL" to EmeraldSuccess)
            if (l.fvg_bear) add("FVG BEAR" to RoseError)
            if (l.sweep_high) add("SWEEP HIGH" to RoseError)
            if (l.sweep_low) add("SWEEP LOW" to EmeraldSuccess)
            if (l.bos_bull) add("BOS BULL" to EmeraldSuccess)
            if (l.bos_bear) add("BOS BEAR" to RoseError)
        }
        if (flags.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                flags.forEach { (label, color) ->
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
                    ) {
                        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// ─── Structure Section ───────────────────────────────────────────────────────

@Composable
private fun StructureSection(s: com.asc.markets.data.ASCStructureData?) {
    var expanded by remember { mutableStateOf(false) }
    if (s == null) return

    ExpandableSectionCard(title = "STRUCTURE", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "BIAS",
                value = s.bias,
                caption = "structure",
                meter = (pctNumber(s.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = when (s.bias) { "BULLISH" -> EmeraldSuccess; "BEARISH" -> RoseError; else -> SlateText },
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "QUALITY",
                value = pct(s.quality),
                caption = "structure quality",
                meter = (pctNumber(s.quality) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = confColor(pctNumber(s.quality)),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Indicator Section ───────────────────────────────────────────────────────

@Composable
private fun IndicatorSection(i: ASCIndicatorsData?) {
    var expanded by remember { mutableStateOf(false) }
    if (i == null) return

    ExpandableSectionCard(title = "INDICATORS", expanded = expanded, onToggle = { expanded = !expanded }) {
        IntelligenceTile(
            title = "BIAS",
            value = i.bias,
            caption = "indicator bias",
            meter = (pctNumber(i.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
            color = when (i.bias) { "BULLISH" -> EmeraldSuccess; "BEARISH" -> RoseError; else -> SlateText },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "RSI",
                value = String.format(Locale.US, "%.1f", i.rsi),
                caption = "momentum",
                meter = (i.rsi / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = if (i.rsi > 70) RoseError else if (i.rsi < 30) EmeraldSuccess else SlateText,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "STOCH",
                value = String.format(Locale.US, "%.1f", i.stochastic),
                caption = "oscillator",
                meter = (i.stochastic / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = if (i.stochastic > 80) RoseError else if (i.stochastic < 20) EmeraldSuccess else SlateText,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "MACD",
                value = String.format(Locale.US, "%.3f", i.macd_main),
                caption = "trend",
                meter = ((i.macd_main * 100 + 50) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = if (i.macd_main > i.macd_signal) EmeraldSuccess else RoseError,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "ADX",
                value = String.format(Locale.US, "%.1f", i.adx),
                caption = "strength",
                meter = (i.adx / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = if (i.adx > 25) EmeraldSuccess else SlateText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Session Section ─────────────────────────────────────────────────────────

@Composable
private fun SessionSection(s: com.asc.markets.data.ASCSessionData?) {
    var expanded by remember { mutableStateOf(false) }
    if (s == null) return

    ExpandableSectionCard(title = "SESSION", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "SESSION",
                value = s.name,
                caption = "trading session",
                meter = (pctNumber(s.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = confColor(pctNumber(s.score)),
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "LIQUIDITY",
                value = if (s.high_liquidity) "HIGH" else "LOW",
                caption = "depth",
                meter = if (s.high_liquidity) 0.85f else 0.3f,
                color = if (s.high_liquidity) EmeraldSuccess else SlateText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Entry Section ───────────────────────────────────────────────────────────

@Composable
private fun EntrySection(e: com.asc.markets.data.ASCEntryData?) {
    var expanded by remember { mutableStateOf(false) }
    if (e == null) return

    ExpandableSectionCard(title = "ENTRY", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "STATE",
                value = e.state,
                caption = "style: ${e.style}",
                meter = (pctNumber(e.score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = confColor(pctNumber(e.score)),
                modifier = Modifier.weight(1f)
            )
            ConfidenceGauge(value = e.confidence, label = "CONFIDENCE", modifier = Modifier.weight(1f))
        }
        IntelligenceTile(
            title = "QUALITY",
            value = e.quality,
            caption = "entry quality",
            meter = 0.5f,
            color = IndigoAccent,
            modifier = Modifier.fillMaxWidth()
        )
        if (e.reason.isNotBlank()) {
            Text("REASON: ${e.reason}", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

// ─── Trade Params Section ────────────────────────────────────────────────────

@Composable
private fun TradeParamsSection(t: com.asc.markets.data.ASCTradeParams?) {
    var expanded by remember { mutableStateOf(true) }
    if (t == null || (t.stop_loss == 0.0 && t.take_profit == 0.0 && t.risk_pct == 0.0)) return

    ExpandableSectionCard(title = "TRADE PARAMS", expanded = expanded, onToggle = { expanded = !expanded }, accentColor = Color(0xFFF59E0B)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "STOP LOSS",
                value = prettyNum(t.stop_loss),
                caption = "invalidation",
                meter = 0.4f,
                color = RoseError,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "TAKE PROFIT",
                value = prettyNum(t.take_profit),
                caption = "target",
                meter = 0.8f,
                color = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }
        IntelligenceTile(
            title = "RISK %",
            value = String.format(Locale.US, "%.1f%%", t.risk_pct),
            caption = "position risk",
            meter = (t.risk_pct / 5.0).coerceIn(0.0, 1.0).toFloat(),
            color = if (t.risk_pct <= 2.0) EmeraldSuccess else Color(0xFFF59E0B),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Pattern Section ─────────────────────────────────────────────────────────

@Composable
private fun PatternSection(p: com.asc.markets.data.ASCPatternDetection?) {
    var expanded by remember { mutableStateOf(false) }
    if (p == null || p.detected_pattern.isBlank()) return

    ExpandableSectionCard(title = "PATTERN DETECTION", expanded = expanded, onToggle = { expanded = !expanded }) {
        IntelligenceTile(
            title = "PATTERN",
            value = p.detected_pattern,
            caption = "detected",
            meter = (pctNumber(p.pattern_confidence) / 100.0).coerceIn(0.0, 1.0).toFloat(),
            color = confColor(pctNumber(p.pattern_confidence)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Zone Section ────────────────────────────────────────────────────────────

@Composable
private fun ZoneSection(signal: ASCSignalData) {
    var expanded by remember { mutableStateOf(false) }
    val hasData = signal.zone_context_type.isNotBlank() || signal.current_zones.isNotBlank() || signal.target_zone.isNotBlank()
    if (!hasData && !signal.zone_context_valid) return

    ExpandableSectionCard(title = "ZONE CONTEXT", expanded = expanded, onToggle = { expanded = !expanded }) {
        if (signal.zone_context_valid) {
            StatusPill(label = "ZONE VALID", color = EmeraldSuccess)
        }
        if (signal.zone_context_type.isNotBlank()) {
            IntelligenceTile(
                title = "TYPE",
                value = signal.zone_context_type,
                caption = signal.zone_relationship,
                meter = 0.5f,
                color = IndigoAccent,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (signal.current_zones.isNotBlank()) {
            Text("CURRENT ZONES: ${signal.current_zones}", color = SlateText, fontSize = 10.sp, lineHeight = 15.sp)
        }
        if (signal.target_zone.isNotBlank()) {
            Text("TARGET: ${signal.target_zone}", color = IndigoAccent, fontSize = 10.sp, lineHeight = 15.sp)
        }
        if (signal.zone_distance_pips > 0) {
            Text("DISTANCE: ${String.format(Locale.US, "%.1f", signal.zone_distance_pips)} pips", color = SlateText, fontSize = 10.sp)
        }
    }
}

// ─── Exhaustion Section ──────────────────────────────────────────────────────

@Composable
private fun ExhaustionSection(signal: ASCSignalData) {
    var expanded by remember { mutableStateOf(false) }

    ExpandableSectionCard(title = "EXHAUSTION", expanded = expanded, onToggle = { expanded = !expanded }) {
        IntelligenceTile(
            title = "DETECTED",
            value = if (signal.exhaustion_detected) "YES" else "NO",
            caption = signal.exhaustion_bias,
            meter = (pctNumber(signal.exhaustion_score) / 100.0).coerceIn(0.0, 1.0).toFloat(),
            color = if (signal.exhaustion_detected) RoseError else EmeraldSuccess,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Notice Card ─────────────────────────────────────────────────────────────

@Composable
private fun NoticeCard(title: String, body: String) {
    InfoBox(minHeight = 80.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = body, color = SlateText, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

// ─── Timeframe Card ──────────────────────────────────────────────────────────

@Composable
private fun TimeframeCard(label: String, tf: EATimeframeData?) {
    var expanded by remember { mutableStateOf(false) }
    if (tf == null) return
    val decimals = if (tf.close >= 1000) 2 else if (tf.close >= 10) 3 else 4
    val change = tf.changePercent
    val positive = (change ?: 0.0) >= 0

    ExpandableSectionCard(title = "TIMEFRAME \u00b7 $label", expanded = expanded, onToggle = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "OPEN",
                value = String.format(Locale.US, "%,.${decimals}f", tf.open),
                caption = "price",
                meter = 0.5f,
                color = SlateText,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "HIGH",
                value = String.format(Locale.US, "%,.${decimals}f", tf.high),
                caption = "peak",
                meter = 0.9f,
                color = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "LOW",
                value = String.format(Locale.US, "%,.${decimals}f", tf.low),
                caption = "trough",
                meter = 0.2f,
                color = RoseError,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "CLOSE",
                value = String.format(Locale.US, "%,.${decimals}f", tf.close),
                caption = "current",
                meter = 0.5f,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntelligenceTile(
                title = "CHANGE",
                value = if (change != null) (if (positive) "+" else "") + String.format(Locale.US, "%.2f%%", change) else "---",
                caption = "period",
                meter = ((change ?: 0.0) / 5.0).coerceIn(-1.0, 1.0).toFloat() * 0.5f + 0.5f,
                color = if (change == null) SlateText else if (positive) EmeraldSuccess else RoseError,
                modifier = Modifier.weight(1f)
            )
            IntelligenceTile(
                title = "VOLUME",
                value = "%,d".format(Locale.US, tf.volume),
                caption = "ticks",
                meter = (tf.volume / 10000.0).coerceIn(0.0, 1.0).toFloat(),
                color = IndigoAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── helpers ─────────────────────────────────────────────────────────────────

private fun normalizeSymbol(symbol: String): String = symbol
    .uppercase(Locale.US)
    .replace("/", "")
    .replace("-", "")
    .replace("_", "")
    .replace(" ", "")
    .replace(".", "")
    .removeSuffix("M")

private fun pctNumber(value: Double?): Double {
    if (value == null) return 0.0
    return if (value <= 1.0) value * 100 else value
}

private fun pct(value: Double?): String {
    if (value == null) return "---"
    return String.format(Locale.US, "%.0f%%", pctNumber(value))
}

private fun confColor(value: Double): Color =
    if (value >= 70) EmeraldSuccess else if (value >= 50) Color(0xFFF59E0B) else SlateText

private fun alignColor(value: Double): Color =
    if (value >= 70) EmeraldSuccess else if (value >= 50) Color(0xFFF59E0B) else RoseError

private fun prettyPrice(price: Double): String {
    val decimals = if (price >= 1000) 2 else if (price >= 10) 3 else 4
    return String.format(Locale.US, "%,.${decimals}f", price)
}

private fun prettyNum(value: Double): String {
    if (value == 0.0) return "---"
    val decimals = when {
        abs(value) >= 1000 -> 2
        abs(value) >= 10 -> 3
        abs(value) >= 1 -> 4
        else -> 5
    }
    return String.format(Locale.US, "%,.${decimals}f", value)
}
