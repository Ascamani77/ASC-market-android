package com.asc.markets.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.asc.markets.data.CombinedFallbackStore

class CombinedFallbackActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getLongExtra(CombinedFallbackNotifier.EXTRA_REQUEST_ID, 0L)
        when (intent.action) {
            CombinedFallbackNotifier.ACTION_ACCEPT -> CombinedFallbackStore.accept(requestId)
            CombinedFallbackNotifier.ACTION_DENY -> CombinedFallbackStore.deny(requestId)
        }
        CombinedFallbackNotifier.cancel(context)
    }
}
