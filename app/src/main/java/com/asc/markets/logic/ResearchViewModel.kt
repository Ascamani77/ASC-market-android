package com.asc.markets.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.ui.screens.tradeDashboard.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ResearchViewModel : ViewModel() {
    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles

    private val _bookmarks = MutableStateFlow<Set<String>>(emptySet())
    val bookmarks: StateFlow<Set<String>> = _bookmarks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var lastRefreshTime: Long = 0
    private var lastRefreshDay: Int = -1

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        refreshIfNecessary()
    }

    fun refreshIfNecessary(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        
        val isNewDay = currentDay != lastRefreshDay
        val isCacheExpired = now - lastRefreshTime > 2 * 60 * 60 * 1000 // 2 hours

        if (force || isNewDay || isCacheExpired) {
            fetchArticles(isNewDay)
            lastRefreshTime = now
            lastRefreshDay = currentDay
        }
    }

    private fun fetchArticles(replaceEntirely: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            // In a real app, this would call ResearchRepository
            // For now, we simulate API fetch
            val newArticles = getMockArticles()
            
            if (replaceEntirely) {
                _articles.value = newArticles
            } else {
                val existingIds = _articles.value.map { it.id }.toSet()
                val uniqueNew = newArticles.filter { it.id !in existingIds }
                _articles.value = _articles.value + uniqueNew
            }
            _isLoading.value = false
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            // Hybrid Search Logic
            val localResults = _articles.value.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.intelligence.assetTags.any { tag -> tag.contains(query, ignoreCase = true) }
            }

            if (localResults.size < 3 && query.isNotEmpty()) {
                // Fallback to external search
                fetchExternalSearch(query)
            }
        }
    }

    private fun fetchExternalSearch(query: String) {
        viewModelScope.launch {
            // Simulate GET /api/news/search?q=query
        }
    }

    fun toggleBookmark(article: NewsArticle) {
        val current = _bookmarks.value.toMutableSet()
        if (article.id in current) {
            current.remove(article.id)
        } else {
            current.add(article.id)
            // Capture full content if in detail view (handled by UI passing the article)
        }
        _bookmarks.value = current
    }

    private fun getMockArticles(): List<NewsArticle> {
        val now = System.currentTimeMillis()
        return listOf(
            NewsArticle(
                id = "1",
                title = "ECB Signals Potential Rate Cut in June",
                summary = "Inflation data shows cooling trend across Eurozone, prompting policy shift discussions.",
                content = "Detailed report on ECB's latest meeting minutes and inflation projections...",
                url = "https://example.com/ecb-news",
                source = "Reuters",
                publishedAt = now - 1000 * 60 * 45,
                category = "Central Banks",
                intelligence = ArticleIntelligence(
                    sentiment = Sentiment.POSITIVE,
                    confidence = Confidence.HIGH,
                    assetTags = listOf("eur_usd", "ecb", "monetary_policy"),
                    impactScore = 8.5
                )
            ),
            NewsArticle(
                id = "2",
                title = "Nvidia Unveils Next-Gen AI Architecture",
                summary = "Blackwell platform promises 30x performance boost for LLM inference workloads.",
                content = "Full breakdown of NVDA's GTC keynote and technical specs of the B200 GPU...",
                url = "https://example.com/nvda-gtc",
                source = "TechCrunch",
                publishedAt = now - 1000 * 60 * 120,
                category = "AI Sorted",
                intelligence = ArticleIntelligence(
                    sentiment = Sentiment.POSITIVE,
                    confidence = Confidence.HIGH,
                    assetTags = listOf("nvda", "ai_trends", "tech_sector"),
                    impactScore = 9.2
                )
            )
        )
    }
}
