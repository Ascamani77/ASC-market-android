# Binance Price Not Updating - Quick Fix

## Confirmed: Your App Uses Futures API Correctly ✅

- **WebSocket**: `wss://fstream.binance.com` (Futures) ✅
- **Historical Data**: `/fapi/v1/klines` (Futures) ✅
- **NOT using spot** - This is correct!

## The Real Issue

Since both WebSocket and chart use Futures API, the problem is in the **data flow**, not the API.

## Quick Diagnostic

### Step 1: Check WebSocket Connection
```bash
adb logcat -c
adb logcat | grep "BinanceService"
```

**Look for**:
```
BinanceService: Connecting to: wss://fstream.binance.com/stream?streams=...
BinanceService: Binance WebSocket CONNECTED (fast mode)
```

**If you DON'T see "CONNECTED"**: WebSocket failed to connect
**If you see "Region block detected"**: You're blocked by Binance (use VPN)

### Step 2: Check if Messages Are Received
```bash
adb logcat | grep "aggTrade\|miniTicker"
```

**If you see messages**: WebSocket is receiving data ✅
**If you DON'T see messages**: WebSocket connected but no data

### Step 3: Check if onQuoteUpdate Is Called
```bash
adb logcat | grep "onQuoteUpdate\|Price update"
```

**If you see updates**: Data is being processed ✅
**If you DON'T see updates**: Data not reaching callback

### Step 4: Check if UI Observes BinanceDataStore
```bash
adb logcat | grep "BinanceDataStore"
```

**Look for**: "Updated BTCUSDT" or similar
**If missing**: UI not observing or DataStore not updating

## Most Likely Causes

### Cause 1: Region Block (HTTP 451)
**Symptoms**: WebSocket connects then immediately closes
**Fix**: Use VPN or change to demo mode

**Check**:
```bash
adb logcat | grep "451\|region\|blocked"
```

**Solution**:
```kotlin
// In your app, switch to demo mode
val mode = BinanceTradingMode.DEMO
```

### Cause 2: Symbols Not Subscribed
**Symptoms**: WebSocket connects but no data
**Fix**: Verify symbols are added before connect()

**Check**:
```bash
adb logcat | grep "Symbols:"
```

**Should see**: `Symbols: BTCUSDT, ETHUSDT`

**Solution**: Ensure `subscribeSymbols()` is called before `connect()`

### Cause 3: onQuoteUpdate Not Wired
**Symptoms**: Messages received but UI doesn't update
**Fix**: Verify callback is connected to BinanceDataStore

**Check in TradingApp.kt**:
```kotlin
val binanceQuoteService = remember(binanceTradingMode) {
    BinanceService(
        tradingMode = binanceTradingMode,
        marketType = BinanceMarketType.FUTURES,
        onQuoteUpdate = { quote ->
            if (chartFeedType == ChartFeedType.BINANCE) {
                cacheSelectedSourceQuote(quote)  // ← This must be called
            }
        },
        onHistoryUpdate = { _, _ -> }
    )
}
```

### Cause 4: Chart Feed Type Mismatch
**Symptoms**: WebSocket works but chart doesn't update
**Fix**: Verify `chartFeedType == ChartFeedType.BINANCE`

**Check**:
```kotlin
// In TradingApp.kt
val chartFeedType = streamFeedType  // Should be BINANCE
```

## Quick Fix Script

Run this to see everything at once:

```bash
adb logcat -c
echo "=== Checking Binance Connection ==="
timeout 10 adb logcat | grep -E "BinanceService|aggTrade|miniTicker|onQuoteUpdate|BinanceDataStore"
```

## Immediate Actions

### Action 1: Add Debug Logging
Add this to `BinanceService.kt` in `onMessage`:

```kotlin
override fun onMessage(webSocket: WebSocket, text: String) {
    Log.d("BinanceService", "📨 Message received: ${text.take(100)}...")  // ← Add this
    try {
        val root = JSONObject(text)
        val stream = root.optString("stream", "")
        val data = root.optJSONObject("data") ?: return
        
        Log.d("BinanceService", "Stream: $stream")  // ← Add this
        
        when {
            stream.endsWith("@aggTrade") -> {
                val symbol = data.optString("s")
                val price = data.optString("p", "0").toFloatOrNull() ?: 0f
                Log.d("BinanceService", "💰 Price update: $symbol = $price")  // ← Add this
                // ... rest of code
            }
        }
    }
}
```

### Action 2: Add Debug Logging to onQuoteUpdate
In `TradingApp.kt`:

```kotlin
onQuoteUpdate = { quote ->
    Log.d("TradingApp", "🔔 Quote received: ${quote.name} = ${quote.lastPrice}")  // ← Add this
    if (chartFeedType == ChartFeedType.BINANCE) {
        cacheSelectedSourceQuote(quote)
    }
}
```

### Action 3: Verify Subscription
Add this to `BinanceService.kt` in `subscribeSymbols`:

```kotlin
fun subscribeSymbols(symbols: List<String>) {
    this.symbols.clear()
    this.symbols.addAll(symbols.map { it.uppercase().replace("/", "") })
    Log.d("BinanceService", "✅ Subscribed symbols: ${this.symbols.joinToString()}")  // ← Add this
    if (this.symbols.isNotEmpty()) {
        connect()
    }
}
```

## Expected Output (Working)

```
BinanceService: Connecting to: wss://fstream.binance.com/stream?streams=btcusdt@aggTrade/btcusdt@miniTicker/ethusdt@aggTrade/ethusdt@miniTicker
BinanceService: Market type: FUTURES, Trading mode: LIVE
BinanceService: Symbols: BTCUSDT, ETHUSDT
BinanceService: Binance WebSocket CONNECTED (fast mode)
BinanceService: 📨 Message received: {"stream":"btcusdt@aggTrade","data":{"e":"aggTrade","E":1778890430000...
BinanceService: Stream: btcusdt@aggTrade
BinanceService: 💰 Price update: BTCUSDT = 79170.0
TradingApp: 🔔 Quote received: BTCUSDT = 79170.0
BinanceDataStore: Updated BTCUSDT
```

## If Still Not Working

### Nuclear Option 1: Force Reconnect
Add a button to manually reconnect:

```kotlin
Button(onClick = {
    binanceQuoteService.disconnect()
    delay(1000)
    binanceQuoteService.connect()
}) {
    Text("Reconnect Binance")
}
```

### Nuclear Option 2: Switch to Demo Mode
```kotlin
// In NetworkConfig or SharedPreferences
BinanceTradingMode.DEMO  // Uses testnet, no region blocks
```

### Nuclear Option 3: Use Polling Instead
If WebSocket keeps failing, fall back to REST API polling:

```kotlin
// Poll every 2 seconds
LaunchedEffect(Unit) {
    while (isActive) {
        try {
            val response = client.newCall(
                Request.Builder()
                    .url("https://fapi.binance.com/fapi/v1/ticker/price?symbol=BTCUSDT")
                    .build()
            ).execute()
            
            val json = JSONObject(response.body?.string() ?: "{}")
            val price = json.optString("price", "0").toFloatOrNull() ?: 0f
            
            onQuoteUpdate(SymbolQuote(
                name = "BTCUSDT",
                lastPrice = price,
                // ... other fields
            ))
        } catch (e: Exception) {
            Log.e("BinancePolling", "Error: ${e.message}")
        }
        delay(2000)
    }
}
```

## Summary

Your app is **correctly using Futures API** for both WebSocket and historical data. The issue is in the **data flow**, not the API choice.

**Most likely cause**: Region block (HTTP 451) or symbols not subscribed

**Quick fix**: Add debug logging and check logcat

**Nuclear fix**: Switch to demo mode or use polling

---

**Run the diagnostic script above and share the output to identify the exact issue.**
