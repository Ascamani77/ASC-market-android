# Chart Zoom Independence Fix

## Problem
All chart types (Binance, Pepperstone cTrader, Binance Connect, and Exness) were sharing the same zoom settings. When you zoomed in or out on one chart type, all other chart types would have the same zoom level.

## Root Cause
The issue was in `TradingChart.kt` where several state variables were using `remember` without including `chartFeedType` as a key. This caused Compose to reuse the same state instances across different chart types, including:

1. **`chartsViewApi`** - The main chart view instance that holds the zoom/visible range state
2. **`seriesApi`** - The main candlestick series API
3. **All indicator series APIs** - EMA, SMA, RSI, MACD, etc.
4. **Chart data state** - `ohlcData`, `isLoadingMore`, `hasMoreHistory`
5. **Quote state** - `currentQuoteState`
6. **Initial fit state** - `hasFittedInitialHistory`

When Compose recomposed with a different `chartFeedType`, it would reuse the same `ChartsView` instance, which internally maintains the zoom/visible range state. This caused all chart types to share the same zoom level.

## Solution

### Part 1: Independent State per Chart Type
Added `chartFeedType` as a key to all relevant `remember` blocks in `TradingChart.kt`:

#### Changed State Variables:
```kotlin
// Before:
var seriesApi by remember { mutableStateOf<SeriesApi?>(null) }
var chartsViewApi by remember { mutableStateOf<ChartsView?>(null) }
var hasFittedInitialHistory by remember { mutableStateOf(false) }

// After:
var seriesApi by remember(chartFeedType) { mutableStateOf<SeriesApi?>(null) }
var chartsViewApi by remember(chartFeedType) { mutableStateOf<ChartsView?>(null) }
var hasFittedInitialHistory by remember(chartFeedType) { mutableStateOf(false) }
```

#### All Fixed State Variables:
1. `seriesApi` - Main candlestick series
2. `chartsViewApi` - Main chart view (holds zoom state)
3. `hasFittedInitialHistory` - Initial fit flag
4. `ohlcData` - Chart candle data
5. `isLoadingMore` - Loading state
6. `hasMoreHistory` - History availability
7. `useMt5FallbackForCrypto` - Fallback flag
8. `currentQuoteState` - Current quote
9. All indicator series APIs (20+ variables):
   - `ema10SeriesApi`, `ema20SeriesApi`
   - `sma1SeriesApi`, `sma2SeriesApi`
   - `vwapSeriesApi`, `vwapUpperSeriesApi`, `vwapLowerSeriesApi`, etc.
   - `bbUpperSeriesApi`, `bbMiddleSeriesApi`, `bbLowerSeriesApi`, etc.
   - `macdLineSeriesApi`, `macdSignalSeriesApi`, `macdHistogramSeriesApi`
   - `volumeSeriesApi`, `volumeMaSeriesApi`
   - `atrSeriesApi`
   - Band fill and mask series

### Part 2: Different Initial Zoom Levels per Chart Type
Set different `barSpacing` values in `TimeScaleOptions` based on chart type:

```kotlin
timeScale = TimeScaleOptions(
    borderColor = chartSettings.canvas.scaleLineColor.toIntColor(),
    visible = true,
    timeVisible = true,
    rightOffset = 15f,
    barSpacing = when (chartFeedType) {
        ChartFeedType.EXNESS -> 12f  // 50% more zoomed in
        ChartFeedType.BINANCE -> 12f  // 50% more zoomed in
        ChartFeedType.BINANCE_CONNECT -> 12f  // 50% more zoomed in
        ChartFeedType.PEPPERSTONE_CTRADER -> 8f  // Default zoom
        else -> 8f
    }
)
```

**Zoom Levels:**
- **Exness**: 12f barSpacing (50% more zoomed in)
- **Binance**: 12f barSpacing (50% more zoomed in)
- **Binance Connect**: 12f barSpacing (50% more zoomed in)
- **Pepperstone cTrader**: 8f barSpacing (default zoom level)

Higher `barSpacing` values = more zoomed in (fewer candles visible, more detail per candle)

## How It Works Now
Each chart type (identified by `chartFeedType`) now has its own:
- Chart view instance with independent zoom/visible range
- Series API instances
- Chart data and loading states
- Quote state
- Indicator series
- Initial zoom level (barSpacing)

When you switch between chart types, Compose will:
1. Dispose of the old chart type's state
2. Create new state instances for the new chart type
3. Each chart type maintains its own zoom level independently
4. Each chart type starts with its configured initial zoom level

## Chart Types
The four independent chart types are:
1. **EXNESS** (`"exness"`) - 50% more zoomed in
2. **PEPPERSTONE_CTRADER** (`"pepperstone_ctrader"`) - Default zoom
3. **BINANCE** (`"binance"`) - 50% more zoomed in
4. **BINANCE_CONNECT** (`"binance_connect"`) - 50% more zoomed in

## Testing
To verify the fix:
1. Open Binance chart - should be zoomed in 50% more than before
2. Switch to Pepperstone cTrader chart - should have default zoom level
3. Switch to Binance Connect chart - should be zoomed in 50%
4. Switch to Exness chart - should be zoomed in 50%
5. Zoom manually on any chart and switch away, then back - it should remember your manual zoom
6. Each chart type should maintain its own independent zoom state

## Files Modified
- `app/src/main/kotlin/com/trading/app/components/TradingChart.kt`

## Date
May 20, 2026
