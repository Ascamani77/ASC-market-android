package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun RightPanel(
    symbol: String,
    onSymbolSelect: (String) -> Unit,
    isSidebarVisible: Boolean,
    isWatchlistVisible: Boolean,
    onSidebarToggle: () -> Unit,
    onWatchlistToggle: () -> Unit,
    backgroundColor: Color = Color(0xFF121212)
) {
    val watchlist = listOf(
        "BTCUSD" to 65432.10,
        "BTCUSDT" to 65435.50,
        "ETHUSD" to 3456.78,
        "ETHUSDT" to 3458.20,
        "EURUSD" to 1.0850,
        "GBPUSD" to 1.2650,
        "USDJPY" to 151.20,
        "AUDUSD" to 0.6540,
        "USDCAD" to 1.3520,
        "USDCHF" to 0.8850,
        "USOIL" to 82.30,
        "BRENTOIL" to 86.40,
        "US02Y" to 4.65,
        "US10Y" to 4.25,
        "DGS2" to 4.62,
        "DGS10" to 4.22,
        "SPX" to 5240.50,
        "NASDAQ100" to 18350.20,
        "DJIA" to 39450.80,
        "TSLA" to 175.20,
        "AAPL" to 182.45,
        "NVDA" to 890.15,
        "MSFT" to 415.60,
        "AMZN" to 178.30,
        "XAGUSD" to 24.50,
        "XAUUSD" to 2150.30
    )

    Row(modifier = Modifier.fillMaxHeight()) {
        if (!isSidebarVisible) {
            // Level 0: Tiny strip with arrow to open sidebar
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(backgroundColor)
                    .border(width = 0.5.dp, color = Color(0xFF2A2E39))
                    .clickable { onSidebarToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    null,
                    tint = Color(0xFF787B86),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            // Level 1: Icons Sidebar (48.dp)
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .background(backgroundColor)
                    .border(width = 0.5.dp, color = Color(0xFF2A2E39))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Chevron to close sidebar
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color(0xFF787B86),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onSidebarToggle() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Icon(
                    Icons.Default.FormatListBulleted, 
                    null, 
                    tint = if (isWatchlistVisible) Color(0xFF2962FF) else Color(0xFF787B86), 
                    modifier = Modifier.size(20.dp).clickable { onWatchlistToggle() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Icon(Icons.Default.Notifications, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).clickable { })
                Spacer(modifier = Modifier.height(16.dp))
                Icon(Icons.Default.QueryStats, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).clickable { })
                Spacer(modifier = Modifier.height(16.dp))
                Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).clickable { })
                Spacer(modifier = Modifier.height(16.dp))
                Icon(Icons.Default.Public, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).clickable { })
            }

            // Level 2: Watchlist Panel (280.dp)
            if (isWatchlistVisible) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(backgroundColor)
                        .border(width = 0.5.dp, color = Color(0xFF2A2E39))
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Watchlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).clickable { })
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.MoreHoriz, null, tint = Color(0xFF787B86), modifier = Modifier.size(20.dp).clickable { })
                        }
                    }

                    Divider(color = Color(0xFF2A2E39))

                    // Watchlist Items
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(watchlist) { (itemSymbol, price) ->
                            val isActive = symbol == itemSymbol
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isActive) Color(0xFF2962FF).copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { onSymbolSelect(itemSymbol) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(itemSymbol, color = if (isActive) Color(0xFF2962FF) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(getFullSymbolName(itemSymbol), color = Color(0xFF787B86), fontSize = 11.sp, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(String.format(Locale.US, "%.2f", price), color = Color.White, fontSize = 13.sp)
                                    Text("+1.23%", color = Color(0xFF089981), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Divider(color = Color(0xFF2A2E39))

                    // Symbol Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(getFullSymbolName(symbol).uppercase(), color = Color(0xFF787B86), fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("65,432.10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                Text("+1,234.56 (+1.23%)", color = Color(0xFF089981), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ShowChart, null, tint = Color(0xFF089981), modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}
