# Fix ModalBottomSheet windowInsets Compilation Error

The project is failing to build because `ModalBottomSheet` in Material 3 1.4.0 (and newer) has renamed its `windowInsets` parameter to `contentWindowInsets` and changed its type to a lambda `@Composable () -> WindowInsets`.

## User Review Required

> [!IMPORTANT]
> This change affects multiple files (approximately 14 files) across the project that use `ModalBottomSheet`. The fix is consistent: renaming `windowInsets = WindowInsets(0)` to `contentWindowInsets = { WindowInsets(0) }`.

## Proposed Changes

### UI Components

I will update all occurrences of `windowInsets = WindowInsets(0)` to `contentWindowInsets = { WindowInsets(0) }` in `ModalBottomSheet` calls.

#### [MODIFY] [EventDetailsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/components/EventDetailsModal.kt)
#### [MODIFY] [ChatScreen.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/screens/ChatScreen.kt)
#### [MODIFY] [AnalysisHubModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/AnalysisHubModal.kt)
#### [MODIFY] [ChartSettingsBottomSheet.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/ChartSettingsBottomSheet.kt)
#### [MODIFY] [ChartTypeModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/ChartTypeModal.kt)
#### [MODIFY] [DrawingsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/DrawingsModal.kt)
#### [MODIFY] [ExitLevelsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/ExitLevelsModal.kt)
#### [MODIFY] [MarketStatusModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/MarketStatusModal.kt)
#### [MODIFY] [OrderModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/OrderModal.kt)
#### [MODIFY] [PaperTradingSettingsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/PaperTradingSettingsModal.kt)
#### [MODIFY] [PositionActionsModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/PositionActionsModal.kt)
#### [MODIFY] [SimpleOrderPage.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/SimpleOrderPage.kt)
#### [MODIFY] [TradingChart.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart.kt)
#### [MODIFY] [TradingChart2.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/kotlin/com/trading/app/components/TradingChart2.kt)

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that all files compile successfully.

### Manual Verification
- N/A (Build fix)
