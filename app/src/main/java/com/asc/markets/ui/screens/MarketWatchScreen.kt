package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.PreMoveCandidate
import com.asc.markets.data.PreMoveIntelligenceStore
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import java.util.Locale


@Composable
fun MarketWatchScreen() {
    val candidates by PreMoveIntelligenceStore.candidates.collectAsState(initial = emptyList())
    val samples = candidates.take(8)

    Column(modifier = Modifier.fillMaxSize().background(PureBlack).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
        // Top banner: icon, title, sync badge, filter
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFF101010)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("PRE-MOVE MARKET WATCH", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                Text("DETERMINISTIC CANDIDATES RANKED BEFORE EXPANSION", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
            }

            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Engine card
        InfoBox(minHeight = 120.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFF101010)) {
                        Box(contentAlignment = Alignment.Center) { Text("⟲", color = Color.White) }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ASC PRE-MOVE SCAN ACTIVE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                        Text("SCANNING COMPRESSION, LIQUIDITY MAGNETS, IGNITION READINESS, REGIME ALIGNMENT AND RISK GATES.", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample signals list — match images: three sample cards
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            samples.forEach { candidate ->
                MarketWatchSignalCard(candidate)
            }
        }
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun MarketWatchSignalCard(s: PreMoveCandidate) {
    val vm: ForexViewModel = viewModel()
    InfoBox(minHeight = 180.dp, modifier = Modifier.fillMaxWidth().clickable {
        // Select the symbol in the ViewModel without changing view, then navigate to Analysis Node
        vm.selectPairBySymbolNoNavigate(s.symbol)
        vm.navigateTo(AppView.ANALYSIS_RESULTS)
    }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // small badge
                    PairFlags(symbol = s.symbol, size = 28)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(s.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                        Surface(color = stateColor(s), shape = RoundedCornerShape(6.dp)) {
                            Text(s.state.uppercase(Locale.getDefault()), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.preMoveScore}%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                    Text("PRE-MOVE SCORE", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
            }

            // Quote box
            Surface(color = Color(0xFF080808), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(s.deterministicReason, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp, fontFamily = InterFontFamily)
            }

            // Metrics row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Momentum bar and label
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((s.compressionScore / 100f).coerceIn(0.05f, 1f)).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("COMPRESSION", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                        Text("${s.compressionScore}%", color = Color.White, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IGNITION", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Text("${s.ignitionScore}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RISK GATE", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Text(s.riskGate, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatWatchPrice(s.price), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.width(8.dp))
                Text(String.format(Locale.US, "%s%.2f%%", if (s.changePercent >= 0.0) "+" else "", s.changePercent), color = if (s.changePercent >= 0.0) EmeraldSuccess else RoseError, fontSize = 12.sp, fontFamily = InterFontFamily)

                Spacer(modifier = Modifier.weight(1f))
                Text("OPEN NODE →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}

private fun stateColor(candidate: PreMoveCandidate): Color {
    return when (candidate.state) {
        "ARMED" -> EmeraldSuccess
        "WATCH" -> IndigoAccent
        "COMPRESSING" -> Color(0xFF6B4800)
        "LATE MOVE" -> RoseError
        else -> Color(0xFF3A3A3A)
    }
}

private fun formatWatchPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format(Locale.US, "%,.2f", price)
        price >= 1 -> String.format(Locale.US, "%.5f", price) // 5 decimals for forex (like MT5)
        else -> String.format(Locale.US, "%.6f", price)
    }
}
