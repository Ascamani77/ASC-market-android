package com.asc.markets

import android.app.Application
import androidx.room.Room
import com.asc.markets.data.trade.AppDatabase
import com.asc.markets.data.trade.TradeHistoryRepository
import com.asc.markets.data.repository.AiRepository

class MyApp : Application() {
    lateinit var database: AppDatabase
    lateinit var tradeRepository: TradeHistoryRepository
    lateinit var aiRepository: AiRepository

    override fun onCreate() {
        super.onCreate()
        aiRepository = AiRepository()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "trading_db"
        ).build()

        tradeRepository = TradeHistoryRepository(database.tradeDao())
    }
}
