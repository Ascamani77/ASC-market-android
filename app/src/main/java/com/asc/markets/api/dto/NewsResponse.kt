package com.asc.markets.api.dto

import com.asc.markets.data.NewsStory

data class NewsResponse(
    val data: List<NewsStory> = emptyList(),
    val news: List<NewsStory> = emptyList(),
    val articles: List<NewsStory> = emptyList()
) {
    fun getNewsList(): List<NewsStory> {
        return when {
            data.isNotEmpty() -> data
            news.isNotEmpty() -> news
            articles.isNotEmpty() -> articles
            else -> emptyList()
        }
    }
}
