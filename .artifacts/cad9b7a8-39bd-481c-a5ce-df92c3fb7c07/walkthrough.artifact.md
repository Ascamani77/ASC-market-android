# Walkthrough - Fixed ModalBottomSheet windowInsets Compilation Error

I have fixed the compilation error "No parameter with name 'windowInsets' found" in `ModalBottomSheet` calls across the project. This was due to an API change in Material 3 1.4.0 where `windowInsets` was renamed to `contentWindowInsets` and its type changed to a composable lambda.

## Changes

I updated the following files to use `contentWindowInsets = { WindowInsets(0) }` instead of `windowInsets = WindowInsets(0)`:

- [EventDetailsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/components/EventDetailsModal.kt)
- [ChatScreen.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/screens/ChatScreen.kt)
- [AnalysisHubModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/AnalysisHubModal.kt)
- [ChartSettingsBottomSheet.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/ChartSettingsBottomSheet.kt)
- [ChartTypeModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/ChartTypeModal.kt)
- [DrawingsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/DrawingsModal.kt)
- [ExitLevelsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/ExitLevelsModal.kt)
- [MarketStatusModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/MarketStatusModal.kt)
- [OrderModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/OrderModal.kt)
- [PaperTradingSettingsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/PaperTradingSettingsModal.kt)
- [PositionActionsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/PositionActionsModal.kt)
- [SimpleOrderPage.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/SimpleOrderPage.kt)
- [TradingChart.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart.kt)
- [TradingChart2.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart2.kt)

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin`.
- **Result:** The errors related to `ModalBottomSheet` and `windowInsets` are resolved. Note that there are other pre-existing compilation errors in the project (e.g., in `EASimulationPanel.kt`, `MacroStreamScreen.kt`, `MarketOverviewTab.kt`, etc.) which are unrelated to this change and need to be addressed separately.
