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
fun ResearchCenterScreen(viewModel: ResearchViewModel = viewModel()) {
    val articles by viewModel.articles.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }
    
    val categories = listOf(
        NewsCategory("all", "All", "🌐"),
        NewsCategory("ai", "AI Sorted", "✨"),
        NewsCategory("cb", "Central Banks", "🏦"),
        NewsCategory("energy", "Energy", "⚡"),
        NewsCategory("macro", "Macro", "📉"),
        NewsCategory("gov", "Gov & Reg", "⚖️")
    )

    Scaffold(
        topBar = {
            ResearchTopBar(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.search(it) }
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
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { cat ->
                    CategoryChip(
                        category = cat,
                        isSelected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name }
                    )
                }
            }

            // Trending Topics (Mock)
            TrendingTopicsRow()

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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
