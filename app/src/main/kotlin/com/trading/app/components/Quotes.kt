package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.trading.app.models.SymbolInfo
import com.trading.app.data.ChartFeedType
import com.trading.app.data.chartFeedQuotes
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale

private fun defaultBrokerSymbolFor(ticker: String, type: String): String {
    val normalizedTicker = ticker.trim()
    if (normalizedTicker.isEmpty()) return normalizedTicker
    if (normalizedTicker.endsWith("m", ignoreCase = true)) return normalizedTicker

    return when {
        normalizedTicker.equals("SPX", ignoreCase = true) -> "US500m"
        normalizedTicker.equals("NASDAQ100", ignoreCase = true) -> "USTECm"
        normalizedTicker.equals("DJIA", ignoreCase = true) -> "US30m"
        normalizedTicker.equals("BRENTOIL", ignoreCase = true) -> "UKOILm"
        type.contains("forex", ignoreCase = true) -> "${normalizedTicker}m"
        type.contains("crypto", ignoreCase = true) && normalizedTicker.endsWith("USD", ignoreCase = true) -> "${normalizedTicker}m"
        type.contains("commodity", ignoreCase = true) -> "${normalizedTicker}m"
        else -> normalizedTicker
    }
}

fun defaultQuoteSymbols(): List<SymbolInfo> {
    val feedQuotes = chartFeedQuotes(ChartFeedType.EXNESS)
    
    val constantsQuotes = com.asc.markets.data.FOREX_PAIRS.map { pair ->
        val ticker = pair.symbol.replace("/", "")
        com.trading.app.models.SymbolInfo(
            ticker = ticker,
            name = pair.name,
            exchange = "Exness",
            type = pair.category.name.lowercase(Locale.US),
            brokerSymbol = pair.symbol,
            price = pair.price.toFloat(),
            change = pair.change.toFloat(),
            changePercent = pair.changePercent.toFloat()
        )
    }
    
    return (feedQuotes + constantsQuotes).distinctBy { it.ticker }
}

fun mergeQuoteCatalog(symbols: List<SymbolInfo>): List<SymbolInfo> {
    return mergeQuoteCatalog(symbols, defaultQuoteSymbols())
}

fun mergeQuoteCatalog(symbols: List<SymbolInfo>, baseQuotes: List<SymbolInfo>): List<SymbolInfo> {
    val baseByTicker = baseQuotes
        .groupBy { it.ticker.trim().uppercase(Locale.US) }
        .mapValues { (_, candidates) -> candidates.first() }

    // The MT5 broker symbol list is the source of truth - every symbol returned by the
    // bridge appears in the quote search. The static catalog only enriches metadata.
    val fromMt5 = symbols
        .asSequence()
        .mapNotNull { quote ->
            val ticker = quote.ticker.trim().uppercase(Locale.US)
            if (ticker.isEmpty()) return@mapNotNull null

            val base = baseByTicker[ticker]
            val brokerSymbol = quote.brokerSymbol.trim().ifBlank {
                defaultBrokerSymbolFor(ticker, quote.type)
            }
            quote.copy(
                ticker = ticker,
                brokerSymbol = brokerSymbol,
                name = quote.name.ifBlank { base?.name ?: ticker },
                exchange = quote.exchange.ifBlank { base?.exchange ?: "Exness" }
            )
        }
        .groupBy { it.ticker }
        .mapValues { (_, candidates) ->
            candidates.maxByOrNull { candidate ->
                (if (!candidate.brokerSymbol.equals(candidate.ticker, ignoreCase = true)) 2 else 0) +
                    (if (!candidate.name.equals(candidate.ticker, ignoreCase = true)) 1 else 0)
            } ?: candidates.first()
        }

    val merged = fromMt5.values.toMutableList()

    // Keep catalog-only entries until MT5 reports them so the list is never empty pre-sync.
    baseQuotes.forEach { defaultQuote ->
        if (defaultQuote.ticker.uppercase(Locale.US) !in fromMt5.keys) merged.add(defaultQuote)
    }

    return merged
}

private fun isForexTicker(ticker: String): Boolean {
    val normalized = ticker.uppercase(Locale.US)
    return normalized.length == 6 &&
        normalized.take(3).all { it.isLetter() } &&
        normalized.takeLast(3).all { it.isLetter() }
}

private fun quoteDecimalsFor(ticker: String, value: Float): Int {
    val normalized = ticker.uppercase(Locale.US)
    return when {
        isForexTicker(normalized) && normalized.endsWith("JPY") -> 3
        isForexTicker(normalized) -> 5
        value >= 1000f -> 2
        value >= 1f -> 2
        value >= 0.1f -> 4
        else -> 6
    }
}

private fun formatQuoteValue(ticker: String, value: Float): String {
    val decimals = quoteDecimalsFor(ticker, value)
    return String.format(Locale.US, "%,.${decimals}f", value)
}

private fun formatSignedChange(ticker: String, value: Float): String {
    val decimals = if (isForexTicker(ticker)) 5 else 2
    return String.format(Locale.US, "%+.${decimals}f", value)
}

private fun quoteLookupKeys(vararg identifiers: String): List<String> {
    val keys = mutableListOf<String>()

    identifiers
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { identifier ->
            val upperIdentifier = identifier.uppercase(Locale.US)
            keys += upperIdentifier
            keys += upperIdentifier.removeSuffix("M")
            keys += upperIdentifier.removeSuffix(".M")
            keys += upperIdentifier.removeSuffix(".PRO")
            keys += upperIdentifier.removeSuffix(".ECN")
            keys += upperIdentifier.removeSuffix(".S")
            keys += upperIdentifier.removeSuffix(".SPOT")
            keys += upperIdentifier.removeSuffix("+")
            keys += upperIdentifier.removeSuffix(".P")
        }

    return keys.distinct()
}

private fun resolveQuoteForSymbol(
    symbol: SymbolInfo,
    quotesByTicker: Map<String, SymbolQuote>
): SymbolQuote? {
    for (key in quoteLookupKeys(symbol.ticker, symbol.brokerSymbol)) {
        val quote = quotesByTicker[key]
        if (quote != null) return quote
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Quotes(
    onClose: () -> Unit,
    quotes: List<SymbolInfo> = defaultQuoteSymbols(),
    onQuoteSelect: (SymbolInfo) -> Unit,
    quotesByTicker: Map<String, SymbolQuote> = emptyMap(),
    onVisibleSymbolsChanged: (List<String>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val quoteCatalog = quotes.toList()
    
    val categories = remember(quoteCatalog) {
        val baseCategories = mutableListOf("All")
        val types = quoteCatalog.map { it.type.lowercase(Locale.US) }
        
        if (types.any { it.contains("stock") }) baseCategories.add("Stocks")
        if (types.any { it.contains("forex") }) baseCategories.add("Forex")
        if (types.any { it.contains("crypto") }) baseCategories.add("Crypto")
        if (types.any { it.contains("index") }) baseCategories.add("Indices")
        if (types.any { it.contains("bond") }) baseCategories.add("Bonds")
        if (types.any { it.contains("commodity") || it.contains("cfd") }) baseCategories.add("Commodities")
        
        baseCategories.distinct()
    }

    val filteredQuotes = remember(quoteCatalog, searchQuery, selectedCategory) {
        quoteCatalog.filter { quote ->
            val matchesSearch = quote.ticker.contains(searchQuery, ignoreCase = true) ||
                quote.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Stocks" -> quote.type.contains("stock", ignoreCase = true)
                "Forex" -> quote.type.contains("forex", ignoreCase = true)
                "Crypto" -> quote.type.contains("crypto", ignoreCase = true)
                "Bonds" -> quote.type.contains("bond", ignoreCase = true)
                "Indices" -> quote.type.contains("index", ignoreCase = true)
                "Commodities" -> (quote.type.contains("commodity", ignoreCase = true) || quote.type.contains("cfd", ignoreCase = true)) && !quote.type.contains("crypto", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    val listState = rememberLazyListState()
    val visibleSymbols by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                filteredQuotes.getOrNull(item.index)?.let {
                    it.brokerSymbol.ifBlank { it.ticker }
                }
            }
        }
    }

    LaunchedEffect(visibleSymbols) {
        onVisibleSymbolsChanged(visibleSymbols.distinctBy { it.uppercase(Locale.US) })
    }

    DisposableEffect(Unit) {
        onDispose {
            onVisibleSymbolsChanged(emptyList())
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = {
                            Text(
                                "Search quotes",
                                color = Color(0xFF787B86),
                                fontSize = 16.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            cursorColor = Color(0xFF2962FF),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFF2A2E39) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else Color(0xFF787B86),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredQuotes,
                        key = { it.ticker + it.exchange }
                    ) { item ->
                        val liveQuote = resolveQuoteForSymbol(item, quotesByTicker)
                        QuoteListItem(
                            quoteInfo = item,
                            onSelect = {
                                onQuoteSelect(item)
                                onClose()
                            },
                            liveQuote = liveQuote
                        )
                        Divider(color = Color(0xFF121212), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteListItem(
    quoteInfo: SymbolInfo,
    onSelect: () -> Unit,
    liveQuote: SymbolQuote? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon(quoteInfo, size = 40)
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quoteInfo.ticker,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = quoteInfo.name,
                color = Color(0xFF787B86),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            // Display actual price (use live quote if available, otherwise show placeholder)
            val priceToShow = liveQuote?.lastPrice ?: quoteInfo.price
            val changeToShow = liveQuote?.changePercent ?: quoteInfo.changePercent
            Text(
                text = formatQuoteValue(quoteInfo.ticker, priceToShow),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${if (changeToShow >= 0) "+" else ""}${String.format(Locale.US, "%.2f%%", changeToShow)}",
                color = if (changeToShow >= 0) Color(0xFF089981) else Color(0xFFF23645),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun SourceLogo(exchange: String) {
    val logoColor = when (exchange.lowercase()) {
        "binance" -> Color(0xFFF3BA2F)
        "exness" -> Color(0xFFFFD500)
        
        else -> Color(0xFF787B86)
    }
    
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(logoColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = exchange.take(1).uppercase(),
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}
