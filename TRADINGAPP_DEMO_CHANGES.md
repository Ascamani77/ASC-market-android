# TradingApp.kt Changes for Pepperstone Demo

## Changes Needed

### 1. Add Demo Services (after line 570)
```kotlin
// Add after cTraderTradingService initialization
val cTraderDemoTradingService = remember {
    CTraderDemoService(
        onQuoteUpdate = { quote ->
            if (chartFeedType == ChartFeedType.PEPPERSTONE_DEMO) {
                cacheSelectedSourceQuote(quote)
            }
        },
        onPositionsUpdate = { updatedPositions ->
            Log.d("TradingApp", "cTrader DEMO positions update received: ${updatedPositions.size} positions")
            updatedPositions.forEachIndexed { index, pos ->
                Log.d("TradingApp", "Demo Position $index: id=${pos.id}, symbol=${pos.symbol}, type=${pos.type}, volume=${pos.volume}")
            }
            if (chartFeedType == ChartFeedType.PEPPERSTONE_DEMO) {
                paperPositions = updatedPositions
            }
        },
        onAccountUpdate = { accountInfo ->
            Log.d("TradingApp", "cTrader DEMO account update received: balance=${accountInfo?.balance}, equity=${accountInfo?.equity}")
            if (chartFeedType == ChartFeedType.PEPPERSTONE_DEMO) {
                mt5AccountInfo = accountInfo
            }
        },
        onConnectionStatusUpdate = { connected ->
            if (chartFeedType == ChartFeedType.PEPPERSTONE_DEMO) {
                isConnected = connected
            }
        }
    )
}

val pepperstoneDemoQuoteService = remember {
    val prefs = context.getSharedPreferences("asc_prefs", android.content.Context.MODE_PRIVATE)
    val redisHost = prefs.getString("redis_host", "10.164.138.133") ?: "10.164.138.133"
    val redisPort = prefs.getInt("redis_port", 6379)
    
    PepperstoneDemoChartService(
        host = cTraderDemoHost,
        port = cTraderDemoPort,
        onQuoteUpdate = { quote ->
            if (chartFeedType == ChartFeedType.PEPPERSTONE_DEMO) {
                cacheSelectedSourceQuote(quote)
            }
        },
        redisHost = redisHost,
        redisPort = redisPort,
        publishToRedis = true
    )
}
```

### 2. Add Demo Host/Port (after line 404)
```kotlin
val cTraderDemoHost = remember { NetworkConfig.cTraderDemoHost(context) }
val cTraderDemoPort = remember { NetworkConfig.cTraderDemoPort(context) }
```

### 3. Update DisposableEffect (around line 489)
```kotlin
DisposableEffect(Unit) {
    onDispose {
        pepperstoneQuoteService.disconnect()
        pepperstoneDemoQuoteService.disconnect()  // ADD THIS
        binanceQuoteService.disconnect()
        binanceConnectQuoteService.disconnect()
    }
}
```

### 4. Update LaunchedEffect for subscriptions (around line 507)
```kotlin
LaunchedEffect(chartFeedType, sourceSymbols, timeframe) {
    when (chartFeedType) {
        ChartFeedType.EXNESS -> {
            pepperstoneQuoteService.stopActiveStream()
            pepperstoneDemoQuoteService.stopActiveStream()  // ADD THIS
            binanceQuoteService.stopActiveStream()
            binanceConnectQuoteService.stopActiveStream()
            mt5Service.updateWatchlist(watchlistSymbols.ifEmpty { sourceSymbols })
        }
        ChartFeedType.PEPPERSTONE_CTRADER -> {
            pepperstoneDemoQuoteService.stopActiveStream()  // ADD THIS
            binanceQuoteService.stopActiveStream()
            binanceConnectQuoteService.stopActiveStream()
            pepperstoneQuoteService.subscribeSymbols(sourceSymbols, timeframe)
        }
        ChartFeedType.PEPPERSTONE_DEMO -> {  // ADD THIS ENTIRE BLOCK
            pepperstoneQuoteService.stopActiveStream()
            binanceQuoteService.stopActiveStream()
            binanceConnectQuoteService.stopActiveStream()
            pepperstoneDemoQuoteService.subscribeSymbols(sourceSymbols, timeframe)
        }
        ChartFeedType.BINANCE -> {
            pepperstoneQuoteService.stopActiveStream()
            pepperstoneDemoQuoteService.stopActiveStream()  // ADD THIS
            binanceConnectQuoteService.stopActiveStream()
            binanceQuoteService.subscribeSymbols(sourceSymbols, timeframe)
        }
        ChartFeedType.BINANCE_CONNECT -> {
            pepperstoneQuoteService.stopActiveStream()
            pepperstoneDemoQuoteService.stopActiveStream()  // ADD THIS
            binanceQuoteService.stopActiveStream()
            binanceConnectQuoteService.subscribeSymbols(sourceSymbols, timeframe)
        }
    }
}
```

### 5. Update connection LaunchedEffect (around line 750)
```kotlin
LaunchedEffect(chartFeedType) {
    when (chartFeedType) {
        ChartFeedType.EXNESS -> {
            cTraderTradingService.disconnect()
            cTraderDemoTradingService.disconnect()  // ADD THIS
            reverseBridge.disconnect()
            mt5Service.connect()
        }
        ChartFeedType.PEPPERSTONE_CTRADER -> {
            cTraderDemoTradingService.disconnect()  // ADD THIS
            reverseBridge.disconnect()
            mt5Service.disconnect()
            cTraderTradingService.connect()
        }
        ChartFeedType.PEPPERSTONE_DEMO -> {  // ADD THIS ENTIRE BLOCK
            cTraderTradingService.disconnect()
            reverseBridge.disconnect()
            mt5Service.disconnect()
            cTraderDemoTradingService.connect()
        }
        ChartFeedType.BINANCE -> {
            reverseBridge.disconnect()
            mt5Service.disconnect()
            cTraderTradingService.disconnect()
            cTraderDemoTradingService.disconnect()  // ADD THIS
            isConnected = binanceTradingService.isConfigured()
        }
        ChartFeedType.BINANCE_CONNECT -> {
            reverseBridge.disconnect()
            mt5Service.disconnect()
            cTraderTradingService.disconnect()
            cTraderDemoTradingService.disconnect()  // ADD THIS
            Log.d("BinanceConnect", "View-only mode - no trading connections")
        }
    }
}
```

### 6. Update symbol subscription (around line 950)
```kotlin
LaunchedEffect(symbol, chartFeedType) {
    when (chartFeedType) {
        ChartFeedType.EXNESS -> {
            liveTradeAccountLabel = "Exness MT5"
        }
        ChartFeedType.PEPPERSTONE_CTRADER -> {
            liveTradeAccountLabel = "Pepperstone cTrader"
            cTraderTradingService.subscribe(brokerSymbolForTicker(symbol))
        }
        ChartFeedType.PEPPERSTONE_DEMO -> {  // ADD THIS ENTIRE BLOCK
            liveTradeAccountLabel = "Pepperstone Demo"
            cTraderDemoTradingService.subscribe(brokerSymbolForTicker(symbol))
        }
        else -> {}
    }
}
```

### 7. Update disconnect on dispose (around line 970)
```kotlin
DisposableEffect(Unit) {
    onDispose {
        reverseBridge.disconnect()
        mt5Service.disconnect()
        cTraderTradingService.disconnect()
        cTraderDemoTradingService.disconnect()  // ADD THIS
        binanceFuturesService.disconnectUserDataStream()
    }
}
```

### 8. Update order placement (around line 1110)
```kotlin
ChartFeedType.PEPPERSTONE_CTRADER -> {
    if (orderType == "Market Execution") {
        cTraderTradingService.placeMarketOrder(
            symbol = brokerSymbolForTicker(position.symbol),
            side = position.type,
            volume = position.volume.toDouble(),
            stopLoss = position.sl?.toDouble(),
            takeProfit = position.tp?.toDouble()
        ) { success, message ->
            // ... existing code
        }
    }
}
ChartFeedType.PEPPERSTONE_DEMO -> {  // ADD THIS ENTIRE BLOCK
    if (orderType == "Market Execution") {
        cTraderDemoTradingService.placeMarketOrder(
            symbol = brokerSymbolForTicker(position.symbol),
            side = position.type,
            volume = position.volume.toDouble(),
            stopLoss = position.sl?.toDouble(),
            takeProfit = position.tp?.toDouble()
        ) { success, message ->
            Log.d("TradingApp", "Pepperstone DEMO order result: success=$success message=$message")
            if (success) {
                tradeNotifications.add(
                    TradeNotification(
                        id = UUID.randomUUID().toString(),
                        symbol = position.symbol,
                        volume = position.volume,
                        isBuy = position.type == "buy",
                        type = "executed",
                        exchange = "Pepperstone Demo"
                    )
                )
            }
            onResult(success, message)
        }
    }
}
```

### 9. Update position closing (around line 1195)
```kotlin
ChartFeedType.PEPPERSTONE_CTRADER -> {
    cTraderTradingService.closePosition(
        positionId = position.id,
        volume = position.volume.toDouble()
    ) { success, message ->
        // ... existing code
    }
}
ChartFeedType.PEPPERSTONE_DEMO -> {  // ADD THIS ENTIRE BLOCK
    cTraderDemoTradingService.closePosition(
        positionId = position.id,
        volume = position.volume.toDouble()
    ) { success, message ->
        Log.d("TradingApp", "Pepperstone DEMO close result: success=$success message=$message")
        if (success) {
            tradeNotifications.add(
                TradeNotification(
                    id = UUID.randomUUID().toString(),
                    symbol = position.symbol,
                    volume = position.volume,
                    isBuy = position.type == "buy",
                    type = "closed",
                    exchange = "Pepperstone Demo"
                )
            )
        }
        onResult(success, message)
    }
}
```

### 10. Update display labels (around line 363)
```kotlin
fun liveTradeDefaultAccountLabel(): String {
    return when (chartFeedType) {
        ChartFeedType.BINANCE_CONNECT -> "Binance Connect (View Only)"
        ChartFeedType.EXNESS -> "Exness Live Trade"
        ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader Live"
        ChartFeedType.PEPPERSTONE_DEMO -> "Pepperstone Demo"  // ADD THIS
        else -> "Live Trade"
    }
}

fun brokerDisplayName(): String {
    return when (chartFeedType) {
        ChartFeedType.BINANCE_CONNECT -> "Binance Connect (No Trading)"
        ChartFeedType.EXNESS -> "Exness MT5"
        ChartFeedType.PEPPERSTONE_CTRADER -> "Pepperstone cTrader"
        ChartFeedType.PEPPERSTONE_DEMO -> "Pepperstone Demo"  // ADD THIS
        else -> "Broker"
    }
}
```

## Summary

Total additions:
- 2 new service instances (CTraderDemoService, PepperstoneDemoChartService)
- 2 new config variables (cTraderDemoHost, cTraderDemoPort)
- ~15 places where PEPPERSTONE_DEMO case needs to be added
- All demo operations isolated from live operations

## Testing After Implementation

1. Select "Pepperstone Demo" from broker list
2. Verify connection to port 8083
3. Check live prices flowing
4. Verify demo balance shows $50,000
5. Place demo order
6. Close demo position
7. Switch to "Pepperstone cTrader" (live) and verify it uses port 8082
