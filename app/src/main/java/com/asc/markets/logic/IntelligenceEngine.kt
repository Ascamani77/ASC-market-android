package com.asc.markets.logic

import com.asc.markets.data.MarketSignal
import kotlin.math.roundToInt

data class AnalystPersona(
    val id: String,
    val name: String,
    val desc: String,
    val instruction: String,
    val icon: String = "⊙"
)

val ANALYST_MODELS = listOf(
    AnalystPersona("macro", "Macro Pulse", "Regime & Context", "Monitor scheduled macroeconomic releases (rates, NFP, CPI) and policy communications.", "⊙"),
    AnalystPersona("smc", "SMC Core", "Directional Structure", "Analyze BOS and CHoCH to identify shifts in directional control.", "⚡"),
    AnalystPersona("liquidity", "Liquidity Scan", "Execution Timing", "Examine wick behavior and volume anomalies.", "⏳"),
    AnalystPersona("algo", "Algo Quant", "Expectancy Filter", "Evaluate probabilistic edges using distributional behavior.", "⚙"),
    AnalystPersona("sentiment", "Sentiment Hub", "Risk Modifier", "Evaluate crowding risk and contrarian positioning.", "👤"),
    AnalystPersona("prop", "Prop Guard", "FINAL VETO", "Enforce capital protection rules. FINAL VETO authority.", "🛡")
)

data class IntelligenceReport(
    val totalConfidence: Int,
    val regime: String,
    val technicalWeight: Int,
    val fundamentalWeight: Int,
    val confluences: List<Pair<String, Boolean>>,
    val sentimentLabel: String
)

object IntelligenceEngine {
    fun generateFullAudit(pair: String, signal: MarketSignal): IntelligenceReport {
        val status = CalendarService.getTradingStatus(pair)
        val technicalScore = signal.confidenceScore
        val fundamentalScore = if (status.isBlocked) 0 else 100
        
        val totalConfidence = ((technicalScore * 0.6) + (fundamentalScore * 0.4)).roundToInt()
        
        return IntelligenceReport(
            totalConfidence = totalConfidence,
            regime = if (signal.direction == "BUY") "EXPANSION" else "RETRACEMENT",
            technicalWeight = 60,
            fundamentalWeight = 40,
            sentimentLabel = when {
                totalConfidence > 75 -> "Strong Bullish"
                totalConfidence > 50 -> "Cautious Bullish"
                else -> "Neutral"
            },
            confluences = listOf(
                "Structure Alignment" to true,
                "Liquidity Sweep" to true,
                "Safety Gate Clear" to !status.isBlocked,
                "Volume Delta Sync" to (technicalScore > 80)
            )
        )
    }
}