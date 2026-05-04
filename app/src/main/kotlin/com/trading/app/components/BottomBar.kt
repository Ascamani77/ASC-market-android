package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.trading.app.models.ChartSettings
import com.trading.app.models.SymbolInfo
import com.trading.app.data.Mt5Service
import java.util.Locale

@Composable
fun BottomBar(
    onRangeClick: (String) -> Unit = {},
    onGoToClick: () -> Unit = {},
    onTabClick: (String) -> Unit = {},
    activeTab: String? = null,
    recentPairs: List<Pair<String, String>> = emptyList(),
    currentSymbol: String = "",
    currentTimeframe: String = "",
    onPairSelect: (String, String) -> Unit = { _, _ -> },
    backgroundColor: Color = Color(0xFF08090C),
    settings: ChartSettings = ChartSettings(),
    currentQuote: SymbolQuote? = null,
    recentPairQuotes: Map<String, SymbolQuote> = emptyMap(),
    onAccountUpdate: (Mt5Service.AccountInfo) -> Unit = {},
    selectedTzLabel: String = "",
    onVisibleSymbolsChanged: (List<String>) -> Unit = {}
) {
    val pairsScrollState = rememberScrollState()
    val listState = remember { LazyListState() }

    // Visibility tracking
    val visibleItems = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                recentPairs.getOrNull(item.index)?.first
            }
        }
    }

    LaunchedEffect(visibleItems.value) {
        onVisibleSymbolsChanged(visibleItems.value)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        if (recentPairs.isNotEmpty()) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.4.dp)
                    .padding(start = 1.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(recentPairs) { index, (symbol, timeframe) ->
                    val isActive = symbol == currentSymbol && timeframe == currentTimeframe
                    val quoteKey = (symbol + "_" + timeframe).uppercase(Locale.US)
                    val rowQuote = if (isActive) currentQuote else recentPairQuotes[quoteKey]
                    val displayChange = rowQuote?.let {
                        String.format(Locale.US, "%+.2f %%", it.changePercent)
                    } ?: "--"
                    val isUp = (rowQuote?.changePercent ?: 0f) >= 0f
                    val displayPrice = rowQuote?.let {
                        val decimals = when {
                            symbol.uppercase(Locale.US).endsWith("JPY") -> 3
                            symbol.length == 6 && (symbol.contains("USD") || symbol.contains("EUR")) -> 5
                            it.lastPrice >= 1000f -> 2
                            it.lastPrice >= 1f -> 2
                            it.lastPrice >= 0.1f -> 4
                            else -> 6
                        }
                        String.format(Locale.US, "%,.${decimals}f", it.lastPrice)
                    } ?: "--"

                    val symbolInfo = remember(symbol) {
                        val type = when {
                            symbol.startsWith("BTC") || symbol.startsWith("ETH") || symbol.startsWith("SOL") -> "Crypto"
                            symbol.length == 6 && (symbol.contains("USD") || symbol.contains("EUR") || symbol.contains("JPY")) -> "Forex"
                            else -> "Stock"
                        }
                        SymbolInfo(ticker = symbol, name = "", type = type)
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = if (index == 0) 0.dp else 2.8.dp, end = 2.8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isActive) Color(0xFF131722) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isActive) Color(0xFF363A45) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onPairSelect(symbol, timeframe) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetIcon(symbolInfo, size = 24)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$symbol,$timeframe",
                                color = if (isActive) Color.White else Color(0xFFD1D4DC),
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = displayPrice,
                                color = when {
                                    rowQuote == null -> if (isActive) Color.White else Color(0xFFD1D4DC)
                                    isUp -> Color(0xFF089981)
                                    else -> Color(0xFFF23645)
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (rowQuote != null) {
                                Icon(
                                    imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isUp) Color(0xFF089981) else Color(0xFFF23645),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = displayChange,
                                color = when {
                                    rowQuote == null -> Color(0xFF787B86)
                                    isUp -> Color(0xFF089981)
                                    else -> Color(0xFFF23645)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            // Divider(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFF2A2E39))
        }
    }
}
