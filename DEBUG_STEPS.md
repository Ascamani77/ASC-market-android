# Debug Steps - Binance Balance Issue

## Step 1: Rebuild the App

You MUST rebuild the app for the logging changes to take effect:

```bash
cd MyRealApp
./gradlew clean assembleDebug
```

Wait for the build to complete successfully.

## Step 2: Install the New APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Step 3: Start Logcat BEFORE Opening the App

```bash
adb logcat -s BinanceBalance:D BinanceTrading:D *:E
```

This will show:
- All BinanceBalance logs (our debug logs)
- All BinanceTrading logs (API calls)
- All ERROR logs from any component

## Step 4: Open the App and Navigate

1. **Open the app**
2. **Make sure you're in Binance mode** (not Pepperstone or Exness)
3. **Open the paper trading panel** (tap the currency/balance button)

You should immediately see logs like:
```
BinanceBalance: LaunchedEffect triggered: chartFeedType=BINANCE, showPaperTradingPanel=true, binanceTradingMode=DEMO
BinanceBalance: Entering BINANCE branch
BinanceBalance: Binance service IS configured, fetching account info...
BinanceBalance: === Binance Account Balances ===
...
```

## Step 5: What to Look For

### If you see NO logs at all:
- The app wasn't rebuilt
- You're not in Binance mode (check which feed is selected)
- The paper trading panel isn't open

### If you see "Binance service NOT configured":
- API keys are missing or empty
- Check `env.demo` file exists and has keys

### If you see "Failed to fetch account info":
- Network issue
- Wrong API keys
- API endpoint is wrong

### If you see the balance breakdown:
- You'll see exactly which stablecoins you have
- This will explain the 10,000 total

## Step 6: Share the Logs

Copy the entire logcat output and share it so we can see what's happening.

## Quick Test - Check if App Was Rebuilt

Run this to see the APK build time:
```bash
ls -l app/build/outputs/apk/debug/app-debug.apk
```

The timestamp should be AFTER you ran the gradle build command.

## Alternative: Use Android Studio Logcat

If you prefer Android Studio:
1. Open Android Studio
2. Go to **View** → **Tool Windows** → **Logcat**
3. In the filter box, type: `BinanceBalance`
4. Run the app from Android Studio
5. Open the paper trading panel
