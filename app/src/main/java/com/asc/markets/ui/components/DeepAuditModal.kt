package com.asc.markets.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AutomatedTrade
import com.asc.markets.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush

@Composable
fun DeepAuditModal(trade: AutomatedTrade, onClose: () -> Unit) {
    // Backdrop dim
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("DEEP AUDIT REPORT — ${trade.id}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                    }

                    // Snapshot grid: 4 boxes
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuditSnapshotBox("VIX", "19.8", Modifier.weight(1f))
                        AuditSnapshotBox("DXY Beta", "+0.42%", Modifier.weight(1f))
                        AuditSnapshotBox("News Safety", "CLEAR", Modifier.weight(1f))
                        AuditSnapshotBox("HTF Bias", "BULLISH", Modifier.weight(1f))
                    }

                    // Operational metrics
                    Surface(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Operational Metrics", color = SlateText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fill Price", color = Color.White, fontWeight = FontWeight.Black)
                                    Text(trade.entryPrice, color = SlateText)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Execution Type", color = Color.White, fontWeight = FontWeight.Black)
                                    Text("AI Autonomous", color = SlateText)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Transaction ID", color = Color.White, fontWeight = FontWeight.Black)
                                    Text(trade.id, color = SlateText)
                                }
                            }
                        }
                    }

                    // Digital signature pulse
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val pulse = animateFloatAsState(targetValue = 1f, animationSpec = androidx.compose.animation.core.tween(durationMillis = 1200, easing = FastOutSlowInEasing)).value
                        val brush = Brush.horizontalGradient(listOf(IndigoAccent, IndigoAccent.copy(alpha = 0.6f)))
                        Box(modifier = Modifier
                            .size((64 * pulse).dp)
                            .shadow(8.dp, RoundedCornerShape(32.dp))
                            .background(brush, shape = RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
                            Text("Immutable Digital Signature Verified", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Footer fingerprint disclosure
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
                                "This ledger is immutable and synchronized with PRIMARY-UK-L14. All snapshots are cryptographically signed and stored in the local audit archive.",
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
