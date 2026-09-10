# Complete EA Migration Guide: Replace All Data Sources

This guide shows you how to **completely replace** Pepperstone, Binance, and all other data sources with your **MT5 EA live feed**.

---

## Architecture Change

### Before (Multiple Sources):
```
App
├── MarketDataStore (Pepperstone API)
├── BinanceDataStore (Binance API)  
├── CombinedFallbackDataStore (Fallback)
└── AIContextService (Backend AI)
```

### After (Single Source):
```
App
├── EALiveDataStore (MT5 EA) ← ALL MARKET DATA
└── AIContextService (Backend AI) ← AI SCORES ONLY
```

---

## Step 1: Implement EALiveDataStore

Already provided in previous guide. Make sure you have:
- ✅ `EALiveDataStore.kt` created
- ✅ `EALiveDataStore.start()` in `MyApp.onCreate()`

---

## Step 2: Create Unified Market Data Manager

Create: `app/src/main/java/com/asc/markets/data/UnifiedMarketDataStore.kt`

```kotlin
package com.asc.markets.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * Unified Market Data Store - Single source of truth for all market data.
 * Prioritizes EA data, falls back to Pepperstone if EA unavailable.
 */
object UnifiedMarketDataStore {
    private const val TAG = "UnifiedMarketData"
    
    // Primary data source state
    private val _dataSource = MutableStateFlow(DataSource.LOADING)
    val dataSource: StateFlow<DataSource> = _dataSource
    
    // Unified pairs list
    private val _allPairs = MutableStateFlow<List<ForexPair>>(emptyList())
    val allPairs: StateFlow<List<ForexPair>> = _allPairs
    
    // Price history (symbol -> prices)
    private val _priceHistory = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Double>>> = _priceHistory
    
    // Timed price history (symbol -> timed prices)
    private val _timedPriceHistory = MutableStateFlow<Map<String, List<TimedPrice>>>(emptyMap())
    val timedPriceHistory: StateFlow<Map<String, List<TimedPrice>>> = _timedPriceHistory
    
    init {
        // Combine EA connection state with data
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            combine(
                EALiveDataStore.isConnected,
                EALiveDataStore.liveAssets
            ) { eaConnected, eaAssets ->
                if (eaConnected && eaAssets.isNotEmpty()) {
                    _dataSource.value = DataSource.MT5_EA
                    updateFromEA(eaAssets)
                    Log.d(TAG, "Using MT5 EA data: ${eaAssets.size} assets")
                } else {
                    _dataSource.value = DataSource.PEPPERSTONE_FALLBACK
                    updateFromFallback()
                    Log.d(TAG, "Falling back to Pepperstone data")
                }
            }.collect {}
        }
    }
    
    private fun updateFromEA(eaAssets: List<EAAssetData>) {
        // Convert EA assets to ForexPair format
        val pairs = eaAssets.map { asset ->
            ForexPair(
                symbol = asset.symbol,
                name = asset.symbol,
                price = asset.prices.last,
                change = asset.m1.close - asset.m1.open,
                changePercent = asset.m1.changePercent ?: 0.0,
                category = inferCategory(asset.symbol)
            )
        }
        _allPairs.value = pairs
        
        // Build price history from M1 data
        val historyMap = mutableMapOf<String, List<Double>>()
        val timedHistoryMap = mutableMapOf<String, List<TimedPrice>>()
        
        eaAssets.forEach { asset ->
            // Simple history using current OHLC
            historyMap[asset.symbol] = listOf(
                asset.m1.open,
                asset.m1.high,
                asset.m1.low,
                asset.m1.close
            )
            
            // Timed history
            timedHistoryMap[asset.symbol] = listOf(
                TimedPrice(
                    timestampMillis = asset.timestamp,
                    price = asset.m1.close
                )
            )
        }
        
        _priceHistory.value = historyMap
        _timedPriceHistory.value = timedHistoryMap
    }
    
    private fun updateFromFallback() {
        // Combine existing data sources as fallback
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            combine(
                MarketDataStore.allPairs,
                BinanceDataStore.allPairs,
                CombinedFallbackDataStore.allPairs
            ) { market, binance, fallback ->
                val combined = (market + binance + fallback).distinctBy { it.symbol }
                _allPairs.value = combined
            }.collect {}
            
            // Combine price histories
            combine(
                MarketDataStore.priceHistory,
                BinanceDataStore.priceHistory,
                CombinedFallbackDataStore.priceHistory
            ) { market, binance, fallback ->
                _priceHistory.value = market + binance + fallback
            }.collect {}
            
            combine(
                MarketDataStore.timedPriceHistory,
                BinanceDataStore.timedPriceHistory,
                CombinedFallbackDataStore.timedPriceHistory
            ) { market, binance, fallback ->
                _timedPriceHistory.value = market + binance + fallback
            }.collect {}
        }
    }
    
    private fun inferCategory(symbol: String): MarketCategory {
        return when {
            symbol.contains("BTC", ignoreCase = true) ||
            symbol.contains("ETH", ignoreCase = true) ||
            symbol.contains("DOGE", ignoreCase = true) ||
            symbol.contains("USDT", ignoreCase = true) -> MarketCategory.CRYPTO
            
            symbol.contains("XAU", ignoreCase = true) ||
            symbol.contains("XAG", ignoreCase = true) ||
            symbol.contains("OIL", ignoreCase = true) ||
            symbol.contains("BRENT", ignoreCase = true) -> MarketCategory.COMMODITIES
            
            symbol.contains("US30", ignoreCase = true) ||
            symbol.contains("SPX", ignoreCase = true) ||
            symbol.contains("NAS", ignoreCase = true) ||
            symbol.contains("DOW", ignoreCase = true) -> MarketCategory.INDICES
            
            symbol.matches(Regex("[A-Z]{6}")) -> MarketCategory.FOREX
            
            symbol.length == 4 && symbol.all { it.isLetter() } -> MarketCategory.STOCK
            
            else -> MarketCategory.FOREX
        }
    }
    
    /**
     * Get live price for a specific symbol
     */
    fun getLivePrice(symbol: String): Double? {
        return _allPairs.value.find { it.symbol == symbol }?.price
    }
    
    /**
     * Get all symbols for a category
     */
    fun getSymbolsByCategory(category: MarketCategory): List<ForexPair> {
        return _allPairs.value.filter { it.category == category }
    }
}

enum class DataSource {
    LOADING,
    MT5_EA,
    PEPPERSTONE_FALLBACK
}

data class TimedPrice(
    val timestampMillis: Long,
    val price: Double
)
```

---

## Step 3: Update All Screens to Use Unified Store

### 3.1 Update CurrencyStrengthPanel.kt

**Replace:**
```kotlin
val marketPairs by MarketDataStore.allPairs.collectAsState()
val binancePairs by BinanceDataStore.allPairs.collectAsState()
val fallbackPairs by CombinedFallbackDataStore.allPairs.collectAsState()
val allPairs = (marketPairs + binancePairs + fallbackPairs).distinctBy { it.symbol }
```

**With:**
```kotlin
val allPairs by UnifiedMarketDataStore.allPairs.collectAsState()
val dataSource by UnifiedMarketDataStore.dataSource.collectAsState()
```

**Same for price history:**
```kotlin
val priceHistory by UnifiedMarketDataStore.priceHistory.collectAsState()
val timedPriceHistory by UnifiedMarketDataStore.timedPriceHistory.collectAsState()
```

### 3.2 Update MarketOverviewTab.kt

**Replace all data source references:**
```kotlin
// OLD
val marketPairs by MarketDataStore.allPairs.collectAsState()
val binancePairs by BinanceDataStore.allPairs.collectAsState()

// NEW
val allPairs by UnifiedMarketDataStore.allPairs.collectAsState()
```

### 3.3 Update Any Other Screen Using Market Data

Search for these patterns and replace:
```kotlin
// Pattern to find:
MarketDataStore.allPairs
BinanceDataStore.allPairs
CombinedFallbackDataStore.allPairs

// Replace with:
UnifiedMarketDataStore.allPairs
```

---

## Step 4: Add Data Source Indicator (Optional but Recommended)

Add visual indicator showing which data source is active:

```kotlin
@Composable
fun DataSourceIndicator() {
    val dataSource by UnifiedMarketDataStore.dataSource.collectAsState()
    val assetCount by UnifiedMarketDataStore.allPairs.collectAsState()
    
    Surface(
        color = when (dataSource) {
            DataSource.MT5_EA -> Color(0xFF10B981) // Green
            DataSource.PEPPERSTONE_FALLBACK -> Color(0xFFF59E0B) // Orange
            DataSource.LOADING -> Color(0xFF6B7280) // Gray
        }.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        when (dataSource) {
                            DataSource.MT5_EA -> Color(0xFF10B981)
                            DataSource.PEPPERSTONE_FALLBACK -> Color(0xFFF59E0B)
                            DataSource.LOADING -> Color(0xFF6B7280)
                        },
                        CircleShape
                    )
            )
            Text(
                text = when (dataSource) {
                    DataSource.MT5_EA -> "EA LIVE • ${assetCount.size} assets"
                    DataSource.PEPPERSTONE_FALLBACK -> "FALLBACK • ${assetCount.size} assets"
                    DataSource.LOADING -> "LOADING..."
                },
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Add to your dashboard:
@Composable
fun DashboardScreen() {
    Column {
        DataSourceIndicator() // ← Add at top
        // ... rest of dashboard
    }
}
```

---

## Step 5: (Optional) Disable Old Data Sources

Once EA is working, you can disable the old sources to save resources:

```kotlin
// In MyApp.kt
override fun onCreate() {
    super.onCreate()
    
    // Start AI Context Service
    AIContextService.start()
    
    // Start EA Live Data Service (PRIMARY)
    EALiveDataStore.start()
    
    // DISABLE old sources (uncomment when EA is stable)
    // MarketDataStore.stop()
    // BinanceDataStore.stop()
    // CombinedFallbackDataStore.stop()
}
```

---

## Step 6: Testing Checklist

### ✅ EA Side
- [ ] EA attached to MT5 chart
- [ ] LiveDataStreamer enabled
- [ ] JSON file being created every 10 seconds
- [ ] File contains 20+ assets

### ✅ Backend Side
- [ ] Backend running at `http://10.164.138.133:8000`
- [ ] `/live-market-data` endpoint returns data
- [ ] `curl` test successful

### ✅ App Side
- [ ] `EALiveDataStore.start()` called in `MyApp`
- [ ] All screens updated to use `UnifiedMarketDataStore`
- [ ] Data source indicator shows "EA LIVE"
- [ ] Accumulation Radar shows all assets
- [ ] Charts display correct prices
- [ ] Price updates every 10 seconds

---

## What You Gain

### ✅ Benefits

1. **Unlimited Assets**
   - Before: 8-10 assets (Pepperstone limit)
   - After: 50+ assets (EA limit = your Market Watch size)

2. **Zero API Costs**
   - Before: Pepperstone API rate limits
   - After: No limits, no costs

3. **Perfect Data Sync**
   - EA and app see identical prices
   - Same data source EA trades on

4. **Multi-Timeframe**
   - M1, M5, H1 all available
   - No additional API calls needed

5. **Simpler Architecture**
   - One data source instead of three
   - Less code to maintain
   - Faster data updates

### ⚠️ Requirements

1. **Desktop Must Run**
   - MT5 needs to be open
   - Can be minimized, but must run
   - Consider VPS for 24/7

2. **Backend Must Run**
   - `ai_api.py` must be active
   - Could also run on VPS

3. **Same Network**
   - App must reach backend IP
   - Use VPN if on different networks

---

## Fallback Strategy (Recommended)

Keep Pepperstone as emergency fallback:

```kotlin
// In UnifiedMarketDataStore
private fun updateFromEA(eaAssets: List<EAAssetData>) {
    if (eaAssets.isEmpty()) {
        Log.w(TAG, "EA returned empty data, falling back")
        _dataSource.value = DataSource.PEPPERSTONE_FALLBACK
        updateFromFallback()
        return
    }
    
    // ... normal EA processing
}
```

This way:
- ✅ **Desktop ON** → App uses EA data (50+ assets, unlimited)
- ✅ **Desktop OFF** → App uses Pepperstone (8-10 assets, limited)
- ✅ **Always functional** → App never breaks

---

## Migration Steps Summary

1. ✅ Create `EALiveDataStore.kt`
2. ✅ Create `UnifiedMarketDataStore.kt`
3. ✅ Update all screens to use unified store
4. ✅ Add data source indicator
5. ✅ Test with EA running
6. ✅ Test with EA stopped (fallback)
7. ✅ Disable old sources when stable

---

## Final Answer to Your Question

**YES!** Your entire app can get live data from your EA without needing Pepperstone.

**Every screen, every feature, every price** will come from your MT5 EA:
- ✅ Dashboard
- ✅ Market Watch
- ✅ Charts
- ✅ Accumulation Radar
- ✅ Trade Ledger
- ✅ Scalping
- ✅ Alerts
- ✅ Everything

The only dependency is your **desktop with MT5 must be running**. But you can keep Pepperstone as a fallback for when you're away from your desktop.

**Result:** Professional-grade real-time market data feed, completely under your control! 🚀
