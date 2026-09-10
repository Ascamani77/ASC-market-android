# Implementation Plan - Remove cTrader and Pepperstone Integration

The user no longer needs the Paper Trading panel and wants to completely remove all cTrader and Pepperstone related code, files, and configurations from the project.

## User Review Required

> [!CAUTION]
> This is a comprehensive cleanup that will remove all trading functionality related to cTrader and Pepperstone. Ensure that no other features depend on these services (e.g., Redis might still be needed for other AI features, so I will be careful with shared dependencies).

## Proposed Changes

### Components & UI

#### [DELETE] [PaperTradingPanel.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/PaperTradingPanel.kt)
#### [DELETE] [TradingChartPepperstoneCTrader.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChartPepperstoneCTrader.kt)
#### [DELETE] [TradingChartPepperstoneDemo.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChartPepperstoneDemo.kt)

#### [MODIFY] [TradingApp.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/TradingApp.kt)
- Remove `PaperTradingPanel` usage and `showPaperTradingPanel` state.
- Remove cTrader/Pepperstone related imports.

#### [MODIFY] [TradingChart.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart.kt)
- Remove cTrader host/port settings and rendering logic for `PEPPERSTONE_CTRADER`.

#### [MODIFY] [AssetIcons.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/AssetIcons.kt) & [Quotes.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/Quotes.kt)
- Remove Pepperstone-specific colors and branding.

### Data & Logic

#### [DELETE] [CTraderBridgeClient.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/network/CTraderBridgeClient.kt)
#### [DELETE] [CTraderHistoryCache.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/data/CTraderHistoryCache.kt)

#### [MODIFY] [ChartFeedType.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/data/ChartFeedType.kt)
- Remove `PEPPERSTONE_CTRADER` and `PEPPERSTONE_DEMO` enum values.
- Clean up `chartFeedQuotes()` and `fromPref()`.

#### [MODIFY] [ForexViewModel.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/logic/ForexViewModel.kt)
- Remove `CTraderBridgeClient` initialization and usage.

#### [MODIFY] [NetworkConfig.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/data/NetworkConfig.kt)
- Remove cTrader bridge host and port configurations.

#### [MODIFY] [SystemTelemetry.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/data/SystemTelemetry.kt) & [MarketDataStore.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/data/MarketDataStore.kt)
- Remove telemetry tracking for cTrader/Pepperstone.

### Infrastructure & Config

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/build.gradle.kts)
- Remove `CTRADER_*` `buildConfigField` entries.

#### [DELETE] Bridge Scripts & Docker Files
- Delete `ctrader_bridge.py`, `Dockerfile.ctrader`, `docker-compose.ctrader.yml`, and other related scripts in the root directory.
- Delete the numerous Pepperstone/cTrader documentation `.md` files in the root.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the project builds successfully without any unresolved references.

### Manual Verification
- Verify that the Paper Trading panel is no longer accessible or referenced in the app.
- Ensure the chart feed options no longer include Pepperstone.
