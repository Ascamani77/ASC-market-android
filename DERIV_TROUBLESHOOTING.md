# Deriv Integration Troubleshooting

## Issue: Commodities Not Showing Live Data

### Root Cause
The DerivService was implemented but **never initialized or connected** in the main app. The service needs to be created and connected when the app starts.

### Solution Applied ✅

**File**: `app/src/main/kotlin/com/trading/app/TradingApp.kt`

Added DerivService initialization after MT5Service:

```kotlin
// Deriv WebSocket Service for commodities and crypto
val derivService = remember {
    com.trading.app.data.DerivService(
        onQuoteUpdate = { quote ->
            // Propagate Deriv price updates to PriceStreamManager
            PriceStreamManager.updatePrice(quote.name, quote.lastPrice.toDouble())
            android.util.Log.d("DerivService", "Updated ${quote.name}: ${quote.lastPrice}")
        },
        onHistoryUpdate = { _, _ -> }
    )
}

// Connect Deriv and subscribe to commodities
LaunchedEffect(Unit) {
    derivService.connect()
    delay(1000) // Wait for connection
    
    // Subscribe to commodities (using FOREX_PAIRS format)
    derivService.subscribe("XAU/USD")  // Gold
    derivService.subscribe("XAG/USD")  // Silver
    derivService.subscribe("USOIL")    // WTI Crude
    
    // Also subscribe to crypto for backup
    derivService.subscribe("BTC/USD")
    derivService.subscribe("ETH/USD")
    
    android.util.Log.i("TradingApp", "Deriv service connected and subscribed to commodities")
}

// Cleanup Deriv on dispose
DisposableEffect(Unit) {
    onDispose {
        derivService.disconnect()
    }
}
```

### Symbol Mapping Updated ✅

**File**: `app/src/main/kotlin/com/trading/app/data/DerivService.kt`

Updated symbol mapping to match FOREX_PAIRS format:

```kotlin
private val symbolMapping = mapOf(
    "frxBTCUSD" to "BTC/USD",
    "frxETHUSD" to "ETH/USD",
    "frxXAUUSD" to "XAU/USD", // Gold
    "frxXAGUSD" to "XAG/USD", // Silver
    "frxBROUSD" to "BROUSD",
    "frxWTIOUSD" to "USOIL"   // WTI Crude Oil
)

private val reverseSymbolMapping = buildMap {
    symbolMapping.forEach { (derivSymbol, appSymbol) ->
        put(appSymbol, derivSymbol)
        // Also map without slashes
        put(appSymbol.replace("/", ""), derivSymbol)
    }
}
```

## Verification Steps

### 1. Build and Run
```bash
cd MyRealApp
./gradlew clean build
./gradlew installDebug
```

### 2. Check Logs
```bash
adb logcat | grep -E "DerivService|TradingApp"
```

Expected output:
```
TradingApp: Deriv service connected and subscribed to commodities
DerivService: Connecting to Deriv WebSocket...
DerivService: Deriv WebSocket Connected
DerivService: Subscribed to frxXAUUSD (app: XAU/USD)
DerivService: Subscribed to frxXAGUSD (app: XAG/USD)
DerivService: Subscribed to frxWTIOUSD (app: USOIL)
DerivService: Tick: XAU/USD = 2342.50
DerivService: Updated XAU/USD: 2342.50
```

### 3. Check Market Overview
1. Open the app
2. Navigate to Market Overview
3. Switch to "Commodities" tab
4. Verify prices are updating in real-time

### 4. Check PriceStreamManager
The prices should flow through:
```
Deriv WebSocket
    ↓
DerivService.onQuoteUpdate
    ↓
PriceStreamManager.updatePrice
    ↓
MarketDataStore.updatePair
    ↓
UI updates (Market Overview, Charts, etc.)
```

## Common Issues

### Issue: No Connection
**Symptom**: No "Deriv WebSocket Connected" in logs

**Solutions**:
1. Check internet connection
2. Verify firewall allows WebSocket connections
3. Check if Deriv API is up: https://deriv.statuspage.io/
4. Try restarting the app

### Issue: No Price Updates
**Symptom**: Connected but no tick updates

**Solutions**:
1. Check symbol mapping matches FOREX_PAIRS
2. Verify subscriptions are sent (check logs)
3. Ensure PriceStreamManager is receiving updates
4. Check MarketDataStore.matchesSymbol() logic

### Issue: Wrong Symbols
**Symptom**: Prices update but for wrong symbols

**Solutions**:
1. Verify symbol mapping in DerivService
2. Check FOREX_PAIRS uses correct format
3. Update reverseSymbolMapping if needed

### Issue: Prices Not Showing in UI
**Symptom**: Logs show updates but UI doesn't change

**Solutions**:
1. Check MarketDataStore.updatePair() is called
2. Verify UI observes MarketDataStore.allPairs
3. Check symbol normalization in MarketDataStore
4. Ensure StateFlow is being collected

## Debug Commands

### Check if Deriv is connected
```kotlin
if (derivService.isConnected()) {
    Log.d("Debug", "Deriv is connected")
}
```

### Check current prices
```kotlin
val price = PriceStreamManager.getPrice("XAU/USD")
Log.d("Debug", "Gold price: $price")
```

### Check MarketDataStore
```kotlin
val pair = MarketDataStore.pairSnapshot("XAU/USD")
Log.d("Debug", "Gold pair: ${pair?.price}")
```

### Force update
```kotlin
PriceStreamManager.updatePrice("XAU/USD", 2350.0)
```

## Testing Checklist

- [ ] App builds successfully
- [ ] DerivService connects on app start
- [ ] Subscriptions are sent for all commodities
- [ ] Tick updates appear in logs
- [ ] PriceStreamManager receives updates
- [ ] MarketDataStore is updated
- [ ] UI shows live prices
- [ ] Prices update in real-time (< 1 second)

## Performance Monitoring

### Check Latency
```bash
adb logcat | grep "DerivService: Tick"
```

Expected: < 100ms from server to app

### Check Memory
```bash
adb shell dumpsys meminfo com.asc.markets
```

DerivService should use < 5MB

### Check CPU
```bash
adb shell top | grep com.asc.markets
```

Should be < 5% when idle

## Rollback (If Needed)

If issues persist, you can temporarily disable Deriv:

```kotlin
// Comment out in TradingApp.kt
/*
val derivService = remember { ... }
LaunchedEffect(Unit) { ... }
DisposableEffect(Unit) { ... }
*/
```

## Additional Resources

- **DerivService Code**: `app/src/main/kotlin/com/trading/app/data/DerivService.kt`
- **Integration Guide**: `DERIV_INTEGRATION.md`
- **Quick Start**: `DERIV_QUICK_START.md`
- **Deriv API Docs**: https://api.deriv.com
- **API Status**: https://deriv.statuspage.io/

## Support

If issues persist after following this guide:
1. Check all logs for errors
2. Verify network connectivity
3. Test with example screen: `DerivIntegrationExample`
4. Review symbol mapping carefully
5. Check Deriv API status page

---

**Last Updated**: 2026-05-07  
**Status**: ✅ Fixed - DerivService now initialized and connected
