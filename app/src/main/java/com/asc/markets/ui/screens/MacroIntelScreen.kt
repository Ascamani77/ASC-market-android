package com.asc.markets.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ResearchViewModel
import com.asc.markets.ui.screens.tradeDashboard.model.*

@Composable
fun MacroIntelScreen(viewModel: ResearchViewModel = viewModel()) {
    val articles by viewModel.articles.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    
    val categories = listOf(
        NewsCategory("all", "All", "", 748),
        NewsCategory("ai", "AI sorted", "", 40),
        NewsCategory("cb", "Central Banks", "", 303),
        NewsCategory("energy", "Energy & Commodities", "", 5),
        NewsCategory("macro_data", "Macro Data", "", 85),
        NewsCategory("macro_cal", "Macro Calendar", "", 133),
        NewsCategory("gov", "Gov & Reg", "", 224)
    )

    Scaffold(
        topBar = {
            ResearchTopBar(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.search(it) },
                onBack = { /* Handle back */ },
                onRefresh = { viewModel.refreshIfNecessary(true) }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navigation Categories
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(categories) { cat ->
                    CategoryChip(
                        category = cat,
                        isSelected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name }
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // News Feed
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                val filteredArticles = articles.filter { 
                    selectedCategory == "All" || it.category == selectedCategory 
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(filteredArticles) { article ->
                        NewsCard(
                            article = article,
                            isBookmarked = article.id in bookmarks,
                            onBookmarkToggle = { viewModel.toggleBookmark(article) },
                            onClick = { selectedArticle = article }
                        )
                    }
                }
            }
        }
    }

    if (selectedArticle != null) {
        ArticleDetailOverlay(
            article = selectedArticle!!,
            onClose = { selectedArticle = null },
            onBookmarkToggle = { viewModel.toggleBookmark(selectedArticle!!) },
            isBookmarked = selectedArticle!!.id in bookmarks
        )
    }
}
