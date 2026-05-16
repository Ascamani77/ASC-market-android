# Binance Chart Auto-Sync with Market Type

## What Was Added

The chart data source now **automatically matches** the selected market type:

- **Futures Mode** → Chart shows Futures data
- **Spot Mode** → Chart shows Spot data

## How It Works

When you switch between Spot and Futures using the dropdown, the chart WebSocket connection automatically switches to the correct endpoint:

### Spot Mode
- **Demo WebSocket**: `wss://demo-stream.binance.com`
- **Live WebSocket**: `wss://stream.binance.com`
- **Demo REST**: `https://demo-api.binance.com`
- **Live REST**: `https://api.binance.com`

### Futures Mode
- **Demo WebSocket**: `wss://demo-fstream.binance.com`
- **Live WebSocket**: `wss://fstream.binance.com`
- **Demo REST**: `https://demo-fapi.binance.com`
- **Live REST**: `https://fapi.binance.com`

## User Experience

1. **Open paper trading panel**
2. **Click "Binance Spot Demo"**
3. **Select "Binance Futures Demo"**
4. **Chart automatically reconnects** to Futures WebSocket
5. **Chart now shows Futures prices** (perpetual contracts)

The switch is seamless - no need to manually change chart settings!

## Technical Details

### BinanceService.kt
Updated to accept `marketType` parameter:
```kotlin
class BinanceService(
    private val tradingMode: BinanceTradingMode,
    private val marketType: BinanceMarketType,  // NEW
    ...
)
```

The service now selects the correct WebSocket URL based on both `tradingMode` (Live/Demo) and `marketType` (Spot/Futures).

### TradingApp.kt
The `binanceQuoteService` is now recreated when `binanceMarketType` changes:
```kotlin
val binanceQuoteService = remember(binanceTradingMode, binanceMarketType) {
    BinanceService(
        tradingMode = binanceTradingMode,
        marketType = binanceMarketType,  // Pass market type
        ...
    )
}
```

This triggers a reconnection with the new WebSocket URL.

## Symbol Differences

### Spot Symbols
- BTCUSDT
- ETHUSDT
- BNBUSDT

### Futures Symbols (Same format)
- BTCUSDT (perpetual contract)
- ETHUSDT (perpetual contract)
- BNBUSDT (perpetual contract)

The symbol format is the same, but the data comes from different markets:
- **Spot**: Actual crypto ownership
- **Futures**: Contracts with leverage

## Price Differences

You may notice slight price differences between Spot and Futures:
- **Spot price**: Current market price for buying/selling crypto
- **Futures price**: Contract price (can have funding rate premium/discount)

This is normal and expected!

## Testing

### Step 1: Rebuild
```bash
cd MyRealApp
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Test Chart Sync
1. Open app in Binance mode
2. Open chart for BTCUSDT
3. Note the current price
4. Open paper trading panel
5. Switch from Spot to Futures
6. Watch the chart - price may change slightly
7. Check the WebSocket connection logs

### Step 3: Verify WebSocket
```bash
adb logcat -s BinanceService:D
```

**Expected logs when switching to Futures:**
```
BinanceService: Binance WebSocket Closed: Reconnecting
BinanceService: Connecting to wss://demo-fstream.binance.com/stream?streams=btcusdt@ticker
BinanceService: WEBSOCKET_CONNECTED
```

## Benefits

✅ **Automatic sync** - No manual chart configuration needed
✅ **Correct data** - Chart always shows data from the right market
✅ **Seamless switching** - Just use the dropdown, chart updates automatically
✅ **Consistent experience** - Chart matches your trading mode

## Summary

The chart is now **smart** - it automatically knows whether to show Spot or Futures data based on your selection in the paper trading panel. This ensures you're always looking at the right prices for the market you're trading in!
