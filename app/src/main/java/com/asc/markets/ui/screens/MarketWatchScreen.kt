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
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.data.AppView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MiniChart
import com.asc.markets.ui.components.ForexIcon
import com.asc.markets.ui.theme.*
import java.util.Locale


@Composable
fun MarketWatchScreen() {
    Column(modifier = Modifier.fillMaxSize().background(PureBlack).verticalScroll(rememberScrollState())) {
        // Top banner: icon, title, sync badge, filter
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFF101010)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("PROACTIVE MARKET WATCH", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text("AI‑POWERED PATTERN DISCOVERY & BIAS WEIGHTING", color = SlateText, fontSize = 11.sp)
            }

            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Engine card
        InfoBox(minHeight = 120.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = Color(0xFF101010)) {
                        Box(contentAlignment = Alignment.Center) { Text("⟲", color = Color.White) }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("GLOBAL SCAN ENGINE ACTIVE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Text("SCANNING FOR MOMENTUM INFLOW AND STRUCTURAL FAILURE. CURRENT FOCUS IS ON IDENTIFYING LEAD-TIME ADVANTAGES.", color = SlateText, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample signals list — match images: three sample cards
        val samples = remember { sampleSignals() }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            samples.forEach { signal ->
                MarketWatchSignalCard(signal)
            }
        }
    }
}

data class SignalModel(
    val symbol: String,
    val tag: String,
    val tagColor: Color,
    val confidence: Int,
    val quote: String,
    val price: String,
    val changeText: String,
    val momentumPct: Float,
    val volatility: String,
    val atr: String,
    val inStructure: String
)

@Composable
private fun MarketWatchSignalCard(s: SignalModel) {
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
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF0B0B0B)), contentAlignment = Alignment.Center) {
                        Text(s.symbol.take(1), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(s.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Surface(color = s.tagColor, shape = RoundedCornerShape(6.dp)) {
                            Text(s.tag.uppercase(Locale.getDefault()), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.confidence}%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Text("AI CONFIDENCE", color = SlateText, fontSize = 11.sp)
                }
            }

            // Quote box
            Surface(color = Color(0xFF080808), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("\"${s.quote}\"", color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
            }

            // Metrics row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Momentum bar and label
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(s.momentumPct).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MOMENTUM", color = SlateText, fontSize = 10.sp)
                        Text(s.volatility.uppercase(Locale.getDefault()), color = Color.White, fontSize = 10.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ATR (H1)", color = SlateText, fontSize = 10.sp)
                    Text(s.atr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN STRUCTURE", color = SlateText, fontSize = 10.sp)
                    Text(s.inStructure, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.price, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.changeText, color = if (s.changeText.startsWith("+")) EmeraldSuccess else RoseError, fontSize = 12.sp)

                Spacer(modifier = Modifier.weight(1f))
                Text("OPEN ANALYSIS →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun sampleSignals(): List<SignalModel> {
    return listOf(
        SignalModel(
            symbol = "EUR/USD",
            tag = "Trend Break",
            tagColor = Color(0xFF222831),
            confidence = 78,
            quote = "H4 TRENDLINE VIOLATION WITH INSTITUTIONAL VOLUME SUPPORT.",
            price = "1.0845",
            changeText = "+0.11%",
            momentumPct = 0.6f,
            volatility = "Normal",
            atr = "0.0029",
            inStructure = "7h"
        ),
        SignalModel(
            symbol = "GBP/USD",
            tag = "Mean Revert",
            tagColor = Color(0xFF6B4800),
            confidence = 67,
            quote = "EXTENDED DEVIATION FROM 50D EMA; LOB IMBALANCE DETECTED.",
            price = "1.2634",
            changeText = "-0.17%",
            momentumPct = 0.45f,
            volatility = "Normal",
            atr = "0.0008",
            inStructure = "12h"
        ),
        SignalModel(
            symbol = "USD/JPY",
            tag = "Momentum Surge",
            tagColor = Color(0xFF0B6B4F),
            confidence = 81,
            quote = "AGGRESSIVE BUY PROGRAM ENGINEERING DISPLACEMENT ON M15.",
            price = "151.4200",
            changeText = "+0.23%",
            momentumPct = 0.8f,
            volatility = "Normal",
            atr = "0.0027",
            inStructure = "5h"
        )
    )
}

