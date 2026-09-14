package com.researchcenter.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import com.asc.markets.ui.components.AscRollingSpinner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.NetworkConfig
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.data.models.ViewMode
import com.researchcenter.ui.components.ArticleDetail
import com.researchcenter.ui.theme.Black
import com.researchcenter.ui.theme.Gray400
import com.researchcenter.ui.theme.SidebarBg
import com.researchcenter.ui.theme.White
import com.researchcenter.ui.viewmodel.NewsViewModel
import com.researchcenter.ui.viewmodel.NewsViewModelFactory
import com.trading.app.data.Mt5NewsStore
import com.trading.app.data.Mt5Service
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun AnalysisOpinionScreen(
    viewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(LocalContext.current)),
    onBackToApp: () -> Unit = {},
    activeAssets: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val articles by viewModel.articles.collectAsState()
    val aiDiscoveryArticles by viewModel.aiDiscoveryArticles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val aiExplanation by viewModel.aiExplanation.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showBookmarksOnly by remember { mutableStateOf(false) }

    val mt5Host = remember(context) { NetworkConfig.mt5Host(context) }
    val mt5Port = remember(context) { NetworkConfig.mt5Port(context) }
    val mt5NewsService = remember(mt5Host, mt5Port) {
        Mt5Service(
            pcIpAddress = mt5Host,
            port = mt5Port,
            onHistoryUpdate = { _, _ -> },
            onQuoteUpdate = {},
            onNewsUpdate = { payload ->
                Log.i("AnalysisOpinionScreen", "Received ${payload.items.size} MT5 FXStreet news items")
                Mt5NewsStore.updateNews(payload.items)
            },
            onConnectionStatusUpdate = { connected ->
                Log.i("AnalysisOpinionScreen", "MT5 news bridge connected=$connected")
            }
        )
    }

    LaunchedEffect(mt5NewsService) {
        mt5NewsService.connect()
        mt5NewsService.requestNews()
    }

    DisposableEffect(mt5NewsService) {
        onDispose {
            mt5NewsService.disconnect()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchAiSortedNews()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "analysis_refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "analysis_rotation"
    )

    BackHandler(enabled = true) {
        when {
            viewMode == ViewMode.ARTICLE -> {
                viewMode = ViewMode.LIST
                selectedArticle = null
            }
            isSearchVisible -> {
                if (searchTerm.isNotEmpty()) {
                    viewModel.searchNews("")
                } else {
                    isSearchVisible = false
                }
            }
            showBookmarksOnly -> {
                showBookmarksOnly = false
            }
            else -> {
                onBackToApp()
            }
        }
    }

    val filteredArticles = remember(articles, activeAssets) {
        articles
            .filterNot(::isCalendarOrFutureArticle)
            .filter { isArticleRelevantToAssets(it, activeAssets) }
            .let(::buildBalancedAnalysisFeed)
    }
    val featuredInsights = remember(aiDiscoveryArticles, filteredArticles, activeAssets) {
        val source = if (aiDiscoveryArticles.isNotEmpty()) aiDiscoveryArticles else filteredArticles
        source
            .filterNot(::isCalendarOrFutureArticle)
            .filter { isArticleRelevantToAssets(it, activeAssets) }
            .sortedByDescending(::analysisScore)
            .take(3)
    }
    val displayArticles = remember(filteredArticles, searchTerm, searchResults, showBookmarksOnly, bookmarks) {
        when {
            showBookmarksOnly -> bookmarks.sortedByDescending(::analysisScore)
            searchTerm.isNotEmpty() -> searchResults.sortedByDescending(::analysisScore)
            else -> filteredArticles
        }
    }
    val highlightedIds = remember(featuredInsights) { featuredInsights.map { it.id }.toSet() }
    val coverageArticles = remember(displayArticles, highlightedIds, showBookmarksOnly, searchTerm) {
        if (showBookmarksOnly || searchTerm.isNotEmpty()) {
            displayArticles
        } else {
            displayArticles.filterNot { it.id in highlightedIds }
        }
    }
    val highImpactCount = remember(displayArticles) {
        displayArticles.count { (it.intelligence?.impact_score ?: 0.0) >= 0.7 }
    }
    val trackedThemes = remember(displayArticles) {
        displayArticles.flatMap { it.intelligence?.asset_tags ?: emptyList() }.distinct().take(6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        if (viewMode == ViewMode.LIST) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (showBookmarksOnly) {
                        showBookmarksOnly = false
                    } else {
                        onBackToApp()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showBookmarksOnly) "Saved insights" else "Analysis & Opinion",
                        color = White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                if (!showBookmarksOnly) {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = White, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = {
                            viewModel.refreshNews()
                            viewModel.fetchAiSortedNews()
                            mt5NewsService.requestNews()
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = White,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(if (isLoading) rotation else 0f)
                        )
                    }

                    IconButton(onClick = { showBookmarksOnly = true }) {
                        Icon(
                            if (bookmarks.isNotEmpty()) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmarks",
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (isSearchVisible && !showBookmarksOnly) {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    TextField(
                        value = searchTerm,
                        onValueChange = { viewModel.searchNews(it) },
                        placeholder = { Text("Search market narratives...", color = Gray400, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SidebarBg,
                            unfocusedContainerColor = SidebarBg,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (searchTerm.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchNews("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = White)
                                }
                            } else {
                                IconButton(onClick = { isSearchVisible = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = White)
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (viewMode == ViewMode.LIST) {
                if (isLoading && displayArticles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AscRollingSpinner(color = White)
                    }
                } else {
                    AnalysisOpinionList(
                        featuredInsights = if (showBookmarksOnly || searchTerm.isNotEmpty()) emptyList() else featuredInsights,
                        coverageArticles = coverageArticles,
                        totalArticles = displayArticles.size,
                        highImpactCount = highImpactCount,
                        trackedThemes = trackedThemes,
                        onArticleClick = { article ->
                            selectedArticle = article
                            viewMode = ViewMode.ARTICLE
                            viewModel.getExplanation(article)
                        },
                        onBookmarkClick = { article -> viewModel.toggleBookmark(article) },
                        bookmarkedIds = bookmarks.map { it.id }.toSet()
                    )
                }
            } else if (selectedArticle != null) {
                ArticleDetail(
                    article = selectedArticle!!,
                    aiExplanation = aiExplanation,
                    isBookmarked = bookmarks.any { it.id == selectedArticle!!.id },
                    onBackClick = {
                        viewMode = ViewMode.LIST
                        selectedArticle = null
                    },
                    onBookmarkClick = {
                        viewModel.toggleBookmark(selectedArticle!!)
                    },
                    screenTitle = "Analysis & Opinion",
                    insightLabel = "ANALYST BRIEF",
                    showAiExplanation = true
                )
            }
        }
    }
}

@Composable
private fun AnalysisOpinionList(
    featuredInsights: List<NewsArticle>,
    coverageArticles: List<NewsArticle>,
    totalArticles: Int,
    highImpactCount: Int,
    trackedThemes: List<String>,
    onArticleClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit,
    bookmarkedIds: Set<String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            InsightSummaryRow(
                totalArticles = totalArticles,
                highImpactCount = highImpactCount,
                trackedThemes = trackedThemes.size
            )
        }

        if (featuredInsights.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Top narratives",
                    subtitle = "AI-ranked or highest-signal stories worth interpreting first.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(featuredInsights, key = { it.id }) { article ->
                FeaturedInsightCard(
                    article = article,
                    isBookmarked = bookmarkedIds.contains(article.id),
                    onClick = { onArticleClick(article) },
                    onBookmarkClick = { onBookmarkClick(article) }
                )
            }
        }

        item {
            SectionTitle(
                title = if (featuredInsights.isNotEmpty()) "Broader coverage" else "Coverage",
                subtitle = "Use this layer for wider context after the lead narratives.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        items(coverageArticles, key = { it.id }) { article ->
            InsightArticleCard(
                article = article,
                isBookmarked = bookmarkedIds.contains(article.id),
                onClick = { onArticleClick(article) },
                onBookmarkClick = { onBookmarkClick(article) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun InsightSummaryRow(
    totalArticles: Int,
    highImpactCount: Int,
    trackedThemes: Int
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        maxItemsInEachRow = 3
    ) {
        InsightMetricTile(label = "Coverage", value = totalArticles.toString(), modifier = Modifier.weight(1f, fill = true))
        InsightMetricTile(label = "High impact", value = highImpactCount.toString(), modifier = Modifier.weight(1f, fill = true))
        InsightMetricTile(label = "Themes", value = trackedThemes.toString(), modifier = Modifier.weight(1f, fill = true))
    }
}

@Composable
private fun InsightMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Text(text = label, color = Gray400, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, color = Gray400, fontSize = 12.sp)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FeaturedInsightCard(
    article: NewsArticle,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Black,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.title,
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = article.summary.ifBlank { article.content.ifBlank { article.title } },
                        color = White.copy(alpha = 0.82f),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = White
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                article.intelligence?.impact_score?.let { score ->
                    InsightChip(text = "Impact ${(score * 100).roundToInt()}%")
                }
                val src = article.source.uppercase()
                val cat = article.category.uppercase()
                InsightChip(text = src)
                if (cat != src && !src.contains(cat) && !cat.contains(src) && cat != "FXSTREET") {
                    InsightChip(text = cat)
                }
                article.intelligence?.confidence?.takeIf { it.isNotBlank() }?.let { conf ->
                    InsightChip(text = conf.uppercase())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatArticleTime(article.publishedAt),
                color = Gray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun InsightArticleCard(
    article: NewsArticle,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Black,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.title,
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = article.summary.ifBlank { article.content.ifBlank { article.title } },
                        color = White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = Gray400
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                article.intelligence?.impact_score?.let { score ->
                    InsightChip(text = "Impact ${(score * 100).roundToInt()}%")
                }
                val src = article.source.uppercase()
                val cat = article.category.uppercase()
                InsightChip(text = src)
                if (cat != src && !src.contains(cat) && !cat.contains(src) && cat != "FXSTREET") {
                    InsightChip(text = cat)
                }
                article.intelligence?.confidence?.takeIf { it.isNotBlank() }?.let { conf ->
                    InsightChip(text = conf.uppercase())
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatArticleTime(article.publishedAt),
                color = Gray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetadataRow(article: NewsArticle) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightChip(text = formatArticleTime(article.publishedAt))
        InsightChip(text = article.source.uppercase())
        InsightChip(text = article.category.uppercase())
        article.intelligence?.confidence?.takeIf { it.isNotBlank() }?.let {
            InsightChip(text = it.uppercase())
        }
        article.intelligence?.impact_score?.let {
            InsightChip(text = "Impact ${(it * 100).roundToInt()}%")
        }
    }
}

@Composable
private fun InsightChip(text: String) {
    Surface(
        color = SidebarBg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = Gray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun analysisScore(article: NewsArticle): Double {
    val intelligence = article.intelligence
    val impact = intelligence?.impact_score ?: 0.0
    val confidenceBoost = if (intelligence?.confidence.equals("high", ignoreCase = true)) 0.2 else 0.0
    val sourceBoost = if (article.id.startsWith("mt5_")) -0.05 else 0.15
    val narrativeBoost = if (article.summary.isNotBlank() && article.summary != article.title) 0.1 else 0.0
    return impact + confidenceBoost + sourceBoost + narrativeBoost
}

private fun buildBalancedAnalysisFeed(articles: List<NewsArticle>): List<NewsArticle> {
    val rankedNonMt5 = articles
        .filterNot { it.id.startsWith("mt5_") }
        .sortedByDescending(::analysisScore)

    val rankedMt5 = articles
        .filter { it.id.startsWith("mt5_") }
        .sortedByDescending(::analysisScore)

    if (rankedNonMt5.isEmpty() || rankedMt5.isEmpty()) {
        return articles.sortedByDescending(::analysisScore)
    }

    val totalLimit = minOf(200, articles.size)
    val reservedNonMt5 = minOf(rankedNonMt5.size, maxOf(totalLimit / 2, 40))
    val reservedMt5 = minOf(rankedMt5.size, totalLimit - reservedNonMt5)

    val blended = mutableListOf<NewsArticle>()
    val primaryNonMt5 = rankedNonMt5.take(reservedNonMt5)
    val primaryMt5 = rankedMt5.take(reservedMt5)
    val remainder = (rankedNonMt5.drop(reservedNonMt5) + rankedMt5.drop(reservedMt5))
        .sortedByDescending(::analysisScore)

    val maxLead = maxOf(primaryNonMt5.size, primaryMt5.size)
    for (index in 0 until maxLead) {
        primaryNonMt5.getOrNull(index)?.let(blended::add)
        primaryMt5.getOrNull(index)?.let(blended::add)
    }

    return (blended + remainder)
        .distinctBy { it.id }
        .take(totalLimit)
}

private fun formatArticleTime(publishedAt: String): String {
    return try {
        OffsetDateTime.parse(publishedAt)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))
    } catch (_: Exception) {
        publishedAt.take(16).ifBlank { "Unknown time" }
    }
}

private fun isArticleRelevantToAssets(article: NewsArticle, activeAssets: Set<String>): Boolean {
    if (activeAssets.isEmpty()) return true

    val assetKeywords = activeAssets.flatMap { symbol ->
        when {
            symbol.length == 6 -> listOf(symbol.take(3), symbol.takeLast(3))
            symbol.endsWith("USD") -> listOf(symbol.removeSuffix("USD"), "USD")
            else -> listOf(symbol)
        }
    }.filter { it.isNotBlank() }.map { it.uppercase() }.toSet()

    val generalRelevanceKeywords = setOf("FED", "FOMC", "INFLATION", "CPI", "CENTRAL BANK", "INTEREST RATE", "MACRO")

    val tags = article.intelligence?.asset_tags?.map { it.uppercase() } ?: emptyList()
    if (tags.any { tag -> assetKeywords.any { asset -> tag.contains(asset) } }) {
        return true
    }

    val titleUpper = article.title.uppercase()
    val summaryUpper = article.summary.uppercase()
    val contentUpper = article.content.uppercase()

    if (assetKeywords.any { asset -> 
            titleUpper.contains(asset) || 
            summaryUpper.contains(asset) || 
            contentUpper.contains(asset)
        }) {
        return true
    }

    if (generalRelevanceKeywords.any { kw ->
            titleUpper.contains(kw) ||
            summaryUpper.contains(kw)
        }) {
        return true
    }

    return false
}

private fun isCalendarOrFutureArticle(article: NewsArticle): Boolean {
    val isCalendarEvent = article.category == "calendar" ||
        article.category == "macro_cal" ||
        article.intelligence?.asset_tags?.any { it.contains("SCHEDULE") } == true ||
        article.source.contains("TREASURYDIRECT.GOV", ignoreCase = true) ||
        article.title.contains("monthly statement of public debt", ignoreCase = true)

    if (isCalendarEvent) return true

    return try {
        val ts = OffsetDateTime.parse(article.publishedAt).toInstant().toEpochMilli()
        ts > System.currentTimeMillis()
    } catch (_: Exception) {
        false
    }
}
