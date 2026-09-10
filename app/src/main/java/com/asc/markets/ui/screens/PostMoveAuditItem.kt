package com.asc.markets.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AppView
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.label
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PostMoveAuditItem(
    entry: PostMoveAuditCase,
    expanded: SnapshotStateMap<String, Boolean>,
    viewModel: ForexViewModel,
    context: Context
) {
    val coroutineScope = rememberCoroutineScope()
    val isExpanded = expanded[entry.id] ?: false
    val impactColor = when {
        entry.targetHit == true -> EmeraldSuccess
        entry.invalidationHit == true -> RoseError
        else -> IndigoAccent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PureBlack,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.symbol, color = SlateText, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry.status, color = impactColor, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry.source.label(), color = Color(0xFF0F6F52), modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val confidenceInt = entry.modelAccuracyScore ?: entry.confidence
                    val confidenceColor = when {
                        confidenceInt != null && confidenceInt >= 60 -> EmeraldSuccess
                        confidenceInt != null && confidenceInt <= 40 -> RoseError
                        else -> Color(0xFFFFC107)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("[ ", color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily))
                        Text("${confidenceInt ?: 0}%", color = confidenceColor, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                        Text(" ] outcome score", color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily))
                    }
                    Spacer(modifier = Modifier.height(35.dp))

                    Text(entry.postMoveOutcome, color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, fontFamily = InterFontFamily), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                    Box(modifier = Modifier.wrapContentWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(fmt.format(Instant.ofEpochMilli(entry.timestamp)), color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            if (entry.status == "UNRESOLVED") {
                                val infiniteTransition = rememberInfiniteTransition()
                                val blinkAlpha by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 0.3f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 1000),
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

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isExpanded) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { expanded[entry.id] = true }) {
                        Text("VIEW CONTEXT \u203A", color = IndigoAccent, style = TerminalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, letterSpacing = 0.08.em))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        // Thesis
                        Text("POST-MOVE THESIS", color = SlateText, style = TerminalTypography.labelSmall.copy(letterSpacing = 1.sp, fontFamily = InterFontFamily), modifier = Modifier.padding(horizontal = 8.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(entry.thesis, color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, fontFamily = InterFontFamily), modifier = Modifier.padding(horizontal = 8.dp))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Failure reason if present
                        if (entry.failureReason != null) {
                            Surface(
                                color = RoseError.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, RoseError.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                                    Text(entry.failureReason, color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Audit Trace Log header
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUDIT TRACE LOG", color = SlateText, style = TerminalTypography.labelSmall.copy(fontFamily = InterFontFamily))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Source Node + Outcome Check
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("Source Node", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(entry.nodeId, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("OUTCOME CHECK", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(entry.status, color = impactColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Regime Stack Card
                        if (entry.regimeStack.isNotBlank()) {
                            Surface(
                                color = IndigoAccent.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("REGIME STACK", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                        Text(entry.regimeStack, color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Pre-Move Intelligence Scores
                        if (entry.preMoveScoreAtEntry != null || entry.compressionScoreAtEntry != null || entry.ignitionScoreAtEntry != null) {
                            InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("PRE-MOVE INTELLIGENCE AT ENTRY", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        entry.preMoveScoreAtEntry?.let { score ->
                                            Column {
                                                Text("PRE-MOVE", color = SlateText, fontSize = 9.sp)
                                                Text("$score%", color = when { score >= 70 -> EmeraldSuccess; score >= 50 -> Color(0xFFFFC107); else -> RoseError }, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        entry.compressionScoreAtEntry?.let { score ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("COMPRESSION", color = SlateText, fontSize = 9.sp)
                                                Text("$score%", color = when { score >= 70 -> EmeraldSuccess; score >= 50 -> Color(0xFFFFC107); else -> RoseError }, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        entry.ignitionScoreAtEntry?.let { score ->
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("IGNITION", color = SlateText, fontSize = 9.sp)
                                                Text("$score%", color = when { score >= 70 -> EmeraldSuccess; score >= 50 -> Color(0xFFFFC107); else -> RoseError }, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Liquidity Target
                        if (entry.liquidityTarget != null) {
                            Surface(
                                color = Color(0xFFFFA500).copy(alpha = 0.06f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFA500).copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = Color(0xFFFFA500), modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("LIQUIDITY MAGNET", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                        Text(entry.liquidityTarget, color = Color(0xFFFFA500), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Analytical Context
                        InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ANALYTICAL CONTEXT", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (entry.direction != "UNSPECIFIED" || entry.riskPct != null || entry.deploymentLabel != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (entry.direction != "UNSPECIFIED") {
                                            Column {
                                                Text("BIAS", color = SlateText, fontSize = 10.sp)
                                                Text(entry.direction, color = if (entry.direction == "LONG") EmeraldSuccess else RoseError, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            }
                                        }
                                        if (entry.riskPct != null) {
                                            Column {
                                                Text("RISK ALLOCATION", color = SlateText, fontSize = 10.sp)
                                                Text("${entry.riskPct}%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            }
                                        }
                                        if (entry.deploymentLabel != null) {
                                            Column {
                                                Text("BUCKET", color = SlateText, fontSize = 10.sp)
                                                Text(entry.deploymentLabel, color = IndigoAccent, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Text(buildExpandedAnalyticalContext(entry, entry.nodeId), color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, fontFamily = InterFontFamily))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("PRICES & MOVEMENT", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("ENTRY", color = SlateText, fontSize = 10.sp)
                                        Text(PostMoveAuditStore.formatPrice(entry.entryPrice), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("EXIT", color = SlateText, fontSize = 10.sp)
                                        Text(PostMoveAuditStore.formatPrice(entry.exitPrice), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                    if (entry.pnl != null) {
                                        Column {
                                            Text("P&L", color = SlateText, fontSize = 10.sp)
                                            Text(PostMoveAuditStore.formatSigned(entry.pnl), color = if (entry.pnl >= 0) EmeraldSuccess else RoseError, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (entry.actualMovePct != null) {
                                        Column {
                                            Text("ACTUAL MOVE", color = SlateText, fontSize = 10.sp)
                                            Text(PostMoveAuditStore.formatPercent(entry.actualMovePct), color = if (entry.actualMovePct >= 0) EmeraldSuccess else RoseError, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                    if (entry.maxFavorableExcursionPct != null) {
                                        Column {
                                            Text("MFE", color = SlateText, fontSize = 10.sp)
                                            Text(PostMoveAuditStore.formatPercent(entry.maxFavorableExcursionPct), color = EmeraldSuccess, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                    if (entry.maxAdverseExcursionPct != null) {
                                        Column {
                                            Text("MAE", color = SlateText, fontSize = 10.sp)
                                            Text(PostMoveAuditStore.formatPercent(entry.maxAdverseExcursionPct), color = RoseError, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                        }
                                    }
                                }
                                if (entry.timeToTargetMs != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        Text("DURATION: ", color = SlateText, fontSize = 10.sp)
                                        Text(PostMoveAuditStore.formatDuration(entry.timeToTargetMs), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        if (entry.slippagePips != null) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("SLIPPAGE: ", color = SlateText, fontSize = 10.sp)
                                            Text(String.format(Locale.US, "%.2f pips", entry.slippagePips), color = if (entry.slippagePips > 2.0) RoseError else EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reconstruction lines
                        if (entry.reconstructionLines.isNotEmpty()) {
                            InfoBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("RECONSTRUCTION", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    entry.reconstructionLines.forEach { line ->
                                        Text(line, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = InterFontFamily, lineHeight = 16.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Action Buttons
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                            Button(onClick = {
                                val sourceId = entry.originAuditRecordId
                                if (sourceId != null) {
                                    viewModel.markAuditRecordAudited(sourceId)
                                    Toast.makeText(context, "Marked reviewed", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Closed-trade records are read-only", Toast.LENGTH_SHORT).show()
                                }
                            }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MARK REVIEWED", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            }

                            Button(onClick = { viewModel.navigateTo(AppView.TRADE_RECONSTRUCTION) }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("OPEN RECONSTRUCTION", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TS_MICRO: ${entry.timestamp}", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            Text("SEQ_ID: ${entry.id.take(3).uppercase()}", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.White.copy(alpha = 0.04f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("LOG::${entry.id.take(3).uppercase()}", color = SlateText.copy(alpha = 0.6f), style = TerminalTypography.labelSmall.copy(fontSize = 11.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium))
                            TextButton(onClick = { expanded[entry.id] = false }) { Text("MINIMIZE CONTEXT", color = IndigoAccent, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}
