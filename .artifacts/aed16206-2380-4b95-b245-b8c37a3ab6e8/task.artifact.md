# Tasks - Remove Binance and AI Deployment Code

- [ ] Modify `app/build.gradle.kts` to remove Binance build config fields and unused dependencies.
- [ ] Modify `app/src/main/kotlin/com/trading/app/data/ChartFeedType.kt` to remove Binance enum values.
- [ ] Modify `app/src/main/kotlin/com/trading/app/TradingApp.kt` to remove all Binance and AI deployment references.
- [ ] Modify `app/src/main/kotlin/com/trading/app/components/TradingChart.kt` to remove Binance-specific logic.
- [ ] Delete Binance-related data service files.
- [ ] Verify build with `./gradlew :app:compileDebugKotlin`.
