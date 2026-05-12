package com.asc.markets.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.asc.markets.MainActivity
import com.asc.markets.R
import com.asc.markets.data.CombinedFallbackState

object CombinedFallbackNotifier {
    const val ACTION_ACCEPT = "com.asc.markets.action.ACCEPT_COMBINED_FALLBACK"
    const val ACTION_DENY = "com.asc.markets.action.DENY_COMBINED_FALLBACK"
    const val EXTRA_REQUEST_ID = "combined_fallback_request_id"
    private const val CHANNEL_ID = "combined_fallback_alerts"
    private const val NOTIFICATION_ID = 4217

    fun show(context: Context, fallbackState: CombinedFallbackState) {
        val appContext = context.applicationContext
        if (!canPostNotifications(appContext)) {
            return
        }
        ensureChannel(appContext)

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(appContext, 0, openIntent, pendingFlags)
        val actionRequestCode = (fallbackState.requestId % Int.MAX_VALUE).toInt()
        val acceptIntent = Intent(appContext, CombinedFallbackActionReceiver::class.java).apply {
            action = ACTION_ACCEPT
            putExtra(EXTRA_REQUEST_ID, fallbackState.requestId)
        }
        val denyIntent = Intent(appContext, CombinedFallbackActionReceiver::class.java).apply {
            action = ACTION_DENY
            putExtra(EXTRA_REQUEST_ID, fallbackState.requestId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(appContext, actionRequestCode + 1, acceptIntent, pendingFlags)
        val denyPendingIntent = PendingIntent.getBroadcast(appContext, actionRequestCode + 2, denyIntent, pendingFlags)
        val message = "Live data switched to Combined fallback. Accept or deny."
        val detail = fallbackState.reason.ifBlank { message }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.lucide_activity)
            .setContentTitle("Combined fallback")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.lucide_activity, "Accept", acceptPendingIntent)
            .addAction(R.drawable.lucide_activity, "Deny", denyPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Combined fallback", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts when live data switches to Combined fallback"
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }
}
