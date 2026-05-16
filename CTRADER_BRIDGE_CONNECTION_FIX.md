# cTrader Bridge Connection Fix

## Problem
The Android app was trying to connect directly to cTrader's API servers instead of connecting to the local bridge running on your PC. This is why:
- The bridge showed account balance correctly ($50,000)
- The app received price/candle data (because it was connecting somewhere)
- But the app showed "---" for balance/equity/margin (wrong connection)

## Solution
Modified `CTraderService.kt` to connect to the bridge WebSocket instead of cTrader's API directly.

## Changes Made

### 1. CTraderService.kt
**Before:** Connected to `wss://demo.ctraderapi.com` or `wss://live.ctraderapi.com`
**After:** Connects to `ws://YOUR_PC_IP:8082` (the bridge)

**Key changes:**
- Removed REST API calls to cTrader
- Removed direct WebSocket connection to cTrader
- Now connects to bridge WebSocket at `ws://{CTRADER_BRIDGE_HOST}:{CTRADER_BRIDGE_PORT}`
- Updated message handling to match bridge message format:
  - `"tick"` for price updates (not "QUOTE")
  - `"ACCOUNT"` for account balance (uppercase)
  - `"history"` for candle data
  - `"status"` for bridge status updates
- Added extensive logging to track message flow

### 2. build.gradle.kts
Added new BuildConfig field:
```kotlin
buildConfigField("String", "CTRADER_BRIDGE_HOST", "\"${localProps.getProperty("CTRADER_BRIDGE_HOST") ?: "192.168.1.100"}\"")
```

### 3. local.properties
Added bridge connection settings:
```properties
CTRADER_BRIDGE_HOST=192.168.1.100
CTRADER_BRIDGE_PORT=8082
```

### 4. TradingApp.kt
Fixed compilation error and added logging:
```kotlin
onAccountUpdate = { accountInfo ->
    Log.d("TradingApp", "cTrader account update received: balance=${accountInfo?.balance}, equity=${accountInfo?.equity}, margin=${accountInfo?.margin}")
    mt5AccountInfo = accountInfo
    Log.d("TradingApp", "mt5AccountInfo updated to: $mt5AccountInfo")
}
```

## What You Need To Do

### Step 1: Find Your PC's IP Address
Run this command in PowerShell:
```powershell
ipconfig
```

Look for "IPv4 Address" under your active network adapter (WiFi or Ethernet).
Example: `192.168.1.100`

### Step 2: Update local.properties
Edit `MyRealApp/local.properties` and change this line:
```properties
CTRADER_BRIDGE_HOST=192.168.1.100
```
Replace `192.168.1.100` with your actual PC IP address.

### Step 3: Rebuild the App
In Android Studio:
1. Click **Build** → **Clean Project**
2. Click **Build** → **Rebuild Project**
3. Install the app on your phone

### Step 4: Test
1. Start the bridge on your PC:
   ```powershell
   cd C:\Users\HP\AndroidStudioProjects\MyRealApp
   .\start_ctrader_bridge.ps1
   ```

2. Wait for this message:
   ```
   Account Balance: $50,000.00, Equity: $50,000.00, Margin: $0.00
   ```

3. Open the app on your phone and go to "Pepperstone Live Trade" screen

4. Check logcat for these messages:
   ```
   adb logcat -s CTraderService TradingApp
   ```

   You should see:
   ```
   CTraderService: WebSocket connected to cTrader bridge
   CTraderService: Bridge status: account_authenticated - cTrader account 47312778 authenticated
   CTraderService: ACCOUNT message received: {"type":"ACCOUNT","source":"pepperstone_ctrader",...}
   CTraderService: handleAccountUpdate called with JSON: {...}
   CTraderService: Parsed values - balance: 50000.0, equity: 50000.0, margin: 0.0
   TradingApp: cTrader account update received: balance=50000.0, equity=50000.0, margin=0.0
   TradingApp: mt5AccountInfo updated to: AccountInfo(balance=50000.0, equity=50000.0, margin=0.0, ...)
   ```

## Expected Behavior

### Bridge Output
```
[cTrader] account_authenticated: cTrader account 47312778 authenticated
[DEBUG] Raw trader response: ctidTraderAccountId: 47312778...
Account Balance: $50,000.00, Equity: $50,000.00, Margin: $0.00
[DEBUG] Broadcasting account message: {"type":"ACCOUNT","source":"pepperstone_ctrader","accountId":47312778,"balance":50000.0,"equity":50000.0,"margin":0.0,...}
```

### App Logcat
```
CTraderService: WebSocket connected to cTrader bridge
CTraderService: ACCOUNT message received: {"type":"ACCOUNT",...}
CTraderService: handleAccountUpdate called with JSON: {...}
TradingApp: cTrader account update received: balance=50000.0, equity=50000.0, margin=0.0
```

### App UI
The "Pepperstone Live Trade" screen should show:
- **Balance:** $50,000.00
- **Equity:** $50,000.00
- **Margin:** $0.00

## Troubleshooting

### If app still shows "---":
1. Check your PC's firewall - it must allow incoming connections on port 8082
2. Verify your phone and PC are on the same WiFi network
3. Test the connection:
   ```powershell
   # On PC, check if bridge is listening
   netstat -an | findstr 8082
   ```
   Should show: `0.0.0.0:8082` or `[::]:8082`

4. From your phone's browser, try: `http://YOUR_PC_IP:8082`
   - If it doesn't connect, it's a network/firewall issue

### If logcat shows connection errors:
- Check `CTRADER_BRIDGE_HOST` in `local.properties` matches your PC's IP
- Rebuild the app after changing `local.properties`
- Restart the bridge

### If bridge shows "Port 8082 is already in use":
The bridge is already running. Either:
- Use the existing bridge (it should work)
- Or stop it first: Press Ctrl+C in the PowerShell window

## Message Flow

```
cTrader API → Bridge (Python) → Android App
                ↓
         Converts Protobuf to JSON
         Broadcasts to all clients
                ↓
         {"type":"ACCOUNT","balance":50000.0,...}
                ↓
         CTraderService.handleMessage()
                ↓
         CTraderService.handleAccountUpdate()
                ↓
         onAccountUpdate callback
                ↓
         TradingApp: mt5AccountInfo = accountInfo
                ↓
         PaperTradingPanel displays balance
```

## Files Modified
1. `app/src/main/kotlin/com/trading/app/data/CTraderService.kt` - Connect to bridge instead of cTrader
2. `app/src/main/kotlin/com/trading/app/TradingApp.kt` - Fixed compilation error, added logging
3. `app/build.gradle.kts` - Added CTRADER_BRIDGE_HOST BuildConfig field
4. `local.properties` - Added CTRADER_BRIDGE_HOST and CTRADER_BRIDGE_PORT

## Next Steps
After confirming the balance displays correctly:
1. Test with live trading (if needed)
2. Remove excessive debug logging (optional)
3. Test position updates and order placement
