package com.asc.markets.backend

import android.util.Log

data class OrderPayload(
    val pair: String,
    val side: String,
    val volume: Double,
    val slippagePips: Int,
    val comment: String,
    val nodeSignature: String = "ASC-L14-UK"
)

object OrderEngine {
    private const val TAG = "OrderEngine"

    fun dispatchInstitutionalOrder(payload: OrderPayload, isArmed: Boolean): Boolean {
        // Corrected signature call to match PolicyGuard.kt
        val (allowed, reason) = PolicyGuard.canExecute(payload.pair, isArmed)
        
        if (!allowed) {
            Log.e(TAG, "DISPATCH_REJECTED: $reason")
            return false
        }

        // Internal Node Dispatch Logic
        Log.i(TAG, "PIPELINE_ACTIVE: Dispatching ${payload.side} on ${payload.pair} for ${payload.volume} lots")
        Log.i(TAG, "NODE_SIG: ${payload.nodeSignature}")
        
        return true
    }
}