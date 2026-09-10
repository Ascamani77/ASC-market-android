# Fix Unresolved reference 'BinanceService' in MarketOverviewTab.kt

The project has recently undergone a major refactoring to remove all Binance-related dependencies and services, moving towards an EA-only data architecture. `MarketOverviewTab.kt` was partially missed during this cleanup, leading to build errors because `BinanceService` has been deleted from the codebase.

## Proposed Changes

### [Component Name] UI Layer (Dashboard)

#### [MODIFY] [MarketOverviewTab.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/screens/dashboard/MarketOverviewTab.kt)

- **Remove deleted import**: Delete `import com.trading.app.data.BinanceService`.
- **Remove Binance-specific history fetching**:
    - Delete `rememberOverviewBoardBinanceHistory` function.
    - Delete `shouldUseOverviewBoardBinanceHistory` function.
    - Delete `overviewBoardBinanceInterval` function (used only by `rememberOverviewBoardBinanceHistory`).
- **Simplify Chart State**:
    - Update `rememberOverviewBoardChartState` to remove `liveBinanceHistory` and `expectsLiveHistory` logic.
    - The chart will now exclusively use `derivedCandles` generated from `MarketDataStore`'s `priceHistory`.
- **Clean up remaining Binance-related references**:
    - Ensure all logic previously branching on "crypto/USDT" symbols now follows the standard `MarketDataStore` path.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the build error is resolved.
- Run `./gradlew :app:assembleDebug` to ensure the entire module builds correctly.

### Manual Verification
- Deploy the app and navigate to the "Market Overview" tab.
- Verify that charts for all asset classes (Crypto, Forex, etc.) still render correctly using the available data from `MarketDataStore`.
- Check that switching categories in the overview board works as expected without crashing.
