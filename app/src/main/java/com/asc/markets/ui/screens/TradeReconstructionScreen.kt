package com.asc.markets.ui.screens

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.data.label
import com.asc.markets.data.trade.TradeEntity
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TradeReconstructionScreen(viewModel: ForexViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedCase by remember { mutableStateOf<PostMoveAuditCase?>(null) }
    var closedTrades by remember { mutableStateOf<List<TradeEntity>>(emptyList()) }
    val auditRecords by viewModel.auditRecords.collectAsState()
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val marketTimedHistory by MarketDataStore.timedPriceHistory.collectAsState()
    val timedHistory = remember(marketTimedHistory) { marketTimedHistory }
    val cases = remember(closedTrades, auditRecords, candidates, timedHistory) {
        PostMoveAuditStore.buildCases(closedTrades, auditRecords, candidates, timedHistory)
    }

    val filterDirection = remember { mutableStateOf<String?>(null) }
    val filterOutcome = remember { mutableStateOf<String?>(null) }
    val filterRegime = remember { mutableStateOf<String?>(null) }

    val filteredCases = remember(cases, filterDirection.value, filterOutcome.value, filterRegime.value) {
        cases.filter { case ->
            val directionMatch = filterDirection.value == null || case.direction == filterDirection.value
            val outcomeMatch = filterOutcome.value == null ||
                (filterOutcome.value == "WIN" && case.targetHit == true) ||
                (filterOutcome.value == "LOSS" && case.invalidationHit == true)
            val regimeMatch = filterRegime.value == null || case.regimeStack.equals(filterRegime.value, ignoreCase = true)
            directionMatch && outcomeMatch && regimeMatch
        }
    }

    val stats = remember(filteredCases) { PostMoveAuditStore.computeStats(filteredCases) }

    LaunchedEffect(viewModel.tradeHistoryRepository) {
        closedTrades = withContext(Dispatchers.IO) {
            viewModel.tradeHistoryRepository?.getLast100Trades().orEmpty()
        }
    }

    if (selectedCase != null) {
        DynamicReconstructionDetailView(
            case = selectedCase!!,
            onClose = { selectedCase = null },
            context = context
        )
    } else {
        DynamicReconstructionListView(
            cases = filteredCases,
            onAuditSelected = { selectedCase = it },
            stats = stats,
            filterDirection = filterDirection,
            filterOutcome = filterOutcome,
            filterRegime = filterRegime,
            viewModel = viewModel,
            context = context
        )
    }
}

@Composable
private fun DynamicReconstructionListView(
    cases: List<PostMoveAuditCase>,
    onAuditSelected: (PostMoveAuditCase) -> Unit,
    stats: com.asc.markets.data.AuditStats,
    filterDirection: MutableState<String?>,
    filterOutcome: MutableState<String?>,
    filterRegime: MutableState<String?>,
    viewModel: ForexViewModel,
    context: Context
) {
    val regimes = remember(cases) { cases.map { it.regimeStack }.filter { it.isNotBlank() }.distinct().sorted() }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReconstructionListHeader(
                filterDirection = filterDirection,
                filterOutcome = filterOutcome,
                filterRegime = filterRegime,
                onBack = { viewModel.navigateBack() }
            )

            // Content
            if (cases.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(modifier = Modifier.size(72.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF17171D), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF333340), modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("NO RECONSTRUCTION DATA", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reconstruction cases appear once trades close or AI decisions have post-signal market history for forensic analysis.",
                        color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 48.dp), lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 120.dp)
                ) {
                    // Specialized reconstruction stats
                    item { ReconstructionStatsCard(stats) }

                    // Regime filter chips (if multiple)
                    if (regimes.size > 1) {
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(regimes) { regime ->
                                    val selected = filterRegime.value == regime
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (selected) Color(0xFFFFA500) else Color(0xFF17171D))
                                            .border(1.dp, if (selected) Color(0xFFFFA500) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                            .clickable { filterRegime.value = if (selected) null else regime }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(regime, color = if (selected) Color.White else SlateText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                    }

                    // Section header
                    item {
                        Text("RECONSTRUCTION CASES (${cases.size})", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }

                    // Case cards
                    items(cases, key = { it.id }) { auditCase ->
                        DynamicReconstructionCaseCard(case = auditCase, onClick = { onAuditSelected(auditCase) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReconstructionListHeader(
    filterDirection: MutableState<String?>,
    filterOutcome: MutableState<String?>,
    filterRegime: MutableState<String?>,
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val headerTopPad = (configuration.screenHeightDp * 0.02f).dp

    val activeFilters = filterDirection.value != null || filterOutcome.value != null || filterRegime.value != null

    Column(modifier = Modifier.background(DeepBlack)) {
        Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = headerTopPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("POST-MOVE RECONSTRUCTION", color = Color.White, style = TerminalTypography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily))
                }
                Row(modifier = Modifier.wrapContentWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        filterDirection.value = null
                        filterOutcome.value = null
                        filterRegime.value = null
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = "Clear filters",
                            tint = if (activeFilters) EmeraldSuccess else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter tabs — market overview style (plain labels + white underline on active)
        val tabs = listOf("ALL", "LONG", "SHORT", "WINS", "LOSSES")
        val tabLabels = mapOf(
            "ALL" to "All",
            "LONG" to "Long",
            "SHORT" to "Short",
            "WINS" to "Wins",
            "LOSSES" to "Losses"
        )
        val selectedTab = when {
            filterDirection.value == "LONG" -> "LONG"
            filterDirection.value == "SHORT" -> "SHORT"
            filterOutcome.value == "WIN" -> "WINS"
            filterOutcome.value == "LOSS" -> "LOSSES"
            else -> "ALL"
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs.size) { index ->
                val label = tabs[index]
                val isSelected = label == selectedTab
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable {
                            when (label) {
                                "ALL" -> { filterDirection.value = null; filterOutcome.value = null; filterRegime.value = null }
                                "LONG" -> if (filterDirection.value == "LONG") { filterDirection.value = null } else { filterDirection.value = "LONG"; filterOutcome.value = null; filterRegime.value = null }
                                "SHORT" -> if (filterDirection.value == "SHORT") { filterDirection.value = null } else { filterDirection.value = "SHORT"; filterOutcome.value = null; filterRegime.value = null }
                                "WINS" -> if (filterOutcome.value == "WIN") { filterOutcome.value = null } else { filterOutcome.value = "WIN"; filterDirection.value = null; filterRegime.value = null }
                                "LOSSES" -> if (filterOutcome.value == "LOSS") { filterOutcome.value = null } else { filterOutcome.value = "LOSS"; filterDirection.value = null; filterRegime.value = null }
                            }
                        }
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabLabels[label] ?: label,
                        color = if (isSelected) Color.White else Color(0xFF8E8E8E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(if (isSelected) Color.White else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ReconstructionStatsCard(stats: com.asc.markets.data.AuditStats) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF0F3A7D)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("RECONSTRUCTION", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                    Text("FORENSIC ANALYSIS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: Win Rate + Total
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WIN RATE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text("${stats.winRate}%", color = when { stats.winRate >= 60 -> EmeraldSuccess; stats.winRate >= 45 -> IndigoAccent; else -> RoseError }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${stats.winningRecords} / ${stats.totalRecords}", color = SlateText, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("TOTAL P&L", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text(PostMoveAuditStore.formatSigned(stats.totalPnl), color = if (stats.totalPnl >= 0) EmeraldSuccess else RoseError, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Profit Factor + Avg R-Multiple
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PROFIT FACTOR", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text(
                        stats.profitFactor?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A",
                        color = when { (stats.profitFactor ?: 0.0) >= 2.0 -> EmeraldSuccess; (stats.profitFactor ?: 0.0) >= 1.0 -> IndigoAccent; else -> RoseError },
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("AVG R-MULTIPLE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text(
                        stats.avgRMultiple?.let { String.format(Locale.US, "%+.2fR", it) } ?: "N/A",
                        color = if ((stats.avgRMultiple ?: 0.0) >= 0) EmeraldSuccess else RoseError,
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Max Consec Losses + Best/Worst
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MAX CONSEC LOSSES", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text("${stats.maxConsecutiveLosses}", color = if (stats.maxConsecutiveLosses > 3) RoseError else Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("BEST / WORST", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text(
                        "${stats.bestTradePnl?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A"} / ${stats.worstTradePnl?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A"}",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicReconstructionCaseCard(case: PostMoveAuditCase, onClick: () -> Unit) {
    val impactColor = when {
        case.targetHit == true -> EmeraldSuccess
        case.invalidationHit == true -> RoseError
        else -> IndigoAccent
    }
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm dd MMM").withZone(ZoneId.systemDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = PureBlack,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PairFlags(case.symbol, 40)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(case.symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(case.direction, color = if (case.direction == "LONG") EmeraldSuccess else if (case.direction == "SHORT") RoseError else SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    if (case.regimeStack.isNotBlank()) {
                        Surface(color = IndigoAccent.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(case.regimeStack, color = IndigoAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = InterFontFamily)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (case.actualMovePct != null) {
                        Text("MOVE ${PostMoveAuditStore.formatPercent(case.actualMovePct)}", color = if (case.actualMovePct >= 0) EmeraldSuccess else RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                    if (case.pnl != null) {
                        Text("P&L ${PostMoveAuditStore.formatSigned(case.pnl)}", color = if (case.pnl >= 0) EmeraldSuccess else RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // MFE/MAE mini bar
                if (case.maxFavorableExcursionPct != null || case.maxAdverseExcursionPct != null) {
                    val mfe = case.maxFavorableExcursionPct ?: 0.0
                    val mae = case.maxAdverseExcursionPct ?: 0.0
                    val totalRange = (mfe - mae).coerceAtLeast(0.01)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MFE", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))) {
                            val mfeWidth = if (totalRange > 0) (mfe / totalRange) else 0.5f
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(mfeWidth.toFloat().coerceIn(0f, 1f)).background(EmeraldSuccess.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                        }
                        Text("MAE", color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(fmt.format(Instant.ofEpochMilli(case.timestamp)), color = SlateText, fontSize = 9.sp, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = impactColor.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text(case.status, color = impactColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontFamily = InterFontFamily)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(case.source.label(), color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun DynamicReconstructionDetailView(case: PostMoveAuditCase, onClose: () -> Unit, context: Context) {
    val impactColor = when {
        case.targetHit == true -> EmeraldSuccess
        case.invalidationHit == true -> RoseError
        else -> IndigoAccent
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 36.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("RECONSTRUCTION", color = Color.White, style = TerminalTypography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily))
                                Text("${case.symbol}  ${case.source.label()}", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                            }
                        }
                        IconButton(onClick = { exportPostMoveReconstructionPdf(context, case) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color.White)
                        }
                    }
                }
            }

            // Symbol + Status hero
            item {
                InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        PairFlags(case.symbol, 48)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(case.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text(case.direction, color = if (case.direction == "LONG") EmeraldSuccess else if (case.direction == "SHORT") RoseError else SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                            Text(case.status, color = impactColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                        if (case.regimeStack.isNotBlank()) {
                            Surface(color = IndigoAccent.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f))) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("REGIME", color = SlateText, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Text(case.regimeStack, color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }
            }

            // Full metric grid
            item {
                InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("EXECUTION METRICS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ReconstructionMetric("ENTRY", PostMoveAuditStore.formatPrice(case.entryPrice), Modifier.weight(1f))
                            ReconstructionMetric("EXIT", PostMoveAuditStore.formatPrice(case.exitPrice), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ReconstructionMetric("ACTUAL MOVE", PostMoveAuditStore.formatPercent(case.actualMovePct), Modifier.weight(1f))
                            ReconstructionMetric("P&L", case.pnl?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A", Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ReconstructionMetric("MFE", PostMoveAuditStore.formatPercent(case.maxFavorableExcursionPct), Modifier.weight(1f))
                            ReconstructionMetric("MAE", PostMoveAuditStore.formatPercent(case.maxAdverseExcursionPct), Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ReconstructionMetric("DURATION", case.timeToTargetMs?.let { PostMoveAuditStore.formatDuration(it) } ?: "N/A", Modifier.weight(1f))
                            ReconstructionMetric("SLIPPAGE", case.slippagePips?.let { String.format(Locale.US, "%.2f pips", it) } ?: "N/A", Modifier.weight(1f))
                        }
                    }
                }
            }

            // MFE/MAE Forensic Analysis
            if (case.maxFavorableExcursionPct != null && case.maxAdverseExcursionPct != null) {
                item {
                    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("EXCURSION ANALYSIS", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.height(8.dp))
                            val mfe = case.maxFavorableExcursionPct
                            val mae = case.maxAdverseExcursionPct
                            val entryEfficiency = if (case.actualMovePct != null && case.actualMovePct > 0 && mfe > 0) {
                                ((case.actualMovePct / mfe) * 100).toInt().coerceIn(0, 100)
                            } else null
                            val exitQuality = if (mae < 0 && mfe > 0) {
                                val recoveryFromMae = ((mfe - mae) / mfe * 100).toInt().coerceIn(0, 100)
                                recoveryFromMae
                            } else null

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("MFE (Best Case)", color = SlateText, fontSize = 9.sp)
                                    Text(PostMoveAuditStore.formatPercent(mfe), color = EmeraldSuccess, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Text("Peak favorable excursion after entry", color = SlateText, fontSize = 8.sp, lineHeight = 12.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("MAE (Worst Case)", color = SlateText, fontSize = 9.sp)
                                    Text(PostMoveAuditStore.formatPercent(mae), color = RoseError, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Text("Peak adverse excursion after entry", color = SlateText, fontSize = 8.sp, lineHeight = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            entryEfficiency?.let { eff ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ENTRY EFFICIENCY", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    Text("$eff%", color = when { eff >= 70 -> EmeraldSuccess; eff >= 40 -> IndigoAccent; else -> RoseError }, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("How much of the available upside was captured at exit. Higher = better entry timing.", color = SlateText, fontSize = 9.sp, lineHeight = 13.sp, fontFamily = InterFontFamily)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Excursion bar visualization
                            val totalRange = (mfe - mae).coerceAtLeast(0.01)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(String.format(Locale.US, "%.2f%%", mae), color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(50.dp))
                                Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))) {
                                    // MAE portion (red, left side)
                                    val maeWidth = (kotlin.math.abs(mae) / totalRange).toFloat().coerceIn(0f, 1f)
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(maeWidth).background(RoseError.copy(alpha = 0.4f), RoundedCornerShape(4.dp, 0.dp, 0.dp, 4.dp)).align(Alignment.CenterStart))
                                    // MFE portion (green, right side)
                                    val mfeWidth = (mfe / totalRange).toFloat().coerceIn(0f, 1f)
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(mfeWidth).background(EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(0.dp, 4.dp, 4.dp, 0.dp)).align(Alignment.CenterEnd))
                                }
                                Text(String.format(Locale.US, "+%.2f%%", mfe), color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(50.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                        }
                    }
                }
            }

            // Pre-Move Intelligence
            if (case.preMoveScoreAtEntry != null || case.compressionScoreAtEntry != null || case.ignitionScoreAtEntry != null) {
                item {
                    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("PRE-MOVE INTELLIGENCE AT ENTRY", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                case.preMoveScoreAtEntry?.let { score ->
                                    Column {
                                        Text("PRE-MOVE", color = SlateText, fontSize = 9.sp)
                                        Text("$score%", color = when { score >= 70 -> EmeraldSuccess; score >= 50 -> Color(0xFFFFC107); else -> RoseError }, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                case.compressionScoreAtEntry?.let { score ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("COMPRESSION", color = SlateText, fontSize = 9.sp)
                                        Text("$score%", color = when { score >= 70 -> EmeraldSuccess; score >= 50 -> Color(0xFFFFC107); else -> RoseError }, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                case.ignitionScoreAtEntry?.let { score ->
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("IGNITION", color = SlateText, fontSize = 9.sp)
                                        Text("$score%", color = when { score >= 70 -> EmeraldSuccess; score >= 50 -> Color(0xFFFFC107); else -> RoseError }, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Liquidity Target
            if (case.liquidityTarget != null) {
                item {
                    Surface(
                        color = Color(0xFFFFA500).copy(alpha = 0.06f),
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFA500).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = Color(0xFFFFA500), modifier = Modifier.size(18.dp))
                            Column {
                                Text("LIQUIDITY MAGNET", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text(case.liquidityTarget, color = Color(0xFFFFA500), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }

            // Original Thesis
            item {
                ReconstructionTextBlock("ORIGINAL THESIS", case.thesis)
            }

            // Post-Move Outcome
            item {
                ReconstructionTextBlock("POST-MOVE OUTCOME", case.postMoveOutcome)
            }

            // Reconstruction Trace
            item {
                InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("RECONSTRUCTION TRACE", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        case.reconstructionLines.forEach { line ->
                            Text(line, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
            }

            // Failure Reason
            if (case.failureReason != null) {
                item {
                    Surface(
                        color = RoseError.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                            Column {
                                Text("FAILURE REASON", color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(case.failureReason, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }

            // Export button
            item {
                Button(
                    onClick = { exportPostMoveReconstructionPdf(context, case) },
                    modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT RECONSTRUCTION PDF", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                }
            }

            // Metadata
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TS: ${case.timestamp}", color = SlateText.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = InterFontFamily)
                    Text("SEQ: ${case.id.take(6).uppercase()}", color = SlateText.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = InterFontFamily)
                }
            }
        }
    }
}

@Composable
private fun ReconstructionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun ReconstructionTextBlock(title: String, body: String) {
    InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
            Text(body, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = InterFontFamily)
        }
    }
}

private fun exportPostMoveReconstructionPdf(context: Context, case: PostMoveAuditCase) {
    try {
        val fileName = "PostMoveReconstruction_${case.id}_${System.currentTimeMillis()}.pdf"
        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ASC")
            }
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create PDF entry in MediaStore")
            resolver.openOutputStream(uri)?.use { os -> writeReconstructionPdf(os, case) }
                ?: throw IllegalStateException("Could not open PDF output stream")
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val pdfFile = File(downloadsDir, fileName)
            FileOutputStream(pdfFile).use { os -> writeReconstructionPdf(os, case) }
            uri = Uri.fromFile(pdfFile)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "PDF", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Reconstruction PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun writeReconstructionPdf(os: java.io.OutputStream, case: PostMoveAuditCase) {
    val writer = PdfWriter(os)
    val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(writer)
    val document = Document(pdfDocument)
    val timeText = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(case.timestamp))

        document.add(Paragraph("POST-MOVE RECONSTRUCTION").setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("Case: ${case.id}  |  Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph(" "))

        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Symbol").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("${case.symbol} (${case.direction})")))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Status").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(case.status)))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Source").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(case.source.label())))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Regime").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(case.regimeStack.ifBlank { "N/A" })))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Time").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(timeText)))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Entry / Exit").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("${PostMoveAuditStore.formatPrice(case.entryPrice)} / ${PostMoveAuditStore.formatPrice(case.exitPrice)}")))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Move / P&L").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("${PostMoveAuditStore.formatPercent(case.actualMovePct)} / ${case.pnl?.let { PostMoveAuditStore.formatSigned(it) } ?: "N/A"}")))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("MFE / MAE").setBold()))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("${PostMoveAuditStore.formatPercent(case.maxFavorableExcursionPct)} / ${PostMoveAuditStore.formatPercent(case.maxAdverseExcursionPct)}")))
        case.preMoveScoreAtEntry?.let { pre ->
            table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("Pre-Move / Comp / Ign").setBold()))
            table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph("$pre% / ${case.compressionScoreAtEntry ?: "N/A"}% / ${case.ignitionScoreAtEntry ?: "N/A"}%")))
        }
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
}
