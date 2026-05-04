package com.trading.app.data

import com.trading.app.models.EconomicCalendarAiPayload
import com.trading.app.models.EconomicCalendarDisplayPayload

object CalendarSnapshotStore {
    @Volatile
    var latestDisplayPayload: EconomicCalendarDisplayPayload? = null

    @Volatile
    var latestAiPayload: EconomicCalendarAiPayload? = null

    @Volatile
    var latestAiPayloadJson: String = ""
}
