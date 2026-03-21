package com.intelligence.dashboard.service

import com.intelligence.dashboard.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object IntelligenceService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://ais-dev-tkwrivsdwrm6fjfbr2g3ta-466295561767.europe-west2.run.app"

    suspend fun fetchEvents(): List<IntelligenceEvent> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/api/events")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Network Error: ${response.code}")

            val body = response.body?.string() ?: throw Exception("Empty Response")
            parseEvents(body)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun parseEvents(json: String): List<IntelligenceEvent> {
        val events = mutableListOf<IntelligenceEvent>()
        val array = JSONArray(json)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            events.add(parseEvent(obj))
        }

        return events
    }

    private fun parseEvent(obj: JSONObject): IntelligenceEvent {
        return IntelligenceEvent(
            id = obj.getString("id"),
            title = obj.getString("title"),
            timestampUtc = obj.getLong("timestamp_utc"),
            assetClass = AssetClass.valueOf(obj.getString("asset_class").uppercase()),
            assetsAffected = parseStringArray(obj.getJSONArray("assets_affected")),
            narrativeSummary = obj.getString("narrative_summary"),
            persistenceCount = obj.getInt("persistence_count"),
            source = obj.getString("source"),
            unlockState = UnlockState.valueOf(obj.getString("unlock_state").uppercase()),
            gateReleaseTime = obj.optLong("gate_release_time", 0),
            hardUnlockTime = obj.optLong("hard_unlock_time", 0),
            ebc = obj.optJSONObject("ebc")?.let { ebcObj ->
                ExecutionBoundaryContract(
                    status = EBCStatus.valueOf(ebcObj.getString("status").uppercase()),
                    violations = parseStringArray(ebcObj.optJSONArray("violations"))
                )
            },
            strategyContext = obj.optJSONObject("strategy_context")?.let { scObj ->
                StrategyEligibility(
                    bias = scObj.getString("bias"),
                    riskPosture = scObj.getString("risk_posture"),
                    rationale = scObj.getString("rationale"),
                    transitionTriggers = parseTriggers(scObj.optJSONArray("transition_triggers"))
                )
            },
            correlationHeat = obj.optInt("correlation_heat", 50),
            liquidityDepth = obj.optInt("liquidity_depth", 50)
        )
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun parseTriggers(array: JSONArray?): List<TransitionTrigger> {
        if (array == null) return emptyList()
        val list = mutableListOf<TransitionTrigger>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TransitionTrigger(
                    condition = obj.getString("condition"),
                    status = obj.getString("status")
                )
            )
        }
        return list
    }
}
