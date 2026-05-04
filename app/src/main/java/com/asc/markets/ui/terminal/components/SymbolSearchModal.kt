package com.asc.markets.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.asc.markets.data.MarketDataStore
import com.asc.markets.ui.terminal.theme.*

@Composable
fun SymbolSearchModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    if (!isOpen) return

    var query by remember { mutableStateOf("") }
    val pairs by MarketDataStore.allPairs.collectAsState()
    val symbols = remember(pairs, query) {
        pairs
            .map { pair ->
                SymbolItem(
                    ticker = pair.symbol,
                    name = pair.name,
                    exchange = exchangeFor(pair.symbol),
                    type = pair.category.name.lowercase(),
                    category = pair.category.name
                )
            }
            .filter {
                it.ticker.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true)
            }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            color = Color(0xFF1E1E1E),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF363A45))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYMBOL SEARCH",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                    }
                }
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Symbol, ISIN, or CUSIP", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2D2D2D),
                        unfocusedContainerColor = Color(0xFF2D2D2D),
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color(0xFF2A2E39),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(symbols) { item ->
                        SymbolListItem(item) {
                            onSelect(item.ticker)
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

data class SymbolItem(val ticker: String, val name: String, val exchange: String, val type: String, val category: String)

private fun exchangeFor(symbol: String): String {
    return when {
        symbol.endsWith("/USDT") || symbol.endsWith("/USD") -> "BINANCE"
        symbol.contains("/") -> "FX"
        else -> "MARKET"
    }
}

@Composable
private fun SymbolListItem(item: SymbolItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.ticker,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.name,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.exchange,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.type,
                color = Color.Gray,
                fontSize = 9.sp
            )
        }
    }
}
