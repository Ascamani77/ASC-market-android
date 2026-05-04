package com.asc.markets.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AiApiService {

    @GET("health")
    suspend fun healthCheck(): Map<String, Any>

    @POST("run-ai")
    suspend fun runAi(@Body request: RunAiRequest): RunAiResponse

    @POST("update-market")
    suspend fun updateMarket(@Body request: MarketUpdateRequest): Map<String, Any>

    @GET("latest-deployments")
    suspend fun getLatestDeployments(): LatestDeploymentsResponse
}
