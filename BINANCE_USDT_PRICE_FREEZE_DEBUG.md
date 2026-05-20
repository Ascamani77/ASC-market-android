# Binance USDT Price Freeze - Diagnostic Guide

## Issue

Binance USDT prices (BTCUSDT, ETHUSDT) are not updating in the app.

## Possible Causes

### 1. WebSocket Connection Issue
**Symptoms**: Prices frozen, no updates
**Check**:
```bash
adb logcat | grep "BinanceService\|BinanceDataStore"
```
**Look for**:
- "WebSocket connected" messages
- "Price update" messages
- Connection errors

### 2. Binance API Rate Limiting
**Symptoms**: Connection drops after initial success
**Check**: Look for HTTP 429 errors in logs
**Solution**: Reduce request frequency or use different API key

### 3. Network Connectivity
**Symptoms**: All Binance data stops
**Check**:
```bash
curl https://fapi.binance.com/fapi/v1/ping
```
**Expected**: `{}`

### 4. Symbol Subscription Issue
**Symptoms**: Some symbols update, others don't
**Check**: Verify symbols are subscribed
```bash
adb logcat | grep "Subscribing to Binance symbols"
```

### 5. Data Store Not Updating
**Symptoms**: Data received but UI not updating
**Check**: Verify BinanceDataStore.updatePair() is called

## Quick Diagnostic Steps

### Step 1: Check BinanceService Connection
```bash
adb logcat -c
adb logcat | grep BinanceService
```

Look for:
- ✅ "BinanceService: Connecting..."
- ✅ "BinanceService: WebSocket connected"
- ✅ "BinanceService: Subscribed to BTCUSDT"
- ❌ "Connection failed" or "WebSocket error"

### Step 2: Check Price Updates
```bash
adb logcat | grep "Price update\|updatePair"
```

Look for:
- ✅ "Price update: BTCUSDT = 79170.00"
- ❌ No price updates (indicates subscription issue)

### Step 3: Check UI Updates
```bash
adb logcat | grep "BinanceDataStore\|MarketDataStore"
```

Look for:
- ✅ "BinanceDataStore: Updated BTCUSDT"
- ❌ No updates (indicates data flow issue)

### Step 4: Test Binance API Directly
```bash
curl "https://fapi.binance.com/fapi/v1/ticker/price?symbol=BTCUSDT"
```

Expected:
```json
{"symbol":"BTCUSDT","price":"79170.00"}
```

## Common Fixes

### Fix 1: Restart BinanceService
**In code**: Add reconnection logic
```kotlin
// In BinanceService.kt
fun reconnect() {
    disconnect()
    delay(1000)
    connect()
}
```

### Fix 2: Check Symbol Format
Binance expects: `BTCUSDT` (no slash, no dash)
```kotlin
// Verify symbol normalization
val symbol = "BTC/USDT".replace("/", "").replace("-", "")
// Result: "BTCUSDT" ✅
```

### Fix 3: Verify WebSocket URL
```kotlin
// Should be:
wss://fstream.binance.com/ws/btcusdt@ticker

// NOT:
wss://stream.binance.com/ws/btcusdt@ticker  // Wrong (spot, not futures)
```

### Fix 4: Check Trading Mode
```kotlin
// Verify correct mode
val mode = BinanceTradingMode.current(context)
// Should match your intent (DEMO or LIVE)
```

## Detailed Investigation

### Check BinanceService.kt

1. **Verify WebSocket URL**:
   ```kotlin
   private const val BINANCE_WS_URL = "wss://fstream.binance.com/ws"
   ```

2. **Verify Subscription Format**:
   ```kotlin
   val subscribeMessage = """
   {
     "method": "SUBSCRIBE",
     "params": ["btcusdt@ticker", "ethusdt@ticker"],
     "id": 1
   }
   """
   ```

3. **Verify onMessage Handler**:
   ```kotlin
   override fun onMessage(webSocket: WebSocket, text: String) {
       // Should parse and call onQuoteUpdate
       val quote = parseTickerData(text)
       onQuoteUpdate(quote)
   }
   ```

### Check BinanceDataStore.kt

1. **Verify updatePair Method**:
   ```kotlin
   fun updatePair(pair: ForexPair) {
       _allPairs.value = _allPairs.value.map {
           if (it.symbol == pair.symbol) pair else it
       }
   }
   ```

2. **Verify StateFlow Emission**:
   ```kotlin
   private val _allPairs = MutableStateFlow<List<ForexPair>>(emptyList())
   val allPairs: StateFlow<List<ForexPair>> = _allPairs.asStateFlow()
   ```

### Check UI Observation

1. **Verify Composable Observes Correctly**:
   ```kotlin
   val binancePairs by BinanceDataStore.allPairs.collectAsState()
   ```

2. **Verify UI Updates on State Change**:
   ```kotlin
   LaunchedEffect(binancePairs) {
       Log.d("UI", "Binance pairs updated: ${binancePairs.size}")
   }
   ```

## Testing Procedure

### Test 1: Manual Price Update
```kotlin
// Add to BinanceService for testing
fun testPriceUpdate() {
    val testQuote = SymbolQuote(
        name = "BTCUSDT",
        lastPrice = 79170.0f,
        bid = 79169.0f,
        ask = 79171.0f,
        time = System.currentTimeMillis()
    )
    onQuoteUpdate(testQuote)
}
```

Call this and verify UI updates. If UI updates, issue is with WebSocket. If not, issue is with data flow.

### Test 2: WebSocket Connection
```kotlin
// Add logging to WebSocket callbacks
override fun onOpen(webSocket: WebSocket, response: Response) {
    Log.d("BinanceService", "✅ WebSocket OPENED")
}

override fun onMessage(webSocket: WebSocket, text: String) {
    Log.d("BinanceService", "📨 Message received: $text")
}

override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    Log.e("BinanceService", "❌ WebSocket FAILED: ${t.message}")
}

override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
    Log.w("BinanceService", "⚠️ WebSocket CLOSING: $code - $reason")
}
```

### Test 3: Symbol Subscription
```kotlin
// Verify symbols are correctly formatted
val symbols = listOf("BTCUSDT", "ETHUSDT")
symbols.forEach { symbol ->
    Log.d("BinanceService", "Subscribing to: $symbol")
    // Should see these in logcat
}
```

## Solution Checklist

- [ ] BinanceService connects successfully
- [ ] WebSocket stays connected (no disconnects)
- [ ] Symbols are subscribed correctly
- [ ] onMessage receives ticker data
- [ ] onQuoteUpdate is called with parsed data
- [ ] BinanceDataStore.updatePair is called
- [ ] StateFlow emits new values
- [ ] UI observes StateFlow
- [ ] UI re-renders on state change

## If Still Not Working

### Nuclear Option: Full Restart
1. Stop app completely
2. Clear app data: `adb shell pm clear com.asc.markets`
3. Rebuild app: `./gradlew clean assembleDebug`
4. Reinstall and test

### Alternative: Use Polling Instead of WebSocket
```kotlin
// Fallback to REST API polling
private fun startPolling() {
    scope.launch {
        while (isActive) {
            try {
                val price = fetchPriceFromREST("BTCUSDT")
                updatePrice(price)
                delay(1000) // Poll every second
            } catch (e: Exception) {
                Log.e("BinanceService", "Polling error", e)
            }
        }
    }
}
```

## Related Files

- `app/src/main/kotlin/com/trading/app/data/BinanceService.kt`
- `app/src/main/java/com/asc/markets/data/BinanceDataStore.kt`
- `app/src/main/java/com/asc/markets/ui/screens/dashboard/MarketOverviewTab.kt`

## Contact

If issue persists after trying all solutions, check:
1. Binance API status: https://www.binance.com/en/support/announcement
2. Network firewall settings
3. VPN/proxy interference

---

**Created**: 2026-05-16
**Issue**: Binance USDT prices not updating
**Status**: Diagnostic guide provided
