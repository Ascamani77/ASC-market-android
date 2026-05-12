package com.asc.markets.data.repository

import com.asc.markets.data.SystemTelemetry
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
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.runAi(RunAiRequest(mode = "full"))
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            if (response.success) {
                fetchLatestDeployments()
            }
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun fetchLatestDeployments(): Result<LatestDeploymentsResponse> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.getLatestDeployments()
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            _deployments.value = response
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun healthCheck(): Result<Map<String, Any>> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.healthCheck()
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun updateMarketData(request: com.asc.markets.data.remote.MarketUpdateRequest): Result<Map<String, Any>> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.updateMarket(request)
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }
}
