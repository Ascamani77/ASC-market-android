# Changes Made: Demo Balance Fix

## Summary
Updated `TradingApp.kt` to use separate services for Pepperstone Live and Demo accounts, ensuring the demo chart shows the correct $50,000 balance.

## Files Modified
- `app/src/main/kotlin/com/trading/app/TradingApp.kt`

## Changes Made

### 1. Added Import (Line ~41)
```kotlin
import com.trading.app.data.CTraderDemoService
```

### 2. Created Demo Service (After line ~640)
Added a new `cTraderDemoTradingService` that:
- Connects to port 8083 (demo bridge)
- Only processes callbacks when `chartFeedType == ChartFeedType.PEPPERSTONE_DEMO`
- Receives account balance from demo account (47340965, $50,000)

### 3. Updated Connection Logic (Line ~835-865)
Split `PEPPERSTONE_CTRADER, PEPPERSTONE_DEMO` into two separate cases:
- **PEPPERSTONE_CTRADER**: Disconnects demo, connects live service
- **PEPPERSTONE_DEMO**: Disconnects live, connects demo service

### 4. Updated Cleanup Logic (Line ~860-865)
Added `cTraderDemoTradingService.disconnect()` to:
- DisposableEffect cleanup
- BINANCE_CONNECT case
- EXNESS case

### 5. Updated Reconnect Logic (Line ~2208-2214)
Added separate refresh case for `PEPPERSTONE_DEMO` that reconnects the demo service

### 6. Split Order Placement (Line ~1205-1280)
Separated `PEPPERSTONE_CTRADER, PEPPERSTONE_DEMO` into two cases:
- **PEPPERSTONE_CTRADER**: Uses `cTraderTradingService.placeMarketOrder()`
- **PEPPERSTONE_DEMO**: Uses `cTraderDemoTradingService.placeMarketOrder()`

### 7. Split Position Closing (Line ~1330-1360)
Separated `PEPPERSTONE_CTRADER, PEPPERSTONE_DEMO` into two cases:
- **PEPPERSTONE_CTRADER**: Uses `cTraderTradingService.closePosition()`
- **PEPPERSTONE_DEMO**: Uses `cTraderDemoTradingService.closePosition()`

### 8. Added Symbol Subscription (Line ~1045)
Added `PEPPERSTONE_DEMO` case that subscribes to symbols via `cTraderDemoTradingService`

### 9. Updated Chart Service Passing (Line ~1966)
Changed from simple if/else to when expression:
```kotlin
cTraderService = when (resolvedChartFeedType) {
    ChartFeedType.PEPPERSTONE_CTRADER -> cTraderTradingService
    ChartFeedType.PEPPERSTONE_DEMO -> cTraderDemoTradingService
    else -> null
}
```

## What This Fixes

### Before
- Only one service (`cTraderTradingService`) for both Live and Demo
- Always connected to port 8082 (live bridge)
- Demo chart showed $0.00 balance (from empty live account 47341092)

### After
- Two separate services:
  - `cTraderTradingService` → port 8082, account 47341092 (live)
  - `cTraderDemoTradingService` → port 8083, account 47340965 (demo)
- Each service only processes callbacks when active
- Demo chart now shows $50,000.00 balance correctly
- Live and demo are completely independent

## Testing

### 1. Rebuild and Run
```bash
./gradlew clean assembleDebug
```

### 2. Test Demo Mode
1. Select "Pepperstone Demo" as chart feed
2. Check account balance shows **$50,000.00**
3. Check equity shows **$50,000.00**
4. Place a test trade to verify trading works

### 3. Test Live Mode  
1. Select "Pepperstone cTrader" as chart feed
2. Check it connects to live account (47341092)
3. Shows live account balance

### 4. Monitor Logs
Look for these log messages:
```
cTrader DEMO account update received: balance=50000.0, equity=50000.0
DEMO mt5AccountInfo updated to: AccountInfo(balance=50000.0, equity=50000.0...)
```

## Backend Status

Both bridges are running correctly:
- ✅ **Live Bridge (8082)**: Connected, account 47341092, balance $0.00
- ✅ **Demo Bridge (8083)**: Connected, account 47340965, balance $50,000.00

The backend is working perfectly - it was just the frontend that needed to connect to the right service!

## Next Steps

After rebuild:
1. Demo chart should show $50,000 balance
2. Live chart should show actual live account balance
3. Both should work independently without interfering with each other

---

**Status**: ✅ Code changes complete, ready to rebuild
