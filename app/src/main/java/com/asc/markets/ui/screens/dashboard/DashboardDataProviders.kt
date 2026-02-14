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

// ===== DYNAMIC INFOBOX MODEL (FLEXIBLE FOR ANY DATA TYPE) =====
sealed class DynamicInfoBox {
    data class KeyValuePair(
        val label: String,
        val value: String,
        val sublabel: String = "",
        val accentColor: Color = Color.White
    ) : DynamicInfoBox()
    
    data class GridLayout(
        val title: String,
        val items: List<GridItem>
    ) : DynamicInfoBox()
    
    data class TextBlock(
        val title: String,
        val content: String,
        val accentColor: Color = Color.White
    ) : DynamicInfoBox()
    
    data class ProgressVisualization(
        val title: String,
        val progress: Int,
        val subtitle: String = ""
    ) : DynamicInfoBox()
    
    data class MetricsRow(
        val metrics: List<MetricItem>
    ) : DynamicInfoBox()
}

data class GridItem(
    val label: String,
    val value: String,
    val unit: String = ""
)

data class MetricItem(
    val label: String,
    val value: String,
    val status: String = ""
)

// ===== SESSION DATA MODEL (NOW WITH DYNAMIC INFOBOXES) =====
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
    val additionalMetrics: List<DynamicInfoBox> = emptyList()  // NEW: Allow custom infoboxes
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
    val safetyGateClosed: Boolean,
    val additionalMetrics: List<DynamicInfoBox> = emptyList()  // NEW: Allow custom infoboxes
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
            additionalMetrics = emptyList()  // Can be populated with real data
        )
    )
    
    val sessionData: StateFlow<SessionData> = _sessionData
    
    fun updateSessionData(data: SessionData) {
        _sessionData.value = data
    }
    
    // ===== AI SERVICE METHODS =====
    // AI can call these methods to update metrics without modifying base session data
    fun addAdditionalMetrics(metrics: List<DynamicInfoBox>) {
        val current = _sessionData.value
        _sessionData.value = current.copy(additionalMetrics = metrics)
    }
    
    fun addMetric(metric: DynamicInfoBox) {
        val current = _sessionData.value
        _sessionData.value = current.copy(
            additionalMetrics = current.additionalMetrics + metric
        )
    }
    
    fun clearAdditionalMetrics() {
        val current = _sessionData.value
        _sessionData.value = current.copy(additionalMetrics = emptyList())
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
            safetyGateClosed = false,
            additionalMetrics = emptyList()  // Can be populated with real data
        )
    )
    
    val vitalsData: StateFlow<TechnicalVitalsData> = _vitalsData
    
    fun updateVitalsData(data: TechnicalVitalsData) {
        _vitalsData.value = data
    }
    
    // ===== AI SERVICE METHODS =====
    // AI can call these methods to update metrics without modifying base vitals data
    fun addAdditionalMetrics(metrics: List<DynamicInfoBox>) {
        val current = _vitalsData.value
        _vitalsData.value = current.copy(additionalMetrics = metrics)
    }
    
    fun addMetric(metric: DynamicInfoBox) {
        val current = _vitalsData.value
        _vitalsData.value = current.copy(
            additionalMetrics = current.additionalMetrics + metric
        )
    }
    
    fun clearAdditionalMetrics() {
        val current = _vitalsData.value
        _vitalsData.value = current.copy(additionalMetrics = emptyList())
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

// ===== DYNAMIC INFOBOX RENDERER =====
@Composable
fun RenderDynamicInfoBox(infoBox: DynamicInfoBox) {
    val padding = 24.dp
    val spacing = 12.dp
    
    when (infoBox) {
        is DynamicInfoBox.KeyValuePair -> {
            RenderKeyValueBox(infoBox)
        }
        is DynamicInfoBox.TextBlock -> {
            RenderTextBlock(infoBox)
        }
        is DynamicInfoBox.GridLayout -> {
            RenderGridLayout(infoBox)
        }
        is DynamicInfoBox.ProgressVisualization -> {
            RenderProgressBox(infoBox)
        }
        is DynamicInfoBox.MetricsRow -> {
            RenderMetricsRow(infoBox)
        }
    }
}

@Composable
private fun RenderKeyValueBox(box: DynamicInfoBox.KeyValuePair) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    box.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = InterFontFamily
                )
                if (box.sublabel.isNotEmpty()) {
                    Text(
                        box.sublabel,
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                box.value,
                color = box.accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun RenderTextBlock(box: DynamicInfoBox.TextBlock) {
    InfoBox(minHeight = 140.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(IndigoAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                )
                Text(
                    box.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                box.content,
                color = SlateText,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun RenderGridLayout(box: DynamicInfoBox.GridLayout) {
    InfoBox(minHeight = 120.dp) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                box.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = InterFontFamily
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                box.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            item.label,
                            color = SlateText,
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            "${item.value}${item.unit}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderProgressBox(box: DynamicInfoBox.ProgressVisualization) {
    InfoBox(minHeight = 100.dp) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                box.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = InterFontFamily
            )
            LinearProgressIndicator(
                progress = box.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = EmeraldSuccess,
                trackColor = DeepBlack
            )
            Text(
                "${box.progress}% ${box.subtitle}",
                color = SlateText,
                fontSize = 11.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun RenderMetricsRow(box: DynamicInfoBox.MetricsRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        box.metrics.forEach { metric ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                color = DeepBlack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        metric.value,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        metric.label,
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp),
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

// ===== AI INFOBOX BUILDER (FOR AI SERVICES) =====
object AIInfoBoxBuilder {
    /**
     * Simple helper for AI to create key-value pair infoboxes
     * Example: AIInfoBoxBuilder.keyValue("RISK/REWARD", "1:2.5", com.asc.markets.ui.theme.EmeraldSuccess)
     */
    fun keyValue(label: String, value: String, sublabel: String = "", color: Color = Color.White): DynamicInfoBox.KeyValuePair {
        return DynamicInfoBox.KeyValuePair(label, value, sublabel, color)
    }
    
    /**
     * Helper for creating text block infoboxes (for analysis, insights, etc.)
     * Example: AIInfoBoxBuilder.textBlock("AI ANALYSIS", "Market is trending up with strong momentum...")
     */
    fun textBlock(title: String, content: String, color: Color = Color.White): DynamicInfoBox.TextBlock {
        return DynamicInfoBox.TextBlock(title, content, color)
    }
    
    /**
     * Helper for creating grid layout infoboxes
     * Example: AIInfoBoxBuilder.grid("STATS", listOf(
     *     GridItem("Win Rate", "72", "%"),
     *     GridItem("Ratio", "1:2.5", "RR")
     * ))
     */
    fun grid(title: String, items: List<GridItem>): DynamicInfoBox.GridLayout {
        return DynamicInfoBox.GridLayout(title, items)
    }
    
    /**
     * Helper for creating progress visualization infoboxes
     * Example: AIInfoBoxBuilder.progress("Trade Win Rate", 72, "of 156 trades")
     */
    fun progress(title: String, progress: Int, subtitle: String = ""): DynamicInfoBox.ProgressVisualization {
        return DynamicInfoBox.ProgressVisualization(title, progress, subtitle)
    }
    
    /**
     * Helper for creating metrics row infoboxes
     * Example: AIInfoBoxBuilder.metrics(listOf(
     *     MetricItem("ProfitFactor", "2.85", "Excellent"),
     *     MetricItem("MaxDrawdown", "-12.5", "Active")
     * ))
     */
    fun metrics(items: List<MetricItem>): DynamicInfoBox.MetricsRow {
        return DynamicInfoBox.MetricsRow(items)
    }
}

// ===== CENTRAL AI APP MANAGER =====
/**
 * Single entry point for AI to manage the entire app.
 * The AI can call these methods to update any part of the application in real-time.
 * 
 * Example from AI service:
 * AIAppManager.updateDashboardSession(...)
 * AIAppManager.addDashboardInsight(...)
 * AIAppManager.updateMarketTechnicals(...)
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
                additionalMetrics = current.additionalMetrics
            )
        )
    }
    
    /** Add insight or metric to dashboard */
    fun addDashboardInsight(insight: String, color: Color = EmeraldSuccess) {
        SessionDataProvider.addMetric(
            AIInfoBoxBuilder.textBlock("AI INSIGHT", insight, color)
        )
    }
    
    /** Add key-value metric to dashboard */
    fun addDashboardMetric(label: String, value: String, sublabel: String = "", color: Color = EmeraldSuccess) {
        SessionDataProvider.addMetric(
            AIInfoBoxBuilder.keyValue(label, value, sublabel, color)
        )
    }
    
    /** Clear all dashboard insights */
    fun clearDashboardInsights() {
        SessionDataProvider.clearAdditionalMetrics()
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
                safetyGateClosed = safetyGateClosed ?: current.safetyGateClosed,
                additionalMetrics = current.additionalMetrics
            )
        )
    }
    
    /** Add insight to technical vitals tab */
    fun addTechnicalVitalsInsight(insight: String, color: Color = IndigoAccent) {
        TechnicalVitalsProvider.addMetric(
            AIInfoBoxBuilder.textBlock("TECHNICAL ALERT", insight, color)
        )
    }
    
    /** Clear vitals insights */
    fun clearTechnicalVitalsInsights() {
        TechnicalVitalsProvider.clearAdditionalMetrics()
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
    
    // ===== INFOBOX HELPERS =====
    
    /** Create a key-value infobox for any screen */
    fun createMetricBox(label: String, value: String, sublabel: String = "", color: Color = EmeraldSuccess): DynamicInfoBox.KeyValuePair {
        return AIInfoBoxBuilder.keyValue(label, value, sublabel, color)
    }
    
    /** Create a text insight/analysis box */
    fun createInsightBox(title: String, content: String, color: Color = EmeraldSuccess): DynamicInfoBox.TextBlock {
        return AIInfoBoxBuilder.textBlock(title, content, color)
    }
    
    /** Create a grid layout box for multiple metrics */
    fun createGridBox(title: String, items: List<GridItem>): DynamicInfoBox.GridLayout {
        return AIInfoBoxBuilder.grid(title, items)
    }
    
    /** Create a progress visualization box */
    fun createProgressBox(title: String, progress: Int, subtitle: String = ""): DynamicInfoBox.ProgressVisualization {
        return AIInfoBoxBuilder.progress(title, progress, subtitle)
    }
    
    /** Create a metrics row with multiple indicators */
    fun createMetricsRow(items: List<MetricItem>): DynamicInfoBox.MetricsRow {
        return AIInfoBoxBuilder.metrics(items)
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
    
    /** Clear all AI-injected insights across all screens */
    fun clearAllInsights() {
        SessionDataProvider.clearAdditionalMetrics()
        TechnicalVitalsProvider.clearAdditionalMetrics()
    }
}
