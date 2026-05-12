# Deriv Integration Architecture

## System Overview

```
┌─────────────────────────────────────────────────────┐
│                   Android App                        │
│                                                      │
│  UI Layer (Compose)                                 │
│  ├─ TradingChart                                    │
│  ├─ MarketWatchScreen                               │
│  └─ WatchlistScreen                                 │
│           ↓                                          │
│  MarketDataStore (State Management)                 │
│           ↓                                          │
│  MarketDataSourceManager (Smart Routing)            │
│           ↓                                          │
│  ┌──────────────┬──────────────┬──────────────┐    │
│  │ DerivService │BinanceService│  Mt5Service  │    │
│  └──────┬───────┴──────┬───────┴──────┬───────┘    │
└─────────┼──────────────┼──────────────┼────────────┘
          ↓              ↓              ↓
    Deriv WS       Binance WS       MT5 WS
```

## Data Flow

### Real-Time Updates
```
User → UI → Manager → DerivService → WebSocket → Deriv Server
                                          ↓
                                    Tick Updates
                                          ↓
                                    onQuoteUpdate
                                          ↓
                                   MarketDataStore
                                          ↓
                                    UI Updates
```

### Smart Routing
```
Symbol → Normalize → Check Deriv List
                          ↓
                    ┌─────┴─────┐
                    │ In List?  │
                    └─────┬─────┘
                    YES   │   NO
                    ↓     │     ↓
                 Deriv    │  Check USDT?
                          │     ↓
                          │  Binance or MT5
```

## Key Components

### DerivService
- WebSocket connection management
- Real-time tick subscriptions
- Historical data fetching
- Auto-reconnection
- Symbol mapping (BTCUSD ↔ frxBTCUSD)

### MarketDataSourceManager
- Smart routing logic
- Unified API for all sources
- Data source selection
- Fallback handling

### MarketDataStore
- Centralized state management
- Price history tracking
- Symbol normalization
- Reactive updates (StateFlow)

## Integration Pattern

```kotlin
@Composable
fun TradingScreen() {
    val manager = remember {
        MarketDataSourceManager(
            onQuoteUpdate = { quote -> /* handle */ }
        )
    }
    
    LaunchedEffect(Unit) {
        manager.connectDeriv()
    }
    
    LaunchedEffect(symbol) {
        manager.streamActiveSymbol(symbol)
    }
    
    DisposableEffect(Unit) {
        onDispose { manager.disconnectAll() }
    }
}
```

## Symbol Mapping

| App Symbol | Deriv Symbol | Data Source |
|------------|--------------|-------------|
| BTCUSD | frxBTCUSD | Deriv |
| ETHUSD | frxETHUSD | Deriv |
| XAUUSD | frxXAUUSD | Deriv |
| BTCUSDT | BTCUSDT | Binance |
| EURUSD | EURUSD | MT5 |

## Authentication (Optional)

For authenticated features:
1. Get token from Deriv
2. Add to local.properties
3. Request OTP URL
4. Connect to authenticated WebSocket
5. Send trading commands

See `DERIV_SETUP.md` for details.
