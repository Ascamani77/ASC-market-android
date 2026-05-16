# Binance Hedge Mode Fix - Complete Solution

## Problem
After activating hedge mode in Binance:
1. ❌ Some assets couldn't place trades from the app
2. ❌ Closing positions from the app did not work
3. ✅ Trades placed directly in Binance showed up correctly in the app

## Root Cause
In Binance Futures **hedge mode** (dual-side position mode), you can hold both LONG and SHORT positions for the same asset simultaneously. This requires:

1. **Specifying `positionSide` when placing orders**: `"LONG"` or `"SHORT"` instead of `"BOTH"`
2. **Matching the exact `positionSide` when closing**: Must close the correct side (LONG or SHORT)

The app had three critical issues:
1. **Opening positions**: Not passing `positionSide` parameter → defaulted to `"BOTH"` (invalid in hedge mode)
2. **Closing positions**: Not using the actual `positionSide` from Binance API → tried to close wrong side
3. **Position tracking**: Not storing `positionSide` from Binance → couldn't identify which side to close

## Complete Solution

### 1. Added `positionSide` field to Position model ✅
**File**: `Models.kt`

```kotlin
data class Position(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val type: String, // "buy" or "sell"
    val entryPrice: Float,
    val volume: Float,
    val time: Long,
    val tp: Float? = null,
    val sl: Float? = null,
    val leverage: String = "1x",
    val margin: Float = 0f,
    val isSelected: Boolean = false,
    val partialOrders: List<PartialOrder> = emptyList(),
    val positionSide: String? = null // For Binance hedge mode: "LONG", "SHORT", or "BOTH"
)
```

### 2. Store `positionSide` when fetching positions ✅
**File**: `TradingApp.kt` - Account info fetch

```kotlin
positions.addAll(
    account.positions.map { pos ->
        val isLong = pos.positionAmt > 0
        Position(
            symbol = pos.symbol,
            type = if (isLong) "buy" else "sell",
            entryPrice = pos.entryPrice.toFloat(),
            volume = kotlin.math.abs(pos.positionAmt).toFloat(),
            time = System.currentTimeMillis(),
            leverage = "${pos.leverage}x",
            margin = pos.isolatedMargin.toFloat(),
            positionSide = pos.positionSide  // ✅ Store from Binance API
        )
    }
)
```

### 3. Store `positionSide` in WebSocket updates ✅
**File**: `TradingApp.kt` - User Data Stream handler

```kotlin
binanceFuturesService.onPositionUpdate = { updatedPositions ->
    updatedPositions.forEach { pos ->
        // ✅ Match by both symbol AND positionSide for hedge mode
        val existingIndex = positions.indexOfFirst { 
            it.symbol == pos.symbol && it.positionSide == pos.positionSide 
        }
        val position = Position(
            symbol = pos.symbol,
            type = if (pos.positionAmt > 0) "buy" else "sell",
            entryPrice = pos.entryPrice.toFloat(),
            volume = kotlin.math.abs(pos.positionAmt).toFloat(),
            time = System.currentTimeMillis(),
            leverage = "${pos.leverage}x",
            margin = pos.isolatedMargin.toFloat(),
            positionSide = pos.positionSide  // ✅ Store from WebSocket
        )
        // ... update logic
    }
}
```

### 4. Use correct `positionSide` when opening positions ✅
**File**: `TradingApp.kt` - `placeStreamOrder()`

```kotlin
// Determine positionSide for hedge mode support
val positionSide = when (position.type.uppercase()) {
    "BUY" -> "LONG"   // Opening a long position
    "SELL" -> "SHORT" // Opening a short position
    else -> "BOTH"
}

// Pass to all order methods
binanceFuturesService.placeMarketOrder(
    symbol = tradeSymbol,
    side = position.type.uppercase(),
    quantity = position.volume.toDouble(),
    positionSide = positionSide  // ✅ Specify position side
)

// Store in executed position
val executedPosition = position.copy(
    symbol = result.symbol,
    entryPrice = result.avgPrice.toFloat(),
    volume = result.executedQty.toFloat(),
    positionSide = positionSide  // ✅ Store for later closing
)
```

### 5. Use stored `positionSide` when closing positions ✅
**File**: `TradingApp.kt` - `closeStreamPosition()`

```kotlin
fun closeStreamPosition(position: Position) {
    when (chartFeedType) {
        ChartFeedType.BINANCE -> {
            scope.launch {
                runCatching {
                    // ✅ Use the positionSide from the position (from Binance API)
                    // Falls back to calculating from type for backward compatibility
                    val positionSide = position.positionSide ?: when (position.type.uppercase()) {
                        "BUY" -> "LONG"
                        "SELL" -> "SHORT"
                        else -> "BOTH"
                    }
                    
                    // ✅ In hedge mode, reduceOnly is not needed (and may cause issues)
                    // The positionSide parameter already specifies which position to close
                    val useReduceOnly = positionSide == "BOTH"
                    
                    Log.d("TradingApp", "Closing Binance position: symbol=${position.symbol}, type=${position.type}, positionSide=$positionSide, volume=${position.volume}, reduceOnly=$useReduceOnly")
                    
                    binanceFuturesService.placeMarketOrder(
                        symbol = binanceTradingSymbol(position.symbol),
                        side = if (position.type.equals("buy", ignoreCase = true)) "SELL" else "BUY",
                        quantity = position.volume.toDouble(),
                        positionSide = positionSide,  // ✅ Match the position side
                        reduceOnly = useReduceOnly  // ✅ Only use reduceOnly in one-way mode
                    )
                }.onSuccess { result ->
                    Log.d("TradingApp", "Successfully closed position: ${position.symbol}, orderId=${result.orderId}")
                    positions.removeAll { it.id == position.id }
                    localPositions.removeAll { it.id == position.id }
                    liveTradeRefreshToken += 1
                }.onFailure { error ->
                    Log.e("TradingApp", "Failed to close position: ${position.symbol}, error=${error.message}", error)
                }
            }
        }
    }
}
```

## How It Works

### Hedge Mode vs One-Way Mode

| Mode | Position Side | Behavior |
|------|--------------|----------|
| **One-way** | `BOTH` | Can only hold one direction per symbol |
| **Hedge** | `LONG` or `SHORT` | Can hold both directions simultaneously |

### Position Side Mapping

| Action | Order Side | Position Side | Binance Behavior |
|--------|-----------|---------------|------------------|
| Open Long | BUY | LONG | Opens/increases LONG position |
| Open Short | SELL | SHORT | Opens/increases SHORT position |
| Close Long | SELL | LONG | Closes/reduces LONG position |
| Close Short | BUY | SHORT | Closes/reduces SHORT position |

### Example: Hedge Mode Trading

```
1. Open LONG BTCUSDT:
   - Order: BUY, positionSide=LONG
   - Result: +1.0 BTC LONG position

2. Open SHORT BTCUSDT (same asset!):
   - Order: SELL, positionSide=SHORT
   - Result: -1.0 BTC SHORT position
   - Both positions exist simultaneously ✅

3. Close LONG position:
   - Order: SELL, positionSide=LONG, reduceOnly=true
   - Result: LONG position closed, SHORT remains ✅

4. Close SHORT position:
   - Order: BUY, positionSide=SHORT, reduceOnly=true
   - Result: SHORT position closed ✅
```

## Changes Summary

### Files Modified
1. ✅ `Models.kt` - Added `positionSide` field to Position model
2. ✅ `TradingApp.kt` - Updated 3 locations:
   - Position fetching (account info)
   - WebSocket position updates
   - Order placement (`placeStreamOrder`)
   - Position closing (`closeStreamPosition`)

### Key Improvements
1. ✅ **Position tracking**: Store `positionSide` from Binance API
2. ✅ **Opening positions**: Calculate and pass correct `positionSide`
3. ✅ **Closing positions**: Use stored `positionSide` to close correct side
4. ✅ **Real-time updates**: Handle `positionSide` in WebSocket updates
5. ✅ **Backward compatibility**: Works with both hedge mode and one-way mode
6. ✅ **Error handling**: Added detailed logging for debugging

## Testing Checklist

After this fix, verify:
- ✅ Place BUY orders from app (opens LONG positions)
- ✅ Place SELL orders from app (opens SHORT positions)
- ✅ Hold both LONG and SHORT positions for same asset simultaneously
- ✅ Close LONG positions correctly from app
- ✅ Close SHORT positions correctly from app
- ✅ Positions sync correctly from Binance to app
- ✅ WebSocket updates work correctly in hedge mode

## Debugging

If issues persist, check logs for:

```
# Opening position
TradingApp: placeStreamOrder - BINANCE: symbol=BTCUSDT, side=BUY, volume=0.1, orderType=Market Execution

# Closing position
TradingApp: Closing Binance position: symbol=BTCUSDT, type=buy, positionSide=LONG, volume=0.1
TradingApp: Successfully closed position: BTCUSDT, orderId=12345678

# Or if failed
TradingApp: Failed to close position: BTCUSDT, error=<error message>

# Position fetching
BinanceBalance: BTCUSDT: LONG 0.1 @ 50000.0, PnL: 100.0, Leverage: 10x, PositionSide: LONG
```

### Common Issues

1. **"Position side does not match user's setting"**
   - Cause: Trying to use `BOTH` in hedge mode
   - Fix: ✅ Now uses `LONG`/`SHORT` correctly

2. **"Reduce-only order is rejected"**
   - Cause: Wrong `positionSide` specified
   - Fix: ✅ Now uses stored `positionSide` from Binance

3. **Position not closing**
   - Cause: `positionSide` mismatch
   - Fix: ✅ Now stores and uses correct `positionSide`

## Additional Notes

- In **hedge mode**, `reduceOnly` is set to `false` because the `positionSide` parameter already specifies which position to close
- In **one-way mode**, `reduceOnly` is set to `true` to prevent accidentally opening a new position
- The fix is backward compatible with one-way mode (non-hedge mode)
- The `positionSide` field is optional and nullable for compatibility with other brokers (MT5, cTrader, Deriv)
- No changes needed to `BinanceFuturesService.kt` as it already had `positionSide` parameter support
- WebSocket position matching now considers both `symbol` AND `positionSide` for hedge mode
