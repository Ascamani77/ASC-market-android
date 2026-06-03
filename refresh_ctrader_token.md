# cTrader Access Token Expired - How to Refresh

## Problem
Your cTrader bridge is showing authentication timeout errors:
```
[ERROR] Account auth timed out - verify ACCESS_TOKEN and ACCOUNT_ID
```

This means your **ACCESS_TOKEN has expired** and needs to be refreshed.

## Solution: Get New Access Token

### Step 1: Log into Pepperstone cTrader Portal
1. Go to: https://openapi.ctrader.com/
2. Click "Sign In" (top right)
3. Use your Pepperstone credentials

### Step 2: Navigate to Applications
1. After login, click "Applications" in the menu
2. Find your application (or create a new one if needed)
3. Click on your application name

### Step 3: Get New Access Token
1. Look for "Access Token" section
2. Click "Generate New Token" or "Refresh Token"
3. Copy the new token (it will look like: `bz5h7SrzyS_xb8udgcckRcGJU2D9FgIMCAkegWiRFeE`)

### Step 4: Update Docker Container
Once you have the new token, update the container:

```powershell
# Stop and remove old container
docker stop ctrader-bridge
docker rm ctrader-bridge

# Start with new token
docker run -d --name ctrader-bridge --restart unless-stopped -p 8082:8082 `
  -e CTRADER_CLIENT_ID="27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s" `
  -e CTRADER_CLIENT_SECRET="loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty" `
  -e CTRADER_ACCESS_TOKEN="YOUR_NEW_TOKEN_HERE" `
  -e CTRADER_ACCOUNT_ID="47341092" `
  -e CTRADER_HOST_TYPE="live" `
  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
  -e CTRADER_BRIDGE_PORT="8082" `
  myrealapp-ctrader-bridge:latest
```

Replace `YOUR_NEW_TOKEN_HERE` with the token you copied.

### Step 5: Verify Connection
```powershell
# Wait a few seconds, then check logs
Start-Sleep -Seconds 5
docker logs ctrader-bridge --tail 20
```

You should see:
```
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: cTrader account 47341092 authenticated
[cTrader] symbols_loaded: Loaded 1889 broker symbols
```

## Alternative: Use Refresh Token Flow

If you have a refresh token, you can automate this. But for now, manual refresh is simpler.

## How Often Do Tokens Expire?

- **Access Tokens**: Typically expire after 24-48 hours
- **Refresh Tokens**: Last longer (weeks/months)

For production use, you should implement automatic token refresh using the OAuth2 refresh token flow.

## Quick Test Script

Save this as `test_ctrader_auth.ps1`:

```powershell
$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$ACCESS_TOKEN = "YOUR_TOKEN_HERE"
$ACCOUNT_ID = "47341092"

Write-Host "Testing cTrader credentials..." -ForegroundColor Cyan
Write-Host "CLIENT_ID: $($CLIENT_ID.Substring(0, 20))..." -ForegroundColor Gray
Write-Host "ACCESS_TOKEN: $($ACCESS_TOKEN.Substring(0, 20))..." -ForegroundColor Gray
Write-Host "ACCOUNT_ID: $ACCOUNT_ID" -ForegroundColor Gray
Write-Host ""

# Test by starting a temporary container
docker run --rm -it `
  -e CTRADER_CLIENT_ID="$CLIENT_ID" `
  -e CTRADER_CLIENT_SECRET="$CLIENT_SECRET" `
  -e CTRADER_ACCESS_TOKEN="$ACCESS_TOKEN" `
  -e CTRADER_ACCOUNT_ID="$ACCOUNT_ID" `
  -e CTRADER_HOST_TYPE="live" `
  myrealapp-ctrader-bridge:latest

# Watch for authentication messages
# Press Ctrl+C to stop
```

## Troubleshooting

### Error: "CANT_ROUTE_REQUEST"
- Your CLIENT_ID or CLIENT_SECRET is wrong
- Or you're using demo credentials on live endpoint (or vice versa)

### Error: "BLOCKED_PAYLOAD_TYPE: You are being rate limited"
- You're sending too many requests
- Wait 5 minutes and try again
- Increase heartbeat interval (already set to 120s)

### Error: "Account auth timed out"
- ACCESS_TOKEN is expired ← **This is your current issue**
- Get a new token from Pepperstone portal

### Error: "ACCOUNT_ID doesn't match"
- The ACCESS_TOKEN is for a different account
- Make sure you're using the token for account 47341092

## Important Notes

1. **Never commit tokens to git** - They're sensitive credentials
2. **Tokens expire** - You'll need to refresh them periodically
3. **Use environment variables** - Don't hardcode tokens in code
4. **Consider refresh token flow** - For production, implement automatic refresh

## Links

- Pepperstone cTrader Portal: https://openapi.ctrader.com/
- cTrader API Docs: https://help.ctrader.com/open-api/
- OAuth2 Guide: https://help.ctrader.com/open-api/authentication/

---

**Current Status**: ACCESS_TOKEN expired
**Action Required**: Get new token from Pepperstone portal and restart container
