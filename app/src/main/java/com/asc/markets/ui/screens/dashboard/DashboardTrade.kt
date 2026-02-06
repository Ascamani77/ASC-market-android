package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.asc.markets.R
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlin.random.Random

/**
 * Macro Intelligence Ledger (DashboardTrade) - Institutional Audit layout
 */
@Composable
fun DashboardTrade() {
    var selected by remember { mutableStateOf<TradeEntry?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(DeepBlack)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Header: Real-time Node Hub
        RealTimeNodeHeader(node = sampleNode())

        // Dispatch Audit Rows
        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sampleTrades()) { t ->
                DispatchAuditRow(trade = t, onOpenDeep = { selected = it })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer: Protocol Integrity Disclosure
        ProtocolIntegrityFooter()
    }

    if (selected != null) {
        DeepComplianceModal(entry = selected!!, onClose = { selected = null })
    }
}

@Composable
private fun RealTimeNodeHeader(node: NodeStatus) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.lucide_arrow_left_right), contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Real-time Node", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(node.name, color = SlateText, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Protocol Locked
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(EmeraldSuccess.copy(alpha = 0.95f)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Protocol Locked", color = SlateText, fontSize = 11.sp)
                    }
                    Text("Synced", color = EmeraldSuccess, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }

                // State Integrity
                Column(horizontalAlignment = Alignment.End) {
                    Text("State Integrity", color = SlateText, fontSize = 11.sp)
                    Text("SMC-L4 SECURE", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DispatchAuditRow(trade: TradeEntry, onOpenDeep: (TradeEntry) -> Unit) {
    InfoBox(modifier = Modifier.fillMaxWidth().clickable { /* expand/collapse if needed */ }) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            // Column A: Financial Verification
            Column(modifier = Modifier.weight(0.33f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(id = R.drawable.lucide_activity), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(trade.symbol, color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(trade.tradeId, color = SlateText, fontSize = 11.sp)
                Text(trade.direction, color = if (trade.direction == "Buy") EmeraldSuccess else RoseError, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(trade.auditResult, color = when (trade.auditResult) {
                    "WON" -> EmeraldSuccess
                    "LOST" -> RoseError
                    else -> SlateText
                }, fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text(trade.realizedDelta, color = Color.White, fontSize = 20.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Black)
            }

            // Column B: Intelligence Rationale
            Column(modifier = Modifier.weight(0.44f).padding(start = 12.dp, end = 12.dp)) {
                Text("Institutional Reasoning", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(trade.reason, color = Color.White, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoBox(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Internal Context", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(trade.internalContext, color = Color.White, fontSize = 11.sp)
                            }
                            // optional compact indicator or value
                        }
                    }
                    InfoBox(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Outcome Profile", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(trade.outcomeProfile, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Column C: Transmission Metadata
            Column(modifier = Modifier.weight(0.23f), horizontalAlignment = Alignment.End) {
                Text(trade.relayNode, color = SlateText, fontSize = 11.sp)
                Text(trade.timestamp, color = Color.White, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("Latency: ${trade.latencyMs}ms", color = SlateText, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(10.dp))
                Text("Details", color = IndigoAccent, modifier = Modifier.clickable { onOpenDeep(trade) })
            }
        }
    }
}

@Composable
private fun DeepComplianceModal(entry: TradeEntry, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
                Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deep Compliance Audit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                    }

                    Divider(color = Color.White.copy(alpha = 0.06f))

                    Text("Transaction Hash:", color = SlateText, fontSize = 12.sp)
                    Text(entry.txHash, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Operational Metrics", color = SlateText, fontWeight = FontWeight.Black)
                            Text("Timestamp: ${entry.timestamp}", color = SlateText)
                            Text("Action Type: AI Autonomous", color = SlateText)
                            Text("Latency: ${entry.latencyMs} ms", color = SlateText)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Asset Snapshot", color = SlateText, fontWeight = FontWeight.Black)
                            Text("Fill Price: ${entry.fillPrice}", color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("Side: ${entry.direction}", color = Color.White)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.06f))

                    Text("Black Box Record", color = SlateText, fontWeight = FontWeight.Black)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).height(80.dp).background(Color.White.copy(alpha = 0.02f))) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("VIX: ${entry.blackBox.vix}", color = Color.White)
                                Text("DXY Beta: ${entry.blackBox.dxy}", color = Color.White)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).height(80.dp).background(Color.White.copy(alpha = 0.02f))) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("News Gate: ${entry.blackBox.newsGate}", color = Color.White)
                                Text("HTF Bias: ${entry.blackBox.htf}", color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Immutable Digital Signature pulse", color = SlateText)
                        Text("Download PDF", color = IndigoAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolIntegrityFooter() {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.lucide_binary), contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Protocol Integrity", color = Color.White, fontWeight = FontWeight.Black)
                Text("All ledger entries are immutable and policy-driven; reflects engine decisions.", color = SlateText, fontSize = 11.sp)
            }
        }
    }
}

// --- Sample data models and generators ---
data class TradeEntry(
    val tradeId: String,
    val symbol: String,
    val direction: String,
    val auditResult: String,
    val realizedDelta: String,
    val reason: String,
    val internalContext: String,
    val outcomeProfile: String,
    val relayNode: String,
    val timestamp: String,
    val latencyMs: String,
    val txHash: String,
    val fillPrice: String,
    val blackBox: BlackBox
)

data class BlackBox(val vix: String, val dxy: String, val newsGate: String, val htf: String)

data class NodeStatus(val name: String, val synced: Boolean)

private fun sampleNode() = NodeStatus(name = "Node L14-UK (Relay)", synced = true)

private fun sampleTrades(): List<TradeEntry> {
    return List(5) { i ->
        val won = if (i % 2 == 0) "WON" else "OPEN"
        TradeEntry(
            tradeId = "#T-84${i}",
            symbol = listOf("EURUSD","GBPUSD","BTCUSD","AAPL","TSLA")[i%5],
            direction = if (i%2==0) "Buy" else "Sell",
            auditResult = won,
            realizedDelta = "${if (i%2==0) "+${(10..120).random()}" else "-${(5..60).random()}"} pips",
            reason = "Breakout from consolidation with HTF alignment and liquidity sweep.",
            internalContext = "Pre-entry: low vol, accumulation",
            outcomeProfile = "Post-entry: 2:1 RR hit in 14m",
            relayNode = "Node L14-UK",
            timestamp = "${2026}-${(1..12).random()}-03 10:${(10..59).random()}",
            latencyMs = String.format("%.2f", listOf(0.02,0.05,0.11,0.20).random()),
            txHash = Random.nextBytes(8).joinToString("") { "%02x".format(it) },
            fillPrice = String.format("%.5f", listOf(1.12345,234.56,0.01234,150.23,620.12)[i%5]),
            blackBox = BlackBox(vix = "${(12..28).random()}", dxy = "${(90..110).random()}", newsGate = if (i%3==0) "OPEN" else "CLOSED", htf = "BULL")
        )
    }
}
