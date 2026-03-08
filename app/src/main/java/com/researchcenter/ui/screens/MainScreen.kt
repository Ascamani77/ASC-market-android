package com.researchcenter.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.data.models.ViewMode
import com.researchcenter.ui.components.ArticleDetail
import com.researchcenter.ui.components.NewsList
import com.researchcenter.ui.viewmodel.NewsViewModel
import com.researchcenter.util.Constants
import com.researchcenter.ui.theme.Black
import com.researchcenter.ui.theme.Gray400
import com.researchcenter.ui.theme.SidebarBg
import com.researchcenter.ui.theme.White
import java.time.OffsetDateTime
import java.util.Calendar

@Composable
fun MainScreen(
    viewModel: NewsViewModel = viewModel(),
    onBackToApp: () -> Unit = {}
) {
    val articles by viewModel.articles.collectAsState()
    val aiDiscoveryArticles by viewModel.aiDiscoveryArticles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchTerm by viewModel.searchTerm.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val aiExplanation by viewModel.aiExplanation.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var activeCategory by remember { mutableStateOf("all") }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showBookmarksOnly by remember { mutableStateOf(false) }

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

    val displayArticles = remember(activeCategory, articles, aiDiscoveryArticles, searchTerm, searchResults, showBookmarksOnly, bookmarks) {
        if (showBookmarksOnly) {
            bookmarks
        } else if (searchTerm.isNotEmpty()) {
            searchResults
        } else {
            val baseList = if (activeCategory == "all") {
                articles
            } else {
                articles.filter { it.category == activeCategory }
            }

            val now = System.currentTimeMillis()
            val upcoming = mutableListOf<NewsArticle>()
            val passed = mutableListOf<NewsArticle>()

            val threeDaysLater = Calendar.getInstance()
            threeDaysLater.add(Calendar.DAY_OF_YEAR, 3)
            threeDaysLater.set(Calendar.HOUR_OF_DAY, 23)
            threeDaysLater.set(Calendar.MINUTE, 59)
            threeDaysLater.set(Calendar.SECOND, 59)
            threeDaysLater.set(Calendar.MILLISECOND, 999)
            val horizonTime = threeDaysLater.timeInMillis

            baseList.forEach { a ->
                try {
                    val ts = OffsetDateTime.parse(a.publishedAt).toInstant().toEpochMilli()
                    if (ts > now) {
                        if (ts <= horizonTime) {
                            upcoming.add(a)
                        }
                    } else {
                        passed.add(a)
                    }
                } catch (e: Exception) {
                    upcoming.add(a)
                }
            }

            upcoming.sortBy { try { OffsetDateTime.parse(it.publishedAt).toInstant().toEpochMilli() } catch(e: Exception) { 0L } }
            passed.sortByDescending { try { OffsetDateTime.parse(it.publishedAt).toInstant().toEpochMilli() } catch(e: Exception) { 0L } }

            upcoming + passed
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
                        onClick = { viewModel.refreshNews() },
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
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Black),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(Constants.CATEGORIES) { cat ->
                        val isActive = activeCategory == cat.id
                        val count = categoryCounts[cat.id] ?: 0

                        Column(
                            modifier = Modifier
                                .clickable {
                                    activeCategory = cat.id
                                    viewMode = ViewMode.LIST
                                    selectedArticle = null
                                }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (cat.id == "all") "All" else cat.name,
                                    color = if (isActive) White else Gray400,
                                    fontSize = 16.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    count.toString(),
                                    color = if (isActive) White.copy(alpha = 0.6f) else Gray400.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isActive) {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(3.dp)
                                        .background(White)
                                )
                            } else {
                                Spacer(Modifier.height(11.dp))
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (viewMode == ViewMode.LIST) {
                if (isLoading && displayArticles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = White)
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
