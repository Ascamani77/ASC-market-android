
package com.asc.markets.backend

import com.asc.markets.logic.CalendarService
import android.util.Log

object PolicyGuard {
    private const val TAG = "PolicyGuard"

    /**
     * Replicates validateSurveillanceRequest() logic precisely.
     */
    fun canExecute(pair: String, isArmed: Boolean): Pair<Boolean, String> {
        val newsStatus = CalendarService.getTradingStatus(pair)
        
        if (newsStatus.isBlocked) {
            Log.w(TAG, "SURVEILLANCE_REJECTED: Safety Gate Active - ${newsStatus.reason}")
            return false to "SAFETY_GATE_ACTIVE"
        }

        if (!isArmed) {
            Log.w(TAG, "SURVEILLANCE_REJECTED: Surveillance Not Armed")
            return false to "SURVEILLANCE_NOT_ARMED"
        }

        Log.i(TAG, "SURVEILLANCE_ACTIVE: Policy compliance verified for $pair")
        return true to "OK"
    }
}
