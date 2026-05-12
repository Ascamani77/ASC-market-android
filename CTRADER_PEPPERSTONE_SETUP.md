# cTrader Pepperstone Integration Setup

## Overview
Configuration for connecting your Android app to cTrader Pepperstone demo/live accounts.

## Configuration Added

### 1. local.properties
Added the following cTrader configuration:

```properties
# cTrader Pepperstone Configuration
CTRADER_HOST_TYPE=demo
CTRADER_CLIENT_ID=YOUR_CLIENT_ID
CTRADER_CLIENT_SECRET=YOUR_CLIENT_SECRET
CTRADER_ACCESS_TOKEN=YOUR_ACCESS_TOKEN
CTRADER_ACCOUNT_ID=YOUR_DEMO_ACCOUNT_ID
CTRADER_BRIDGE_PORT=8082
```

### 2. app/build.gradle.kts
Added BuildConfig fields:

```kotlin
buildConfigField("String", "CTRADER_HOST_TYPE", "...")
buildConfigField("String", "CTRADER_CLIENT_ID", "...")
buildConfigField("String", "CTRADER_CLIENT_SECRET", "...")
buildConfigField("String", "CTRADER_ACCESS_TOKEN", "...")
buildConfigField("String", "CTRADER_ACCOUNT_ID", "...")
buildConfigField("int", "CTRADER_BRIDGE_PORT", "...")
```

## Getting Your cTrader Credentials

### Step 1: Register for cTrader Open API
1. Go to https://openapi.ctrader.com/
2. Sign up or log in with your Pepperstone account
3. Create a new application

### Step 2: Get Your Credentials
After creating an application, you'll receive:
- **Client ID**: Your application's unique identifier
- **Client Secret**: Your application's secret key

### Step 3: Get Access Token
1. Use OAuth2 flow to get an access token
2. Or use the cTrader Open API playground to generate a token
3. For demo accounts, make sure to select "Demo" environment

### Step 4: Get Account ID
1. Log into your cTrader account
2. Your account ID is visible in the account settings
3. For demo accounts, use your demo account ID

### Step 5: Update local.properties
Replace the placeholder values in `local.properties`:

```properties
CTRADER_HOST_TYPE=demo  # or "live" for live trading
CTRADER_CLIENT_ID=your_actual_client_id_here
CTRADER_CLIENT_SECRET=your_actual_client_secret_here
CTRADER_ACCESS_TOKEN=your_actual_access_token_here
CTRADER_ACCOUNT_ID=your_actual_account_id_here
CTRADER_BRIDGE_PORT=8082  # or your preferred port
```

## Using in Your App

Access the configuration in your Kotlin code:

```kotlin
val hostType = BuildConfig.CTRADER_HOST_TYPE
val clientId = BuildConfig.CTRADER_CLIENT_ID
val clientSecret = BuildConfig.CTRADER_CLIENT_SECRET
val accessToken = BuildConfig.CTRADER_ACCESS_TOKEN
val accountId = BuildConfig.CTRADER_ACCOUNT_ID
val bridgePort = BuildConfig.CTRADER_BRIDGE_PORT
```

## cTrader API Endpoints

### Demo Environment
- **API Base URL**: `https://demo.ctraderapi.com`
- **WebSocket**: `wss://demo.ctraderapi.com`

### Live Environment
- **API Base URL**: `https://live.ctraderapi.com`
- **WebSocket**: `wss://live.ctraderapi.com`

## Bridge Server Setup

If you're using a local bridge server (Python/Node.js) to connect to cTrader:

1. The bridge server should run on `localhost:8082` (or your configured port)
2. The bridge handles authentication and WebSocket connections
3. Your Android app connects to the bridge via HTTP/WebSocket

## Security Notes

⚠️ **IMPORTANT SECURITY WARNINGS:**

1. **Never commit credentials to git**
   - `local.properties` is in `.gitignore` by default
   - Keep it that way!

2. **Access Token Security**
   - Access tokens expire and need to be refreshed
   - Implement token refresh logic in your app
   - Store tokens securely using Android Keystore

3. **Demo vs Live**
   - Always test with demo accounts first
   - Use `CTRADER_HOST_TYPE=demo` for testing
   - Only switch to `live` when ready for real trading

4. **Client Secret**
   - Never expose client secret in client-side code
   - Consider using a backend server for authentication
   - The current setup is for development/testing only

## Next Steps

1. **Replace placeholder values** in `local.properties` with your actual credentials
2. **Rebuild the app** to pick up the new configuration
3. **Create a cTrader service** similar to `Mt5Service` or `DerivService`
4. **Implement WebSocket connection** to cTrader API
5. **Handle authentication** and token refresh
6. **Subscribe to market data** and account updates

## Resources

- [cTrader Open API Documentation](https://help.ctrader.com/open-api/)
- [cTrader API Reference](https://openapi.ctrader.com/docs)
- [Pepperstone cTrader](https://pepperstone.com/en/trading-platforms/ctrader)
- [OAuth2 Flow Guide](https://help.ctrader.com/open-api/authentication/)

## Example Service Implementation

You'll need to create a service similar to your existing MT5 and Deriv services:

```kotlin
class CTraderService(
    private val onQuoteUpdate: (SymbolQuote) -> Unit,
    private val onPositionsUpdate: (List<Position>) -> Unit,
    private val onAccountUpdate: (AccountInfo) -> Unit
) {
    private val hostType = BuildConfig.CTRADER_HOST_TYPE
    private val clientId = BuildConfig.CTRADER_CLIENT_ID
    private val accessToken = BuildConfig.CTRADER_ACCESS_TOKEN
    private val accountId = BuildConfig.CTRADER_ACCOUNT_ID
    
    fun connect() {
        // Implement WebSocket connection to cTrader API
    }
    
    fun subscribe(symbol: String) {
        // Subscribe to symbol quotes
    }
    
    fun disconnect() {
        // Clean up connections
    }
}
```

## Troubleshooting

### "Invalid credentials" error
- Verify your Client ID and Client Secret are correct
- Check that your Access Token hasn't expired
- Ensure you're using the correct environment (demo/live)

### "Account not found" error
- Verify your Account ID is correct
- Make sure the account belongs to the authenticated user
- Check that you're using the right environment

### Connection timeout
- Check your internet connection
- Verify the bridge server is running (if using one)
- Check firewall settings

### Token expired
- Implement token refresh logic
- Request a new access token from cTrader API
- Store the new token securely
