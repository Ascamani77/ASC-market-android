# Deriv WebSocket Setup Guide

## Quick Start (No Authentication Required)

For **public market data** (real-time prices for BTC/USD, ETH/USD, commodities), no authentication is required! The integration works out of the box using Deriv's public WebSocket endpoint.

Just use the `DerivService` or `MarketDataSourceManager` and you're good to go.

## Optional: Add Your Deriv API Token

If you want to use **authenticated features** (trading, account management), follow these steps:

### Step 1: Get Your Deriv API Token

1. Go to [Deriv API Token Page](https://app.deriv.com/account/api-token)
2. Log in to your Deriv account
3. Create a new API token with the required scopes:
   - **Read**: For market data
   - **Trade**: For placing orders (if needed)
   - **Payments**: For account operations (if needed)
4. Copy the generated token

### Step 2: Add Token to local.properties

Open `MyRealApp/local.properties` and add:

```properties
# Deriv API Configuration
DERIV_APP_ID=1089
DERIV_API_TOKEN=your_token_here
```

Replace `your_token_here` with your actual token.

**Note**: The `local.properties` file is already in `.gitignore`, so your token won't be committed to version control.

### Step 3: Rebuild the Project

After adding the token, rebuild the project:

```bash
./gradlew clean build
```

Or in Android Studio: **Build → Rebuild Project**

## Using the Token in Code

The token is available via `BuildConfig`:

```kotlin
val token = BuildConfig.DERIV_API_TOKEN
val appId = BuildConfig.DERIV_APP_ID
```

### Example: Authenticated WebSocket Connection

```kotlin
import okhttp3.*
import org.json.JSONObject

suspend fun connectAuthenticatedWebSocket(accountId: String) {
    // Step 1: Get OTP URL
    val otpUrl = getDerivOTP(accountId, BuildConfig.DERIV_API_TOKEN)
    
    // Step 2: Connect to authenticated WebSocket
    val client = OkHttpClient()
    val request = Request.Builder().url(otpUrl).build()
    
    val webSocket = client.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("Authenticated WebSocket connected!")
            
            // Now you can send trading commands
            val buyMessage = JSONObject().apply {
                put("buy", 1)
                put("price", 100)
                put("parameters", JSONObject().apply {
                    put("contract_type", "CALL")
                    put("duration", 5)
                    put("duration_unit", "m")
                    put("symbol", "frxBTCUSD")
                })
            }
            webSocket.send(buyMessage.toString())
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            println("Received: $text")
        }
    })
}

suspend fun getDerivOTP(accountId: String, token: String): String {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.derivws.com/trading/v1/options/accounts/$accountId/otp")
        .post(RequestBody.create(null, ""))
        .addHeader("Deriv-App-ID", BuildConfig.DERIV_APP_ID)
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    val response = client.newCall(request).execute()
    val json = JSONObject(response.body?.string() ?: "")
    return json.getJSONObject("data").getString("url")
}
```

## Verification

To verify your setup:

1. **Check BuildConfig**: Ensure the token is accessible
   ```kotlin
   Log.d("Deriv", "Token: ${BuildConfig.DERIV_API_TOKEN}")
   Log.d("Deriv", "App ID: ${BuildConfig.DERIV_APP_ID}")
   ```

2. **Test Connection**: Run the example screens
   - `DerivIntegrationExample` - Shows live prices
   - `SmartRoutingExample` - Shows data source routing

3. **Check Logs**: Look for "DerivService" in Logcat
   ```
   DerivService: Connecting to Deriv WebSocket...
   DerivService: Deriv WebSocket Connected
   DerivService: Subscribed to frxBTCUSD (app: BTCUSD)
   DerivService: Tick: BTCUSD = 43250.50
   ```

## Troubleshooting

### Token Not Found
**Error**: `BuildConfig.DERIV_API_TOKEN` is empty

**Solution**: 
1. Ensure you added the token to `local.properties`
2. Rebuild the project (Build → Rebuild Project)
3. Sync Gradle files

### Invalid Token
**Error**: "Unauthorized - Invalid or missing authentication credentials"

**Solution**:
1. Verify the token is correct (copy-paste from Deriv)
2. Check token hasn't expired
3. Ensure token has the required scopes

### Connection Failed
**Error**: WebSocket connection fails

**Solution**:
1. Check internet connection
2. Verify firewall/proxy settings
3. Try the public endpoint first (no auth required)

## Security Best Practices

1. **Never commit tokens**: The `local.properties` file is in `.gitignore`
2. **Use environment variables**: For CI/CD, use environment variables
3. **Rotate tokens regularly**: Generate new tokens periodically
4. **Limit token scopes**: Only grant necessary permissions
5. **Monitor token usage**: Check Deriv dashboard for suspicious activity

## Public vs Authenticated Endpoints

| Feature | Public Endpoint | Authenticated Endpoint |
|---------|----------------|------------------------|
| Real-time prices | ✅ Yes | ✅ Yes |
| Historical data | ✅ Yes | ✅ Yes |
| Place trades | ❌ No | ✅ Yes |
| Account info | ❌ No | ✅ Yes |
| Token required | ❌ No | ✅ Yes |
| Rate limits | Lower | Higher |

## Next Steps

1. ✅ **Test Public Endpoint**: Run `DerivIntegrationExample` to see live prices
2. ✅ **Add Token** (optional): Follow steps above for authenticated features
3. ✅ **Integrate in App**: Use `MarketDataSourceManager` in your charts
4. ✅ **Monitor Performance**: Check SystemTelemetry for latency metrics

## Resources

- [Deriv API Documentation](https://api.deriv.com)
- [WebSocket API Explorer](https://api.deriv.com/api-explorer)
- [Deriv Developer Community](https://community.deriv.com)
- [API Token Management](https://app.deriv.com/account/api-token)

## Support

If you encounter issues:
1. Check the [Deriv API Status](https://deriv.statuspage.io/)
2. Review [API Documentation](https://api.deriv.com)
3. Ask in [Deriv Community](https://community.deriv.com)
