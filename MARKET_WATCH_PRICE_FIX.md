# Market Watch Price Display Issue - Diagnosis & Fix

## Problem
The Market Watch page is not showing prices and price changes for the pre-move candidates.

## Root Cause Analysis

### Data Flow
1. **AI Backend** → Provides `FinalDecisionItem` with analysis scores and `asset_1` (symbol)
2. **CombinedFallbackDataStore** → Provides live price data for symbols
3. **MarketWatchScreen** → Merges AI data + live prices into `PreMoveCandidate` objects

### The Issue
The `toPreMoveCandidate()` function in MarketWatchScreen.kt merges AI backend data with live price data:

```kotlin
private fun FinalDecisionItem.toPreMoveCandidate(livePair: ForexPair?): PreMoveCandidate {
    // ...
    return PreMoveCandidate(
        // ...
        price = livePair?.price ?: 0.0,  // ← Falls back to 0.0 if no live data
        changePercent = livePair?.changePercent ?: 0.0,  // ← Falls back to 0.0
        // ...
    )
}
```

**When `livePair` is null (no price data available), prices show as 0.0**

### Why is `livePair` null?

Possible reasons:
1. **Symbol mismatch** - AI backend returns symbols in a format that doesn't match CombinedFallbackDataStore
2. **No price data yet** - Price feeds haven't populated CombinedFallbackDataStore when MarketWatch loads
3. **Price feed not running** - Deriv, cTrader, or Tiingo websockets aren't connected

### Price Feed Priority
From `ForexViewModel.kt` init block:
1. **Primary**: Pepperstone cTrader Bridge (via `cTraderBridgeClient`)
2. **Secondary**: Deriv WebSocket (for FOREX, commodities, indices)
3. **Fallback**: Tiingo FX/IEX WebSocket (only if Deriv fails)
4. **Last Resort**: Tiingo REST API (polling every hour)

## Diagnostic Steps Added

Added logging to MarketWatchScreen.kt:
```kotlin
// Log each symbol processing
android.util.Log.d("MarketWatch", "Processing symbol: $symbol, livePair found: ${livePair != null}, price: ${livePair?.price}, change: ${livePair?.changePercent}")

// Log final candidate count
android.util.Log.d("MarketWatch", "Total AI decisions: ${aiDecisions.size}, Final candidates: ${candidates.size}")

// Log all available pairs in CombinedFallbackDataStore
android.util.Log.d("MarketWatch", "CombinedFallbackDataStore has ${allPairs.size} pairs: ${allPairs.map { "${it.symbol}=${it.price}" }.joinToString(", ")}")
```

## How to Debug

1. **Run the app** and navigate to Market Watch
2. **Check logcat** for "MarketWatch" tags:
   ```
   adb logcat -s MarketWatch
   ```
3. **Look for**:
   - How many AI decisions are received
   - Which symbols are being processed
   - Whether `livePair` is found for each symbol
   - What pairs are in CombinedFallbackDataStore

## Expected Findings

### Scenario A: Symbol Mismatch
```
MarketWatch: Processing symbol: EURUSD, livePair found: false, price: null, change: null
MarketWatch: CombinedFallbackDataStore has 10 pairs: EUR/USD=1.0845, GBP/USD=1.2634, ...
```
**Fix**: Symbol normalization issue - AI returns `EURUSD` but store has `EUR/USD`

### Scenario B: No Price Data
```
MarketWatch: Processing symbol: EURUSD, livePair found: false, price: null, change: null
MarketWatch: CombinedFallbackDataStore has 0 pairs:
```
**Fix**: Price feeds not running or not connected

### Scenario C: No AI Data
```
MarketWatch: Total AI decisions: 0, Final candidates: 0
```
**Fix**: AI backend not returning data or API connection issue

## Potential Fixes

### Fix 1: Ensure Price Feeds Are Running
Check that these services are started in `ForexViewModel.kt`:
- `derivService.connect()` ✓
- `cTraderBridgeClient.connect(cTraderSymbols)` ✓
- `tiingoFxManager?.connect(tiingoForexSymbols)` ✓ (fallback)

### Fix 2: Add Fallback Display
Show a placeholder when price data is unavailable:
```kotlin
Text(
    text = if (s.price > 0.0) formatWatchPrice(s.price) else "Loading...",
    color = if (s.price > 0.0) Color.White else SlateText,
    fontSize = 14.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFamily = InterFontFamily
)
```

### Fix 3: Wait for Price Data Before Showing
Add a loading state that waits for both AI data AND price data:
```kotlin
val hasPriceData = allPairs.isNotEmpty()
val hasAiData = aiDecisions.isNotEmpty()

if (!hasPriceData || !hasAiData) {
    // Show loading indicator
} else {
    // Show candidates
}
```

### Fix 4: Use Alternative Price Source
If CombinedFallbackDataStore is empty, try MarketDataStore or BinanceDataStore:
```kotlin
val livePair = CombinedFallbackDataStore.pairSnapshot(symbol)
    ?: MarketDataStore.pairSnapshot(symbol)
    ?: BinanceDataStore.pairSnapshot(symbol)
```

## Next Steps

1. **Run the app with logging** to identify which scenario is occurring
2. **Check backend AI service** - ensure it's running and returning data
3. **Check price feed services** - ensure Deriv/cTrader/Tiingo are connected
4. **Implement appropriate fix** based on findings

## Files Modified
- `app/src/main/java/com/asc/markets/ui/screens/MarketWatchScreen.kt` - Added diagnostic logging
