package com.asc.markets.data.remote

import android.content.Context
import com.asc.markets.data.NetworkConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AiRetrofitClient {

    // Replace 10.95.77.133 with your laptop's actual Wi-Fi IP address
    // Do NOT use 127.0.0.1 or localhost - those point to the phone/emulator, not your PC
    private val DEFAULT_BASE_URL = "${NetworkConfig.DEFAULT_BACKEND_URL}/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var configuredBaseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedApi: AiApiService? = null

    fun configure(context: Context) {
        configure(NetworkConfig.backendUrl(context))
    }

    fun configure(baseUrl: String) {
        configuredBaseUrl = normalizeBaseUrl(baseUrl)
    }

    val api: AiApiService
        get() {
            val baseUrl = configuredBaseUrl
            val existing = cachedApi
            if (existing != null && cachedBaseUrl == baseUrl) {
                return existing
            }

            return synchronized(this) {
                val current = cachedApi
                if (current != null && cachedBaseUrl == baseUrl) {
                    current
                } else {
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AiApiService::class.java)
                        .also {
                            cachedApi = it
                            cachedBaseUrl = baseUrl
                        }
                }
            }
        }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val normalized = NetworkConfig.normalizedBackendUrl(baseUrl)
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }
}
