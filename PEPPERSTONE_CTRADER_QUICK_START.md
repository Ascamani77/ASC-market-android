# Pepperstone cTrader Chart - Quick Start Guide

## What is it?

The **Pepperstone cTrader** chart is a new independent chart type that uses the same Pepperstone data source but with its own dedicated connection and state management. This means it operates completely independently from the standard Pepperstone chart.

## How to Use

### 1. Enable the Chart Type

1. Open the app
2. Go to **Settings**
3. Find the **Chart Feed Type** section
4. Select **"Pepperstone cTrader"**
5. The chart will automatically switch to the new independent feed

### 2. What You'll See

- **Display Name:** "Pepperstone cTrader"
- **Description:** "Independent Pepperstone cTrader chart with dedicated connection"
- **Account Label:** "Pepperstone cTrader Independent"

### 3. Features

✅ **Independent Connection**
- Separate WebSocket connection to cTrader bridge
- Won't affect other charts if there's a connection issue
- Independent reconnection logic

✅ **Same Trading Capabilities**
- Place market orders (BUY/SELL)
- Set stop loss and take profit
- Close and modify positions
- Real-time account updates

✅ **Same Symbols**
- All Pepperstone symbols available:
  - Forex: EURUSD, GBPUSD, USDJPY, AUDUSD, USDCAD, NZDUSD, USDCHF
  - Commodities: XAUUSD (Gold), XAGUSD (Silver), USOIL (WTI Crude)
  - Crypto CFDs: BTCUSD, ETHUSD
  - Indices: NAS100, US30, SPX500

✅ **AI Pipeline Integration**
- Publishes to separate Redis stream: `market.ticks.stream.ctrader`
- Source identifier: `pepperstone_ctrader_independent`
- Allows AI to process this feed independently

## Differences from Standard Pepperstone Chart

| Feature | Standard Pepperstone | Pepperstone cTrader (New) |
|---------|---------------------|---------------------------|
| Connection | Shared service | Independent service |
| WebSocket | Shared connection | Dedicated connection |
| State Management | Shared state | Independent state |
| Redis Stream | market.ticks.stream | market.ticks.stream.ctrader |
| Telemetry Tag | PEPPERSTONE_CHART | PEPPERSTONE_CTRADER_CHART |
| Account Label | Pepperstone cTrader | Pepperstone cTrader Independent |

## When to Use

### Use Standard Pepperstone When:
- You want the default Pepperstone experience
- You're already using it and it works fine

### Use Pepperstone cTrader When:
- You want an independent connection
- You're experiencing issues with the standard chart
- You want to isolate this chart from others
- You need separate monitoring/telemetry
- You want the AI pipeline to process this feed separately

## Configuration

The chart uses the same configuration as standard Pepperstone:
- **Host:** From `NetworkConfig.cTraderHost(context)`
- **Port:** From `NetworkConfig.cTraderPort(context)`
- **Trading Service:** Same CTraderService for order execution

## Monitoring

### Logs
Look for these log tags:
- `TradingChartPepperstoneCTrader` - Chart wrapper logs
- `PepperstoneCTraderChartService` - Service logs

### Telemetry Events
- Connection events: `PEPPERSTONE_CTRADER_CHART`
- Tick events: `PEPPERSTONE_CTRADER_CHART`

### Redis Stream
Check the Redis stream for published ticks:
```bash
redis-cli XREAD COUNT 10 STREAMS market.ticks.stream.ctrader 0
```

## Troubleshooting

### Chart Not Loading
1. Check cTrader bridge is running
2. Verify host/port configuration in NetworkConfig
3. Check logs for connection errors with tag `PepperstoneCTraderChartService`

### No Real-Time Updates
1. Check WebSocket connection status in logs
2. Verify symbol is subscribed (check logs for "Streaming symbol")
3. Check if cTrader bridge is sending data

### Orders Not Executing
1. Verify CTraderService is connected
2. Check account status
3. Review order placement logs with tag `TradingChart2`

### Redis Not Publishing
1. Check Redis connection (host: 10.164.138.133, port: 6379)
2. Verify `publishToRedis` is true (default)
3. Check logs for "Published Pepperstone cTrader tick to Redis"

## Technical Details

### Service Lifecycle
```kotlin
// Service is created when chart is composed
val service = remember(cTraderHost, cTraderPort) {
    PepperstoneCTraderChartService(
        host = cTraderHost,
        port = cTraderPort,
        onQuoteUpdate = { ... },
        onHistoryUpdate = { ... }
    )
}

// Service connects and subscribes when symbol/timeframe changes
LaunchedEffect(symbol, timeframe) {
    service.streamActiveSymbol(symbol, timeframe, 500)
}

// Service disconnects when chart is disposed
DisposableEffect(service) {
    onDispose { service.disconnect() }
}
```

### Data Flow
1. User selects symbol/timeframe
2. Chart wrapper creates service instance
3. Service connects to cTrader bridge via WebSocket
4. Service subscribes to symbol/timeframe
5. Bridge sends historical candles
6. Bridge sends real-time ticks
7. Service publishes ticks to Redis
8. Chart displays data
9. User places orders via CTraderService
10. Orders execute on Pepperstone cTrader

## Support

If you encounter issues:
1. Check the logs with relevant tags
2. Verify cTrader bridge is running and accessible
3. Check Redis connection if AI integration is needed
4. Review the full implementation guide: `PEPPERSTONE_CTRADER_CHART_IMPLEMENTATION.md`
