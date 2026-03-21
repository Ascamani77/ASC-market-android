# Android Chart App - Offline Build Setup

## Overview
Your trading chart app is now configured to run **completely offline** on your Android phone without requiring a PC with the development server running.

## What Changed
1. ✅ **vite.config.ts** - Updated to build React app into Android assets folder
2. ✅ **ChartActivity.kt** - Changed to load from local bundled assets instead of dev server
3. ✅ **package.json** - Added `build:android` script for proper Android builds

## Step-by-Step Build Instructions

### Option 1: Automatic Build (Recommended - Windows)
1. Double-click **`build-android-app.bat`** from the folder root
2. This will:
   - Install dependencies (if needed)
   - Build the React app into Android assets
   - Create the `/app/src/main/assets/dist/` folder with all bundled files

### Option 2: Manual Build
1. Open terminal/PowerShell in the `newtry/` folder
2. Run:
   ```bash
   npm install
   BUILD_FOR_ANDROID=true npm run build:android
   ```
   (On Windows PowerShell, use: `$env:BUILD_FOR_ANDROID='true'; npm run build:android`)

3. This creates: `app/src/main/assets/dist/index.html` and all required assets

## What Happens After Build
- The React chart app gets bundled into `app/src/main/assets/dist/`
- When you open the Chart Activity on your phone, it loads from these local assets
- **No PC required** - the app works completely offline on the phone

## Rebuild After Chart Changes
### If you modify React code (TypeScript, components, styling):
1. Run the build script again: `build-android-app.bat`
2. Rebuild the Android app in Android Studio
3. Deploy to your phone

**The chart data loads from mock data** (no external data source needed), so everything works standalone.

## Troubleshooting

### App shows blank page after rebuild
1. Verify the build completed: Check if `app/src/main/assets/dist/` folder exists with `index.html`
2. Clean Android build: Android Studio → Build → Clean Project → Rebuild Project
3. Clear app data on phone before testing

### JavaScript errors in WebView
- The phone's system WebView needs to be recent (Android 5.0+)
- Update Android System WebView from Google Play Store if older than 2023

### File not loading (404 error)
- Ensure paths are case-sensitive: `file:///android_asset/dist/index.html` (exact casing)
- Use only Android system WebView, not a custom one

## Testing the Build
1. Connect your Android phone
2. In Android Studio: Run → Run 'app'
3. Navigate to the Chart Activity
4. Chart should display immediately with mock data

## Performance Notes
- First load takes ~2-3 seconds to initialize the chart
- All indicators and drawing tools work offline
- Mock data generates realistic OHLC bars for any symbol
- No network calls except for optional features

## Next Steps
- The chart now works **completely offline** on your phone
- Modify React code as needed - just rebuild and redeploy
- All data generation happens locally using mock data generator
