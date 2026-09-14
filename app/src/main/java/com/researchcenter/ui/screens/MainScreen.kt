package com.researchcenter.ui.screens
import com.asc.markets.ui.components.AscRollingSpinner

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.data.NetworkConfig
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.data.models.ViewMode
import com.researchcenter.ui.components.ArticleDetail
import com.researchcenter.ui.components.NewsList
import com.researchcenter.ui.viewmodel.NewsViewModel
import com.researchcenter.ui.viewmodel.NewsViewModelFactory
import com.researchcenter.util.Constants
import com.researchcenter.ui.theme.Black
import com.researchcenter.ui.theme.Gray400
import com.researchcenter.ui.theme.SidebarBg
import com.researchcenter.ui.theme.White
import com.trading.app.data.Mt5NewsStore
import com.trading.app.data.Mt5Service
import java.time.OffsetDateTime
import java.util.Calendar

@Composable
fun MainScreen(
    viewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(LocalContext.current)),
    onBackToApp: () -> Unit = {}
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
                Log.i("ResearchMainScreen", "Received ${payload.items.size} MT5 FXStreet news items")
                Mt5NewsStore.updateNews(payload.items)
            },
            onConnectionStatusUpdate = { connected ->
                Log.i("ResearchMainScreen", "MT5 news bridge connected=$connected")
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

    // Refresh Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // System Back Button Handler
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

    // Category counts logic
    val categoryCounts = remember(articles, aiDiscoveryArticles) {
        val counts = mutableMapOf<String, Int>()
        counts["all"] = articles.size
        Constants.CATEGORIES.forEach { cat ->
            if (cat.id == "ai-sorted") {
                counts[cat.id] = if (aiDiscoveryArticles.isNotEmpty()) aiDiscoveryArticles.size else 40
            } else if (cat.id != "all") {
                counts[cat.id] = articles.filter { it.category == cat.id }.size
            }
        }
        counts
    }

    val displayArticles = remember(articles, aiDiscoveryArticles, searchTerm, searchResults, showBookmarksOnly, bookmarks) {
        if (showBookmarksOnly) {
            bookmarks
        } else if (searchTerm.isNotEmpty()) {
            searchResults
        } else {
            // Show ALL articles together (no category filtering)
            val baseList = articles
            // Filter out upcoming events (where publishedAt is in the future) and strict calendar/schedule events
            val now = System.currentTimeMillis()
            baseList.filter { article ->
                val isCalendarEvent = article.category == "calendar" || 
                                      article.category == "macro_cal" || 
                                      article.intelligence?.asset_tags?.any { it.contains("SCHEDULE") } == true ||
                                      article.source.contains("TREASURYDIRECT.GOV", ignoreCase = true) ||
                                      article.title.contains("monthly statement of public debt", ignoreCase = true)

                if (isCalendarEvent) return@filter false

                try {
                    val ts = OffsetDateTime.parse(article.publishedAt).toInstant().toEpochMilli()
                    ts <= now
                } catch (e: Exception) {
                    true // Include if parsing fails
                }
            }.let { filtered ->
                val mt5Articles = filtered
                    .filter { it.id.startsWith("mt5_") }
                    .sortedByDescending { article ->
                        try {
                            OffsetDateTime.parse(article.publishedAt).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            0L
                        }
                    }
                val otherArticles = filtered
                    .filterNot { it.id.startsWith("mt5_") }
                    .sortedByDescending { it.publishedAt }
                mt5Articles + otherArticles
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Black)) {
        if (viewMode == ViewMode.LIST) {
            // Header
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

                Text(
                    if (showBookmarksOnly) "Bookmarks" else "Research",
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.weight(1f)
                )

                if (!showBookmarksOnly) {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = White, modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = {
                            viewModel.refreshNews()
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
                        BadgedBox(
                            badge = {
                                if (bookmarks.isNotEmpty()) {
                                    Badge(
                                        containerColor = White,
                                        contentColor = Black,
                                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                    ) {
                                        Text(bookmarks.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (bookmarks.isNotEmpty()) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmarks",
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            if (isSearchVisible && !showBookmarksOnly) {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    TextField(
                        value = searchTerm,
                        onValueChange = { viewModel.searchNews(it) },
                        placeholder = { Text("Search intelligence...", color = Gray400, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
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

            if (!showBookmarksOnly) {
                // Category navbar removed - all articles shown together
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (viewMode == ViewMode.LIST) {
                if (isLoading && displayArticles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AscRollingSpinner(color = White)
                    }
                } else {
                    NewsList(
                        articles = displayArticles,
                        onArticleClick = { article ->
                            selectedArticle = article
                            viewMode = ViewMode.ARTICLE
                            viewModel.getExplanation(article)
                        },
                        onBookmarkClick = { article ->
                            viewModel.toggleBookmark(article)
                        },
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
                    }
                )
            }
        }
    }
}
