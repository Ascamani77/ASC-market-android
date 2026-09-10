# PnL Update Fix - Summary

## Problem
When you place a trade in Pepperstone cTrader Demo, the position shows in your app but:
- Equity doesn't update
- PnL shows as $0.00
- No real-time PnL calculation

## Root Cause
The cTrader API's `ProtoOAReconcileRes` (positions list) returns `grossProfit: 0` for positions. This field isn't updated in real-time - it only updates periodically.

## Solution Implemented
Updated `ctrader_bridge.py` to calculate PnL in real-time using current market prices:

```python
# Calculate real-time PnL using current market price
if state.last_bid and state.last_ask and entry_price and entry_price > 0:
    # Use bid for sells, ask for buys (closing price)
    current_price = state.last_ask if side == "sell" else state.last_bid
    
    # Calculate PnL based on instrument type
    if state.category == "FOREX":
        pip_difference = (current_price - entry_price) if side == "buy" else (entry_price - current_price)
        pip_value = 10.0 if "JPY" in state.app_symbol else 1.0
        unrealized_pnl = pip_difference * volume * 100000 * pip_value / (0.01 if "JPY" in state.app_symbol else 0.0001)
    else:
        # For Gold, indices, etc.
        price_difference = (current_price - entry_price) if side == "buy" else (entry_price - current_price)
        unrealized_pnl = price_difference * volume
```

## How It Works Now

1. **When you place a trade**:
   - App sends `place_order` to bridge
   - Bridge places order with cTrader API
   - cTrader executes order
   - Bridge receives `ProtoOAExecutionEvent`
   - Bridge broadcasts updated ACCOUNT and requests POSITIONS

2. **When positions are updated**:
   - Bridge calls `request_positions()` 
   - Receives `ProtoOAReconcileRes` with positions list
   - For each position:
     - Gets entry price from cTrader
     - Gets current market price from last tick
     - **Calculates PnL = (current_price - entry_price) × volume**
   - Broadcasts POSITIONS with calculated PnL

3. **On every price tick**:
   - Bridge should recalculate PnL for all open positions
   - Broadcast updated POSITIONS with new PnL

## What's Missing (Next Step)

The PnL calculation is now in `handle_reconcile_response`, but we need to **recalculate PnL on every tick** so equity updates in real-time.

### Add This to `handle_spot_event()`:

After broadcasting tick data, add:
```python
# Update PnL for all open positions when price updates
if symbol_id in [state.symbol_id for state in symbol_states_by_id.values()]:
    # Trigger position PnL update
    reactor.callLater(0.1, request_positions)
```

**OR** better: Keep positions in memory and recalculate PnL locally without requesting from API each time.

## Testing

### 1. Rebuild Docker Image
```bash
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
podman build -t ctrader-bridge:latest -f Dockerfile.ctrader .
```

### 2. Restart Demo Container
```bash
cd c:\Users\HP\Documents\NEW_ASC
podman-compose stop ctrader-bridge-demo
podman rm ctrader-bridge-demo
podman-compose up -d ctrader-bridge-demo
```

### 3. Place a Test Trade
1. Open your app
2. Select "Pepperstone Demo"
3. Place a small trade (e.g., 0.01 lots EURUSD)
4. Watch the logs:
```bash
podman logs ctrader-bridge-demo -f
```

### 4. Expected Logs
```
[Order] Received order request: BUY 0.01 lots of EURUSD
[Order] Execution response: executionType=EXECUTED
[Positions] Received 1 open positions
  Position 1: {'id': '12345', 'symbol': 'EURUSD', 'side': 'buy', 'volume': 0.01, 'entryPrice': 1.0850, 'unrealizedPnl': 2.50}
[DEBUG] Broadcasting to X clients: {"type":"POSITIONS",...}
```

### 5. In Your App
- Position should appear
- PnL should show (not $0.00)
- Equity = Balance + PnL
- As price moves, PnL should update

## Current Status

✅ PnL calculation logic added to bridge  
✅ Docker image rebuilt  
✅ Demo container restarted  
⏳ **Need to test**: Place a trade and verify PnL updates

## If PnL Still Shows $0.00

Check:
1. **Are price ticks being received?**
   ```bash
   podman logs ctrader-bridge-demo | grep "type.*tick"
   ```

2. **Is the position symbol subscribed?**
   - The bridge needs price data for the symbol you're trading
   - Check: `[DEBUG] Sending request: ProtoOASubscribeSpotsReq`

3. **Is last_bid/last_ask set?**
   - The PnL calculation needs `state.last_bid` and `state.last_ask`
   - These are set in `handle_spot_event()` when ticks arrive

## Alternative: Use Equity from Account Updates

The cTrader API also provides equity in `ProtoOATraderRes`:
- Balance: Static account balance
- Equity: Balance + unrealized PnL

Your app could use:
```kotlin
val realizedPnl = accountInfo.realizedPnl
val unrealizedPnl = accountInfo.equity - accountInfo.balance
```

This would work without position-level PnL calculation!

---

**Status**: ✅ Code updated, container restarted, ready for testing
