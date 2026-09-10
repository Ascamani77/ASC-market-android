# FINAL SETUP: EA Built-in AI + Live Data Streaming

## Your Architecture (Simplified)

```
MT5 EA (Built-in AI) → JSON File → HTTP Server → Android App
```

No external AI system needed!

---

## Complete Setup (4 Steps)

### ✅ Step 1: Copy LiveDataStreamer to MT5

```powershell
Copy-Item "C:\Users\HP\Documents\NEW_ASC\ASC_UNIFIED_AI\include\LiveDataStreamer.mqh" `
          "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Include\"
```

### ✅ Step 2: Compile and Attach EA

1. Open MT5 MetaEditor (F4)
2. Open your `ASC_EA.mq5` file
3. Compile (F7) - Check for errors
4. Close MetaEditor
5. In MT5, drag EA to any chart
6. In EA settings:
   ```
   Enable Live Data Streaming to App: TRUE
   Stream Interval (seconds): 10
   ```
7. Click OK

### ✅ Step 3: Start HTTP Server

Open PowerShell and run:

```powershell
C:\Users\HP\start_simple_server.ps1
```

**Leave this window open!** You should see:
```
============================================================
EA LIVE DATA SERVER
============================================================

📁 Serving files from:
   C:\Users\HP\AppData\Roaming\MetaQuotes\...\MQL5\Files

🌐 Server running on:
   http://localhost:8000

✅ Found: live_market_data.json
   Last updated: 2026-07-28 14:30:15

✅ Ready! Your app can now fetch EA data
```

### ✅ Step 4: Run Android App

1. Open Android Studio
2. Build → Rebuild Project
3. Run → Run 'app'
4. Open Dashboard
5. Look for **"EA LIVE"** indicator (green badge)

---

## Verification Checklist

### ✅ EA Side:
- [ ] `LiveDataStreamer.mqh` in MT5 Include folder
- [ ] EA compiles without errors
- [ ] EA attached to chart
- [ ] "Enable Live Data Streaming" = TRUE
- [ ] Check Expert log for:
  ```
  ✅ Live Data Streaming enabled for X assets
  ✅ Live data streamed: X assets to live_market_data.json
  ```

### ✅ Server Side:
- [ ] HTTP server running (PowerShell window open)
- [ ] `live_market_data.json` file exists
- [ ] File timestamp updates every 10 seconds
- [ ] Test endpoint works:
  ```powershell
  curl http://localhost:8000/live_market_data.json
  ```

### ✅ App Side:
- [ ] App rebuilt after changes
- [ ] Check Logcat for:
  ```
  D/MyApp: ✅ EA Live Data Service started
  D/EALiveDataStore: ✅ Fetched X assets from EA
  D/UnifiedMarketData: ✅ Using MT5 EA data: X assets
  ```
- [ ] Dashboard shows "EA LIVE" (green)
- [ ] Accumulation Radar shows all assets

---

## Quick Test Commands

### Test 1: Check JSON file exists
```powershell
Get-Content "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files\live_market_data.json"
```

### Test 2: Check HTTP server
```powershell
curl http://localhost:8000/live_market_data.json
```

### Test 3: Check from app's perspective
```powershell
curl http://10.164.138.133:8000/live_market_data.json
```

All three should return the same JSON with asset data!

---

## Troubleshooting

### ❌ "LiveDataStreamer.mqh not found" (Compile Error)

**Fix:**
```powershell
# Verify file exists
Test-Path "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Include\LiveDataStreamer.mqh"

# If False, copy it again
Copy-Item "C:\Users\HP\Documents\NEW_ASC\ASC_UNIFIED_AI\include\LiveDataStreamer.mqh" `
          "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Include\"
```

### ❌ App shows "FALLBACK" instead of "EA LIVE"

**Possible causes:**
1. HTTP server not running
2. JSON file not being created
3. Network issue

**Fix:**
```powershell
# 1. Check server is running
Get-Process python | Where-Object {$_.CommandLine -like "*http.server*"}

# 2. Check file exists and is recent
Get-Item "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files\live_market_data.json" | Select-Object Name, LastWriteTime

# 3. Check EA Expert log for streaming messages

# 4. Restart HTTP server
# Press Ctrl+C in server window, then run start_simple_server.ps1 again
```

### ❌ JSON file not being created

**Fix:**
1. Check EA is attached to chart
2. Check "Enable Live Data Streaming" = TRUE in EA inputs
3. Wait 10 seconds (first write takes one interval)
4. Check MT5 Expert log for errors

### ❌ "Python not found" when starting server

**Fix:**
```powershell
# Check Python is installed
python --version

# If not installed, download from python.org
# Or use Node.js instead:
npx http-server "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files" -p 8000
```

---

## Daily Startup Routine

Every time you want to use the app:

```powershell
# 1. Start HTTP server (one command)
C:\Users\HP\start_simple_server.ps1

# 2. Open MT5 and attach EA (if not already attached)

# 3. Run Android app

# Done! All assets appear instantly.
```

---

## What You Get

✅ **50+ Assets** - All symbols in MT5 Market Watch  
✅ **10-Second Updates** - Real-time price data  
✅ **Zero API Costs** - No Pepperstone limits  
✅ **EA Built-in AI** - All AI logic runs in MT5  
✅ **Automatic Fallback** - Uses Pepperstone when EA off  
✅ **Simple Setup** - Just 3 components  

---

## Architecture Summary

| Component | Purpose | Port |
|-----------|---------|------|
| **MT5 EA** | Built-in AI + data streaming | N/A |
| **HTTP Server** | Serves JSON file | 8000 |
| **Android App** | Displays live data | N/A |

No complex AI pipeline. No Redis. No multiple ports. Just **EA → File → Server → App**!

---

## Files Modified/Created

### Created:
1. ✅ `LiveDataStreamer.mqh` (MT5 Include)
2. ✅ `EALiveDataStore.kt` (Android)
3. ✅ `UnifiedMarketDataStore.kt` (Android)
4. ✅ `start_simple_server.ps1` (HTTP server)

### Modified:
1. ✅ `ASC_EA.mq5` (Added streaming)
2. ✅ `MyApp.kt` (Start EA service)
3. ✅ `CurrencyStrengthPanel.kt` (Use unified store)

---

## Next Steps

1. **Follow Step 1-4 above**
2. **Verify all checkboxes**
3. **Test with all 3 curl commands**
4. **Run app and check for "EA LIVE" indicator**

You now have a **professional real-time data feed** powered by your EA! 🚀

No external AI dependencies. No complex setup. Just your EA and a simple server.
