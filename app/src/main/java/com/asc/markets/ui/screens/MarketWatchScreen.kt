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
import com.asc.markets.data.MarketDataStore
import com.asc.markets.data.MarketCategory
import com.asc.markets.ui.theme.InterFontFamily
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MiniChart
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*
import kotlin.math.abs
import java.util.Locale


@Composable
fun MarketWatchScreen() {
    val livePairs by MarketDataStore.allPairs.collectAsState()
    val samples = remember(livePairs) {
        livePairs
            .filter { it.category != MarketCategory.BONDS }
            .sortedByDescending { abs(it.changePercent) }
            .take(6)
            .map(::toSignalModel)
    }

    Column(modifier = Modifier.fillMaxSize().background(PureBlack).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
        // Top banner: icon, title, sync badge, filter
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = Color(0xFF101010)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("PROACTIVE MARKET WATCH", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                Text("AI‑POWERED PATTERN DISCOVERY & BIAS WEIGHTING", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
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
                        Text("GLOBAL SCAN ENGINE ACTIVE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                        Text("SCANNING FOR MOMENTUM INFLOW AND STRUCTURAL FAILURE. CURRENT FOCUS IS ON IDENTIFYING LEAD-TIME ADVANTAGES.", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sample signals list — match images: three sample cards
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            samples.forEach { signal ->
                MarketWatchSignalCard(signal)
            }
        }
        
        Spacer(modifier = Modifier.height(120.dp))
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
                    PairFlags(symbol = s.symbol, size = 28)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(s.symbol, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                        Surface(color = s.tagColor, shape = RoundedCornerShape(6.dp)) {
                            Text(s.tag.uppercase(Locale.getDefault()), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.confidence}%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                    Text("AI CONFIDENCE", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
            }

            // Quote box
            Surface(color = Color(0xFF080808), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("\"${s.quote}\"", color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp, fontFamily = InterFontFamily)
            }

            // Metrics row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Momentum bar and label
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(s.momentumPct).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MOMENTUM", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                        Text(s.volatility.uppercase(Locale.getDefault()), color = Color.White, fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ATR (H1)", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Text(s.atr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IN STRUCTURE", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                    Text(s.inStructure, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.price, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = InterFontFamily)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.changeText, color = if (s.changeText.startsWith("+")) EmeraldSuccess else RoseError, fontSize = 12.sp, fontFamily = InterFontFamily)

                Spacer(modifier = Modifier.weight(1f))
                Text("OPEN ANALYSIS →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
    }
}

private fun toSignalModel(pair: ForexPair): SignalModel {
    val absMove = abs(pair.changePercent)
    val isBullish = pair.changePercent >= 0.0
    val tag = when {
        absMove >= 2.0 -> "Momentum Surge"
        absMove >= 1.0 -> "Trend Break"
        else -> "Range Shift"
    }
    val tagColor = when (pair.category) {
        MarketCategory.CRYPTO -> Color(0xFF0B6B4F)
        MarketCategory.FOREX -> Color(0xFF222831)
        MarketCategory.STOCK -> Color(0xFF1F3A5F)
        MarketCategory.COMMODITIES -> Color(0xFF6B4800)
        else -> Color(0xFF3A3A3A)
    }
    val volatility = when {
        absMove >= 2.0 -> "High"
        absMove >= 1.0 -> "Elevated"
        else -> "Normal"
    }

    return SignalModel(
        symbol = pair.symbol,
        tag = tag,
        tagColor = tagColor,
        confidence = (60 + (absMove * 12)).toInt().coerceAtMost(95),
        quote = buildQuote(pair, isBullish),
        price = formatWatchPrice(pair.price),
        changeText = String.format(Locale.US, "%s%.2f%%", if (isBullish) "+" else "", pair.changePercent),
        momentumPct = (absMove / 3.0).coerceIn(0.15, 1.0).toFloat(),
        volatility = volatility,
        atr = String.format(Locale.US, "%.4f", pair.price * 0.0025),
        inStructure = if (absMove >= 1.0) "Live" else "Stable"
    )
}

private fun buildQuote(pair: ForexPair, isBullish: Boolean): String {
    val direction = if (isBullish) "BUYING" else "SELLING"
    return when (pair.category) {
        MarketCategory.CRYPTO -> "$direction PRESSURE PERSISTING ACROSS BINANCE PERPETUAL FLOW."
        MarketCategory.FOREX -> "$direction FLOW DOMINATES THE CURRENT SESSION RANGE."
        MarketCategory.STOCK -> "$direction IMBALANCE REMAINS IN CONTROL OF THE TAPE."
        MarketCategory.COMMODITIES -> "$direction INTEREST IS DRIVING THE LATEST COMMODITY LEG."
        else -> "$direction CONDITIONS REMAIN FAVOURABLE FOR FOLLOW-THROUGH."
    }
}

private fun formatWatchPrice(price: Double): String {
    return when {
        price >= 1000 -> String.format(Locale.US, "%,.2f", price)
        price >= 1 -> String.format(Locale.US, "%.4f", price)
        else -> String.format(Locale.US, "%.6f", price)
    }
}
