package com.asc.markets.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.asc.markets.data.NetworkConfig

object ApiClient {
    // Use the same configurable backend URL as the rest of the app
    private val BASE_URL: String = NetworkConfig.DEFAULT_BACKEND_URL.removeSuffix("/") + "/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val calendarApi: CalendarApi = retrofit.create(CalendarApi::class.java)
    val aiApi: com.asc.markets.data.remote.AiApiService = retrofit.create(com.asc.markets.data.remote.AiApiService::class.java)
}
