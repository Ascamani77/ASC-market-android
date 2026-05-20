package com.trading.app.data

import android.util.Log

/**
 * Shared connection state for all BinanceService instances.
 * This prevents desync between multiple service instances (e.g., main service vs chart service).
 */
object BinanceConnectionState {
    @Volatile
    private var _isRegionBlocked = false
    
    var isRegionBlocked: Boolean
        get() = _isRegionBlocked
        set(value) {
            if (_isRegionBlocked != value) {
                _isRegionBlocked = value
                Log.w("BinanceConnectionState", "Region block status changed: $value")
            }
        }
    
    /**
     * Reset the region block flag.
     * Call this when VPN is connected or network changes.
     */
    fun reset() {
        _isRegionBlocked = false
        Log.i("BinanceConnectionState", "Region block reset - services will attempt reconnection")
    }
}
