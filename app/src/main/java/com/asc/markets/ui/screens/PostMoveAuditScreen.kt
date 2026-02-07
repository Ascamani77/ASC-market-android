
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import com.asc.markets.data.AuditRecord

private fun exportAuditPdf(context: Context, entry: AuditRecord): File? {
    return try {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 12f }
        var y = 40f
        canvas.drawText("Execution Audit", 40f, y, paint); y += 24f
        canvas.drawText("Headline: ${entry.headline}", 40f, y, paint); y += 18f
        canvas.drawText("Impact: ${entry.impact}  Confidence: ${entry.confidence}%", 40f, y, paint); y += 18f
        canvas.drawText("Assets: ${entry.assets}", 40f, y, paint); y += 18f
        val timeText = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.timeUtc))
        canvas.drawText("Time: $timeText", 40f, y, paint); y += 18f
        canvas.drawText("Node: ${entry.nodeId}", 40f, y, paint); y += 18f
        canvas.drawText("Integrity: ${entry.integrityHash}", 40f, y, paint); y += 24f

        // reasoning text (wrap simple)
        val chunkSize = 90
        entry.reasoning.chunked(chunkSize).forEach { line ->
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

private fun sharePdf(context: Context, file: File) {
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


@Composable
fun PostMoveAuditScreen(viewModel: ForexViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val headerTopPad = (configuration.screenHeightDp * 0.02f).dp

    val filterState = remember { mutableStateOf("ALL") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    val audits by viewModel.auditRecords.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
        // collapsing header + sticky submenu pattern
        val showMainHeader = rememberSaveable { mutableStateOf(true) }
        val listState = rememberLazyListState()

        // watch scroll direction to toggle main header visibility
        LaunchedEffect(listState) {
            var previous = 0L
            snapshotFlow { listState.firstVisibleItemIndex.toLong() * 100000L + listState.firstVisibleItemScrollOffset }
                .collect { cur ->
                    if (cur > previous) {
                        // scrolling down -> hide main header
                        showMainHeader.value = false
                    } else if (cur < previous) {
                        // scrolling up -> show main header
                        showMainHeader.value = true
                    }
                    previous = cur
                }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
            // We animate the combined header+submenu together to produce a smooth collapse
            var mainHeaderHeightPx by remember { mutableStateOf(0) }
            var submenuHeightPx by remember { mutableStateOf(0) }

            // When hidden we slide the header up; additionally we shrink the internal header top padding
            val targetOffset = if (showMainHeader.value) 0 else -mainHeaderHeightPx
            val animatedOffset by animateIntAsState(targetValue = targetOffset, animationSpec = tween(180))

            val animatedHeaderTopPad by animateDpAsState(targetValue = if (showMainHeader.value) headerTopPad else 6.dp, animationSpec = tween(180))

            Column(modifier = Modifier.offset { IntOffset(0, animatedOffset) }) {
                // Main local header (measured)
                Surface(color = PureBlack, modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { mainHeaderHeightPx = it.size.height }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = animatedHeaderTopPad), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("POST-MOVE AUDIT", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Row(modifier = Modifier.wrapContentWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { /* toggle search modal */ Toast.makeText(context, "Search", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) }
                            IconButton(onClick = { /* navigate to settings */ Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) }
                            var moreOpen by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { moreOpen = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White) }
                                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                                    DropdownMenuItem(text = { Text("Clear Ledger") }, onClick = { viewModel.clearAuditLedger(); moreOpen = false })
                                    DropdownMenuItem(text = { Text("Mark All Audited") }, onClick = { viewModel.markAllAuditRecordsAudited(); moreOpen = false })
                                }
                            }
                        }
                    }
                }

                // Sticky Sub-Menu (moves together with header for smooth transition)
                val pills = listOf("ALL", "SIMPLE ALERTS", "SMART ALERTS", "NEWS", "STRATEGY", "SYSTEM", "ACCOUNT")
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 8.dp)
                    .onGloballyPositioned { submenuHeightPx = it.size.height }) {
                    pills.forEach { p ->
                        val active = p == filterState.value
                        Surface(color = if (active) DeepBlack else Color.Transparent, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(end = 6.dp)) {
                            Text(p, color = if (active) Color.White else SlateText, modifier = Modifier
                                .clickable { filterState.value = p }
                                .padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }, bottomBar = {
            Surface(color = Color(0xFF08121A), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "THE NOTIFICATION LEDGER IS READ-ONLY. ANALYTICAL STATE ADJUSTMENTS AND EXECUTION COMMANDS MUST BE ROUTED THROUGH THE PRIMARY TERMINAL NODES.",
                    color = SlateText,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }, content = { paddingValues ->
            // Ledger list (Safe Set)
            LazyColumn(state = listState, modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(paddingValues)) {
                items(audits, key = { it.id }) { entry ->
                    val isExpanded = expanded[entry.id] ?: false
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = Color(0xFF071017),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                // Left icon
                                Box(modifier = Modifier.size(56.dp).background(Color(0xFF08121A), shape = RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = IndigoAccent)
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Tag pills row (pair, impact, status)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = Color(0xFF0F2630), shape = RoundedCornerShape(8.dp)) {
                                            Text(entry.assets, color = SlateText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(color = when (entry.impact) { "CRITICAL" -> RoseError; "INFO" -> Color(0xFF142737); else -> IndigoAccent }, shape = RoundedCornerShape(8.dp)) {
                                            Text(entry.impact, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(color = if (entry.status.equals("UPCOMING", true)) Color(0xFFB06A00) else Color(0xFF0F6F52), shape = RoundedCornerShape(8.dp)) {
                                            Text(entry.status.uppercase(), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Confidence large
                                    Text("${entry.confidence}% CONFIDENCE", color = SlateText.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Headline
                                    Text(entry.headline, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }

                                // Time + activity dot
                                Column(horizontalAlignment = Alignment.End) {
                                    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (entry.status.equals("ACTIVE", true)) {
                                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF3B30), shape = RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(fmt.format(Instant.ofEpochMilli(entry.timeUtc)), color = SlateText, fontSize = 12.sp)
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(vertical = 12.dp))

                            // Bottom row: log id left, view context right
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("LOG::${entry.id.take(3).uppercase()}", color = SlateText.copy(alpha = 0.6f), fontSize = 11.sp)
                                if (!isExpanded) {
                                    TextButton(onClick = { expanded[entry.id] = true }) {
                                        Text("VIEW CONTEXT ›", color = IndigoAccent, fontWeight = FontWeight.Black)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Analytical Reasoning", color = SlateText, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(entry.reasoning, color = Color.White, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Source Node: ${entry.nodeId}", color = SlateText, fontSize = 11.sp)
                                        Text("Integrity: ${entry.integrityHash}", color = SlateText, fontSize = 11.sp)
                                        Text("Analytical Context: ${if (entry.headline.contains("LIQUIDITY", true)) "Macro-weighted" else "Structural-weighted"}", color = SlateText, fontSize = 11.sp)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row {
                                            TextButton(onClick = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    val pdf = exportAuditPdf(context, entry)
                                                    if (pdf != null) {
                                                        withContext(Dispatchers.Main) {
                                                            sharePdf(context, pdf)
                                                        }
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }) { Text("Export PDF", color = IndigoAccent) }
                                            TextButton(onClick = { viewModel.markAuditRecordAudited(entry.id); Toast.makeText(context, "Marked audited", Toast.LENGTH_SHORT).show() }) { Text("Mark Audited", color = IndigoAccent) }
                                        }
                                        Row {
                                            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))) }) { Text("External Link", color = IndigoAccent) }
                                            TextButton(onClick = { expanded[entry.id] = false }) { Text("Minimize", color = SlateText) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        })
    }
}
