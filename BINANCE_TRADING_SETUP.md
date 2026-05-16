# Binance Trading Integration Setup

## Overview
Your app now has Binance trading capability. You can place real trades (market, limit, stop-limit orders) directly from the app.

## Setup Steps

### 1. Add Your Binance API Keys

Edit `MyRealApp/local.properties` and replace the placeholders with your real keys:

```properties
# Binance API Configuration
BINANCE_API_KEY=your_actual_api_key_here
BINANCE_SECRET_KEY=your_actual_secret_key_here
```

### 2. Create Binance API Keys

1. Log in to [Binance](https://www.binance.com)
2. Go to **Profile** → **API Management**
3. Click **Create API**
4. Give it a name (e.g., "MyRealApp")
5. Complete 2FA verification
6. **IMPORTANT: Edit API Permissions:**
   - ✅ **Enable Reading** (required)
   - ✅ **Enable Spot & Margin Trading** (required for trading)
   - ❌ **Enable Withdrawals** (recommended to keep OFF for security)
7. Copy the **API Key** and **Secret Key** to `local.properties`

### 3. IP Restriction (Recommended)

For security, restrict API access to your IP:
1. In API Management, click **Edit restrictions**
2. Select **Restrict access to trusted IPs only**
3. Add your IP address

### 4. Rebuild the App

```bash
./gradlew clean
./gradlew assembleDebug
```

## Usage

The `BinanceTradingService` provides:

### Check Configuration
```kotlin
val service = BinanceTradingService()
if (service.isConfigured()) {
    // Ready to trade
}
```

### Get Account Info
```kotlin
val account = service.getAccountInfo()
println("Can trade: ${account.canTrade}")
println("USDT Balance: ${account.balances.find { it.asset == "USDT" }?.free}")
```

### Place Market Order
```kotlin
// Buy $100 worth of BTC
val order = service.placeMarketOrder(
    symbol = "BTCUSDT",
    side = "BUY",
    quoteOrderQty = 100.0
)
println("Order ID: ${order.orderId}")

// Sell 0.01 BTC
val sellOrder = service.placeMarketOrder(
    symbol = "BTCUSDT", 
    side = "SELL",
    quantity = 0.01
)
```

### Place Limit Order
```kotlin
val order = service.placeLimitOrder(
    symbol = "BTCUSDT",
    side = "BUY",
    quantity = 0.01,
    price = 50000.0
)
```

### Place Stop-Limit Order
```kotlin
// Sell 0.01 BTC if price drops to $49,000
val order = service.placeStopLimitOrder(
    symbol = "BTCUSDT",
    side = "SELL",
    quantity = 0.01,
    price = 48500.0,  // Limit price
    stopPrice = 49000.0  // Trigger price
)
```

### Cancel Order
```kotlin
val success = service.cancelOrder("BTCUSDT", orderId)
```

### Get Open Orders
```kotlin
val orders = service.getOpenOrders("BTCUSDT")
```

### Get Current Price
```kotlin
val price = service.getPrice("BTCUSDT")
```

## Security Notes

1. **NEVER commit `local.properties` to git** - it contains your API keys
2. **Disable withdrawals** on your API key for security
3. **Use IP restrictions** if possible
4. **Start with small amounts** to test
5. **Testnet**: Set `useTestnet = true` in `BinanceTradingService` for testing without real money

## Testnet (Recommended for Testing)

Binance provides a testnet for paper trading:

1. Go to [Binance Spot Testnet](https://testnet.binance.vision/)
2. Log in with GitHub
3. Generate API keys
4. Use these keys for testing

To use testnet in the app:
```kotlin
// In BinanceTradingService.kt, change:
private val useTestnet: Boolean = true
```

## Files Created

- `MyRealApp/app/src/main/kotlin/com/trading/app/data/BinanceTradingService.kt` - Trading service

## Next Steps

1. Add your API keys to `local.properties`
2. Rebuild the app
3. Test with `service.testConnection()` first
4. Start with small trades or use testnet

## Troubleshooting

### "Binance API key not configured"
- Make sure you added keys to `local.properties`
- Rebuild the app (Build → Clean Project, then Build → Rebuild Project)

### "Binance API error: 401"
- API key is invalid or has been revoked
- Check if IP restriction is blocking your requests

### "Binance API error: 1021"
- Timestamp issue - your device time might be wrong
- Sync your device time

### "Insufficient balance"
- Check your available balance: `service.getAccountInfo()`
- Make sure you have enough of the base/quote asset

## Example Integration

To integrate into your TradingApp, you can add a trading panel:

```kotlin
// Example usage in a Composable
val binanceService = remember { BinanceTradingService() }
var accountInfo by remember { mutableStateOf<BinanceTradingService.AccountInfo?>(null) }

LaunchedEffect(Unit) {
    if (binanceService.isConfigured()) {
        accountInfo = binanceService.getAccountInfo()
    }
}

// Show balance
accountInfo?.balances?.filter { it.free > 0 }?.forEach { balance ->
    Text("${balance.asset}: ${balance.free}")
}
```
