# Deriv WebSocket Integration Guide

## Overview

This app now integrates Deriv WebSocket API as the **primary data source** for:
- **Cryptocurrencies**: BTC/USD, ETH/USD
- **Commodities**: Gold (XAU/USD), Silver (XAG/USD), Brent Crude (BRO/USD), WTI Crude (WTI/USD)

The integration uses a **priority-based fallback system**:
1. **Deriv** - First choice for supported symbols (crypto & commodities)
2. **Binance** - Fallback for crypto pairs ending in USDT
3. **MT5** - Fallback for forex and other instruments

## Architecture

### Components

1. **DerivService.kt** - Core WebSocket client for Deriv API
   - Handles real-time tick data
   - Fetches historical candle data
   - Manages subscriptions and reconnections
   - Uses public WebSocket endpoint (no auth required for market data)

2. **MarketDataSourceManager.kt** - Smart routing layer
   - Determines which data source to use for each symbol
   - Manages multiple data sources
   - Provides unified interface for subscribing and fetching data

## Configuration

### 1. Add Deriv Credentials (Optional)

For public market data, no authentication is required. The default app ID (1089) is used.

If you have a Deriv API token for authenticated features, add to `local.properties`:

```properties
DERIV_APP_ID=your_app_id
DERIV_API_TOKEN=your_api_token
```

### 2. Supported Symbols

The following symbols are automatically routed to Deriv:

| Symbol | Description | Deriv Symbol |
|--------|-------------|--------------|
| BTCUSD, BTC/USD | Bitcoin | frxBTCUSD |
| ETHUSD, ETH/USD | Ethereum | frxETHUSD |
| XAUUSD, XAU/USD, GOLD | Gold | frxXAUUSD |
| XAGUSD, XAG/USD, SILVER | Silver | frxXAGUSD |
| BROUSD, BRO/USD, BRENT | Brent Crude Oil | frxBROUSD |
| WTIUSD, WTI/USD, CRUDE | WTI Crude Oil | frxWTIOUSD |

## Usage Examples

### Example 1: Basic Integration in TradingChart

```kotlin
import com.trading.app.data.DerivService
import com.trading.app.data.MarketDataSourceManager

@Composable
fun TradingChart(symbol: String) {
    // Create data source manager
    val dataSourceManager = remember {
        MarketDataSourceManager(
            onQuoteUpdate = { quote ->
                // Handle real-time price updates
                Log.d("Chart", "${quote.name}: ${quote.lastPrice}")
            },
            onHistoryUpdate = { symbol, history ->
                // Handle historical data
                Log.d("Chart", "$symbol: ${history.size} candles")
            }
        )
    }
    
    // Connect to Deriv on launch
    LaunchedEffect(Unit) {
        dataSourceManager.connectDeriv()
    }
    
    // Subscribe to symbol changes
    LaunchedEffect(symbol) {
        dataSourceManager.streamActiveSymbol(symbol)
        dataSourceManager.fetchHistory(symbol, "1h")
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            dataSourceManager.disconnectAll()
        }
    }
}
```

### Example 2: Using DerivService Directly

```kotlin
import com.trading.app.data.DerivService

val derivService = DerivService(
    onQuoteUpdate = { quote ->
        println("${quote.name}: ${quote.lastPrice}")
    },
    onHistoryUpdate = { symbol, history ->
        println("$symbol: ${history.size} candles")
    }
)

// Connect
derivService.connect()

// Subscribe to BTC/USD
derivService.subscribe("BTCUSD")

// Subscribe to Gold
derivService.subscribe("XAUUSD")

// Fetch 1-hour candles for ETH/USD
derivService.fetchHistory("ETHUSD", "1h")

// Disconnect when done
derivService.disconnect()
```

### Example 3: Multi-Symbol Monitoring

```kotlin
val symbols = listOf("BTCUSD", "ETHUSD", "XAUUSD", "XAGUSD")

val derivService = DerivService(
    onQuoteUpdate = { quote ->
        // Update UI with latest price
        updatePrice(quote.name, quote.lastPrice)
    }
)

derivService.connect()

// Subscribe to all symbols
symbols.forEach { symbol ->
    derivService.subscribe(symbol)
}
```

## API Reference

### DerivService

#### Methods

**`connect()`**
- Establishes WebSocket connection to Deriv
- Auto-reconnects on failure
- Resubscribes to all symbols after reconnection

**`subscribe(symbol: String)`**
- Subscribe to real-time tick updates for a symbol
- Example: `derivService.subscribe("BTCUSD")`

**`streamActiveSymbol(symbol: String)`**
- Stream a single symbol (clears other subscriptions)
- Useful for focused chart views
- Example: `derivService.streamActiveSymbol("ETHUSD")`

**`fetchHistory(symbol: String, timeframe: String, endTime: Long? = null)`**
- Fetch historical candle data
- Timeframes: "1m", "5m", "15m", "30m", "1h", "4h", "1d"
- Returns up to 500 candles
- Example: `derivService.fetchHistory("BTCUSD", "1h")`

**`unsubscribe(symbol: String)`**
- Unsubscribe from a symbol
- Example: `derivService.unsubscribe("BTCUSD")`

**`disconnect()`**
- Close WebSocket connection
- Call in `onDispose` or when app closes

**`isConnected(): Boolean`**
- Check connection status

### MarketDataSourceManager

#### Methods

**`getDataSourceForSymbol(symbol: String): DataSource`**
- Determines which data source to use
- Returns: `DERIV`, `BINANCE`, or `MT5`

**`subscribe(symbol: String)`**
- Subscribe using the appropriate data source
- Automatically routes to Deriv for supported symbols

**`streamActiveSymbol(symbol: String)`**
- Stream a single symbol via appropriate source

**`fetchHistory(symbol: String, timeframe: String, endTime: Long? = null)`**
- Fetch history via appropriate source

**`connectDeriv()`**
- Connect to Deriv WebSocket

**`disconnectAll()`**
- Disconnect all data sources

**`isDerivConnected(): Boolean`**
- Check Deriv connection status

## Data Flow

```
User Request (e.g., "Show BTC/USD chart")
    ↓
MarketDataSourceManager.streamActiveSymbol("BTCUSD")
    ↓
Determines: BTCUSD → Use Deriv
    ↓
DerivService.streamActiveSymbol("BTCUSD")
    ↓
WebSocket: Subscribe to "frxBTCUSD"
    ↓
Deriv Server sends tick updates
    ↓
DerivService.handleTickMessage()
    ↓
onQuoteUpdate callback
    ↓
UI updates with latest price
```

## Timeframe Mapping

| App Timeframe | Deriv Granularity |
|---------------|-------------------|
| 1m | 60 seconds |
| 5m | 300 seconds |
| 15m | 900 seconds |
| 30m | 1800 seconds |
| 1h | 3600 seconds |
| 4h | 14400 seconds |
| 1d | 86400 seconds |

## Error Handling

The DerivService includes robust error handling:

1. **Connection Failures**: Auto-reconnect after 5 seconds
2. **Message Parsing Errors**: Logged but don't crash the app
3. **API Errors**: Logged with error code and message
4. **Subscription Failures**: Logged and can be retried

## Monitoring & Telemetry

The service integrates with `SystemTelemetry` to track:
- Connection events (connected, failed, closed)
- Tick latency (time from server to app)
- Data source usage

## Advanced: Authenticated WebSocket (Optional)

For authenticated trading features, you'll need to:

1. Get an OTP from the REST API:
```kotlin
suspend fun getDerivOTP(accountId: String, token: String): String {
    val response = httpClient.post("https://api.derivws.com/trading/v1/options/accounts/$accountId/otp") {
        header("Deriv-App-ID", BuildConfig.DERIV_APP_ID)
        header("Authorization", "Bearer $token")
    }
    val json = JSONObject(response.body)
    return json.getJSONObject("data").getString("url")
}
```

2. Connect to the authenticated WebSocket:
```kotlin
val otpUrl = getDerivOTP("DOT90004580", BuildConfig.DERIV_API_TOKEN)
val request = Request.Builder().url(otpUrl).build()
webSocket = client.newWebSocket(request, listener)
```

## Testing

To test the integration:

1. **Check Logs**: Look for "DerivService" tags in Logcat
2. **Monitor Connection**: Check for "WEBSOCKET_CONNECTED" events
3. **Verify Data**: Ensure prices update in real-time
4. **Test Reconnection**: Disable/enable network to test auto-reconnect

## Troubleshooting

### No Data Received
- Check internet connection
- Verify symbol is supported (see Supported Symbols table)
- Check Logcat for error messages

### Connection Keeps Dropping
- Check network stability
- Verify firewall/proxy settings
- Ensure WebSocket connections are allowed

### Wrong Data Source Used
- Check `MarketDataSourceManager.getDataSourceForSymbol()`
- Verify symbol normalization is working correctly

## Future Enhancements

Potential improvements:
1. Add more commodity symbols (Natural Gas, Copper, etc.)
2. Implement subscription ID tracking for proper unsubscribe
3. Add support for more crypto pairs
4. Implement rate limiting and backpressure handling
5. Add offline caching of historical data

## Resources

- [Deriv API Documentation](https://api.deriv.com)
- [WebSocket API Reference](https://api.deriv.com/api-explorer)
- [Deriv Developer Portal](https://developers.deriv.com)
