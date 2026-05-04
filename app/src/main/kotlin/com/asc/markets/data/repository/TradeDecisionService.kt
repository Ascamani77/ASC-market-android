package com.asc.markets.data.repository

import android.util.Log
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.data.remote.RunAiRequest
import com.asc.markets.data.remote.FinalDecisionItem

class TradeDecisionService {

    /**
     * Fetch trades from backend and filter for PRIMARY_DEPLOYMENT only
     */
    suspend fun getPrimaryDeploymentTrades(): Result<List<FinalDecisionItem>> {
        return try {
            val response = AiRetrofitClient.api.runAi(RunAiRequest(mode = "full"))
            
            if (response.success) {
                // Filter for PRIMARY_DEPLOYMENT only
                val primaryTrades = response.final_decision.filter { 
                    it.portfolio_decision_label == "PRIMARY_DEPLOYMENT"
                }
                Result.success(primaryTrades)
            } else {
                Result.failure(Exception("Backend returned success=false: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e("TradeDecisionService", "Error fetching trades", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch ALL decisions (not filtered)
     */
    suspend fun getAllDecisions(): Result<List<FinalDecisionItem>> {
        return try {
            val response = AiRetrofitClient.api.runAi(RunAiRequest(mode = "full"))
            
            if (response.success) {
                Result.success(response.final_decision)
            } else {
                Result.failure(Exception("Backend error: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e("TradeDecisionService", "Error fetching all decisions", e)
            Result.failure(e)
        }
    }
}
