
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditSource
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.PreMoveIntelligenceStore
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

fun buildExpandedAnalyticalContext(entry: PostMoveAuditCase, displayedNode: String): String {
    val timeText = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.timestamp))
    return buildString {
        append(entry.thesis.trim())
        append("\n\n")
        append("Outcome: ${entry.postMoveOutcome}\n")
        append("Symbol: ${entry.symbol}\n")
        append("Status: ${entry.status} — Score: ${entry.modelAccuracyScore?.let { "$it%" } ?: "pending"}\n")
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
    val assetCtx by com.asc.markets.state.AssetContextStore.context.collectAsState()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
    val auditRecords by viewModel.auditRecords.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    val binanceTimedHistory by BinanceDataStore.timedPriceHistory.collectAsState()
    val fallbackTimedHistory by com.asc.markets.data.CombinedFallbackDataStore.timedPriceHistory.collectAsState()
    val timedHistory = remember(marketTimedHistory, binanceTimedHistory, fallbackTimedHistory) {
        marketTimedHistory + binanceTimedHistory + fallbackTimedHistory
    }
    val allCases = remember(closedTrades, auditRecords, candidates, timedHistory) {
        PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
    }
    val auditsForDisplay = remember(allCases, assetCtx, filterState.value) {
        val assetFiltered = if (assetCtx == com.asc.markets.state.AssetContext.ALL) {
            allCases
        } else {
            allCases.filter { entry -> entry.symbol.contains(assetCtx.name, true) }
        }
        when (filterState.value) {
            "CLOSED TRADE" -> assetFiltered.filter { it.source == PostMoveAuditSource.CLOSED_TRADE }
            "AI OUTCOME" -> assetFiltered.filter { it.source == PostMoveAuditSource.AI_DECISION }
            "TARGET HIT" -> assetFiltered.filter { it.targetHit == true }
            "INVALIDATED" -> assetFiltered.filter { it.invalidationHit == true }
            "UNRESOLVED" -> assetFiltered.filter { it.status == "UNRESOLVED" }
            else -> assetFiltered
        }
    }

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
            topBar = { PostMoveAuditHeader(showMainHeader, filterState, viewModel) },
            bottomBar = {
            Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "THE NOTIFICATION LEDGER IS READ-ONLY. ANALYTICAL STATE ADJUSTMENTS AND EXECUTION COMMANDS MUST BE ROUTED THROUGH THE PRIMARY TERMINAL NODES.",
                    color = SlateText,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(10.dp),
                    fontFamily = InterFontFamily
                )
            }
        }, content = { paddingValues ->
                if (auditsForDisplay.isEmpty()) {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Text("No post-move audit records to display.", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Records appear after closed trades or AI decisions have enough post-signal market history.", color = SlateText, fontSize = 11.sp)
                    }
                } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = paddingValues) {
                items(auditsForDisplay, key = { it.id }) { entry ->
                    Spacer(modifier = Modifier.height(4.dp))
                    PostMoveAuditItem(entry = entry, expanded = expanded, viewModel = viewModel, context = context)
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                }
            }

        })
    }
}
