package com.asc.markets.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.asc.markets.data.NetworkConfig

interface AiApi {
    @GET("latest-deployments")
    suspend fun getLatestDeployments(): LatestDeploymentsResponse

    @GET("signals")
    suspend fun getHybridSignals(): HybridSignalsResponse

    @POST("run-ai")
    suspend fun runAi(@Body request: RunAiRequest): RunAiResponse

    @GET("scalping-signals")
    suspend fun getScalpingSignals(): ScalpingSignalsResponse

    @GET("swing-signals")
    suspend fun getSwingSignals(): ScalpingSignalsResponse
    
    @POST("api/simulate-trade")
    suspend fun simulateTrade(@Body request: TradeSimulationRequest): TradeSimulationResponse
    
    @GET("api/simulation-status")
    suspend fun getSimulationStatus(): SimulationStatusResponse
    
    @POST("api/chart-display-settings")
    suspend fun saveChartDisplaySettings(@Body request: ChartDisplaySettingsRequest): ChartDisplaySettingsResponse
    
    @GET("api/chart-display-settings")
    suspend fun getChartDisplaySettings(): ChartDisplaySettingsResponse
}

object AiRetrofitClient {
    private var baseUrl = "http://${NetworkConfig.DEFAULT_HOST}:5000/"

    private var _api: AiApi? = null
    val api: AiApi
        get() = _api ?: synchronized(this) {
            _api ?: buildApi(baseUrl).also { _api = it }
        }

    fun configure(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        _api = buildApi(baseUrl)
    }

    private fun buildApi(url: String): AiApi {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApi::class.java)
    }
}
