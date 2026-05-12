# Deriv WebSocket - Quick Start Guide

## 🚀 5-Minute Setup

### Step 1: Import the Service

```kotlin
import com.trading.app.data.DerivService
import com.trading.app.data.MarketDataSourceManager
```

### Step 2: Create Instance

```kotlin
val derivService = DerivService(
    onQuoteUpdate = { quote ->
        // Handle price updates
        println("${quote.name}: ${quote.lastPrice}")
    },
    onHistoryUpdate = { symbol, history ->
        // Handle historical data
        println("$symbol: ${history.size} candles")
    }
)
```

### Step 3: Connect & Subscribe

```kotlin
// Connect
derivService.connect()

// Subscribe to symbols
derivService.subscribe("BTCUSD")
derivService.subscribe("ETHUSD")
derivService.subscribe("XAUUSD")  // Gold

// Fetch history
derivService.fetchHistory("BTCUSD", "1h")
```

### Step 4: Cleanup

```kotlin
// When done
derivService.disconnect()
```

## 📊 Supported Symbols

| Symbol | Asset | Deriv Code |
|--------|-------|------------|
| BTCUSD | Bitcoin | frxBTCUSD |
| ETHUSD | Ethereum | frxETHUSD |
| XAUUSD | Gold | frxXAUUSD |
| XAGUSD | Silver | frxXAGUSD |
| BROUSD | Brent Crude | frxBROUSD |
| WTIUSD | WTI Crude | frxWTIOUSD |

## 🎯 Smart Routing (Recommended)

```kotlin
val manager = MarketDataSourceManager(
    onQuoteUpdate = { quote -> /* handle */ }
)

// Connect
manager.connectDeriv()

// Subscribe (auto-routes to best source)
manager.subscribe("BTCUSD")  // → Deriv
manager.subscribe("BTCUSDT") // → Binance
manager.subscribe("EURUSD")  // → MT5
```

## 📝 Common Patterns

### Pattern 1: Live Price Monitor

```kotlin
@Composable
fun LivePriceMonitor() {
    var price by remember { mutableStateOf(0f) }
    
    val derivService = remember {
        DerivService(
            onQuoteUpdate = { quote ->
                if (quote.name == "BTCUSD") {
                    price = quote.lastPrice
                }
            }
        )
    }
    
    LaunchedEffect(Unit) {
        derivService.connect()
        derivService.subscribe("BTCUSD")
    }
    
    DisposableEffect(Unit) {
        onDispose { derivService.disconnect() }
    }
    
    Text("BTC/USD: $$price")
}
```

### Pattern 2: Multi-Symbol Watchlist

```kotlin
val symbols = listOf("BTCUSD", "ETHUSD", "XAUUSD")
val prices = mutableStateMapOf<String, Float>()

val derivService = DerivService(
    onQuoteUpdate = { quote ->
        prices[quote.name] = quote.lastPrice
    }
)

derivService.connect()
symbols.forEach { derivService.subscribe(it) }
```

### Pattern 3: Chart with History

```kotlin
@Composable
fun ChartWithHistory(symbol: String) {
    var candles by remember { mutableStateOf<List<OHLCData>>(emptyList()) }
    
    val derivService = remember {
        DerivService(
            onHistoryUpdate = { sym, history ->
                if (sym == symbol) {
                    candles = history
                }
            }
        )
    }
    
    LaunchedEffect(symbol) {
        derivService.connect()
        derivService.fetchHistory(symbol, "1h")
    }
    
    // Render chart with candles
}
```

## ⚙️ Configuration

### No Token Required (Public Data)
Works out of the box! No setup needed.

### With Token (Optional - for trading)
Add to `local.properties`:
```properties
DERIV_API_TOKEN=your_token_here
```

Get token: https://app.deriv.com/account/api-token

## 🔍 Debugging

### Check Connection
```kotlin
if (derivService.isConnected()) {
    println("Connected!")
}
```

### Monitor Logs
Look for "DerivService" in Logcat:
```
DerivService: Deriv WebSocket Connected
DerivService: Subscribed to frxBTCUSD
DerivService: Tick: BTCUSD = 43250.50
```

## 📚 More Info

- **Full Guide**: `DERIV_INTEGRATION.md`
- **Setup**: `DERIV_SETUP.md`
- **Examples**: `DerivIntegrationExample.kt`
- **Summary**: `DERIV_SUMMARY.md`

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| No data | Check internet, verify symbol |
| Connection fails | Check firewall, network |
| Wrong prices | Verify symbol mapping |

## 💡 Pro Tips

1. **Use Smart Routing** - Let `MarketDataSourceManager` choose the best source
2. **Monitor Latency** - Check `SystemTelemetry` for performance metrics
3. **Handle Reconnects** - Service auto-reconnects, no manual handling needed
4. **Batch Subscriptions** - Subscribe to multiple symbols at once
5. **Cache History** - Store historical data to reduce API calls

## 🎉 You're Ready!

Start with the example screens, then integrate into your app using the patterns above.

**Need help?** Check the full documentation in `DERIV_INTEGRATION.md`.
