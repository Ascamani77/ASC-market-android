package com.asc.markets

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import com.asc.markets.data.AppVisibilityStore
import com.asc.markets.data.EALiveDataStore
import com.asc.markets.data.EASignalLiveStore
import com.asc.markets.data.ScannerSignalsStore
import com.asc.markets.data.UiAppearanceStore
import com.asc.markets.data.UserProfileStore
import com.asc.markets.data.trade.AppDatabase
import com.asc.markets.data.trade.TradeHistoryRepository

class MyApp : Application() {
    lateinit var database: AppDatabase
    lateinit var tradeRepository: TradeHistoryRepository
    val aiRepository = com.asc.markets.data.repository.AiRepository()
    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()

        // Initialize user profile from SharedPreferences
        UserProfileStore.init(this)

        // Load UI appearance prefs (InfoBox outline brightness, etc.)
        UiAppearanceStore.init(this)

        // Start EA Live Data Service (Primary Market Data Source)
        EALiveDataStore.start(applicationContext)
        Log.d("MyApp", "✅ EA Live Data Service started")

        // Start live EA write-up stream (WebSocket push from the MT5 bridge)
        EASignalLiveStore.start(applicationContext)
        Log.d("MyApp", "✅ EA Signal Live Store started")

        // Start live MT5 scanner poll (ASSET/DIR/CONF/P(T)/AGE)
        ScannerSignalsStore.start(applicationContext)
        Log.d("MyApp", "✅ Scanner Signals Store started")

        // Start vigilance alert monitor (evaluates deployed EA nodes live)
        com.asc.markets.logic.VigilanceMonitor.start(applicationContext)
        Log.d("MyApp", "✅ Vigilance Monitor started")

        // Pin the process as foreground so the monitor keeps running (and alerting)
        // while the app is backgrounded — without this, Android freezes/kills a
        // backgrounded process and notifications only arrive after reopening the
        // app. Can be switched off in Settings → Push Notification.
        if (getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
                .getBoolean("background_monitor_enabled", true)
        ) {
            com.asc.markets.notifications.AlertMonitorService.start(applicationContext)
        }

        // Register the Trading Alerts channel so Android Settings → Notifications
        // for this app exists and notifications can display on Android 8+.
        com.asc.markets.notifications.NotificationHelper.createChannel(applicationContext)
        
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
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "trading_db"
        ).build()

        tradeRepository = TradeHistoryRepository(database.tradeDao())
    }
}
