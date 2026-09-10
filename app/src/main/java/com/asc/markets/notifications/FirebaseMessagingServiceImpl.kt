package com.asc.markets.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.asc.markets.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceImpl : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        const val CHANNEL_ID = "trading_alerts"
        const val CHANNEL_NAME = "Trading Alerts"
        private const val CHANNEL_DESC = "Price alerts, indicator signals, and market notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        applicationContext.getSharedPreferences("asc_prefs", Context.MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply()
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Trading Alert"
        val body = data["body"] ?: message.notification?.body ?: ""
        val type = data["type"] ?: "generic"
        val symbol = data["symbol"] ?: ""

        NotificationHelper.showAlert(this, title, body, type, symbol)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendTokenToServer(token: String) {
        Log.d(TAG, "TODO: Send token to your backend: $token")
    }
}
