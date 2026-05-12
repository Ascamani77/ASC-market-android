package com.trading.app.data

import com.trading.app.models.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton store for MT5 FXStreet news items.
 * Allows TradingApp to store news and NewsViewModel to consume it.
 */
object Mt5NewsStore {
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()
    
    fun updateNews(items: List<NewsItem>) {
        android.util.Log.i("Mt5NewsStore", "=== UPDATING NEWS STORE ===")
        android.util.Log.i("Mt5NewsStore", "Received ${items.size} news items")
        items.take(3).forEach { item ->
            android.util.Log.i("Mt5NewsStore", "  - ${item.title}")
        }
        _newsItems.value = items
        android.util.Log.i("Mt5NewsStore", "Store updated, current size: ${_newsItems.value.size}")
    }
    
    fun clear() {
        _newsItems.value = emptyList()
    }
}
