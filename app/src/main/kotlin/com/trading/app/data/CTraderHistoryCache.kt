package com.trading.app.data

import android.content.Context
import android.util.Log
import com.trading.app.models.OHLCData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for cTrader historical candle data
 * Dramatically improves scrolling performance by avoiding bridge calls for cached data
 */
class CTraderHistoryCache private constructor() {
    
    // Cache structure: symbol -> timeframe -> sorted list of candles
    private val cache = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableList<OHLCData>>>()
    
    // Track when data was last updated for cache invalidation
    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
    
    companion object {
        @Volatile
        private var instance: CTraderHistoryCache? = null
        
        private const val TAG = "CTraderHistoryCache"
        private const val CACHE_TTL_MS = 3600_000L // 1 hour
        private const val MAX_CANDLES_PER_SYMBOL = 10000 // Limit memory usage
        
        fun getInstance(): CTraderHistoryCache {
            return instance ?: synchronized(this) {
                instance ?: CTraderHistoryCache().also { instance = it }
            }
        }
    }
    
    /**
     * Get cached candles for a time range
     * Returns null if data is not in cache or is stale
     */
    suspend fun getCandles(
        symbol: String,
        timeframe: String,
        startTime: Long,
        endTime: Long
    ): List<OHLCData>? = withContext(Dispatchers.Default) {
        val key = cacheKey(symbol, timeframe)
        
        // Check if cache is stale
        val lastUpdate = lastUpdateTimes[key] ?: 0L
        if (System.currentTimeMillis() - lastUpdate > CACHE_TTL_MS) {
            Log.d(TAG, "Cache stale for $key")
            return@withContext null
        }
        
        val symbolCache = cache[symbol] ?: return@withContext null
        val candles = symbolCache[timeframe] ?: return@withContext null
        
        if (candles.isEmpty()) return@withContext null
        
        // Filter candles in the requested time range
        val filtered = candles.filter { it.time in startTime..endTime }
        
        // Check if we have complete coverage of the requested range
        if (filtered.isEmpty()) {
            Log.d(TAG, "No cached data for $key in range $startTime-$endTime")
            return@withContext null
        }
        
        // Check for gaps in the data
        val hasGaps = filtered.zipWithNext().any { (a, b) ->
            val expectedInterval = timeframeToSeconds(timeframe)
            (b.time - a.time) > expectedInterval * 2 // Allow for some variance
        }
        
        if (hasGaps) {
            Log.d(TAG, "Gaps detected in cached data for $key")
            return@withContext null
        }
        
        Log.d(TAG, "Cache hit for $key: ${filtered.size} candles")
        filtered
    }
    
    /**
     * Save candles to cache
     */
    suspend fun saveCandles(
        symbol: String,
        timeframe: String,
        candles: List<OHLCData>
    ) = withContext(Dispatchers.Default) {
        if (candles.isEmpty()) return@withContext
        
        val key = cacheKey(symbol, timeframe)
        
        // Get or create symbol cache
        val symbolCache = cache.getOrPut(symbol) { ConcurrentHashMap() }
        
        // Get or create timeframe cache
        val existingCandles = symbolCache.getOrPut(timeframe) { mutableListOf() }
        
        // Merge new candles with existing ones
        val merged = (existingCandles + candles)
            .distinctBy { it.time }
            .sortedBy { it.time }
            .takeLast(MAX_CANDLES_PER_SYMBOL) // Limit memory usage
        
        existingCandles.clear()
        existingCandles.addAll(merged)
        
        // Update last update time
        lastUpdateTimes[key] = System.currentTimeMillis()
        
        Log.d(TAG, "Cached ${candles.size} candles for $key (total: ${existingCandles.size})")
    }
    
    /**
     * Check if we have data for a specific time range
     */
    suspend fun hasDataInRange(
        symbol: String,
        timeframe: String,
        startTime: Long,
        endTime: Long
    ): Boolean = withContext(Dispatchers.Default) {
        val cached = getCandles(symbol, timeframe, startTime, endTime)
        cached != null && cached.isNotEmpty()
    }
    
    /**
     * Get the earliest cached timestamp for a symbol/timeframe
     */
    suspend fun getEarliestTime(
        symbol: String,
        timeframe: String
    ): Long? = withContext(Dispatchers.Default) {
        val symbolCache = cache[symbol] ?: return@withContext null
        val candles = symbolCache[timeframe] ?: return@withContext null
        candles.minOfOrNull { it.time }
    }
    
    /**
     * Get the latest cached timestamp for a symbol/timeframe
     */
    suspend fun getLatestTime(
        symbol: String,
        timeframe: String
    ): Long? = withContext(Dispatchers.Default) {
        val symbolCache = cache[symbol] ?: return@withContext null
        val candles = symbolCache[timeframe] ?: return@withContext null
        candles.maxOfOrNull { it.time }
    }
    
    /**
     * Clear cache for a specific symbol/timeframe
     */
    fun clearCache(symbol: String, timeframe: String) {
        val key = cacheKey(symbol, timeframe)
        cache[symbol]?.remove(timeframe)
        lastUpdateTimes.remove(key)
        Log.d(TAG, "Cleared cache for $key")
    }
    
    /**
     * Clear all cache
     */
    fun clearAll() {
        cache.clear()
        lastUpdateTimes.clear()
        Log.d(TAG, "Cleared all cache")
    }
    
    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        val totalSymbols = cache.size
        val totalCandles = cache.values.sumOf { symbolCache ->
            symbolCache.values.sumOf { it.size }
        }
        val totalTimeframes = cache.values.sumOf { it.size }
        
        return CacheStats(
            totalSymbols = totalSymbols,
            totalTimeframes = totalTimeframes,
            totalCandles = totalCandles,
            estimatedMemoryKB = (totalCandles * 40) / 1024 // Rough estimate: 40 bytes per candle
        )
    }
    
    private fun cacheKey(symbol: String, timeframe: String): String {
        return "${symbol.uppercase()}_${timeframe.uppercase()}"
    }
    
    private fun timeframeToSeconds(timeframe: String): Long {
        return when (timeframe.lowercase()) {
            "1m" -> 60L
            "5m" -> 5 * 60L
            "15m" -> 15 * 60L
            "30m" -> 30 * 60L
            "1h" -> 60 * 60L
            "4h" -> 4 * 60 * 60L
            "1d" -> 24 * 60 * 60L
            "1w" -> 7 * 24 * 60 * 60L
            else -> 60 * 60L
        }
    }
    
    data class CacheStats(
        val totalSymbols: Int,
        val totalTimeframes: Int,
        val totalCandles: Int,
        val estimatedMemoryKB: Int
    )
}
