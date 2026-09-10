package com.asc.markets.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateIntAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.res.painterResource
import com.asc.markets.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.asc.markets.ui.theme.*
import com.asc.markets.ui.components.InfoBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.OpenInNew
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import com.asc.markets.data.AuditRecord
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditSource
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.data.label
import com.asc.markets.data.trade.TradeEntity

fun exportAuditPdf(context: Context, entry: PostMoveAuditCase): File? {
    return try {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 12f }
        var y = 40f
        canvas.drawText("Post-Move Audit", 40f, y, paint); y += 24f
        canvas.drawText("Symbol: ${entry.symbol}", 40f, y, paint); y += 18f
        canvas.drawText("Status: ${entry.status}  Score: ${entry.modelAccuracyScore ?: 0}%", 40f, y, paint); y += 18f
        canvas.drawText("Source: ${entry.source}", 40f, y, paint); y += 18f
        val timeText = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.timestamp))
        canvas.drawText("Time: $timeText", 40f, y, paint); y += 18f
        canvas.drawText("Node: ${entry.nodeId}", 40f, y, paint); y += 18f
        canvas.drawText("Regime: ${entry.regimeStack.ifBlank { "N/A" }}", 40f, y, paint); y += 18f
        canvas.drawText("Integrity: ${entry.integrityHash}", 40f, y, paint); y += 24f

        val chunkSize = 90
        "${entry.thesis}\n${entry.postMoveOutcome}".chunked(chunkSize).forEach { line ->
            canvas.drawText(line, 40f, y, paint)
            y += 16f
        }

        doc.finishPage(page)
        val outFile = File(context.cacheDir, "audit_${entry.id}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

fun sharePdf(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share Audit PDF"))
    } catch (t: Throwable) {
        t.printStackTrace()
        Toast.makeText(context, "Unable to share PDF", Toast.LENGTH_SHORT).show()
    }
}

fun exportCsv(context: Context, cases: List<PostMoveAuditCase>): File? {
    return try {
        val header = PostMoveAuditStore.exportCsvHeader()
        val rows = cases.joinToString("\n") { PostMoveAuditStore.exportCsvRow(it) }
        val content = "$header\n$rows"
        val outFile = File(context.cacheDir, "audit_export_${System.currentTimeMillis()}.csv")
        outFile.writeText(content)
        outFile
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

fun shareCsv(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share Audit CSV"))
    } catch (t: Throwable) {
        t.printStackTrace()
        Toast.makeText(context, "Unable to share CSV", Toast.LENGTH_SHORT).show()
    }
}

fun buildExpandedAnalyticalContext(entry: PostMoveAuditCase, displayedNode: String): String {
    val timeText = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.timestamp))
    return buildString {
        append(entry.thesis.trim())
        append("\n\n")
        append("Outcome: ${entry.postMoveOutcome}\n")
        append("Symbol: ${entry.symbol}\n")
        append("Status: ${entry.status} — Score: ${entry.modelAccuracyScore?.let { "$it%" } ?: "pending"}\n")
        append("Regime: ${entry.regimeStack.ifBlank { "N/A" }}\n")
        append("Node: $displayedNode — Integrity: ${entry.integrityHash}\n")
        append("Time: $timeText")
    }
}


@Composable
fun PostMoveAuditScreen(viewModel: ForexViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val headerTopPad = (configuration.screenHeightDp * 0.02f).dp

    val filterState = remember { mutableStateOf("ALL") }
    val filterDirection = remember { mutableStateOf<String?>(null) }
    val filterOutcome = remember { mutableStateOf<String?>(null) }
    val showFilters = remember { mutableStateOf(false) }
    val assetCtx by com.asc.markets.state.AssetContextStore.context.collectAsState()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
    val auditRecords by viewModel.auditRecords.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    val timedHistory = remember(marketTimedHistory) { marketTimedHistory }
    val allCases = remember(closedTrades, auditRecords, candidates, timedHistory) {
        PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
    }

    val auditsForDisplay = remember(allCases, assetCtx, filterState.value, filterDirection.value, filterOutcome.value) {
        val assetFiltered = if (assetCtx == com.asc.markets.state.AssetContext.ALL) {
            allCases
        } else {
            allCases.filter { entry -> entry.symbol.contains(assetCtx.name, true) }
        }
        val sourceFiltered = when (filterState.value) {
            "TRADES" -> assetFiltered.filter { it.source == PostMoveAuditSource.CLOSED_TRADE }
            "AI OUTCOMES" -> assetFiltered.filter { it.source == PostMoveAuditSource.AI_DECISION }
            "TARGET HIT" -> assetFiltered.filter { it.targetHit == true }
            "INVALIDATED" -> assetFiltered.filter { it.invalidationHit == true }
            "UNRESOLVED" -> assetFiltered.filter { it.status == "UNRESOLVED" }
            else -> assetFiltered
        }
        sourceFiltered.filter { case ->
            val directionMatch = filterDirection.value == null || case.direction == filterDirection.value
            val outcomeMatch = filterOutcome.value == null ||
                (filterOutcome.value == "WIN" && case.targetHit == true) ||
                (filterOutcome.value == "LOSS" && case.invalidationHit == true)
            directionMatch && outcomeMatch
        }
    }

    val stats = remember(auditsForDisplay) { PostMoveAuditStore.computeStats(auditsForDisplay) }

    LaunchedEffect(viewModel.tradeHistoryRepository) {
        closedTrades = withContext(Dispatchers.IO) {
            viewModel.tradeHistoryRepository?.getLast100Trades().orEmpty()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
        val showMainHeader = rememberSaveable { mutableStateOf(true) }
        val listState = rememberLazyListState()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                PostMoveAuditHeader(
                    showMainHeader, filterState, viewModel, showFilters,
                    filterDirection, filterOutcome,
                    onExportCsv = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val csv = exportCsv(context, auditsForDisplay)
                            if (csv != null) withContext(Dispatchers.Main) { shareCsv(context, csv) }
                            else withContext(Dispatchers.Main) { Toast.makeText(context, "CSV export failed", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    onMarkAllReviewed = {
                        viewModel.markAllAuditRecordsAudited()
                        Toast.makeText(context, "All records marked reviewed", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            bottomBar = {
                if (showFilters.value) {
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0A0A0A)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DIRECTION & OUTCOME", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val chips = listOf(
                                    Triple("ALL", filterDirection.value == null && filterOutcome.value == null) { filterDirection.value = null; filterOutcome.value = null },
                                    Triple("LONG", filterDirection.value == "LONG") { filterDirection.value = if (filterDirection.value == "LONG") null else "LONG" },
                                    Triple("SHORT", filterDirection.value == "SHORT") { filterDirection.value = if (filterDirection.value == "SHORT") null else "SHORT" },
                                    Triple("WINS", filterOutcome.value == "WIN") { filterOutcome.value = if (filterOutcome.value == "WIN") null else "WIN" },
                                    Triple("LOSSES", filterOutcome.value == "LOSS") { filterOutcome.value = if (filterOutcome.value == "LOSS") null else "LOSS" }
                                )
                                chips.forEach { (label, selected, action) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (selected) IndigoAccent else Color(0xFF17171D))
                                            .border(1.dp, if (selected) IndigoAccent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                            .clickable { action() }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(label, color = if (selected) Color.White else SlateText, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                    }
                }
            }, content = { paddingValues ->
                if (auditsForDisplay.isEmpty()) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Surface(modifier = Modifier.size(72.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF17171D), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF333340), modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("NO AUDIT RECORDS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (filterState.value != "ALL" || filterDirection.value != null || filterOutcome.value != null)
                                "No records match the current filters. Try adjusting your filter criteria."
                            else
                                "Audit records will appear here once trades close or AI decisions have enough post-signal market data to evaluate.",
                            color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 48.dp),
                            lineHeight = 18.sp
                        )
                    }
                } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = paddingValues) {
                    // Statistics Summary
                    item {
                        StatsSummaryCard(stats, auditsForDisplay)
                    }

                    // Regime Breakdown (if more than 1 regime)
                    if (stats.regimeBreakdown.size > 1) {
                        item {
                            RegimeBreakdownCard(stats.regimeBreakdown)
                        }
                    }

                    items(auditsForDisplay, key = { it.id }) { entry ->
                        Spacer(modifier = Modifier.height(4.dp))
                        PostMoveAuditItem(entry = entry, expanded = expanded, viewModel = viewModel, context = context)
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }

        })
    }
}

@Composable
private fun StatsSummaryCard(stats: com.asc.markets.data.AuditStats, cases: List<PostMoveAuditCase>) {
    val context = LocalContext.current
    InfoBox(minHeight = 180.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF101010)) {
                    Box(contentAlignment = Alignment.Center) { Text("\uD83D\uDCCA", color = Color.White, fontSize = 20.sp) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("AUDIT", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("STATISTICS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("POST-MOVE AUDIT LEDGER", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Row 1: Win Rate & Total P&L
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WIN RATE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("${stats.winRate}%", color = when { stats.winRate >= 60 -> EmeraldSuccess; stats.winRate >= 45 -> IndigoAccent; else -> RoseError }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${stats.winningRecords} / ${stats.totalRecords}", color = SlateText, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("TOTAL P&L", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(PostMoveAuditStore.formatSigned(stats.totalPnl), color = if (stats.totalPnl >= 0) EmeraldSuccess else RoseError, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Profit Factor & Expectancy
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PROFIT FACTOR", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        stats.profitFactor?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A",
                        color = when { (stats.profitFactor ?: 0.0) >= 2.0 -> EmeraldSuccess; (stats.profitFactor ?: 0.0) >= 1.0 -> IndigoAccent; else -> RoseError },
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("EXPECTANCY", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        stats.expectancy?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A",
                        color = if ((stats.expectancy ?: 0.0) >= 0) EmeraldSuccess else RoseError,
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Avg R-Multiple & Max Consecutive Losses
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AVG R-MULTIPLE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        stats.avgRMultiple?.let { String.format(Locale.US, "%+.2fR", it) } ?: "N/A",
                        color = if ((stats.avgRMultiple ?: 0.0) >= 0) EmeraldSuccess else RoseError,
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("MAX CONSEC LOSSES", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("${stats.maxConsecutiveLosses}", color = if (stats.maxConsecutiveLosses > 3) RoseError else Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 4: Best/Worst Trade & Avg Duration
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BEST / WORST", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${stats.bestTradePnl?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A"} / ${stats.worstTradePnl?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A"}",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("AVG DURATION", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(PostMoveAuditStore.formatDuration(stats.avgTradeDurationMs), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Source breakdown
            if (stats.sourceBreakdown.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    stats.sourceBreakdown.forEach { (source, count) ->
                        Column {
                            Text(source, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("$count", color = IndigoAccent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegimeBreakdownCard(regimeBreakdown: Map<String, com.asc.markets.data.RegimeStats>) {
    InfoBox(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("REGIME PERFORMANCE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            regimeBreakdown.entries.sortedByDescending { it.value.count }.forEach { (regime, rs) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(regime, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${rs.count} trades", color = SlateText, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${rs.winRate}%", color = when { rs.winRate >= 60 -> EmeraldSuccess; rs.winRate >= 45 -> IndigoAccent; else -> RoseError }, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("WR", color = SlateText, fontSize = 8.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(PostMoveAuditStore.formatSigned(rs.totalPnl), color = if (rs.totalPnl >= 0) EmeraldSuccess else RoseError, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("P&L", color = SlateText, fontSize = 8.sp)
                    }
                }
                if (regime != regimeBreakdown.keys.last()) Divider(color = Color.White.copy(alpha = 0.03f), modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
