# Binance Chart Not Loading - Debug Guide

## Problem

When switching to Binance chart:
- No price shown at top
- No price change displayed
- No candles on chart
- Quote page shows price but doesn't update

## Root Cause

The Binance WebSocket isn't connecting or receiving data properly.

## Debug Steps

### Step 1: Rebuild with Logging

```bash
cd MyRealApp
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Check Logs

```bash
adb logcat -s BinanceService:D BinanceBalance:D
```

### Step 3: What to Look For

**When you switch to Binance chart, you should see:**

```
BinanceService: Subscribing to Binance symbols: BTCUSDT, ETHUSDT, ...
BinanceService: Connecting to: wss://demo-stream.binance.com/stream?streams=btcusdt@ticker/ethusdt@ticker
BinanceService: Market type: SPOT, Trading mode: DEMO
BinanceService: Symbols: btcusdt, ethusdt
BinanceService: Binance WebSocket CONNECTED to wss://...
BinanceService: Received message: {"stream":"btcusdt@ticker","data":{...}}
BinanceService: Quote: BTCUSDT = 80610.5, change: 125.3
```

## Common Issues

### Issue 1: No Connection Log

**Symptom:**
```
BinanceService: Subscribing to Binance symbols: ...
(nothing else)
```

**Cause:** WebSocket not connecting

**Fix:**
- Check internet connection
- Verify Binance demo site is accessible
- Check firewall/VPN settings

### Issue 2: Connection Fails

**Symptom:**
```
BinanceService: Binance WebSocket Failure: ...
```

**Causes:**
- Wrong URL format
- Network issue
- Binance demo down

**Fix:**
- Check URL in logs
- Test URL in browser: `wss://demo-stream.binance.com/ws/btcusdt@ticker`
- Try switching between Spot and Futures

### Issue 3: No Symbols Subscribed

**Symptom:**
```
BinanceService: Subscribing to Binance symbols: (empty)
```

**Cause:** No symbols in watchlist

**Fix:**
- Add symbols to watchlist
- Check `chartFeedQuotes(ChartFeedType.BINANCE)` returns symbols

### Issue 4: Wrong Symbol Format

**Symptom:**
```
BinanceService: Symbols: BTC/USDT, ETH/USDT
```

**Cause:** Symbols have slashes (wrong format)

**Expected:**
```
BinanceService: Symbols: btcusdt, ethusdt
```

**Fix:** Symbol normalization in `binanceTradingSymbol()` function

### Issue 5: Messages Received But No Updates

**Symptom:**
```
BinanceService: Received message: {...}
(but chart doesn't update)
```

**Cause:** Quote not being cached properly

**Fix:** Check `cacheSelectedSourceQuote()` is being called

## Manual Test

### Test WebSocket Directly

1. Open browser console
2. Run:
```javascript
const ws = new WebSocket('wss://demo-stream.binance.com/ws/btcusdt@ticker');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

3. You should see price updates every second

If this works, the issue is in the app code.
If this doesn't work, Binance demo might be down.

## Quick Fixes

### Fix 1: Force Reconnect

Close and reopen the app completely (not just switch charts).

### Fix 2: Clear Cache

```bash
adb shell pm clear com.asc.markets
```

Then reopen app and try again.

### Fix 3: Switch Market Type

Try switching between Spot and Futures:
1. Open paper trading panel
2. Click "Binance Spot Demo"
3. Select "Binance Futures Demo"
4. Check if chart loads

### Fix 4: Try Different Symbol

Instead of BTCUSDT, try:
- ETHUSDT
- BNBUSDT
- SOLUSDT

## Expected Behavior

**Working chart should show:**
- ✅ Current price at top (e.g., "80,610.50")
- ✅ Price change (e.g., "+125.30 (+0.16%)")
- ✅ Candles on chart
- ✅ Price updates every second
- ✅ Quote page updates in real-time

## Network Requirements

- **Internet**: Required
- **Ports**: 443 (HTTPS), 9443 (WSS)
- **Domains**: 
  - `demo-stream.binance.com`
  - `demo-api.binance.com`
  - `demo-fstream.binance.com` (Futures)
  - `demo-fapi.binance.com` (Futures)

## Next Steps

1. **Rebuild** with logging
2. **Check logs** for connection status
3. **Share logs** if issue persists
4. **Try manual WebSocket test** to verify Binance is working

The logs will tell us exactly what's happening!
