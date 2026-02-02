package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MiniChart
import com.asc.markets.ui.theme.*
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import com.asc.markets.ui.components.InstrumentIcon
import com.asc.markets.ui.components.ForexIcon
import com.asc.markets.ui.components.classifyAsset
import com.asc.markets.ui.components.AssetType

@Composable
fun MarketOverviewTab(selectedPair: ForexPair, onAssetClick: (ForexPair) -> Unit = {}) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val categories = listOf("Overview", "Stocks", "Crypto", "Futures", "Forex", "Bonds")
    var selectedCat by remember { mutableStateOf("Overview") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // 1. TOP HEADER (Explore + Search)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Institutional Explore", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Icon(androidx.compose.material.icons.autoMirrored.outlined.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // 2. CATEGORY CHIPS
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCat == cat
                    Surface(
                        color = if (isSelected) Color(0xFF2d2d2d) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                        modifier = Modifier.clickable { 
                            vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                            selectedCat = cat 
                        }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF94a3b8),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2.5. MARKET OVERVIEW SUMMARY
        item {
            InfoBox(minHeight = 180.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Market Overview", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Global Sentiment", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Bullish", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volatility Index", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("18.5 (Low)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trading Volume", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("High", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top Gainer", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("BTC +8.2%", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Top Loser", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("EUR/USD -1.1%", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Market Cap", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("2.56 Trillion", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    // Explanatory points
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "• Global Sentiment reflects aggregate market bias: Bullish indicates net long positioning across major indices and crypto.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Volatility Index (VIX-equivalent) measures expected price swing range: Low = calm markets, High = institutional hedging active.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Trading Volume surge indicates breakout potential: High volume on bullish days confirms trend strength and liquidity.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Top Gainer/Loser tracks momentum leaders: Use as sentiment gauge for sector rotation and relative strength analysis.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Market Cap concentration shows where institutional capital flows: Crypto > $2.5T signals strong institutional adoption.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                        Text(
                            "• Real-time updates: All metrics refresh every 60 seconds to capture micro-trends before they cascade across asset classes.",
                            color = Color.White, fontSize = 10.sp, lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // 3. EXPLORE GRID (2-Column Matrix)
        val gridItems = FOREX_PAIRS.take(6).chunked(2)
        items(gridItems) { row ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { pair ->
                    ExploreMiniCard(
                        pair = pair,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                            onAssetClick(pair)
                        }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        // 4. NEWS FLOW HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("News Flow >", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }

        // 5. NEWS FLOW ITEMS
        val newsItems = listOf(
            Triple("Reuters • 10:06 pm", "Two things OPEC+ can't control: Trump and China imports", "B"),
            Triple("Dow Jones • 10:00 pm", "Week Ahead for FX, Bonds: U.S. Jobs Data, Central Bank Decisions in Focus", "W"),
            Triple("Reuters • 09:45 pm", "Island Pharmaceuticals Seeks Trading Halt", "I"),
            Triple("Bloomberg • 09:30 pm", "Bitcoin's Price Sinks Further: High Volatility Expected", "B")
        )

        items(newsItems) { (meta, title, iconChar) ->
            NewsFlowRow(meta, title, iconChar)
        }

        // MARKET FLOW (NEWS STREAM) WITH VISUALIZATION
        item {
            InfoBox(minHeight = 160.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header with title and status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Newspaper, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Text("Market Flow", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Impact headline metric
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("High-Impact Headlines", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("4 critical news events in last 2 hours", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    
                    // Source distribution progress bars
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Reuters
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reuters", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                            Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.65f).background(RoseError, RoundedCornerShape(2.dp)))
                            }
                            Text("65%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Bloomberg
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bloomberg", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                            Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(IndigoAccent, RoundedCornerShape(2.dp)))
                            }
                            Text("35%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // LP ROUTING FLOW WITH VENUE LATENCY
        item {
            InfoBox(minHeight = 180.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header with title and icon
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                        Text("LP Routing Flow", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    // Venue liquidity distribution
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // JPM-NODE
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("JPM-NODE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.78f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.01MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // LMAX-UK
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LMAX-UK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.56f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.02MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // CITADEL
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("CITADEL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.45f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.01MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // BARC-L7
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("BARC-L7", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                            Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.32f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                            }
                            Text("0.04MS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // VOLATILITY PULSE
        item {
            InfoBox(minHeight = 140.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header with title and status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Text("Volatility Pulse", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "STABLE",
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Standard Deviation metric
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Standard Deviation", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.683f).background(Color(0xFFFFA500), RoundedCornerShape(4.dp)))
                            }
                            Text("68.3%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    // Market context insight
                    Text(
                        "Market compression detected. Expansion phase expected within the next 45 minutes of NY session.",
                        color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium
                    )
                    
                    // Large metric display
                    Text(
                        "1.4",
                        color = IndigoAccent,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 6. MARKET DEPTH LADDER
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 16.dp)) {
                MarketDepthLadder(selectedPair = selectedPair)
            }
        }

        // NEW SECTIONS: Crypto, Stocks, Indices Cards
        item { CryptoCardsSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { StockCardsSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { IndicesCardsSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { MajorIndicesSection(onAssetClick) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        // Crypto market cap removed per request
    }
}

@Composable
private fun ExploreMiniCard(pair: ForexPair, modifier: Modifier, onClick: () -> Unit) {
    val isUp = pair.change >= 0
    val color = if (isUp) EmeraldSuccess else RoseError

    InfoBox(
        modifier = modifier.clickable { onClick() },
        height = 130.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Content Row
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Show flags for forex pairs, badge for others
                    if (pair.symbol.contains("/")) {
                        ForexIcon(pair.symbol, size = 20)
                    } else {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
                            Text(pair.symbol.take(1), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Column {
                        Text(pair.symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(pair.name.take(8).uppercase() + "...", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("D —", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    text = String.format(Locale.US, "%.2f", pair.price) + if (pair.symbol.contains("/")) "" else " INR",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "${if (isUp) "+" else ""}${String.format(Locale.US, "%.2f", pair.changePercent)}% today",
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Edge-to-Edge Sparkline
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))) {
                MiniChart(
                    values = List(15) { pair.price + kotlin.random.Random.nextDouble(-pair.price*0.01, pair.price*0.01) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NewsFlowRow(meta: String, title: String, iconChar: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                Text(iconChar, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Text(meta, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            fontFamily = InterFontFamily
        )
    }
}

// ============= NEW SECTIONS: Crypto, Stocks, Indices Cards =============

@Composable
fun CryptoCardsSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Crypto Gainers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(0.dp)) {
            items(3) { idx ->
                val cryptos = listOf(
                    Triple("Bitcoin", "BTC", "76,762"),
                    Triple("Ethereum", "ETH", "2,300.9"),
                    Triple("zkSync", "ZK", "0.028705")
                )
                val (name, symbol, price) = cryptos[idx % cryptos.size]
                CryptoCard(name, symbol, price) {
                    val p = price.replace(",", "").toDoubleOrNull() ?: 0.0
                    onAssetClick(ForexPair(symbol, name, p, 0.0, 0.0))
                }
            }
        }
    }
}

@Composable
private fun CryptoCard(name: String, symbol: String, price: String, onClick: () -> Unit = {}) {
    InfoBox(modifier = Modifier.width(160.dp).clickable { onClick() }, height = 200.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Crypto badge with emoji icon
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                    Text("₿", color = Color(0xFFF7931A), fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Column {
                    Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(symbol, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(price + " USD", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("+20.59% today", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(6.dp))) {
                MiniChart(
                    values = List(20) { 100.0 + kotlin.random.Random.nextDouble(-5.0, 5.0) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun StockCardsSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Stocks Gainers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val stocks = listOf(
            Pair("United Foods", "217.44 INR"),
            Pair("Creative Dynamics", "615.65 INR"),
            Pair("Gas Holdings", "1.8308 USD")
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(0.dp)) {
            items(stocks.size) { idx ->
                val (name, price) = stocks[idx]
                StockCard(name, price) {
                    val p = price.replace(" INR", "").replace(" USD", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    onAssetClick(ForexPair(name, name, p, 0.0, 0.0))
                }
            }
        }
    }
}

@Composable
private fun StockCard(name: String, price: String, onClick: () -> Unit = {}) {
    InfoBox(modifier = Modifier.width(160.dp).clickable { onClick() }, height = 200.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Stock badge
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                    Text("📈", fontSize = 16.sp)
                }
                Text(name.take(6), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(price, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("+20.0% today", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(6.dp))) {
                MiniChart(
                    values = List(20) { 217.0 + kotlin.random.Random.nextDouble(-10.0, 10.0) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun IndicesCardsSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Major Indices", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val indices = listOf(
            Triple("S&P 500", "SPX", "6,939.02"),
            Triple("Nasdaq 100", "NDX", "25,552.39"),
            Triple("DAX", "DAX", "24,538.81")
        )
        
        indices.forEach { (name, symbol, price) ->
            IndexCard(name, symbol, price) {
                val p = price.replace(",", "").replace(" USD", "").replace(" JPY", "").toDoubleOrNull() ?: 0.0
                onAssetClick(ForexPair(symbol, name, p, 0.0, 0.0))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun IndexCard(name: String, symbol: String, price: String, onClick: () -> Unit = {}) {
    InfoBox(modifier = Modifier.fillMaxWidth().clickable { onClick() }, height = 150.dp) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Index badge
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                        Text(symbol.take(3), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Text(price, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("-0.43%", color = RoseError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(8.dp))) {
                MiniChart(
                    values = List(25) { 6900.0 + kotlin.random.Random.nextDouble(-50.0, 50.0) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MajorIndicesSection(onAssetClick: (ForexPair) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("All Major Markets", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        
        val majorMarkets = listOf(
            // STOCK INDICES
            MajorIndex("S&P 500", "SPX", "6,939.02 USD", "+0.87%", EmeraldSuccess),
            MajorIndex("Nasdaq 100", "NDX", "25,552.39 USD", "+1.42%", EmeraldSuccess),
            MajorIndex("DAX", "DAX", "24,538.81 EUR", "+0.94%", EmeraldSuccess),
            MajorIndex("FTSE 100", "UKX", "10,223.54 GBP", "+0.51%", EmeraldSuccess),
            
            // FOREX PAIRS
            MajorIndex("EUR/USD", "EURUSD", "1.0852", "+0.23%", EmeraldSuccess),
            MajorIndex("GBP/USD", "GBPUSD", "1.2734", "-0.15%", RoseError),
            MajorIndex("USD/JPY", "USDJPY", "149.85", "+0.42%", EmeraldSuccess),
            
            // ASIAN INDICES
            MajorIndex("Japan 225", "NI225", "53,322.80 JPY", "+2.10%", EmeraldSuccess),
            MajorIndex("SSE Composite", "000001", "4,117.9476 CNY", "-0.96%", RoseError),
            
            // COMMODITIES
            MajorIndex("Gold (Spot)", "XAUUSD", "2,087.50 USD", "+1.85%", EmeraldSuccess),
            MajorIndex("Crude Oil WTI", "WTICRUDEOJ", "76.45 USD", "-1.32%", RoseError),
            MajorIndex("Natural Gas", "NGAS", "2.856 USD", "+3.21%", EmeraldSuccess)
        )
        
        majorMarkets.forEach { market ->
            MajorIndexRow(market) {
                val p = market.value.replace(",", "").replace(" USD", "").replace(" JPY", "").replace(" GBP", "").toDoubleOrNull() ?: 0.0
                onAssetClick(ForexPair(market.code, market.name, p, 0.0, 0.0))
            }
        }
        
        Text(
            "See all markets >",
            color = IndigoAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

data class MajorIndex(val name: String, val code: String, val value: String, val change: String, val changeColor: Color)

@Composable
private fun MajorIndexRow(index: MajorIndex, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            // Show flags for forex pairs, badges for others
            if (index.code.contains("/")) {
                ForexIcon(index.code, size = 40)
            } else {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(IndigoAccent), contentAlignment = Alignment.Center) {
                    Text(index.code.take(3), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            Column {
                Text(index.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(index.code, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(index.value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(index.change, color = index.changeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Crypto market cap section removed per user request