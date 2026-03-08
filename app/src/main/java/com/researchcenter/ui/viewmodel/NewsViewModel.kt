package com.researchcenter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.services.AiService
import com.researchcenter.services.NewsService
import com.researchcenter.util.StringUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(
    private val newsService: NewsService = NewsService(),
    private val aiService: AiService = AiService()
) : ViewModel() {

    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val _aiDiscoveryArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val aiDiscoveryArticles: StateFlow<List<NewsArticle>> = _aiDiscoveryArticles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchTerm = MutableStateFlow("")
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NewsArticle>>(emptyList())
    val searchResults: StateFlow<List<NewsArticle>> = _searchResults.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    private val _fullContent = MutableStateFlow<String?>(null)
    val fullContent: StateFlow<String?> = _fullContent.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<NewsArticle>>(emptyList())
    val bookmarks: StateFlow<List<NewsArticle>> = _bookmarks.asStateFlow()

    private var _lastAiUpdate = 0L

    init {
        fetchAllNews()
    }

    fun refreshNews() {
        fetchAllNews()
        // Force AI refresh if we've already loaded it once
        if (_aiDiscoveryArticles.value.isNotEmpty()) {
            _lastAiUpdate = 0L
            fetchAiSortedNews()
        }
    }

    fun fetchAllNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _articles.value = newsService.fetchAllNews()
            _isLoading.value = false
        }
    }

    fun fetchAiSortedNews() {
        val now = System.currentTimeMillis()
        val twoHoursPassed = (now - _lastAiUpdate) >= (2 * 60 * 60 * 1000)

        // Simple day check (mirrors JS logic)
        val lastDate = java.util.Date(_lastAiUpdate).toString().substring(0, 10)
        val nowDate = java.util.Date(now).toString().substring(0, 10)
        val isNewDay = lastDate != nowDate

        if (_aiDiscoveryArticles.value.isEmpty() || isNewDay || twoHoursPassed) {
            viewModelScope.launch {
                _isLoading.value = true
                val discovered = aiService.discoverAISortedNews()
                if (discovered.isNotEmpty()) {
                    if (isNewDay || _aiDiscoveryArticles.value.isEmpty()) {
                        _aiDiscoveryArticles.value = discovered
                    } else {
                        val existingIds = _aiDiscoveryArticles.value.map { it.id }.toSet()
                        val newOnes = discovered.filter { it.id !in existingIds }
                        _aiDiscoveryArticles.value = _aiDiscoveryArticles.value + newOnes
                    }
                    _lastAiUpdate = now
                }
                _isLoading.value = false
            }
        }
    }

    fun searchNews(query: String) {
        _searchTerm.value = query
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        val term = query.lowercase().trim()

        // Hybrid search logic: First check internal articles
        val internalMatches = _articles.value.filter { article ->
            val titleSim = StringUtils.getSimilarity(term, article.title)
            val tagMatch = article.intelligence?.asset_tags?.any { it.lowercase() == term.replace(" ", "_") } ?: false
            val contentMatch = article.summary.lowercase().contains(term) || article.title.lowercase().contains(term)
            titleSim >= 0.85 || tagMatch || contentMatch
        }

        if (internalMatches.size >= 3) {
            _searchResults.value = internalMatches
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                val externalResults = newsService.searchExternalNews(query)
                // Combine and remove duplicates
                _searchResults.value = (internalMatches + externalResults).distinctBy { it.id }
                _isLoading.value = false
            }
        }
    }

    fun getExplanation(article: NewsArticle) {
        _aiExplanation.value = null
        _fullContent.value = null
        viewModelScope.launch {
            // Replicate JS behavior: Fetch explanation AND full content
            launch {
                _aiExplanation.value = aiService.getArticleExplanation(article.title, article.summary)
            }
            launch {
                val scraped = newsService.fetchFullArticleContent(article.id)
                if (scraped.isNotEmpty()) {
                    _fullContent.value = scraped
                }
            }
        }
    }

    fun toggleBookmark(article: NewsArticle, savedContent: String? = null) {
        val current = _bookmarks.value.toMutableList()
        val index = current.indexOfFirst { it.id == article.id }

        if (index != -1) {
            val existing = current[index]
            if (savedContent != null && existing.savedContent == null) {
                // Update with content if it was missing
                current[index] = existing.copy(savedContent = savedContent)
            } else {
                // Normal toggle off
                current.removeAt(index)
            }
        } else {
            // Add new bookmark
            val articleToSave = if (savedContent != null) article.copy(savedContent = savedContent) else article
            current.add(articleToSave)
        }
        _bookmarks.value = current
    }
}
