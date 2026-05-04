package com.asc.markets.data.repository

import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.data.remote.RunAiRequest
import com.asc.markets.data.remote.RunAiResponse

import com.asc.markets.data.remote.LatestDeploymentsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiRepository {

    private val _deployments = MutableStateFlow<LatestDeploymentsResponse?>(null)
    val deployments: StateFlow<LatestDeploymentsResponse?> = _deployments.asStateFlow()

    suspend fun runAiPipeline(): Result<RunAiResponse> {
        return try {
            val response = AiRetrofitClient.api.runAi(RunAiRequest(mode = "full"))
            if (response.success) {
                fetchLatestDeployments()
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestDeployments(): Result<LatestDeploymentsResponse> {
        return try {
            val response = AiRetrofitClient.api.getLatestDeployments()
            _deployments.value = response
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun healthCheck(): Result<Map<String, Any>> {
        return try {
            val response = AiRetrofitClient.api.healthCheck()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMarketData(request: com.asc.markets.data.remote.MarketUpdateRequest): Result<Map<String, Any>> {
        return try {
            val response = AiRetrofitClient.api.updateMarket(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
