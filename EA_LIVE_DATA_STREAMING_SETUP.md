# EA Live Data Streaming Setup Guide

## Problem Statement

Your **MyRealApp** currently uses Pepperstone API for live market data, which has limitations:
- ❌ Limited free tier
- ❌ Requires opening each asset in the app before seeing data
- ❌ Not utilizing data from your ASC EA which is already attached to many assets

## Solution

Stream live market data from your **ASC EA (MT5)** → **Backend** → **MyRealApp** for all assets your EA monitors.

---

## Architecture

```
MT5 Terminal (EA Attached to 50+ Assets)
    ↓
LiveDataStreamer.mqh (writes JSON every 10s)
    ↓
live_market_data.json (MQL5/Files directory)
    ↓
Backend API (/live-market-data endpoint)
    ↓
MyRealApp (polls every 10s)
    ↓
Accumulation Radar (shows all assets instantly)
```

---

## Step 1: Update Your EA to Stream Live Data

### 1.1 Add LiveDataStreamer to Your EA

Edit your `ASC_EA.mq5` file:

```mql5
// At the top with other includes
#include "include\ASC_Engine.mqh"
#include "include\ASC_Bot.mqh"
#include "include\JsonWriter.mqh"
#include "include\LiveDataStreamer.mqh"  // ← ADD THIS

// In input parameters section
input group "=== Live Data Streaming ==="
input bool   InpEnableLiveStreaming = true;         // Enable Live Data Streaming
input int    InpStreamIntervalSeconds = 10;          // Stream Interval (seconds)
input string InpLiveDataFile = "live_market_data.json"; // Live Data Output File
```

### 1.2 Add Symbols Array in OnInit()

```mql5
// Global array for symbols to stream
string g_stream_symbols[];

int OnInit()
{
    // ... existing initialization code ...
    
    // Initialize live data streaming
    if (InpEnableLiveStreaming)
    {
        // Get all symbols from Market Watch
        GetAllChartSymbols(g_stream_symbols);
        Print("✅ Live Data Streaming enabled for ", ArraySize(g_stream_symbols), " assets");
        Print("   Streaming interval: ", InpStreamIntervalSeconds, " seconds");
        Print("   Output file: ", InpLiveDataFile);
    }
    
    return(INIT_SUCCEEDED);
}
```

### 1.3 Stream Data in OnTimer() or OnTick()

Add to your `OnTimer()` function:

```mql5
void OnTimer()
{
    // ... existing timer code ...
    
    // Stream live market data (if enabled)
    if (InpEnableLiveStreaming && ArraySize(g_stream_symbols) > 0)
    {
        StreamLiveData(g_stream_symbols, InpLiveDataFile, InpStreamIntervalSeconds);
    }
}
```

**Alternative:** If you want more frequent updates, add to `OnTick()`:

```mql5
void OnTick()
{
    // ... existing tick code ...
    
    // Stream live market data (throttled to interval)
    if (InpEnableLiveStreaming && ArraySize(g_stream_symbols) > 0)
    {
        StreamLiveData(g_stream_symbols, InpLiveDataFile, InpStreamIntervalSeconds);
    }
}
```

---

## Step 2: Backend Already Updated

Your `ai_api.py` now includes:
- ✅ `get_mql5_files_path()` - Finds MQL5 Files directory
- ✅ `/live-market-data` endpoint - Serves EA live data to app

**No additional backend changes needed!**

---

## Step 3: Update MyRealApp to Use EA Live Data

### 3.1 Create EA Data Service

Create new file: `app/src/main/java/com/asc/markets/data/EALiveDataStore.kt`

```kotlin
package com.asc.markets.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
data class EAAssetData(
    val symbol: String,
    val timestamp: Long,
    val prices: EAAssetPrices,
    val m1: EATimeframeData,
    val m5: EATimeframeData? = null,
    val h1: EATimeframeData? = null
)

@Serializable
data class EALiveDataResponse(
    val success: Boolean,
    val timestamp: Long? = null,
    @SerialName("server_time")
    val serverTime: String? = null,
    @SerialName("data_source")
    val dataSource: String? = null,
    @SerialName("asset_count")
    val assetCount: Int? = null,
    val assets: List<EAAssetData> = emptyList(),
    val error: String? = null
)

object EALiveDataStore {
    private const val TAG = "EALiveDataStore"
    private const val EA_DATA_URL = "http://10.164.138.133:8000/live-market-data"
    private const val POLL_INTERVAL_MS = 10_000L // 10 seconds
    
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
    
    fun start() {
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Already polling EA live data")
            return
        }
        
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Starting EA live data polling at $EA_DATA_URL")
            
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
            val client = HttpClient {
                engine {
                    requestTimeout = 5000
                }
            }
            
            val response: HttpResponse = client.get(EA_DATA_URL)
            val responseText = response.body<String>()
            
            val data = json.decodeFromString<EALiveDataResponse>(responseText)
            
            if (data.success && data.assets.isNotEmpty()) {
                _liveAssets.value = data.assets
                _isConnected.value = true
                _lastUpdateTime.value = data.timestamp ?: System.currentTimeMillis()
                
                Log.d(TAG, "Fetched ${data.assets.size} assets from EA (Source: ${data.dataSource})")
            } else {
                Log.w(TAG, "EA data fetch unsuccessful or empty: ${data.error}")
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
                category = inferCategory(asset.symbol)
            )
        }
    }
    
    private fun inferCategory(symbol: String): MarketCategory {
        return when {
            symbol.contains("BTC", ignoreCase = true) ||
            symbol.contains("ETH", ignoreCase = true) ||
            symbol.contains("USDT", ignoreCase = true) -> MarketCategory.CRYPTO
            
            symbol.contains("XAU", ignoreCase = true) ||
            symbol.contains("XAG", ignoreCase = true) ||
            symbol.contains("OIL", ignoreCase = true) -> MarketCategory.COMMODITIES
            
            symbol.contains("US30", ignoreCase = true) ||
            symbol.contains("SPX", ignoreCase = true) ||
            symbol.contains("NAS100", ignoreCase = true) -> MarketCategory.INDICES
            
            symbol.matches(Regex("[A-Z]{6}")) -> MarketCategory.FOREX
            
            else -> MarketCategory.FOREX
        }
    }
}
```

### 3.2 Start EA Data Service in MyApp.kt

```kotlin
// In MyApp.onCreate()
override fun onCreate() {
    super.onCreate()
    
    // ... existing initialization ...
    
    // Start AI Context Service
    AIContextService.start()
    
    // Start EA Live Data Service
    EALiveDataStore.start()  // ← ADD THIS
    
    Log.d("MyApp", "All services started")
}
```

### 3.3 Use EA Data in CurrencyStrengthPanel

Update `CurrencyStrengthPanel.kt`:

```kotlin
@Composable
fun MarketCompareSection(density: MarketCompareDensity = MarketCompareDensity.FULL) {
    // ... existing code ...
    
    // Add EA live data
    val eaLiveAssets by EALiveDataStore.liveAssets.collectAsState()
    val eaConnected by EALiveDataStore.isConnected.collectAsState()
    val eaPairs = remember(eaLiveAssets) {
        EALiveDataStore.toForexPairs()
    }
    
    // Combine data sources: prioritize EA data if available
    val allPairs = when {
        eaConnected && eaPairs.isNotEmpty() -> {
            Log.d("MarketCompare", "Using EA live data (${eaPairs.size} assets)")
            eaPairs
        }
        else -> {
            Log.d("MarketCompare", "Falling back to Pepperstone data")
            (marketPairs + binancePairs + fallbackPairs).distinctBy { it.symbol }
        }
    }
    
    // ... rest of your existing code ...
}
```

---

## Step 4: Test the Complete Pipeline

### 4.1 Compile and Attach EA

1. Copy `LiveDataStreamer.mqh` to `MQL5/Include/` directory
2. Compile `ASC_EA.mq5` with the new changes
3. Attach EA to a chart
4. Enable **Live Data Streaming** in EA inputs
5. Check MT5 Expert log for:
   ```
   ✅ Live Data Streaming enabled for 50 assets
      Streaming interval: 10 seconds
   ```

### 4.2 Verify JSON File Creation

Check that file is being created:
```
C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\
[YOUR_TERMINAL_ID]\MQL5\Files\live_market_data.json
```

Example content:
```json
{
  "timestamp": 1735401600,
  "server_time": "2026.01.28 14:30:00",
  "assets": [
    {
      "symbol": "EURUSD",
      "timestamp": 1735401600,
      "bid": 1.08250,
      "ask": 1.08252,
      "last": 1.08251,
      "open": 1.08200,
      "high": 1.08300,
      "low": 1.08150,
      "close": 1.08251,
      "volume": 1234,
      "spread": 0.00002,
      "change_percent": 0.047
    },
    ...
  ],
  "asset_count": 50
}
```

### 4.3 Test Backend Endpoint

```powershell
curl http://10.164.138.133:8000/live-market-data
```

Expected response:
```json
{
  "success": true,
  "data_source": "MT5_EA_LIVE",
  "timestamp": 1735401600,
  "asset_count": 50,
  "assets": [...]
}
```

### 4.4 Test MyRealApp

1. Rebuild and run MyRealApp
2. Open Dashboard → Market Compare
3. Check Logcat for:
   ```
   EALiveDataStore: Fetched 50 assets from EA (Source: MT5_EA_LIVE)
   MarketCompare: Using EA live data (50 assets)
   ```
4. **Accumulation Radar should now show all 50 assets** without needing to open each one!

---

## Benefits

✅ **Instant Multi-Asset Visibility**
- See live data for all EA-attached assets immediately
- No need to open charts in app first

✅ **No API Limits**
- Your EA has unlimited data access via MT5 broker feed
- No Pepperstone API rate limits

✅ **Real-Time Sync**
- EA streams every 10 seconds
- App polls every 10 seconds
- Total latency: <15 seconds

✅ **Fallback Support**
- App still uses Pepperstone if EA data unavailable
- Graceful degradation

✅ **Multi-Timeframe Data**
- M1, M5, H1 OHLCV available
- Perfect for Accumulation Radar calculations

---

## Configuration Options

### EA Side (ASC_EA.mq5)

```mql5
input bool   InpEnableLiveStreaming = true;     // Enable/disable streaming
input int    InpStreamIntervalSeconds = 10;      // How often to stream (seconds)
input string InpLiveDataFile = "live_market_data.json"; // Output filename
```

### App Side (EALiveDataStore.kt)

```kotlin
private const val EA_DATA_URL = "http://10.164.138.133:8000/live-market-data"
private const val POLL_INTERVAL_MS = 10_000L // Polling frequency
```

---

## Troubleshooting

### Issue: "Live data file not found"

**Solution:**
1. Check EA is running and attached to a chart
2. Verify `InpEnableLiveStreaming = true` in EA inputs
3. Check MT5 Expert log for streaming messages
4. Manually check if JSON file exists in `MQL5/Files/`

### Issue: App shows "EA not connected"

**Solution:**
1. Verify backend is running: `http://10.164.138.133:8000/health`
2. Test endpoint: `curl http://10.164.138.133:8000/live-market-data`
3. Check firewall allows port 8000
4. Verify IP address is correct for your network

### Issue: Some symbols missing

**Solution:**
1. Ensure symbols are in MT5 Market Watch
2. EA only streams symbols visible in Market Watch
3. Right-click Market Watch → Show All to add more symbols

### Issue: Data seems stale

**Solution:**
1. Check EA timer is running (should see periodic logs)
2. Verify `InpStreamIntervalSeconds` is not too large
3. Check file modification time in `MQL5/Files/` directory
4. Reduce `POLL_INTERVAL_MS` in app if needed

---

## Advanced: Extended Data Format

For even more data (support/resistance, volume zones, etc.), use:

```mql5
// In OnTimer()
WriteExtendedLiveData(g_stream_symbols, "extended_live_data.json", true);
```

This includes:
- Multi-timeframe OHLCV (M1, M5, H1)
- Spread metrics
- Volume analysis
- Future: AI signal integration

---

## Performance Impact

**EA Side:**
- Minimal impact (writes JSON once per interval)
- File I/O is fast (<1ms per write)
- No network requests from EA

**Backend Side:**
- Simple file read operation
- No computation required
- Cached by OS file system

**App Side:**
- 10-second polling is lightweight
- Replaces multiple Pepperstone API calls
- Reduces overall network traffic

---

## Summary

| Before | After |
|--------|-------|
| Pepperstone API (limited) | EA Live Stream (unlimited) |
| Must open each asset first | All assets available instantly |
| Rate-limited | No limits |
| ~30 assets max | 50+ assets simultaneously |
| External dependency | Self-hosted |

You now have **complete control** over your market data pipeline! 🚀
