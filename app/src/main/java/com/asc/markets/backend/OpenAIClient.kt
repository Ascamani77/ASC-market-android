package com.asc.markets.backend

import com.asc.markets.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenAI Chat Completions client.
 *
 * NOTE: this client reads the API key from `BuildConfig.OPENAI_API_KEY` which is populated
 * from your Gradle/project properties (see `app/build.gradle.kts`). Do NOT commit API keys
 * into source control. Put the key in `local.properties` as `OPENAI_API_KEY=sk-...` or supply
 * it via your CI secrets.
 */
object OpenAIClient {
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE = "https://api.openai.com/v1"

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val max_tokens: Int = 512
    )

    @Serializable
    private data class Choice(@SerialName("message") val message: MessageResponse?)

    @Serializable
    private data class MessageResponse(val role: String? = null, val content: String? = null)

    @Serializable
    private data class ChatResponse(val id: String? = null, val choices: List<Choice> = emptyList())

    suspend fun chatCompletion(prompt: String, model: String = "gpt-4o-mini"): String = withContext(Dispatchers.IO) {
        // Only use the build-time injected key. Do NOT accept runtime/pasted API keys.
        val key = BuildConfig.OPENAI_API_KEY
        require(key.isNotBlank()) { "OPENAI_API_KEY not set in BuildConfig. Add it to app/local.properties as OPENAI_API_KEY=sk-... and rebuild the app." }

        val req = ChatRequest(model = model, messages = listOf(Message(role = "user", content = prompt)))
        val url = URL("$BASE/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
            connectTimeout = 30_000
            readTimeout = 30_000
        }

        val body = json.encodeToString(req)
        BufferedOutputStream(conn.outputStream).use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val reader = if (code in 200..299) BufferedReader(InputStreamReader(conn.inputStream)) else BufferedReader(InputStreamReader(conn.errorStream))
        val respText = reader.use { it.readText() }

        if (code !in 200..299) throw IllegalStateException("OpenAI API error ($code): $respText")

        val parsed = json.parseToJsonElement(respText).jsonObject
        // try to extract choices[0].message.content or fallback to raw text
        val choices = parsed["choices"]
        val content = choices?.let {
            try {
                val chatResp = json.decodeFromString(ChatResponse.serializer(), respText)
                chatResp.choices.firstOrNull()?.message?.content
            } catch (_: Throwable) { null }
        }

        return@withContext (content ?: respText)
    }

    /**
     * Helper to let callers quickly check whether an API key is available at runtime.
     */
    fun isKeyConfigured(): Boolean {
        // Only consider the build-time injected key as valid. This prevents accepting pasted/runtime keys.
        val buildKey = BuildConfig.OPENAI_API_KEY
        return buildKey.isNotBlank()
    }
}
