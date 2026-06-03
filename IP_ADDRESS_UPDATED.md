# IP Address Updated to 192.168.1.198

## Summary
All hardcoded IP addresses have been updated from the old IPs to your current Wi-Fi IP: **192.168.1.198**

## Files Updated

### 1. NetworkConfig.kt
**Location**: `app/src/main/java/com/asc/markets/data/NetworkConfig.kt`

**Changes**:
```kotlin
const val DEFAULT_HOST = "192.168.1.198"  // was "10.76.160.133"
const val DEFAULT_BACKEND_URL = "http://192.168.1.198:8000"
```

This affects:
- Backend API URL
- MT5 Bridge Host (default)
- cTrader Live Bridge Host (default)
- cTrader Demo Bridge Host (default)
- Redis Host (default)

### 2. local.properties (Root)
**Location**: `local.properties`

**Changes**:
```properties
CTRADER_BRIDGE_HOST=192.168.1.198  # was 10.164.138.133
CTRADER_DEMO_BRIDGE_HOST=192.168.1.198  # was 10.164.138.133
```

### 3. local.properties (Assets)
**Location**: `app/src/main/assets/local.properties`

**Changes**:
```properties
CTRADER_BRIDGE_HOST=192.168.1.198  # was 10.164.138.133
```

### 4. TradingApp.kt
**Location**: `app/src/main/kotlin/com/trading/app/TradingApp.kt`

**Changes**:
```kotlin
val redisHost = prefs.getString("redis_host", "192.168.1.198") ?: "192.168.1.198"
// was "10.164.138.133"
```

### 5. PepperstoneDemoChartService.kt
**Location**: `app/src/main/kotlin/com/trading/app/data/PepperstoneDemoChartService.kt`

**Changes**:
```kotlin
private val redisHost: String = "192.168.1.198"  // was "10.164.138.133"
```

### 6. PepperstoneCTraderChartService.kt
**Location**: `app/src/main/kotlin/com/trading/app/data/PepperstoneCTraderChartService.kt`

**Changes**:
```kotlin
private val redisHost: String = "192.168.1.198"  // was "10.164.138.133"
```

### 7. PepperstoneChartService.kt
**Location**: `app/src/main/kotlin/com/trading/app/data/PepperstoneChartService.kt`

**Changes**:
```kotlin
private val redisHost: String = "192.168.1.198"  // was "10.164.138.133"
```

### 8. TradingChartPepperstoneDemo.kt
**Location**: `app/src/main/kotlin/com/trading/app/components/TradingChartPepperstoneDemo.kt`

**Changes**:
```kotlin
redisHost = "192.168.1.198"  // was "10.164.138.133"
```

## Services Configuration

All services should now connect to:

| Service | Address | Port | Status |
|---------|---------|------|--------|
| Redis | 192.168.1.198 | 6379 | ✅ Listening |
| AI Backend | 192.168.1.198 | 8000 | ✅ Listening |
| cTrader Live Bridge | 192.168.1.198 | 8082 | ✅ Listening |
| cTrader Demo Bridge | 192.168.1.198 | 8083 | ✅ Listening |
| MT5 Bridge | 192.168.1.198 | 8081 | ⚠️ Check if running |

## Next Steps

### 1. Rebuild the App
```bash
# Clean and rebuild
./gradlew clean assembleDebug
```

Or in Android Studio:
- Build → Clean Project
- Build → Rebuild Project

### 2. Reinstall on Device
- Uninstall the old version from your Android device
- Install the newly built APK

### 3. Verify Connection
After installing:
1. Open the app
2. Navigate to **Market Data Bus** page
3. Check that all services show **ACTIVE** status (green dot)
4. Verify real-time data is flowing

### 4. Check Settings (Optional)
Go to Settings and verify the values are correct:
- Backend URL: `http://192.168.1.198:8000`
- Redis Host: `192.168.1.198`
- cTrader Host: `192.168.1.198`

## Troubleshooting

### If Services Still Show IDLE:

1. **Verify Services Are Running**:
```powershell
netstat -an | findstr "6379 8000 8082 8083"
```
All should show `LISTENING` on `0.0.0.0`

2. **Check Android and PC on Same Network**:
- Both should be on Wi-Fi: 192.168.1.x
- Try pinging PC from Android

3. **Test Backend Connection**:
Open browser on Android:
```
http://192.168.1.198:8000/docs
```
Should show FastAPI documentation

4. **Check Firewall**:
Windows Firewall should allow connections on ports 6379, 8000, 8082, 8083

5. **Restart Services**:
```powershell
cd C:\Users\HP\Documents\NEW_ASC
.\START_EVERYTHING_PODMAN.ps1
```

### If IP Changes in Future:

Your Wi-Fi IP (192.168.1.198) may change if your router assigns dynamic IPs.

**Solution**: Set a static IP reservation in your router for your PC's MAC address.

**Or**: Update the IP addresses again following this same process.

## Important Notes

- ✅ All default IP addresses updated
- ✅ Redis configuration updated
- ✅ cTrader bridge configuration updated
- ✅ MT5 bridge will use new default
- ✅ Backend URL updated
- ⚠️ Settings in app will now persist correctly
- ⚠️ Must rebuild and reinstall app for changes to take effect

## Verification Checklist

After rebuilding and installing:
- [ ] App connects to backend (check Market Data Bus)
- [ ] Pepperstone cTrader Live shows ACTIVE
- [ ] Pepperstone cTrader Demo shows ACTIVE
- [ ] Binance shows ACTIVE
- [ ] ASC AI Backend shows ACTIVE
- [ ] Prices update in real-time
- [ ] Charts load data
- [ ] AI deployments fetch successfully
