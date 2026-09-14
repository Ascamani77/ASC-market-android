package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.NetworkConfig
import com.asc.markets.data.ScannerSignalsStore
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AiStatusTab(viewModel: ForexViewModel) {
    val context = LocalContext.current
    val scannerSignals by ScannerSignalsStore.signals.collectAsState()
    val scannerConnected by ScannerSignalsStore.isConnected.collectAsState()
    LaunchedEffect(Unit) { ScannerSignalsStore.start(context) }

    val listState = rememberLazyListState()

    // Collapse the Home/Signals/AI tabs + ASC MARKET header as this list scrolls.
    // Quantized to ~5% steps so the shared header only recomposes ~20 times per
    // full collapse instead of on every scroll frame (keeps scrolling buttery).
    val collapseRange = 220f
    val collapseProgress by remember {
        derivedStateOf {
            val absoluteScroll = (listState.firstVisibleItemIndex * 100f) + listState.firstVisibleItemScrollOffset
            ((absoluteScroll / collapseRange).coerceIn(0f, 1f) * 20f).roundToInt() / 20f
        }
    }
    LaunchedEffect(collapseProgress) {
        viewModel.setGlobalHeaderCollapse(collapseProgress)
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            InfoBox {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AI SYSTEM STATUS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (scannerConnected) "● ONLINE" else "○ OFFLINE", color = if (scannerConnected) EmeraldSuccess else RoseError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${scannerSignals.size} signals tracked", color = SlateText, fontSize = 12.sp)
                    }
                    Text("Hybrid AI: 50% SMC + 50% ML • Server ${NetworkConfig.DEFAULT_SCANNER_URL}", color = SlateText, fontSize = 11.sp)
                }
            }
        }
        if (scannerSignals.isEmpty()) {
            item {
                InfoBox {
                    Text("No AI signals yet — waiting for MT5 EA to push candles (POST /predict). Ensure hybrid_ai_server.py is running and EA is attached.", color = SlateText, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(scannerSignals) { sig ->
                val dirColor = when (sig.direction) { "LONG" -> EmeraldSuccess; "SHORT" -> RoseError; else -> SlateText }
                InfoBox {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(sig.asset, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(sig.direction, color = dirColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("CONF ${String.format("%.0f%%", sig.confidence * 100)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("P(T) ${if (sig.pTrade >= 0) String.format("%.0f%%", sig.pTrade * 100) else "—"} • ${sig.age}s ago", color = SlateText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
