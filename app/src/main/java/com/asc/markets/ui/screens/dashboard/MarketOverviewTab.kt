package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ForexPair
import com.asc.markets.data.MarketDataStore
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
// FOREX_PAIRS moved to centralized providers to avoid direct references across UI
import com.asc.markets.data.MarketCategory
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.OrderFlowMiniChart
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.screens.dashboard.OrderBookSplit
import com.asc.markets.ui.components.dashboard.MarketDepthLadder
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
import java.time.OffsetDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import com.asc.markets.BuildConfig
import com.trading.app.data.BinanceService
import com.trading.app.models.OHLCData

// --- Data model ---
data class NewsItem(
    val headline: String,
    val source: String,
    val timestamp: String,
    val assetType: String,
    val assetSymbol: String = "",
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
            val assetSymbol = obj.optString("assetSymbol", "").trim()
            val headline = obj.optString("headline", obj.optString("title", "")).trim()
            if (headline.isEmpty()) continue
            val source = obj.optString("source", "Market")
            val timestamp = normalizeNewsTimestamp(
                explicitTimestamp = obj.optString("timestamp", "").trim(),
                datetimeUtc = obj.optString("datetime_utc", "").trim()
            )
            val assetType = obj.optString("assetType", "").trim()
                .ifBlank { inferAssetTypeFromSymbol(assetSymbol, headline) }
            val imageUrl = obj.optString("imageUrl", "")
            items.add(
                NewsItem(
                    headline = headline,
                    source = source,
                    timestamp = timestamp,
                    assetType = assetType,
                    assetSymbol = assetSymbol.ifBlank { inferNewsAssetSymbol(assetType, headline) },
                    imageUrl = imageUrl
                )
            )
        }

        android.util.Log.d("AscNews", "Parsed ${items.size} news items from OpenAI")
        // Defensive filter: ensure returned items are within requested context when ctx provided
        val filtered = if (ctx != null) items.filter { matchesAssetContext(it, ctx) } else items
        if (filtered.isNotEmpty()) filtered else emptyList()
    } catch (e: Exception) {
        android.util.Log.e("AscNews", "Exception in fetchNewsFromGemini: ${e.message}", e)
        emptyList()
    }
}

// --- Fallback/mock data ---
fun getMockAscNews(): List<NewsItem> = listOf(
    NewsItem("Dollar elevated near multi-year highs against the yen as rate decisions loom", "Reuters", "8h ago", "forex", "USD/JPY"),
    NewsItem("Bitcoin climbs toward $80,000 as spot demand slowly rebuilds", "Bloomberg", "8h ago", "crypto", "BTC/USD"),
    NewsItem("Nasdaq futures erase early losses as traders look past geopolitical jitters", "CNBC", "9h ago", "indices", "IXIC"),
    NewsItem("Gold exits a rising channel after a sharp pullback dents momentum", "MarketWatch", "3d ago", "commodities", "XAU/USD"),
    NewsItem("S&P 500 futures hold steady as ceasefire hopes calm headline risk", "WSJ", "3d ago", "futures", "SPX"),
    NewsItem("Intel rallies after earnings as margin recovery beats expectations", "Barron's", "3d ago", "stocks", "INTC"),
    NewsItem("Euro firms versus the dollar after ECB officials reinforce a restrictive bias", "FT", "5h ago", "forex", "EUR/USD"),
    NewsItem("Ethereum outperforms majors as ETF allocation chatter resurfaces", "CoinDesk", "2h ago", "crypto", "ETH/USD"),
    NewsItem("WTI crude stabilizes as traders weigh inventory draws against demand concerns", "Reuters", "6h ago", "commodities", "WTI"),
    NewsItem("Treasury yields edge higher after a soft auction leaves duration under pressure", "Bureau of Labor", "4h ago", "bonds", "US10Y")
)

// Context-aware news filtering helper
fun getNewsForContext(ctx: com.asc.markets.state.AssetContext): List<NewsItem> {
    val all = getMockAscNews()
    return all.filter { matchesAssetContext(it, ctx) }
}

private fun matchesAssetContext(item: NewsItem, ctx: AssetContext): Boolean {
    val type = item.assetType.lowercase(Locale.US)
    return when (ctx) {
        AssetContext.ALL -> true
        AssetContext.FOREX -> type == "forex"
        AssetContext.CRYPTO -> type == "crypto"
        AssetContext.COMMODITIES -> type == "commodities" || type == "energy"
        AssetContext.INDICES -> type == "indices"
        AssetContext.STOCKS -> type == "stocks"
        AssetContext.FUTURES -> type == "futures" || type == "indices"
        AssetContext.BONDS -> type == "bonds"
    }
}

private fun normalizeNewsTimestamp(explicitTimestamp: String, datetimeUtc: String): String {
    if (explicitTimestamp.isNotBlank()) return explicitTimestamp
    if (datetimeUtc.isBlank()) return "Just now"

    return try {
        val eventTime = OffsetDateTime.parse(datetimeUtc).toInstant().toEpochMilli()
        val diffMillis = eventTime - System.currentTimeMillis()
        val absMinutes = kotlin.math.abs(diffMillis) / 60_000
        when {
            absMinutes < 1 -> "Just now"
            diffMillis > 0 && absMinutes < 60 -> "In ${absMinutes}m"
            diffMillis > 0 && absMinutes < 1_440 -> "In ${absMinutes / 60}h"
            diffMillis > 0 -> "Upcoming"
            absMinutes < 60 -> "${absMinutes}m ago"
            absMinutes < 1_440 -> "${absMinutes / 60}h ago"
            absMinutes < 10_080 -> "${absMinutes / 1_440}d ago"
            else -> "Earlier"
        }
    } catch (_: Exception) {
        "Just now"
    }
}

private fun inferAssetTypeFromSymbol(assetSymbol: String, headline: String): String {
    val normalizedSymbol = assetSymbol.uppercase(Locale.US)
    val normalizedHeadline = headline.lowercase(Locale.US)
    return when {
        normalizedSymbol.contains("BTC") || normalizedSymbol.contains("ETH") || normalizedHeadline.contains("bitcoin") || normalizedHeadline.contains("ethereum") -> "crypto"
        normalizedSymbol.contains("XAU") || normalizedSymbol == "WTI" || normalizedSymbol == "BRENT" ||
            normalizedHeadline.contains("gold") || normalizedHeadline.contains("oil") || normalizedHeadline.contains("crude") -> "commodities"
        normalizedSymbol == "US10Y" || normalizedSymbol == "UST10Y" ||
            normalizedHeadline.contains("yield") || normalizedHeadline.contains("treasury") || normalizedHeadline.contains("bond") -> "bonds"
        normalizedSymbol == "SPX" || normalizedSymbol == "IXIC" || normalizedSymbol == "NDX" || normalizedSymbol == "DJI" || normalizedSymbol == "FTSE" -> "indices"
        normalizedSymbol.endsWith("1!") -> "futures"
        normalizedSymbol.contains("/") -> "forex"
        normalizedSymbol.length in 1..5 -> "stocks"
        else -> "forex"
    }
}

private fun inferNewsAssetSymbol(assetType: String, headline: String): String {
    val normalizedHeadline = headline.lowercase(Locale.US)
    return when (assetType.lowercase(Locale.US)) {
        "forex" -> when {
            normalizedHeadline.contains("yen") -> "USD/JPY"
            normalizedHeadline.contains("euro") || normalizedHeadline.contains("ecb") -> "EUR/USD"
            normalizedHeadline.contains("pound") || normalizedHeadline.contains("boe") -> "GBP/USD"
            else -> "EUR/USD"
        }
        "crypto" -> when {
            normalizedHeadline.contains("ethereum") -> "ETH/USD"
            else -> "BTC/USD"
        }
        "commodities" -> when {
            normalizedHeadline.contains("oil") || normalizedHeadline.contains("crude") -> "WTI"
            else -> "XAU/USD"
        }
        "indices" -> when {
            normalizedHeadline.contains("nasdaq") -> "IXIC"
            normalizedHeadline.contains("ftse") -> "FTSE"
            else -> "SPX"
        }
        "futures" -> when {
            normalizedHeadline.contains("nasdaq") -> "NQ1!"
            else -> "SPX"
        }
        "stocks" -> when {
            normalizedHeadline.contains("intel") -> "INTC"
            normalizedHeadline.contains("apple") -> "AAPL"
            normalizedHeadline.contains("nvidia") -> "NVDA"
            else -> "AAPL"
        }
        "bonds" -> "US10Y"
        else -> ""
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketOverviewTab(selectedPair: ForexPair, onAssetClick: (ForexPair) -> Unit = {}, viewModel: ForexViewModel = viewModel()) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val categories = listOf("All", "Commodities", "Stocks", "Crypto", "Futures", "Forex", "Bonds")

    // Observe core data providers for real-time AI updates
    val sessionData = rememberSessionData()
    val vitalsData = rememberTechnicalVitals()
    val aiDeployments by viewModel.aiDeployments.collectAsState()
    val macroStreamEvents by viewModel.macroStreamEvents.collectAsState()

    // Observe current AssetContext at composable scope for use in several sections
    val assetCtxForNews by AssetContextStore.context.collectAsState()
    val cryptoLiveList by viewModel.cryptoPairs.collectAsState()
    val allMarketPairs by MarketDataStore.allPairs.collectAsState()
    val priceHistory by MarketDataStore.priceHistory.collectAsState()
    val selectedPairCtx = mapCategoryToAssetContext(selectedPair.category.name)
    val newsItemsForCtx = remember(assetCtxForNews) { getNewsForContext(assetCtxForNews) }

    // Watch scroll and animate header collapse smoothly
    val collapseRange = 150f  // pixels to scroll before header fully collapses
    val collapseProgress by remember {
        derivedStateOf {
            // Calculate absolute scroll position (works across item boundaries)
            val absoluteScroll = (listState.firstVisibleItemIndex * 100f) + listState.firstVisibleItemScrollOffset
            (absoluteScroll / collapseRange).coerceIn(0f, 1f)
        }
    }
    
    LaunchedEffect(collapseProgress) {
        viewModel.setGlobalHeaderCollapse(collapseProgress)
    }


    @Composable
    fun UniversalOverviewBox(ctx: AssetContext, pair: ForexPair) {
        var showPostMarket by remember { mutableStateOf(false) }

        // Find AI deployment for the selected pair to drive the bias engine
        val specificAiDecision = aiDeployments?.final_decision?.find { it.asset_1 == pair.symbol || it.asset_1?.replace("/", "") == pair.symbol.replace("/", "") }

        // Use provider helpers so these fields can be wired to real data later.
        val marketState = getRegimeForContext(ctx, pair)
        val volatilityState = specificAiDecision?.portfolio_deployment_bucket ?: getVolatilityForPair(pair)
        val liquidityCondition = getLiquidityConditionForContext(ctx)
        val sessionSensitivity = getSessionSensitivityForContext(ctx)
        
        val biasLabel = specificAiDecision?.journal_direction ?: getBiasLabelForPair(pair)
        val confidence = (specificAiDecision?.journal_score ?: getConfidenceForPair(pair).toDouble()).toInt()
        val reason = specificAiDecision?.portfolio_decision_reason ?: "${pair.symbol} ${if (biasLabel.contains("Bull", true)) "momentum and liquidity bias" else if (biasLabel.contains("Bear", true)) "selling pressure and flow" else "mixed signals"}"

        val (level1, level2, level3) = getKeyLevelsForPair(pair)
        val invalidation = getInvalidationLevelForPair(pair)
        val macroAlignment = getMacroAlignmentForContext(ctx, pair)
        val playbook = getPlaybookReadinessForContext(ctx, pair)
        val marketRegimeScore = when {
            marketState.contains("risk", true) && marketState.contains("off", true) -> 0.25f
            marketState.contains("bull", true) || marketState.contains("trend", true) || marketState.contains("risk_on", true) -> 0.75f
            else -> 0.5f
        }
        val volatilityScore = when {
            volatilityState.contains("high", true) -> 0.88f
            volatilityState.contains("elevated", true) -> 0.68f
            volatilityState.contains("low", true) -> 0.28f
            else -> 0.5f
        }
        val liquidityScore = when {
            liquidityCondition.contains("deep", true) || liquidityCondition.contains("high", true) -> 0.82f
            liquidityCondition.contains("thin", true) -> 0.32f
            liquidityCondition.contains("variable", true) -> 0.52f
            else -> 0.58f
        }
        val sessionScore = when {
            sessionSensitivity.contains("high", true) -> 0.78f
            sessionSensitivity.contains("low", true) -> 0.28f
            else -> 0.55f
        }
        val biasScore = when {
            biasLabel.contains("bull", true) || biasLabel.contains("buy", true) || biasLabel.contains("long", true) -> 0.84f
            biasLabel.contains("bear", true) || biasLabel.contains("sell", true) || biasLabel.contains("short", true) -> 0.16f
            else -> 0.5f
        }
        val confidenceScore = confidence.coerceIn(0, 100) / 100f
        val invalidationRiskScore = (1f - confidenceScore).coerceIn(0.15f, 0.85f)
        val macroPressureScore = when {
            macroAlignment.contains("bull", true) || macroAlignment.contains("aligned", true) -> 0.72f
            macroAlignment.contains("bear", true) || macroAlignment.contains("conflict", true) -> 0.36f
            else -> 0.5f
        }
        val playbookScore = ((confidenceScore * 0.7f) + (liquidityScore * 0.3f)).coerceIn(0f, 1f)

        InfoBox(minHeight = 220.dp, containerColor = PureBlack) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Overview — AI Decision Intelligence", color = IndigoAccent, fontSize = DashboardFontSizes.sectionHeaderMedium, fontWeight = FontWeight.Black)

                // 1. Market Regime Snapshot
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Market Regime Snapshot", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("Asset", ctx.name, IndigoAccent, Modifier.weight(1f))
                        DashboardMeter("Regime", marketState, marketRegimeScore, IndigoAccent, "Risk-Off", "Risk-On", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMeter("Volatility", volatilityState, volatilityScore, if (volatilityScore > 0.7f) RoseError else IndigoAccent, "Low", "High", Modifier.weight(1f))
                        DashboardMeter("Liquidity", liquidityCondition, liquidityScore, EmeraldSuccess, "Thin", "Deep", Modifier.weight(1f))
                    }
                    DashboardMeter("Session Sensitivity", sessionSensitivity, sessionScore, IndigoAccent, "Low", "High")
                }

                // 2. Dominant Bias Engine
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Dominant Bias Engine (AI Driven)", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    DashboardMeter("Bias", biasLabel.uppercase(), biasScore, if (biasScore > 0.55f) EmeraldSuccess else if (biasScore < 0.45f) RoseError else Color.White, "Sell", "Buy")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMeter("Confidence", "$confidence%", confidenceScore, IndigoAccent, "0", "100", Modifier.weight(1f))
                        DashboardMeter("Invalidation Risk", invalidation, invalidationRiskScore, if (invalidationRiskScore > 0.55f) RoseError else EmeraldSuccess, "Low", "High", Modifier.weight(1f))
                    }
                    Text(reason, color = SlateText, fontSize = DashboardFontSizes.bodyTiny, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                // 3. Key Levels That Matter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Key Levels That Matter", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("L1", level1, RoseError, Modifier.weight(1f))
                        MetricPill("L2", level2, EmeraldSuccess, Modifier.weight(1f))
                        MetricPill("EQ", level3, IndigoAccent, Modifier.weight(1f))
                    }
                }

                // Tactical Depth (Split View)
                Column {
                    Text("Market Depth (Tactical Structure)", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OrderBookSplit(
                        selectedPair = pair,
                        modifier = Modifier.fillMaxWidth(),
                        showExplanation = true,
                        explanationMode = DepthExplanationMode.STRUCTURE
                    )
                }

                // 4. Invalidation & Risk Flags
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. Invalidation & Risk Flags", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    DashboardMeter("Risk Flags", if (invalidationRiskScore > 0.55f) "Elevated" else "Controlled", invalidationRiskScore, if (invalidationRiskScore > 0.55f) RoseError else EmeraldSuccess, "Low", "High")
                }

                // 5. Macro Pressure Index
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("5. Macro Pressure Index", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    DashboardMeter("Macro Alignment", macroAlignment, macroPressureScore, IndigoAccent, "Conflict", "Aligned")
                    DashboardMeter("Event Sensitivity", "Medium", 0.55f, IndigoAccent, "Low", "High")
                }

                // 6. Playbook Readiness
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("6. Playbook Readiness", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    DashboardMeter("Setup Quality", if (confidence > 60) "Clean" else "Developing", confidenceScore, EmeraldSuccess, "Poor", "Clean")
                    DashboardMeter("Playbook", playbook, playbookScore, IndigoAccent, "Wait", "Ready")
                }

                // 7. Institutional Timing & Dispatch (Deterministic)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("7. Institutional Timing & Dispatch", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Bold)
                    val timingWindow = specificAiDecision?.journal_label?.contains("Timing", true) ?: false
                    val timingLabel = if (timingWindow) "DISPATCH IMMINENT" else "ACCUMULATION PHASE"
                    val timingScore = if (timingWindow) 0.92f else 0.35f
                    
                    DashboardMeter("Timing Window", timingLabel, timingScore, if (timingScore > 0.8f) RoseError else IndigoAccent, "Accumulating", "Dispatch")
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMeter("Prop Guard", "SAFE", 0.85f, EmeraldSuccess, "Veto", "Clear", Modifier.weight(1f))
                        DashboardMeter("Liquidity Gap", "Targeting L2", 0.75f, IndigoAccent, "Filled", "Open", Modifier.weight(1f))
                    }
                }

                // 8. Post-Market Micro Review (collapsed by default)
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("8. Post-Move Audit", color = SlateText, fontSize = DashboardFontSizes.sectionHeaderSmall)
                        Text(if (showPostMarket) "Hide" else "Show", color = IndigoAccent, modifier = Modifier.clickable { showPostMarket = !showPostMarket })
                    }
                    if (showPostMarket) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Did bias play out? —", color = Color.White, fontSize = DashboardFontSizes.bodyMedium)
                        Text("Liquidity behavior: —", color = Color.White, fontSize = DashboardFontSizes.bodyMedium)
                        Text("Learning: —", color = Color.White, fontSize = DashboardFontSizes.bodyMedium)
                    }
                }

                // Asset-specific note (small)
                Spacer(modifier = Modifier.height(6.dp))
                when (ctx) {
                    AssetContext.FOREX -> Text("Focus: Central bank divergence / Dominant Currency / Session in control", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.COMMODITIES -> Text("Focus: Supply/demand / Inventory pressure / Physical vs paper", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.CRYPTO -> Text("Focus: Liquidity cycles / BTC dominance / Funding pressure", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.INDICES -> Text("Focus: Risk appetite / Sector leadership / Yield pressure", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.STOCKS -> Text("Focus: Earnings / Sector movers / Company-specific risk", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.FUTURES -> Text("Focus: Contract rolls / Liquidity across maturities / Curve structure", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.BONDS -> Text("Focus: Yield curve / Inflation expectations / Duration risk", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                    AssetContext.ALL -> Text("Focus: Cross-asset pressure and correlation regime", color = SlateText, fontSize = DashboardFontSizes.bodyMedium)
                }
            }
        }
    }

    @Composable
    fun PerAssetOverviewBox(ctx: AssetContext, pair: ForexPair) {
        var showPostAnalysis by remember { mutableStateOf(false) }

        // Find AI deployment for the selected pair
        val specificAiDecision = aiDeployments?.final_decision?.find { it.asset_1 == pair.symbol || it.asset_1?.replace("/", "") == pair.symbol.replace("/", "") }

        // Lightweight summary derived from live macro events and AI decisions
        val topEvent = macroStreamEvents.firstOrNull()?.title ?: "No major macro events"

        val absChange = kotlin.math.abs(pair.changePercent)
        val structure = when {
            absChange > 1.0 -> "Trend (HTF)"
            absChange > 0.5 -> "Trend (LTF)"
            absChange > 0.2 -> "Range"
            else -> "Compression"
        }

        val volatility = specificAiDecision?.portfolio_deployment_bucket ?: when {
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

        val bias = specificAiDecision?.journal_direction ?: when {
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

        val confidence = (specificAiDecision?.journal_score ?: (50 + (absChange * 20)).toDouble()).toInt().coerceIn(0, 99)
        val execPermission = when {
            confidence > 75 -> "GO (AI Approved)"
            confidence > 60 && volatility != "High" -> "CAUTION"
            else -> "HOLD"
        }
        val structureScore = when {
            structure.contains("Trend", true) -> 0.78f
            structure == "Range" -> 0.52f
            else -> 0.28f
        }
        val liquidityScore = when {
            liquidity.contains("Deep", true) -> 0.82f
            liquidity.contains("Thin", true) -> 0.32f
            liquidity.contains("Variable", true) -> 0.52f
            else -> 0.58f
        }
        val volatilityScore = when {
            volatility.contains("High", true) -> 0.88f
            volatility.contains("Elevated", true) -> 0.68f
            volatility.contains("Low", true) -> 0.28f
            else -> 0.5f
        }
        val biasScore = when {
            bias.contains("Bull", true) || bias.contains("Long", true) -> 0.84f
            bias.contains("Bear", true) || bias.contains("Short", true) -> 0.16f
            else -> 0.5f
        }
        val confidenceScore = confidence / 100f
        val executionScore = when {
            execPermission.contains("GO", true) -> 0.88f
            execPermission.contains("CAUTION", true) -> 0.58f
            else -> 0.24f
        }

        InfoBox(minHeight = 160.dp, containerColor = PureBlack) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Overview — AI Analysis Summary", color = IndigoAccent, fontSize = DashboardFontSizes.sectionHeaderMedium, fontWeight = FontWeight.Black)

                Text("Live Macro Signal:", color = SlateText, fontSize = DashboardFontSizes.labelLarge)
                Text(topEvent, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)

                DashboardMeter("Institutional Bias", bias.uppercase(), biasScore, if (biasScore > 0.55f) EmeraldSuccess else if (biasScore < 0.45f) RoseError else Color.White, "Bearish", "Bullish")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMeter("Structure", structure, structureScore, IndigoAccent, "Weak", "Strong", Modifier.weight(1f))
                    DashboardMeter("Liquidity", liquidity, liquidityScore, EmeraldSuccess, "Thin", "Deep", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMeter("Volatility", volatility, volatilityScore, if (volatilityScore > 0.7f) RoseError else IndigoAccent, "Low", "High", Modifier.weight(1f))
                    DashboardMeter("Confidence", "$confidence%", confidenceScore, IndigoAccent, "0", "100", Modifier.weight(1f))
                }
                DashboardMeter("Execution Permission", execPermission, executionScore, if (executionScore > 0.75f) EmeraldSuccess else if (executionScore < 0.4f) RoseError else IndigoAccent, "Hold", "Go")
                
                // Timing & Liquidity Gap (Deterministic)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val timingLabel = if (confidence > 80) "DISPATCH" else "ACCUMULATING"
                    val timingScore = if (confidence > 80) 0.88f else 0.42f
                    DashboardMeter("Timing", timingLabel, timingScore, if (timingScore > 0.7f) RoseError else IndigoAccent, "Wait", "Now", Modifier.weight(1f))
                    DashboardMeter("Gap Target", "L2 / SMC", 0.65f, EmeraldSuccess, "Filled", "Open", Modifier.weight(1f))
                }

                Text("Playbook: $primaryPlay / $altPlay", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, maxLines = 1, overflow = TextOverflow.Ellipsis)

                // Post-Move Audit (collapsed by default)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Post-Move Audit", color = SlateText, fontSize = DashboardFontSizes.labelLarge)
                    Text(if (showPostAnalysis) "Hide" else "Show", color = IndigoAccent, modifier = Modifier.clickable { showPostAnalysis = !showPostAnalysis })
                }
                if (showPostAnalysis) {
                    Text("Outcome: —", color = Color.White, fontSize = DashboardFontSizes.bodyMedium)
                    Text("Execution notes: —", color = Color.White, fontSize = DashboardFontSizes.bodyMedium)
                }

                // AI reasoning scope note (ensures prompts include active asset)
                Text("AI scope: ${AssetContextStore.aiPromptPrefix()}", color = SlateText, fontSize = DashboardFontSizes.aiScopeNote)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PureBlack)) {
        // Institutional-style Tab Bar matching DashboardTopNavbar
        Surface(
            color = Color(0xFF141414), // Dark charcoal matching the image
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { cat ->
                        val isSelected = mapCategoryToAssetContext(cat) == assetCtxForNews
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .clickable {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                                    AssetContextStore.setAndInvalidate(mapCategoryToAssetContext(cat))
                                    coroutineScope.launch { listState.scrollToItem(0) }
                                }
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else Color(0xFF8E8E8E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            // Bold white indicator
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color.White else Color.Transparent)
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 158.dp)
        ) {
            // When `All` is selected, show the Universal Overview box below the chips
            if (assetCtxForNews == AssetContext.ALL) {
                item {
                    AllTabLiveMarketBoard(
                        selectedPair = selectedPair,
                        allPairs = allMarketPairs,
                        priceHistory = priceHistory,
                        onPairFocused = { pair ->
                            viewModel.selectPairBySymbolNoNavigate(pair.symbol)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    UniversalOverviewBox(assetCtxForNews, selectedPair)
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Market overview summary
            item {
                InfoBox(minHeight = 180.dp, containerColor = PureBlack) {
                    val cfg = MarketOverviewConfigs.configs[assetCtxForNews] ?: MarketOverviewConfigs.default

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Market Overview", color = IndigoAccent, fontSize = DashboardFontSizes.sectionHeaderMedium, fontWeight = FontWeight.Black)
                        Text(cfg.primarySentimentLabel, color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold)
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cfg.metrics.take(6).forEachIndexed { index, metric ->
                                val score = listOf(0.68f, 0.56f, 0.42f, 0.74f, 0.51f, 0.63f).getOrElse(index) { 0.5f }
                                val value = when {
                                    metric.label.contains("USD", true) -> "Firm"
                                    metric.label.contains("Vol", true) -> "Moderate"
                                    metric.label.contains("Liquidity", true) -> "Deep"
                                    metric.label.contains("Risk", true) -> "Constructive"
                                    else -> "Neutral"
                                }
                                DashboardMeter(metric.label, value, score, if (score > 0.65f) EmeraldSuccess else IndigoAccent, "Low", "High")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            if (assetCtxForNews != AssetContext.ALL) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    PerAssetOverviewBox(assetCtxForNews, selectedPair)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 1. LIQUIDITY POOL VISUALIZATION (Deterministic Gap 1)
            item {
                LiquidityPoolVisual(selectedPair)
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 2. SETUP CONFLUENCE MATRIX (Deterministic Gap 2)
            item {
                ConfluenceMatrix(selectedPair)
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 3. SESSION KILLZONES (Deterministic Gap 3 - Replaces Session Progress)
            item {
                SessionKillzones()
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 2. OPERATIONAL VITALS GRID
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("Operational Vitals", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp, containerColor = PureBlack) {
                            DashboardMeter("Spread", sessionData.avgSpread, 0.28f, EmeraldSuccess, "Tight", "Wide", Modifier.padding(12.dp))
                        }
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp, containerColor = PureBlack) {
                            val volScore = if (sessionData.volatility.contains("High", true)) 0.86f else if (sessionData.volatility.contains("Low", true)) 0.28f else 0.55f
                            DashboardMeter("Volatility", sessionData.volatility, volScore, if (volScore > 0.7f) RoseError else IndigoAccent, "Low", "Extreme", Modifier.padding(12.dp))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp, containerColor = PureBlack) {
                            DashboardMeter("Safety Gate", sessionData.safetyGateStatus, if (sessionData.safetyGateArmed) 0.9f else 0.18f, if(sessionData.safetyGateArmed) EmeraldSuccess else RoseError, "Locked", "Armed", Modifier.padding(12.dp))
                        }
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 100.dp, containerColor = PureBlack) {
                            val latencyScore = (1f - (vitalsData.latencyMs.toFloat() / 200f)).coerceIn(0f, 1f)
                            DashboardMeter("Node Latency", "${String.format("%.1f", vitalsData.latencyMs)} ms", latencyScore, if (latencyScore > 0.65f) EmeraldSuccess else RoseError, "Poor", "Optimal", Modifier.padding(12.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 3. NET EXPOSURE HUB
            item {
                InfoBox(minHeight = 120.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💰", fontSize = DashboardFontSizes.emojiIcon)
                                Text("Net Exposure", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
                            }
                            Surface(color = IndigoAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("BALANCED", color = IndigoAccent, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Text("Net USD Direction", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(0.4f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.6f).background(RoseError, RoundedCornerShape(4.dp)))
                            }
                            Box(modifier = Modifier.weight(0.6f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f).background(EmeraldSuccess, RoundedCornerShape(4.dp)))
                            }
                        }
                        Text("Short 2.3M USD | Long 2.1M USD | Net: 0.2M Short", color = Color.White, fontSize = DashboardFontSizes.labelSmall, fontFamily = InterFontFamily)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 4. MACRO REGIME CLASSIFIER
            item {
                InfoBox(minHeight = 120.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🌍", fontSize = DashboardFontSizes.emojiIcon)
                            Text("Macro Regime", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardMeter("VIX Index", "16.8 · RISK_ON", 0.32f, EmeraldSuccess, "Calm", "Fear")
                            DashboardMeter("DXY 24h", "+0.34% · USD_DOMINANT", 0.68f, IndigoAccent, "Weak USD", "Strong USD")
                            DashboardMeter("Risk Appetite", "Growth Favored", 0.72f, EmeraldSuccess, "Defensive", "Aggressive")
                        }
                        Text("Environment: Growth assets favored while USD strength persists.", color = SlateText, fontSize = DashboardFontSizes.bodyTiny, lineHeight = 13.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 5. RAW DATA NEWS
            item {
                InfoBox(minHeight = 150.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📊", fontSize = DashboardFontSizes.emojiIcon)
                            Text(
                                "Raw Feed >",
                                color = Color.White,
                                fontSize = DashboardFontSizes.valueLarge,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (newsItemsForCtx.isEmpty()) {
                                Text(
                                    "No asset-tagged headlines available for this lens.",
                                    color = SlateText,
                                    fontSize = DashboardFontSizes.labelMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            } else {
                                // Deterministic filter: only show news with high-impact keywords or specific asset tags
                                val filteredNews = newsItemsForCtx.filter { item ->
                                    val h = item.headline.lowercase()
                                    h.contains("breakout") || h.contains("rally") || h.contains("crash") || 
                                    h.contains("decision") || h.contains("rate") || h.contains("fed") || 
                                    h.contains("ecb") || h.contains("inflation") || h.contains("cpi") ||
                                    h.contains("momentum") || h.contains("liquidity")
                                }
                                
                                (if (filteredNews.isNotEmpty()) filteredNews else newsItemsForCtx).take(4).forEach { item ->
                                    RawFeedNewsRow(
                                        newsItem = item,
                                        fallbackSymbol = if (assetCtxForNews == AssetContext.ALL) selectedPair.symbol else inferNewsAssetSymbol(item.assetType, item.headline)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 6. STRUCTURAL CONTEXT CARD
            item {
                InfoBox(minHeight = 110.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔍", fontSize = DashboardFontSizes.emojiIcon)
                            Text("Structural Context", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black)
                        }
                        DashboardMeter("Structure", "Bullish HH / HL", 0.78f, EmeraldSuccess, "Bearish", "Bullish")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricPill("BOS", "Confirmed", EmeraldSuccess, Modifier.weight(1f))
                            MetricPill("Sweep", "62%", IndigoAccent, Modifier.weight(1f))
                            MetricPill("Quality", "Strong", EmeraldSuccess, Modifier.weight(1f))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 7. INSTITUTIONAL LEVELS GRID
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("Institutional Levels", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp, containerColor = PureBlack) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Support", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                                Text("1.0845", color = Color.White, fontSize = DashboardFontSizes.valueSmall, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("SL: 1.0835", color = RoseError, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp, containerColor = PureBlack) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Resistance", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                                Text("1.0915", color = Color.White, fontSize = DashboardFontSizes.valueSmall, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("TP: 1.0925", color = EmeraldSuccess, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp, containerColor = PureBlack) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Daily High", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                                Text("1.0928", color = Color.White, fontSize = DashboardFontSizes.valueSmall, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("+0.8% from open", color = EmeraldSuccess, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        InfoBox(modifier = Modifier.weight(1f), minHeight = 90.dp, containerColor = PureBlack) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Daily Low", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                                Text("1.0812", color = Color.White, fontSize = DashboardFontSizes.valueSmall, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("-0.65% from open", color = RoseError, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                AssetGallerySection(
                    ctx = assetCtxForNews,
                    allPairs = allMarketPairs,
                    liveCryptoPairs = cryptoLiveList,
                    onAssetClick = { pair ->
                        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                        onAssetClick(pair)
                    }
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("News Flow >", color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
            }

            items(newsItemsForCtx) { item ->
                val meta = "${item.source} • ${item.timestamp}"
                val iconChar = item.assetType.take(1).uppercase()
                NewsFlowRow(meta, item.headline, iconChar)
            }

            item {
                InfoBox(minHeight = 160.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Newspaper, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                                Text("Market Flow", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black)
                            }
                            Surface(color = EmeraldSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("ACTIVE", color = EmeraldSuccess, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("High-Impact Headlines", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold)
                            Text("4 critical news events in last 2 hours", color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Reuters", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.65f).background(RoseError, RoundedCornerShape(2.dp)))
                                }
                                Text("65%", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Bloomberg", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                                Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(IndigoAccent, RoundedCornerShape(2.dp)))
                                }
                                Text("35%", color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                InfoBox(minHeight = 180.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Text("LP Routing Flow", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("JPM-NODE", color = Color.White, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.78f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                                }
                                Text("0.01MS", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.56f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                                }
                                Text("0.02MS", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CITADEL", color = Color.White, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.45f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                                }
                                Text("0.01MS", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("BARC-L7", color = Color.White, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Black, modifier = Modifier.width(80.dp))
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.32f).background(EmeraldSuccess, RoundedCornerShape(3.dp)))
                                }
                                Text("0.04MS", color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                InfoBox(minHeight = 140.dp, containerColor = PureBlack) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("↑", color = IndigoAccent, fontSize = DashboardFontSizes.emojiIcon, fontWeight = FontWeight.Black)
                                Text("Volatility Pulse", color = Color.White, fontSize = DashboardFontSizes.sectionHeaderSmall, fontWeight = FontWeight.Black)
                            }
                            Surface(color = EmeraldSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("STABLE", color = EmeraldSuccess, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Standard Deviation", color = SlateText, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))) {
                                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.683f).background(Color(0xFFFFA500), RoundedCornerShape(4.dp)))
                                }
                                Text("68.3%", color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
                            }
                        }
                        Text("Market compression detected. Expansion phase expected within the next 45 minutes of NY session.", color = Color.White, fontSize = DashboardFontSizes.bodyTiny, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                        Text("1.4", color = IndigoAccent, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.End))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { AscNewsSection() }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            if (assetCtxForNews == AssetContext.ALL || assetCtxForNews == selectedPairCtx) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        MarketDepthLadder(
                            symbol = selectedPair.symbol,
                            price = selectedPair.price
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMeter(
    label: String,
    value: String,
    progress: Float,
    accent: Color,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = SlateText, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = Color.White, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(accent, RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .offset(x = ((clamped * 260).dp).coerceAtMost(260.dp))
                    .size(8.dp)
                    .background(Color.White, CircleShape)
                    .align(Alignment.CenterStart)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
            Text(endLabel, color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = SlateText, fontSize = DashboardFontSizes.bodyTiny, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = Color.White, fontSize = DashboardFontSizes.labelSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NewsFlowRow(meta: String, title: String, iconChar: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                Text(iconChar, color = Color.White, fontSize = DashboardFontSizes.labelMedium, fontWeight = FontWeight.Black)
            }
            Text(meta, color = Color.Gray, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, fontFamily = InterFontFamily)
    }
}

@Composable
private fun RawFeedNewsRow(newsItem: NewsItem, fallbackSymbol: String) {
    val assetSymbol = newsItem.assetSymbol
        .ifBlank { fallbackSymbol }
        .ifBlank { inferNewsAssetSymbol(newsItem.assetType, newsItem.headline) }
    val headline = remember(newsItem.headline, assetSymbol) {
        if (assetSymbol.isBlank() || newsItem.headline.startsWith("$assetSymbol:", ignoreCase = true)) {
            newsItem.headline
        } else {
            "$assetSymbol: ${newsItem.headline}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PairFlags(symbol = assetSymbol, size = 18)
            Text(
                text = newsItem.timestamp,
                color = Color(0xFF8B8B8B),
                fontSize = DashboardFontSizes.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = headline,
            color = Color.White,
            fontSize = DashboardFontSizes.valueMediumLarge,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
            fontFamily = InterFontFamily,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private enum class OverviewBoardAssetClass(val label: String, val category: MarketCategory) {
    CRYPTO("Crypto", MarketCategory.CRYPTO),
    INDICES("Indices", MarketCategory.INDICES),
    FUTURES("Futures", MarketCategory.FUTURES),
    BONDS("Bonds", MarketCategory.BONDS),
    FOREX("Forex", MarketCategory.FOREX),
    STOCKS("Stocks", MarketCategory.STOCK),
    COMMODITIES("Commodities", MarketCategory.COMMODITIES)
}

private enum class OverviewBoardRange(
    val label: String,
    val secondsPerBar: Long,
    val candleCount: Int,
    val smoothingWindow: Int,
    val amplitudeMultiplier: Double
) {
    FOUR_HOUR("4H", 300L, 48, 1, 0.72),
    ONE_DAY("1D", 900L, 96, 2, 0.78),
    ONE_MONTH("1M", 14_400L, 120, 3, 0.92),
    THREE_MONTH("3M", 86_400L, 120, 4, 1.05),
    ONE_YEAR("1Y", 604_800L, 110, 5, 1.16),
    FIVE_YEAR("5Y", 2_592_000L, 100, 6, 1.24),
    ALL("All", 5_184_000L, 120, 7, 1.30)
}

@Composable
private fun AllTabLiveMarketBoard(
    selectedPair: ForexPair,
    allPairs: List<ForexPair>,
    priceHistory: Map<String, List<Double>>,
    onPairFocused: (ForexPair) -> Unit
) {
    val availableClasses = remember(allPairs) {
        OverviewBoardAssetClass.values().filter { overviewBoardPairsForClass(it, allPairs).isNotEmpty() }
    }
    if (availableClasses.isEmpty()) return

    var selectedClass by remember(availableClasses) {
        mutableStateOf(defaultOverviewBoardClass(selectedPair, availableClasses))
    }
    var selectedRange by remember { mutableStateOf(OverviewBoardRange.ONE_DAY) }
    var focusedSymbol by remember { mutableStateOf(selectedPair.symbol) }

    val boardPairs = remember(selectedClass, allPairs) {
        overviewBoardPairsForClass(selectedClass, allPairs).take(6)
    }
    if (boardPairs.isEmpty()) return

    LaunchedEffect(selectedPair.symbol, selectedClass) {
        if (selectedPair.category == selectedClass.category) {
            focusedSymbol = selectedPair.symbol
        }
    }

    LaunchedEffect(selectedClass, boardPairs) {
        if (boardPairs.none { overviewBoardSymbolsEqual(it.symbol, focusedSymbol) }) {
            val fallbackPair = boardPairs.firstOrNull() ?: return@LaunchedEffect
            focusedSymbol = fallbackPair.symbol
            onPairFocused(fallbackPair)
        }
    }

    val activePair = remember(boardPairs, focusedSymbol) {
        boardPairs.firstOrNull { overviewBoardSymbolsEqual(it.symbol, focusedSymbol) } ?: boardPairs.first()
    }
    val chartState = rememberOverviewBoardChartState(
        pair = activePair,
        range = selectedRange,
        priceHistory = priceHistory,
        allPairs = allPairs
    )

    InfoBox(
        modifier = Modifier.fillMaxWidth(),
        minHeight = 0.dp,
        containerColor = PureBlack
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableClasses, key = { it.name }) { assetClass ->
                    val selected = assetClass == selectedClass
                    Surface(
                        color = if (selected) Color(0xFF2E2E2E) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { selectedClass = assetClass }
                    ) {
                        Text(
                            text = assetClass.label,
                            color = Color.White.copy(alpha = if (selected) 1f else 0.8f),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(258.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF040404))
            ) {
                GalleryChartGridOverlay(modifier = Modifier.matchParentSize())
                if (chartState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = IndigoAccent,
                                strokeWidth = 2.2.dp
                            )
                            Text(
                                text = "Rolling live data",
                                color = SlateText,
                                fontSize = DashboardFontSizes.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (chartState.candles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No data available",
                            color = SlateText,
                            fontSize = DashboardFontSizes.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    OrderFlowMiniChart(
                        candles = chartState.candles,
                        priceScaleMarginTop = 0.004f,
                        priceScaleMarginBottom = 0.004f,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp, bottom = 1.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OverviewBoardRange.values().forEach { range ->
                    val selected = range == selectedRange
                    Surface(
                        color = if (selected) Color(0xFF2E2E2E) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { selectedRange = range }
                    ) {
                        Text(
                            text = range.label,
                            color = Color.White.copy(alpha = if (selected) 1f else 0.8f),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                boardPairs.forEachIndexed { index, pair ->
                    val selected = overviewBoardSymbolsEqual(pair.symbol, activePair.symbol)
                    OverviewBoardRow(
                        pair = pair,
                        selected = selected,
                        onClick = {
                            focusedSymbol = pair.symbol
                            onPairFocused(pair)
                        }
                    )
                    if (index != boardPairs.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewBoardRow(
    pair: ForexPair,
    selected: Boolean,
    onClick: () -> Unit
) {
    val changeColor = if (pair.changePercent >= 0) EmeraldSuccess else RoseError

    Surface(
        color = if (selected) Color.White.copy(alpha = 0.035f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PairFlags(symbol = pair.symbol, size = 34)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = pair.symbol.replace("/", ""),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        maxLines = 1
                    )
                    Text(
                        text = pair.name,
                        color = SlateText,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatGalleryPrice(pair),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "${formatOverviewBoardChange(pair)}  ${String.format(Locale.US, "%+.2f%%", pair.changePercent)}",
                    color = changeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun GalleryChartGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gridColor = Color.White.copy(alpha = 0.12f)
        val dotPattern = PathEffect.dashPathEffect(floatArrayOf(5f, 8f), 0f)
        val verticalDivisions = 4
        val horizontalDivisions = 4

        repeat(verticalDivisions) { index ->
            val x = size.width * ((index + 1).toFloat() / (verticalDivisions + 1).toFloat())
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dotPattern
            )
        }

        repeat(horizontalDivisions) { index ->
            val y = size.height * ((index + 1).toFloat() / (horizontalDivisions + 1).toFloat())
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dotPattern
            )
        }
    }
}

private data class OverviewBoardChartState(
    val candles: List<OHLCData>,
    val isLoading: Boolean
)

@Composable
private fun rememberOverviewBoardChartState(
    pair: ForexPair,
    range: OverviewBoardRange,
    priceHistory: Map<String, List<Double>>,
    allPairs: List<ForexPair>
): OverviewBoardChartState {
    val baseHistory = remember(priceHistory, allPairs, pair.symbol) {
        resolveOverviewBoardHistory(priceHistory, allPairs, pair.symbol)
    }
    val liveBinanceHistory = rememberOverviewBoardBinanceHistory(pair.symbol, range)
    val derivedCandles = remember(pair.symbol, range, baseHistory, pair.price) {
        buildOverviewBoardCandles(
            symbol = pair.symbol,
            range = range,
            baseHistory = baseHistory,
            lastPrice = pair.price
        )
    }
    val expectsLiveHistory = remember(pair.symbol) { shouldUseOverviewBoardBinanceHistory(pair.symbol) }

    return remember(pair.price, liveBinanceHistory, derivedCandles, expectsLiveHistory) {
        if (expectsLiveHistory && liveBinanceHistory.isEmpty()) {
            OverviewBoardChartState(
                candles = emptyList(),
                isLoading = true
            )
        } else {
            val source = if (expectsLiveHistory) {
                liveBinanceHistory
            } else {
                derivedCandles
            }
            OverviewBoardChartState(
                candles = applyLivePriceToOverviewBoardCandles(source, pair.price.toFloat()),
                isLoading = false
            )
        }
    }
}

@Composable
private fun rememberOverviewBoardBinanceHistory(
    symbol: String,
    range: OverviewBoardRange
): List<OHLCData> {
    val normalizedSymbol = remember(symbol) { symbol.replace("/", "").uppercase(Locale.US) }
    val interval = remember(range) { overviewBoardBinanceInterval(range) }
    val shouldFetch = remember(symbol) { shouldUseOverviewBoardBinanceHistory(symbol) }
    var history by remember(symbol, range) { mutableStateOf<List<OHLCData>>(emptyList()) }

    DisposableEffect(normalizedSymbol, interval, shouldFetch) {
        history = emptyList()
        if (!shouldFetch) {
            return@DisposableEffect onDispose {}
        }

        val service = BinanceService(
            onQuoteUpdate = {},
            onHistoryUpdate = { receivedSymbol, incomingHistory ->
                if (receivedSymbol.equals(normalizedSymbol, ignoreCase = true) && incomingHistory.isNotEmpty()) {
                    history = sanitizeOverviewBoardCandles(incomingHistory)
                }
            }
        )
        service.fetchHistory(normalizedSymbol, interval, null)

        onDispose {
            service.disconnect()
        }
    }

    return history
}

private fun overviewBoardPairsForClass(
    assetClass: OverviewBoardAssetClass,
    allPairs: List<ForexPair>
): List<ForexPair> {
    val scopedPairs = allPairs
        .filter { it.category == assetClass.category }
        .distinctBy { it.symbol }

    return scopedPairs
}

private fun overviewBoardSymbolsEqual(left: String, right: String): Boolean {
    return normalizeOverviewBoardSymbol(left) == normalizeOverviewBoardSymbol(right)
}

private fun normalizeOverviewBoardSymbol(symbol: String): String {
    return symbol
        .replace("/", "")
        .replace("-", "")
        .replace("_", "")
        .replace("!", "")
        .uppercase(Locale.US)
}

private fun defaultOverviewBoardClass(
    selectedPair: ForexPair,
    availableClasses: List<OverviewBoardAssetClass>
): OverviewBoardAssetClass {
    return availableClasses.firstOrNull { it.category == selectedPair.category }
        ?: availableClasses.firstOrNull { it == OverviewBoardAssetClass.CRYPTO }
        ?: availableClasses.first()
}

private fun resolveOverviewBoardHistory(
    historyMap: Map<String, List<Double>>,
    pairs: List<ForexPair>,
    symbol: String
): List<Double> {
    val canonicalSymbol = pairs.firstOrNull { MarketDataStore.matchesSymbol(it.symbol, symbol) }?.symbol
    if (canonicalSymbol != null) {
        return historyMap[canonicalSymbol] ?: emptyList()
    }
    return historyMap.entries.firstOrNull { MarketDataStore.matchesSymbol(it.key, symbol) }?.value ?: emptyList()
}

private fun buildOverviewBoardCandles(
    symbol: String,
    range: OverviewBoardRange,
    baseHistory: List<Double>,
    lastPrice: Double
): List<OHLCData> {
    val realSeries = baseHistory
        .filter { it.isFinite() && it > 0.0 }
        .takeLast(range.candleCount)
    if (realSeries.size < 2) return emptyList()

    val now = System.currentTimeMillis() / 1000L
    return realSeries.mapIndexed { index, closeValue ->
        val previousClose = realSeries.getOrElse(index - 1) { closeValue }
        OHLCData(
            time = now - ((realSeries.size - index).toLong() * range.secondsPerBar),
            open = previousClose.toFloat(),
            high = max(previousClose, closeValue).toFloat(),
            low = min(previousClose, closeValue).toFloat(),
            close = closeValue.toFloat(),
            volume = 0f
        )
    }
}

private fun resampleOverviewBoardSeries(values: List<Double>, targetSize: Int): List<Double> {
    if (values.isEmpty()) return List(targetSize) { 0.0 }
    if (values.size == targetSize) return values
    if (targetSize <= 1) return listOf(values.last())

    return List(targetSize) { index ->
        val position = index.toDouble() * (values.lastIndex.toDouble() / (targetSize - 1).toDouble())
        val lowerIndex = position.toInt()
        val upperIndex = min(lowerIndex + 1, values.lastIndex)
        val fraction = position - lowerIndex
        val lower = values[lowerIndex]
        val upper = values[upperIndex]
        lower + ((upper - lower) * fraction)
    }
}

private fun smoothOverviewBoardSeries(values: List<Double>, windowSize: Int): List<Double> {
    if (windowSize <= 1) return values
    return values.indices.map { index ->
        val start = max(0, index - windowSize + 1)
        values.subList(start, index + 1).average()
    }
}

private fun estimateOverviewBoardTickSize(price: Double): Double {
    return when {
        price >= 10_000 -> 5.0
        price >= 1_000 -> 1.0
        price >= 100 -> 0.1
        price >= 10 -> 0.01
        price >= 1 -> 0.0001
        else -> 0.00001
    }
}

private fun sanitizeOverviewBoardCandles(candles: List<OHLCData>): List<OHLCData> {
    return candles
        .asSequence()
        .filter { candle ->
            candle.time > 0L &&
                candle.open.isFinite() &&
                candle.high.isFinite() &&
                candle.low.isFinite() &&
                candle.close.isFinite()
        }
        .map { candle ->
            val high = max(candle.high, max(candle.open, candle.close))
            val low = min(candle.low, min(candle.open, candle.close))
            candle.copy(
                high = high,
                low = low,
                volume = candle.volume.coerceAtLeast(0f)
            )
        }
        .sortedBy(OHLCData::time)
        .distinctBy(OHLCData::time)
        .toList()
}

private fun applyLivePriceToOverviewBoardCandles(
    candles: List<OHLCData>,
    livePrice: Float
): List<OHLCData> {
    if (candles.isEmpty() || !livePrice.isFinite() || livePrice <= 0f) {
        return candles
    }

    val updated = candles.toMutableList()
    val last = updated.last()
    val adjustedHigh = max(last.high, livePrice)
    val adjustedLow = min(last.low, livePrice)
    updated[updated.lastIndex] = last.copy(
        high = adjustedHigh,
        low = adjustedLow,
        close = livePrice
    )
    return updated
}

private fun shouldUseOverviewBoardBinanceHistory(symbol: String): Boolean {
    return symbol.replace("/", "").uppercase(Locale.US).endsWith("USDT")
}

private fun overviewBoardBinanceInterval(range: OverviewBoardRange): String {
    return when (range) {
        OverviewBoardRange.FOUR_HOUR -> "5m"
        OverviewBoardRange.ONE_DAY -> "15m"
        OverviewBoardRange.ONE_MONTH -> "4h"
        OverviewBoardRange.THREE_MONTH -> "1d"
        OverviewBoardRange.ONE_YEAR -> "1w"
        OverviewBoardRange.FIVE_YEAR -> "1M"
        OverviewBoardRange.ALL -> "1M"
    }
}

private fun formatOverviewBoardChange(pair: ForexPair): String {
    val decimals = when (pair.category) {
        MarketCategory.FOREX -> if (abs(pair.change) >= 1.0) 2 else 4
        MarketCategory.BONDS -> 3
        else -> 2
    }
    return String.format(Locale.US, "%+,.${decimals}f", pair.change)
}

@Composable
private fun AssetGallerySection(
    ctx: AssetContext,
    allPairs: List<ForexPair>,
    liveCryptoPairs: List<ForexPair>,
    onAssetClick: (ForexPair) -> Unit
) {
    val pairs = remember(ctx, allPairs, liveCryptoPairs) { marketPairsForGallery(ctx, allPairs, liveCryptoPairs) }
    if (pairs.isEmpty()) return
    var galleryView by remember(ctx) { mutableStateOf(AssetGalleryView.ACTIVE) }

    if (ctx == AssetContext.ALL) {
        val visiblePairs = remember(pairs, galleryView) { pairsForGalleryView(pairs, galleryView) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssetGalleryViewSwitcher(
                selectedView = galleryView,
                onViewSelected = { galleryView = it }
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visiblePairs, key = { "${galleryView.name}-${it.symbol}" }) { pair ->
                    ReferenceAssetCard(
                        pair = pair,
                        isHorizontal = true,
                        modifier = Modifier.width(316.dp),
                        onClick = { onAssetClick(pair) }
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pairs.forEach { pair ->
                ReferenceAssetCard(
                    pair = pair,
                    isHorizontal = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAssetClick(pair) }
                )
            }
        }
    }
}

private enum class AssetGalleryView {
    ACTIVE,
    GAINERS,
    LOSERS
}

@Composable
private fun AssetGalleryViewSwitcher(
    selectedView: AssetGalleryView,
    onViewSelected: (AssetGalleryView) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetGalleryView.values().forEach { view ->
            val selected = view == selectedView
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Color(0xFF303030) else Color.Transparent)
                    .clickable { onViewSelected(view) }
            ) {
                Text(
                    text = view.name,
                    color = Color.White.copy(alpha = if (selected) 1f else 0.9f),
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun ReferenceAssetCard(
    pair: ForexPair,
    isHorizontal: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ticker = remember(pair.symbol) { pair.symbol.replace("/", "").replace("!", "") }
    val changeColor = if (pair.changePercent >= 0) Color(0xFF67B7A8) else Color(0xFFD66F79)
    val sparklinePoints = remember(pair.symbol, pair.price, pair.changePercent, isHorizontal) {
        generateSparklinePoints(
            pair = pair,
            points = if (isHorizontal) 144 else 184,
            compression = if (isHorizontal) 1.45f else 2.05f
        )
    }
    val cardShape = RoundedCornerShape(if (isHorizontal) 16.dp else 12.dp)
    val symbolSize = if (isHorizontal) 18.sp else 15.sp
    val priceSize = if (isHorizontal) 22.sp else 16.sp

    Box(
        modifier = modifier
            .height(if (isHorizontal) 214.dp else 146.dp)
            .clip(cardShape)
            .background(Color(0xFF030303))
            .border(1.dp, Color.White.copy(alpha = 0.08f), cardShape)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isHorizontal) 12.dp else 10.dp,
                    vertical = if (isHorizontal) 10.dp else 9.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isHorizontal) 10.dp else 8.dp)
            ) {
                AssetBadge(pair = pair, size = if (isHorizontal) 38.dp else 32.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = ticker,
                        color = Color.White,
                        fontSize = symbolSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "D",
                        color = Color(0xFFC5964A),
                        fontSize = if (isHorizontal) 11.sp else 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .width(if (isHorizontal) 24.dp else 18.dp)
                            .height(if (isHorizontal) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isHorizontal) 10.dp else 6.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(if (isHorizontal) 6.dp else 4.dp)
            ) {
                Text(
                    text = formatGalleryPrice(pair),
                    color = Color.White,
                    fontSize = priceSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = galleryPriceUnit(pair),
                    color = SlateText,
                    fontSize = if (isHorizontal) 13.sp else 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = String.format(Locale.US, "%+.2f%%", pair.changePercent),
                    color = changeColor,
                    fontSize = if (isHorizontal) 16.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AssetSparkline(
                points = sparklinePoints,
                color = changeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isHorizontal) 112.dp else 82.dp)
            )
        }
    }
}

@Composable
private fun AssetBadge(pair: ForexPair, size: androidx.compose.ui.unit.Dp) {
    val label = remember(pair.symbol) { assetBadgeLabel(pair) }
    val colors = remember(pair.symbol, pair.category) { assetBadgeColors(pair) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(brush = Brush.linearGradient(colors), shape = CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = (size.value / 2.5f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AssetSparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val lineWidth = 2.dp.toPx()
        val glowWidth = 5.dp.toPx()
        val drawableHeight = size.height * 0.92f
        val topPadding = size.height * 0.02f
        val stepX = size.width / (points.lastIndex.coerceAtLeast(1))
        val isRedSeries = color.red > color.green
        val fillTop = if (isRedSeries) color.copy(alpha = 0.38f) else color.copy(alpha = 0.28f)
        val fillMid = if (isRedSeries) color.copy(alpha = 0.18f) else color.copy(alpha = 0.11f)

        fun pointAt(index: Int): Offset {
            val normalized = points[index].coerceIn(0.03f, 0.97f)
            return Offset(
                x = index * stepX,
                y = topPadding + ((1f - normalized) * drawableHeight)
            )
        }

        val linePath = Path()
        val fillPath = Path()
        val firstPoint = pointAt(0)
        linePath.moveTo(firstPoint.x, firstPoint.y)
        fillPath.moveTo(firstPoint.x, size.height)
        fillPath.lineTo(firstPoint.x, firstPoint.y)

        for (index in 1 until points.size) {
            val previous = pointAt(index - 1)
            val point = pointAt(index)
            val controlOffset = stepX * 0.45f
            linePath.cubicTo(
                previous.x + controlOffset,
                previous.y,
                point.x - controlOffset,
                point.y,
                point.x,
                point.y
            )
            fillPath.cubicTo(
                previous.x + controlOffset,
                previous.y,
                point.x - controlOffset,
                point.y,
                point.x,
                point.y
            )
        }

        val lastPoint = pointAt(points.lastIndex)
        fillPath.lineTo(lastPoint.x, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillTop, fillMid, Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )
        drawPath(path = linePath, color = color.copy(alpha = if (isRedSeries) 0.18f else 0.14f), style = Stroke(width = glowWidth, cap = StrokeCap.Round))
        drawPath(path = linePath, color = color, style = Stroke(width = lineWidth, cap = StrokeCap.Round))
        drawCircle(color = color, radius = 4.dp.toPx(), center = lastPoint)
    }
}

private fun pairsForGalleryView(
    pairs: List<ForexPair>,
    view: AssetGalleryView
): List<ForexPair> = when (view) {
    AssetGalleryView.ACTIVE -> pairs
    AssetGalleryView.GAINERS -> {
        val gainers = pairs.filter { it.changePercent >= 0.0 }.sortedByDescending { it.changePercent }
        if (gainers.isNotEmpty()) gainers else pairs.sortedByDescending { it.changePercent }
    }
    AssetGalleryView.LOSERS -> {
        val losers = pairs.filter { it.changePercent < 0.0 }.sortedBy { it.changePercent }
        if (losers.isNotEmpty()) losers else pairs.sortedBy { it.changePercent }
    }
}

private fun marketPairsForGallery(
    ctx: AssetContext,
    allPairs: List<ForexPair>,
    liveCryptoPairs: List<ForexPair>
): List<ForexPair> {
    fun stable(pairs: List<ForexPair>): List<ForexPair> =
        pairs.distinctBy { it.symbol }

    fun livePairs(category: MarketCategory, fallback: List<ForexPair>): List<ForexPair> =
        allPairs.filter { it.category == category }.takeIf { it.isNotEmpty() } ?: fallback

    val stockPairs = stable(livePairs(MarketCategory.STOCK, provideStocksExplore()))
    val forexPairs = stable(livePairs(MarketCategory.FOREX, provideForexExplore()))
    val commodityPairs = stable(livePairs(MarketCategory.COMMODITIES, provideCommoditiesExplore()))
    val indexPairs = stable(livePairs(MarketCategory.INDICES, provideIndicesExplore()))
    val bondPairs = stable(livePairs(MarketCategory.BONDS, provideBondsExplore()))
    val futuresPairs = stable(livePairs(MarketCategory.FUTURES, provideFuturesExplore()))
    val cryptoPairs = stable(liveCryptoPairs.takeIf { it.isNotEmpty() } ?: provideCryptoExplore())

    return when (ctx) {
        AssetContext.ALL -> buildList {
            addAll(stockPairs.take(3))
            addAll(cryptoPairs.take(3))
            addAll(forexPairs.take(3))
            addAll(commodityPairs.take(2))
            addAll(indexPairs.take(2))
            addAll(bondPairs.take(2))
            addAll(futuresPairs.take(2))
        }.distinctBy { it.symbol }
        AssetContext.FOREX -> forexPairs
        AssetContext.CRYPTO -> cryptoPairs
        AssetContext.COMMODITIES -> commodityPairs
        AssetContext.INDICES -> indexPairs
        AssetContext.STOCKS -> stockPairs
        AssetContext.FUTURES -> futuresPairs
        AssetContext.BONDS -> bondPairs
    }
}

private fun formatGalleryPrice(pair: ForexPair): String = when (pair.category) {
    MarketCategory.FOREX -> if (pair.price >= 100.0) {
        String.format(Locale.US, "%.2f", pair.price)
    } else {
        String.format(Locale.US, "%.4f", pair.price)
    }
    MarketCategory.BONDS -> String.format(Locale.US, "%.3f", pair.price)
    else -> String.format(Locale.US, "%.2f", pair.price)
}

private fun galleryPriceUnit(pair: ForexPair): String = when {
    pair.symbol.contains("/") -> pair.symbol.substringAfter("/").uppercase(Locale.US)
    pair.category == MarketCategory.BONDS -> "%"
    pair.category == MarketCategory.INDICES -> "PTS"
    else -> "USD"
}

private fun assetBadgeLabel(pair: ForexPair): String = when {
    pair.symbol.startsWith("BTC", ignoreCase = true) -> "₿"
    pair.symbol.startsWith("ETH", ignoreCase = true) -> "Ξ"
    pair.symbol.startsWith("XAU", ignoreCase = true) || pair.symbol.startsWith("GC", ignoreCase = true) -> "Au"
    pair.symbol.startsWith("XAG", ignoreCase = true) -> "Ag"
    pair.symbol.startsWith("USOIL", ignoreCase = true) || pair.symbol.startsWith("CL", ignoreCase = true) -> "O"
    pair.symbol.startsWith("DXY", ignoreCase = true) -> "DX"
    else -> pair.symbol.filter { it.isLetterOrDigit() }.take(2).uppercase(Locale.US)
}

private fun assetBadgeColors(pair: ForexPair): List<Color> = when {
    pair.symbol.startsWith("BTC", ignoreCase = true) -> listOf(Color(0xFFF7931A), Color(0xFFFFC76B))
    pair.symbol.startsWith("ETH", ignoreCase = true) -> listOf(Color(0xFF627EEA), Color(0xFF98A8FF))
    pair.symbol.startsWith("NVDA", ignoreCase = true) -> listOf(Color(0xFF6AAE43), Color(0xFFA1DB67))
    pair.symbol.startsWith("TSLA", ignoreCase = true) -> listOf(Color(0xFFD74D4D), Color(0xFFFF9A9A))
    pair.symbol.startsWith("AAPL", ignoreCase = true) -> listOf(Color(0xFF707D8B), Color(0xFFB3BECA))
    pair.symbol.startsWith("XAU", ignoreCase = true) || pair.symbol.startsWith("GC", ignoreCase = true) -> listOf(Color(0xFFD3A437), Color(0xFFF2D06F))
    pair.symbol.startsWith("XAG", ignoreCase = true) -> listOf(Color(0xFF98A8B7), Color(0xFFE0E7EF))
    pair.category == MarketCategory.FOREX -> listOf(Color(0xFF2D68FF), Color(0xFF6BB9FF))
    pair.category == MarketCategory.STOCK -> listOf(Color(0xFF4B76FF), Color(0xFF7AA5FF))
    pair.category == MarketCategory.CRYPTO -> listOf(Color(0xFF169B62), Color(0xFF64D59A))
    pair.category == MarketCategory.COMMODITIES -> listOf(Color(0xFFC68D34), Color(0xFFF0C36C))
    pair.category == MarketCategory.INDICES -> listOf(Color(0xFF6556F5), Color(0xFFA79EFF))
    pair.category == MarketCategory.BONDS -> listOf(Color(0xFF5E6874), Color(0xFF9BA7B5))
    pair.category == MarketCategory.FUTURES -> listOf(Color(0xFF0AA6A4), Color(0xFF75E6DA))
    else -> listOf(Color(0xFF4B76FF), Color(0xFF7AA5FF))
}

private fun generateSparklinePoints(
    pair: ForexPair,
    points: Int = 144,
    compression: Float = 1.45f
): List<Float> {
    val random = Random(pair.symbol.hashCode())
    val trendBias = (pair.changePercent / 10.0).coerceIn(-0.24, 0.24).toFloat()
    val values = MutableList(points) { 0f }
    val phaseA = random.nextFloat() * (2f * kotlin.math.PI.toFloat())
    val phaseB = random.nextFloat() * (2f * kotlin.math.PI.toFloat())
    val phaseC = random.nextFloat() * (2f * kotlin.math.PI.toFloat())
    val phaseD = random.nextFloat() * (2f * kotlin.math.PI.toFloat())
    var current = (0.54f - trendBias * 0.35f).coerceIn(0.18f, 0.82f)
    val anchor = current

    repeat(points) { index ->
        val progress = index / (points - 1f)
        val compressedProgress = progress * compression
        val macroWave = kotlin.math.sin((compressedProgress * 10.5f) + phaseA) * 0.098f
        val mediumWave = kotlin.math.sin((compressedProgress * 23.5f) + phaseB) * 0.058f
        val microWave = kotlin.math.sin((compressedProgress * 41.0f) + phaseC) * 0.026f
        val pulseWave = kotlin.math.sin((compressedProgress * 69.0f) + phaseD) * 0.014f
        val target = anchor + macroWave + mediumWave + microWave + pulseWave + (trendBias * (progress - 0.34f))
        val drift = (target - current) * 0.51f
        val noise = (random.nextFloat() - 0.5f) * 0.028f
        current = (current + drift + noise).coerceIn(0.10f, 0.92f)
        values[index] = current
    }

    values[values.lastIndex] = (values.last() + (trendBias * 0.30f)).coerceIn(0.10f, 0.92f)

    return values.mapIndexed { index, value ->
        val prev = values.getOrElse(index - 1) { value }
        val next = values.getOrElse(index + 1) { value }
        ((prev * 0.2f) + (value * 0.6f) + (next * 0.2f)).coerceIn(0.10f, 0.92f)
    }
}

@Composable
fun AscNewsSection() {
    val assetCtx by AssetContextStore.context.collectAsState()
    var newsList by remember { mutableStateOf<List<NewsItem>>(getNewsForContext(assetCtx)) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(assetCtx) {
        try {
            com.asc.markets.data.AssetDataCache.invalidateAll()
            val fetchedNews = fetchNewsFromGemini(assetCtx)
            if (fetchedNews.isNotEmpty()) {
                newsList = if (assetCtx == com.asc.markets.state.AssetContext.ALL) fetchedNews else fetchedNews.filter { it.assetType.equals(assetCtx.name.lowercase(), true) }
                com.asc.markets.data.AssetDataCache.putNews(assetCtx, newsList)
            } else { newsList = getNewsForContext(assetCtx) }
        } catch (e: Exception) { newsList = getNewsForContext(assetCtx) }
        isLoading = false
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("ASC News >", color = Color.White, fontSize = DashboardFontSizes.valueLarge, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
    }
    newsList.take(10).forEach { newsItem -> AscNewsItemRow(newsItem) }
}

@Composable
fun LiquidityPoolVisual(pair: ForexPair) {
    InfoBox(minHeight = 140.dp, containerColor = PureBlack) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🌊", fontSize = DashboardFontSizes.emojiIcon)
                Text("Liquidity Pool Matrix", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidityPoolRow("Buy Side (BSL)", "1.0925", 0.85f, EmeraldSuccess)
                LiquidityPoolRow("Sell Side (SSL)", "1.0815", 0.65f, RoseError)
                LiquidityPoolRow("Internal Range", "1.0870", 0.35f, IndigoAccent)
            }
            Text("Price is drawn to BSL pool. Expect expansion on sweep.", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
        }
    }
}

@Composable
private fun LiquidityPoolRow(label: String, price: String, proximity: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Text(price, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))) {
            Box(modifier = Modifier.fillMaxWidth(proximity).fillMaxHeight().background(color, RoundedCornerShape(3.dp)))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfluenceMatrix(pair: ForexPair) {
    InfoBox(minHeight = 160.dp, containerColor = PureBlack) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🧩", fontSize = DashboardFontSizes.emojiIcon)
                Text("Setup Confluence Matrix", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
            }
            
            val factors = listOf(
                "Higher Timeframe Bias" to true,
                "Liquidity Sweep Confirmed" to true,
                "Market Structure Shift" to true,
                "FVG / Orderblock Entry" to false,
                "Time & Price Alignment" to true
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                factors.forEach { (label, active) ->
                    Surface(
                        color = if (active) EmeraldSuccess.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (active) EmeraldSuccess.copy(alpha = 0.3f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(if (active) "✓" else "○", color = if (active) EmeraldSuccess else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(label, color = if (active) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Text("Score: 4/5 - High Probability Setup Developing", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun SessionKillzones() {
    InfoBox(minHeight = 140.dp, containerColor = PureBlack) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚔️", fontSize = DashboardFontSizes.emojiIcon)
                    Text("Session Killzones", color = Color.White, fontSize = DashboardFontSizes.valueMedium, fontWeight = FontWeight.Black)
                }
                Surface(color = EmeraldSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text("LONDON OPEN", color = EmeraldSuccess, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Current UTC: 08:42:15", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    KillzoneBar("London", "07:00-10:00", 0.6f, true)
                    KillzoneBar("New York", "12:00-15:00", 0f, false)
                    KillzoneBar("Asia", "00:00-03:00", 1f, false)
                }
            }
        }
    }
}

@Composable
private fun KillzoneBar(name: String, time: String, progress: Float, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, color = if (active) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
        Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))) {
            if (active) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(EmeraldSuccess, RoundedCornerShape(2.dp)))
            }
        }
        Text(time, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AscNewsItemRow(newsItem: NewsItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF2d2d2d)), contentAlignment = Alignment.Center) {
                Text(when (newsItem.assetType) { "forex" -> "🔄"; "crypto" -> "₿"; "stocks" -> "📈"; "indices" -> "📊"; else -> "📰" }, fontSize = DashboardFontSizes.bodyTiny)
            }
            Text("${newsItem.source} • ${newsItem.timestamp}", color = Color.Gray, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = newsItem.headline, color = Color.White, fontSize = DashboardFontSizes.valueMediumLarge, fontWeight = FontWeight.Bold, lineHeight = 22.sp, fontFamily = InterFontFamily, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
