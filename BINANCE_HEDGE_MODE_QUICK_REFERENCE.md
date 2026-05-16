# Binance Hedge Mode - Quick Reference

## What Changed?

### ✅ Fixed Issues
1. **Opening positions** - Now works in hedge mode
2. **Closing positions** - Now closes the correct position side
3. **Position tracking** - Stores and uses `positionSide` from Binance

### 🔧 Technical Changes
- Added `positionSide` field to `Position` model
- Updated 4 locations in `TradingApp.kt`:
  1. Account info fetch
  2. WebSocket updates
  3. Order placement
  4. Position closing

## How to Use Hedge Mode

### Enable Hedge Mode in Binance
1. Go to Binance Futures
2. Settings → Position Mode
3. Select "Hedge Mode"
4. Confirm the change

### Trading in Hedge Mode

#### Open Positions
```kotlin
// Open LONG position
BUY BTCUSDT → positionSide = "LONG"

// Open SHORT position (same asset!)
SELL BTCUSDT → positionSide = "SHORT"

// Result: Both positions exist simultaneously
```

#### Close Positions
```kotlin
// Close LONG position
SELL with positionSide = "LONG"

// Close SHORT position
BUY with positionSide = "SHORT"
```

## Position Side Logic

| Your Action | Order Side | Position Side | What Happens |
|------------|-----------|---------------|--------------|
| Buy to open | BUY | LONG | Opens/adds to LONG |
| Sell to open | SELL | SHORT | Opens/adds to SHORT |
| Sell to close long | SELL | LONG | Closes LONG |
| Buy to close short | BUY | SHORT | Closes SHORT |

## Debugging

### Check Logs
```
# When opening
TradingApp: placeStreamOrder - BINANCE: symbol=BTCUSDT, side=BUY, volume=0.1

# When closing
TradingApp: Closing Binance position: symbol=BTCUSDT, type=buy, positionSide=LONG, volume=0.1
TradingApp: Successfully closed position: BTCUSDT, orderId=12345678

# Position info
BinanceBalance: BTCUSDT: LONG 0.1 @ 50000.0, PnL: 100.0, Leverage: 10x, PositionSide: LONG
```

### Common Errors (Now Fixed)

❌ **Before Fix**: "Position side does not match user's setting"
✅ **After Fix**: Uses correct LONG/SHORT position side

❌ **Before Fix**: "Reduce-only order is rejected"
✅ **After Fix**: Uses stored positionSide from Binance

❌ **Before Fix**: Position doesn't close
✅ **After Fix**: Matches exact positionSide when closing

## Testing Checklist

Test these scenarios:
- [ ] Open LONG position (BUY order)
- [ ] Open SHORT position (SELL order)
- [ ] Hold both LONG and SHORT for same asset
- [ ] Close LONG position
- [ ] Close SHORT position
- [ ] Verify positions sync from Binance
- [ ] Check WebSocket updates work

## Important Notes

1. **Backward Compatible**: Works with both hedge mode and one-way mode
2. **Auto-Detection**: App automatically uses correct positionSide
3. **Safe Closing**: `reduceOnly=true` prevents accidental new positions
4. **Real-time Sync**: WebSocket updates include positionSide

## Need Help?

Check the full documentation: `BINANCE_HEDGE_MODE_FIX.md`
