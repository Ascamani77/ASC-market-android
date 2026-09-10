# Simple Setup: EA Built-in AI Only (No External Backend)

## Your Setup

You're using the **ASC EA's built-in AI engine** - no external Python AI system needed!

```
┌──────────────────────────────────┐
│  MT5 Terminal                    │
│  ├─ ASC_EA.mq5                  │
│  │  ├─ Built-in AI Engine       │
│  │  │  ├─ Regime Detection      │
│  │  │  ├─ Volatility Analysis   │
│  │  │  ├─ Structure Analysis    │
│  │  │  ├─ Liquidity Detection   │
│  │  │  └─ Signal Generation     │
│  │  └─ LiveDataStreamer        │
│  │     └─ Streams OHLCV data   │
│  └─ Writes: live_market_data.json │
└──────────────┬───────────────────┘
               ↓
┌──────────────────────────────────┐
│  Simple HTTP Server              │
│  (Serves the JSON file)          │
│  Port: 8000                      │
└──────────────┬───────────────────┘
               ↓
┌──────────────────────────────────┐
│  MyRealApp (Android)             │
│  └─ Gets live data from EA       │
└──────────────────────────────────┘
```

## What You DON'T Need

❌ No NEW_ASC system  
❌ No Python AI feeders  
❌ No Redis database  
❌ No port 8003/8004  
❌ No external AI pipeline  

## What You DO Need

✅ MT5 with ASC EA running  
✅ Simple HTTP server to serve JSON file  
✅ Your Android app  

---

## Setup Instructions

### Step 1: Copy LiveDataStreamer.mqh

```powershell
# Copy to your MT5 Include directory
Copy-Item "C:\Users\HP\Documents\NEW_ASC\ASC_UNIFIED_AI\include\LiveDataStreamer.mqh" `
          "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Include\"
```

### Step 2: Update and Compile EA

1. Open MT5 MetaEditor
2. Open `ASC_EA.mq5`
3. Verify these changes are in the file:
   - Include: `#include "include\LiveDataStreamer.mqh"`
   - Input: `Enable Live Data Streaming to App: TRUE`
   - OnInit: Symbol array initialization
   - OnTimer: `StreamLiveData()` call
4. Click Compile (F7)

### Step 3: Attach EA to Chart

1. Drag EA to any chart
2. In EA inputs, ensure:
   ```
   Enable Live Data Streaming to App: TRUE
   Stream Interval (seconds): 10
   ```

### Step 4: Start Simple HTTP Server

Create file: `C:\Users\HP\start_simple_server.ps1`

```powershell
# Simple HTTP Server for EA Live Data
$MQL5_FILES = "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files"

Write-Host "=" * 60 -ForegroundColor Green
Write-Host "STARTING EA LIVE DATA SERVER" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Green
Write-Host ""
Write-Host "Serving from: $MQL5_FILES" -ForegroundColor Yellow
Write-Host "Port: 8000" -ForegroundColor Yellow
Write-Host "Endpoint: http://localhost:8000/live_market_data.json" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

# Start Python HTTP server
Set-Location $MQL5_FILES
python -m http.server 8000 --bind 0.0.0.0
```

Run it:
```powershell
powershell -File C:\Users\HP\start_simple_server.ps1
```

### Step 5: Update App to Use Simple Server

Update `EALiveDataStore.kt`:

```kotlin
object EALiveDataStore {
    private const val TAG = "EALiveDataStore"
    private const val EA_DATA_URL = "http://10.164.138.133:8000/live_market_data.json"
    //                                                    ↑               ↑
    //                                              Port 8000      Direct JSON file
    private const val POLL_INTERVAL_MS = 10_000L
```

And update the fetch function to parse raw JSON:

```kotlin
private suspend fun fetchLiveData() {
    try {
        val client = HttpClient {
            engine {
                requestTimeout = 5000
            }
        }
        
        val response: HttpResponse = client.get(EA_DATA_URL)
        val responseText = response.body<String>()
        
        // Parse the raw JSON from EA
        val json = Json.decodeFromString<EALiveDataResponse>(responseText)
        
        if (json.assets.isNotEmpty()) {
            _liveAssets.value = json.assets
            _isConnected.value = true
            _lastUpdateTime.value = json.timestamp ?: System.currentTimeMillis()
            Log.d(TAG, "✅ Fetched ${json.assets.size} assets from EA")
        }
        
        client.close()
        
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching EA live data", e)
        _isConnected.value = false
    }
}
```

### Step 6: Rebuild and Run App

1. Build → Rebuild Project
2. Run → Run 'app'
3. Check for "EA LIVE" indicator

---

## Verification

### Check EA is streaming:
```powershell
Get-Content "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files\live_market_data.json"
```

### Check HTTP server:
```powershell
curl http://localhost:8000/live_market_data.json
```

### Check app logs:
```
D/EALiveDataStore: ✅ Fetched 52 assets from EA
D/UnifiedMarketData: ✅ Using MT5 EA data: 52 assets
```

---

## Summary

Your system is **standalone and self-contained**:

✅ **EA has built-in AI** - No external Python AI needed  
✅ **EA streams data** - Just OHLCV market data  
✅ **Simple HTTP server** - Just serves the JSON file  
✅ **App gets data** - All assets instantly visible  

**No complex AI system required!** Just EA + HTTP server + App 🚀

---

## Complete Startup Commands

```powershell
# 1. Start HTTP server (serves EA's JSON file)
powershell -File C:\Users\HP\start_simple_server.ps1

# 2. Open MT5 and attach EA to chart

# 3. Run Android app
```

That's it! Three simple steps.
