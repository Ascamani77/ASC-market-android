# Fix Unresolved Reference 'aiDeployments' in DataVaultScreen.kt

The build is failing because `ForexViewModel` is missing the `aiDeployments` property, which was recently removed or cleared. However, `DataVaultScreen` still depends on it to display AI-related data.

## Proposed Changes

### [logic]

#### [MODIFY] [ForexViewModel.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/logic/ForexViewModel.kt)
- Restore `aiDeployments` by delegating to `aiRepository.deployments`.
- Implement `syncDataVaultNow()`, `runAiPipelineNow()`, and `refreshAiDeploymentsNow()` by calling their respective logic in `aiRepository`.
- This will fix the compilation error and restore the functionality of the Data Vault screen.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the unresolved reference is fixed.

### Manual Verification
- Deploy the app and navigate to the "Data Vault" screen to ensure it displays data (if available from the backend).
