package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.trading.app.data.PaperTradingAccountSnapshot
import com.trading.app.data.PaperTradingSnapshotStore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

import com.asc.markets.ui.screens.dashboard.CurrencyStrengthPanel

@Composable
fun CommandCenterTab(viewModel: ForexViewModel) {
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val macroEvents by viewModel.macroStreamEvents.collectAsState()
    val isArmed by viewModel.isArmed.collectAsState()
    val status by viewModel.commandCenterStatus.collectAsState()
    val auditRecords by viewModel.auditRecords.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()
    val paperTradingSnapshot = PaperTradingSnapshotStore.snapshot

    val signals = aiDeployments?.final_decision ?: emptyList()
    val primaryCount = signals.count { it.portfolio_decision_label == "PRIMARY_DEPLOYMENT" }
    val secondaryCount = signals.count { it.portfolio_decision_label == "SECONDARY_DEPLOYMENT" }
    val rejectedCount = signals.count { it.portfolio_decision_label == "REJECTED_BY_PORTFOLIO" }
    val longCount = signals.count { it.journal_direction.equals("LONG", ignoreCase = true) }
    val shortCount = signals.count { it.journal_direction.equals("SHORT", ignoreCase = true) }
    val rankedSignals = remember(signals) {
        signals.sortedWith(
            compareByDescending<FinalDecisionItem> { deploymentWeight(it) }
                .thenByDescending { normalize01(it.journal_score) }
        )
    }
    val topSignals = rankedSignals.take(5)
    val leadSignal = topSignals.firstOrNull()
    
    LazyColumn(
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
                totalSignals = signals.size,
                primaryCount = primaryCount,
                secondaryCount = secondaryCount,
                rejectedCount = rejectedCount
            )
        }

        // 3. DETERMINISTIC LIQUIDITY RADAR (NEW - from NEW_ASC)
        item {
            LiquidityRadarWidget(signals)
        }

        // 4. CURRENCY STRENGTH & MARKET PULSE
        item {
            CurrencyStrengthPanel()
        }

        item {
            AccountExecutionStateWidget(paperTradingSnapshot)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AiRunStatusWidget(
                    modifier = Modifier.weight(1f),
                    lastUpdated = aiDeployments?.last_updated,
                    status = status.lastMessage,
                    lastActionAtMillis = status.lastActionAtMillis,
                    isLoading = status.isLoading,
                    isConnected = status.isConnected,
                    decisionCount = signals.size
                )
                JournalSummaryWidget(
                    modifier = Modifier.weight(1f),
                    audits = auditRecords,
                    snapshot = paperTradingSnapshot
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

        item {
            PreMoveIntelligenceBoard(
                signal = leadSignal,
                signals = signals,
                isConnected = status.isConnected
            )
        }

        // 3. TOP AI SIGNALS (Compact)
        item {
            CompactSignalsWidget(topSignals, viewModel)
        }

        // 4. RAW FEED / MACRO STREAM (Compact + backend status)
        item {
            CompactMacroWidget(macroEvents.take(5), status.isConnected)
        }

        // 5. EXECUTION QUEUE & RISK
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExecutionQueueWidget(
                    modifier = Modifier.weight(1.2f),
                    ready = primaryCount + secondaryCount,
                    blocked = rejectedCount,
                    nextSignal = topSignals.firstOrNull()?.asset_1 ?: "—"
                )
                RiskSummaryWidget(
                    modifier = Modifier.weight(1f),
                    longCount = longCount,
                    shortCount = shortCount,
                    candidateCount = signals.size
                )
            }
        }

        // 6. HEALTH & TELEMETRY
        item {
            HealthTelemetryWidget(
                isConnected = status.isConnected,
                isLoading = status.isLoading,
                macroCount = macroEvents.size,
                unreadAlerts = unread,
                auditCount = auditRecords.size
            )
        }

        // 7. QUICK ACTIONS & LINKS
        item {
            QuickActionsWidget(viewModel, status.isLoading)
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

@Composable
private fun AccountExecutionStateWidget(snapshot: PaperTradingAccountSnapshot) {
    val hasAccount = snapshot.hasLiveAccountData
    val hasPnl = snapshot.hasLiveAccountData || snapshot.hasLiveTradeData
    val pnlColor = if (!hasPnl) Color.Gray else if ((snapshot.currentTradePnl ?: snapshot.floatingPnl) >= 0.0) EmeraldSuccess else RoseError
    val riskMeter = (snapshot.openRiskPct / 10.0).toFloat().coerceIn(0.04f, 1f)
    InfoBox(minHeight = 318.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ACCOUNT / EXECUTION STATE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(if (hasAccount) "live paper-trading account stream" else "waiting for live account stream", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BackendStatusPill(snapshot.isConnected)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountMetricTile("Balance", liveMoneyText(snapshot.balance, hasAccount), "cash balance", if (hasAccount) 0.76f else 0.04f, EmeraldSuccess, Modifier.weight(1f))
                AccountMetricTile("Equity", liveMoneyText(snapshot.equity, hasAccount), "balance + floating P/L", if (hasAccount) equityMeter(snapshot) else 0.04f, if (snapshot.equity >= snapshot.balance) EmeraldSuccess else RoseError, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountMetricTile("Free Margin", liveMoneyText(snapshot.freeMargin, hasAccount), "available funds", if (hasAccount) freeMarginMeter(snapshot) else 0.04f, IndigoAccent, Modifier.weight(1f))
                AccountMetricTile("Floating P/L", if (hasPnl) signedMoneyText(snapshot.floatingPnl) else "WAITING", "Realized ${if (hasAccount) signedMoneyText(snapshot.realizedPnl) else "WAITING"}", if (hasPnl) (kotlin.math.abs(snapshot.floatingPnl) / snapshot.equity.coerceAtLeast(1.0)).toFloat().coerceIn(0.04f, 1f) else 0.04f, pnlColor, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountMetricTile("Active Trades", snapshot.activeTrades.toString(), "${snapshot.activeOrders} live orders", (snapshot.activeTrades / 8f).coerceIn(0.04f, 1f), IndigoAccent, Modifier.weight(1f))
                AccountMetricTile("Margin Used", liveMoneyText(snapshot.margin, hasAccount || snapshot.activeTrades > 0), "Level ${String.format(Locale.US, "%.1f%%", snapshot.marginLevel)}", if (snapshot.equity > 0.0) (snapshot.margin / snapshot.equity).toFloat().coerceIn(0.04f, 1f) else 0.04f, IndigoAccent, Modifier.weight(1f))
            }
            LiveTradeStatePanel(snapshot)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Open Risk", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (snapshot.activeTrades > 0) "${moneyLargeText(snapshot.openRisk)} • ${String.format(Locale.US, "%.2f%%", snapshot.openRiskPct)}" else "NO OPEN RISK", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(riskMeter)
                            .fillMaxHeight()
                            .background(if (snapshot.openRiskPct >= 5.0) RoseError else EmeraldSuccess, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTradeStatePanel(snapshot: PaperTradingAccountSnapshot) {
    val pnl = snapshot.currentTradePnl
    val pnlColor = if (pnl == null) Color.Gray else if (pnl >= 0.0) EmeraldSuccess else RoseError
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("CURRENT LIVE TRADE", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(snapshot.currentTradeSymbol ?: "NO OPEN TRADE", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(
                    color = pnlColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, pnlColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = snapshot.currentTradeSide?.uppercase(Locale.US) ?: "FLAT",
                        color = pnlColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TradeMiniStat("Price", priceText(snapshot.currentTradePrice), "${signedNumberText(snapshot.currentTradePriceChange)} • ${signedPercentText(snapshot.currentTradePriceChangePct)}", if ((snapshot.currentTradePriceChange ?: 0.0) >= 0.0) EmeraldSuccess else RoseError, Modifier.weight(1f))
                TradeMiniStat("Trade P/L", pnl?.let { signedMoneyText(it) } ?: "WAITING", signedPercentText(snapshot.currentTradePnlPct), pnlColor, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entry ${priceText(snapshot.currentTradeEntryPrice)}", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Size ${volumeText(snapshot.currentTradeVolume)}", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tick ${formatMillisTime(snapshot.currentQuoteUpdatedMillis)}", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TradeMiniStat(
    label: String,
    value: String,
    caption: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AccountMetricTile(
    label: String,
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
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(meter.coerceIn(0.04f, 1f))
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

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
    audits: List<AuditRecord>,
    snapshot: PaperTradingAccountSnapshot
) {
    val audited = audits.count { it.audited }
    val pending = (audits.size - audited).coerceAtLeast(0)
    val disciplineScore = if (audits.isEmpty()) 1f else (audited.toFloat() / audits.size.toFloat()).coerceIn(0f, 1f)
    InfoBox(modifier = modifier, height = 150.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("JOURNAL SUMMARY", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$pending pending audits", color = if (pending > 0) Color(0xFFF59E0B) else EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Text(snapshot.balanceHistoryCount.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            PreMoveScale("Discipline Score", disciplineScore, "Open", "Reviewed", if (disciplineScore >= 0.7f) EmeraldSuccess else Color(0xFFF59E0B))
            Text("Realized ${signedMoneyText(snapshot.realizedPnl)} • ${audits.size} audit records", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                IntelligenceTile("3 Entry Timing", entryWindow, "Quality ${percentText(entryQuality)}", entryQuality, EmeraldSuccess, Modifier.weight(1f))
                IntelligenceTile("4 Exit Logic", exitPlan, "Pressure ${percentText(exitPressure)}", exitPressure, if (exitPressure >= 0.62f) RoseError else IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("5 Position Size", percentText(positionScale), "${riskPercentText(riskPct)} • ${moneyText(riskAmount)}", positionScale, EmeraldSuccess, Modifier.weight(1f))
                IntelligenceTile("6 Correlation", correlationRegime, correlationWarning, correlationRisk, if (correlationRisk >= 0.7f) RoseError else IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("7 Regime", "Persist ${percentText(persistence)}", "Shift ${percentText(transition)}", persistence, IndigoAccent, Modifier.weight(1f))
                IntelligenceTile("8 Structure", structuralLabel, "Pressure ${percentText(structural)}", structural, if (structural >= 0.72f) RoseError else IndigoAccent, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntelligenceTile("9 Direction", direction, "Score ${directionScoreText(directionalRaw)} • Conf ${percentText(directionConfidence)}", directionalScale, if (directionalRaw < -0.05f) RoseError else if (directionalRaw > 0.05f) EmeraldSuccess else Color.White, Modifier.weight(1f))
                IntelligenceTile("10 Live / TF", timeframe, "$liveStatus • $liveCount ticks", if (liveStatus == "OFFLINE") 0.12f else mtfAlignment, if (liveStatus == "OFFLINE") RoseError else IndigoAccent, Modifier.weight(1f))
            }
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
                    viewModel.navigateTo(com.asc.markets.data.AppView.INTELLIGENCE_STREAM)
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
                Text(signal.journal_label ?: signal.portfolio_decision_label ?: "pre-move candidate", color = SlateText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SignalStrengthMiniChart(score = ignition, color = color, modifier = Modifier.width(72.dp).height(30.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(percentText(score), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("conf", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SignalStrengthMiniChart(score: Float, color: Color, modifier: Modifier = Modifier) {
    val clamped = score.coerceIn(0f, 1f)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📊", fontSize = DashboardFontSizes.emojiIcon)
                Text(
                    "Raw Feed >",
                    color = Color.White,
                    fontSize = DashboardFontSizes.valueLarge,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
            }
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
            events.forEach { event ->
                CommandCenterRawFeedRow(event)
            }
        }
    }
}

@Composable
private fun BackendStatusPill(isConnected: Boolean?) {
    val color = when (isConnected) {
        true -> EmeraldSuccess
        false -> RoseError
        null -> Color.Gray
    }
    val label = when (isConnected) {
        true -> "CONNECTED"
        false -> "OFFLINE"
        null -> "UNKNOWN"
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
    val isCritical = event.priority == com.asc.markets.data.ImpactPriority.CRITICAL
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(18.dp).background(if (isCritical) RoseError.copy(alpha = 0.18f) else IndigoAccent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(7.dp).background(if (isCritical) RoseError else IndigoAccent, CircleShape))
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.US).format(Date(event.datetimeUtc)),
                color = Color(0xFF8B8B8B),
                fontSize = DashboardFontSizes.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = event.title,
            color = Color.White,
            fontSize = DashboardFontSizes.valueMediumLarge,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
            fontFamily = InterFontFamily,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExecutionQueueWidget(modifier: Modifier, ready: Int, blocked: Int, nextSignal: String) {
    val total = (ready + blocked).coerceAtLeast(1)
    val readyShare = ready.toFloat() / total.toFloat()
    InfoBox(modifier = modifier, height = 124.dp) {
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
    InfoBox(modifier = modifier, height = 124.dp) {
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
    auditCount: Int
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
                TelemetryStatusCard("AUDIT", auditCount.toString(), "journal trail", (auditCount / 20f).coerceIn(0.10f, 1f), Color.White, Modifier.weight(1f))
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

@Composable
private fun QuickActionsWidget(viewModel: ForexViewModel, isLoading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(if (isLoading) "RUNNING..." else "RUN PIPELINE", Icons.Default.PlayArrow, Modifier.weight(1f)) {
                if (!isLoading) viewModel.runAiPipelineNow()
            }
            ActionButton("REFRESH", Icons.Default.Refresh, Modifier.weight(1f)) {
                if (!isLoading) viewModel.refreshAiDeploymentsNow()
            }
            ActionButton("HEALTH", Icons.Default.Wifi, Modifier.weight(1f)) {
                if (!isLoading) viewModel.checkAiHealthNow()
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinkButton("JOURNAL", Modifier.weight(1f)) { viewModel.navigateTo(com.asc.markets.data.AppView.TRADE) }
            LinkButton("SETTINGS", Modifier.weight(1f)) { viewModel.navigateTo(com.asc.markets.data.AppView.SETTINGS) }
            LinkButton("AI CHAT", Modifier.weight(1f)) { viewModel.navigateTo(com.asc.markets.data.AppView.CHAT) }
        }
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
    // Identify assets with sweep or displacement signals from the NEW_ASC logic
    val highPriorityAssets = signals.filter { 
        val reason = it.portfolio_decision_reason?.uppercase() ?: ""
        reason.contains("SWEEP") || reason.contains("DISPLACEMENT") || reason.contains("CHOCH") || reason.contains("BOS")
    }.take(4)

    InfoBox(minHeight = 160.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LIQUIDITY RADAR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("Pre-move liquidity sweeps & displacement", color = SlateText, fontSize = 10.sp)
                }
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("DETERMINISTIC", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            if (highPriorityAssets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No high-probability liquidity sweeps detected.", color = SlateText, fontSize = 11.sp)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    highPriorityAssets.forEach { signal ->
                        LiquidityAssetCard(signal, Modifier.weight(1f))
                    }
                }
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

private fun equityMeter(snapshot: PaperTradingAccountSnapshot): Float {
    val baseline = kotlin.math.abs(snapshot.balance).coerceAtLeast(1.0)
    return (snapshot.equity / baseline).toFloat().coerceIn(0.04f, 1f)
}

private fun freeMarginMeter(snapshot: PaperTradingAccountSnapshot): Float {
    val baseline = kotlin.math.abs(snapshot.equity).coerceAtLeast(1.0)
    return (snapshot.freeMargin / baseline).toFloat().coerceIn(0.04f, 1f)
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
private fun LinkButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(32.dp).clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
