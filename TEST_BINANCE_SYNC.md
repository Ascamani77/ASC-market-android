# Test: Binance Trade Sync

## Quick Test Steps

### 1. Rebuild the App
```bash
cd MyRealApp
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Start Logcat
```bash
adb logcat -s BinanceBalance:D BinanceTrading:D
```

### 3. Test Scenario A: Trade from Binance Web → App

**On Binance Web:**
1. Go to https://demo.binance.com/
2. Navigate to Spot trading
3. Buy 0.001 BTC (or any small amount)
4. Note the execution price

**In Your App:**
1. Make sure you're in Binance Demo mode
2. Open the paper trading panel (tap balance/currency)
3. Go to **Positions** tab

**Expected Result:**
- You should see BTC position appear
- Volume: 0.001 BTC
- Entry price: Current market price (NOT your actual entry price)
- PnL: ~0 (because entry price is wrong)

**Logs to Check:**
```
BinanceBalance: Non-stablecoin balances (shown as positions):
BinanceBalance:   BTC: 0.001 @ 80610.5
BinanceBalance: Total positions: 1
```

### 4. Test Scenario B: Trade from App → Binance Web

**In Your App:**
1. Open the chart for BTCUSDT
2. Tap the **+** button or order button
3. Place a market buy order for 0.001 BTC
4. Confirm the order

**On Binance Web:**
1. Go to https://demo.binance.com/
2. Check **Wallet** → **Spot**
3. Look for BTC balance

**Expected Result:**
- BTC balance should increase by 0.001
- Order should appear in Order History
- USDT balance should decrease

**Logs to Check:**
```
BinanceTrading: Request: POST /api/v3/order
BinanceTrading: Response: 200 - {"orderId":...}
```

### 5. Test Scenario C: Open Orders

**On Binance Web:**
1. Place a **limit order** (not market)
2. Set price above/below current market so it doesn't fill immediately

**In Your App:**
1. Open paper trading panel
2. Go to **Orders** tab

**Expected Result:**
- Order should appear in the Orders tab
- Status: Working
- Price: Your limit price

**Logs to Check:**
```
BinanceBalance: Fetched X open orders
BinanceBalance: Order: BTCUSDT buy 0.001 @ 75000.0 [NEW]
```

## Current Limitations

### ✅ What Works
1. **Balances sync** - Your USDT/USDC/BTC balances are correct
2. **Open orders sync** - Pending orders show in both places
3. **App → Binance** - Orders placed in app execute on Binance

### ❌ What Doesn't Work Correctly
1. **Entry price tracking** - Shows current price, not actual entry
2. **PnL calculation** - Always ~0 because entry price is wrong
3. **Position history** - Can't see when you opened the position

## Why Entry Price is Wrong

**The Problem:**
```kotlin
// Current code uses CURRENT market price as entry price
entryPrice = marketPrice.toFloat()  // ❌ This is NOW price, not ENTRY price
```

**What We Need:**
```kotlin
// We need to fetch trade history and calculate:
entryPrice = weightedAverageOfAllBuyTrades()  // ✅ Actual entry price
```

**Example:**
- You bought 0.001 BTC at $80,000
- Current price is $81,000
- **Current behavior**: Shows entry at $81,000, PnL = $0
- **Correct behavior**: Shows entry at $80,000, PnL = $1

## Next Steps

After testing, we need to:

1. ✅ Verify positions appear (even with wrong entry price)
2. ✅ Verify orders sync both ways
3. ⏳ Implement trade history fetching
4. ⏳ Calculate correct entry prices
5. ⏳ Show accurate PnL

## Share Your Results

After running the tests, share:
1. The logcat output
2. Screenshots of the Positions tab
3. Whether the position appeared after placing a trade on Binance web
