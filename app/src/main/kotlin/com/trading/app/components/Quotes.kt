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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.util.Locale

private val defaultQuotesCatalog = listOf(
    SymbolInfo("BTCUSD", "Bitcoin / U.S. Dollar", "Bitstamp", "spot crypto"),
    SymbolInfo("BTCUSDT", "Bitcoin / TetherUS", "Binance", "spot crypto"),
    SymbolInfo("ETHUSD", "Ethereum / U.S. Dollar", "Bitstamp", "spot crypto"),
    SymbolInfo("ETHUSDT", "Ethereum / TetherUS", "Binance", "spot crypto"),
    SymbolInfo("EURUSD", "Euro / U.S. Dollar", "FXCM", "forex"),
    SymbolInfo("GBPUSD", "British Pound / U.S. Dollar", "FXCM", "forex"),
    SymbolInfo("USDJPY", "U.S. Dollar / Japanese Yen", "FXCM", "forex"),
    SymbolInfo("AUDUSD", "Australian Dollar / U.S. Dollar", "OANDA", "forex"),
    SymbolInfo("USDCAD", "U.S. Dollar / Canadian Dollar", "FXCM", "forex"),
    SymbolInfo("USDCHF", "U.S. Dollar / Swiss Franc", "FXCM", "forex"),
    SymbolInfo("USOIL", "WTI Crude Oil", "TVC", "commodity cfd"),
    SymbolInfo("US02Y", "United States 2Y Gov Bond", "TVC", "bond"),
    SymbolInfo("US10Y", "United States 10Y Gov Bond", "TVC", "bond"),
    SymbolInfo("SPX", "S&P 500 Index", "S&P", "index"),
    SymbolInfo("TSLA", "Tesla, Inc.", "NASDAQ", "stock"),
    SymbolInfo("AAPL", "Apple Inc.", "NASDAQ", "stock"),
    SymbolInfo("NVDA", "NVIDIA Corporation", "NASDAQ", "stock"),
    SymbolInfo("NASDAQ100", "Nasdaq 100 Index", "NASDAQ", "index"),
    SymbolInfo("XAGUSD", "Silver / U.S. Dollar", "OANDA", "commodity cfd"),
    SymbolInfo("XAUUSD", "Gold / U.S. Dollar", "OANDA", "commodity cfd"),
    SymbolInfo("MSFT", "Microsoft Corporation", "NASDAQ", "stock"),
    SymbolInfo("AMZN", "Amazon.com, Inc.", "NASDAQ", "stock"),
    SymbolInfo("DJIA", "Dow Jones Industrial Average", "DJI", "index"),
    SymbolInfo("DGS2", "US 2-Year Treasury Yield", "FRED", "bond"),
    SymbolInfo("DGS10", "US 10-Year Treasury Yield", "FRED", "bond"),
    SymbolInfo("BRENTOIL", "Brent Crude Oil", "TVC", "commodity cfd")
)

private val allowedQuoteTickers = defaultQuotesCatalog
    .map { it.ticker.uppercase(Locale.US) }
    .toSet()

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

fun defaultQuoteSymbols(): List<SymbolInfo> = defaultQuotesCatalog.map { quote ->
    quote.copy(brokerSymbol = defaultBrokerSymbolFor(quote.ticker, quote.type))
}

fun mergeQuoteCatalog(symbols: List<SymbolInfo>): List<SymbolInfo> {
    val incomingByTicker = symbols
        .asSequence()
        .mapNotNull { quote ->
            val ticker = quote.ticker.trim().uppercase(Locale.US)
            if (ticker.isEmpty() || ticker !in allowedQuoteTickers) return@mapNotNull null

            val brokerSymbol = quote.brokerSymbol.trim().ifBlank {
                defaultBrokerSymbolFor(ticker, quote.type)
            }
            quote.copy(
                ticker = ticker,
                brokerSymbol = brokerSymbol,
                name = quote.name.ifBlank { ticker }
            )
        }
        .groupBy { it.ticker }
        .mapValues { (_, candidates) ->
            candidates.maxByOrNull { candidate ->
                (if (!candidate.brokerSymbol.equals(candidate.ticker, ignoreCase = true)) 2 else 0) +
                    (if (!candidate.name.equals(candidate.ticker, ignoreCase = true)) 1 else 0)
            } ?: candidates.first()
        }

    return defaultQuoteSymbols().map { defaultQuote ->
        val incoming = incomingByTicker[defaultQuote.ticker.uppercase(Locale.US)] ?: return@map defaultQuote
        defaultQuote.copy(
            name = incoming.name.ifBlank { defaultQuote.name },
            exchange = incoming.exchange.ifBlank { defaultQuote.exchange },
            type = incoming.type.ifBlank { defaultQuote.type },
            brokerSymbol = incoming.brokerSymbol.ifBlank { defaultQuote.brokerSymbol },
            price = incoming.price,
            change = incoming.change,
            changePercent = incoming.changePercent
        )
    }
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
    onQuoteSelect: (String) -> Unit,
    quotesByTicker: Map<String, SymbolQuote> = emptyMap(),
    onVisibleSymbolsChanged: (List<String>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val quoteCatalog = quotes.toList()
    val categories = listOf("All", "Stocks", "Forex", "Crypto", "Indices", "Bonds", "Commodities")

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
                "Commodities" -> quote.type.contains("commodity", ignoreCase = true) || quote.type.contains("cfd", ignoreCase = true)
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
                    items(filteredQuotes) { item ->
                        val liveQuote = resolveQuoteForSymbol(item, quotesByTicker)
                        QuoteListItem(
                            quoteInfo = item,
                            quote = liveQuote,
                            onSelect = {
                                onQuoteSelect(item.ticker)
                                onClose()
                            }
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
    quote: SymbolQuote?,
    onSelect: () -> Unit
) {
    val changePercent = quote?.changePercent ?: 0f
    val isUp = (quote?.change ?: changePercent) >= 0f
    val numberColor = if (isUp) Color(0xFF089981) else Color(0xFFF23645)
    val changeText = if (quote != null) {
        "${formatSignedChange(quoteInfo.ticker, quote.change)} ${String.format(Locale.US, "%+.2f%%", quote.changePercent)}"
    } else {
        "--"
    }

    val displayPrice = quote?.let {
        when {
            it.lastPrice > 0f -> it.lastPrice
            it.bid > 0f -> it.bid
            else -> it.ask
        }
    }
    val displayLow = quote?.let { if (it.low > 0f) it.low else (displayPrice ?: 0f) }
    val displayHigh = quote?.let { if (it.high > 0f) it.high else (displayPrice ?: 0f) }
    val displayBid = quote?.let { if (it.bid > 0f) it.bid else (displayPrice ?: 0f) }
    val displayAsk = quote?.let { if (it.ask > 0f) it.ask else (displayPrice ?: 0f) }

    val priceText = displayPrice?.let { formatQuoteValue(quoteInfo.ticker, it) } ?: "--"
    val bidText = displayBid?.let { formatQuoteValue(quoteInfo.ticker, it) } ?: "--"
    val askText = displayAsk?.let { formatQuoteValue(quoteInfo.ticker, it) } ?: "--"
    val lowText = displayLow?.let { formatQuoteValue(quoteInfo.ticker, it) } ?: "--"
    val highText = displayHigh?.let { formatQuoteValue(quoteInfo.ticker, it) } ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetIcon(quoteInfo)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = changeText,
                color = if (quote == null) Color(0xFF787B86) else numberColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = quoteInfo.ticker,
                color = Color(0xFFD1D4DC),
                fontWeight = FontWeight.Bold,
                fontSize = 17.5.sp
            )
            Text(
                text = quoteInfo.name,
                color = Color(0xFF787B86),
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(min = 150.dp)
        ) {
            Text(
                text = priceText,
                color = if (quote == null) Color(0xFF787B86) else numberColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bid: $bidText   Ask: $askText",
                color = Color(0xFF787B86),
                fontSize = 10.sp
            )
            Text(
                text = "L: $lowText   H: $highText",
                color = Color(0xFF787B86),
                fontSize = 10.sp
            )
        }
    }
}
