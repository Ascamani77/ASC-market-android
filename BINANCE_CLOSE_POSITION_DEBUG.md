# Binance Close Position Debug Guide

## Updated Fix
Changed `reduceOnly` behavior:
- **Hedge mode** (positionSide = LONG/SHORT): `reduceOnly = false`
- **One-way mode** (positionSide = BOTH): `reduceOnly = true`

Reason: In hedge mode, `reduceOnly` may conflict with `positionSide` parameter.

## How to Debug

### 1. Check Android Logs
Open Android Studio → Logcat and filter by "TradingApp"

Look for these log messages when you try to close a position:

```
# When you click close
TradingApp: Closing Binance position: symbol=BTCUSDT, type=buy, positionSide=LONG, volume=0.1, reduceOnly=false

# If successful
TradingApp: Successfully closed position: BTCUSDT, orderId=12345678

# If failed
TradingApp: Failed to close position: BTCUSDT, error=<ERROR MESSAGE HERE>
```

### 2. Common Error Messages

#### Error: "Position side does not match user's setting"
**Cause**: Wrong positionSide value
**Check**: 
- Is `positionSide` showing as `LONG` or `SHORT` in the log?
- If it shows `null` or `BOTH`, the position wasn't fetched correctly

#### Error: "Reduce-only order is rejected"
**Cause**: `reduceOnly=true` conflicts with hedge mode
**Fix**: ✅ Already fixed - now uses `reduceOnly=false` in hedge mode

#### Error: "Order would immediately trigger"
**Cause**: Price/quantity issue
**Check**: Volume in the log matches your position size

#### Error: "Invalid quantity"
**Cause**: Quantity precision issue
**Check**: Volume value in the log

### 3. Verify Position Data

Add this temporary debug code to see what data the position has:

```kotlin
// Before closing, log the position details
Log.d("DEBUG_POSITION", """
    Position Details:
    - ID: ${position.id}
    - Symbol: ${position.symbol}
    - Type: ${position.type}
    - Volume: ${position.volume}
    - PositionSide: ${position.positionSide}
    - Entry Price: ${position.entryPrice}
""".trimIndent())
```

### 4. Check Position Fetching

Look for this log when positions are fetched:

```
BinanceBalance: BTCUSDT: LONG 0.1 @ 50000.0, PnL: 100.0, Leverage: 10x, PositionSide: LONG
```

**Important**: If `PositionSide` shows as `null` or `BOTH`, the position wasn't stored correctly.

### 5. Test Scenarios

Try these in order:

#### Test 1: Check Position Data
1. Open a position in Binance directly
2. Check if it appears in your app
3. Look at the logs - does it show the correct `positionSide`?

#### Test 2: Close from App
1. Try to close the position from your app
2. Check the "Closing Binance position" log
3. Note the error message if it fails

#### Test 3: Manual API Test
If closing still fails, test the API directly:
```kotlin
// Test closing manually
binanceFuturesService.placeMarketOrder(
    symbol = "BTCUSDT",
    side = "SELL",
    quantity = 0.001,
    positionSide = "LONG",
    reduceOnly = false
)
```

## What to Report

If it still doesn't work, please provide:

1. **The exact error message** from the log:
   ```
   TradingApp: Failed to close position: BTCUSDT, error=<COPY THIS>
   ```

2. **The position details** from the log:
   ```
   TradingApp: Closing Binance position: symbol=?, type=?, positionSide=?, volume=?, reduceOnly=?
   ```

3. **How the position was created**:
   - [ ] Opened from the app
   - [ ] Opened from Binance website/app
   - [ ] Opened before hedge mode was enabled

4. **Position mode in Binance**:
   - [ ] Hedge mode (dual-side)
   - [ ] One-way mode

## Quick Fixes to Try

### Fix 1: Restart the app
Sometimes position data needs to refresh after enabling hedge mode.

### Fix 2: Check position mode
Make sure hedge mode is actually enabled in Binance:
1. Binance Futures → Settings → Position Mode
2. Should show "Hedge Mode" selected

### Fix 3: Check if position exists in Binance
1. Go to Binance Futures
2. Check if the position actually exists
3. Try closing it from Binance to verify it's a real position

### Fix 4: Check symbol format
The log shows `symbol=` - make sure it's in the correct format:
- ✅ Correct: `BTCUSDT`
- ❌ Wrong: `BTC/USDT`, `BTCUSD`, `BTC-USDT`

## Expected Behavior

### Hedge Mode (LONG position)
```
Input:
- position.type = "buy"
- position.positionSide = "LONG"
- position.volume = 0.1

API Call:
- symbol = "BTCUSDT"
- side = "SELL"
- quantity = 0.1
- positionSide = "LONG"
- reduceOnly = false

Result: LONG position closed ✅
```

### Hedge Mode (SHORT position)
```
Input:
- position.type = "sell"
- position.positionSide = "SHORT"
- position.volume = 0.1

API Call:
- symbol = "BTCUSDT"
- side = "BUY"
- quantity = 0.1
- positionSide = "SHORT"
- reduceOnly = false

Result: SHORT position closed ✅
```

## Next Steps

1. Run the app and try to close a position
2. Check the logs immediately
3. Copy the error message
4. Report back with the log details

The logs will tell us exactly what's wrong!
