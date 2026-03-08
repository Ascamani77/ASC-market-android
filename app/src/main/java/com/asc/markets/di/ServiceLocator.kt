package com.asc.markets.di

import android.content.Context
import com.asc.markets.MyApp
import com.asc.markets.data.trade.TradeHistoryRepository

/**
 * Lightweight service locator that exposes application-scoped services without
 * introducing a DI framework. Safe, non-invasive, and easy to replace with
 * Hilt/Dagger later.
 */
object ServiceLocator {
    fun tradeRepository(context: Context): TradeHistoryRepository? {
        return try {
            val app = context.applicationContext as? MyApp
            app?.tradeRepository
        } catch (_: Exception) {
            null
        }
    }
}
