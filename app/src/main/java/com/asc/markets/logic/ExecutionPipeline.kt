package com.asc.markets.logic

import android.util.Log
import com.asc.markets.backend.ExecutionStateManager

object ExecutionPipeline {
    private const val TAG = "ExecutionPipeline"

    /**
     * Policy Guard: Final deterministic check before dispatching to order engine.
     */
    fun authorizeDispatch(pair: String, side: String, size: Double): Boolean {
        val gate = CalendarService.getTradingStatus(pair)
        val armed = ExecutionStateManager.state.value.isArmed

        if (gate.isBlocked) {
            Log.e(TAG, "DISPATCH_VETO: Safety Gate Blocked - ${gate.reason}")
            return false
        }

        if (!armed) {
            Log.e(TAG, "DISPATCH_VETO: Pipeline Not Armed")
            return false
        }

        Log.i(TAG, "DISPATCH_AUTHORIZED: $side $pair $size lots. Policy: COMPLIANT")
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