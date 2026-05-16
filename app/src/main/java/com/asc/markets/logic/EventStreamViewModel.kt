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
        // Initial fetch happens once on creation
        fetchEvents()
    }

    fun refresh() {
        // Called by pull-to-refresh, uses special flag to prevent blanking screen
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
            // Only show full-screen loading if it's not a background refresh
            if (!isPullToRefresh) {
                _uiState.value = EventStreamUiState.Loading
            }
            
            try {
                // Small delay to allow any pending calendar updates
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
                        val intelligenceEvents = mutableListOf<IntelligenceEvent>()
                        val jobs = filteredEvents.map { event ->
                            async {
                                val timestamp = try {
                                    dateFormat.parse(event.isoDateTime)?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) { System.currentTimeMillis() }
                                
                                val aiAnalysis = SimulationGeminiService.generateEventAnalysis(
                                    eventTitle = event.title,
                                    actual = event.actual,
                                    forecast = event.forecast,
                                    previous = event.previous,
                                    importance = event.importance
                                )
                                
                                mapToIntelligenceEvent(event, aiAnalysis, timestamp)
                            }
                        }
                        intelligenceEvents.addAll(jobs.awaitAll())
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
        aiAnalysis: org.json.JSONObject,
        timestamp: Long
    ): IntelligenceEvent {
        val biasStr = aiAnalysis.optString("bias", "neutral")
        val postureStr = aiAnalysis.optString("posture", "balanced")
        val confidence = aiAnalysis.optInt("confidence", 50)
        val summary = aiAnalysis.optString("narrative_summary", "Event detected.")
        val severityStr = aiAnalysis.optString("severity", "normal")
        val assetClassStr = aiAnalysis.optString("asset_class", "macro")
        val assetsArray = aiAnalysis.optJSONArray("assets")
        
        val assets = mutableListOf<String>()
        if (assetsArray != null) {
            for (i in 0 until assetsArray.length()) {
                assets.add(assetsArray.getString(i))
            }
        }
        if (assets.isEmpty()) {
            assets.add(event.currencyCode)
        }

        val assetClass = try { AssetClass.valueOf(assetClassStr.lowercase()) } catch(e: Exception) { AssetClass.macro }
        val severity = try { IntelligenceSeverity.valueOf(severityStr.lowercase()) } catch(e: Exception) { IntelligenceSeverity.normal }

        val unlockState = when (event.impactDirection) {
            1 -> IntelligenceUnlockState.HARD_UNLOCK
            2 -> IntelligenceUnlockState.LOCKED
            else -> IntelligenceUnlockState.SOFT_UNLOCK
        }

        return IntelligenceEvent(
            id = event.id.toString(),
            asset_class = assetClass,
            source = "ASC-AI-CALENDAR",
            source_type = SourceType.derived,
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
            execution_regime = RegimeState.REGIME_NEUTRAL,
            visual_state = VisualState.NEUTRAL,
            persistence_count = 1,
            transition_status = TransitionStatus.active,
            volatility_confirmed = false,
            severity = severity,
            safety_gate = false,
            unlock_state = unlockState,
            narrative_summary = summary
        )
    }
}
