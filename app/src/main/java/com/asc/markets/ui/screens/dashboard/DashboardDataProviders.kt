package com.asc.markets.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

// ===== ANALYSIS BOX DATA MODELS =====
data class GapAnalysis(
    val gapSize: String,
    val gapPercent: String,
    val direction: String  // "UP" or "DOWN"
)

data class VolumeAnalysis(
    val volumeLevel: String,
    val trend: String,  // "INCREASING", "STABLE", "DECREASING"
    val strength: String  // "WEAK", "NORMAL", "STRONG"
)

data class AISentiment(
    val sentimentScore: Int,  // 0-100
    val sentimentState: String,  // "BEARISH", "NEUTRAL", "BULLISH"
    val confidence: Int  // 0-100
)

data class RiskLevel(
    val riskScore: String,  // descriptive: "LOW", "MEDIUM", "HIGH"
    val riskValue: String,  // numeric representation
    val status: String  // "SAFE", "CAUTION", "DANGER"
)

data class NewsCatalyst(
    val catalystName: String,
    val impactLevel: String,  // "LOW", "MEDIUM", "HIGH"
    val timeToEvent: String,  // "15 mins", "1 hour", etc
    val isActive: Boolean
)

data class ProbabilityScore(
    val scoreValue: Int,  // 0-100
    val confidenceLevel: String,  // "LOW", "MODERATE", "HIGH"
    val prediction: String  // "BULLISH", "BEARISH", "NEUTRAL"
)

// ===== SESSION DATA MODEL =====
data class SessionData(
    val hubName: String,
    val isActive: Boolean,
    val completionPercent: Int,
    val avgSpread: String,
    val volatility: String,
    val nextEventTime: String,
    val nextEventLabel: String,
    val safetyGateStatus: String,
    val safetyGateArmed: Boolean,
    val globalRegimeText: String,
    val gapAnalysis: GapAnalysis,
    val volumeAnalysis: VolumeAnalysis,
    val aiSentiment: AISentiment,
    val riskLevel: RiskLevel,
    val newsCatalyst: NewsCatalyst,
    val probabilityScore: ProbabilityScore
)

// ===== TECHNICAL VITALS DATA MODEL =====
data class TechnicalVitalsData(
    val nodeHealth: Double,
    val avgSpread: Double,
    val volatilityPerHour: Double,
    val latencyMs: Double,
    val globalRegime: String,
    val vixValue: Double,
    val dxyChange: Double,
    val macroComment: String,
    val safetyGateClosed: Boolean
)

// ===== SESSION DATA PROVIDER =====
object SessionDataProvider {
    private val _sessionData = MutableStateFlow(
        SessionData(
            hubName = "LONDON HUB",
            isActive = true,
            completionPercent = 65,
            avgSpread = "0.4 pips",
            volatility = "24 p/h",
            nextEventTime = "13:30",
            nextEventLabel = "UTC WINDOW",
            safetyGateStatus = "ARMED",
            safetyGateArmed = true,
            globalRegimeText = "Current environment is defined by MACRO-DRIVEN COMPRESSION. Equities and Forex are displaying high levels of institutional rebalancing ahead of NY open.",
            gapAnalysis = GapAnalysis("45 pips", "0.42%", "UP"),
            volumeAnalysis = VolumeAnalysis("2.3x MA", "INCREASING", "STRONG"),
            aiSentiment = AISentiment(72, "BULLISH", 85),
            riskLevel = RiskLevel("MEDIUM", "2.5%", "CAUTION"),
            newsCatalyst = NewsCatalyst("ECB Decision", "HIGH", "45 mins", true),
            probabilityScore = ProbabilityScore(78, "HIGH", "BULLISH")
        )
    )
    
    val sessionData: StateFlow<SessionData> = _sessionData
    
    fun updateSessionData(data: SessionData) {
        _sessionData.value = data
    }
}

// ===== TECHNICAL VITALS PROVIDER =====
object TechnicalVitalsProvider {
    private val _vitalsData = MutableStateFlow(
        TechnicalVitalsData(
            nodeHealth = 0.98,
            avgSpread = 0.42,
            volatilityPerHour = 24.5,
            latencyMs = 0.32,
            globalRegime = "Risk-Off: USD Strength & Tightening",
            vixValue = 19.8,
            dxyChange = 0.42,
            macroComment = "Institutional liquidity building at key support levels. Monitor NY session open for breakout confirmation.",
            safetyGateClosed = false
        )
    )
    
    val vitalsData: StateFlow<TechnicalVitalsData> = _vitalsData
    
    fun updateVitalsData(data: TechnicalVitalsData) {
        _vitalsData.value = data
    }
}

// ===== COMPOSABLE HOOKS =====
@Composable
fun rememberSessionData(): SessionData {
    return SessionDataProvider.sessionData.collectAsState().value
}

@Composable
fun rememberTechnicalVitals(): TechnicalVitalsData {
    return TechnicalVitalsProvider.vitalsData.collectAsState().value
}

// ===== STRATEGY SIGNAL DATA MODEL =====
data class StrategySignalData(
    val pair: String,
    val direction: String,
    val confidence: Int,
    val status: String,
    val entryPrice: String,
    val riskReward: String
)

// ===== STRATEGY SIGNAL PROVIDER =====
object StrategySignalProvider {
    private val _signalData = MutableStateFlow(
        StrategySignalData(
            pair = "EUR/USD",
            direction = "BUY",
            confidence = 78,
            status = "ACTIVE",
            entryPrice = "1.08420",
            riskReward = "RR 1:2.4"
        )
    )
    
    val signalData: StateFlow<StrategySignalData> = _signalData
    
    fun updateSignalData(data: StrategySignalData) {
        _signalData.value = data
    }
}

@Composable
fun rememberStrategySignal(): StrategySignalData {
    return StrategySignalProvider.signalData.collectAsState().value
}

// ===== PSYCHOLOGY DATA MODEL =====
data class PsychologyData(
    val psychologyScore: Int,
    val sentimentState: String,
    val volatilityState: String,
    val dxyBeta: String,
    val sentimentColor: androidx.compose.ui.graphics.Color
)

// ===== PSYCHOLOGY PROVIDER =====
object PsychologyProvider {
    private val _psychologyData = MutableStateFlow(
        PsychologyData(
            psychologyScore = 72,
            sentimentState = "GREED REGIME",
            volatilityState = "STABLE",
            dxyBeta = "0.82 HIGH",
            sentimentColor = androidx.compose.ui.graphics.Color(0xFF00FF41) // EmeraldSuccess
        )
    )
    
    val psychologyData: StateFlow<PsychologyData> = _psychologyData
    
    fun updatePsychologyData(data: PsychologyData) {
        _psychologyData.value = data
    }
}

@Composable
fun rememberPsychologyData(): PsychologyData {
    return PsychologyProvider.psychologyData.collectAsState().value
}

// ===== EXECUTION METRICS MODEL =====
data class ExecutionMetrics(
    val totalTrades: Int,
    val winRate: Int,
    val avgWinSize: Double,
    val avgLossSize: Double,
    val slippageAvg: Double,
    val latencyAvg: Double,
    val profitFactor: Double
)

// ===== EXECUTION PROVIDER =====
object ExecutionProvider {
    private val _executionData = MutableStateFlow(
        ExecutionMetrics(
            totalTrades = 156,
            winRate = 68,
            avgWinSize = 42.5,
            avgLossSize = 12.3,
            slippageAvg = 0.8,
            latencyAvg = 0.24,
            profitFactor = 2.85
        )
    )
    
    val executionData: StateFlow<ExecutionMetrics> = _executionData
    
    fun updateExecutionData(data: ExecutionMetrics) {
        _executionData.value = data
    }
}

@Composable
fun rememberExecutionMetrics(): ExecutionMetrics {
    return ExecutionProvider.executionData.collectAsState().value
}

// ===== CENTRAL AI APP MANAGER =====
/**
 * Single entry point for AI to manage the core data of the entire app.
 * The AI can call these methods to update any part of the application in real-time.
 * 
 * Example from AI service:
 * AIAppManager.updateDashboardSession(...)
 * AIAppManager.updateTechnicalVitals(...)
 * AIAppManager.updateStrategySignal(...)
 */
object AIAppManager {
    
    // ===== DASHBOARD MANAGEMENT =====
    
    /** Update the entire session/overview section */
    fun updateDashboardSession(
        hubName: String? = null,
        isActive: Boolean? = null,
        completionPercent: Int? = null,
        avgSpread: String? = null,
        volatility: String? = null,
        nextEventTime: String? = null,
        nextEventLabel: String? = null,
        safetyGateStatus: String? = null,
        safetyGateArmed: Boolean? = null,
        globalRegimeText: String? = null
    ) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            SessionData(
                hubName = hubName ?: current.hubName,
                isActive = isActive ?: current.isActive,
                completionPercent = completionPercent ?: current.completionPercent,
                avgSpread = avgSpread ?: current.avgSpread,
                volatility = volatility ?: current.volatility,
                nextEventTime = nextEventTime ?: current.nextEventTime,
                nextEventLabel = nextEventLabel ?: current.nextEventLabel,
                safetyGateStatus = safetyGateStatus ?: current.safetyGateStatus,
                safetyGateArmed = safetyGateArmed ?: current.safetyGateArmed,
                globalRegimeText = globalRegimeText ?: current.globalRegimeText,
                gapAnalysis = current.gapAnalysis,
                volumeAnalysis = current.volumeAnalysis,
                aiSentiment = current.aiSentiment,
                riskLevel = current.riskLevel,
                newsCatalyst = current.newsCatalyst,
                probabilityScore = current.probabilityScore
            )
        )
    }
    
    // ===== STRUCTURED ANALYSIS BOX MANAGEMENT =====
    
    /** Update Gap Analysis structured box */
    fun updateGapAnalysis(gapSize: String, gapPercent: String, direction: String) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            current.copy(
                gapAnalysis = GapAnalysis(gapSize, gapPercent, direction)
            )
        )
    }
    
    /** Update Volume Analysis structured box */
    fun updateVolumeAnalysis(volumeLevel: String, trend: String, strength: String) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            current.copy(
                volumeAnalysis = VolumeAnalysis(volumeLevel, trend, strength)
            )
        )
    }
    
    /** Update AI Sentiment structured box */
    fun updateAISentiment(sentimentScore: Int, sentimentState: String, confidence: Int) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            current.copy(
                aiSentiment = AISentiment(sentimentScore, sentimentState, confidence)
            )
        )
    }
    
    /** Update Risk Level structured box */
    fun updateRiskLevel(riskScore: String, riskValue: String, status: String) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            current.copy(
                riskLevel = RiskLevel(riskScore, riskValue, status)
            )
        )
    }
    
    /** Update News Catalyst structured box */
    fun updateNewsCatalyst(catalystName: String, impactLevel: String, timeToEvent: String, isActive: Boolean) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            current.copy(
                newsCatalyst = NewsCatalyst(catalystName, impactLevel, timeToEvent, isActive)
            )
        )
    }
    
    /** Update Probability Score structured box */
    fun updateProbabilityScore(scoreValue: Int, confidenceLevel: String, prediction: String) {
        val current = SessionDataProvider.sessionData.value
        SessionDataProvider.updateSessionData(
            current.copy(
                probabilityScore = ProbabilityScore(scoreValue, confidenceLevel, prediction)
            )
        )
    }
    
    // ===== TECHNICAL VITALS MANAGEMENT =====
    
    /** Update technical vitals/node health metrics */
    fun updateTechnicalVitals(
        nodeHealth: Double? = null,
        avgSpread: Double? = null,
        volatilityPerHour: Double? = null,
        latencyMs: Double? = null,
        globalRegime: String? = null,
        vixValue: Double? = null,
        dxyChange: Double? = null,
        macroComment: String? = null,
        safetyGateClosed: Boolean? = null
    ) {
        val current = TechnicalVitalsProvider.vitalsData.value
        TechnicalVitalsProvider.updateVitalsData(
            TechnicalVitalsData(
                nodeHealth = nodeHealth ?: current.nodeHealth,
                avgSpread = avgSpread ?: current.avgSpread,
                volatilityPerHour = volatilityPerHour ?: current.volatilityPerHour,
                latencyMs = latencyMs ?: current.latencyMs,
                globalRegime = globalRegime ?: current.globalRegime,
                vixValue = vixValue ?: current.vixValue,
                dxyChange = dxyChange ?: current.dxyChange,
                macroComment = macroComment ?: current.macroComment,
                safetyGateClosed = safetyGateClosed ?: current.safetyGateClosed
            )
        )
    }
    
    // ===== STRATEGY SIGNALS MANAGEMENT =====
    
    /** Update active trading signal */
    fun updateStrategySignal(
        pair: String,
        direction: String,
        confidence: Int,
        status: String,
        entryPrice: String,
        riskReward: String
    ) {
        StrategySignalProvider.updateSignalData(
            StrategySignalData(
                pair = pair,
                direction = direction,
                confidence = confidence,
                status = status,
                entryPrice = entryPrice,
                riskReward = riskReward
            )
        )
    }
    
    // ===== PSYCHOLOGY/SENTIMENT MANAGEMENT =====
    
    /** Update market psychology/sentiment data */
    fun updateMarketPsychology(
        psychologyScore: Int,
        sentimentState: String,
        volatilityState: String,
        dxyBeta: String,
        sentimentColor: Color = Color.White
    ) {
        PsychologyProvider.updatePsychologyData(
            PsychologyData(
                psychologyScore = psychologyScore,
                sentimentState = sentimentState,
                volatilityState = volatilityState,
                dxyBeta = dxyBeta,
                sentimentColor = sentimentColor
            )
        )
    }
    
    // ===== EXECUTION METRICS MANAGEMENT =====
    
    /** Update execution/trading performance metrics */
    fun updateExecutionMetrics(
        totalTrades: Int,
        winRate: Int,
        avgWinSize: Double,
        avgLossSize: Double,
        slippageAvg: Double,
        latencyAvg: Double,
        profitFactor: Double
    ) {
        ExecutionProvider.updateExecutionData(
            ExecutionMetrics(
                totalTrades = totalTrades,
                winRate = winRate,
                avgWinSize = avgWinSize,
                avgLossSize = avgLossSize,
                slippageAvg = slippageAvg,
                latencyAvg = latencyAvg,
                profitFactor = profitFactor
            )
        )
    }
    
    // ===== ADVANCED: BATCH OPERATIONS =====
    
    /**
     * Update multiple screens at once
     * Useful when AI needs to sync data across the app
     */
    fun updateAppState(
        session: SessionData? = null,
        vitals: TechnicalVitalsData? = null,
        signal: StrategySignalData? = null,
        psychology: PsychologyData? = null,
        execution: ExecutionMetrics? = null
    ) {
        if (session != null) SessionDataProvider.updateSessionData(session)
        if (vitals != null) TechnicalVitalsProvider.updateVitalsData(vitals)
        if (signal != null) StrategySignalProvider.updateSignalData(signal)
        if (psychology != null) PsychologyProvider.updatePsychologyData(psychology)
        if (execution != null) ExecutionProvider.updateExecutionData(execution)
    }
}
