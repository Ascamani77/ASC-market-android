package com.asc.markets.data

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

@Serializable
data class EAAssetPrices(
    val bid: Double,
    val ask: Double,
    val last: Double,
    val spread: Double,
    @SerialName("spread_percent")
    val spreadPercent: Double
)

@Serializable
data class EATimeframeData(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    @SerialName("change_percent")
    val changePercent: Double? = null
)

@Serializable
data class EAAiData(
    val confidence: Double = 0.0,
    @SerialName("regime_confidence")
    val regimeConfidence: String = "NONE",
    val direction: String = "WAIT",
    @SerialName("alignment_percentage")
    val alignmentPercentage: Double = 0.0
)

@Serializable
data class EAAssetData(
    val symbol: String,
    val timestamp: Long,
    val prices: EAAssetPrices,
    val m1: EATimeframeData,
    val m5: EATimeframeData? = null,
    val h1: EATimeframeData? = null,
    @SerialName("ea_ai")
    val eaAi: EAAiData? = null
)

@Serializable
data class EALiveDataResponse(
    val timestamp: Long? = null,
    @SerialName("server_time")
    val serverTime: String? = null,
    @SerialName("asset_count")
    val assetCount: Int? = null,
    val assets: List<EAAssetData> = emptyList(),
    // Optional backend fields (not used in direct JSON mode)
    val success: Boolean? = null,
    @SerialName("data_source")
    val dataSource: String? = null,
    val error: String? = null
)

object EALiveDataStore {
    private const val TAG = "EALiveDataStore"
    private const val POLL_INTERVAL_MS = 10_000L // 10 seconds

    private var eaDataUrl: String = ""
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    private val _liveAssets = MutableStateFlow<List<EAAssetData>>(emptyList())
    val liveAssets: StateFlow<List<EAAssetData>> = _liveAssets
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _lastUpdateTime = MutableStateFlow<Long?>(null)
    val lastUpdateTime: StateFlow<Long?> = _lastUpdateTime
    
    private var pollingJob: Job? = null
    
    fun start(context: Context) {
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Already polling EA live data")
            return
        }
        eaDataUrl = NetworkConfig.backendUrl(context) + "/live_market_data.json"

        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Starting EA live data polling at $eaDataUrl")
            
            while (isActive) {
                try {
                    fetchLiveData()
                    delay(POLL_INTERVAL_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop", e)
                    _isConnected.value = false
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }
    
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _isConnected.value = false
        Log.d(TAG, "Stopped EA live data polling")
    }
    
    private suspend fun fetchLiveData() {
        try {
            val client = HttpClient(OkHttp) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 5000
                }
            }
            
            val response: HttpResponse = client.get(eaDataUrl)
            val responseText = response.body<String>()
            
            // Parse raw JSON directly from EA file
            val data = json.decodeFromString<EALiveDataResponse>(responseText)
            
            if (data.assets.isNotEmpty()) {
                _liveAssets.value = data.assets.filter { isTrainedAssetTicker(it.symbol) }
                _isConnected.value = true
                _lastUpdateTime.value = data.timestamp ?: System.currentTimeMillis()
                
                Log.d(TAG, "✅ Fetched ${data.assets.size} assets from EA (Direct JSON)")
            } else {
                Log.w(TAG, "EA data is empty")
                _isConnected.value = false
            }
            
            client.close()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching EA live data", e)
            _isConnected.value = false
        }
    }
    
    /**
     * Convert EA data to ForexPair format for compatibility with existing UI
     */
    fun toForexPairs(): List<ForexPair> {
        return _liveAssets.value.map { asset ->
            val m1 = asset.m1
            ForexPair(
                symbol = asset.symbol,
                name = asset.symbol,
                price = asset.prices.last,
                change = (m1.close - m1.open),
                changePercent = m1.changePercent ?: 0.0,
                category = inferCategory(asset.symbol),
                eaConfidence = asset.eaAi?.confidence ?: 0.0,
                regimeConfidence = asset.eaAi?.regimeConfidence ?: "NONE",
                alignmentPercentage = asset.eaAi?.alignmentPercentage ?: 0.0,
                eaDirection = asset.eaAi?.direction ?: "WAIT"
            )
        }
    }
    
    /**
     * Get price history map (symbol -> prices)
     */
    fun getPriceHistory(): Map<String, List<Double>> {
        val historyMap = mutableMapOf<String, List<Double>>()
        _liveAssets.value.forEach { asset ->
            historyMap[asset.symbol] = listOf(
                asset.m1.open,
                asset.m1.high,
                asset.m1.low,
                asset.m1.close
            )
        }
        return historyMap
    }
    
    /**
     * Get timed price history map (symbol -> timed prices)
     */
    fun getTimedPriceHistory(): Map<String, List<TimedPrice>> {
        val timedHistoryMap = mutableMapOf<String, List<TimedPrice>>()
        _liveAssets.value.forEach { asset ->
            timedHistoryMap[asset.symbol] = listOf(
                TimedPrice(
                    timestampMillis = asset.timestamp,
                    price = asset.m1.close
                )
            )
        }
        return timedHistoryMap
    }
    
    fun inferCategory(symbol: String): MarketCategory {
        return when {
            symbol.contains("BTC", ignoreCase = true) ||
            symbol.contains("ETH", ignoreCase = true) ||
            symbol.contains("DOGE", ignoreCase = true) ||
            symbol.contains("USDT", ignoreCase = true) -> MarketCategory.CRYPTO
            
            symbol.contains("XAU", ignoreCase = true) ||
            symbol.contains("XAG", ignoreCase = true) ||
            symbol.contains("OIL", ignoreCase = true) ||
            symbol.contains("BRENT", ignoreCase = true) -> MarketCategory.COMMODITIES
            
            symbol.contains("US30", ignoreCase = true) ||
            symbol.contains("SPX", ignoreCase = true) ||
            symbol.contains("NAS", ignoreCase = true) ||
            symbol.contains("DOW", ignoreCase = true) -> MarketCategory.INDICES
            
            symbol.matches(Regex("[A-Z]{6}")) -> MarketCategory.FOREX
            
            symbol.length == 4 && symbol.all { it.isLetter() } -> MarketCategory.STOCK
            
            else -> MarketCategory.FOREX
        }
    }
}
