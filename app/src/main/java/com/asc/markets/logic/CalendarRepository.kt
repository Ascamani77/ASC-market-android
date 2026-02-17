package com.asc.markets.logic

import com.asc.markets.api.ApiClient
import com.asc.markets.data.EconomicEvent
import android.util.Log

class CalendarRepository {
    private val calendarApi = ApiClient.calendarApi

    suspend fun fetchCalendarEvents(asset: String? = null): Result<List<EconomicEvent>> = try {
        val response = calendarApi.getCalendarEvents(asset = asset)
        Log.d("CalendarRepository", "Successfully fetched ${response.count} events")
        Result.success(response.data)
    } catch (e: Exception) {
        Log.e("CalendarRepository", "Error fetching events: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun fetchCalendarEvent(id: String): Result<EconomicEvent> = try {
        val event = calendarApi.getCalendarEvent(id)
        Log.d("CalendarRepository", "Successfully fetched event: $id")
        Result.success(event)
    } catch (e: Exception) {
        Log.e("CalendarRepository", "Error fetching event $id: ${e.message}", e)
        Result.failure(e)
    }
}
