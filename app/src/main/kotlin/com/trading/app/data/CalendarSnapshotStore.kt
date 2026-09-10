package com.trading.app.data

import com.trading.app.models.EconomicCalendarAiPayload
import com.trading.app.models.EconomicCalendarDisplayPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CalendarSnapshotStore {
    private val _latestDisplayPayload = MutableStateFlow<EconomicCalendarDisplayPayload?>(null)
    val latestDisplayPayloadFlow: StateFlow<EconomicCalendarDisplayPayload?> = _latestDisplayPayload.asStateFlow()

    // Backward-compatible accessors: existing readers/writers keep working unchanged,
    // while Compose screens can collect latestDisplayPayloadFlow to react to updates.
    // (StateFlow.value is already thread-safe, so no @Volatile needed here.)
    var latestDisplayPayload: EconomicCalendarDisplayPayload?
        get() = _latestDisplayPayload.value
        set(value) { _latestDisplayPayload.value = value }

    @Volatile
    var latestAiPayload: EconomicCalendarAiPayload? = null

    @Volatile
    var latestAiPayloadJson: String = ""
}
