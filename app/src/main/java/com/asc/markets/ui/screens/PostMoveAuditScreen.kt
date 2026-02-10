
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

private fun buildExpandedAnalyticalContext(entry: com.asc.markets.data.AuditRecord, displayedNode: String): String {
    val timeText = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.timeUtc))
    return buildString {
        append(entry.reasoning.trim())
        append("\n\n")
        append("Headline: ${entry.headline}\n")
        append("Assets: ${entry.assets}\n")
        append("Impact: ${entry.impact} — Confidence: ${entry.confidence}%\n")
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

    val audits by viewModel.auditRecords.collectAsState()
    // Apply ActiveAssetContext filtering: if not ALL, show only audits referencing the active asset
    val auditsForDisplay = remember(audits, assetCtx) {
        if (assetCtx == com.asc.markets.state.AssetContext.ALL) audits else audits.filter { entry ->
            entry.assets.contains(assetCtx.name, true)
        }
    }

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
                            Text("POST-MOVE AUDIT", color = Color.White, style = TerminalTypography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily))
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
                                .padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }, bottomBar = {
            Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "THE NOTIFICATION LEDGER IS READ-ONLY. ANALYTICAL STATE ADJUSTMENTS AND EXECUTION COMMANDS MUST BE ROUTED THROUGH THE PRIMARY TERMINAL NODES.",
                    color = SlateText,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(10.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }, content = { paddingValues ->
            // Ledger list (Safe Set)
                LazyColumn(state = listState, modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)) {
                items(auditsForDisplay, key = { it.id }) { entry ->
                    val isExpanded = expanded[entry.id] ?: false
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                        color = PureBlack,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                // Removed left icon; keep small horizontal gap for visual breathing
                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    // Tag pills row (pair, impact, status)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(entry.assets, color = SlateText, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(entry.impact, color = when (entry.impact) { "CRITICAL" -> RoseError; "INFO" -> SlateText.copy(alpha = 0.7f); else -> IndigoAccent }, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(entry.status.uppercase(), color = if (entry.status.equals("UPCOMING", true)) Color(0xFFB06A00) else Color(0xFF0F6F52), modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Confidence small (reduced)
                                    // Color confidence by range: >=60 green, <=40 red, else yellow
                                    val confidenceColor = when {
                                        try { entry.confidence.toInt() } catch (e: Throwable) { null }?.let { it >= 60 } == true -> EmeraldSuccess
                                        try { entry.confidence.toInt() } catch (e: Throwable) { null }?.let { it <= 40 } == true -> RoseError
                                        else -> Color(0xFFFFC107)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("[ ", color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily))
                                        Text("${entry.confidence}%", color = confidenceColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                                        Text(" ] confidence", color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily))
                                    }
                                    Spacer(modifier = Modifier.height(35.dp))

                                    // Headline (use Inter font parity with MacroIntel)
                                    // Preserve original headline casing exactly as stored in the data
                                    Text(entry.headline, color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, fontFamily = InterFontFamily), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }

                                // Time with blinking indicator directly under the time (aligned with EUR/USD padding)
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                                    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                                    // Center the time and dot together, but keep the whole block right-aligned
                                    Box(modifier = Modifier.wrapContentWidth()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(fmt.format(Instant.ofEpochMilli(entry.timeUtc)), color = SlateText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                            if (entry.status.equals("ACTIVE", true)) {
                                                val infiniteTransition = rememberInfiniteTransition()
                                                val blinkAlpha by infiniteTransition.animateFloat(
                                                    initialValue = 1f,
                                                    targetValue = 0.3f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(durationMillis = 1000, easing = LinearEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF3B30).copy(alpha = blinkAlpha), shape = CircleShape))
                                            }
                                        }
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))

                            // Bottom row: view context right; simplified expansion block aligned with main content
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (!isExpanded) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { expanded[entry.id] = true }) {
                                        Text("VIEW CONTEXT ›", color = IndigoAccent, style = TerminalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, letterSpacing = 0.08.em))
                                    }
                                } else {
                                    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                                            Text("ANALYTICAL REASONING", color = SlateText, style = TerminalTypography.labelSmall.copy(letterSpacing = 1.sp, fontFamily = InterFontFamily))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            // Big reasoning text (use Inter font to match pre-expansion)
                                            // Preserve analytical context casing exactly as stored
                                            Text(entry.reasoning, color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, fontFamily = InterFontFamily))

                                            Spacer(modifier = Modifier.height(12.dp))
                                            // Audit trace log header
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("AUDIT TRACE LOG", color = SlateText, style = TerminalTypography.labelSmall.copy(fontFamily = InterFontFamily))
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Source node + Integrity check boxes
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                // ARCH_RULE: Use the label "Insight Node" here because this UI element
                                                // represents an explanatory/read-only source. Do NOT reuse "Insight Node"
                                                // for authoritative or execution nodes elsewhere in the system.
                                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF070707), modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(6.dp)) {
                                                            Text("Insight Node", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text("Insight Node", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                                        }
                                                    }

                                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF070707), modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(6.dp)) {
                                                            Text("INTEGRITY CHECK", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text("VERIFIED", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                                        }
                                                    }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Analytical context box (InfoBox) inside the shared curved outer Surface
                                            InfoBox(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text("ANALYTICAL CONTEXT", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    // Use the same displayed node label as the UI (Insight Node)
                                                    Text(buildExpandedAnalyticalContext(entry, "Insight Node"), color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, fontFamily = InterFontFamily), fontStyle = FontStyle.Italic)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Action buttons: Export, Mark Audited, External
                                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Button(onClick = {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val pdf = exportAuditPdf(context, entry)
                                                        if (pdf != null) {
                                                            withContext(Dispatchers.Main) { sharePdf(context, pdf) }
                                                        } else {
                                                            withContext(Dispatchers.Main) { Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show() }
                                                        }
                                                    }
                                                }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                                    Icon(Icons.Default.FileCopy, contentDescription = null, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("EXPORT AUDIT PDF", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                                }

                                                Button(onClick = { viewModel.markAuditRecordAudited(entry.id); Toast.makeText(context, "Marked audited", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("MARK AUDITED", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                                                }

                                                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))) }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = Color.White)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Footer: TS_MICRO and SEQ_ID
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("TS_MICRO: ${entry.timeUtc}", color = SlateText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                                Text("SEQ_ID: ${entry.id.take(3).uppercase()}", color = SlateText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Divider(color = Color.White.copy(alpha = 0.04f))

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("LOG::${entry.id.take(3).uppercase()}", color = SlateText.copy(alpha = 0.6f), style = TerminalTypography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium))
                                                TextButton(onClick = { expanded[entry.id] = false }) { Text("MINIMIZE CONTEXT", color = IndigoAccent, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold) }
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
