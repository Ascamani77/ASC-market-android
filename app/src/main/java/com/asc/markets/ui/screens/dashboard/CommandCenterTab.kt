package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.asc.markets.data.AuditRecord
import com.asc.markets.data.ImpactPriority
import com.asc.markets.data.MacroEvent
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.InfoBox

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

import com.asc.markets.ui.screens.dashboard.CurrencyStrengthPanel

@Composable
fun CommandCenterTab(viewModel: ForexViewModel) {
    val macroEvents by viewModel.macroStreamEvents.collectAsState()
    val isArmed by viewModel.isArmed.collectAsState()
    val status by viewModel.commandCenterStatus.collectAsState()
    val auditRecords by viewModel.auditRecords.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()


    val deployments by viewModel.aiDeployments.collectAsState()
    val allSignals = deployments?.final_decision ?: emptyList()
    val signals = allSignals
    val eaAssetsSnap by com.asc.markets.data.EALiveDataStore.liveAssets.collectAsState()
    val eaBySym = remember(eaAssetsSnap) { eaAssetsSnap.associateBy { it.symbol.uppercase() } }
    fun isEaVetoed(s: com.asc.markets.data.remote.FinalDecisionItem): Boolean {
        val ea = eaBySym[s.asset_1?.uppercase() ?: ""] ?: return false
        val dir = ea.eaAi?.direction ?: "WAIT"
        val conf = ea.eaAi?.confidence ?: 0.0
        return dir.equals("WAIT", true) || conf < 0.25
    }
    fun isRejectedAi(s: com.asc.markets.data.remote.FinalDecisionItem) = (s.final_trade_state ?: "").equals("REJECTED", true)
    val primaryCount = allSignals.count { ((it.portfolio_deployment_bucket ?: "").equals("PRIMARY", true) || (it.portfolio_decision_label ?: "").contains("PRIMARY", true)) && !isEaVetoed(it) && !isRejectedAi(it) }
    val secondaryCount = allSignals.count { ((it.portfolio_deployment_bucket ?: "").equals("SECONDARY", true) || (it.portfolio_decision_label ?: "").contains("SECONDARY", true)) && !isEaVetoed(it) && !isRejectedAi(it) }
    val rejectedCount = allSignals.count { isRejectedAi(it) || isEaVetoed(it) }
    val longCount = allSignals.count { (it.journal_direction ?: "").equals("LONG", true) || (it.journal_direction ?: "").equals("BUY", true) }
    val shortCount = allSignals.count { (it.journal_direction ?: "").equals("SHORT", true) || (it.journal_direction ?: "").equals("SELL", true) }
    // Live fallback: the deployments backend (localhost:8003) doesn't exist on the
    // phone, so when it yields nothing, derive the same headline numbers from the
    // live EA write-ups + MT5 scanner feed instead of showing zeros.
    val eaWriteupsHome by com.asc.markets.data.EASignalLiveStore.signalsByAsset.collectAsState()
    val scannerHome by com.asc.markets.data.ScannerSignalsStore.signals.collectAsState()
    val liveInfos = remember(eaWriteupsHome, scannerHome) {
        buildLiveSignalInfos(eaWriteupsHome, scannerHome)
    }
    val useLiveCounts = allSignals.isEmpty() && liveInfos.isNotEmpty()
    val dispTotal = if (useLiveCounts) liveInfos.size else signals.size
    val dispPrimary = if (useLiveCounts) liveInfos.count { liveBucketOf(it) == 'P' } else primaryCount
    val dispSecondary = if (useLiveCounts) liveInfos.count { liveBucketOf(it) == 'S' } else secondaryCount
    val dispRejected = if (useLiveCounts) liveInfos.count { liveBucketOf(it) == 'R' } else rejectedCount
    val dispLong = if (useLiveCounts) liveInfos.count { isLiveLong(it.direction) } else longCount
    val dispShort = if (useLiveCounts) liveInfos.count { isLiveShort(it.direction) } else shortCount
    val dispNext = if (useLiveCounts) liveInfos.firstOrNull()?.asset ?: "—" else allSignals.maxByOrNull { it.journal_score ?: 0.0 }?.asset_1 ?: "—"
    // Hoisted upcoming feed: the same calendar-backed list the Upcoming Events box
    // shows, so the SYSTEM TELEMETRY cards can count it too instead of zeros.
    val calPayloadHome by com.trading.app.data.CalendarSnapshotStore.latestDisplayPayloadFlow.collectAsState()
    val eaConnectedHome by com.asc.markets.data.EALiveDataStore.isConnected.collectAsState()
    val upcomingHome = remember(macroEvents, calPayloadHome) {
        if (macroEvents.isNotEmpty()) macroEvents.take(5)
        else {
            val cal = calPayloadHome?.events ?: emptyList()
            val now = System.currentTimeMillis()
            val parsed = cal.map { ev ->
                val millis = try {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(ev.isoDateTime.take(19))?.time ?: 0L
                } catch (_: Exception) { 0L }
                millis to ev
            }.sortedBy { it.first }
            val upcoming = parsed.filter { it.first >= now - 3600_000L }.take(5)
            (if (upcoming.isNotEmpty()) upcoming else parsed.take(5)).map { (millis, ev) ->
                com.asc.markets.data.MacroEvent(
                    title = ev.title,
                    currency = ev.currencyCode.ifBlank { ev.countryCode },
                    datetimeUtc = if (millis > 0L) millis else now,
                    priority = when (ev.importance.uppercase()) { "HIGH", "HOLIDAY" -> com.asc.markets.data.ImpactPriority.CRITICAL; "MEDIUM" -> com.asc.markets.data.ImpactPriority.HIGH; else -> com.asc.markets.data.ImpactPriority.MEDIUM },
                    status = com.asc.markets.data.MacroEventStatus.UPCOMING,
                    source = "Calendar",
                    actual = ev.actual,
                    forecast = ev.forecast,
                    previous = ev.previous
                )
            }
        }
    }
    // The box is live when EITHER the command feed or the calendar snapshot
    // backing it has data — not just when the command socket is up.
    val feedLive = status.isConnected == true || calPayloadHome != null || macroEvents.isNotEmpty()
    val topSignals = allSignals.sortedByDescending { it.journal_score ?: 0.0 }.take(5)
    val leadSignal: FinalDecisionItem? = allSignals.maxByOrNull { it.journal_score ?: 0.0 }
    
    val listState = rememberLazyListState()

    // Collapse the Home/Signals/AI tabs + ASC MARKET header as this list scrolls.
    // Quantized to ~5% steps so the shared header only recomposes ~20 times per
    // full collapse instead of on every scroll frame (keeps scrolling buttery).
    val collapseRange = 220f
    val collapseProgress by remember {
        derivedStateOf {
            val absoluteScroll = (listState.firstVisibleItemIndex * 100f) + listState.firstVisibleItemScrollOffset
            ((absoluteScroll / collapseRange).coerceIn(0f, 1f) * 20f).roundToInt() / 20f
        }
    }
    LaunchedEffect(collapseProgress) {
        viewModel.setGlobalHeaderCollapse(collapseProgress)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. HEADER WIDGET (Restored to top)
        item {
            HeaderWidget(isArmed, status.lastMessage, status.lastActionAtMillis)
        }

        // 2. GLOBAL SNAPSHOT (Restored to top)
        item {
            SnapshotWidget(
                totalSignals = dispTotal,
                primaryCount = dispPrimary,
                secondaryCount = dispSecondary,
                rejectedCount = dispRejected
            )
        }

        // 3. LIQUIDITY RADAR — EA live + AI deployments (no old detached AI)
        item {
            LiquidityRadarWidget(signals = signals)
        }

        // 4. CURRENCY STRENGTH & MARKET PULSE
        item {
            CurrencyStrengthPanel()
        }

        item {
            EaAiSummaryPanel(viewModel)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AiRunStatusWidget(
                    modifier = Modifier.weight(1f),
                    lastUpdated = null,
                    status = status.lastMessage,
                    lastActionAtMillis = status.lastActionAtMillis,
                    isLoading = status.isLoading,
                    isConnected = status.isConnected,
                    decisionCount = 0
                )
                JournalSummaryWidget(
                    modifier = Modifier.weight(1f),
                    audits = auditRecords
                )
            }
        }

        item {
            AlertPriorityTimelineWidget(
                events = macroEvents.take(4),
                audits = auditRecords.take(4),
                unreadAlerts = unread
            )
        }

        // EA + AI live summary (replaces old Pre-Move board)
        item {
            EaAiLiveSummaryWidget(signals = signals, leadSignal = leadSignal, viewModel = viewModel)
        }

        // TOP AI SIGNALS (Compact) — deployments backend first, live EA/scanner fallback
        // (the deployments backend at localhost:8003 doesn't exist on the phone,
        // so without the fallback this box is always empty).
        item {
            val eaWriteupsTop by com.asc.markets.data.EASignalLiveStore.signalsByAsset.collectAsState()
            val scannerTop by com.asc.markets.data.ScannerSignalsStore.signals.collectAsState()
            val liveTop = remember(eaWriteupsTop, scannerTop) {
                buildLiveTopSignals(eaWriteupsTop, scannerTop)
            }
            if (topSignals.isNotEmpty()) {
                CompactSignalsWidget(topSignals, viewModel)
            } else {
                CompactLiveSignalsWidget(liveTop, viewModel)
            }
        }

        // 4. UPCOMING EVENTS (Compact + backend status)
        item {
            CompactMacroWidget(upcomingHome, feedLive)
        }

        // 5. EXECUTION QUEUE & RISK
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExecutionQueueWidget(
                    modifier = Modifier.weight(1.2f),
                    ready = dispPrimary + dispSecondary,
                    blocked = dispRejected,
                    nextSignal = dispNext
                )
                RiskSummaryWidget(
                    modifier = Modifier.weight(1f),
                    longCount = dispLong,
                    shortCount = dispShort,
                    candidateCount = dispTotal
                )
            }
        }

        // 6. HEALTH & TELEMETRY — the audit/alert/macro stores are never populated
        // by any producer, so count the live equivalents instead of showing zeros.
        item {
            HealthTelemetryWidget(
                isConnected = status.isConnected == true || eaConnectedHome || calPayloadHome != null,
                isLoading = status.isLoading,
                macroCount = if (macroEvents.isNotEmpty()) macroEvents.size else upcomingHome.size,
                unreadAlerts = upcomingHome.count { it.priority == com.asc.markets.data.ImpactPriority.CRITICAL },
                auditCount = eaWriteupsHome.values.count { it.chart_panel?.validator_active == true },
                auditCaption = "validated live"
            )
        }
    }
}

@Composable
private fun HeaderWidget(isArmed: Boolean, lastMessage: String, lastActionAtMillis: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("ASC COMMAND CENTER", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val lastAction = if (lastActionAtMillis > 0L) SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(lastActionAtMillis)) else "N/A"
            Text("v2.4.1 • $now UTC • Last Action: $lastAction", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(lastMessage, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        
        Surface(
            color = if (isArmed) EmeraldSuccess.copy(alpha = 0.1f) else Color.DarkGray,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, if (isArmed) EmeraldSuccess else Color.Gray)
        ) {
            Text(
                text = if (isArmed) "SURVEILLANCE: ACTIVE" else "MODE: DEV/SIM",
                color = if (isArmed) EmeraldSuccess else Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SnapshotWidget(totalSignals: Int, primaryCount: Int, secondaryCount: Int, rejectedCount: Int) {
    InfoBox(height = 92.dp) {
        Row(modifier = Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI DEPLOYMENTS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(totalSignals.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(" total", color = SlateText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White.copy(0.1f))
            Column(modifier = Modifier.weight(0.8f)) {
                Text("P/S/R", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("$primaryCount/$secondaryCount/$rejectedCount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* AccountExecutionStateWidget REMOVED */

/* LiveTradeStatePanel REMOVED */

/* TradeMiniStat REMOVED */

/* AccountMetricTile REMOVED */


@Composable
private fun AiRunStatusWidget(
    modifier: Modifier,
    lastUpdated: String?,
    status: String,
    lastActionAtMillis: Long,
    isLoading: Boolean,
    isConnected: Boolean?,
    decisionCount: Int
) {
    val color = when {
        isLoading -> Color(0xFFF59E0B)
        isConnected == true -> EmeraldSuccess
        isConnected == false -> RoseError
        else -> IndigoAccent
    }
    val stateText = when {
        isLoading -> "RUNNING"
        isConnected == true -> "SYNCED"
        isConnected == false -> "FAILED"
        else -> "WAITING"
    }
    InfoBox(modifier = modifier, height = 150.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LAST AI RUN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(stateText, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                MiniTelemetryRing(progress = if (isConnected == false) 0.22f else if (isLoading) 0.58f else 0.88f, color = color, modifier = Modifier.size(36.dp))
            }
            Text(status, color = SlateText, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$decisionCount decisions", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(lastUpdated ?: formatMillisTime(lastActionAtMillis), color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun JournalSummaryWidget(
    modifier: Modifier,
    audits: List<AuditRecord>
) {
    val audited = audits.count { it.audited }
    val pending = (audits.size - audited).coerceAtLeast(0)
    val disciplineScore = if (audits.isEmpty()) 1f else (audited.toFloat() / audits.size.toFloat()).coerceIn(0f, 1f)
    InfoBox(modifier = modifier, height = 150.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Top) {
            Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("JOURNAL SUMMARY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$pending pending audits", color = if (pending > 0) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PreMoveScale("Discipline Score", disciplineScore, "Open", "Reviewed", if (disciplineScore >= 0.7f) EmeraldSuccess else Color(0xFFF59E0B))
            Spacer(modifier = Modifier.height(8.dp))
            Text("${audits.size} audit records", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AlertPriorityTimelineWidget(
    events: List<MacroEvent>,
    audits: List<AuditRecord>,
    unreadAlerts: Int
) {
    val timeline = remember(events, audits) {
        val macro = events.map { TimelineItem(it.title, it.datetimeUtc, it.priority == ImpactPriority.CRITICAL, it.currency.ifBlank { "MACRO" }) }
        val audit = audits.map { TimelineItem(it.headline, it.timeUtc, it.impact.equals("CRITICAL", true) || it.impact.equals("HIGH", true), it.assets.ifBlank { "AUDIT" }) }
        (macro + audit).sortedByDescending { it.time }.take(5)
    }
    InfoBox(minHeight = 148.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ALERT PRIORITY TIMELINE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$unreadAlerts unread alerts queued", color = SlateText, fontSize = 10.sp)
                }
                Text(if (timeline.any { it.critical }) "HOT" else "NORMAL", color = if (timeline.any { it.critical }) RoseError else EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            if (timeline.isEmpty()) {
                Text("No priority alerts or audit events yet.", color = Color.Gray, fontSize = 11.sp)
            } else {
                timeline.forEach { item ->
                    TimelineRow(item)
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(8.dp).background(if (item.critical) RoseError else IndigoAccent, CircleShape))
        Text(formatMillisTime(item.time), color = SlateText, fontSize = 9.sp, modifier = Modifier.width(44.dp))
        Text(item.source, color = if (item.critical) RoseError else IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.title, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

private data class TimelineItem(
    val title: String,
    val time: Long,
    val critical: Boolean,
    val source: String
)

@Composable
private fun PreMoveIntelligenceBoard(
    signal: FinalDecisionItem?,
    signals: List<FinalDecisionItem>,
    isConnected: Boolean?
) {
    val baseScore = normalize01(signal?.journal_score)
    val positionScale = metricOrFallback(signal?.final_position_scale ?: signal?.recommended_position_scale, baseScore * 0.65f)
    val ignition = metricOrFallback(signal?.ignition_probability, (baseScore * 0.72f + positionScale * 0.28f).coerceIn(0f, 1f))
    val expansion = metricOrFallback(signal?.expansion_probability, (baseScore * 0.62f + positionScale * 0.38f).coerceIn(0f, 1f))
    val confluence = metricOrFallback(signal?.confluence_score, baseScore)
    val entryQuality = metricOrFallback(signal?.entry_quality_score, (baseScore * 0.8f + positionScale * 0.2f).coerceIn(0f, 1f))
    val exitPressure = metricOrFallback(signal?.exit_pressure_score, 1f - baseScore)
    val correlationRisk = metricOrFallback(signal?.correlation_risk_score, if ((signal?.portfolio_decision_reason ?: "").contains("PORTFOLIO", true)) 0.7f else 0.25f)
    val persistence = metricOrFallback(signal?.regime_persistence_score, (baseScore * 0.68f + (1f - correlationRisk) * 0.32f).coerceIn(0f, 1f))
    val transition = metricOrFallback(signal?.regime_transition_probability, 1f - persistence)
    val structural = metricOrFallback(signal?.structural_pressure_score, ((ignition + expansion) / 2f).coerceIn(0f, 1f))
    val directionalRaw = signal?.directional_score?.toFloat()?.coerceIn(-1f, 1f) ?: when {
        signal?.journal_direction.equals("SHORT", true) || signal?.journal_direction.equals("BEARISH", true) -> -baseScore
        signal?.journal_direction.equals("LONG", true) || signal?.journal_direction.equals("BULLISH", true) -> baseScore
        else -> 0f
    }
    val directionalScale = ((directionalRaw + 1f) / 2f).coerceIn(0f, 1f)
    val directionConfidence = metricOrFallback(signal?.direction_confidence, baseScore)
    val mtfAlignment = metricOrFallback(signal?.mtf_alignment_score, (baseScore * 0.75f + persistence * 0.25f).coerceIn(0f, 1f))
    val confluenceTotal = signal?.confluence_total ?: 6
    val confluenceCount = signal?.confluence_count ?: (confluence * confluenceTotal).toInt().coerceIn(0, confluenceTotal)
    val decile = signal?.ignition_decile ?: ((ignition * 10f).toInt() + 1).coerceIn(1, 10)
    val entryWindow = signal?.entry_window ?: when {
        entryQuality >= 0.78f -> "NOW-15M"
        entryQuality >= 0.55f -> "15M-60M"
        else -> "WAIT"
    }
    val exitPlan = signal?.exit_plan ?: when {
        exitPressure >= 0.62f -> "TRAIL TIGHT"
        exitPressure >= 0.35f -> "STANDARD INVALIDATION"
        else -> "HOLD STRUCTURE"
    }
    val correlationRegime = signal?.correlation_regime ?: when {
        correlationRisk >= 0.7f -> "HIGH_CLUSTER"
        correlationRisk >= 0.4f -> "MODERATE_CLUSTER"
        else -> "LOW_CLUSTER"
    }
    val correlationWarning = signal?.correlation_warning ?: if (correlationRisk >= 0.7f) "Correlation guard active" else "No major cluster block"
    val structuralLabel = signal?.structural_pressure_label ?: when {
        structural >= 0.72f -> "PRESSURE_BUILD"
        structural >= 0.48f -> "STRUCTURE_FORMING"
        else -> "QUIET"
    }
    val liveStatus = signal?.live_tick_status ?: when (isConnected) {
        true -> "STREAM_ACTIVE"
        false -> "OFFLINE"
        null -> "POLLING"
    }
    val liveCount = signal?.live_tick_count ?: 0
    val timeframe = signal?.source_timeframe ?: "H1"
    val riskPct = signal?.final_risk_pct ?: signal?.recommended_risk_pct
    val riskAmount = signal?.final_risk_amount ?: signal?.recommended_risk_amount
    val asset = signal?.asset_1 ?: "SCANNING"
    val direction = signal?.journal_direction?.uppercase(Locale.US) ?: "NEUTRAL"

    InfoBox(minHeight = 470.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("PRE-MOVE INTELLIGENCE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("$asset • $direction • $timeframe SURFACE", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${signals.size} SIGNALS", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IgnitionGauge(
                    value = ignition,
                    label = "IGNITION",
                    detail = "DECILE $decile",
                    modifier = Modifier.width(132.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    PreMoveScale("Expansion Radar", expansion, "Compression", "Expansion", if (expansion >= 0.72f) EmeraldSuccess else IndigoAccent)
                    PreMoveScale("Structural Pressure", structural, "Quiet", "Pressure", if (structural >= 0.72f) RoseError else IndigoAccent)
                    PreMoveScale("MTF Alignment", mtfAlignment, "Mixed", "Aligned", EmeraldSuccess)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("1 Ignition", percentText(ignition), "Pre-move decile $decile", ignition, EmeraldSuccess, Modifier.weight(1f))
                IntelligenceTile("2 Confluence", "$confluenceCount/$confluenceTotal", percentText(confluence), confluence, IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("3 Entry Timing", aiStatusDisplayText(entryWindow), "Quality ${percentText(entryQuality)}", entryQuality, EmeraldSuccess, Modifier.weight(1f))
                IntelligenceTile("4 Exit Logic", aiStatusDisplayText(exitPlan), "Pressure ${percentText(exitPressure)}", exitPressure, if (exitPressure >= 0.62f) RoseError else IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("5 Position Size", percentText(positionScale), "${riskPercentText(riskPct)} • ${moneyText(riskAmount)}", positionScale, EmeraldSuccess, Modifier.weight(1f))
                IntelligenceTile("6 Correlation", aiStatusDisplayText(correlationRegime), aiStatusDisplayText(correlationWarning), correlationRisk, if (correlationRisk >= 0.7f) RoseError else IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("7 Regime", "Persist ${percentText(persistence)}", "Shift ${percentText(transition)}", persistence, IndigoAccent, Modifier.weight(1f))
                IntelligenceTile("8 Structure", aiStatusDisplayText(structuralLabel), "Pressure ${percentText(structural)}", structural, if (structural >= 0.72f) RoseError else IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("9 Direction", direction, "Score ${directionScoreText(directionalRaw)} • Conf ${percentText(directionConfidence)}", directionalScale, if (directionalRaw < -0.05f) RoseError else if (directionalRaw > 0.05f) EmeraldSuccess else Color.White, Modifier.weight(1f))
                IntelligenceTile("10 Live / TF", timeframe, "${aiStatusDisplayText(liveStatus)} • $liveCount ticks", if (liveStatus == "OFFLINE") 0.12f else mtfAlignment, if (liveStatus == "OFFLINE") RoseError else IndigoAccent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AiReasoningStatusWidget(signal: FinalDecisionItem?) {
    var isExpanded by remember { mutableStateOf(false) }
    val finalState = signal?.final_trade_state?.uppercase(Locale.US) ?: "REJECTED"
    val rawReason = signal?.final_trade_reason?.takeIf { it.isNotBlank() }
        ?: signal?.portfolio_decision_reason.orEmpty()
    val reasons = rawReason
        .split(" | ")
        .mapNotNull { item ->
            val formatted = aiStatusDisplayText(item, "")
            formatted.takeIf { it.isNotBlank() }
        }
    val headerColor = when (finalState) {
        "TRADE_CANDIDATE" -> EmeraldSuccess
        "MANUAL_REVIEW" -> Color(0xFFF59E0B)
        else -> RoseError
    }

    InfoBox(minHeight = if (isExpanded) 408.dp else 78.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("AI REASONING", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${signal?.asset_1 ?: "SCANNING"} • ${aiStatusDisplayText(finalState)}",
                        color = headerColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = headerColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, headerColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = if (isExpanded) "COLLAPSE" else "EXPAND",
                        color = headerColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = headerColor.copy(alpha = 0.18f), thickness = 1.dp)
                AiReasoningGateGrid(signal = signal)
                AiReasoningFactorsSection(reasons = reasons, finalState = finalState, headerColor = headerColor)
                AiReasoningFeederStatesSection(signal = signal)
            }
        }
    }
}

@Composable
private fun AiReasoningGateGrid(signal: FinalDecisionItem?) {
    val gates = listOf(
        Triple("ENTRY", aiStatusDisplayText(signal?.entry_state, "No Entry"), signal?.entry_state == "READY"),
        Triple("CONFLUENCE", aiStatusDisplayText(signal?.confluence_state, "No Confluence"), signal?.confluence_state == "TRADEABLE_SETUP"),
        Triple("PLAN", aiStatusDisplayText(signal?.plan_state, "No Plan"), signal?.plan_state == "PLAN_READY"),
        Triple("EXECUTION", aiStatusDisplayText(signal?.execution_status, "Blocked"), signal?.execution_status == "READY"),
        Triple("SIGNAL", aiStatusDisplayText(signal?.signal_quality_state, "No Signal Quality"), signal?.signal_quality_state in listOf("STRONG_SIGNAL", "ELITE_SIGNAL")),
        Triple("RISK", aiStatusDisplayText(signal?.feeder_risk_state, "Risk Off"), signal?.feeder_risk_state != "RISK_OFF")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CRITICAL GATES", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        gates.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (title, state, passed) ->
                    AiReasoningGateTile(
                        title = title,
                        state = state,
                        passed = passed,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AiReasoningGateTile(
    title: String,
    state: String,
    passed: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (passed) EmeraldSuccess else RoseError
    val meter = if (passed) 0.96f else 0.16f
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(state, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
private fun AiReasoningFactorsSection(reasons: List<String>, finalState: String, headerColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (finalState == "TRADE_CANDIDATE") "APPROVAL FACTORS" else "BLOCKING FACTORS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        if (reasons.isEmpty()) {
            Text("No reasoning data available", color = Color.Gray, fontSize = 10.sp)
        } else {
            reasons.take(8).forEach { reason ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = headerColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, headerColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("▸", color = headerColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(reason, color = Color.White, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
            if (reasons.size > 8) {
                Text("+ ${reasons.size - 8} more factors", color = SlateText, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AiReasoningFeederStatesSection(signal: FinalDecisionItem?) {
    val feederStates = listOf(
        "REGIME" to aiStatusDisplayText(signal?.regime_state, "Unknown"),
        "VOLATILITY" to aiStatusDisplayText(signal?.feeder_volatility_state, "Unknown"),
        "STRUCTURE" to aiStatusDisplayText(signal?.structure_state, "Unknown"),
        "TREND" to aiStatusDisplayText(signal?.trend_state, "Unknown"),
        "LIQUIDITY" to aiStatusDisplayText(signal?.feeder_liquidity_state, "Unknown"),
        "INDICATOR" to aiStatusDisplayText(signal?.feeder_indicator_state, "Unknown")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("KEY FEEDER STATES", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.025f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                feederStates.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun aiStatusDisplayText(value: String?, fallback: String = "Unknown"): String {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return fallback
    val parts = raw.split("=", limit = 2)
    return if (parts.size == 2) {
        "${aiStatusWords(parts[0])}: ${aiStatusWords(parts[1])}"
    } else {
        aiStatusWords(raw)
    }
}

private fun aiStatusWords(raw: String): String {
    return raw
        .trim()
        .split(Regex("[_\\s]+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            val upper = token.uppercase(Locale.US)
            when {
                upper in setOf("AI", "USD", "USDT", "BTC", "ETH", "XAU", "XAG", "ATR", "MTF", "TF", "FX", "BOS", "CHOCH") -> upper
                token.any { it.isDigit() } -> upper
                else -> token.lowercase(Locale.US).replaceFirstChar { ch -> ch.titlecase(Locale.US) }
            }
        }
}

@Composable
private fun IgnitionGauge(value: Float, label: String, detail: String, modifier: Modifier = Modifier) {
    val clamped = value.coerceIn(0f, 1f)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val diameter = minOf(size.width - stroke, size.height * 1.85f - stroke)
                val topLeft = Offset((size.width - diameter) / 2f, 8.dp.toPx())
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
                    color = if (clamped >= 0.72f) EmeraldSuccess else IndigoAccent,
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
                val needleLength = diameter / 2f - 14.dp.toPx()
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
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = center)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 18.dp)) {
                Text(percentText(clamped), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(label, color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(detail, color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PreMoveScale(label: String, value: Float, left: String, right: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(percentText(value), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Box(modifier = Modifier.fillMaxWidth().height(7.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            }
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
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

@Composable
private fun CompactSignalsWidget(signals: List<FinalDecisionItem>, viewModel: ForexViewModel) {
    InfoBox(minHeight = 188.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TOP AI SIGNALS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("ranked by deployment priority + confidence", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("VIEW ALL", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                    viewModel.navigateTo(com.asc.markets.data.AppView.TRADE_DASHBOARD)
                })
            }
            if (signals.isEmpty()) {
                Text("Scanning for tradeable signals...", color = Color.Gray, fontSize = 12.sp)
            } else {
                signals.forEachIndexed { index, signal ->
                    TopSignalVisualRow(signal = signal, rank = index + 1)
                }
            }
        }
    }
}

@Composable
private fun TopSignalVisualRow(signal: FinalDecisionItem, rank: Int) {
    val score = normalize01(signal.journal_score)
    val ignition = metricOrFallback(signal.ignition_probability, score)
    val bias = signal.journal_direction?.uppercase(Locale.US) ?: "NEUTRAL"
    val color = when {
        bias.contains("SHORT") || bias.contains("BEAR") || bias.contains("SELL") -> RoseError
        bias.contains("LONG") || bias.contains("BULL") || bias.contains("BUY") -> EmeraldSuccess
        else -> IndigoAccent
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(rank.toString(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(signal.asset_1 ?: "---", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(bias, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
                Text(aiStatusDisplayText(signal.journal_label ?: signal.portfolio_decision_label, "Pre Move Candidate"), color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SignalStrengthMiniChart(score = ignition, color = color, modifier = Modifier.width(72.dp).height(30.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(percentText(score), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("conf", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class LiveSignalInfo(
    val asset: String,
    val direction: String,
    val score: Float,
    val tier: String,
    val hasSweep: Boolean,
    val hasFvg: Boolean,
    val hasBos: Boolean
)

// Merged live view of what the phone actually receives: EA write-ups first,
// then MT5 scanner signals. Sorted by score, highest first.
private fun buildLiveSignalInfos(
    writeups: Map<String, com.asc.markets.data.ASCSignalData>,
    scanner: List<com.asc.markets.data.ScannerSignal>
): List<LiveSignalInfo> {
    val merged = linkedMapOf<String, LiveSignalInfo>()
    writeups.forEach { (key, s) ->
        val dir = s.direction.uppercase(Locale.US)
        val score = maxOf(normalize01(s.confidence), normalize01(s.chart_panel?.votes?.win_pct))
        if (dir == "WAIT" && score <= 0f) return@forEach
        val liq = s.liquidity
        merged[key] = LiveSignalInfo(
            asset = s.asset.ifBlank { key },
            direction = dir,
            score = score,
            tier = (s.chart_panel?.quality_tier ?: "").uppercase(Locale.US),
            hasSweep = liq?.sweep_high == true || liq?.sweep_low == true,
            hasFvg = liq?.fvg_bull == true || liq?.fvg_bear == true,
            hasBos = liq?.bos_bull == true || liq?.bos_bear == true
        )
    }
    scanner.forEach { sc ->
        val key = liveSignalKey(sc.asset)
        if (merged.containsKey(key)) return@forEach
        val dir = sc.direction.uppercase(Locale.US)
        val score = normalize01(sc.confidence)
        if (dir == "WAIT" && score <= 0f) return@forEach
        merged[key] = LiveSignalInfo(
            asset = sc.asset.removeSuffix("m").removeSuffix("M"),
            direction = dir,
            score = score,
            tier = "",
            hasSweep = false,
            hasFvg = false,
            hasBos = false
        )
    }
    return merged.values.sortedByDescending { it.score }
}

// Deployment-bucket equivalent for live signals: rejected tiers stay rejected,
// strong tiers / high scores go primary, valid / mid scores secondary.
private fun liveBucketOf(info: LiveSignalInfo): Char = when {
    info.tier == "FILTERED" || info.tier == "LOW" || info.tier == "REJECTED" -> 'R'
    info.tier == "ELITE" || info.tier == "STRONG" || info.score >= 0.70f -> 'P'
    info.tier == "VALID" || info.score >= 0.45f -> 'S'
    else -> 'R'
}

private fun isLiveLong(direction: String): Boolean {
    val d = direction.uppercase(Locale.US)
    return d == "LONG" || d == "BUY" || d == "BULLISH"
}

private fun isLiveShort(direction: String): Boolean {
    val d = direction.uppercase(Locale.US)
    return d == "SHORT" || d == "SELL" || d == "BEARISH"
}

private data class LiveTopSignal(
    val asset: String,
    val direction: String,
    val label: String,
    val score: Float
)

private fun liveSignalKey(raw: String): String = raw.uppercase(Locale.US)
    .replace("/", "").replace("-", "").replace("_", "")
    .replace(" ", "").replace(".", "").removeSuffix("M")

// Fallback ranking from data the phone actually receives: live EA write-ups
// first, then MT5 scanner signals. Skips WAIT/empty entries.
private fun buildLiveTopSignals(
    writeups: Map<String, com.asc.markets.data.ASCSignalData>,
    scanner: List<com.asc.markets.data.ScannerSignal>
): List<LiveTopSignal> {
    val merged = linkedMapOf<String, LiveTopSignal>()
    writeups.forEach { (key, s) ->
        val dir = s.direction.uppercase(Locale.US)
        val score = maxOf(normalize01(s.confidence), normalize01(s.chart_panel?.votes?.win_pct))
        if (dir == "WAIT" && score <= 0f) return@forEach
        val label = s.chart_panel?.quality_tier?.takeIf { it.isNotBlank() && !it.equals("NONE", true) }
            ?: s.regime?.state?.takeIf { it.isNotBlank() && !it.equals("UNKNOWN", true) }
            ?: "EA write-up"
        merged[key] = LiveTopSignal(
            asset = s.asset.ifBlank { key },
            direction = dir,
            label = label.replace("_", " "),
            score = score
        )
    }
    scanner.forEach { sc ->
        val key = liveSignalKey(sc.asset)
        if (merged.containsKey(key)) return@forEach
        val dir = sc.direction.uppercase(Locale.US)
        val score = normalize01(sc.confidence)
        if (dir == "WAIT" && score <= 0f) return@forEach
        merged[key] = LiveTopSignal(
            asset = sc.asset.removeSuffix("m").removeSuffix("M"),
            direction = dir,
            label = "P ${percentText(score)} • ${sc.age}s ago",
            score = score
        )
    }
    return merged.values.sortedByDescending { it.score }.take(5)
}

@Composable
private fun CompactLiveSignalsWidget(signals: List<LiveTopSignal>, viewModel: ForexViewModel) {
    InfoBox(minHeight = 188.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TOP AI SIGNALS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("live EA write-ups + MT5 scanner", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("VIEW ALL", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                    viewModel.navigateTo(com.asc.markets.data.AppView.TRADE_DASHBOARD)
                })
            }
            if (signals.isEmpty()) {
                Text("Scanning for tradeable signals...", color = Color.Gray, fontSize = 12.sp)
            } else {
                signals.forEachIndexed { index, signal ->
                    LiveTopSignalRow(signal = signal, rank = index + 1)
                }
            }
        }
    }
}

@Composable
private fun LiveTopSignalRow(signal: LiveTopSignal, rank: Int) {
    val bias = signal.direction.uppercase(Locale.US)
    val color = when {
        bias.contains("SHORT") || bias.contains("BEAR") || bias.contains("SELL") -> RoseError
        bias.contains("LONG") || bias.contains("BULL") || bias.contains("BUY") -> EmeraldSuccess
        else -> IndigoAccent
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(rank.toString(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(signal.asset, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(bias, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
                Text(signal.label, color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SignalStrengthMiniChart(score = signal.score, color = color, modifier = Modifier.width(72.dp).height(30.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(percentText(signal.score), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("conf", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SignalStrengthMiniChart(score: Float, color: Color, modifier: Modifier = Modifier) {    val clamped = score.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val bars = 7
        val gap = 3.dp.toPx()
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

@Composable
private fun CompactMacroWidget(events: List<com.asc.markets.data.MacroEvent>, isConnected: Boolean?) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Upcoming Events",
                color = Color.White,
                fontSize = DashboardFontSizes.valueLarge,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
            BackendStatusPill(isConnected)
        }
        if (events.isEmpty()) {
            Text(
                "No macro/backend events available for this lens.",
                color = SlateText,
                fontSize = DashboardFontSizes.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            events.forEachIndexed { index, event ->
                CommandCenterRawFeedRow(event)
                if (index != events.lastIndex) {
                    Divider(
                        color = Color.White.copy(alpha = 0.06f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackendStatusPill(isConnected: Boolean?) {
    val connected = isConnected ?: false
    val color = when (connected) {
        true -> EmeraldSuccess
        false -> RoseError
    }
    val label = when (connected) {
        true -> "CONNECTED"
        false -> "OFFLINE"
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CommandCenterRawFeedRow(event: com.asc.markets.data.MacroEvent) {
    // Severity color matches the calendar page: HIGH red, MEDIUM amber, LOW gray.
    val sevColor = when (event.priority) {
        com.asc.markets.data.ImpactPriority.CRITICAL -> Color(0xFFF23645)
        com.asc.markets.data.ImpactPriority.HIGH -> Color(0xFFFFC857)
        else -> Color(0xFF6B7280)
    }
    val hasAfp = event.actual.isNotBlank() || event.forecast.isNotBlank() || event.previous.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (hasAfp) 56.dp else 40.dp)
                .background(sevColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.US).format(Date(event.datetimeUtc)),
                    color = Color(0xFF8B8B8B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (event.currency.isNotBlank()) {
                    Text(
                        text = event.currency.uppercase(Locale.US),
                        color = sevColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (hasAfp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AfpMini("ACT", event.actual)
                    AfpMini("FCST", event.forecast)
                    AfpMini("PREV", event.previous)
                }
            }
        }
    }
}

@Composable
private fun AfpMini(label: String, value: String) {
    Column {
        Text(
            text = value.ifBlank { "--" },
            color = Color(0xFFE8EAED),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            color = Color(0xFF8D95A5),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun ExecutionQueueWidget(modifier: Modifier, ready: Int, blocked: Int, nextSignal: String) {
    val total = (ready + blocked).coerceAtLeast(1)
    val readyShare = ready.toFloat() / total.toFloat()
    InfoBox(modifier = modifier, minHeight = 124.dp) {
        Row(modifier = Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QueueDonut(readyShare = readyShare, modifier = Modifier.size(58.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("EXECUTION QUEUE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QueueMetric("READY", ready.toString(), EmeraldSuccess)
                    QueueMetric("BLOCKED", blocked.toString(), RoseError)
                }
                QueueSplitBar(readyShare = readyShare)
                Text("Next deployment: $nextSignal", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QueueDonut(readyShare: Float, modifier: Modifier = Modifier) {
    val clamped = readyShare.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = RoseError.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = EmeraldSuccess,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(percentText(clamped), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QueueMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QueueSplitBar(readyShare: Float) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(RoseError.copy(alpha = 0.18f), RoundedCornerShape(8.dp))) {
        Box(
            modifier = Modifier
                .fillMaxWidth(readyShare.coerceIn(0.02f, 1f))
                .fillMaxHeight()
                .background(EmeraldSuccess, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun RiskSummaryWidget(modifier: Modifier, longCount: Int, shortCount: Int, candidateCount: Int) {
    val totalDirectional = (longCount + shortCount).coerceAtLeast(1)
    val longShare = longCount.toFloat() / totalDirectional.toFloat()
    val dir = when {
        longCount > shortCount -> "LONG BIAS"
        shortCount > longCount -> "SHORT BIAS"
        else -> "BALANCED"
    }
    val accent = when (dir) {
        "LONG BIAS" -> EmeraldSuccess
        "SHORT BIAS" -> RoseError
        else -> IndigoAccent
    }
    InfoBox(modifier = modifier, minHeight = 124.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("EXPOSURE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(dir, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Text(candidateCount.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            ExposureSplitVisualizer(longShare = longShare)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LONG $longCount", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("SHORT $shortCount", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExposureSplitVisualizer(longShare: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
        val centerY = size.height / 2f
        val stroke = 12.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = EmeraldSuccess,
            start = Offset(0f, centerY),
            end = Offset(size.width * longShare.coerceIn(0f, 1f), centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = RoseError,
            start = Offset(size.width, centerY),
            end = Offset(size.width * longShare.coerceIn(0f, 1f), centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(size.width / 2f, 2.dp.toPx()),
            end = Offset(size.width / 2f, size.height - 2.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun HealthTelemetryWidget(
    isConnected: Boolean?,
    isLoading: Boolean,
    macroCount: Int,
    unreadAlerts: Int,
    auditCount: Int,
    auditCaption: String = "journal trail"
) {
    val statusText = when {
        isLoading -> "RUN"
        isConnected == true -> "OK"
        isConnected == false -> "FAIL"
        else -> "N/A"
    }
    val statusColor = when {
        isLoading -> Color(0xFFF59E0B)
        isConnected == true -> EmeraldSuccess
        isConnected == false -> RoseError
        else -> Color.Gray
    }
    InfoBox(minHeight = 158.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SYSTEM TELEMETRY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("LIVE MONITOR", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatusCard("BACKEND", statusText, if (isLoading) "pipeline active" else "api channel", if (statusColor == RoseError) 0.22f else 0.86f, statusColor, Modifier.weight(1f))
                TelemetryStatusCard("MACRO", macroCount.toString(), "stream events", (macroCount / 12f).coerceIn(0.08f, 1f), IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryStatusCard("ALERTS", unreadAlerts.toString(), if (unreadAlerts > 0) "attention" else "clear", if (unreadAlerts > 0) 0.75f else 0.18f, if (unreadAlerts > 0) Color(0xFFF59E0B) else EmeraldSuccess, Modifier.weight(1f))
                TelemetryStatusCard("AUDIT", auditCount.toString(), auditCaption, (auditCount / 20f).coerceIn(0.10f, 1f), Color.White, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TelemetryStatusCard(
    label: String,
    value: String,
    caption: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MiniTelemetryRing(progress = progress, color = color, modifier = Modifier.size(34.dp))
            Column {
                Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(caption, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

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

private fun deploymentWeight(signal: FinalDecisionItem): Int {
    val label = signal.portfolio_decision_label?.uppercase(Locale.US) ?: ""
    val bucket = signal.portfolio_deployment_bucket?.uppercase(Locale.US) ?: ""
    return when {
        label.contains("PRIMARY") || bucket.contains("PRIMARY") -> 4
        label.contains("SECONDARY") || bucket.contains("SECONDARY") -> 3
        label.contains("QUEUE") || bucket.contains("QUEUE") -> 2
        label.contains("REJECT") -> 0
        else -> 1
    }
}

@Composable
private fun LiquidityRadarWidget(signals: List<FinalDecisionItem>) {
    // EA + AI combined: join EA live assets with AI sweep/displacement signals, ranked by EA confidence + AI score
    val eaAssets by com.asc.markets.data.EALiveDataStore.liveAssets.collectAsState()
    val eaBySymbol = remember(eaAssets) { eaAssets.associateBy { it.symbol.uppercase() } }

    // High-priority AI signals (sweep/displacement/choch/bos)
    val highPriorityAi = remember(signals) {
        signals.filter {
            val reason = it.portfolio_decision_reason?.uppercase() ?: ""
            reason.contains("SWEEP") || reason.contains("DISPLACEMENT") || reason.contains("CHOCH") || reason.contains("BOS")
        }
    }

    // Merged EA+AI list: prefer assets that have both EA live data and AI signal, rank by combined score
    val merged = remember(highPriorityAi, eaBySymbol) {
        // Start from AI high-priority, enrich with EA; if AI empty, fall back to top EA by confidence
        val fromAi = highPriorityAi.mapNotNull { s ->
            val ea = eaBySymbol[s.asset_1?.uppercase() ?: ""]
            // Combined score: AI journal_score (0-100) + EA confidence (0-1*100)
            val aiScore = (s.journal_score ?: 0.0)
            val eaScore = (ea?.eaAi?.confidence ?: 0.0) * 100
            val combined = maxOf(aiScore, eaScore)
            // Keep only if either side signals sweep/displacement or EA confidence meaningful
            if (combined > 0) s to combined else null
        }.sortedByDescending { it.second }.map { it.first }

        if (fromAi.isNotEmpty()) fromAi
        else {
            // No AI sweep signals: show EA assets by confidence that have a direction.
            // No cap — the row scrolls sideways when there are more than fit.
            val topEaSignals = eaAssets
                .filter { (it.eaAi?.confidence ?: 0.0) > 0.0 && (it.eaAi?.direction ?: "WAIT") != "WAIT" }
                .sortedByDescending { it.eaAi?.confidence ?: 0.0 }
            // Map EA to synthetic signal-like items for card display (keep signal type for card color)
            topEaSignals.mapNotNull { ea ->
                signals.find { it.asset_1?.equals(ea.symbol, true) == true }
                    ?: signals.firstOrNull()
            }.takeIf { it.isNotEmpty() } ?: emptyList()
        }
    }

    val eaConnected by com.asc.markets.data.EALiveDataStore.isConnected.collectAsState()

    // Live fallback: when the deployments backend yields nothing, radar the live
    // EA liquidity flags (sweep/FVG/BOS) + top live scores instead of staying empty.
    val wuRadar by com.asc.markets.data.EASignalLiveStore.signalsByAsset.collectAsState()
    val scRadar by com.asc.markets.data.ScannerSignalsStore.signals.collectAsState()
    val liveRadarCards = remember(wuRadar, scRadar, signals) {
        if (signals.isNotEmpty()) emptyList()
        else {
            val infos = buildLiveSignalInfos(wuRadar, scRadar)
            val flagged = infos.filter { it.hasSweep || it.hasFvg || it.hasBos }
            flagged + infos.filterNot { flagged.contains(it) }
        }
    }

    InfoBox(minHeight = 160.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LIQUIDITY RADAR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(if (eaConnected) "EA live + AI deployments" else "AI deployments (EA offline)", color = SlateText, fontSize = 10.sp)
                }
                Surface(color = (if (eaConnected) EmeraldSuccess else SlateText).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(if (eaConnected) "EA + AI" else "AI ONLY", color = if (eaConnected) EmeraldSuccess else SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            if (merged.isEmpty() && liveRadarCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text(if (eaConnected) "No EA/AI sweep signals at this time." else "Awaiting EA stream — showing AI when available.", color = SlateText, fontSize = 11.sp)
                }
            } else {
                // Horizontal rail: fixed-width cards — scrolls sideways when there
                // are more assets than fit on screen.
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (merged.isNotEmpty()) {
                        items(merged) { signal ->
                            LiquidityAssetCard(signal, Modifier.width(140.dp))
                        }
                    } else {
                        items(liveRadarCards) { info ->
                            LiveLiquidityAssetCard(info, Modifier.width(140.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLiquidityAssetCard(info: LiveSignalInfo, modifier: Modifier = Modifier) {
    val typeLabel = when {
        info.hasSweep -> "SWEEP"
        info.hasBos -> "DISPLACE"
        info.hasFvg -> "FVG"
        else -> "GAP"
    }
    val accent = when {
        info.hasSweep -> RoseError
        info.hasBos -> EmeraldSuccess
        info.hasFvg -> Color(0xFF60A5FA)
        else -> IndigoAccent
    }
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(info.asset, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(typeLabel, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(info.score.coerceIn(0.2f, 1f))
                        .fillMaxHeight()
                        .background(accent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun LiquidityAssetCard(signal: FinalDecisionItem, modifier: Modifier = Modifier) {
    val reason = signal.portfolio_decision_reason?.uppercase() ?: ""
    val isSweep = reason.contains("SWEEP")
    val isDisplacement = reason.contains("DISPLACEMENT")
    val accent = if (isSweep) RoseError else if (isDisplacement) EmeraldSuccess else IndigoAccent
    
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(signal.asset_1 ?: "---", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
            
            val typeLabel = when {
                isSweep -> "SWEEP"
                isDisplacement -> "DISPLACE"
                else -> "GAP"
            }
            
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(typeLabel, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(normalize01(signal.journal_score).coerceIn(0.2f, 1f))
                        .fillMaxHeight()
                        .background(accent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

private fun normalize01(value: Double?): Float {
    val raw = value?.toFloat() ?: return 0f
    val normalized = if (raw > 1f) raw / 100f else raw
    return normalized.coerceIn(0f, 1f)
}

private fun metricOrFallback(value: Double?, fallback: Float): Float {
    return if (value == null) fallback.coerceIn(0f, 1f) else normalize01(value)
}

private fun percentText(value: Float): String {
    return "${(value.coerceIn(0f, 1f) * 100f).toInt()}%"
}

private fun moneyLargeText(value: Double): String {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue >= 1_000_000.0 -> "${'$'}${String.format(Locale.US, "%.2fM", value / 1_000_000.0)}"
        absValue >= 1_000.0 -> "${'$'}${String.format(Locale.US, "%,.1fK", value / 1_000.0)}"
        else -> "${'$'}${String.format(Locale.US, "%,.2f", value)}"
    }
}

private fun liveMoneyText(value: Double, hasLiveValue: Boolean): String {
    return if (hasLiveValue) moneyLargeText(value) else "WAITING"
}

private fun signedMoneyText(value: Double): String {
    val sign = if (value >= 0.0) "+" else "-"
    return "$sign${moneyLargeText(kotlin.math.abs(value))}"
}

private fun priceText(value: Double?): String {
    return value?.let { String.format(Locale.US, "%,.2f", it) } ?: "WAITING"
}

private fun signedNumberText(value: Double?): String {
    return value?.let { String.format(Locale.US, "%+.2f", it) } ?: "WAITING"
}

private fun signedPercentText(value: Double?): String {
    return value?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "WAITING"
}

private fun volumeText(value: Double?): String {
    return value?.let { String.format(Locale.US, "%,.4f", it) } ?: "WAITING"
}



private fun formatMillisTime(value: Long): String {
    return if (value > 0L) SimpleDateFormat("HH:mm", Locale.US).format(Date(value)) else "N/A"
}

private fun riskPercentText(value: Double?): String {
    val raw = value ?: return "Risk N/A"
    val pct = if (raw <= 1.0) raw * 100.0 else raw
    return "Risk ${String.format(Locale.US, "%.2f%%", pct)}"
}

private fun moneyText(value: Double?): String {
    val amount = value ?: return "${'$'}0"
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 1_000_000.0 -> "${'$'}${String.format(Locale.US, "%.1fM", amount / 1_000_000.0)}"
        absAmount >= 1_000.0 -> "${'$'}${String.format(Locale.US, "%.1fK", amount / 1_000.0)}"
        else -> "${'$'}${String.format(Locale.US, "%.0f", amount)}"
    }
}

private fun directionScoreText(value: Float): String {
    return String.format(Locale.US, "%+.2f", value.coerceIn(-1f, 1f))
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(36.dp).clickable { onClick() },
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EaAiSummaryPanel(viewModel: ForexViewModel) {
    val eaAssets by com.asc.markets.data.EALiveDataStore.liveAssets.collectAsState()
    val eaConnected by com.asc.markets.data.EALiveDataStore.isConnected.collectAsState()
    val simResult by viewModel.simulationResult.collectAsState()
    val deployments by viewModel.aiDeployments.collectAsState()
    val aiCount = deployments?.final_decision?.size ?: 0
    // Live fallback when the deployments backend is unreachable.
    val wuPanel by com.asc.markets.data.EASignalLiveStore.signalsByAsset.collectAsState()
    val scPanel by com.asc.markets.data.ScannerSignalsStore.signals.collectAsState()
    val livePanelInfos = remember(wuPanel, scPanel, deployments) {
        if (deployments != null) emptyList() else buildLiveSignalInfos(wuPanel, scPanel)
    }
    val livePanelTop = livePanelInfos.firstOrNull()
    val aiDisplayCount = if (aiCount > 0) aiCount else livePanelInfos.size
    InfoBox(minHeight = 110.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("EA + AI LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    DotLabel(if (eaConnected) "EA LIVE" else "EA OFFLINE", if (eaConnected) EmeraldSuccess else RoseError)
                    DotLabel(
                        if (aiCount > 0) "AI $aiCount" else if (livePanelTop != null) "AI $aiDisplayCount" else "AI idle",
                        if (aiCount > 0 || livePanelTop != null) IndigoAccent else SlateText
                    )
                }
            }
            if (eaAssets.isEmpty() && aiCount == 0 && livePanelTop == null) {
                Text("No live EA assets or AI deployments yet.", color = SlateText, fontSize = 11.sp)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EA Assets", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${eaAssets.size} streaming", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text(eaAssets.take(3).joinToString(", ") { it.symbol }.ifEmpty { "—" }, color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Last AI Signal", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val top = deployments?.final_decision?.maxByOrNull { it.journal_score ?: 0.0 }
                        Text(top?.asset_1 ?: livePanelTop?.asset ?: "—", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        if (simResult != null) Text("${((simResult!!.winProbability ?: 0.0) * 100).toInt()}% win prob", color = EmeraldSuccess, fontSize = 10.sp)
                        else if (livePanelTop != null) Text("${(livePanelTop.score * 100).toInt()}% conf", color = EmeraldSuccess, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EaAiLiveSummaryWidget(signals: List<com.asc.markets.data.remote.FinalDecisionItem>, leadSignal: com.asc.markets.data.remote.FinalDecisionItem?, viewModel: ForexViewModel) {
    val eaAssets by com.asc.markets.data.EALiveDataStore.liveAssets.collectAsState()
    val eaWriteups by com.asc.markets.data.EASignalLiveStore.signalsByAsset.collectAsState()
    val scannerSignals by com.asc.markets.data.ScannerSignalsStore.signals.collectAsState()
    // The live feed's ea_ai block is WAIT/0 until the MT5 EA writes per-asset AI
    // values, so resolve each asset from the live EA write-up first, then the
    // AI scanner, and only then fall back to the feed value.
    fun resolveDirection(symbol: String, feedDir: String?): String {
        if (!feedDir.isNullOrBlank() && !feedDir.equals("WAIT", true)) return feedDir.uppercase(java.util.Locale.US)
        val key = symbol.uppercase(java.util.Locale.US)
            .replace("/", "").replace("-", "").replace("_", "")
            .replace(" ", "").replace(".", "").removeSuffix("M")
        eaWriteups[key]?.direction?.takeIf { it.isNotBlank() && !it.equals("WAIT", true) }?.let {
            return it.uppercase(java.util.Locale.US)
        }
        scannerSignals.firstOrNull {
            it.asset.uppercase(java.util.Locale.US).replace("/", "").removeSuffix("M") == key
        }?.direction?.takeIf { it.isNotBlank() && !it.equals("WAIT", true) }?.let {
            return it.uppercase(java.util.Locale.US)
        }
        return "WAIT"
    }
    fun resolveConfidence(symbol: String, feedConf: Double?): Int {
        if ((feedConf ?: 0.0) > 0.0) return ((feedConf ?: 0.0) * 100).toInt().coerceIn(0, 100)
        val key = symbol.uppercase(java.util.Locale.US)
            .replace("/", "").replace("-", "").replace("_", "")
            .replace(" ", "").replace(".", "").removeSuffix("M")
        eaWriteups[key]?.confidence?.takeIf { it > 0.0 }?.let {
            return (it * 100).toInt().coerceIn(0, 100)
        }
        scannerSignals.firstOrNull {
            it.asset.uppercase(java.util.Locale.US).replace("/", "").removeSuffix("M") == key
        }?.confidence?.takeIf { it > 0.0 }?.let {
            // Scanner confidence may be 0-1 or already 0-100.
            return if (it > 1.0) it.toInt().coerceIn(0, 100) else (it * 100).toInt().coerceIn(0, 100)
        }
        return 0
    }
    InfoBox(minHeight = 220.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("EA + AI SUMMARY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            if (eaAssets.isEmpty() && signals.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Awaiting EA stream and AI deployments", color = SlateText, fontSize = 11.sp)
                }
            } else {
                // EA top assets — rank by live signal (non-WAIT first, then confidence),
                // not feed order, so the box doesn't stick on the first 4 symbols.
                if (eaAssets.isNotEmpty()) {
                    Text("EA Live — Top assets", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val ranked = remember(eaAssets, eaWriteups, scannerSignals) {
                        eaAssets.map { asset ->
                            Triple(
                                asset,
                                resolveDirection(asset.symbol, asset.eaAi?.direction),
                                resolveConfidence(asset.symbol, asset.eaAi?.confidence)
                            )
                        }.sortedWith(
                            compareByDescending<Triple<com.asc.markets.data.EAAssetData, String, Int>> { it.second != "WAIT" }
                                .thenByDescending { it.third }
                        ).take(4)
                    }
                    ranked.forEach { (asset, dir, conf) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(asset.symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(dir, color = when (dir) { "BUY", "LONG" -> EmeraldSuccess; "SELL", "SHORT" -> RoseError; else -> SlateText }, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text("$conf%", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    }
                // AI deployments
                if (signals.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text("AI Deployments — ${signals.size}", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    signals.take(3).forEach { s ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.asset_1 ?: "—", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(s.journal_direction ?: "—", color = SlateText, fontSize = 11.sp)
                            Text("${(s.journal_score ?: 0.0).toInt()}%", color = IndigoAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DotLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}
