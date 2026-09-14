package com.asc.markets.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.asc.markets.MainActivity
import com.asc.markets.R

/**
 * Keeps the signal monitor alive while the app is backgrounded.
 *
 * All the real work (EASignalLiveStore WebSocket, ScannerSignalsStore poll,
 * VigilanceMonitor) is started once from MyApp.onCreate. An Android process
 * that sits backgrounded is normally frozen or killed within minutes — which is
 * exactly why notifications only ever appeared after reopening the app. This
 * foreground service pins the process as foreground so fresh EA write-ups keep
 * streaming and alerting even with the UI closed or the screen off.
 */
class AlertMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "signal_monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Signal monitor",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Keeps ASC MARKET listening for EA signals in the background"
                        setShowBadge(false)
                        setSound(null, null)
                    }
                )
            }
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ASC Market signal monitor active")
            .setContentText("Listening for EA signals — alerts appear here")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2101

        /** Start the service; falls back to a plain start if foreground isn't permitted. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlertMonitorService::class.java)
                )
            } catch (e: Exception) {
                try {
                    context.startService(Intent(context, AlertMonitorService::class.java))
                } catch (_: Exception) {
                }
            }
        }

        /** Stop the service and remove its ongoing notification. */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AlertMonitorService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}