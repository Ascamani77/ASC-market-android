package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.AppView
import com.asc.markets.data.remote.FinalDecisionItem
import com.asc.markets.data.remote.LatestDeploymentsResponse
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.components.PairFlags
import com.asc.markets.ui.screens.dashboard.CurrencyStrengthPanel
import com.asc.markets.ui.screens.dashboard.MarketCompareDensity
import com.asc.markets.ui.theme.*
import com.researchcenter.ui.viewmodel.NewsViewModel as ResearchNewsViewModel
import com.researchcenter.ui.viewmodel.NewsViewModelFactory as ResearchNewsViewModelFactory
import com.researchcenter.data.models.NewsArticle as ResearchNewsArticle
import com.asc.markets.state.AssetContext
import com.asc.markets.state.AssetContextStore
import com.asc.markets.ui.theme.InterFontFamily
import androidx.compose.ui.platform.LocalContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// --- News Data Model ---
data class NewsItem(
    val headline: String,
    val source: String,
    val timestamp: String,
    val assetType: String,
    val assetSymbol: String = "",
    val imageUrl: String = ""
)

// --- Helper Functions ---
private fun formatArticleTime(publishedAt: String): String {
    return try {
        OffsetDateTime.parse(publishedAt)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))
    } catch (_: Exception) {
        publishedAt.take(16).ifBlank { "Unknown time" }
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

@Composable
fun HomeAlertsScreen(viewModel: ForexViewModel) {
    val context = LocalContext.current
    val researchNewsViewModel: ResearchNewsViewModel = viewModel(factory = ResearchNewsViewModelFactory(context))
    val researchArticles by researchNewsViewModel.articles.collectAsState()
    val aiDiscoveryArticles by researchNewsViewModel.aiDiscoveryArticles.collectAsState()

    LaunchedEffect(Unit) {
        researchNewsViewModel.fetchAiSortedNews()
    }

    val newsItemsForCtx = if (aiDiscoveryArticles.isNotEmpty()) aiDiscoveryArticles else researchArticles

    val aiResponseState = viewModel.aiDeployments.collectAsState()
    val aiResponse: LatestDeploymentsResponse? = aiResponseState.value
    val allSignals = aiResponse?.final_decision ?: emptyList<FinalDecisionItem>()

    // Top 3 signals by score
    val topSignals = remember(allSignals) {
        allSignals.sortedByDescending { it.journal_score ?: 0.0 }.take(3)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("AI DEPLOYMENT SUMMARY", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("Top Operational Alerts", color = SlateText, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (aiResponse != null) EmeraldSuccess else RoseError, shape = RoundedCornerShape(5.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Vitals Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalsMiniCard(
                    label = "ACTIVE SIGNALS",
                    value = (aiResponse?.count ?: 0).toString(),
                    modifier = Modifier.weight(1f)
                )
                VitalsMiniCard(
                    label = "SYNC STATUS",
                    value = if (aiResponse != null) "LIVE" else "OFFLINE",
                    modifier = Modifier.weight(1f),
                    valueColor = if (aiResponse != null) EmeraldSuccess else RoseError
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            DeterministicTimingWidget(topSignals)

            Spacer(modifier = Modifier.height(16.dp))
            CurrencyStrengthPanel(density = MarketCompareDensity.COMPACT)

            Spacer(modifier = Modifier.height(24.dp))

            // News Flow Section
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📊", fontSize = 18.sp)
                    Text("Raw Feed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (newsItemsForCtx.isEmpty()) {
                        Text("No asset-tagged headlines available.", color = SlateText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    } else {
                        val mappedNews = newsItemsForCtx.take(5).map { article ->
                            NewsItem(
                                headline = article.title,
                                source = article.source,
                                timestamp = formatArticleTime(article.publishedAt),
                                assetType = article.category,
                                assetSymbol = "",
                                imageUrl = article.imageUrl ?: ""
                            )
                        }
                        mappedNews.forEach { item ->
                            RawFeedNewsRow(newsItem = item, fallbackSymbol = inferNewsAssetSymbol(item.assetType, item.headline))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("PRIORITY OPPORTUNITIES", color = IndigoAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (topSignals.isEmpty()) {
                InfoBox(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active deployments found.", color = SlateText, fontSize = 14.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topSignals.forEach { signal ->
                        HomeSignalAlertItem(signal) {
                            viewModel.navigateTo(AppView.DASHBOARD)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Prompt
            InfoBox(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.navigateTo(AppView.DASHBOARD) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("VIEW FULL MATRIX", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("›", color = IndigoAccent, fontSize = 18.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DeterministicTimingWidget(signals: List<FinalDecisionItem>) {
    InfoBox(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PRE-MOVE TIMING CONVERGENCE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Surface(color = IndigoAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text("PROTOCOL L14", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            
            if (signals.isEmpty()) {
                Text("Scanning timing windows...", color = SlateText, fontSize = 11.sp)
            } else {
                signals.forEach { signal ->
                    val score = ((signal.journal_score ?: 0.0) / 100f).toFloat().coerceIn(0f, 1f)
                    val label = if (score > 0.8) "DISPATCH IMMINENT" else "ACCUMULATING"
                    val color = if (score > 0.8) RoseError else EmeraldSuccess
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(signal.asset_1 ?: "---", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                        Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(score).fillMaxHeight().background(color, RoundedCornerShape(3.dp)))
                        }
                        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalsMiniCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    InfoBox(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HomeSignalAlertItem(item: FinalDecisionItem, onClick: () -> Unit) {
    val dirIsBuy = (item.journal_direction?.uppercase() ?: "BUY") == "BUY"
    
    InfoBox(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.asset_1 ?: "UNKNOWN",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (dirIsBuy) "BUY" else "SELL",
                        color = if (dirIsBuy) EmeraldSuccess else RoseError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    item.portfolio_decision_reason?.take(60)?.let { if (it.length == 60) "$it..." else it } ?: "Awaiting market confluence...",
                    color = SlateText,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(item.journal_score ?: 0.0).roundToInt()}%",
                    color = IndigoAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "CONFIDENCE",
                    color = SlateText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = headline,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
            fontFamily = InterFontFamily,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
