package com.researchcenter.data.models

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticle(
    val id: String,
    val url: String,
    val title: String,
    val publishedAt: String,
    val source: String,
    val author: String,
    val category: String,
    val summary: String,
    val content: String,
    val imageUrl: String? = null,
    val savedContent: String? = null,
    val intelligence: Intelligence? = null,
    val status: String? = "active"
)

@Serializable
data class Intelligence(
    val sentiment: String, // 'positive' | 'neutral' | 'negative' | 'critical'
    val confidence: String, // 'high' | 'low'
    val asset_tags: List<String>,
    val impact_score: Double,
    val market_type: String? = "Macro"
)

@Serializable
data class NewsCategory(
    val id: String,
    val name: String,
    val icon: String
)

enum class ViewMode {
    LIST,
    ARTICLE
}
