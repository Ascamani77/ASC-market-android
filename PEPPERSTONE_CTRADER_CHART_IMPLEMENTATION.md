# Pepperstone cTrader Independent Chart Implementation

## Overview
Created a new independent chart type called "Pepperstone cTrader" that uses the same Pepperstone data source but with completely independent connection and state management.

## Files Created

### 1. PepperstoneCTraderChartService.kt
**Location:** `app/src/main/kotlin/com/trading/app/data/PepperstoneCTraderChartService.kt`

**Purpose:** Independent chart service for Pepperstone cTrader
- Completely separate WebSocket connection from standard PepperstoneChartService
- Independent state management (activeSymbol, activeSymbols, activeTimeframe)
- Separate Redis stream for AI pipeline: `market.ticks.stream.ctrader`
- Source identifier: `pepperstone_ctrader_independent`
- Telemetry tag: `PEPPERSTONE_CTRADER_CHART`

**Key Features:**
- Independent WebSocket connection to Pepperstone cTrader bridge
- Separate reconnection logic and error handling
- Independent quote and history update callbacks
- Dedicated Redis publishing for AI pipeline integration
- Full lifecycle management (connect, disconnect, subscribe, unsubscribe)

### 2. TradingChartPepperstoneCTrader.kt
**Location:** `app/src/main/kotlin/com/trading/app/components/TradingChartPepperstoneCTrader.kt`

**Purpose:** Independent chart wrapper component
- Creates its own service instance (not shared with other charts)
- Independent state management for candles, quotes, loading states
- Separate LaunchedEffect for symbol/timeframe changes
- Independent DisposableEffect for cleanup

**Key Features:**
- Completely isolated from TradingChartPepperstone
- Uses ChartFeedType.PEPPERSTONE_CTRADER
- Independent candle data and quote state
- Separate history loading logic
- Dedicated logging with "TradingChartPepperstoneCTrader" tag

## Files Modified

### 1. ChartFeedType.kt
**Changes:**
- Added new enum value: `PEPPERSTONE_CTRADER("pepperstone_ctrader", "Pepperstone cTrader")`
- Updated `chartFeedQuotes()` to handle both PEPPERSTONE and PEPPERSTONE_CTRADER (same symbol list)
- Updated `chartFeedSymbolFor()` to handle PEPPERSTONE_CTRADER symbol normalization

### 2. TradingApp.kt
**Changes:**
- Added chart rendering case for `ChartFeedType.PEPPERSTONE_CTRADER`
- Updated `chartFeedDisplayName()` to return "Pepperstone cTrader Live"
- Updated `liveTradeDefaultAccountLabel()` to return "Pepperstone cTrader Independent"
- Updated service initialization to handle both PEPPERSTONE types
- Updated connection logic to connect cTrader service for both types
- Updated live trade account label logic
- Updated order placement to handle both PEPPERSTONE types
- Updated position closing to handle both PEPPERSTONE types
- Updated provider label display

### 3. TradingChart2.kt
**Changes:**
- Updated SELL order placement to handle both PEPPERSTONE and PEPPERSTONE_CTRADER
- Updated BUY order placement to handle both PEPPERSTONE and PEPPERSTONE_CTRADER

### 4. TradingChart.kt
**Changes:**
- Updated chart subscription logic to handle both PEPPERSTONE types
- Updated load more history logic to handle both PEPPERSTONE types

### 5. SettingsScreen.kt
**Changes:**
- Added description for PEPPERSTONE_CTRADER: "Independent Pepperstone cTrader chart with dedicated connection."

## Architecture

### Independence Features
1. **Separate Service Instance:** Each chart type creates its own PepperstoneCTraderChartService instance
2. **Independent WebSocket:** Separate WebSocket connection to the cTrader bridge
3. **Isolated State:** Completely separate state management for candles, quotes, and loading states
4. **Dedicated Redis Stream:** Uses different Redis stream name for AI pipeline
5. **Separate Telemetry:** Different telemetry tags for monitoring

### Data Source
Both PEPPERSTONE and PEPPERSTONE_CTRADER use the same:
- cTrader bridge host and port (from NetworkConfig)
- Symbol list (same Pepperstone symbols)
- Trading service (CTraderService for order execution)

### Key Differences from Standard Pepperstone Chart
| Feature | Standard Pepperstone | Pepperstone cTrader |
|---------|---------------------|---------------------|
| Service Class | PepperstoneChartService | PepperstoneCTraderChartService |
| Wrapper Component | TradingChartPepperstone | TradingChartPepperstoneCTrader |
| Redis Stream | market.ticks.stream | market.ticks.stream.ctrader |
| Source ID | pepperstone_ctrader | pepperstone_ctrader_independent |
| Telemetry Tag | PEPPERSTONE_CHART | PEPPERSTONE_CTRADER_CHART |
| Display Name | Pepperstone Live Trade | Pepperstone cTrader Live |
| Account Label | Pepperstone cTrader | Pepperstone cTrader Independent |

## Usage

### Selecting the Chart Type
Users can select "Pepperstone cTrader" from the chart feed type selector in Settings. This will:
1. Create a new independent service instance
2. Establish a separate WebSocket connection
3. Stream data independently from other charts
4. Use the same trading service for order execution

### Trading Integration
- Uses the same CTraderService for order placement and position management
- Supports market orders with stop loss and take profit
- Full position management (open, close, modify)
- Real-time account updates

### AI Pipeline Integration
- Publishes ticks to separate Redis stream: `market.ticks.stream.ctrader`
- Source identifier: `pepperstone_ctrader_independent`
- Allows AI pipeline to distinguish between standard and independent cTrader feeds

## Benefits

1. **Isolation:** Complete independence from other chart types
2. **Reliability:** Separate connection means one chart failure doesn't affect others
3. **Flexibility:** Can run multiple Pepperstone charts simultaneously with different configurations
4. **Monitoring:** Separate telemetry allows independent monitoring and debugging
5. **AI Integration:** Separate Redis stream allows AI pipeline to process feeds independently

## Testing Checklist

- [ ] Chart loads and displays candles correctly
- [ ] Real-time quotes update properly
- [ ] Symbol switching works
- [ ] Timeframe switching works
- [ ] History loading (scroll back) works
- [ ] Order placement works (BUY/SELL)
- [ ] Position management works (close, modify)
- [ ] Account updates display correctly
- [ ] Redis publishing works (check stream: market.ticks.stream.ctrader)
- [ ] Telemetry events are recorded with PEPPERSTONE_CTRADER_CHART tag
- [ ] Chart is independent (doesn't affect other charts)
- [ ] Cleanup works properly (no memory leaks)

## Future Enhancements

1. **Configuration Options:** Allow users to configure separate host/port for independent chart
2. **Multiple Instances:** Support multiple independent cTrader charts simultaneously
3. **Performance Monitoring:** Add dedicated performance metrics for independent chart
4. **Advanced Features:** Add chart-specific features like custom indicators or drawing tools
