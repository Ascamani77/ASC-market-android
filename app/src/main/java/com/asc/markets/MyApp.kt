package com.asc.markets

import android.app.Application
import androidx.room.Room
import com.asc.markets.data.trade.AppDatabase
import com.asc.markets.data.trade.TradeHistoryRepository

class MyApp : Application() {
    lateinit var database: AppDatabase
    lateinit var tradeRepository: TradeHistoryRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "trading_db"
        ).build()

        tradeRepository = TradeHistoryRepository(database.tradeDao())
    }
}
