package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ScannerSignalsStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StrategySignalsTab(viewModel: ForexViewModel = viewModel()) {
    val context = LocalContext.current
    val scannerSignals by ScannerSignalsStore.signals.collectAsState()
    val scannerConnected by ScannerSignalsStore.isConnected.collectAsState()
    LaunchedEffect(Unit) { ScannerSignalsStore.start(context) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            InfoBox {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(14.dp).background(IndigoAccent, shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        androidx.compose.material3.Text("Opportunity Awareness Matrix", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        androidx.compose.material3.Text(if (scannerConnected) "${scannerSignals.size} scanner signals • Live" else "Waiting for scanner feed…", color = SlateText, fontSize = 11.sp)
                    }
                }
            }
        }
        if (scannerSignals.isEmpty()) {
            item {
                InfoBox {
                    androidx.compose.material3.Text("No signals yet. Ensure hybrid_ai_server.py is running on ${com.asc.markets.data.NetworkConfig.DEFAULT_SCANNER_URL} and MT5 EA is pushing (check server log for POST /predict).", color = SlateText, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            // Group by direction
            val longs = scannerSignals.filter { it.direction == "LONG" }
            val shorts = scannerSignals.filter { it.direction == "SHORT" }
            val waits = scannerSignals.filter { it.direction !in listOf("LONG", "SHORT") }

            if (longs.isNotEmpty()) {
                item { androidx.compose.material3.Text("LONG Setups (${longs.size})", color = EmeraldSuccess, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp)) }
                items(longs) { sig -> ScannerSignalCard(sig) }
            }
            if (shorts.isNotEmpty()) {
                item { androidx.compose.material3.Text("SHORT Setups (${shorts.size})", color = RoseError, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp)) }
                items(shorts) { sig -> ScannerSignalCard(sig) }
            }
            if (waits.isNotEmpty()) {
                item { androidx.compose.material3.Text("WAIT / No Setup (${waits.size})", color = SlateText, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp)) }
                items(waits.take(6)) { sig -> ScannerSignalCard(sig) }
            }
        }
    }
}

@Composable
private fun ScannerSignalCard(sig: com.asc.markets.data.ScannerSignal) {
    val hot = (sig.direction == "LONG" || sig.direction == "SHORT") && sig.pTrade >= 0.60 && sig.confidence >= 0.55
    val stale = sig.age > 900
    val dirColor = when (sig.direction) { "LONG" -> EmeraldSuccess; "SHORT" -> RoseError; else -> SlateText }
    InfoBox {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text(sig.asset, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                androidx.compose.material3.Text(sig.direction, color = dirColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text("CONF ${String.format("%.0f%%", sig.confidence * 100)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Text("P(T) ${if (sig.pTrade >= 0) String.format("%.0f%%", sig.pTrade * 100) else "—"}", color = SlateText, fontSize = 12.sp)
                androidx.compose.material3.Text("${sig.age}s ago" + if (hot) " • HOT" else "" + if (stale) " • STALE" else "", color = if (stale) Color.Gray else SlateText, fontSize = 11.sp)
            }
            if (hot) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(EmeraldSuccess.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(sig.confidence.toFloat()).fillMaxHeight().background(EmeraldSuccess, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}
data class SignalData(val pair: String, val dir: String, val conf: Int, val status: String)

@Composable
private fun SignalCard(data: SignalData) {
    val signalData = rememberStrategySignal()
    com.asc.markets.ui.components.dashboard.SignalCardView(
        pair = signalData.pair,
        status = signalData.status,
        conf = signalData.confidence,
        mainValue = signalData.entryPrice,
        delta = null,
        entryZone = signalData.entryPrice,
        rr = signalData.riskReward
    )
}
