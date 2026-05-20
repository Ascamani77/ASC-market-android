package com.asc.markets.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.*
import com.trading.app.data.CalendarSnapshotStore
import com.trading.app.models.EconomicCalendarDisplayEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import com.asc.markets.ai.AIContextService
import com.asc.markets.ai.ImpactLevel
import kotlinx.coroutines.flow.combine

sealed class EventStreamUiState {
    object Loading : EventStreamUiState()
    data class Success(val events: List<IntelligenceEvent>) : EventStreamUiState()
    data class Error(val message: String) : EventStreamUiState()
}

class EventStreamViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<EventStreamUiState>(EventStreamUiState.Loading)
    val uiState: StateFlow<EventStreamUiState> = _uiState.asStateFlow()

    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    private val _globalHeaderCollapse = MutableStateFlow(0f)
    val globalHeaderCollapse: StateFlow<Float> = _globalHeaderCollapse.asStateFlow()

    init {
        // Observe both calendar events and AI context
        viewModelScope.launch {
            combine(
                AIContextService.contextState,
                _watchlist // Or any other local state that should trigger a refresh
            ) { aiContext, _ ->
                fetchEvents(isPullToRefresh = true)
            }.collect {}
        }
        
        fetchEvents()
    }

    fun refresh() {
        AIContextService.refresh()
        fetchEvents(isPullToRefresh = true)
    }

    fun setGlobalHeaderCollapse(progress: Float) {
        _globalHeaderCollapse.value = progress.coerceIn(0f, 1f)
    }

    fun toggleWatchlist(id: String) {
        _watchlist.value = if (_watchlist.value.contains(id)) {
            _watchlist.value - id
        } else {
            _watchlist.value + id
        }
    }

    private fun fetchEvents(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isPullToRefresh) {
                _uiState.value = EventStreamUiState.Loading
            }
            
            try {
                delay(300)
                
                var calendarEvents = mutableListOf<EconomicCalendarDisplayEvent>()
                val displayPayload = CalendarSnapshotStore.latestDisplayPayload
                
                if (displayPayload != null) {
                    calendarEvents.addAll(displayPayload.events)
                } else {
                    val aiPayload = CalendarSnapshotStore.latestAiPayload
                    if (aiPayload != null) {
                        aiPayload.events.forEach { aiEvent ->
                            calendarEvents.add(EconomicCalendarDisplayEvent(
                                id = aiEvent.id,
                                isoDateTime = aiEvent.isoDateTime,
                                releaseTimeLabel = "",
                                countryCode = aiEvent.countryCode,
                                countryName = aiEvent.countryName,
                                currencyCode = aiEvent.currencyCode,
                                title = aiEvent.title,
                                actual = aiEvent.actual,
                                forecast = aiEvent.forecast,
                                previous = aiEvent.previous,
                                importance = aiEvent.importance,
                                impactDirection = aiEvent.impactDirection,
                                isSpeechOrReport = false,
                                isAllDay = false,
                                detailsUrl = aiEvent.detailsUrl
                            ))
                        }
                    }
                }
                
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                
                if (calendarEvents.isNotEmpty()) {
                    val filteredEvents = calendarEvents.filter { event ->
                        val timestamp = try {
                            dateFormat.parse(event.isoDateTime)?.time ?: 0L
                        } catch (e: Exception) { 0L }
                        timestamp > (now - 86400000)
                    }.sortedBy { it.isoDateTime }.take(50)

                    if (filteredEvents.isNotEmpty()) {
                        val aiContext = AIContextService.contextState.value
                        
                        val intelligenceEvents = filteredEvents.map { event ->
                            val timestamp = try {
                                dateFormat.parse(event.isoDateTime)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) { System.currentTimeMillis() }
                            
                            // 1. Try to find a matching news impact headline
                            val matchingNews = aiContext.newsImpacts.find { 
                                it.headline.contains(event.title, ignoreCase = true) || 
                                event.title.contains(it.headline, ignoreCase = true)
                            }
                            
                            // 2. Try to find a matching asset decision
                            val matchingDecision = AIContextService.getDecisionForAsset(event.currencyCode)
                            
                            mapToIntelligenceEvent(event, matchingNews, matchingDecision, timestamp)
                        }
                        _uiState.value = EventStreamUiState.Success(intelligenceEvents)
                        return@launch
                    }
                }

                _uiState.value = EventStreamUiState.Success(emptyList())
                
            } catch (e: Exception) {
                _uiState.value = EventStreamUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun mapToIntelligenceEvent(
        event: EconomicCalendarDisplayEvent,
        newsImpact: com.asc.markets.ai.NewsImpact?,
        aiDecision: com.asc.markets.ai.AIDecision?,
        timestamp: Long
    ): IntelligenceEvent {
        // Use real AI data if available, otherwise default to "Awaiting Analysis" instead of fake 50%
        val biasStr = newsImpact?.let { "neutral" } ?: aiDecision?.direction?.lowercase() ?: "neutral"
        val postureStr = if (newsImpact?.impact == ImpactLevel.HIGH) "defensive" else "balanced"
        
        // Use real confidence if available
        val confidence = when {
            newsImpact != null -> (newsImpact.aiConfidence * 100).toInt()
            aiDecision != null -> (aiDecision.confidence * 100).toInt()
            else -> 0 // 0 indicates "No AI Data" instead of fake 50%
        }
        
        val summary = newsImpact?.headline ?: aiDecision?.reason ?: "Awaiting live ASC AI verification for ${event.title}."
        val severity = when (newsImpact?.impact) {
            ImpactLevel.HIGH -> IntelligenceSeverity.critical
            ImpactLevel.MEDIUM -> IntelligenceSeverity.high
            else -> IntelligenceSeverity.normal
        }

        val assetClass = when (newsImpact?.assetType) {
            "forex" -> AssetClass.forex
            "crypto" -> AssetClass.forex // IntelligenceEvent uses specific categories
            "stocks" -> AssetClass.stock
            "commodities" -> AssetClass.commodity
            else -> AssetClass.macro
        }

        val assets = newsImpact?.affectedAssets ?: listOf(event.currencyCode)

        val unlockState = when (event.impactDirection) {
            1 -> IntelligenceUnlockState.HARD_UNLOCK
            2 -> IntelligenceUnlockState.LOCKED
            else -> IntelligenceUnlockState.SOFT_UNLOCK
        }

        return IntelligenceEvent(
            id = event.id.toString(),
            asset_class = assetClass,
            source = if (newsImpact != null || aiDecision != null) "ASC-CENTRAL-AI" else "ASC-AI-PENDING",
            source_type = if (newsImpact != null || aiDecision != null) SourceType.real else SourceType.derived,
            event_type = IntelligenceEventType.release,
            timestamp_utc = timestamp,
            assets_affected = assets,
            title = event.title,
            actual = event.actual.toDoubleOrNull(),
            estimate = event.forecast.toDoubleOrNull(),
            previous = event.previous.toDoubleOrNull(),
            unit = "",
            strategy_context = StrategyEligibility(
                bias = biasStr,
                `class` = assetClass.name,
                risk_posture = postureStr,
                rationale = summary
            ),
            confidence_score = confidence.toDouble(),
            execution_regime = if (confidence > 70) RegimeState.VOLATILE else RegimeState.REGIME_NEUTRAL,
            visual_state = if (confidence > 80) VisualState.HIGH_CONVICTION else VisualState.NEUTRAL,
            persistence_count = 1,
            transition_status = TransitionStatus.active,
            volatility_confirmed = confidence > 60,
            severity = severity,
            safety_gate = false,
            unlock_state = unlockState,
            narrative_summary = summary
        )
    }
}
