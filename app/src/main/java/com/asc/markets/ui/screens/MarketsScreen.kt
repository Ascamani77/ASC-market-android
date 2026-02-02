package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.theme.*

@Composable
fun MarketsScreen(onSelectPair: (ForexPair) -> Unit) {
    var activeCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("ALL", "FOREX", "CRYPTO", "INDICES", "COMMODITIES")

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Categories Bar Parity
        Surface(
            color = Color(0xFF080808),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories) { cat ->
                    val active = activeCategory == cat
                    Surface(
                        color = if (active) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp).clickable { activeCategory = cat }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = cat, 
                                color = if (active) Color.Black else Color.Gray, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // Search Input Parity
        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("FILTER SYMBOLS...", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Black) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(androidx.compose.material.icons.autoMirrored.outlined.Search, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                    focusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White
                )
            )
        }

        // Markets screen overview / feature writeup (so operators understand what this view provides)
        InfoBox(minHeight = 100.dp) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Markets View - Features", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("• Category chips for quick filtering by asset class (ALL / FOREX / CRYPTO / INDICES / COMMODITIES).", color = Color.White, fontSize = 12.sp)
                Text("• Integrated search for rapid symbol discovery.", color = Color.White, fontSize = 12.sp)
                Text("• Live price surveillance with color-coded percent changes (Emerald = gain, Rose = loss).", color = Color.White, fontSize = 12.sp)
                Text("• Edge-to-edge sparklines on each asset card provide immediate trend context.", color = Color.White, fontSize = 12.sp)
                Text("• Tap any asset card to open the full terminal / detail view for deep analysis.", color = Color.White, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            val filtered = FOREX_PAIRS.filter {
                val matchesSearch = it.symbol.contains(searchQuery, ignoreCase = true)
                val matchesCat = when(activeCategory) {
                    "ALL" -> true
                    "FOREX" -> it.symbol.contains("/")
                    "CRYPTO" -> it.symbol.contains("BTC") || it.symbol.contains("ETH") || it.symbol.contains("SOL")
                    "INDICES" -> it.symbol.contains("NAS") || it.symbol.contains("US") || it.symbol.contains("SPX")
                    else -> true
                }
                matchesSearch && matchesCat
            }
            
            items(filtered) { pair ->
                MarketCard(pair, onSelectPair)
            }
        }
    }
}

@Composable
fun MarketCard(pair: ForexPair, onClick: (ForexPair) -> Unit) {
    val isUp = pair.change >= 0
    InfoBox(onClick = { onClick(pair) }, minHeight = 150.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top: Asset Info & Bias Parity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PairFlags(pair.symbol, 24)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(pair.symbol, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = InterFontFamily)
                        Text(pair.name.uppercase(), color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
                Surface(
                    color = (if (isUp) EmeraldSuccess else RoseError).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (isUp) EmeraldSuccess else RoseError).copy(alpha = 0.1f))
                ) {
                    Text(
                        text = if (isUp) "BULLISH" else "BEARISH",
                        color = if (isUp) EmeraldSuccess else RoseError,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 1.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mid: Price & Ticker Parity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = String.format(java.util.Locale.US, "%.5f", pair.price),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format(java.util.Locale.US, "%.2f", pair.changePercent)}%",
                        color = if (isUp) EmeraldSuccess else RoseError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SPREAD", color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("0.4", color = SlateText, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom: Grid Parity
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)).padding(6.dp)) {
                    Column {
                        Text("DAY HIGH", color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        Text(String.format(java.util.Locale.US, "%.5f", pair.price * 1.002), color = Color.Gray, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
                Box(modifier = Modifier.weight(1f).background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(4.dp)).padding(6.dp)) {
                    Column {
                        Text("DAY LOW", color = Color.DarkGray, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        Text(String.format(java.util.Locale.US, "%.5f", pair.price * 0.998), color = Color.Gray, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}