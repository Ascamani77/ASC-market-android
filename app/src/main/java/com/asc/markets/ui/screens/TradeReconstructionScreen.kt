package com.asc.markets.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.BinanceDataStore
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.data.label
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.EmeraldSuccess
import com.asc.markets.ui.theme.HairlineBorder
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.RoseError
import com.asc.markets.ui.theme.SlateText
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TradeReconstructionScreen(viewModel: ForexViewModel = viewModel()) {
    var selectedCase by remember { mutableStateOf<PostMoveAuditCase?>(null) }
    var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
    val auditRecords by viewModel.auditRecords.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    val binanceTimedHistory by BinanceDataStore.timedPriceHistory.collectAsState()
    val fallbackTimedHistory by com.asc.markets.data.CombinedFallbackDataStore.timedPriceHistory.collectAsState()
    val timedHistory = remember(marketTimedHistory, binanceTimedHistory, fallbackTimedHistory) {
        marketTimedHistory + binanceTimedHistory + fallbackTimedHistory
    }
    val cases = remember(closedTrades, auditRecords, candidates, timedHistory) {
        PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
    }

    LaunchedEffect(viewModel.tradeHistoryRepository) {
        closedTrades = withContext(Dispatchers.IO) {
            viewModel.tradeHistoryRepository?.getLast100Trades().orEmpty()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        if (selectedCase == null) {
            DynamicReconstructionListView(cases = cases, onAuditSelected = { selectedCase = it })
        } else {
            DynamicReconstructionDetailView(case = selectedCase!!, onClose = { selectedCase = null })
        }
    }
}

@Composable
private fun DynamicReconstructionListView(cases: List<PostMoveAuditCase>, onAuditSelected: (PostMoveAuditCase) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 120.dp)
    ) {
        item {
            InfoBox(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).background(Color(0xFF0F3A7D), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("ðŸ“‹", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("POST-MOVE\nRECONSTRUCTION", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("OUTCOME-BASED TRADE AND SIGNAL FORENSICS", color = SlateText, fontSize = 11.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                }
            }
        }
        item {
            Text("POST-MOVE CASE BUFFER", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
        if (cases.isEmpty()) {
            item {
                InfoBox(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("NO RECONSTRUCTION CASES", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text("Closed trades and AI decisions will appear here once post-signal market history is available.", color = SlateText, fontSize = 12.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                    }
                }
            }
        } else {
            items(cases, key = { it.id }) { auditCase ->
                DynamicReconstructionCaseCard(case = auditCase, onClick = { onAuditSelected(auditCase) })
            }
        }
    }
}

@Composable
private fun DynamicReconstructionCaseCard(case: PostMoveAuditCase, onClick: () -> Unit) {
    InfoBox(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                PairFlags(case.symbol, 40)
                Column {
                    Text(case.symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(case.status, color = reconstructionStatusColor(case), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(case.source.label(), color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(case.modelAccuracyScore?.let { "$it%" } ?: "PENDING", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("â¯", color = SlateText, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun DynamicReconstructionDetailView(case: PostMoveAuditCase, onClose: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 120.dp)
    ) {
        item {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = PureBlack), shape = RoundedCornerShape(10.dp)) {
                Text("â† BACK", color = Color.White, fontFamily = InterFontFamily)
            }
        }
        item {
            InfoBox(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PairFlags(case.symbol, 44)
                        Column {
                            Text("POST-MOVE RECONSTRUCTION", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            Text("${case.symbol} â€¢ ${case.status} â€¢ ${case.source.label()}", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }
                    Divider(color = HairlineBorder)
                    DynamicReconstructionMetricGrid(case)
                }
            }
        }
        item { DynamicReconstructionTextBlock("ORIGINAL THESIS", case.thesis) }
        item { DynamicReconstructionTextBlock("POST-MOVE OUTCOME", case.postMoveOutcome) }
        item {
            InfoBox(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RECONSTRUCTION TRACE", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    case.reconstructionLines.forEach { line ->
                        Text(line, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = InterFontFamily)
                    }
                    if (case.failureReason != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("FAILURE REASON", color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Text(case.failureReason, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }
        item {
            Button(
                onClick = { exportPostMoveReconstructionPdf(context, case) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("EXPORT RECONSTRUCTION PDF", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DynamicReconstructionMetricGrid(case: PostMoveAuditCase) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DynamicReconstructionMetric("ENTRY", PostMoveAuditStore.formatPrice(case.entryPrice), Modifier.weight(1f))
            DynamicReconstructionMetric("EXIT", PostMoveAuditStore.formatPrice(case.exitPrice), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DynamicReconstructionMetric("MOVE", PostMoveAuditStore.formatPercent(case.actualMovePct), Modifier.weight(1f))
            DynamicReconstructionMetric("SCORE", case.modelAccuracyScore?.let { "$it%" } ?: "PENDING", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DynamicReconstructionMetric("MFE", PostMoveAuditStore.formatPercent(case.maxFavorableExcursionPct), Modifier.weight(1f))
            DynamicReconstructionMetric("MAE", PostMoveAuditStore.formatPercent(case.maxAdverseExcursionPct), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DynamicReconstructionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun DynamicReconstructionTextBlock(title: String, body: String) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Text(body, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = InterFontFamily)
        }
    }
}

private fun reconstructionStatusColor(case: PostMoveAuditCase): Color {
    return when {
        case.targetHit == true -> EmeraldSuccess
        case.invalidationHit == true -> RoseError
        else -> IndigoAccent
    }
}

private fun exportPostMoveReconstructionPdf(context: Context, case: PostMoveAuditCase) {
    try {
        val fileName = "PostMoveReconstruction_${case.id}_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pdfFile = File(downloadsDir, fileName)
        val writer = PdfWriter(pdfFile)
        val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = Document(pdfDocument)
        val timeText = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(case.timestamp))

        document.add(Paragraph("POST-MOVE RECONSTRUCTION").setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Case: ${case.id}").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph(" "))

        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Symbol").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(case.symbol)))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Status").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(case.status)))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Source").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(case.source.label())))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Time").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(timeText)))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Entry / Exit").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("${PostMoveAuditStore.formatPrice(case.entryPrice)} / ${PostMoveAuditStore.formatPrice(case.exitPrice)}")))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Move / Score").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("${PostMoveAuditStore.formatPercent(case.actualMovePct)} / ${case.modelAccuracyScore?.let { "$it%" } ?: "PENDING"}")))
        document.add(table)

        document.add(Paragraph(" "))
        document.add(Paragraph("ORIGINAL THESIS").setBold())
        document.add(Paragraph(case.thesis))
        document.add(Paragraph("POST-MOVE OUTCOME").setBold())
        document.add(Paragraph(case.postMoveOutcome))
        document.add(Paragraph("RECONSTRUCTION TRACE").setBold())
        case.reconstructionLines.forEach { line -> document.add(Paragraph(line).setFontSize(10f)) }
        case.failureReason?.let {
            document.add(Paragraph("FAILURE REASON").setBold().setFontColor(ColorConstants.RED))
            document.add(Paragraph(it))
        }

        document.close()
        val uri = Uri.fromFile(pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Reconstruction PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
