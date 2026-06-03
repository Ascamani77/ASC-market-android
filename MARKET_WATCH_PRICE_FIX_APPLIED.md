# Market Watch Price Display Fix - Applied

## Changes Made

### 1. Added Diagnostic Logging
Added comprehensive logging to track:
- Which symbols are being processed from AI backend
- Whether live price data is found for each symbol
- Total AI decisions vs final candidates shown
- All pairs available in MarketDataStore and BinanceDataStore

### 2. Use Only Primary Data Sources
Changed to use **only** MarketDataStore and BinanceDataStore (no fallback sources):

**Before:**
```kotlin
val livePair = CombinedFallbackDataStore.pairSnapshot(symbol)
```

**After:**
```kotlin
val livePair = MarketDataStore.pairSnapshot(symbol)
    ?: BinanceDataStore.pairSnapshot(symbol)
```

This ensures we only show data from:
1. **MarketDataStore** - Pepperstone cTrader primary data (FOREX, commodities, indices, crypto)
2. **BinanceDataStore** - Binance USDT pairs only

**No fallback sources** like Deriv, Tiingo, or CombinedFallbackDataStore are used.

### 3. Better UI Feedback
Changed price display to show loading state when price is unavailable:

**Before:**
```kotlin
Text(formatWatchPrice(s.price), ...)  // Shows "0.000000" when no data
Text(String.format(..., s.changePercent), ...)  // Shows "+0.00%" when no data
```

**After:**
```kotlin
if (s.price > 0.0) {
    Text(formatWatchPrice(s.price), color = Color.White, ...)
    Text(String.format(..., s.changePercent), color = EmeraldSuccess/RoseError, ...)
} else {
    Text("Price loading...", color = SlateText, ...)
}
```

## Why This Fixes The Issue

### Root Cause
The Market Watch screen merges two data sources:
1. **AI Backend** - Provides analysis scores and symbols
2. **Price Feeds** - Provides current prices and changes

When price data wasn't available in the primary sources (MarketDataStore/BinanceDataStore), prices showed as 0.0.

### The Fix
1. **Uses only primary data sources** - MarketDataStore (cTrader) and BinanceDataStore
2. **Shows loading state** - Better UX when data isn't available yet
3. **Adds logging** - Makes it easy to debug if issue persists

## Data Source Priority

### MarketDataStore (Primary)
- **Source**: Pepperstone cTrader Bridge
- **Symbols**: FOREX pairs, commodities, indices, crypto (BTC/USD, ETH/USD)
- **Connection**: WebSocket via cTrader bridge

### BinanceDataStore (USDT Pairs)
- **Source**: Binance WebSocket
- **Symbols**: USDT pairs only (BTC/USDT, ETH/USDT, etc.)
- **Connection**: Direct Binance WebSocket

## Testing

### To Verify The Fix:
1. **Ensure cTrader bridge is running**:
   ```powershell
   .\start_ctrader_bridge.ps1
   ```
2. **Run the app** and navigate to Market Watch
3. **Check for**:
   - Prices showing correctly (not 0.0)
   - "Price loading..." message if data not available
   - Price changes showing with correct colors (green/red)

### To Debug If Still Not Working:
1. **Check logcat**:
   ```
   adb logcat -s MarketWatch
   ```
2. **Look for**:
   ```
   MarketWatch: Processing symbol: EURUSD, livePair found: true, price: 1.0845, change: 0.11
   MarketWatch: Total AI decisions: 5, Final candidates: 3
   MarketWatch: MarketDataStore has 15 pairs: EUR/USD=1.0845, GBP/USD=1.2634, ...
   MarketWatch: BinanceDataStore has 5 pairs: BTC/USDT=64209.78, ETH/USDT=3453.76, ...
   ```

### Common Issues:

**Issue 1: No AI Decisions**
```
MarketWatch: Total AI decisions: 0, Final candidates: 0
```
**Solution**: Check AI backend service is running and accessible

**Issue 2: No Price Data in MarketDataStore**
```
MarketWatch: Processing symbol: EURUSD, livePair found: false
MarketWatch: MarketDataStore has 0 pairs:
```
**Solution**: Check cTrader bridge is running and connected:
```powershell
.\start_ctrader_bridge.ps1
```

**Issue 3: No Price Data in BinanceDataStore**
```
MarketWatch: BinanceDataStore has 0 pairs:
```
**Solution**: Check Binance WebSocket connection in ForexViewModel logs

**Issue 4: Symbol Mismatch**
```
MarketWatch: Processing symbol: EUR-USD, livePair found: false
MarketWatch: MarketDataStore has 10 pairs: EUR/USD=1.0845, ...
```
**Solution**: AI backend returning symbols in wrong format (should be EURUSD or EUR/USD)

## Files Modified
- `app/src/main/java/com/asc/markets/ui/screens/MarketWatchScreen.kt`
  - Removed CombinedFallbackDataStore import
  - Changed to use only MarketDataStore and BinanceDataStore
  - Added diagnostic logging for both data stores
  - Added loading state UI for missing prices

## Next Steps
1. Ensure cTrader bridge is running
2. Build and run the app
3. Navigate to Market Watch
4. Verify prices are showing from MarketDataStore/BinanceDataStore only
5. Check logcat if issues persist
