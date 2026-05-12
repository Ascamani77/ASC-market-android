package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.PostMoveAuditCase
import com.asc.markets.data.PostMoveAuditStore
import com.asc.markets.data.label
import com.asc.markets.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke

@Composable
fun DeepAuditModal(case: PostMoveAuditCase, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("POST-MOVE RECONSTRUCTION — ${case.symbol}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuditSnapshotBox("Source", case.source.label(), Modifier.weight(1f))
                        AuditSnapshotBox("Status", case.status, Modifier.weight(1f))
                        AuditSnapshotBox("Move", PostMoveAuditStore.formatPercent(case.actualMovePct), Modifier.weight(1f))
                        AuditSnapshotBox("Score", case.modelAccuracyScore?.let { "$it%" } ?: "PENDING", Modifier.weight(1f))
                    }

                    Surface(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Outcome Metrics", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Entry", color = Color.White, fontWeight = FontWeight.Black)
                                    Text(PostMoveAuditStore.formatPrice(case.entryPrice), color = SlateText)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exit", color = Color.White, fontWeight = FontWeight.Black)
                                    Text(PostMoveAuditStore.formatPrice(case.exitPrice), color = SlateText)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PnL", color = Color.White, fontWeight = FontWeight.Black)
                                    Text(PostMoveAuditStore.formatSigned(case.pnl), color = SlateText)
                                }
                            }
                        }
                    }

                    Surface(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Original Thesis", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(case.thesis, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                            Text("Post-Move Outcome", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(case.postMoveOutcome, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }

                    Surface(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reconstruction Trace", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            case.reconstructionLines.forEach { line ->
                                Text(line, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                            if (case.failureReason != null) {
                                Text("Failure Reason", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text(case.failureReason, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    Surface(
                        color = IndigoAccent.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔐", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "This reconstruction is derived from captured closed trades, recorded AI decisions, and available post-signal market history. Missing fields are shown as not captured instead of being simulated.",
                                color = SlateText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditSnapshotBox(title: String, value: String, modifier: Modifier) {
    Surface(
        color = PureBlack,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier.height(96.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}
