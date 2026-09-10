# Implementation Plan - Remove Binance and AI Deployment Code

This plan outlines the steps to completely remove all Binance-related integration (both trading and "connect" view-only) and AI deployment remnants from the project.

## User Review Required

> [!IMPORTANT]
> This will permanently remove Binance support from the application. The app will rely solely on Exness, Pepperstone, and the MT5 EA for market data and trading.

> [!WARNING]
> AI Deployment logic (remnants of the old Python backend integration) will be removed. Ensure that all necessary AI functionality is now handled by the local MT5 EA integration.

## Proposed Changes

### Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/build.gradle.kts)
- Remove `BINANCE_API_KEY`, `BINANCE_SECRET_KEY`, `BINANCE_DEMO_API_KEY`, `BINANCE_DEMO_SECRET_KEY` build config fields.
- Remove Binance-related comments.
- Evaluate removal of `generativeai` (Gemini) and `jedis` (Redis) if they are no longer used by the EA-only architecture.

### Data Layer

#### [MODIFY] [ChartFeedType.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/data/ChartFeedType.kt)
- Remove `BINANCE` and `BINANCE_CONNECT` enum values.
- Remove Binance-related symbol mappings in `chartFeedQuotes`.
- Update `chartFeedSymbolFor` to remove Binance-specific logic.

### UI Components

#### [MODIFY] [TradingApp.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/TradingApp.kt)
- Remove imports for `BinanceConnectService`, `BinanceFuturesService`, `BinanceTradingService`, `BinanceService`, `BinanceTradingMode`, `BinanceMarketType`.
- Remove `binanceTradingMode` state.
- Remove `binanceQuoteService`, `binanceConnectQuoteService`, `binanceTradingService`, `binanceFuturesService` definitions and their lifecycle management (`LaunchedEffect`, `DisposableEffect`).
- Remove Binance-related logic in `liveTradeSourceName`, `liveTradeDefaultAccountLabel`, `placeStreamOrder`, `closeStreamPosition`.
- Remove UI references to `TradingChartBinance` and `TradingChartBinanceConnect`.
- Cleanup `PaperTradingPanel` for Binance labels.

#### [MODIFY] [TradingChart.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart.kt)
- Remove `BinanceMarketType` parameter and internal state.
- Remove Binance-specific ticker normalization and subscription logic.
- Remove `ChartFeedType.BINANCE` and `ChartFeedType.BINANCE_CONNECT` branches in `LaunchedEffect`.

### Cleanup

#### [DELETE] Leftover Binance/AI Files
- Any files mentioned in `UNUSED_CODE_DELETED.md` that might still exist on disk (e.g., `BinanceConnectService.kt`, `TradingChartBinance.kt`).

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure all unresolved references are fixed and the project builds.

### Manual Verification
- Deploy the app to a device/emulator.
- Verify that Binance options are gone from Settings and Source selectors.
- Verify that Exness and Pepperstone feeds still work correctly.
- Verify that the EA Live data still flows into the Accumulation Radar and charts.
