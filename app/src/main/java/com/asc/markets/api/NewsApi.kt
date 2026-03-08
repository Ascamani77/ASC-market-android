package com.asc.markets.api

import com.asc.markets.api.dto.NewsResponse
import retrofit2.http.GET

interface NewsApi {
    @GET("/api/news")
    suspend fun getNews(): NewsResponse
}
