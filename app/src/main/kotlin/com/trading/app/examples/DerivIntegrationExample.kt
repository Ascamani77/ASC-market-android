package com.trading.app.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.components.SymbolQuote
import com.trading.app.data.DerivService
import com.trading.app.data.MarketDataSourceManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example screen demonstrating Deriv WebSocket integration
 * Shows real-time prices for BTC/USD, ETH/USD, and commodities
 */
@Composable
fun DerivIntegrationExample() {
    var quotes by remember { mutableStateOf<Map<String, SymbolQuote>>(emptyMap()) }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var lastUpdate by remember { mutableStateOf("Never") }
    
    val symbols = listOf(
        "BTCUSD" to "Bitcoin",
        "ETHUSD" to "Ethereum",
        "XAUUSD" to "Gold",
        "XAGUSD" to "Silver",
        "BROUSD" to "Brent Crude",
        "WTIUSD" to "WTI Crude"
    )
    
    // Create Deriv service
    val derivService = remember {
        DerivService(
            onQuoteUpdate = { quote ->
                quotes = quotes + (quote.name to quote)
                lastUpdate = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(quote.time))
            }
        )
    }
    
    // Connect on launch
    LaunchedEffect(Unit) {
        derivService.connect()
        connectionStatus = "Connecting..."
        
        // Wait a bit for connection
        kotlinx.coroutines.delay(1000)
        
        if (derivService.isConnected()) {
            connectionStatus = "Connected"
            // Subscribe to all symbols
            symbols.forEach { (symbol, _) ->
                derivService.subscribe(symbol)
            }
        } else {
            connectionStatus = "Failed to connect"
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            derivService.disconnect()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Deriv Live Market Data",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Connection status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when (connectionStatus) {
                                "Connected" -> Color(0xFF10B981)
                                "Connecting..." -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = connectionStatus,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = "Last: $lastUpdate",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
        
        // Symbol list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(symbols) { (symbol, name) ->
                val quote = quotes[symbol]
                SymbolCard(
                    symbol = symbol,
                    name = name,
                    quote = quote
                )
            }
        }
    }
}

@Composable
fun SymbolCard(
    symbol: String,
    name: String,
    quote: SymbolQuote?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = symbol,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            if (quote != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.2f", quote.lastPrice),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    if (quote.bid > 0 && quote.ask > 0) {
                        Row {
                            Text(
                                text = "Bid: ${String.format("%.2f", quote.bid)}",
                                fontSize = 10.sp,
                                color = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ask: ${String.format("%.2f", quote.ask)}",
                                fontSize = 10.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Waiting...",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * Example using MarketDataSourceManager for smart routing
 */
@Composable
fun SmartRoutingExample() {
    var currentSymbol by remember { mutableStateOf("BTCUSD") }
    var currentPrice by remember { mutableStateOf(0f) }
    var dataSource by remember { mutableStateOf("Unknown") }
    
    val dataSourceManager = remember {
        MarketDataSourceManager(
            onQuoteUpdate = { quote ->
                if (quote.name.equals(currentSymbol, ignoreCase = true)) {
                    currentPrice = quote.lastPrice
                }
            }
        )
    }
    
    LaunchedEffect(Unit) {
        dataSourceManager.connectDeriv()
    }
    
    LaunchedEffect(currentSymbol) {
        val source = dataSourceManager.getDataSourceForSymbol(currentSymbol)
        dataSource = source.name
        dataSourceManager.streamActiveSymbol(currentSymbol)
        dataSourceManager.fetchHistory(currentSymbol, "1h")
    }
    
    DisposableEffect(Unit) {
        onDispose {
            dataSourceManager.disconnectAll()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27))
            .padding(16.dp)
    ) {
        Text(
            text = "Smart Data Routing",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Symbol selector
        val symbols = listOf("BTCUSD", "ETHUSD", "BTCUSDT", "EURUSD", "XAUUSD")
        
        symbols.forEach { symbol ->
            Button(
                onClick = { currentSymbol = symbol },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentSymbol == symbol) 
                        Color(0xFF3B82F6) 
                    else 
                        Color(0xFF1E293B)
                )
            ) {
                Text(symbol)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Current data
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = currentSymbol,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "Price: ${String.format("%.2f", currentPrice)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Text(
                    text = "Data Source: $dataSource",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
