package com.researchcenter.services

import com.asc.markets.BuildConfig
import com.researchcenter.data.models.Intelligence
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

class AiService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getArticleExplanation(articleTitle: String, articleContent: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("${Constants.BASE_URL}/api/explain")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", BuildConfig.GEMINI_API_KEY)

            val body = Json.encodeToString(mapOf("title" to articleTitle, "content" to articleContent))
            connection.outputStream.write(body.toByteArray())

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val obj = json.parseToJsonElement(response).jsonObject
                obj["explanation"]?.jsonPrimitive?.content ?: "Error: Intelligence layer connection failed."
            } else {
                "Error: Intelligence layer returned code ${connection.responseCode}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: Intelligence layer connection failed."
        }
    }

    suspend fun discoverAISortedNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${Constants.BASE_URL}/api/news/ai-sorted")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", BuildConfig.GEMINI_API_KEY)
            connection.setRequestProperty("Content-Type", "application/json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = json.parseToJsonElement(response).jsonArray

                jsonArray.map { element ->
                    val obj = element.jsonObject
                    val intel = obj["intelligence"]?.jsonObject

                    NewsArticle(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        title = obj["title"]?.jsonPrimitive?.content ?: "",
                        source = obj["source"]?.jsonPrimitive?.content ?: "",
                        author = obj["author"]?.jsonPrimitive?.content ?: "",
                        publishedAt = obj["publishedAt"]?.jsonPrimitive?.content ?: "",
                        category = obj["category"]?.jsonPrimitive?.content ?: "",
                        summary = obj["summary"]?.jsonPrimitive?.content ?: "",
                        content = obj["content"]?.jsonPrimitive?.content ?: "",
                        url = obj["url"]?.jsonPrimitive?.content ?: "",
                        intelligence = Intelligence(
                            sentiment = intel?.get("sentiment")?.jsonPrimitive?.content ?: "neutral",
                            confidence = intel?.get("confidence")?.jsonPrimitive?.content ?: "low",
                            asset_tags = intel?.get("asset_tags")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                            impact_score = intel?.get("impact_score")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
