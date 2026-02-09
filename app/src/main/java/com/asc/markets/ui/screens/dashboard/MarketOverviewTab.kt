package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.data.MarketCategory
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.MiniChart
import com.asc.markets.ui.components.ForexIcon
import com.asc.markets.ui.theme.*
import com.asc.markets.state.AssetContext
import com.asc.markets.state.AssetContextStore
import com.asc.markets.state.mapCategoryToAssetContext
import com.asc.markets.state.MarketOverviewConfigs
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import com.asc.markets.BuildConfig

// --- Data model ---
data class NewsItem(
    val headline: String,
    val source: String,
    val timestamp: String,
    val assetType: String,
    val imageUrl: String = ""
)

// --- Networking: fetch from OpenAI (uses BuildConfig.OPENAI_API_KEY) ---
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun fetchNewsFromGemini(ctx: com.asc.markets.state.AssetContext? = null): List<NewsItem> {
    return try {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) {
            android.util.Log.e("AscNews", "OPENAI API key missing in BuildConfig")
            return emptyList()
        }

        val activeCtx = ctx ?: AssetContextStore.get()
        val promptPrefix = AssetContextStore.aiPromptPrefix()
        val basePrompt = com.asc.markets.ai.AiPrompts.buildNewsPrompt()
        // Add hard constraint per asset-scope requirement
        val hardConstraint = "ONLY analyze assets within the active AssetContext: ${activeCtx.name}. Explicitly ignore all others."
        val prompt = "$promptPrefix. $hardConstraint. $basePrompt"

        val bodyJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.7)
            put("max_tokens", 1200)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val url = URL("https://api.openai.com/v1/chat/completions")
        val responseText = withContext(Dispatchers.IO) {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            conn.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

            val code = conn.responseCode
            android.util.Log.d("AscNews", "OpenAI response code: $code")
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                android.util.Log.e("AscNews", "OpenAI HTTP error: $code $err")
                ""
            }
            conn.disconnect()
            text
        }

        if (responseText.isBlank()) {
            android.util.Log.d("AscNews", "Empty responseText from OpenAI")
            return emptyList()
        }

        // Extract assistant content: choices[0].message.content
        val topJson = JSONObject(responseText)
        val choices = topJson.optJSONArray("choices")
        val contentStr = if (choices != null && choices.length() > 0) {
            val msgObj = choices.getJSONObject(0).optJSONObject("message")
            msgObj?.optString("content") ?: ""
        } else {
            ""
        }

        if (contentStr.isBlank()) {
            android.util.Log.d("AscNews", "No content in choices, falling back")
            return emptyList()
        }

        // Try parsing content as a JSON array directly; handle surrounding text if present
        val jsonArray = try {
            JSONArray(contentStr)
        } catch (e: Exception) {
            val start = contentStr.indexOf('[')
            val end = contentStr.lastIndexOf(']')
            if (start != -1 && end != -1 && end > start) {
                JSONArray(contentStr.substring(start, end + 1))
            } else {
                throw e
            }
        }

        val items = mutableListOf<NewsItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val headline = obj.optString("headline", "").trim()
            if (headline.isEmpty()) continue
            val source = obj.optString("source", "Market")
            val timestamp = obj.optString("timestamp", "Just now")
            val assetType = obj.optString("assetType", "forex")
            val imageUrl = obj.optString("imageUrl", "")
            items.add(NewsItem(headline, source, timestamp, assetType, imageUrl))
        }

        android.util.Log.d("AscNews", "Parsed ${items.size} news items from OpenAI")
        // Defensive filter: ensure returned items are within requested context when ctx provided
        val filtered = if (ctx != null) items.filter { it.assetType.equals(ctx.name.lowercase(), true) } else items
        if (filtered.isNotEmpty()) filtered else emptyList()
    } catch (e: Exception) {
        android.util.Log.e("AscNews", "Exception in fetchNewsFromGemini: ${e.message}", e)
        emptyList()
    }
}

// --- Fallback/mock data ---
fun getMockAscNews(): List<NewsItem> = listOf(
    NewsItem("Federal Reserve maintains interest rates amid inflation concerns", "Reuters", "2h ago", "forex"),
    NewsItem("Bitcoin surges 5% as institutional investors return to market", "Bloomberg", "3h ago", "crypto"),
    NewsItem("Tech stocks rally following better-than-expected earnings reports", "CNBC", "4h ago", "stocks"),
    NewsItem("S&P 500 reaches all-time high on strong Q4 performance", "MarketWatch", "1h ago", "indices"),
    NewsItem("Euro strengthens against dollar on ECB hawkish signals", "FT", "2h ago", "forex"),
    NewsItem("Ethereum breaks $3000 as layer-2 scaling solutions gain adoption", "CoinDesk", "1h ago", "crypto"),
    NewsItem("Apple announces record revenue driven by iPhone 16 sales", "WSJ", "3h ago", "stocks"),
    NewsItem("FTSE 100 climbs as energy stocks benefit from oil price surge", "LSE News", "5h ago", "indices"),
    NewsItem("Gold prices stabilize above $2100 per ounce on geopolitical tensions", "Reuters", "2h ago", "forex"),
    NewsItem("US unemployment rate drops to 3.5% signaling robust economic health", "Bureau of Labor", "4h ago", "indices")
)

// Context-aware news filtering helper
fun getNewsForContext(ctx: com.asc.markets.state.AssetContext): List<NewsItem> {
    val all = getMockAscNews()
    return when (ctx) {
        com.asc.markets.state.AssetContext.ALL -> all
        com.asc.markets.state.AssetContext.FOREX -> all.filter { it.assetType.lowercase() == "forex" }
        com.asc.markets.state.AssetContext.CRYPTO -> all.filter { it.assetType.lowercase() == "crypto" }
        com.asc.markets.state.AssetContext.COMMODITIES -> all.filter { it.assetType.lowercase() == "commodities" || it.assetType.lowercase() == "energy" }
        com.asc.markets.state.AssetContext.INDICES -> all.filter { it.assetType.lowercase() == "indices" }
        com.asc.markets.state.AssetContext.STOCKS -> all.filter { it.assetType.lowercase() == "stocks" }
        com.asc.markets.state.AssetContext.FUTURES -> all.filter { it.assetType.lowercase() == "indices" || it.assetType.lowercase() == "futures" }
        com.asc.markets.state.AssetContext.BONDS -> all.filter { it.assetType.lowercase() == "bonds" }
    }
}

// --- Context-aware mock macro events (should be sourced from data layer in future) ---
fun getMacroEventsForContext(ctx: com.asc.markets.state.AssetContext): List<Pair<String, String>> {
    return when (ctx) {
        com.asc.markets.state.AssetContext.FOREX -> listOf(
            "08:41" to "EUR/USD: Momentum shift, watch 1.0900 level",
            "08:38" to "DXY strength persists; USD correlated across majors",
            "08:35" to "Liquidity thinning ahead of London open"
        )
        com.asc.markets.state.AssetContext.CRYPTO -> listOf(
            "08:41" to "On-chain flows show exchange outflows for BTC",
            "08:38" to "Derivatives open interest drops 4%",
            "08:35" to "Large whale moved funds to cold storage"
        )
        com.asc.markets.state.AssetContext.COMMODITIES -> listOf(
            "08:41" to "API inventory surprise in crude: -3.2M barrels",
            "08:38" to "Futures curve steepens for Brent",
            "08:35" to "Metals ETF inflows indicate demand pickup"
        )
        com.asc.markets.state.AssetContext.INDICES -> listOf(
            "08:41" to "Advance/Decline ratio improves; breadth turning positive",
            "08:38" to "Small caps outperform large caps intraday",
            "08:35" to "Sector rotation into cyclicals observed"
        )
        com.asc.markets.state.AssetContext.STOCKS -> listOf(
            "08:41" to "Earnings season driving stock-specific moves",
            "08:38" to "Sector rotation shows strength in technology and energy",
            "08:35" to "Large-cap leadership persists intraday"
        )
        com.asc.markets.state.AssetContext.FUTURES -> listOf(
            "08:41" to "Futures liquidity widens ahead of settlement",
            "08:38" to "Commodity futures curve showing contango in WTI",
            "08:35" to "Index futures lead pre-market price discovery"
        )
        com.asc.markets.state.AssetContext.BONDS -> listOf(
            "08:41" to "10y yield edges up 4bps; curve flattening",
            "08:38" to "Inflation breakevens tick higher",
            "08:35" to "Duration-sensitive funds increase hedges"
        )
        com.asc.markets.state.AssetContext.ALL -> listOf(
            "08:41" to "Cross-asset volatility rising; monitor correlations",
            "08:38" to "Macro headlines impacting FX and equities",
            "08:35" to "Liquidity snapshot: mixed across venues"
        )
    }
}

// Centralized providers for explore/grid items so UI sections reuse the same data source
fun provideForexExplore(): List<ForexPair> = FOREX_PAIRS

fun provideCryptoExplore(): List<ForexPair> = listOf(
    ForexPair("BTC/USD", "Bitcoin", 76762.0, 1200.0, 1.59, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ETH/USD", "Ethereum", 3000.0, 150.0, 5.26, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("SOL/USD", "Solana", 120.0, 8.0, 7.14, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("BNB/USD", "BNB", 420.0, -5.0, -1.17, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("ADA/USD", "Cardano", 0.45, 0.02, 4.65, category = com.asc.markets.data.MarketCategory.CRYPTO),
    ForexPair("XRP/USD", "XRP", 0.62, -0.01, -1.59, category = com.asc.markets.data.MarketCategory.CRYPTO)
)

fun provideCommoditiesExplore(): List<ForexPair> = listOf(
    ForexPair("XAU/USD", "Gold", 2087.5, 38.0, 1.85, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("WTI", "Crude WTI", 76.45, -1.02, -1.32, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("NG", "Natural Gas", 2.856, 0.09, 3.21, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("XAG/USD", "Silver", 25.3, 0.4, 1.61, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("COPPER", "Copper", 4.32, 0.05, 1.17, category = com.asc.markets.data.MarketCategory.COMMODITIES),
    ForexPair("PLAT", "Platinum", 980.0, -10.0, -1.01, category = com.asc.markets.data.MarketCategory.COMMODITIES)
)

fun provideIndicesExplore(): List<ForexPair> = listOf(
    ForexPair("SPX", "S&P 500", 6939.02, 60.0, 0.87, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("NDX", "Nasdaq 100", 25552.39, 358.0, 1.42, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("DAX", "DAX", 24538.81, 229.0, 0.94, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("FTSE", "FTSE 100", 10223.54, 52.0, 0.51, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("NI225", "Japan 225", 53322.8, 1100.0, 2.10, category = com.asc.markets.data.MarketCategory.INDICES),
    ForexPair("SSE", "SSE Comp", 4117.95, -40.0, -0.96, category = com.asc.markets.data.MarketCategory.INDICES)
)

fun provideBondsExplore(): List<ForexPair> = listOf(
    ForexPair("US10Y", "US 10Y", 102.5, 0.2, 0.20, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("US2Y", "US 2Y", 98.3, -0.1, -0.10, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("UK10Y", "UK 10Y", 101.2, 0.3, 0.30, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("GER10Y", "Germany 10Y", 89.7, 0.4, 0.45, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("JPN10Y", "Japan 10Y", 26.5, 0.0, 0.00, category = com.asc.markets.data.MarketCategory.STOCK),
    ForexPair("AUS10Y", "Australia 10Y", 105.4, 0.5, 0.48, category = com.asc.markets.data.MarketCategory.STOCK)
)

fun getExploreItemsForContext(ctx: com.asc.markets.state.AssetContext): List<ForexPair> = when (ctx) {
    com.asc.markets.state.AssetContext.FOREX -> provideForexExplore()
    com.asc.markets.state.AssetContext.CRYPTO -> provideCryptoExplore()
    com.asc.markets.state.AssetContext.COMMODITIES -> provideCommoditiesExplore()
    com.asc.markets.state.AssetContext.INDICES -> provideIndicesExplore()
    com.asc.markets.state.AssetContext.STOCKS -> provideStocksExplore()
    com.asc.markets.state.AssetContext.FUTURES -> provideFuturesExplore()
    com.asc.markets.state.AssetContext.BONDS -> provideBondsExplore()
    com.asc.markets.state.AssetContext.ALL -> provideForexExplore()
}

fun provideStocksExplore(): List<ForexPair> = FOREX_PAIRS.filter { it.category == com.asc.markets.data.MarketCategory.STOCK }

fun provideFuturesExplore(): List<ForexPair> = provideIndicesExplore()

// --- UI ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketOverviewTab(selectedPair: ForexPair, onAssetClick: (ForexPair) -> Unit = {}) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val categories = listOf("All", "Commodities", "Stocks", "Crypto", "Futures", "Forex", "Bonds")

    // Remembered state for the main scrollable list so we can programmatically
    // scroll to top when the user selects an asset category.
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Observe current AssetContext at composable scope for use in several sections
    val assetCtxForNews by AssetContextStore.context.collectAsState()
    val selectedPairCtx = mapCategoryToAssetContext(selectedPair.category.name)
    val newsItemsForCtx = remember(assetCtxForNews) { getNewsForContext(assetCtxForNews) }


    @Composable
    fun UniversalOverviewBox(ctx: AssetContext, pair: ForexPair) {
        var showPostMarket by remember { mutableStateOf(false) }

        // Simple heuristics to derive overview fields from the selected pair and asset context.
        val absChange = kotlin.math.abs(pair.changePercent)
        val marketState = when {
            absChange > 1.0 -> "Trending"
            absChange > 0.5 -> "Transitional"
            else -> "Ranging"
        }

        val volatilityState = when {
            absChange > 2.0 -> "Erratic"
            absChange > 0.8 -> "Expanding"
            else -> "Compressed"
        }

        val liquidityCondition = when (ctx) {
            AssetContext.FOREX -> "Normal"
            AssetContext.CRYPTO -> "Moderate"
            AssetContext.COMMODITIES -> "Normal"
            AssetContext.INDICES -> "Heavy"
            AssetContext.STOCKS -> "Moderate"
            AssetContext.FUTURES -> "Variable"
            AssetContext.BONDS -> "Normal"
            AssetContext.ALL -> "Normal"
        }

        val sessionSensitivity = when (ctx) {
            AssetContext.FOREX -> "Asia / London / NY (overlap: London/NY)"
            AssetContext.COMMODITIES -> "London / NY"
            AssetContext.CRYPTO -> "24/7"
            AssetContext.INDICES -> "NY / London"
            AssetContext.STOCKS -> "Market Hours (Local)"
            AssetContext.FUTURES -> "Settlement-sensitive"
            AssetContext.BONDS -> "NY"
            AssetContext.ALL -> "Cross-asset"
        }

        val biasLabel = when {
            pair.changePercent > 0.25 -> "Bullish (Conditional)"
            pair.changePercent < -0.25 -> "Bearish (Conditional)"
            else -> "Neutral"
        }

        val confidence = kotlin.math.min(90, (50 + (absChange * 20)).toInt())

        // Key levels: use simple price-based approximations (placeholders until feeds wired)
        val level1 = String.format("%.4f", pair.price * (1 + 0.01))
        val level2 = String.format("%.4f", pair.price * (1 - 0.01))
        val level3 = String.format("%.4f", pair.price)

        val invalidation = String.format("%.4f", pair.price * (1 - 0.005))

        val macroAlignment = when {
            pair.changePercent > 0 -> "Risk-On"
            pair.changePercent < 0 -> "Risk-Off"
            else -> "Mixed"
        }

        val playbook = when {
            volatilityState == "Compressed" && liquidityCondition == "Normal" -> "Breakout"
            volatilityState == "Erratic" || liquidityCondition == "Variable" -> "Wait"
            else -> "Mean Reversion"
        }

        InfoBox(minHeight = 220.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Overview — What matters before the market moves", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)

                // 1. Market Regime Snapshot
                Column {
                    Text("1. Market Regime Snapshot", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Asset Class:", color = SlateText, fontSize = 11.sp)
                            Text(ctx.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Market State:", color = SlateText, fontSize = 11.sp)
                            Text(marketState, color = Color.White, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Volatility:", color = SlateText, fontSize = 11.sp)
                            Text(volatilityState, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Liquidity:", color = SlateText, fontSize = 11.sp)
                            Text(liquidityCondition, color = Color.White, fontSize = 12.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Session Sensitivity:", color = SlateText, fontSize = 11.sp)
                            Text(sessionSensitivity, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // 2. Dominant Bias Engine
                Column {
                    Text("2. Dominant Bias Engine", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bias:", color = SlateText, fontSize = 11.sp)
                            Text(biasLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Confidence:", color = SlateText, fontSize = 11.sp)
                            Text("$confidence%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reason:", color = SlateText, fontSize = 11.sp)
                    Text("${pair.symbol} ${if (biasLabel.contains("Bull")) "momentum and liquidity bias" else if (biasLabel.contains("Bear")) "selling pressure and flow" else "mixed signals"}", color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                // 3. Key Levels That Matter
                Column {
                    Text("3. Key Levels That Matter", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Level 1: $level1 — liquidity high / prior session high", color = Color.White, fontSize = 11.sp)
                    Text("• Level 2: $level2 — prior session low", color = Color.White, fontSize = 11.sp)
                    Text("• Level 3: $level3 — weekly equilibrium", color = Color.White, fontSize = 11.sp)
                }

                // 4. Invalidation & Risk Flags
                Column {
                    Text("4. Invalidation & Risk Flags", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Bias invalidated if: price re-enters value below $invalidation", color = Color.White, fontSize = 11.sp)
                    Text("• High-risk windows: check upcoming macro and expiries", color = Color.White, fontSize = 11.sp)
                    Text("• Liquidity traps: equal highs/lows near key levels", color = Color.White, fontSize = 11.sp)
                }

                // 5. Macro Pressure Index
                Column {
                    Text("5. Macro Pressure Index", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Macro Alignment: $macroAlignment", color = Color.White, fontSize = 11.sp)
                    Text("• Key Drivers: Rates / Inflation / Growth", color = SlateText, fontSize = 11.sp)
                    Text("• Event Sensitivity: Medium", color = Color.White, fontSize = 11.sp)
                }

                // 6. Playbook Readiness
                Column {
                    Text("6. Playbook Readiness", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Setup Quality: ${if (confidence > 60) "Clean" else "Developing"}", color = Color.White, fontSize = 11.sp)
                    Text("• Liquidity Availability: $liquidityCondition", color = Color.White, fontSize = 11.sp)
                    Text("• Best Strategy Type: $playbook", color = Color.White, fontSize = 11.sp)
                }

                // 7. Post-Market Micro Review (collapsed by default)
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("7. Post-Market Micro Review", color = SlateText, fontSize = 12.sp)
                        Text(if (showPostMarket) "Hide" else "Show", color = IndigoAccent, modifier = Modifier.clickable { showPostMarket = !showPostMarket })
                    }
                    if (showPostMarket) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Did bias play out? —", color = Color.White, fontSize = 11.sp)
                        Text("Liquidity behavior: —", color = Color.White, fontSize = 11.sp)
                        Text("Learning: —", color = Color.White, fontSize = 11.sp)
                    }
                }

                // Asset-specific note (small)
                Spacer(modifier = Modifier.height(6.dp))
                when (ctx) {
                    AssetContext.FOREX -> Text("Focus: Central bank divergence / Dominant Currency / Session in control", color = SlateText, fontSize = 11.sp)
                    AssetContext.COMMODITIES -> Text("Focus: Supply/demand / Inventory pressure / Physical vs paper", color = SlateText, fontSize = 11.sp)
                    AssetContext.CRYPTO -> Text("Focus: Liquidity cycles / BTC dominance / Funding pressure", color = SlateText, fontSize = 11.sp)
                    AssetContext.INDICES -> Text("Focus: Risk appetite / Sector leadership / Yield pressure", color = SlateText, fontSize = 11.sp)
                    AssetContext.STOCKS -> Text("Focus: Earnings / Sector movers / Company-specific risk", color = SlateText, fontSize = 11.sp)
                    AssetContext.FUTURES -> Text("Focus: Contract rolls / Liquidity across maturities / Curve structure", color = SlateText, fontSize = 11.sp)
                    AssetContext.BONDS -> Text("Focus: Yield curve / Inflation expectations / Duration risk", color = SlateText, fontSize = 11.sp)
                    AssetContext.ALL -> Text("Focus: Cross-asset pressure and correlation regime", color = SlateText, fontSize = 11.sp)
                }
            }
        }
    }

    @Composable
    fun PerAssetOverviewBox(ctx: AssetContext, pair: ForexPair) {
        var showPostAnalysis by remember { mutableStateOf(false) }

        // Lightweight summary derived from macro events (filtered) and pair heuristics
        val events = remember(ctx) { getMacroEventsForContext(ctx) }
        val topEvent = events.firstOrNull()?.second ?: "No major macro events"

        val absChange = kotlin.math.abs(pair.changePercent)
        val structure = when {
            absChange > 1.0 -> "Trend (HTF)"
            absChange > 0.5 -> "Trend (LTF)"
            absChange > 0.2 -> "Range"
            else -> "Compression"
        }

        val volatility = when {
            absChange > 2.0 -> "High"
            absChange > 0.8 -> "Elevated"
            else -> "Low"
        }

        val liquidity = when (ctx) {
            AssetContext.CRYPTO -> "Thin at times"
            AssetContext.FOREX -> "Deep"
            AssetContext.COMMODITIES -> "Variable"
            AssetContext.INDICES -> "Deep"
            AssetContext.STOCKS -> "Market-hours concentrated"
            AssetContext.FUTURES -> "Roll/expiry sensitive"
            AssetContext.BONDS -> "Venue dependent"
            AssetContext.ALL -> "Cross-asset"
        }

        val bias = when {
            pair.changePercent > 0.25 -> "Institutional Bullish"
            pair.changePercent < -0.25 -> "Institutional Bearish"
            else -> "No Clear Institutional Bias"
        }

        val primaryPlay = when {
            structure.contains("Trend") -> "Trend Following"
            structure == "Range" -> "Range Play"
            else -> "Wait for Breakout"
        }
        val altPlay = if (primaryPlay == "Trend Following") "Pullback entries" else "Fade extremes"
        val failurePlan = "If structure invalidates, step aside and reassess"

        val confidence = kotlin.math.min(90, (50 + (absChange * 20)).toInt())
        val execPermission = when {
            confidence > 65 && volatility != "High" -> "Go"
            confidence > 50 -> "Caution"
            else -> "Hold"
        }

        InfoBox(minHeight = 160.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Overview — ${ctx.name} summary", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)

                // Macro Alignment Summary (tiny mirror)
                Text("Macro Alignment:", color = SlateText, fontSize = 11.sp)
                Text(topEvent, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                // Institutional Bias
                Text("Institutional Bias:", color = SlateText, fontSize = 11.sp)
                Text(bias, color = Color.White, fontSize = 12.sp)

                // Structure / Liquidity / Volatility row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Structure:", color = SlateText, fontSize = 10.sp)
                        Text(structure, color = Color.White, fontSize = 12.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Liquidity:", color = SlateText, fontSize = 10.sp)
                        Text(liquidity, color = Color.White, fontSize = 12.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Volatility:", color = SlateText, fontSize = 10.sp)
                        Text(volatility, color = Color.White, fontSize = 12.sp)
                    }
                }

                // Playbook
                Text("Scenario Playbook:", color = SlateText, fontSize = 11.sp)
                Text("Primary: $primaryPlay | Alternative: $altPlay | Failure: $failurePlan", color = Color.White, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                // Execution permission
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Execution Permission:", color = SlateText, fontSize = 10.sp)
                        Text(execPermission, color = Color.White, fontSize = 12.sp)
                    }
                    Text("Confidence: $confidence%", color = SlateText, fontSize = 10.sp)
                }

                // Post-Event Analysis (collapsed by default)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Post-Event Analysis", color = SlateText, fontSize = 11.sp)
                    Text(if (showPostAnalysis) "Hide" else "Show", color = IndigoAccent, modifier = Modifier.clickable { showPostAnalysis = !showPostAnalysis })
                }
                if (showPostAnalysis) {
                    Text("Outcome: —", color = Color.White, fontSize = 11.sp)
                    Text("Execution notes: —", color = Color.White, fontSize = 11.sp)
                }

                // AI reasoning scope note (ensures prompts include active asset)
                Text("AI scope: ${AssetContextStore.aiPromptPrefix()}", color = SlateText, fontSize = 9.sp)
            }
        }
    }

    // Derive the selected chip from the global AssetContext so this composable
    // won't overwrite the app-wide context on initial composition.
    val selectedCatName = remember(assetCtxForNews) {
        when (assetCtxForNews) {
            AssetContext.FOREX -> "Forex"
            AssetContext.CRYPTO -> "Crypto"
            AssetContext.COMMODITIES -> "Commodities"
            AssetContext.INDICES -> "Indices"
            AssetContext.STOCKS -> "Stocks"
            AssetContext.FUTURES -> "Futures"
            AssetContext.BONDS -> "Bonds"
            AssetContext.ALL -> "Markets"
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(DeepBlack),
        contentPadding = PaddingValues(bottom = 158.dp)
    ) {
        // Top header removed per request

        // Category chips as sticky subheader (won't scroll with page)
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DeepBlack
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = mapCategoryToAssetContext(cat) == assetCtxForNews
                            Surface(
                                color = if (isSelected) Color(0xFF2d2d2d) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                                modifier = Modifier.clickable {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                    AssetContextStore.setAndInvalidate(mapCategoryToAssetContext(cat))
                                    // After changing context, jump the list back to the top
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
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

                    // badge removed (frontend)
                }
            }
        }

        // When `All` is selected, show the Universal Overview box below the chips
        if (assetCtxForNews == AssetContext.ALL) {
            item {
                UniversalOverviewBox(assetCtxForNews, selectedPair)
            }
            // Add padding between the first two boxes for the ALL view
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // Market overview summary (driven by AssetContext -> MarketOverviewConfig)
        item {
            InfoBox(minHeight = 180.dp) {
                val assetCtx by AssetContextStore.context.collectAsState()
                val cfg = MarketOverviewConfigs.configs[assetCtx] ?: MarketOverviewConfigs.default

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Market Overview", color = IndigoAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)

                    // Primary sentiment label
                    Text(cfg.primarySentimentLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    // Metrics rendered generically from the config (loop-based)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        cfg.metrics.forEach { metric ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(metric.label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                // Placeholder value rendering; actual sources should map to metric.valueSource in data layer
                                Text(text = "—", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    // Explanatory bullets sourced from config
                    Column(modifier = Modifier.fillMaxWidth()) {
                        cfg.explanatoryText.forEach { line ->
                            Text("• ${line}", color = Color(0xFF94a3b8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Per-asset concise Overview (adds the new fields for each asset view, does not replace existing info)
        if (assetCtxForNews != AssetContext.ALL) {
            // Add padding between the Market Overview and the Per-Asset Overview
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                PerAssetOverviewBox(assetCtxForNews, selectedPair)
            }
        }

        // INSTITUTIONAL NODES SECTION
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // INSTITUTIONAL NODES SECTION
        
        // 1. ADAPTIVE SESSION PROGRESS ENGINE (Clock Node)
        item {
            InfoBox(minHeight = 140.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🕐", fontSize = 18.sp)
                            Text("Session Progress", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("LONDON OPEN", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    
                    // Circular gauge representation
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Time (UTC)", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("08:42:15", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Text("64%", color = IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Text("London 08:00–16:00 UTC | New York 13:00–21:00 UTC | Tokyo 00:00–08:00 UTC", color = SlateText, fontSize = 10.sp, lineHeight = 12.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 2. OPERATIONAL VITALS GRID (KPI)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Operational Vitals", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Spread
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Spread", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("0.18 Pips", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("Below avg", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Volatility
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Volatility", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("12.3 Pips/Hr", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("20-day avg", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Safety Gate
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🛡️", fontSize = 14.sp)
                                Text("Safety Gate", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("ARMED", color = EmeraldSuccess, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("+47 min safe", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Node Latency
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Node Latency", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("8.2 ms", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("Optimal ping", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 3. NET EXPOSURE HUB
        item {
            InfoBox(minHeight = 120.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💰", fontSize = 18.sp)
                            Text("Net Exposure", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(color = IndigoAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text("BALANCED", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    
                    Text("Net USD Direction", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(0.4f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.6f).background(RoseError, RoundedCornerShape(4.dp)))
                        }
                        Box(modifier = Modifier.weight(0.6f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                        }
                    }
                    Text("Short 2.3M USD | Long 2.1M USD | Net: 0.2M Short", color = Color.White, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 4. MACRO REGIME CLASSIFIER
        item {
            InfoBox(minHeight = 120.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌍", fontSize = 18.sp)
                        Text("Macro Regime", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("VIX Index:", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Text("16.8", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("RISK_ON", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("DXY 24h:", color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            Text("+0.34%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("USD_DOMINANT", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Text("Environment: Equities & Growth Assets Favored | USD Strength Persists", color = Color.White, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 5. MACRO INTELLIGENCE STREAM (Event Log) - context-aware
        item {
            InfoBox(minHeight = 150.dp) {
                val assetCtx by AssetContextStore.context.collectAsState()
                val events = getMacroEventsForContext(assetCtx)

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📊", fontSize = 18.sp)
                        Text("Macro Intelligence Stream", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }

                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        events.forEach { (time, text) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(time, color = SlateText, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 6. STRUCTURAL CONTEXT CARD (Fractal Logic)
        item {
            InfoBox(minHeight = 110.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 18.sp)
                        Text("Structural Context", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    
                    Text("Last 4 Swings: Higher High & Higher Low sequence detected (Bullish Structure). Price closed above swing high on M15 → BOS (Break of Structure) confirmed.", color = Color.White, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 7. INSTITUTIONAL LEVELS GRID (S/R, Daily High/Low)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Institutional Levels", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Support", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0845", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("SL: 1.0835", color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Resistance", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0915", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("TP: 1.0925", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daily High", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0928", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("+0.8% from open", color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daily Low", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("1.0812", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text("-0.65% from open", color = RoseError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Explore grid title (context-aware)
        item {
                val exploreTitle = when (assetCtxForNews) {
                    AssetContext.FOREX -> "Forex Majors"
                    AssetContext.CRYPTO -> "Crypto Gainers"
                    AssetContext.COMMODITIES -> "Commodities"
                    AssetContext.INDICES -> "Major Indices"
                    AssetContext.STOCKS -> "Stocks"
                    AssetContext.FUTURES -> "Futures"
                    AssetContext.BONDS -> "Bond Market"
                    AssetContext.ALL -> "Markets"
                }
            Text(
                exploreTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Use centralized provider for explore items
        val exploreItems = getExploreItemsForContext(assetCtxForNews).take(6)
        val gridItems = exploreItems.chunked(2)
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

        // 5. NEWS FLOW ITEMS (context-aware)
        items(newsItemsForCtx) { item ->
            val meta = "${item.source} • ${item.timestamp}"
            val iconChar = item.assetType.take(1).uppercase()
            NewsFlowRow(meta, item.headline, iconChar)
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
                            Icon(Icons.Outlined.Newspaper, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
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
                        Icon(Icons.Filled.Share, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
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
                            Text("↑", color = IndigoAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
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

        // ASC NEWS SECTION
        item {
            AscNewsSection()
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 6. MARKET DEPTH LADDER
        if (assetCtxForNews == AssetContext.ALL || assetCtxForNews == selectedPairCtx) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 16.dp)) {
                    MarketDepthLadder(selectedPair = selectedPair)
                }
            }
        }

        // NEW SECTIONS: show cards only for matching AssetContext (or ALL)
        if (assetCtxForNews == AssetContext.CRYPTO || assetCtxForNews == AssetContext.ALL) {
            item { CryptoCardsSection(onAssetClick) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        if (assetCtxForNews == AssetContext.INDICES || assetCtxForNews == AssetContext.ALL) {
            item { StockCardsSection(onAssetClick) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { IndicesCardsSection(onAssetClick) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { MajorIndicesSection(onAssetClick) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
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
    val ctx by com.asc.markets.state.AssetContextStore.context.collectAsState()
    if (ctx != com.asc.markets.state.AssetContext.ALL && ctx != com.asc.markets.state.AssetContext.CRYPTO) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        val header = when (ctx) {
            com.asc.markets.state.AssetContext.CRYPTO -> "Crypto Gainers"
            com.asc.markets.state.AssetContext.ALL -> "Crypto Gainers"
            else -> "Crypto"
        }
        Text(header, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val cryptoList = provideCryptoExplore()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(0.dp)) {
            items(cryptoList) { fp ->
                CryptoCard(fp.name, fp.symbol, String.format(Locale.US, "%.2f", fp.price)) {
                    onAssetClick(fp)
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
    val ctx by com.asc.markets.state.AssetContextStore.context.collectAsState()
    if (ctx != com.asc.markets.state.AssetContext.ALL && ctx != com.asc.markets.state.AssetContext.STOCKS) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        val header = when (ctx) {
            com.asc.markets.state.AssetContext.STOCKS -> "Stocks Gainers"
            com.asc.markets.state.AssetContext.ALL -> "Stocks Gainers"
            else -> "Stocks"
        }
        Text(header, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val stocks = listOf(
            Pair("United Foods", "217.44 INR"),
            Pair("Creative Dynamics", "615.65 INR"),
            Pair("Gas Holdings", "1.8308 USD")
        )
        
        val stockList = provideStocksExplore()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(0.dp)) {
            items(stockList) { fp ->
                StockCard(fp.name, String.format(Locale.US, "%.2f", fp.price)) {
                    onAssetClick(fp)
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
    val ctx by com.asc.markets.state.AssetContextStore.context.collectAsState()
    if (ctx != com.asc.markets.state.AssetContext.ALL && ctx != com.asc.markets.state.AssetContext.INDICES) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        val header = when (ctx) {
            com.asc.markets.state.AssetContext.INDICES -> "Major Indices"
            com.asc.markets.state.AssetContext.ALL -> "Major Indices"
            else -> "Indices"
        }
        Text(header, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Spacer(modifier = Modifier.height(12.dp))
        
        val indices = listOf(
            Triple("S&P 500", "SPX", "6,939.02"),
            Triple("Nasdaq 100", "NDX", "25,552.39"),
            Triple("DAX", "DAX", "24,538.81")
        )
        
        val indicesList = provideIndicesExplore()
        indicesList.forEach { fp ->
            IndexCard(fp.name, fp.symbol, String.format(Locale.US, "%.2f", fp.price)) {
                onAssetClick(fp)
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
            
                        if (symbol == "SPX" || name.contains("S&P")) {
                                AndroidView(
                                        factory = { ctx ->
                                                WebView(ctx).apply {
                                                        settings.javaScriptEnabled = true
                                                        settings.domStorageEnabled = true
                                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                        val html = """
                                                                <!doctype html>
                                                                <html>
                                                                <head>
                                                                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
                                                                    <script type=\"text/javascript\" src=\"https://s3.tradingview.com/tv.js\"></script>
                                                                </head>
                                                                <body style=\"margin:0;background-color:#0b0b0b;\">
                                                                    <div id=\"tv_chart_container\"></div>
                                                                    <script type=\"text/javascript\">(function() {
                                                                        new TradingView.widget({
                                                                            \"autosize\": true,
                                                                            \"symbol\": \"INDEX:SPX\",
                                                                            \"interval\": \"D\",
                                                                            \"timezone\": \"Etc/UTC\",
                                                                            \"theme\": \"Dark\",
                                                                            \"style\": \"1\",
                                                                            \"locale\": \"en\",
                                                                            \"container_id\": \"tv_chart_container\",
                                                                            \"hide_legend\": true,
                                                                            \"allow_symbol_change\": false,
                                                                            \"hide_side_toolbar\": true,
                                                                            \"withdateranges\": false,
                                                                            \"details\": false,
                                                                            \"toolbar_bg\": \"#0b0b0b\",
                                                                            \"width\": \"100%\",
                                                                            \"height\": 120
                                                                        });
                                                                    })();</script>
                                                                </body>
                                                                </html>
                                                        """.trimIndent()
                                                        loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                                                }
                                        },
                                        modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(8.dp))
                                )
                        } else {
                                Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(8.dp))) {
                                        MiniChart(
                                                values = List(25) { 6900.0 + kotlin.random.Random.nextDouble(-50.0, 50.0) },
                                                modifier = Modifier.fillMaxSize()
                                        )
                                }
                        }
        }
    }
}

@Composable
fun MajorIndicesSection(onAssetClick: (ForexPair) -> Unit) {
    val ctx by com.asc.markets.state.AssetContextStore.context.collectAsState()
    if (ctx != com.asc.markets.state.AssetContext.ALL && ctx != com.asc.markets.state.AssetContext.INDICES) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("All Major Markets", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        }
        
        // majorMarkets inline list removed; use centralized provider instead
        
        val majorMarketsList = provideIndicesExplore()
        majorMarketsList.forEach { fp ->
            val market = MajorIndex(fp.name, fp.symbol, String.format(Locale.US, "%.2f", fp.price), "", if (fp.changePercent >= 0) EmeraldSuccess else RoseError)
            MajorIndexRow(market) {
                onAssetClick(fp)
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

@Composable
fun AscNewsSection() {
    val assetCtx by AssetContextStore.context.collectAsState()
    var newsList by remember { mutableStateOf<List<NewsItem>>(getNewsForContext(assetCtx)) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(assetCtx) {
        try {
            android.util.Log.d("AscNews", "Starting to fetch news from OpenAI for $assetCtx...")
            // Invalidate local caches for this view when context changes
            com.asc.markets.data.AssetDataCache.invalidateAll()

            val fetchedNews = fetchNewsFromGemini(assetCtx)
            android.util.Log.d("AscNews", "Fetched ${fetchedNews.size} news items")
            if (fetchedNews.isNotEmpty()) {
                newsList = if (assetCtx == com.asc.markets.state.AssetContext.ALL) fetchedNews else fetchedNews.filter { it.assetType.equals(assetCtx.name.lowercase(), true) }
                android.util.Log.d("AscNews", "Updated newsList with ${newsList.size} items")
                // store into cache
                com.asc.markets.data.AssetDataCache.putNews(assetCtx, newsList)
            } else {
                // fallback to mock filtered by context
                newsList = getNewsForContext(assetCtx)
            }
        } catch (e: Exception) {
            android.util.Log.e("AscNews", "Error fetching news: ${e.message}", e)
            newsList = getNewsForContext(assetCtx)
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Header
        Text("ASC News >", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }

    // Display news items
    newsList.take(10).forEach { newsItem ->
        AscNewsItemRow(newsItem)
    }
}

@Composable
fun AscNewsItemRow(newsItem: NewsItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Asset type icon
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                Text(
                    when (newsItem.assetType) {
                        "forex" -> "🔄"
                        "crypto" -> "₿"
                        "stocks" -> "📈"
                        "indices" -> "📊"
                        else -> "📰"
                    },
                    fontSize = 11.sp
                )
            }
            Text("${newsItem.source} • ${newsItem.timestamp}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = newsItem.headline,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            fontFamily = InterFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
