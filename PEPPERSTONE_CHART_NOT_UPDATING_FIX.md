# Pepperstone Chart Not Updating - Diagnostic & Fix

## Problem

**Symptoms:**
- cTrader bridge is connected ✅
- Account data is being received (balance, equity, margin) ✅
- Chart shows no candles ❌
- Price/quote is not showing ❌
- Nothing is moving ❌

## Root Cause

The cTrader bridge is **NOT receiving spot/tick events** from cTrader API. This means:
1. Either the app is not sending subscription requests
2. Or the subscription requests are not reaching the bridge
3. Or the bridge is not properly subscribing to cTrader API

## Diagnostic Steps

### Step 1: Check if App is Sending Subscriptions

Run logcat to see if PepperstoneChartService is sending subscription messages:

```bash
adb logcat | grep "PepperstoneChartService\|Pepperstone"
```

**Look for:**
- ✅ "Pepperstone chart WebSocket CONNECTED"
- ✅ "Subscribing to symbols: [EURUSD, GBPUSD, ...]"
- ❌ No subscription messages = app not sending

### Step 2: Check cTrader Bridge Logs

Check if the bridge is receiving subscription requests:

```bash
# In the PowerShell window running ctrader_bridge.py
# Look for:
```

**Expected:**
- `[cTrader] Subscribing to symbols: EURUSD, GBPUSD, ...`
- `[cTrader] Subscribed to X Pepperstone cTrader symbols`

**If missing:**
- Bridge is not receiving subscription requests from app

### Step 3: Check WebSocket Connection

Verify the WebSocket connection between app and bridge:

```bash
adb logcat | grep "WebSocket"
```

**Look for:**
- ✅ "WebSocket CONNECTED" (to cTrader bridge)
- ❌ "WebSocket FAILED" or "Connection refused"

### Step 4: Test Bridge Directly

Test if the bridge can receive subscriptions by sending a manual message:

```python
# In Python console or script
import websocket
import json

ws = websocket.create_connection("ws://10.164.138.133:8082")
ws.send(json.dumps({
    "action": "subscribe",
    "symbols": ["EURUSD", "GBPUSD"],
    "timeframe": "1h"
}))

# Wait for response
while True:
    result = ws.recv()
    print(result)
```

**Expected:**
- Bridge logs: "Subscribing to symbols: EURUSD, GBPUSD"
- Bridge logs: "Subscribed to 2 Pepperstone cTrader symbols"
- WebSocket receives tick messages

## Common Issues & Fixes

### Issue 1: App Not Connecting to Bridge

**Symptom:** No "WebSocket CONNECTED" in logcat

**Check:**
```bash
adb logcat | grep "Pepperstone.*WebSocket"
```

**Fix:**
1. Verify bridge is running: Check PowerShell window
2. Verify bridge host/port in app settings
3. Check network connectivity between phone and PC

### Issue 2: Subscription Not Sent

**Symptom:** WebSocket connected but no subscription messages

**Possible causes:**
- `subscribeSymbols()` not being called
- Empty symbol list
- Chart not initialized

**Fix in TradingApp.kt:**

Check this code around line 500:

```kotlin
LaunchedEffect(chartFeedType, watchlistSymbols, chartFeedQuoteCatalog, timeframe) {
    val sourceSymbols = sourceQuoteSymbols()
    when (chartFeedType) {
        ChartFeedType.PEPPERSTONE -> {
            binanceQuoteService.stopActiveStream()
            pepperstoneQuoteService.subscribeSymbols(sourceSymbols, timeframe)
        }
        // ...
    }
}
```

**Add debug logging:**

```kotlin
ChartFeedType.PEPPERSTONE -> {
    binanceQuoteService.stopActiveStream()
    Log.d("TradingApp", "Subscribing to Pepperstone symbols: ${sourceSymbols.joinToString(", ")}")
    pepperstoneQuoteService.subscribeSymbols(sourceSymbols, timeframe)
}
```

### Issue 3: Bridge Not Subscribing to cTrader API

**Symptom:** Bridge receives subscription request but doesn't forward to cTrader

**Check bridge logs for:**
- "Unresolved cTrader symbols: ..."
- "Subscribed to 0 Pepperstone cTrader symbols"

**Fix:**
Symbol mapping issue. The app symbol doesn't match any broker symbol.

**Solution:** Update `SYMBOL_MAP` in `ctrader_bridge.py`:

```python
DEFAULT_SYMBOL_MAP = {
    "EURUSD": ["EURUSD", "EUR/USD", "EURUSD.RAW"],
    "GBPUSD": ["GBPUSD", "GBP/USD", "GBPUSD.RAW"],
    # Add more mappings as needed
}
```

### Issue 4: cTrader API Not Sending Spot Events

**Symptom:** Bridge subscribes successfully but no spot events received

**Check:**
- Bridge logs: "Subscribed to X Pepperstone cTrader symbols" ✅
- But no tick messages being broadcast ❌

**Possible causes:**
1. cTrader API connection issue
2. Account not authorized for real-time data
3. Symbols not available in account

**Fix:**
1. Check cTrader API status
2. Verify account has real-time data access
3. Try different symbols (EURUSD, GBPUSD are usually available)

## Quick Fixes

### Fix 1: Restart Everything

1. Stop cTrader bridge (Ctrl+C in PowerShell)
2. Force stop Android app
3. Start cTrader bridge: `.\start_ctrader_bridge.ps1`
4. Wait for "symbols_loaded" status
5. Launch Android app
6. Navigate to chart with Pepperstone feed

### Fix 2: Force Subscription

Add this to TradingApp.kt after Pepperstone service initialization:

```kotlin
LaunchedEffect(Unit) {
    delay(2000) // Wait for connection
    if (chartFeedType == ChartFeedType.PEPPERSTONE) {
        val symbols = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD")
        Log.d("TradingApp", "Force subscribing to: ${symbols.joinToString()}")
        pepperstoneQuoteService.subscribeSymbols(symbols, "1h")
    }
}
```

### Fix 3: Check Symbol Catalog

Verify the chart feed catalog has Pepperstone symbols:

```kotlin
// In TradingApp.kt
val chartFeedQuoteCatalog = remember(chartFeedType) { 
    val catalog = chartFeedQuotes(chartFeedType)
    Log.d("TradingApp", "Chart feed catalog for $chartFeedType: ${catalog.map { it.ticker }.joinToString()}")
    catalog
}
```

**Expected output:**
```
Chart feed catalog for PEPPERSTONE: EURUSD, GBPUSD, USDJPY, XAUUSD, ...
```

**If empty:**
- Check `chartFeedQuotes()` function
- Verify Pepperstone symbols are defined

## Testing Procedure

### Test 1: Verify Bridge Connection

```bash
adb logcat -c
adb logcat | grep "Pepperstone"
```

**Expected:**
```
PepperstoneChartService: Pepperstone chart WebSocket CONNECTED
```

### Test 2: Verify Subscription Sent

```bash
adb logcat | grep "subscribe"
```

**Expected:**
```
PepperstoneChartService: Subscribing to symbols: EURUSD, GBPUSD, ...
```

### Test 3: Verify Bridge Receives Subscription

Check PowerShell window running bridge.

**Expected:**
```
[cTrader] Subscribing to symbols: EURUSD, GBPUSD
[cTrader] Subscribed to 2 Pepperstone cTrader symbols
```

### Test 4: Verify Spot Events

Check PowerShell window for tick broadcasts.

**Expected:**
```
Broadcasting tick: EURUSD @ 1.08450
Broadcasting tick: GBPUSD @ 1.26320
```

### Test 5: Verify App Receives Ticks

```bash
adb logcat | grep "tick\|quote\|price"
```

**Expected:**
```
PepperstoneChartService: Received tick: EURUSD @ 1.08450
```

## Solution Checklist

- [ ] cTrader bridge is running
- [ ] Bridge shows "symbols_loaded" status
- [ ] App WebSocket connected to bridge
- [ ] App sends subscription request
- [ ] Bridge receives subscription request
- [ ] Bridge subscribes to cTrader API
- [ ] Bridge receives spot events from cTrader
- [ ] Bridge broadcasts ticks to app
- [ ] App receives and displays ticks
- [ ] Chart updates with live data

## Most Likely Issue

Based on your logs showing account data but no price data, the most likely issue is:

**The app is not sending subscription requests to the bridge.**

This could be because:
1. `chartFeedType` is not set to `PEPPERSTONE`
2. `sourceQuoteSymbols()` returns empty list
3. `subscribeSymbols()` is not being called
4. WebSocket connection failed silently

## Immediate Action

Run this command and share the output:

```bash
adb logcat -c
adb logcat | grep -E "Pepperstone|subscribe|ChartFeed|TradingApp"
```

This will show:
- Which chart feed is active
- If subscription is being sent
- If WebSocket is connected
- Any errors

---

**Created:** 2026-05-16
**Issue:** Pepperstone chart not showing prices
**Status:** Diagnostic guide provided
**Next Step:** Check if app is sending subscription requests
