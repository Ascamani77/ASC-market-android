# Binance Connect Implementation

## Overview
Created a new independent Binance Connect chart feed type that uses the **public Binance WebSocket** for real-time cryptocurrency price data. This is completely independent from the existing Binance trading integration.

## Public Binance WebSocket Used
- **Spot (Live)**: `wss://stream.binance.com:9443`
- **REST API**: `https://api.binance.com`

## Files Created

### 1. BinanceConnectService.kt
**Location**: `app/src/main/kotlin/com/trading/app/data/BinanceConnectService.kt`

Independent service that connects to public Binance WebSocket:
- Uses spot market WebSocket endpoint
- Subscribes to `@aggTrade` (fast price updates) and `@miniTicker` (24h stats)
- Fetches historical klines via REST API
- Auto-reconnects on failure (5 second delay)
- Completely independent from other Binance services

### 2. BinanceConnectChartService.kt
**Location**: `app/src/main/kotlin/com/trading/app/data/BinanceConnectChartService.kt`

Wrapper service for chart integration:
- Normalizes symbols
- Delegates to BinanceConnectService
- Provides clean interface for chart components

### 3. TradingChartBinanceConnect.kt
**Location**: `app/src/main/kotlin/com/trading/app/components/TradingChartBinanceConnect.kt`

Composable chart component:
- Manages candle data and live quotes
- Handles lazy loading with `hasMoreHistory` flag
- Auto-subscribes to symbol changes
- Cleans up connections on dispose

## Files Modified

### 1. ChartFeedType.kt
Added new enum value:
```kotlin
BINANCE_CONNECT("binance_connect", "Binance Connect")
```

Added symbol mappings for Binance Connect with 10 crypto pairs:
- BTCUSDT, ETHUSDT, BNBUSDT, SOLUSDT, XRPUSDT
- ADAUSDT, DOGEUSDT, AVAXUSDT, LINKUSDT, DOTUSDT

### 2. TradingApp.kt
Added BINANCE_CONNECT handling in:
- `liveTradeSourceName()` - Returns "Binance Connect (View Only)"
- `liveTradeDefaultAccountLabel()` - Returns "Binance Connect (No Trading)"
- `LaunchedEffect` for chart feed subscriptions - Stops other services
- Chart rendering `when` expression - Renders TradingChartBinanceConnect
- `placeStreamOrder()` - Logs warning (view-only mode)
- `closeStreamPosition()` - Logs warning (view-only mode)
- Connection setup - Disconnects trading services
- Provider label - Shows "Binance Connect"

### 3. SettingsScreen.kt
Added description for BINANCE_CONNECT in chart source selection:
```
"Public Binance WebSocket for real-time crypto charts (view-only)."
```

## Features

### ✅ Independent Architecture
- Own WebSocket connection
- Own service layer
- No dependencies on existing Binance trading services
- Completely isolated from Binance Futures/Demo modes

### ✅ Real-Time Data
- Live price updates via `@aggTrade` stream
- 24-hour statistics via `@miniTicker` stream
- Auto-reconnection on connection loss

### ✅ Historical Data
- Fetches up to 500 candles per request
- Supports multiple timeframes (1m, 5m, 15m, 1h, 4h, 1d, etc.)
- Lazy loading for older candles

### ✅ View-Only Mode
- No trading functionality
- Prevents accidental order placement
- Clear labeling as "View Only"

## How to Use

1. **Open Settings** → Navigate to "Stream Chart Source"
2. **Select "Binance Connect"** from the list
3. **Open a chart** - It will automatically use the public Binance WebSocket
4. **Select any crypto symbol** - BTCUSDT, ETHUSDT, etc.

## Chart Selection UI
The new "Binance Connect" option appears in:
- Settings Screen → Stream Chart Source
- Shows description: "Public Binance WebSocket for real-time crypto charts (view-only)."

## Technical Details

### WebSocket Connection
```
URL: wss://stream.binance.com:9443/stream?streams=btcusdt@aggTrade/btcusdt@miniTicker
```

### REST API for History
```
URL: https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=500
```

### Timeframe Mapping
- 1, 3, 5, 15, 30 → 1m, 3m, 5m, 15m, 30m
- 60, 120, 240, 360, 480, 720 → 1h, 2h, 4h, 6h, 8h, 12h
- D, W, M → 1d, 1w, 1M

## Limitations
- **View-only**: No trading functionality
- **Spot market only**: Uses public spot WebSocket (not futures)
- **No authentication**: Public data only
- **Rate limits**: Subject to Binance public API rate limits

## Benefits
1. **No API keys required** - Uses public endpoints
2. **Independent** - Doesn't interfere with existing Binance trading
3. **Reliable** - Uses official Binance WebSocket
4. **Real-time** - Fast price updates via aggTrade stream
5. **Free** - No costs associated with public data

## Next Steps
If you want to add more features:
- Add more crypto symbols to ChartFeedType.kt
- Implement order book data (depth stream)
- Add trade history display
- Implement symbol search functionality

## Testing
To test the implementation:
1. Go to Settings → Stream Chart Source
2. Select "Binance Connect"
3. Open a chart with BTCUSDT or any supported symbol
4. Verify real-time price updates
5. Verify historical candles load correctly
6. Test lazy loading by scrolling left on the chart
