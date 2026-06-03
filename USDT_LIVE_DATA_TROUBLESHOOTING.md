# USDT Live Data Troubleshooting

## Issue
USDT assets (BTCUSDT, ETHUSDT) are not showing live data in the app, while Pepperstone assets (EURUSD, XAUUSD, etc.) are working fine.

## Root Cause
USDT pairs come from **Binance WebSocket**, which is separate from the Pepperstone cTrader bridge.

## Data Sources
- **Pepperstone cTrader** (port 8082): EURUSD, GBPUSD, XAUUSD, USOIL, UKOIL, etc. ✅ WORKING
- **Binance WebSocket**: BTCUSDT, ETHUSDT ❌ NOT WORKING

## Code Analysis

### 1. USDT Pairs Are Defined
File: `app/src/main/java/com/asc/markets/data/Constants.kt`
```kotlin
ForexPair("BTC/USDT", "Bitcoin / Tether", ...),
ForexPair("ETH/USDT", "Ethereum / Tether", ...),
```
✅ USDT pairs exist in FOREX_PAIRS

### 2. BinanceWebSocketManager Exists
File: `app/src/main/java/com/asc/markets/network/BinanceWebSocketManager.kt`
```kotlin
class BinanceWebSocketManager(
    private val scope: CoroutineScope,
    private val redisHost: String = "10.0.2.2",
    ...
) {
    fun connect(initialSymbols: List<String>) {
        // Connects to wss://stream.binance.com:9443
    }
}
```
✅ Manager exists and has connect method

### 3. Connection Is Called in Init Block
File: `app/src/main/java/com/asc/markets/logic/ForexViewModel.kt` (line 794)
```kotlin
init {
    val usdtSymbols = FOREX_PAIRS
        .filter { it.symbol.endsWith("/USDT") }
        .map { it.symbol.replace("/", "") }  // ["BTCUSDT", "ETHUSDT"]
        .distinct()
    
    if (usdtSymbols.isNotEmpty()) {
        binanceWsManager.connect(usdtSymbols)  // ✅ This SHOULD be called
    }
}
```
✅ Code is correct and should connect automatically

## Possible Issues

### Issue 1: Binance Region Block
Binance may be blocked in your region. Check app logs for:
```
BinanceWS: Region block detected
BinanceWS: Binance WebSocket failure: 451
```

**Solution**: Use a VPN or proxy

### Issue 2: Network/Firewall
The app can't reach `wss://stream.binance.com:9443`

**Solution**: Check firewall settings, try on different network

### Issue 3: WebSocket Not Connecting
Connection is failing silently

**Solution**: Check logcat for "BinanceWS" messages

### Issue 4: Redis Configuration
BinanceWebSocketManager is trying to publish to Redis but failing

**Solution**: Check Redis host/port in app settings

## Debugging Steps

### Step 1: Check Android Logcat
```bash
adb logcat | findstr "BinanceWS"
```

Look for:
- ✅ "Connected to Binance streams for symbols=btcusdt,ethusdt"
- ❌ "Binance WebSocket failure"
- ❌ "Region block detected"

### Step 2: Check If ViewModel Is Initialized
The `ForexViewModel` init block should run when the app starts. Check logcat for:
```
adb logcat | findstr "ForexViewModel"
```

### Step 3: Test Binance Directly
Open this URL in your browser:
```
https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT
```

If it works, Binance API is accessible.
If it fails with 451 or 403, you're region-blocked.

### Step 4: Check App Settings
In your app, check if Redis settings are correct:
- Redis Host: Should match your PC's IP (not 10.0.2.2 if on physical device)
- Redis Port: 6379
- Backend URL: Should be your PC's IP

### Step 5: Manual Test
Add this to your app's MainActivity or a test screen:
```kotlin
val binanceWs = BinanceWebSocketManager(
    scope = lifecycleScope,
    redisHost = "YOUR_PC_IP",  // e.g., "192.168.1.100"
    redisPort = 6379
)
binanceWs.connect(listOf("btcusdt", "ethusdt"))

lifecycleScope.launch {
    binanceWs.priceUpdates.collect { pair ->
        Log.i("BinanceTest", "Received: ${pair.symbol} ${pair.price}")
    }
}
```

## Quick Fixes

### Fix 1: Force Reconnect
Add this button to your app:
```kotlin
Button(onClick = {
    viewModel.reconnectBinance()  // You'll need to add this method
}) {
    Text("Reconnect Binance")
}
```

### Fix 2: Use Fallback Data Source
If Binance is blocked, you can:
1. Use Deriv for BTCUSD/ETHUSD instead
2. Use a different crypto exchange API
3. Use a proxy/VPN

### Fix 3: Check If App Is Using Emulator
If running on emulator, Redis host should be `10.0.2.2`
If running on physical device, Redis host should be your PC's LAN IP (e.g., `192.168.1.100`)

## Expected Behavior

When working correctly, you should see in logcat:
```
BinanceWS: Connecting to Binance streams for symbols=btcusdt,ethusdt
BinanceWS: Connected to Binance streams for symbols=btcusdt,ethusdt
BinanceWS: Emitting Binance tick BTC/USDT 67432.50
BinanceWS: Routing Binance update to BinanceDataStore BTC/USDT 67432.50
```

And in your app:
- BTC/USDT price updating every second
- ETH/USDT price updating every second
- Change % updating in real-time

## Next Steps

1. **Check logcat** for "BinanceWS" messages
2. **Test Binance API** in browser
3. **Verify Redis settings** in app
4. **Try on different network** (mobile data vs WiFi)
5. **Use VPN** if region-blocked

## Contact Points

- Binance WebSocket URL: `wss://stream.binance.com:9443`
- Binance REST API: `https://api.binance.com`
- Redis: `localhost:6379` (or your PC's IP)
- cTrader Bridge: `localhost:8082` (or your PC's IP)

---

**Status**: Pepperstone ✅ | Binance ❌
**Action Required**: Check Android logcat for Binance connection errors
