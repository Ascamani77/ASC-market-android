# Remove All Fallback Logic and Fix Compilation Errors

This plan outlines the complete removal of fallback data mechanisms across the entire project. The architecture is shifting to a strict "Unified MT5 EA Only" model for trading and market data.

## User Review Required

> [!CAUTION]
> This is a destructive cleanup task. I am removing several files and significant blocks of code responsible for "fallback" or "synthetic" data generation.
> - **Deriv Integration**: Entirely removed.
> - **Synthetic Charts**: `generateFallbackSeries` and similar logic in `MultiTimeframeScreen` will be removed.
> - **Mock News**: `getMockAscNews` in `MarketOverviewTab` will be removed.
> - **Fallback Stores**: `CombinedFallbackDataStore` and its routing in `TradingApp` will be removed.

## Proposed Changes

### [Dependency Management]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/gradle/libs.versions.toml)
- Update `compose-bom` to `2026.06.01` to fix `ComposableFunctionX` and `weight` compilation errors.

### [Data Layer]

#### [DELETE] [DerivService.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/data/DerivService.kt)
#### [DELETE] [MarketDataSourceManager.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/data/MarketDataSourceManager.kt)
#### [DELETE] [DerivIntegrationExample.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/examples/DerivIntegrationExample.kt)

#### [MODIFY] [OrderBookStore.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/data/OrderBookStore.kt)
- Remove `isFallback` flag and synthetic refresh logic.

### [UI Components]

#### [MODIFY] [TradingApp.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/TradingApp.kt)
- Remove `CombinedFallbackDataStore` references.
- Remove invalid `PaperTradingPanel` parameters (`onMarketTypeChange`, `currentMarketType`).
- Inline `chartFeedType = streamFeedType`.

#### [MODIFY] [TradingChart.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart.kt)
- Remove `pepperstoneChartService`.
- Remove `useMt5FallbackForCrypto` logic.
- Clean up dead code branches for `PEPPERSTONE_CTRADER` and `PEPPERSTONE_DEMO`.

#### [MODIFY] [Quotes.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/Quotes.kt)
- Remove `defaultQuotesCatalog`.
- Simplify `defaultQuoteSymbols()` to exclude `BINANCE` and `PEPPERSTONE_CTRADER`.

#### [MODIFY] [MultiTimeframeScreen.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/screens/MultiTimeframeScreen.kt)
- Remove `generateFallbackSeries` and return empty data when MT5 is unavailable.

#### [MODIFY] [MarketOverviewTab.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/screens/dashboard/MarketOverviewTab.kt)
- Remove `getMockAscNews()` and associated fallback logic.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure all references are resolved and code compiles.

### Manual Verification
- Verify the app launches with only Exness/MT5 data sources.
- Check that "Mock News" is no longer visible in the Market Overview.
- Ensure the Order Book does not show fallback/synthetic depth.
