# Quick Start: EA Live Data System

## 3-Step Setup

### Step 1: Copy LiveDataStreamer.mqh to MT5

```powershell
# Copy the file to your MT5 Include directory
Copy-Item "C:\Users\HP\Documents\NEW_ASC\ASC_UNIFIED_AI\include\LiveDataStreamer.mqh" `
          "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Include\"
```

### Step 2: Compile and Attach EA

1. Open MT5
2. Open MetaEditor (F4)
3. Open `ASC_EA.mq5`
4. Click Compile (F7)
5. Check for errors
6. Attach EA to any chart
7. In EA inputs, set:
   ```
   Enable Live Data Streaming to App: TRUE
   Stream Interval (seconds): 10
   ```

### Step 3: Start Backend

```powershell
cd C:\Users\HP\Documents\NEW_ASC
.\START.ps1
```

This starts:
- ✅ Redis database
- ✅ API server on port **8003**
- ✅ MT5 data receiver on port 8004
- ✅ All AI feeders

### Step 4: Rebuild and Run App

1. Open Android Studio
2. Build → Rebuild Project
3. Run → Run 'app'
4. Check Dashboard for "EA LIVE" indicator

---

## Verification Commands

### Check EA is streaming:
```powershell
Get-Content "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files\live_market_data.json"
```

### Check backend is serving:
```powershell
curl http://10.164.138.133:8003/live-market-data
```

### Check app is receiving:
```
# In Android Studio Logcat, filter by:
EALiveDataStore
```

---

## Expected Output

### MT5 Expert Log:
```
✅ Live Data Streaming enabled for 52 assets
   Streaming interval: 10 seconds
   Output file: live_market_data.json
✅ Live data streamed: 52 assets to live_market_data.json
```

### Backend Console:
```
INFO: Reading fresh data from Redis (caching disabled)
INFO: GET /live-market-data - 200 OK
```

### Android Logcat:
```
D/MyApp: ✅ EA Live Data Service started
D/EALiveDataStore: ✅ Fetched 52 assets from EA (Source: MT5_EA_LIVE)
D/UnifiedMarketData: ✅ Using MT5 EA data: 52 assets
```

### App UI:
- Dashboard header shows **"EA LIVE"** badge (green)
- Accumulation Radar shows all 52 assets
- Prices update every 10 seconds

---

## Troubleshooting Quick Fixes

### ❌ Compilation Error: "LiveDataStreamer.mqh not found"
```powershell
# Verify file exists:
Test-Path "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Include\LiveDataStreamer.mqh"
```

### ❌ App shows "FALLBACK"
```powershell
# Check each component:
1. MT5 Expert log: Is "Live data streamed" appearing?
2. JSON file: Does it exist and have recent timestamp?
3. Backend: Is it running and accessible?
4. Network: Can app reach backend IP?
```

### ❌ Empty JSON file
```powershell
# Check MT5 Market Watch has symbols
# Right-click Market Watch → Show All
```

---

That's it! Your app now gets live data from EA! 🚀
