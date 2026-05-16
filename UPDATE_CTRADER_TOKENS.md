# Update cTrader Production Tokens

## Step 1: Get Tokens from Playground

1. Go to: https://openapi.ctrader.com/apps
2. Click **"Playground"** button
3. Select scope: **"trading"**
4. Click **"Get token"**
5. Allow access to account 5283699
6. Copy all tokens from the result

## Step 2: Update local.properties

Replace these lines in `MyRealApp/local.properties`:

```properties
CTRADER_CLIENT_ID=YOUR_NEW_CLIENT_ID_FROM_PLAYGROUND
CTRADER_CLIENT_SECRET=YOUR_NEW_CLIENT_SECRET_FROM_PLAYGROUND
CTRADER_ACCESS_TOKEN=YOUR_NEW_ACCESS_TOKEN_FROM_PLAYGROUND
CTRADER_REFRESH_TOKEN=YOUR_NEW_REFRESH_TOKEN_FROM_PLAYGROUND
CTRADER_ACCOUNT_ID=47223753
CTRADER_HOST_TYPE=demo
```

## Step 3: Update start_ctrader_bridge.ps1

Replace these lines in `MyRealApp/start_ctrader_bridge.ps1`:

```powershell
$env:CTRADER_CLIENT_ID = "YOUR_NEW_CLIENT_ID_FROM_PLAYGROUND"
$env:CTRADER_CLIENT_SECRET = "YOUR_NEW_CLIENT_SECRET_FROM_PLAYGROUND"
$env:CTRADER_ACCESS_TOKEN = "YOUR_NEW_ACCESS_TOKEN_FROM_PLAYGROUND"
$env:CTRADER_ACCOUNT_ID = "47223753"
$env:CTRADER_HOST_TYPE = "demo"
```

## Step 4: Restart Everything

1. Stop the bridge (Ctrl+C)
2. Restart: `.\start_ctrader_bridge.ps1`
3. Rebuild your app

## Expected Result

Your Pepperstone balance should show:
- **Balance**: $50,000 USD
- **Leverage**: 200:1
- **Account**: 5283699 (Demo)
- **Status**: Active

## Token Expiry

- Access token expires in ~30 days
- Use refresh token to get new access token
- Refresh token never expires

## Troubleshooting

If balance still doesn't show:
1. Check bridge logs for "Account authenticated"
2. Check app logcat for "Account info received"
3. Verify account ID is 47223753 (not 5283699)
