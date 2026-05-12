# cTrader Service Integration

## Add this code to TradingApp.kt

Add this code right after the DerivService section (around line 350):

```kotlin
// cTrader Pepperstone Service for forex trading
val cTraderService = remember {
    com.trading.app.data.CTraderService(
        onQuoteUpdate = { quote ->
            // Propagate cTrader price updates to PriceStreamManager
            PriceStreamManager.updatePrice(quote.name, quote.lastPrice.toDouble())
            
            // Update MarketDataStore so AI backend receives the data
            val pair = com.asc.markets.data.FOREX_PAIRS.find { it.symbol == quote.name }
            if (pair != null) {
                val updatedPair = pair.copy(
                    price = quote.lastPrice.toDouble(),
                    change = quote.lastPrice.toDouble() - pair.price,
                    changePercent = if (pair.price > 0) ((quote.lastPrice.toDouble() - pair.price) / pair.price * 100) else 0.0
                )
                com.asc.markets.data.MarketDataStore.updatePair(updatedPair)
                android.util.Log.d("CTraderService", "Updated MarketDataStore: ${quote.name} = ${quote.lastPrice}")
            }
        },
        onPositionsUpdate = { newPositions ->
            // Handle position updates from cTrader
            android.util.Log.d("CTraderService", "Positions updated: ${newPositions.size}")
        },
        onAccountUpdate = { accountInfo ->
            // Handle account updates from cTrader
            if (accountInfo != null) {
                android.util.Log.d("CTraderService", "Account updated: balance=${accountInfo.balance}")
            }
        },
        onConnectionStatusUpdate = { connected ->
            android.util.Log.i("CTraderService", "Connection status: $connected")
        }
    )
}

// Connect cTrader and subscribe to forex pairs
LaunchedEffect(Unit) {
    cTraderService.connect()
    delay(2000) // Wait for connection
    
    // Subscribe to major forex pairs
    cTraderService.subscribe("EURUSD")
    cTraderService.subscribe("GBPUSD")
    cTraderService.subscribe("USDJPY")
    cTraderService.subscribe("AUDUSD")
    cTraderService.subscribe("USDCAD")
    cTraderService.subscribe("NZDUSD")
    cTraderService.subscribe("USDCHF")
    
    android.util.Log.i("TradingApp", "cTrader service connected and subscribed to forex pairs")
}

// Cleanup cTrader on dispose
DisposableEffect(Unit) {
    onDispose {
        cTraderService.disconnect()
    }
}
```

## Where to add it

1. Open `app/src/main/kotlin/com/trading/app/TradingApp.kt`
2. Find the DerivService section (around line 307-350)
3. Add the cTrader code right after the DerivService DisposableEffect block
4. Make sure it's inside the `TradingApp` composable function

## What it does

- **Connects** to cTrader demo/live API using your credentials
- **Subscribes** to major forex pairs for live price updates
- **Updates** PriceStreamManager and MarketDataStore with live prices
- **Handles** position and account updates from cTrader
- **Auto-reconnects** if connection is lost
- **Cleans up** properly when the app closes

## Testing

After adding the code and rebuilding:

1. Check logcat for "CTraderService" messages
2. You should see "Connection status: true" when connected
3. Price updates will appear as "Updated MarketDataStore: EURUSD = ..."
4. If there are errors, they'll show in logcat with details

## Troubleshooting

### "Access token not configured"
- Make sure you rebuilt the app after updating local.properties
- Check that BuildConfig.CTRADER_ACCESS_TOKEN is not empty

### "WebSocket failure"
- Check your internet connection
- Verify the access token hasn't expired (sandbox tokens expire)
- Try regenerating the token from cTrader sandbox

### No price updates
- Check that you're subscribed to the correct symbol names
- cTrader uses format like "EURUSD" (no slash)
- Check logcat for subscription confirmation messages

## Next Steps

Once this is working, you can:
1. Add more symbol subscriptions
2. Implement position management (open/close trades)
3. Add order placement functionality
4. Integrate with your existing trading UI
