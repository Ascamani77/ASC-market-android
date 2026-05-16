# Binance Demo Balance Issue - Debugging Guide

## Issue
Your Binance Demo account shows **5,000 USDT** on the web, but your app displays **10,000.00**.

## Root Cause Analysis

### 1. ✅ Fixed: Wrong API URLs
**Problem:** Your app was using Testnet URLs instead of Demo Mode URLs.

**Fixed in:**
- `BinanceTradingService.kt` - Changed from `testnet.binance.vision` to `demo-api.binance.com`
- `BinanceService.kt` - Changed WebSocket from `stream.testnet.binance.vision` to `demo-stream.binance.com`

### 2. ✅ Verified: API Keys Configuration
**Demo keys are correctly configured in `env.demo`:**
```
BINANCE_DEMO_API_KEY=swtkYkyVTZeznnuJzTRZHbfsYlfKejgUVeIMRdtYtsM4jAN6L8dHGyRcq4Wcmr4V
BINANCE_DEMO_SECRET_KEY=Pq3jNxi1qQFjqQjoWx4fZBXTrDU5GddWS5Uj6cwuJB3d17leKY7TJqWf32HFb7hX
```

### 3. 🔍 Investigating: Balance Calculation
**The app sums ALL stablecoins:**
```kotlin
val stableAssets = setOf("USDT", "USDC", "BUSD", "FDUSD", "TUSD", "DAI")
val cashBalance = account.balances
    .filter { it.asset.uppercase(Locale.US) in stableAssets }
    .sumOf { it.free + it.locked }
```

**Possible reasons for 10,000 instead of 5,000:**
1. You have multiple stablecoins (e.g., 5,000 USDT + 5,000 USDC)
2. The demo account has different balances than expected
3. The API is returning cached/old data

## Debug Logging Added

Added detailed logging in `TradingApp.kt` to track exactly what the API returns:

```kotlin
Log.d("BinanceBalance", "=== Binance Account Balances ===")
Log.d("BinanceBalance", "Trading Mode: $binanceTradingMode")
account.balances.forEach { balance ->
    if ((balance.free + balance.locked) > 0.0) {
        Log.d("BinanceBalance", "${balance.asset}: free=${balance.free}, locked=${balance.locked}, total=${balance.free + balance.locked}")
    }
}
```

## How to Debug

### Step 1: Rebuild the App
```bash
cd MyRealApp
./gradlew clean assembleDebug
```

### Step 2: Install and Run
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Check Logs
```bash
adb logcat -s BinanceBalance
```

You should see output like:
```
BinanceBalance: === Binance Account Balances ===
BinanceBalance: Trading Mode: DEMO
BinanceBalance: USDT: free=5000.0, locked=0.0, total=5000.0
BinanceBalance: USDC: free=5000.0, locked=0.0, total=5000.0
BinanceBalance: Stablecoin balances:
BinanceBalance:   USDT: 5000.0
BinanceBalance:   USDC: 5000.0
BinanceBalance: Total cash balance: 10000.0
BinanceBalance: ================================
```

### Step 4: Verify on Binance Web
1. Go to https://demo.binance.com/
2. Click on **Wallet** → **Spot**
3. Check ALL stablecoin balances (USDT, USDC, BUSD, FDUSD, TUSD, DAI)
4. Sum them up manually

## Expected Outcome

After rebuilding and checking the logs, you'll see exactly which stablecoins are in your demo account and their individual balances. This will explain why the total is 10,000.

## If You Want Only USDT

If you want the app to show only USDT balance (not all stablecoins), modify `TradingApp.kt`:

```kotlin
// Change this line:
val stableAssets = setOf("USDT", "USDC", "BUSD", "FDUSD", "TUSD", "DAI")

// To this:
val stableAssets = setOf("USDT")
```

## Next Steps

1. Rebuild the app with the fixes
2. Check the logcat output
3. Verify the balance breakdown
4. If needed, adjust which stablecoins to include in the balance calculation
