package com.researchcenter.services

import android.util.Log
import android.util.Xml
import com.asc.markets.BuildConfig
import com.researchcenter.data.models.Intelligence
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.util.Constants
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class NewsService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchAllNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val apiArticlesDeferred = async {
            try {
                val url = URL("${Constants.BASE_URL}/api/news?t=$timestamp")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("x-api-key", BuildConfig.GEMINI_API_KEY)
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = json.parseToJsonElement(response).jsonArray
                    jsonArray.map { element -> parseArticle(element.jsonObject) }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<NewsArticle>()
            }
        }

        val rssArticlesDeferred = async { fetchAllRssFeeds() }
        val apiArticles = apiArticlesDeferred.await()
        val rssArticles = rssArticlesDeferred.await()
        
        (apiArticles + rssArticles)
            .sortedByDescending { it.publishedAt }
            .take(150) // Limit total to 150 as requested
    }

    suspend fun searchExternalNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${Constants.BASE_URL}/api/news/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", BuildConfig.GEMINI_API_KEY)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = json.parseToJsonElement(response).jsonArray
                jsonArray.map { element -> parseArticle(element.jsonObject) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchFullArticleContent(articleId: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("${Constants.BASE_URL}/api/news/content/$articleId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", BuildConfig.GEMINI_API_KEY)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val obj = json.parseToJsonElement(response).jsonObject
                obj["content"]?.jsonPrimitive?.content ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun normalizeDate(rawDate: String): String {
        if (rawDate.isEmpty()) return OffsetDateTime.now().toString()
        return try {
            // Try ISO first
            OffsetDateTime.parse(rawDate).toString()
        } catch (e: Exception) {
            try {
                // Try RFC 1123 (Common RSS format)
                val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
                OffsetDateTime.parse(rawDate, formatter).toString()
            } catch (e2: Exception) {
                // Return current time as fallback to keep it at top
                OffsetDateTime.now().toString()
            }
        }
    }

    suspend fun fetchAllRssFeeds(): List<NewsArticle> = coroutineScope {
        Constants.RSS_FEEDS.flatMap { (category, urls) ->
            urls.take(10).map { urlString -> // Increased from 3 to 10 sources per category
                async(Dispatchers.IO) {
                    try {
                        fetchSingleRss(urlString, category)
                    } catch (e: Exception) { emptyList<NewsArticle>() }
                }
            }
        }.awaitAll().flatten()
    }

    private fun fetchSingleRss(urlString: String, category: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        
        val parser = Xml.newPullParser()
        parser.setInput(connection.inputStream, null)

        var eventType = parser.eventType
        var currentTitle = ""
        var currentLink = ""
        var currentPubDate = ""
        var currentDescription = ""
        var currentImageUrl: String? = null
        var inItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "item" || name == "entry") inItem = true
                    else if (inItem) {
                        when (name) {
                            "title" -> currentTitle = try { parser.nextText() } catch(e: Exception) { "" }
                            "link" -> currentLink = try { parser.nextText() ?: parser.getAttributeValue(null, "href") ?: "" } catch(e: Exception) { "" }
                            "pubDate", "published", "updated" -> currentPubDate = try { parser.nextText() } catch(e: Exception) { "" }
                            "description", "summary" -> currentDescription = try { parser.nextText() } catch(e: Exception) { "" }
                            "enclosure" -> currentImageUrl = parser.getAttributeValue(null, "url")
                            "media:content" -> currentImageUrl = parser.getAttributeValue(null, "url")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name == "item" || name == "entry") {
                        articles.add(NewsArticle(
                            id = UUID.nameUUIDFromBytes(currentLink.toByteArray()).toString(),
                            title = currentTitle,
                            url = currentLink,
                            publishedAt = normalizeDate(currentPubDate),
                            source = URL(urlString).host,
                            author = "",
                            category = category,
                            summary = currentDescription.replace(Regex("<[^>]*>"), "").take(200),
                            content = currentDescription,
                            imageUrl = currentImageUrl,
                            intelligence = Intelligence("neutral", "low", emptyList(), 0.0)
                        ))
                        inItem = false
                        
                        // Limit to 10 articles per source to maintain variety and speed
                        if (articles.size >= 10) return articles
                    }
                }
            }
            eventType = parser.next()
        }
        return articles
    }

    private fun parseArticle(obj: kotlinx.serialization.json.JsonObject): NewsArticle {
        val intel = obj["intelligence"]?.jsonObject
        return NewsArticle(
            id = obj["id"]?.jsonPrimitive?.content ?: "",
            title = obj["title"]?.jsonPrimitive?.content ?: "",
            source = obj["source"]?.jsonPrimitive?.content ?: "",
            author = obj["author"]?.jsonPrimitive?.content ?: "",
            publishedAt = normalizeDate(obj["publishedAt"]?.jsonPrimitive?.content ?: ""),
            category = obj["category"]?.jsonPrimitive?.content ?: "",
            summary = obj["summary"]?.jsonPrimitive?.content ?: "",
            content = obj["content"]?.jsonPrimitive?.content ?: "",
            url = obj["url"]?.jsonPrimitive?.content ?: "",
            imageUrl = obj["imageUrl"]?.jsonPrimitive?.content,
            intelligence = Intelligence(
                sentiment = intel?.get("sentiment")?.jsonPrimitive?.content ?: "neutral",
                confidence = intel?.get("confidence")?.jsonPrimitive?.content ?: "low",
                asset_tags = intel?.get("asset_tags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                impact_score = intel?.get("impact_score")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            )
        )
    }
}
