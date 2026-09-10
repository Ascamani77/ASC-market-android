package com.asc.markets.data

import android.content.Context

enum class AccumulationRadarTimeframe(
    val prefValue: String,
    val displayName: String,
    val windowMillis: Long,
    val bucketCount: Int
) {
    MIN_5("5m", "5min", 5L * 60L * 1000L, 20),
    MIN_15("15m", "15min", 15L * 60L * 1000L, 20),
    MIN_30("30m", "30min", 30L * 60L * 1000L, 22),
    HOUR_1("1h", "1H", 60L * 60L * 1000L, 24),
    HOUR_4("4h", "4H", 4L * 60L * 60L * 1000L, 24),
    HOUR_12("12h", "12H", 12L * 60L * 60L * 1000L, 24),
    DAY_1("1d", "1D", 24L * 60L * 60L * 1000L, 24);

    companion object {
        const val PREF_KEY = "accumulation_radar_timeframe"

        fun fromPref(value: String?): AccumulationRadarTimeframe {
            return values().firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: HOUR_1
        }

        fun current(context: Context): AccumulationRadarTimeframe {
            val prefs = context.applicationContext.getSharedPreferences(NetworkConfig.PREFS_NAME, Context.MODE_PRIVATE)
            return fromPref(prefs.getString(PREF_KEY, HOUR_1.prefValue))
        }
    }
}
