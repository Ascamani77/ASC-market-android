package com.trading.app.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.trading.app.models.NewsItem
import com.trading.app.models.SymbolInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun NewsPage(
    newsItems: List<NewsItem>,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Top Stories",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2962FF), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Waiting for market news...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (newsItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No news available right now.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(newsItems) { item ->
                    NewsRow(item)
                    Divider(color = Color(0xFF2A2E39), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun NewsRow(item: NewsItem) {
    val symbolInfo = remember(item.title) { deriveAssetFromHeadline(item.title) }
    val timeText = item.timeLabel.ifBlank { getRelativeTime(item.isoDateTime) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open details page later if needed */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (symbolInfo != null) {
                    if (symbolInfo.type.equals("stock", ignoreCase = true)) {
                        CompanyLogoIcon(symbolInfo = symbolInfo, size = 22)
                    } else {
                        AssetIcon(symbol = symbolInfo, size = 22)
                    }
                } else if (item.countryCode.length == 2 && item.countryCode != "WW") {
                    AsyncImage(
                        model = "https://flagcdn.com/w80/${item.countryCode.lowercase(Locale.US)}.png",
                        contentDescription = item.countryCode,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2E39)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.category.firstOrNull()?.uppercase() ?: "?",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timeText,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                color = Color(0xFFE5E5E5),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun CompanyLogoIcon(symbolInfo: SymbolInfo, size: Int) {
    val ticker = symbolInfo.ticker.uppercase(Locale.US).replace("/", "")
    val logoUrl = officialCompanyLogoUrl(ticker)

    if (logoUrl == null) {
        AssetIcon(symbol = symbolInfo, size = size)
        return
    }

    val painter = rememberAsyncImagePainter(model = logoUrl)
    val state = painter.state

    if (state is AsyncImagePainter.State.Success) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color(0xFF101318)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = ticker,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        AssetIcon(symbol = symbolInfo, size = size)
    }
}

private fun officialCompanyLogoUrl(ticker: String): String? {
    if (ticker.isBlank()) return null
    if (!ticker.all { it.isLetterOrDigit() }) return null
    return "https://financialmodelingprep.com/image-stock/${ticker}.png"
}

private val COMPANY_KEYWORDS_TO_TICKER = listOf(
    "APPLE" to "AAPL",
    "MICROSOFT" to "MSFT",
    "GOOGLE" to "GOOGL",
    "ALPHABET" to "GOOGL",
    "AMAZON" to "AMZN",
    "TESLA" to "TSLA",
    "NVIDIA" to "NVDA",
    "META PLATFORMS" to "META",
    "FACEBOOK" to "META",
    "NETFLIX" to "NFLX",
    "INTEL" to "INTC",
    "AMD" to "AMD",
    "QUALCOMM" to "QCOM",
    "BROADCOM" to "AVGO",
    "ORACLE" to "ORCL",
    "SALESFORCE" to "CRM",
    "ADOBE" to "ADBE",
    "PALANTIR" to "PLTR",
    "DISNEY" to "DIS",
    "BOEING" to "BA",
    "COCA-COLA" to "KO",
    "COCA COLA" to "KO",
    "PEPSICO" to "PEP",
    "WALMART" to "WMT",
    "COSTCO" to "COST",
    "TARGET" to "TGT",
    "HOME DEPOT" to "HD",
    "NIKE" to "NKE",
    "STARBUCKS" to "SBUX",
    "JPMORGAN" to "JPM",
    "GOLDMAN SACHS" to "GS",
    "MORGAN STANLEY" to "MS",
    "BANK OF AMERICA" to "BAC",
    "WELLS FARGO" to "WFC",
    "CITIGROUP" to "C",
    "VISA" to "V",
    "MASTERCARD" to "MA",
    "PAYPAL" to "PYPL",
    "BLACKROCK" to "BLK",
    "EXXON" to "XOM",
    "CHEVRON" to "CVX",
    "SHELL" to "SHEL",
    "CONOCOPHILLIPS" to "COP",
    "TOTALENERGIES" to "TTE",
    "UBER" to "UBER",
    "LYFT" to "LYFT",
    "AIRBNB" to "ABNB",
    "COINBASE" to "COIN",
    "MICROSTRATEGY" to "MSTR",
    "MARATHON DIGITAL" to "MARA",
    "ROBINHOOD" to "HOOD",
    "SHOPIFY" to "SHOP",
    "SPOTIFY" to "SPOT",
    "FEDEX" to "FDX",
    "UPS" to "UPS",
    "AMERICAN AIRLINES" to "AAL",
    "SOUTHWEST AIRLINES" to "LUV",
    "UNITED AIRLINES" to "UAL",
    "DELTA AIR LINES" to "DAL",
    "FORD" to "F",
    "GENERAL MOTORS" to "GM",
    "TOYOTA" to "TM",
    "HONDA" to "HMC",
    "TAIWAN SEMICONDUCTOR" to "TSM",
    "TSMC" to "TSM",
    "ALIBABA" to "BABA",
    "TENCENT" to "TCEHY",
    "BAIDU" to "BIDU",
    "NIO" to "NIO",
    "XPENG" to "XPEV",
    "LI AUTO" to "LI",
    "ELI LILLY" to "LLY",
    "PFIZER" to "PFE",
    "MODERNA" to "MRNA",
    "MERCK" to "MRK",
    "JOHNSON & JOHNSON" to "JNJ",
    "JOHNSON AND JOHNSON" to "JNJ",
    "ABBVIE" to "ABBV",
    "AMGEN" to "AMGN",
    "NOVO NORDISK" to "NVO",
    "ASTRAZENECA" to "AZN",
    "BIONTECH" to "BNTX",
    "BRISTOL MYERS" to "BMY",
    "CVS" to "CVS"
)

private fun inferTickerFromCompanyName(upperTitle: String): String? {
    return COMPANY_KEYWORDS_TO_TICKER.firstOrNull { (keyword, _) ->
        upperTitle.contains(keyword)
    }?.second
}

private fun deriveAssetFromHeadline(headline: String): SymbolInfo? {
    val title = headline.trim()
    if (title.isEmpty()) return null
    val upperTitle = title.uppercase(Locale.US)
    val companyTicker = inferTickerFromCompanyName(upperTitle)

    val leadingSymbol = Regex("^([A-Z]{2,10}(?:/[A-Z]{2,10})?)\\s*[:\\-]")
        .find(upperTitle)
        ?.groupValues
        ?.getOrNull(1)

    val token = when {
        !leadingSymbol.isNullOrBlank() -> leadingSymbol
        !companyTicker.isNullOrBlank() -> companyTicker
        upperTitle.contains("BITCOIN") -> "BTC/USD"
        upperTitle.contains("ETHEREUM") -> "ETH/USD"
        upperTitle.contains("SOLANA") -> "SOL/USD"
        upperTitle.contains("GOLD") -> "XAU/USD"
        upperTitle.contains("SILVER") -> "XAG/USD"
        upperTitle.contains("NASDAQ") -> "IXIC"
        upperTitle.contains("DOW JONES") -> "DJI"
        upperTitle.contains("S&P 500") || upperTitle.contains("S&P500") -> "SPX"
        upperTitle.contains("EUR/USD") -> "EUR/USD"
        upperTitle.contains("GBP/USD") -> "GBP/USD"
        upperTitle.contains("USD/JPY") -> "USD/JPY"
        else -> null
    } ?: return null

    val normalizedTicker = token.replace("/", "")
    val type = when {
        normalizedTicker.startsWith("XAU") || normalizedTicker.startsWith("XAG") -> "commodity"
        normalizedTicker in setOf("IXIC", "DJI", "SPX", "NDX", "RUT", "NIFTY", "BANKNIFTY", "DAX", "FTSE") -> "index"
        normalizedTicker.length == 6 &&
            isFiatCode(normalizedTicker.take(3)) &&
            isFiatCode(normalizedTicker.takeLast(3)) -> "forex"
        normalizedTicker in setOf("BTC", "ETH", "SOL", "XRP", "DOGE", "ADA", "LTC", "BNB", "USDT") -> "crypto"
        normalizedTicker.endsWith("USD") &&
            normalizedTicker.dropLast(3) in setOf("BTC", "ETH", "SOL", "XRP", "DOGE", "ADA", "LTC", "BNB") -> "crypto"
        normalizedTicker.length in 1..6 -> "stock"
        else -> "stock"
    }

    return SymbolInfo(
        ticker = normalizedTicker,
        name = normalizedTicker,
        type = type
    )
}

private fun isFiatCode(code: String): Boolean {
    return code in setOf("USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "NZD", "CNY", "HKD", "SGD")
}

fun getRelativeTime(isoDateTime: String): String {
    return try {
        val date = parseIsoDate(isoDateTime) ?: return ""
        val now = System.currentTimeMillis()
        val diff = now - date.time

        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    } catch (_: Exception) {
        ""
    }
}

private fun parseIsoDate(isoDateTime: String): Date? {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = sdf.parse(isoDateTime)
            if (parsed != null) return parsed
        } catch (_: Exception) {
        }
    }
    return null
}
