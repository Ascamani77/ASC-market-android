package com.asc.markets

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.room.Room
import com.asc.markets.data.AppVisibilityStore
import com.asc.markets.data.trade.AppDatabase
import com.asc.markets.data.trade.TradeHistoryRepository
import com.asc.markets.data.repository.AiRepository
import com.asc.markets.data.remote.AiRetrofitClient
import com.asc.markets.ai.AIContextService

class MyApp : Application() {
    lateinit var database: AppDatabase
    lateinit var tradeRepository: TradeHistoryRepository
    lateinit var aiRepository: AiRepository
    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit

            override fun onActivityStarted(activity: Activity) {
                startedActivityCount += 1
                AppVisibilityStore.setForeground(startedActivityCount > 0)
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                AppVisibilityStore.setForeground(startedActivityCount > 0)
            }
        })
        AiRetrofitClient.configure(applicationContext)
        aiRepository = AiRepository()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "trading_db"
        ).build()

        tradeRepository = TradeHistoryRepository(database.tradeDao())
        
        // Start AI Context Service to poll backend for real-time AI intelligence
        AIContextService.start()
    }
}
