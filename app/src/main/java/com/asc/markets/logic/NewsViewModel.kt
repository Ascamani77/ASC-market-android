package com.asc.markets.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.NewsStory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class NewsViewModel : ViewModel() {
    private val repository = NewsRepository()

    private val _news = MutableStateFlow<List<NewsStory>>(emptyList())
    val news: StateFlow<List<NewsStory>> = _news

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.fetchNews()
                .onSuccess { news ->
                    _news.value = news
                    _error.value = null
                    Log.d("NewsViewModel", "Successfully loaded ${news.size} news items")
                }
                .onFailure { exception ->
                    val errorMsg = "${exception.javaClass.simpleName}: ${exception.message ?: "Unknown error"}"
                    _error.value = errorMsg
                    Log.e("NewsViewModel", "Failed to load news: $errorMsg", exception)
                }
            _isLoading.value = false
        }
    }
}
