package com.asc.markets.logic

import android.os.Handler
import android.os.Looper
import java.util.*

/**
 * Institutional Health Watchdog
 * Monitors clock drift and memory pressure to ensure node integrity.
 */
object IntegrityWatchdog {
    private val handler = Handler(Looper.getMainLooper())
    private var lastTick: Long = System.currentTimeMillis()

    fun start() {
        handler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val drift = (now - lastTick - 1000).toInt()
                val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
                
                HealthMonitor.updateProfiler(usedMemory.toInt(), drift)
                
                lastTick = now
                handler.postDelayed(this, 1000)
            }
        })
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
    }
}