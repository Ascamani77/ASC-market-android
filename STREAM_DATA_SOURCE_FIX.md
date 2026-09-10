# StreamScreen Data Source Fix

## Problem
User separated Pepperstone, Binance, and MT5 Exness in StreamScreen settings. Binance works, but Pepperstone and MT5 Exness data are stale/not working.

## Root Causes Identified

### 1. TradingApp Not Reacting to Settings Changes ✅ FIXED
**Issue**: When user changes data source in SettingsScreen, the change is saved to SharedPreferences, but `TradingApp.kt` doesn't immediately detect and react to the change.

**Fix Applied**: Added a polling mechanism in `TradingApp.kt` to monitor SharedPreferences every 500ms and update `chartFeedType` when it changes.

```kotlin
// Monitor SharedPreferences for chart feed type changes from settings
LaunchedEffect(Unit) {
    while (true) {
        delay(500) // Check every 500ms for settings changes
        val currentFeedType = ChartFeedType.current(context)
        if (currentFeedType != chartFeedType) {
            android.util.Log.i("TradingApp", "Chart feed type changed from ${chartFeedType.displayName} to ${currentFeedType.displayName}")
            chartFeedType = currentFeedType
        }
    }
}
```

### 2. cTrader Bridge Client ID Typo ✅ FIXED
**Issue**: The PowerShell startup script had a typo in the CLIENT_ID:
- Correct (from local.properties): `YOUR_CTRADER_CLIENT_ID`
- Wrong (in script): `YOUR_CTRADER_CLIENT_ID`
- Difference: `6nlO` (lowercase L) vs `6nIO` (uppercase i)

**Fix Applied**: Corrected the CLIENT_ID in `start_ctrader_bridge.ps1` and added the REFRESH_TOKEN.

### 3. cTrader Bridge Authentication Failure ⚠️ NEEDS VERIFICATION
**Issue**: The cTrader bridge Python script (`ctrader_bridge.py`) is failing with:
```
[cTrader] error: errorCode: "CH_CLIENT_AUTH_FAILURE"
description: "wrong random id"
```

**Root Cause**: The bridge is using the protobuf API authentication, but the tokens provided are for the REST/WebSocket API (obtained from sandbox). These are incompatible authentication methods.

**Current Status**: 
- Bridge script exists at `MyRealApp/ctrader_bridge.py`
- Credentials are configured in `local.properties`:
  - `CTRADER_CLIENT_ID=YOUR_CTRADER_CLIENT_ID`
  - `CTRADER_CLIENT_SECRET=YOUR_CTRADER_CLIENT_SECRET`
  - `CTRADER_ACCESS_TOKEN=sfV4Gls2KooxFKKpsqaUpboQswkPBz65DddPOZkLX-E`
  - `CTRADER_REFRESH_TOKEN=O0QYC0A8aH4SH5JLfliBnj7w2K_MWJet4v9ZEpHv0mM`
  - `CTRADER_HOST_TYPE=demo`
  - `CTRADER_BRIDGE_PORT=8082`
- Bridge is listening on `ws://0.0.0.0:8082`
- App is configured to connect to bridge at `${cTraderHost}:${cTraderPort}`

### 3. MT5 Bridge Connection
**Issue**: MT5 Exness data not working.

**Possible Causes**:
- MT5 bridge not running
- MT5 bridge connection settings incorrect
- MT5 service not properly initialized when EXNESS is selected

## Solutions

### Immediate Fix (Already Applied)
✅ Added polling mechanism to detect settings changes in `TradingApp.kt`

### cTrader Bridge Fix Options

#### Option A: Fix Bridge Authentication (Recommended)
The bridge needs to be modified to use REST/WebSocket API instead of protobuf API:

1. **Update `ctrader_bridge.py`** to use the access token for WebSocket authentication
2. **Remove protobuf authentication** and use token-based auth
3. **Use cTrader WebSocket API** endpoints instead of protobuf

#### Option B: Use Direct API Integration
Remove the bridge entirely and use `CTraderService.kt` directly:

1. **Remove redundant integration** in `TradingApp.kt` (lines ~350-395)
2. **Replace `CTraderBridgeClient`** with direct `CTraderService` calls
3. **Update `PepperstoneChartService`** to use REST API instead of bridge

#### Option C: Get Proper Protobuf Credentials
Obtain proper protobuf API credentials from cTrader that work with the bridge's authentication method.

### MT5 Bridge Fix

1. **Verify MT5 bridge is running**:
   ```bash
   # Check if MT5 bridge is listening on port 8081
   netstat -an | findstr 8081
   ```

2. **Check MT5 bridge configuration** in settings:
   - Host: Should match your network IP
   - Port: Default 8081
   - Verify MT5 service connects when EXNESS is selected

3. **Verify MT5 service initialization** in `TradingApp.kt`:
   ```kotlin
   LaunchedEffect(chartFeedType) {
       if (chartFeedType == ChartFeedType.EXNESS) {
           mt5Service.connect()
       } else {
           reverseBridge.disconnect()
           mt5Service.disconnect()
           isConnected = true
       }
   }
   ```

## Testing Steps

1. **Test Settings Change Detection**:
   - Open app with Binance selected
   - Go to Settings → Chart Type
   - Select Pepperstone
   - Check logs for: "Chart feed type changed from Binance to Pepperstone"
   - Verify quotes update to Pepperstone symbols

2. **Test cTrader Bridge**:
   - Start bridge: `python ctrader_bridge.py`
   - Check for connection errors
   - Select Pepperstone in settings
   - Verify quotes update

3. **Test MT5 Bridge**:
   - Verify MT5 bridge is running
   - Select Exness in settings
   - Verify quotes update

## Next Steps

1. **Choose cTrader fix approach** (A, B, or C above)
2. **Implement chosen fix**
3. **Test all three data sources** (Binance, Pepperstone, MT5 Exness)
4. **Verify switching between sources** works smoothly

## Files Modified

- `MyRealApp/app/src/main/kotlin/com/trading/app/TradingApp.kt` - Added settings change polling

## Files to Review/Modify

- `MyRealApp/ctrader_bridge.py` - Bridge authentication needs fixing
- `MyRealApp/app/src/main/kotlin/com/trading/app/data/CTraderService.kt` - Direct API alternative
- `MyRealApp/app/src/main/java/com/asc/markets/network/CTraderBridgeClient.kt` - Bridge client
- `MyRealApp/app/src/main/kotlin/com/trading/app/data/PepperstoneChartService.kt` - Chart service
- `MyRealApp/app/src/main/kotlin/com/trading/app/data/Mt5Service.kt` - MT5 service

## Current Status

✅ Settings change detection - FIXED
✅ cTrader CLIENT_ID typo - FIXED  
⚠️ cTrader bridge authentication - NEEDS TESTING (typo fixed, restart bridge to verify)
❓ MT5 bridge connection - NEEDS VERIFICATION

## How to Test the Fixes

### 1. Restart cTrader Bridge
```powershell
# Stop any running bridge
Get-Process python | Where-Object {$_.Path -like "*ctrader*"} | Stop-Process -Force

# Start bridge with corrected credentials
.\start_ctrader_bridge.ps1
```

Expected output:
```
[cTrader] connected: Connected to cTrader Open API demo endpoint
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_selected: Selected cTrader account 47223753
[cTrader] account_authenticated: cTrader account 47223753 authenticated
[cTrader] symbols_loaded: Loaded X broker symbols
[cTrader] subscribed: Subscribed to Y Pepperstone cTrader symbols
```

If you still see "CH_CLIENT_AUTH_FAILURE", the credentials may be for REST API instead of protobuf API.

### 2. Test Data Source Switching
1. Open the app
2. Go to Settings → Chart Type
3. Select Binance → verify USDT pairs display
4. Select Pepperstone → verify forex/commodity pairs display
5. Select Exness → verify MT5 symbols display
6. Check logs for "Chart feed type changed from X to Y"

### 3. Verify Live Data
1. Select Pepperstone in settings
2. Open StreamScreen
3. Verify quotes are updating (not stale)
4. Check timestamp on quotes (should be recent)

## If cTrader Bridge Still Fails

The credentials might be for REST/WebSocket API instead of protobuf API. Options:

**Option A**: Get protobuf API credentials from cTrader
- Log into https://openapi.ctrader.com/
- Create a new application specifically for protobuf API
- Use those credentials instead

**Option B**: Modify bridge to use REST/WebSocket API
- Update `ctrader_bridge.py` to use WebSocket authentication
- Remove protobuf authentication flow
- Use token-based auth instead

**Option C**: Use direct API integration
- Remove bridge dependency
- Use `CTraderService.kt` directly with REST API
- Simpler but less real-time

