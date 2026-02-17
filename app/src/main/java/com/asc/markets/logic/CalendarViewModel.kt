package com.asc.markets.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asc.markets.data.EconomicEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class CalendarViewModel : ViewModel() {
    private val repository = CalendarRepository()

    private val _events = MutableStateFlow<List<EconomicEvent>>(emptyList())
    val events: StateFlow<List<EconomicEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents(asset: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.fetchCalendarEvents(asset = asset).onSuccess { events ->
                _events.value = events
                Log.d("CalendarViewModel", "Events loaded: ${events.size}")
            }.onFailure { exception ->
                _error.value = "Failed to load events: ${exception.message}"
                Log.e("CalendarViewModel", "Failed to load events", exception)
            }

            _isLoading.value = false
        }
    }

    fun loadEvent(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.fetchCalendarEvent(id).onSuccess { event ->
                _events.value = listOf(event)
                Log.d("CalendarViewModel", "Event loaded: ${event.id}")
            }.onFailure { exception ->
                _error.value = "Failed to load event: ${exception.message}"
                Log.e("CalendarViewModel", "Failed to load event", exception)
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
