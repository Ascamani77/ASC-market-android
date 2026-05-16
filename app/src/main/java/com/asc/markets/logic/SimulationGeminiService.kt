package com.asc.markets.logic

import com.asc.markets.data.SimulationSignal
import com.asc.markets.data.AISimulationStrategy
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.asc.markets.BuildConfig

object SimulationGeminiService {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun generateEventAnalysis(eventTitle: String, actual: String, forecast: String, previous: String, importance: String): JSONObject = withContext(Dispatchers.IO) {
        val prompt = """
            Analyze the following macroeconomic event:
            Title: $eventTitle
            Actual: $actual
            Forecast: $forecast
            Previous: $previous
            Importance: $importance
            
            Provide an analysis and severity in JSON format.
            Include:
            - "bias": 'long', 'short', or 'neutral'
            - "posture": 'aggressive', 'defensive', or 'balanced'
            - "confidence": an integer between 0 and 100
            - "narrative_summary": a short sentence explaining the impact (e.g., "Inflation slows faster than expected.")
            - "severity": 'critical', 'high', or 'normal'
            - "assets": array of 2 related assets (e.g. ["USD", "UST-10Y"] or ["EUR", "GER-30"])
            - "asset_class": 'macro', 'forex', 'stock', or 'commodity'
            
            Example format:
            {
              "bias": "short",
              "posture": "defensive",
              "confidence": 58,
              "narrative_summary": "Central bank emergency meeting scheduled.",
              "severity": "critical",
              "assets": ["USD", "UST-10Y"],
              "asset_class": "macro"
            }
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val text = response.text ?: throw Exception("Empty response")
            JSONObject(text.replace("```json", "").replace("```", "").trim())
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject().apply {
                put("bias", "neutral")
                put("posture", "balanced")
                put("confidence", 50)
                put("narrative_summary", "Awaiting AI analysis due to API error.")
                put("severity", "normal")
                put("assets", org.json.JSONArray(listOf("USD", "GOLD")))
                put("asset_class", "macro")
            }
        }
    }

    private val ASSETS = arrayOf("BTC/USD", "ETH/USD", "EUR/USD", "GBP/USD", "GOLD", "OIL")

    suspend fun generateTradeSignal(
        lookbackCandles: Int = 100,
        enabledStrategies: Set<AISimulationStrategy> = AISimulationStrategy.defaultSelection()
    ): SimulationSignal = withContext(Dispatchers.IO) {
        val asset = ASSETS.random()
        val strategyList = enabledStrategies.ifEmpty { AISimulationStrategy.defaultSelection() }
        val strategyText = strategyList.joinToString(", ") { "${it.label} (${it.description})" }
        
        val prompt = """
            Analyze the market for $asset considering the last $lookbackCandles historical candles. 
            You are only allowed to use these selected auto-trading strategies: $strategyText.
            Ignore any entry idea that is not confirmed by at least one selected strategy. If more than one selected strategy is present, combine them as confluence.
            Provide a simulated trade signal in JSON format based on this historical context and selected strategy filter.
            Include: asset, type ('buy' or 'sell'), entry (current price), sl (stop loss), tp (take profit), risk (percentage), confidence (0-1), and a brief reasoning that names the selected strategies used.
            Make the prices realistic for $asset.
            Example format:
            {
              "asset": "BTC/USD",
              "type": "buy",
              "entry": 65432.10,
              "sl": 64000.00,
              "tp": 68000.00,
              "risk": 2.5,
              "confidence": 0.85,
              "reasoning": "Strong support at 64k with bullish divergence on RSI observed in the lookback period."
            }
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val text = response.text ?: throw Exception("Empty response")
            val json = JSONObject(text)
            
            SimulationSignal(
                asset = json.getString("asset"),
                type = json.getString("type"),
                entry = json.getDouble("entry"),
                sl = json.getDouble("sl"),
                tp = json.getDouble("tp"),
                risk = json.getDouble("risk"),
                confidence = json.getDouble("confidence"),
                reasoning = json.getString("reasoning")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val fallbackStrategies = strategyList.joinToString(" + ") { it.label }
            SimulationSignal(
                asset = asset,
                type = if (Math.random() > 0.5) "buy" else "sell",
                entry = 100.0,
                sl = 95.0,
                tp = 110.0,
                risk = 1.0,
                confidence = 0.5,
                reasoning = "Fallback signal constrained to selected strategies: $fallbackStrategies. API error: ${e.message}"
            )
        }
    }
}