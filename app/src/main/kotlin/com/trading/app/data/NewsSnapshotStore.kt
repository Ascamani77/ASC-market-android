package com.trading.app.data

import com.trading.app.models.NewsPayload

object NewsSnapshotStore {
    @Volatile
    var latestPayload: NewsPayload? = null

    @Volatile
    var latestAiPayloadJson: String = ""
}
