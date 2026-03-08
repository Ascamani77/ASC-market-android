package com.asc.markets.ui.screens.tradeDashboard.model

import java.util.UUID

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val eventTime: Long? = null,
    val category: String,
    val intelligence: ArticleIntelligence,
    val isBookmarked: Boolean = false,
    val savedContent: String? = null,
    val impact: String = "LOW", // LOW, MEDIUM, HIGH
    val previous: String? = null,
    val consensus: String? = null,
    val actual: String? = null,
    val imageUrl: String? = null,
    val hasRedDot: Boolean = false,
    val status: String? = null // IMMINENT, PASSED, etc.
)

data class ArticleIntelligence(
    val sentiment: Sentiment,
    val confidence: Confidence,
    val assetTags: List<String>,
    val impactScore: Double
)

enum class Sentiment {
    POSITIVE, NEUTRAL, NEGATIVE, CRITICAL
}

enum class Confidence {
    HIGH, LOW
}

data class NewsCategory(
    val id: String,
    val name: String,
    val icon: String, // emoji or resource
    val count: Int = 0
)
