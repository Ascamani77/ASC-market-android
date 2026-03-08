package com.asc.markets.logic

import com.asc.markets.api.NewsApiClient
import com.asc.markets.data.NewsStory
import android.util.Log

class NewsRepository {
    suspend fun fetchNews(): Result<List<NewsStory>> = try {
        val response = NewsApiClient.newsApi.getNews()
        Log.d("NewsRepository", "Raw response: $response")
        val news = response.getNewsList()
        Log.d("NewsRepository", "Parsed news: $news")
        Log.d("NewsRepository", "Fetched ${news.size} news items")
        Result.success(news)
    } catch (e: Exception) {
        val errorMsg = "${e.javaClass.simpleName}: ${e.message}"
        Log.e("NewsRepository", "Error fetching news: $errorMsg", e)
        e.printStackTrace()
        Result.failure(e)
    }
}
