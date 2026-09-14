package com.asc.markets.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.asc.markets.MainActivity
import com.asc.markets.R

object NotificationHelper {

    private var notificationId = 1000

    /**
     * Create (or no-op if exists) the "Trading Alerts" notification channel.
     * Called at app startup so the system Settings → Notifications section for
     * this app exists before any alert fires.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(FirebaseMessagingServiceImpl.CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        FirebaseMessagingServiceImpl.CHANNEL_ID,
                        FirebaseMessagingServiceImpl.CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Price alerts, indicator signals, and market notifications"
                        enableVibration(true)
                    }
                )
            }
        }
    }

    private fun ensureChannel(context: Context) {
        createChannel(context)
    }

    /** Push-category preference key gate for a given alert type (null = not filtered). */
    private fun categoryKeyFor(type: String): String? = when (type.lowercase()) {
        "ea_signal", "ea", "entry", "entry_signal" -> "allow_signal_push"
        "volatility", "regime", "burst", "vol", "autofib_cross", "cfvg_zone", "fvg_retest",
        "sd_zone", "price_alert" -> "allow_volatility_push"
        "ai", "ai_signal", "analysis", "signal" -> "allow_ai_push"
        "news", "macro" -> "allow_news_push"
        "execution", "fill", "order", "position", "close_position", "risk", "tp", "sl" -> "allow_execution_push"
        "critical", "security", "vigilance", "liquidation" -> "allow_critical_push"
        else -> null
    }

    /** True = suppress this alert due to cooldown / hourly cap from Settings → Push Notification. */
    private fun throttled(prefs: android.content.SharedPreferences): Boolean {
        val now = System.currentTimeMillis()
        val cooldownMs = prefs.getInt("push_cooldown_minutes", 10).coerceIn(1, 60) * 60_000L
        if (now - prefs.getLong("push_last_alert_ts", 0L) < cooldownMs) return true
        val hourBucket = now / 3_600_000L
        var bucket = prefs.getLong("push_hour_bucket", hourBucket)
        var count = if (bucket == hourBucket) prefs.getInt("push_hour_count", 0) else 0
        val max = prefs.getInt("push_max_alerts_per_hour", 8).coerceIn(1, 50)
        if (count >= max) return true
        prefs.edit()
            .putLong("push_last_alert_ts", now)
            .putLong("push_hour_bucket", hourBucket)
            .putInt("push_hour_count", count + 1)
            .apply()
        return false
    }

    /**
     * Canonical asset key used for per-asset muting: upper-case with the MT5
     * mirror suffix stripped, so "GBPUSDm" and "GBPUSD" are treated as the same asset.
     */
    fun normalizeSymbol(symbol: String): String {
        var s = symbol.uppercase().trim()
        if (s.length > 3 && s.endsWith("M")) {
            val base = s.dropLast(1)
            if (base.all { it.isLetterOrDigit() }) s = base
        }
        return s
    }

    /** True = the user muted this asset in Settings → Push Notification → MUTED ASSETS. */
    fun mutedAssets(prefs: android.content.SharedPreferences): Set<String> =
        prefs.getStringSet("muted_notification_symbols", emptySet()) ?: emptySet()

    private fun isMuted(prefs: android.content.SharedPreferences, symbol: String): Boolean =
        symbol.isNotEmpty() && normalizeSymbol(symbol) in mutedAssets(prefs)

    /**
     * Fire a notification through the user's push policy.
     * Suppressed entirely when the master gate is off, or when the alert's
     * category toggle is disabled in Settings → Push Notification.
     */
    fun showAlert(
        context: Context,
        title: String,
        body: String,
        type: String = "generic",
        symbol: String = ""
    ) {
        ensureChannel(context)
        val prefs = context.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enable_push", true)) return
        categoryKeyFor(type)?.let { key ->
            if (!prefs.getBoolean(key, true)) return
        }
        if (isMuted(prefs, symbol)) return
        if (throttled(prefs)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("notification_symbol", symbol)
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, FirebaseMessagingServiceImpl.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(appIconBitmap(context))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!prefs.getBoolean("push_sound_enabled", true)) builder.setSilent(true)
        if (!prefs.getBoolean("push_vibration_enabled", true)) builder.setVibrate(null)
        if (!prefs.getBoolean("push_lockscreen_enabled", true)) builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        if (prefs.getBoolean("push_grouped_notifications", true)) builder.setGroup("trading_alerts")

        manager.notify(notificationId++, builder.build())
    }

    /** App icon as a bitmap — shown large beside the notification title.
     *  Rendered from the foreground at 1.4x so the "A" fills the icon instead of
     *  sitting tiny inside adaptive-icon padding. */
    private fun appIconBitmap(context: Context): Bitmap? = try {
        val size = 192
        val background = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
        val foreground = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        background?.setBounds(0, 0, size, size)
        background?.draw(canvas)
        if (foreground != null) {
            val fw = (size * 1.4f).toInt()
            val fh = (size * 1.4f).toInt()
            foreground.setBounds((size - fw) / 2, (size - fh) / 2, (size + fw) / 2, (size + fh) / 2)
            foreground.draw(canvas)
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
