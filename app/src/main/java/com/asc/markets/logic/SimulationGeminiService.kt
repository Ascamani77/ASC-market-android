package com.asc.markets.logic

import com.asc.markets.data.SimulationSignal
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

    private val ASSETS = arrayOf("BTC/USD", "ETH/USD", "EUR/USD", "GBP/USD", "GOLD", "OIL")

    suspend fun generateTradeSignal(lookbackCandles: Int = 100): SimulationSignal = withContext(Dispatchers.IO) {
        val asset = ASSETS.random()
        
        val prompt = """
            Analyze the market for $asset considering the last $lookbackCandles historical candles. 
            Provide a simulated trade signal in JSON format based on this historical context.
            Include: asset, type ('buy' or 'sell'), entry (current price), sl (stop loss), tp (take profit), risk (percentage), confidence (0-1), and a brief reasoning that references the $lookbackCandles candles analysis.
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
            // Fallback mock signal
            SimulationSignal(
                asset = asset,
                type = if (Math.random() > 0.5) "buy" else "sell",
                entry = 100.0,
                sl = 95.0,
                tp = 110.0,
                risk = 1.0,
                confidence = 0.5,
                reasoning = "Fallback signal due to API error: ${e.message}"
            )
        }
    }
}
