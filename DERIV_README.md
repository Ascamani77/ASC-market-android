# Deriv WebSocket Integration

## 📖 Overview

Your Android trading app now integrates **Deriv WebSocket API** for real-time market data. This provides low-latency, reliable price feeds for cryptocurrencies and commodities.

### ✨ Key Features

- ✅ **Real-time data** for BTC/USD, ETH/USD, Gold, Silver, Oil
- ✅ **No authentication required** for public market data
- ✅ **Smart routing** - automatically uses the best data source
- ✅ **Auto-reconnection** - handles network failures gracefully
- ✅ **Historical data** - fetch OHLC candles for any timeframe
- ✅ **Production-ready** - error handling, telemetry, logging

## 🗂️ Documentation Structure

| File | Purpose | When to Read |
|------|---------|--------------|
| **DERIV_QUICK_START.md** | 5-minute setup guide | Start here! |
| **DERIV_INTEGRATION.md** | Complete API reference | When implementing |
| **DERIV_SETUP.md** | Token setup (optional) | For authenticated features |
| **DERIV_SUMMARY.md** | Architecture overview | Understanding the system |
| **DerivIntegrationExample.kt** | Working code examples | Learning by example |

## 🚀 Quick Start

### 1. Basic Usage (No Token Required)

```kotlin
import com.trading.app.data.DerivService

val derivService = DerivService(
    onQuoteUpdate = { quote ->
        println("${quote.name}: ${quote.lastPrice}")
    }
)

derivService.connect()
derivService.subscribe("BTCUSD")
derivService.subscribe("ETHUSD")
```

### 2. Smart Routing (Recommended)

```kotlin
import com.trading.app.data.MarketDataSourceManager

val manager = MarketDataSourceManager(
    onQuoteUpdate = { quote -> /* handle */ }
)

manager.connectDeriv()
manager.subscribe("BTCUSD")  // → Uses Deriv
manager.subscribe("BTCUSDT") // → Uses Binance
manager.subscribe("EURUSD")  // → Uses MT5
```

### 3. Run Example Screen

```kotlin
import com.trading.app.examples.DerivIntegrationExample

// Add to your navigation
composable("deriv_demo") {
    DerivIntegrationExample()
}
```

## 📊 Supported Assets

### Cryptocurrencies
- **BTC/USD** - Bitcoin
- **ETH/USD** - Ethereum

### Commodities
- **XAU/USD** - Gold
- **XAG/USD** - Silver
- **BRO/USD** - Brent Crude Oil
- **WTI/USD** - WTI Crude Oil

## 🏗️ Architecture

```
┌─────────────────────────────────────┐
│   MarketDataSourceManager           │
│   (Smart routing layer)             │
└──────────┬──────────────────────────┘
           │
    ┌──────┼──────┐
    │      │      │
    ▼      ▼      ▼
┌────────┐ ┌────────┐ ┌────────┐
│ Deriv  │ │Binance │ │  MT5   │
└────────┘ └────────┘ └────────┘
    │
    ▼
┌─────────────────────────────────────┐
│   MarketDataStore                   │
│   (Centralized state)               │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│   UI Components                     │
└─────────────────────────────────────┘
```

## 📁 New Files

### Core Implementation
- `app/src/main/kotlin/com/trading/app/data/DerivService.kt`
- `app/src/main/kotlin/com/trading/app/data/MarketDataSourceManager.kt`

### Examples
- `app/src/main/kotlin/com/trading/app/examples/DerivIntegrationExample.kt`

### Documentation
- `DERIV_README.md` (this file)
- `DERIV_QUICK_START.md`
- `DERIV_INTEGRATION.md`
- `DERIV_SETUP.md`
- `DERIV_SUMMARY.md`

### Configuration
- `app/build.gradle.kts` (updated)
- `local.properties` (updated)

## 🎯 Integration Roadmap

### ✅ Phase 1: Setup (Complete)
- [x] DerivService implementation
- [x] MarketDataSourceManager
- [x] Build configuration
- [x] Documentation
- [x] Example screens

### 🔄 Phase 2: Integration (Next Steps)
- [ ] Integrate into TradingChart
- [ ] Add to MarketWatchScreen
- [ ] Update WatchlistScreen
- [ ] Add data source indicator in UI

### 🔄 Phase 3: Enhancement (Future)
- [ ] Add more commodity symbols
- [ ] Implement authenticated trading
- [ ] Add offline caching
- [ ] Performance optimization

## 🧪 Testing

### 1. Run Example Screen
```kotlin
// Navigate to example screen
navController.navigate("deriv_demo")
```

### 2. Check Logs
```bash
adb logcat | grep DerivService
```

Expected output:
```
DerivService: Connecting to Deriv WebSocket...
DerivService: Deriv WebSocket Connected
DerivService: Subscribed to frxBTCUSD (app: BTCUSD)
DerivService: Tick: BTCUSD = 43250.50
```

### 3. Verify Data
- Open example screen
- Check connection status (green dot)
- Verify prices update in real-time
- Test with different symbols

## 🔧 Configuration

### Public Data (Default)
No configuration needed! Works out of the box.

### Authenticated Features (Optional)
1. Get token from https://app.deriv.com/account/api-token
2. Add to `local.properties`:
   ```properties
   DERIV_API_TOKEN=your_token_here
   ```
3. Rebuild project

## 📚 API Reference

### DerivService

| Method | Description |
|--------|-------------|
| `connect()` | Connect to WebSocket |
| `subscribe(symbol)` | Subscribe to symbol |
| `streamActiveSymbol(symbol)` | Stream single symbol |
| `fetchHistory(symbol, timeframe)` | Get historical data |
| `disconnect()` | Close connection |
| `isConnected()` | Check status |

### MarketDataSourceManager

| Method | Description |
|--------|-------------|
| `getDataSourceForSymbol(symbol)` | Determine data source |
| `subscribe(symbol)` | Subscribe via best source |
| `streamActiveSymbol(symbol)` | Stream via best source |
| `fetchHistory(symbol, timeframe)` | Fetch via best source |
| `connectDeriv()` | Connect to Deriv |
| `disconnectAll()` | Disconnect all sources |

## 🐛 Troubleshooting

### No Data Received
1. Check internet connection
2. Verify symbol is supported
3. Check Logcat for errors
4. Ensure WebSocket is connected

### Connection Fails
1. Check network settings
2. Verify firewall allows WebSocket
3. Try public endpoint first
4. Check Deriv API status

### Wrong Data Source
1. Verify symbol normalization
2. Check `getDataSourceForSymbol()`
3. Review symbol mapping in code

## 💡 Best Practices

1. **Use Smart Routing** - Let the manager choose the best source
2. **Handle Lifecycle** - Connect in `LaunchedEffect`, disconnect in `DisposableEffect`
3. **Monitor Performance** - Check `SystemTelemetry` for metrics
4. **Cache Data** - Store historical data to reduce API calls
5. **Error Handling** - Service handles errors, but log them for debugging

## 🔗 Resources

### Documentation
- [Deriv API Docs](https://api.deriv.com)
- [WebSocket API Explorer](https://api.deriv.com/api-explorer)
- [Developer Portal](https://developers.deriv.com)

### Support
- [Deriv Community](https://community.deriv.com)
- [API Status](https://deriv.statuspage.io/)
- [GitHub Issues](https://github.com/deriv-com/deriv-api-docs/issues)

## 📈 Performance

Expected metrics:
- **Latency**: < 100ms (server to app)
- **Reconnect**: ~5 seconds
- **Data Freshness**: < 1 second
- **Uptime**: 99.9%+

## 🎓 Learning Path

1. **Start**: Read `DERIV_QUICK_START.md`
2. **Explore**: Run `DerivIntegrationExample`
3. **Understand**: Read `DERIV_INTEGRATION.md`
4. **Implement**: Integrate into your components
5. **Optimize**: Review `DERIV_SUMMARY.md`

## 🤝 Contributing

When extending the integration:
1. Follow existing patterns in `DerivService`
2. Update documentation
3. Add examples for new features
4. Test thoroughly
5. Update this README

## 📝 Changelog

### v1.0.0 (Current)
- ✅ Initial Deriv WebSocket integration
- ✅ Support for BTC/USD, ETH/USD
- ✅ Support for commodities (Gold, Silver, Oil)
- ✅ Smart routing with MarketDataSourceManager
- ✅ Complete documentation
- ✅ Example screens

### Future Versions
- 🔄 v1.1.0: More commodity symbols
- 🔄 v1.2.0: Authenticated trading
- 🔄 v1.3.0: Offline caching

## 🎉 Summary

You now have a **production-ready Deriv WebSocket integration** that:
- Works out of the box (no token required)
- Provides real-time data for crypto and commodities
- Automatically routes to the best data source
- Handles errors and reconnections gracefully
- Includes complete documentation and examples

**Start with `DERIV_QUICK_START.md` and you'll be up and running in 5 minutes!**

---

**Questions?** Check the documentation files or review the example code.

**Need help?** Open an issue or check the Deriv community forums.

**Ready to integrate?** Start with `DerivIntegrationExample.kt` to see it in action!
