# Generate Production cTrader Access Token

## Quick Start

Since you already have **Client ID** and **Client Secret**, you just need to generate a production access token using the OAuth flow.

---

## Option 1: PowerShell Script (Recommended)

### Step 1: Run the token generator
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\generate_ctrader_token.ps1
```

### Step 2: Follow the prompts
1. **Browser will open** - Login with your Pepperstone credentials
2. **Select your account** - Choose demo account (5287516)
3. **Click "Allow access"**
4. **Browser redirects** to `http://localhost:8888/callback?code=ABC123...`
5. **Copy the code** - Everything after `code=` in the URL
6. **Paste it** into the PowerShell prompt

### Step 3: Done!
The script will:
- Exchange the code for an access token
- Save tokens to `ctrader_tokens.json`
- Set environment variables automatically

---

## Option 2: Python Script

### Step 1: Install requests library
```powershell
pip install requests
```

### Step 2: Run the token generator
```powershell
python generate_ctrader_token.py
```

### Step 3: Follow the same process
The Python script will:
- Start a local web server on port 8888
- Open your browser automatically
- Capture the authorization code automatically
- Exchange it for tokens
- Save everything

---

## Option 3: Manual Process

If scripts don't work, you can do it manually:

### Step 1: Build the authorization URL

Replace `YOUR_CLIENT_ID` with your actual Client ID:

```
https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=YOUR_CLIENT_ID&redirect_uri=http://localhost:8888/callback&scope=trading&product=web
```

### Step 2: Open in browser
1. Paste the URL in your browser
2. Login with Pepperstone credentials
3. Select demo account (5287516)
4. Click "Allow access"

### Step 3: Get the authorization code
The browser will redirect to:
```
http://localhost:8888/callback?code=ABC123XYZ_YOUR_CODE_HERE
```

Copy everything after `code=`

### Step 4: Exchange for access token

Run this in PowerShell (replace placeholders):

```powershell
$CLIENT_ID = $env:CTRADER_CLIENT_ID
$CLIENT_SECRET = $env:CTRADER_CLIENT_SECRET
$CODE = "PASTE_YOUR_CODE_HERE"
$REDIRECT_URI = "http://localhost:8888/callback"

$url = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$CODE&redirect_uri=$REDIRECT_URI&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"

$response = Invoke-RestMethod -Uri $url -Method Get -Headers @{
    "Accept" = "application/json"
    "Content-Type" = "application/json"
}

Write-Host "Access Token: $($response.accessToken)"
Write-Host "Refresh Token: $($response.refreshToken)"

# Set environment variables
[System.Environment]::SetEnvironmentVariable('CTRADER_ACCESS_TOKEN', $response.accessToken, 'User')
[System.Environment]::SetEnvironmentVariable('CTRADER_REFRESH_TOKEN', $response.refreshToken, 'User')

Write-Host "`nTokens saved! Close and reopen PowerShell."
```

---

## Verify Setup

After generating tokens, verify everything is set:

```powershell
.\verify_ctrader_credentials.ps1
```

You should see:
```
Checking CTRADER_CLIENT_ID... [OK] (56 chars)
Checking CTRADER_CLIENT_SECRET... [OK] (50 chars)
Checking CTRADER_ACCESS_TOKEN... [OK] (43 chars)
Checking CTRADER_ACCOUNT_ID... [OK]
Checking CTRADER_HOST_TYPE... [OK]

[OK] All credentials are properly configured!
```

---

## Start the Bridge

```powershell
.\start_ctrader_bridge.ps1
```

Expected output:
```
[cTrader] connected: Connected to cTrader Open API demo endpoint
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: Account 5287516 authenticated
[cTrader] symbols_loaded: Loaded 500+ symbols
✓ Bridge ready - accepting WebSocket connections
```

---

## Troubleshooting

### Error: "Browser can't reach localhost:8888"
**This is normal!** The page won't load, but the URL contains the code you need. Just copy the code from the URL bar.

### Error: "Invalid authorization code"
The code expires in 1 minute. If you're too slow, start over and paste it faster.

### Error: "Invalid redirect_uri"
Make sure you added `http://localhost:8888/callback` to your application's redirect URIs in the cTrader portal:
1. Go to https://openapi.ctrader.com/
2. Edit your application
3. Add redirect URI: `http://localhost:8888/callback`
4. Save

### Error: "CANT_ROUTE_REQUEST"
Your Client ID or Secret is wrong. Double-check them in the portal.

---

## Token Expiration

- **Access Token**: Expires in ~30 days
- **Refresh Token**: Never expires

When the access token expires, you can:
1. Use the refresh token to get a new access token (automatic in bridge)
2. Or run this script again to generate new tokens

---

## Security Notes

- ✅ Tokens are stored in environment variables (not in code)
- ✅ Tokens are also saved to `ctrader_tokens.json` (backup)
- ⚠️ Never commit `ctrader_tokens.json` to Git
- ⚠️ Keep your tokens secret

---

## Quick Reference

| What | Where |
|------|-------|
| Generate tokens | `.\generate_ctrader_token.ps1` |
| Verify credentials | `.\verify_ctrader_credentials.ps1` |
| Start bridge | `.\start_ctrader_bridge.ps1` |
| Token backup | `ctrader_tokens.json` |
| cTrader portal | https://openapi.ctrader.com/ |

