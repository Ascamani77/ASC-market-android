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
import androidx.compose.ui.unit.em
import com.asc.markets.data.ForexPair
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MiniChart
import com.asc.markets.ui.theme.*
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun MarketOverviewTab(selectedPair: ForexPair) {
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
                Text("Explore", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
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

        // NEW SECTIONS: Crypto, Stocks, Indices Cards
        item { CryptoCardsSection() }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { StockCardsSection() }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { IndicesCardsSection() }
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
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(0.05f)), contentAlignment = Alignment.Center) {
                        Text(pair.symbol.take(1), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
                    text = String.format("%.2f", pair.price) + if (pair.symbol.contains("/")) "" else " INR",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "${if (isUp) "+" else ""}${String.format("%.2f", pair.changePercent)}% today",
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
fun CryptoCardsSection() {
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
                CryptoCard(name, symbol, price)
            }
        }
    }
}

@Composable
private fun CryptoCard(name: String, symbol: String, price: String) {
    InfoBox(modifier = Modifier.width(160.dp), height = 200.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(IndigoAccent), contentAlignment = Alignment.Center) {
                    Text(symbol.take(1), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
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
fun StockCardsSection() {
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
                StockCard(name, price)
            }
        }
    }
}

@Composable
private fun StockCard(name: String, price: String) {
    InfoBox(modifier = Modifier.width(160.dp), height = 200.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2)
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
fun IndicesCardsSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Major Indices", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val indices = listOf(
            Triple("S&P 500", "SPX", "6,939.02"),
            Triple("Nasdaq 100", "NDX", "25,552.39"),
            Triple("DAX", "DAX", "24,538.81")
        )
        
        indices.forEach { (name, symbol, price) ->
            IndexCard(name, symbol, price)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun IndexCard(name: String, symbol: String, price: String) {
    InfoBox(modifier = Modifier.fillMaxWidth(), height = 150.dp) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(symbol, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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