package com.asc.markets.data.repository

import com.asc.markets.data.SystemTelemetry
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.data.remote.ChartDisplaySettingsRequest
import com.asc.markets.data.remote.ChartDisplaySettingsResponse
import com.asc.markets.data.remote.LatestDeploymentsResponse
import com.asc.markets.data.remote.RunAiRequest
import com.asc.markets.data.remote.RunAiResponse
import com.asc.markets.data.remote.ScalpingSignalsResponse
import com.asc.markets.data.remote.SimulationStatusResponse
import com.asc.markets.data.remote.TradeSimulationRequest
import com.asc.markets.data.remote.TradeSimulationResponse
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

    suspend fun simulateTrade(request: TradeSimulationRequest): Result<TradeSimulationResponse> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.simulateTrade(request)
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun getSimulationStatus(): Result<SimulationStatusResponse> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.getSimulationStatus()
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun saveChartDisplaySettings(settings: Map<String, Boolean>): Result<ChartDisplaySettingsResponse> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.saveChartDisplaySettings(ChartDisplaySettingsRequest(settings))
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun getScalpingSignals(): Result<ScalpingSignalsResponse> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.getScalpingSignals()
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.success(response)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            SystemTelemetry.recordTick("ASC_AI", latency.toDouble().coerceAtLeast(1.0))
            Result.failure(e)
        }
    }

    suspend fun getSwingSignals(): Result<ScalpingSignalsResponse> {
        val start = System.currentTimeMillis()
        return try {
            val response = AiRetrofitClient.api.getSwingSignals()
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
