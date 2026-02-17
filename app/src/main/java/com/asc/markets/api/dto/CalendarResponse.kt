package com.asc.markets.api.dto

import com.asc.markets.data.EconomicEvent

data class CalendarResponse(
    val success: Boolean,
    val count: Int,
    val data: List<EconomicEvent>,
    val filter: String?,
    val timestamp: String
)
