package com.asc.markets.logic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.concurrent.timer

data class FeedStatus(
    val symbol: String,
    val lastTickAt: Long
)

object FeedMonitor {
    private val initial = listOf("EUR/USD","GBP/USD","USD/JPY","BTC/USD","XAU/USD","SPX/500")
    private val _feeds = MutableStateFlow(initial.map { FeedStatus(it, System.currentTimeMillis()) })
    val feeds = _feeds.asStateFlow()

    init {
        // simulate incoming ticks every second for a random feed
        timer(period = 1000) {
            val idx = Random().nextInt(initial.size)
            tickFeed(initial[idx])
        }
    }

    fun tickFeed(symbol: String) {
        val now = System.currentTimeMillis()
        _feeds.value = _feeds.value.map {
            if (it.symbol == symbol) it.copy(lastTickAt = now) else it
        }
    }

    fun setAllStale() {
        _feeds.value = _feeds.value.map { it.copy(lastTickAt = it.lastTickAt - 10000) }
    }
}
