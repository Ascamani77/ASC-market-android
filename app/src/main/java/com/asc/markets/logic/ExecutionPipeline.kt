package com.asc.markets.logic

import android.util.Log
import com.asc.markets.backend.SurveillanceStateManager

object ExecutionPipeline {
    private const val TAG = "SurveillancePipeline"

    /**
     * Policy Guard: Final deterministic check before dispatching to the institutional surveillance engine.
     */
    fun authorizeDispatch(pair: String, side: String, size: Double): Boolean {
        val gate = CalendarService.getTradingStatus(pair)
        val armed = SurveillanceStateManager.state.value.isArmed

        if (gate.isBlocked) {
            Log.e(TAG, "SURVEILLANCE_VETO: Safety Gate Blocked - ${gate.reason}")
            return false
        }

        if (!armed) {
            Log.e(TAG, "SURVEILLANCE_VETO: Surveillance Pipeline Not Armed")
            return false
        }

        Log.i(TAG, "SURVEILLANCE_AUTHORIZED: $side $pair $size lots. Policy: COMPLIANT")
        return true
    }

    fun parseIntentStrict(input: String): String {
        val normalized = input.trim().uppercase()
        return when {
            normalized.startsWith("BUY") -> "BUY"
            normalized.startsWith("SELL") -> "SELL"
            normalized.startsWith("CANCEL") -> "CANCEL"
            else -> "UNKNOWN"
        }
    }
}