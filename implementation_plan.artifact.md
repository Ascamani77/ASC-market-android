# Fix Unresolved Reference 'outlinedTextFieldColors'

The build is failing due to an unresolved reference to `outlinedTextFieldColors` in `TextFieldDefaults`. This is likely because the project is using a modern version of Material 3 where this method has been deprecated or removed in favor of `OutlinedTextFieldDefaults.colors`.

## Proposed Changes

### [app](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app)

#### [MODIFY] [MacroStreamScreen.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/screens/MacroStreamScreen.kt)
Replace `TextFieldDefaults.outlinedTextFieldColors` with `OutlinedTextFieldDefaults.colors` and update parameter names as needed.

#### [MODIFY] [TimezoneModal.kt](file:///C:/Users/HP/AndroidStudioProjects/MyRealApp/app/src/main/java/com/asc/markets/ui/terminal/components/TimezoneModal.kt)
Replace `TextFieldDefaults.outlinedTextFieldColors` with `OutlinedTextFieldDefaults.colors`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the unresolved reference error is resolved.

### Manual Verification
- None required as this is a build-time fix.
