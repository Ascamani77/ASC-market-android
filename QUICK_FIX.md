# ✅ QUICK FIX - Update App IP Address

## Good News!
All your services are running correctly and listening on all network interfaces:
- ✅ Redis: Port 6379 - LISTENING on 0.0.0.0
- ✅ AI Backend: Port 8000 - LISTENING on 0.0.0.0  
- ✅ cTrader Live: Port 8082 - LISTENING on 0.0.0.0
- ✅ cTrader Demo: Port 8083 - LISTENING on 0.0.0.0

## The Problem
Your app is trying to connect to **10.76.160.133**, but your PC's IP is **192.168.1.198**.

## The Solution (2 Minutes)

### Step 1: Open App Settings
1. Launch your app on Android
2. Look for **Settings**, **Configuration**, or **Network Settings**

### Step 2: Update These Values
```
Backend URL:        http://192.168.1.198:8000
cTrader Host:       192.168.1.198
cTrader Port:       8082
cTrader Demo Host:  192.168.1.198
cTrader Demo Port:  8083
Redis Host:         192.168.1.198  (if there's a setting for it)
Redis Port:         6379
```

### Step 3: Save and Restart
1. Save the settings
2. Close and reopen the app
3. Check Market Data Bus page - should show ACTIVE status

## Test Connection First (Optional)
Open browser on your Android device and visit:
```
http://192.168.1.198:8000/docs
```
If you see the FastAPI documentation page, your connection is working!

## Alternative: Update Default in Code
If you want to change the default permanently, edit this file:

**File**: `app/src/main/java/com/asc/markets/data/NetworkConfig.kt`

**Change line 7**:
```kotlin
const val DEFAULT_HOST = "192.168.1.198"  // was "10.76.160.133"
```

**Change line 8**:
```kotlin
const val DEFAULT_BACKEND_URL = "http://192.168.1.198:8000"
```

Then rebuild and reinstall the app.

## Verify It's Working
After updating:
1. Open **Market Data Bus** page
2. You should see:
   - Pepperstone cTrader Live: **ACTIVE** (green dot)
   - Pepperstone cTrader Demo: **ACTIVE** (green dot)  
   - Binance USDT Futures: **ACTIVE** (green dot)
   - ASC AI Backend: **ACTIVE** (green dot)
3. Latency and Buffer Load should show real values
4. Prices should update in real-time

## Still Not Working?
1. Make sure Android and PC are on the same Wi-Fi network (192.168.1.x)
2. Try pinging your PC from Android
3. Check Windows Firewall isn't blocking (though services are listening, so should be OK)
4. Restart the app after changing settings
