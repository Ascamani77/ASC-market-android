package com.asc.markets.api

import com.asc.markets.data.*
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
            timestamp_utc = obj.getLong("timestamp_utc"),
            asset_class = AssetClass.valueOf(obj.getString("asset_class").lowercase()),
            assets_affected = parseStringArray(obj.getJSONArray("assets_affected")),
            narrative_summary = obj.getString("narrative_summary"),
            persistence_count = obj.getInt("persistence_count"),
            source = obj.getString("source"),
            source_type = SourceType.valueOf(obj.optString("source_type", "real").lowercase()),
            event_type = IntelligenceEventType.valueOf(obj.getString("event_type").lowercase()),
            execution_regime = RegimeState.valueOf(obj.optString("execution_regime", "REGIME_NEUTRAL").uppercase()),
            visual_state = VisualState.valueOf(obj.optString("visual_state", "NEUTRAL").uppercase()),
            transition_status = TransitionStatus.valueOf(obj.optString("transition_status", "active").lowercase()),
            volatility_confirmed = obj.optBoolean("volatility_confirmed", false),
            severity = IntelligenceSeverity.valueOf(obj.optString("severity", "normal").lowercase()),
            safety_gate = obj.optBoolean("safety_gate", false),
            unlock_state = IntelligenceUnlockState.valueOf(obj.getString("unlock_state").uppercase()),
            gate_release_time = obj.optLong("gate_release_time", 0),
            hard_unlock_time = obj.optLong("hard_unlock_time", 0),
            ebc = obj.optJSONObject("ebc")?.let { ebcObj ->
                IntelligenceExecutionBoundaryContract(
                    status = IntelligenceEBCStatus.valueOf(ebcObj.getString("status").uppercase()),
                    violations = parseStringArray(ebcObj.optJSONArray("violations"))
                )
            },
            strategy_context = obj.optJSONObject("strategy_context")?.let { scObj ->
                StrategyEligibility(
                    bias = scObj.getString("bias"),
                    risk_posture = scObj.getString("risk_posture"),
                    rationale = scObj.getString("rationale"),
                    `class` = scObj.optString("class", "macro")
                )
            },
            correlation_heat = obj.optInt("correlation_heat", 50),
            liquidity_depth = obj.optInt("liquidity_depth", 50),
            transition_triggers = parseTriggers(obj.optJSONArray("transition_triggers"))
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

    private fun parseTriggers(array: JSONArray?): List<IntelligenceTransitionTrigger> {
        if (array == null) return emptyList()
        val list = mutableListOf<IntelligenceTransitionTrigger>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                IntelligenceTransitionTrigger(
                    label = obj.getString("label"),
                    status = obj.getString("status")
                )
            )
        }
        return list
    }
}
