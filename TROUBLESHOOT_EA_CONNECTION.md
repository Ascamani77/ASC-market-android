# Troubleshooting: Android App Still Shows FALLBACK

## ✅ What's Working
- EA is creating `live_market_data.json` successfully (14-15KB file)
- HTTP server running on port 8001
- Server accessible from PC: `http://192.168.1.198:8001/live_market_data.json` ✅
- Android app code has correct URL
- Network security config allows HTTP to `192.168.1.198`

## 🔍 Diagnosis Steps

### 1. Verify Phone is on Same WiFi Network
**FROM YOUR PHONE BROWSER:**
```
http://192.168.1.198:8001/live_market_data.json
```

**Expected:** You should see JSON data with "assets" array

**If you see 404 or timeout:**
- Phone might be on mobile data instead of WiFi
- Phone might be on different WiFi network than PC
- Check phone WiFi settings

### 2. Check PC Firewall (MOST LIKELY ISSUE)
Windows Firewall might be blocking incoming connections on port 8001.

**FROM PC POWERSHELL (AS ADMINISTRATOR):**
```powershell
# Check if firewall rule exists
Get-NetFirewallRule -DisplayName "Python HTTP Server*" | Select-Object DisplayName, Enabled, Direction, Action

# If no rule exists, ADD ONE:
New-NetFirewallRule -DisplayName "Python HTTP Server EA Data" -Direction Inbound -Protocol TCP -LocalPort 8001 -Action Allow -Profile Any

# Test again from phone browser
```

### 3. Force Android App Cache Clear
**FROM ANDROID STUDIO:**
```
Build > Clean Project
Build > Rebuild Project
```

**ON YOUR PHONE:**
```
Settings > Apps > ASC Markets > Storage > Clear Cache
Settings > Apps > ASC Markets > Storage > Clear Data (WARNING: Loses saved data)
```

Then **UNINSTALL** the app from phone completely, and reinstall fresh from Android Studio.

### 4. Check App Logs in Logcat
**IN ANDROID STUDIO LOGCAT:**
Filter by: `EALiveDataStore`

Look for:
- ✅ `"Starting EA live data polling at http://192.168.1.198:8001/live_market_data.json"`
- ✅ `"✅ Fetched X assets from EA (Direct JSON)"`
- ❌ `"Error fetching EA live data"` (with error details)

### 5. Test with curl from Another Device
If you have another computer or laptop on same WiFi:
```bash
curl http://192.168.1.198:8001/live_market_data.json
```

This confirms if the server is truly accessible on the network.

---

## 🚀 Quick Fix Checklist

1. [ ] **Phone on WiFi** (not mobile data)
2. [ ] **Phone on SAME WiFi network as PC** (192.168.1.x range)
3. [ ] **Windows Firewall rule added for port 8001** (see step 2 above)
4. [ ] **App completely uninstalled and reinstalled**
5. [ ] **Server is running** (PowerShell window still open with server)
6. [ ] **Test from phone browser first** before testing app

---

## 📱 Expected Behavior When Working

**In App UI:**
- Green indicator: **"EA LIVE"** (not orange "FALLBACK")
- Currency strength bars updating every 10 seconds
- All EA assets visible (BCHUSDm, BTCUSDm, ETHUSDm, forex pairs, etc.)

**In Logcat:**
```
D/EALiveDataStore: Starting EA live data polling at http://192.168.1.198:8001/live_market_data.json
D/EALiveDataStore: ✅ Fetched 27 assets from EA (Direct JSON)
D/UnifiedMarketData: ✅ Using MT5 EA data: 27 assets
```

---

## 🔧 Most Common Issue: Windows Firewall

**The firewall rule is the #1 reason this fails.** Even though `curl` from the same PC works, Windows Firewall blocks INCOMING network requests by default.

**TO FIX:**
1. Open PowerShell **AS ADMINISTRATOR**
2. Run:
```powershell
New-NetFirewallRule -DisplayName "Python HTTP Server EA Data" -Direction Inbound -Protocol TCP -LocalPort 8001 -Action Allow -Profile Any
```
3. Test from phone browser IMMEDIATELY
4. Should work now!

---

## 🆘 Still Not Working?

**Try changing to a different port:**
1. Stop current server (Ctrl+C in PowerShell)
2. Edit `C:\Users\HP\start_simple_server.ps1`
3. Change `$PORT = 8001` to `$PORT = 8002`
4. Update Android app URL in `EALiveDataStore.kt`:
   ```kotlin
   private const val EA_DATA_URL = "http://192.168.1.198:8002/live_market_data.json"
   ```
5. Add firewall rule for new port:
   ```powershell
   New-NetFirewallRule -DisplayName "Python HTTP Server EA Data 8002" -Direction Inbound -Protocol TCP -LocalPort 8002 -Action Allow -Profile Any
   ```
6. Rebuild app and test

---

## 📊 Server Status Commands

```powershell
# Check if server process is running
Get-Process python | Where-Object {$_.MainWindowTitle -like "*HTTP*"}

# Check if port 8001 is listening
netstat -ano | findstr :8001

# Test from PC
curl http://localhost:8001/live_market_data.json

# Test from PC using network IP
curl http://192.168.1.198:8001/live_market_data.json

# Check file is updating
Get-Item "C:\Users\HP\AppData\Roaming\MetaQuotes\Terminal\D0E8209F77C8CF37AD8BF550E51FF075\MQL5\Files\live_market_data.json" | Select-Object LastWriteTime, Length
```
