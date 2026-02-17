package com.asc.markets.api

import com.asc.markets.api.dto.CalendarResponse
import com.asc.markets.data.EconomicEvent
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApi {
    @GET("/api/calendar")
    suspend fun getCalendarEvents(
        @Query("asset") asset: String? = null
    ): CalendarResponse

    @GET("/api/calendar/{id}")
    suspend fun getCalendarEvent(
        @Path("id") id: String
    ): EconomicEvent
}
