# Debug Accumulation Radar - N/A Issue

## Problem
- Shows data in FALLBACK mode ✅
- Shows "N/A" and "NO DATA" in EA LIVE mode ❌

## Debug Logs Added

### 1. In `buildAccumulationRadarItems()`
```kotlin
android.util.Log.d("CurrencyStrength", "Building accumulation radar with ${uniquePairs.size} unique pairs")
android.util.Log.d("CurrencyStrength", "First 3 pairs: ${uniquePairs.take(3).map { ... }}")
android.util.Log.d("CurrencyStrength", "priceHistory size: ${priceHistory.size}")
android.util.Log.d("CurrencyStrength", "  ${pair.symbol}: priceScore=${priceScore}, changePercent=${dayChangePercent}")
```

### 2. In `TopMoverRow()`
```kotlin
android.util.Log.d("TopMoverRow", "Symbol: ${pair.symbol}, Change: ${item.changePercent}, Score: ${item.accumulationScore}, Price: ${pair.price}")
```

### 3. Added Null/Invalid Checks
```kotlin
// Shows "N/A" if changePercent is NaN, Infinity, or invalid
text = if (item.changePercent.isFinite()) {
    String.format(Locale.US, "%+.2f%%", item.changePercent)
} else {
    "N/A"
}

// Shows "N/A" if score is 0 or invalid
text = if (item.accumulationScore.isFinite() && item.accumulationScore > 0) {
    String.format(Locale.US, "%.0f", item.accumulationScore * 100)
} else {
    "N/A"
}

// Shows "NO DATA" if price is 0 or invalid
text = if (pair.price.isFinite() && pair.price > 0) {
    String.format(Locale.US, "%.Xf", pair.price)
} else {
    "NO DATA"
}
```

## How to Debug

### Step 1: Rebuild App
```
Build > Clean Project
Build > Rebuild Project
Run on phone
```

### Step 2: Open Logcat
**Filter 1:** `CurrencyStrength`  
**Filter 2:** `TopMoverRow`

### Step 3: Check Logs

**Expected for EA LIVE mode:**
```
D/CurrencyStrength: allPairs count: 47
D/CurrencyStrength: After context filter: 47
D/CurrencyStrength: Sample symbols: [BCHUSDm, BTCUSDm, EURUSDm, ...]
D/CurrencyStrength: Building accumulation radar with 47 unique pairs (EA mode)
D/CurrencyStrength: First 3 pairs: [BCHUSDm(price=203.27, change=0.00%), BTCUSDm(price=94235.50, change=-0.82%), ...]
D/CurrencyStrength: priceHistory size: 47, timedPriceHistory size: 47
D/CurrencyStrength:   BCHUSDm: sampledPrices=20, priceScore=0.45, changePercent=0.00
D/CurrencyStrength:   BTCUSDm: sampledPrices=20, priceScore=0.78, changePercent=-0.82
D/CurrencyStrength: Top 5 accumulation items: [BTCUSDm(score=0.78, change=-0.82%), EURUSDm(score=0.72, change=1.25%), ...]
D/TopMoverRow: Symbol: BTCUSDm, Change: -0.82, Score: 0.78, Price: 94235.50
```

**If you see this (BAD):**
```
D/CurrencyStrength: First 3 pairs: [BCHUSDm(price=0.0, change=NaN%), ...]
D/CurrencyStrength: priceHistory size: 0, timedPriceHistory size: 0
D/CurrencyStrength:   BCHUSDm: sampledPrices=0, priceScore=0.0, changePercent=NaN
D/TopMoverRow: Symbol: BCHUSDm, Change: NaN, Score: 0.0, Price: 0.0
```
**This means:** EA data isn't being converted to `ForexPair` correctly!

## Possible Issues

### Issue 1: EALiveDataStore.toForexPairs() Not Working

**Check:** `EALiveDataStore.kt` line ~135

The function converts EA JSON data to `ForexPair`:
```kotlin
fun toForexPairs(): List<ForexPair> {
    return _liveAssets.value.map { asset ->
        val m1 = asset.m1
        ForexPair(
            symbol = asset.symbol,
            name = asset.symbol,
            price = asset.prices.last,  // ← Using EA price
            change = (m1.close - m1.open),
            changePercent = m1.changePercent ?: 0.0,  // ← Using EA change%
            category = inferCategory(asset.symbol)
        )
    }
}
```

**If this returns empty or wrong data, Accumulation Radar will show N/A**

### Issue 2: UnifiedMarketDataStore Not Using EA Data

**Check:** Is `dataSource` actually `MT5_EA`?

In Logcat, look for:
```
D/UnifiedMarketData: ✅ Using MT5 EA data: 47 assets
```

If you see:
```
D/UnifiedMarketData: ⚠️ Falling back to Pepperstone data
```
**Then EA data isn't reaching UnifiedMarketDataStore!**

### Issue 3: Price History Empty for EA

EA provides M1, M5, H1 OHLCV data, but:
- `priceHistory` might not be populated
- `timedPriceHistory` might be empty

**Check in Logcat:**
```
D/CurrencyStrength: priceHistory size: X, timedPriceHistory size: Y
```

If both are 0, then `EALiveDataStore.getPriceHistory()` and `getTimedPriceHistory()` aren't working.

## Quick Fix Checks

### 1. Verify EA Data is Actually Loading
```kotlin
// In EALiveDataStore.kt, add logging:
android.util.Log.d("EALiveDataStore", "✅ Fetched ${data.assets.size} assets")
data.assets.take(3).forEach { asset ->
    android.util.Log.d("EALiveDataStore", "  ${asset.symbol}: bid=${asset.prices.bid}, last=${asset.prices.last}")
}
```

### 2. Verify toForexPairs() Works
```kotlin
// In UnifiedMarketDataStore.kt updateFromEA():
val pairs = EALiveDataStore.toForexPairs()
android.util.Log.d("UnifiedMarketData", "toForexPairs() returned ${pairs.size} pairs")
pairs.take(3).forEach { pair ->
    android.util.Log.d("UnifiedMarketData", "  ${pair.symbol}: price=${pair.price}, change=${pair.changePercent}%")
}
_allPairs.value = pairs
```

### 3. Check If accumulationRadarScore Returns 0
```kotlin
// In CurrencyStrengthPanel.kt:
val priceScore = accumulationRadarScore(sampledPrices, dayChangePercent, timeframe)
android.util.Log.d("CurrencyStrength", "  ${pair.symbol}: priceScore=${priceScore} (sampledPrices=${sampledPrices.size}, change=${dayChangePercent}%)")
```

If `priceScore` is always 0.0, then `accumulationRadarScore()` function has a bug.

## Expected Outcome After Debug

**Working logs:**
```
D/EALiveDataStore: ✅ Fetched 47 assets from EA (Direct JSON)
D/EALiveDataStore:   BCHUSDm: bid=203.27, last=203.27
D/UnifiedMarketData: ✅ Using MT5 EA data: 47 assets
D/UnifiedMarketData: toForexPairs() returned 47 pairs
D/UnifiedMarketData:   BCHUSDm: price=203.27, change=0.0%
D/CurrencyStrength: allPairs count: 47
D/CurrencyStrength: priceHistory size: 47, timedPriceHistory size: 47
D/CurrencyStrength:   BCHUSDm: priceScore=0.45, changePercent=0.0
D/TopMoverRow: Symbol: BCHUSDm, Change: 0.0, Score: 0.45, Price: 203.27
```

**App displays:**
```
BCHUSDm     +0.00%   45   203.27
CRYPTO      Change   Score Price
```

## Next Steps

1. **Rebuild app** with new logging
2. **Open Logcat** and filter by `CurrencyStrength`, `EALiveDataStore`, `UnifiedMarketData`, `TopMoverRow`
3. **Check the logs** - share them if still showing N/A
4. **Identify which step is failing** (data fetch, conversion, scoring, or display)

---

**Most likely issue:** `EALiveDataStore.toForexPairs()` or `getPriceHistory()` isn't working correctly for EA nested JSON structure.
