# Binance Auto-Refresh - Real-Time Order Sync

## Problem Solved

**Issue:** Orders cancelled on Binance web were still showing in the app as "stuck orders"

**Solution:** Added automatic polling every 3 seconds to keep orders and positions in sync

## How It Works

### Automatic Refresh Loop

When the paper trading panel is open and you're in Binance mode, the app now:

1. **Fetches account info** (balance, positions)
2. **Fetches open orders**
3. **Waits 3 seconds**
4. **Repeats** (while panel is open)

This ensures:
- ✅ Cancelled orders disappear automatically
- ✅ Filled orders move to order history
- ✅ New positions appear immediately
- ✅ Balance updates in real-time
- ✅ PnL updates continuously

### Code Changes

```kotlin
LaunchedEffect(..., showPaperTradingPanel, ...) {
    if (chartFeedType == ChartFeedType.BINANCE) {
        while (showPaperTradingPanel) {
            // Fetch account info
            // Fetch positions
            // Fetch orders
            
            delay(3000)  // Wait 3 seconds
        }
    }
}
```

## Refresh Frequency

**Current:** Every 3 seconds (3000ms)

This is a good balance between:
- **Responsiveness**: Orders update quickly
- **API limits**: Doesn't hit rate limits
- **Battery**: Doesn't drain battery

### Why 3 Seconds?

- **Too fast (< 1s)**: May hit API rate limits
- **Too slow (> 10s)**: Orders feel "stuck"
- **3 seconds**: Sweet spot for real-time feel

## What Gets Refreshed

### Futures Mode
- ✅ Wallet balance
- ✅ Unrealized PnL
- ✅ Margin balance
- ✅ Available balance
- ✅ Open positions (with entry price, leverage, PnL)
- ✅ Open orders
- ✅ Liquidation prices

### Spot Mode
- ✅ USDT balance
- ✅ Crypto holdings (shown as positions)
- ✅ Open orders
- ✅ Locked balances

## Order Lifecycle

### 1. Order Placed
- **On Binance web**: Order appears
- **In your app**: Appears within 3 seconds

### 2. Order Filled
- **On Binance web**: Order fills, becomes position
- **In your app**: 
  - Order disappears from Orders tab
  - Position appears in Positions tab
  - Updates within 3 seconds

### 3. Order Cancelled
- **On Binance web**: Order cancelled
- **In your app**: Order disappears within 3 seconds

### 4. Position Closed
- **On Binance web**: Position closed
- **In your app**: Position disappears within 3 seconds

## Performance Impact

### API Calls Per Minute
- **Account info**: 20 calls/min (every 3s)
- **Open orders**: 20 calls/min (every 3s)
- **Total**: ~40 calls/min

### Binance Rate Limits
- **Spot**: 1200 requests/min
- **Futures**: 2400 requests/min
- **Our usage**: ~40 requests/min (well within limits)

### Battery Impact
- **Minimal**: Only polls when panel is open
- **Stops**: When panel is closed
- **Efficient**: Uses existing API connections

## Why Not WebSocket?

Binance has a **User Data Stream** WebSocket for real-time updates, but it requires:
1. Creating a listen key
2. Keeping it alive (ping every 30 min)
3. Handling reconnections
4. More complex code

**Current solution (polling):**
- ✅ Simple and reliable
- ✅ Works immediately
- ✅ Easy to debug
- ✅ Good enough for most use cases

**Future enhancement:**
We can add WebSocket user data stream later for true real-time updates (< 1s latency).

## Troubleshooting

### Orders still stuck?
1. Check logs: `adb logcat -s BinanceBalance:D`
2. Look for "Fetched X open orders"
3. Verify API is responding

### Orders not updating?
1. Make sure paper trading panel is open
2. Check you're in Binance mode
3. Verify network connection
4. Check API keys are valid

### Too slow?
You can change the refresh interval:
```kotlin
delay(3000)  // Change to 2000 for 2 seconds, or 1000 for 1 second
```

**Warning:** Don't go below 1000ms (1 second) to avoid rate limits.

## Logs to Check

```bash
adb logcat -s BinanceBalance:D BinanceFutures:D
```

**Expected output (every 3 seconds):**
```
BinanceBalance: === Binance Futures Account ===
BinanceBalance: Wallet Balance: 10000.0
BinanceBalance: Positions: 1
BinanceBalance: Fetched 2 open Futures orders
BinanceBalance: Mapped 2 orders to UI
```

## Summary

✅ **Auto-refresh every 3 seconds**
✅ **Orders sync automatically**
✅ **Positions update in real-time**
✅ **Balance refreshes continuously**
✅ **No manual refresh needed**

Your orders will now automatically disappear when cancelled on Binance, and new orders/positions will appear within 3 seconds!
