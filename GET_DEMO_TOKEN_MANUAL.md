# Get Demo Access Token - Manual Guide

## Your Demo Account Details
- **Account ID (ctidTraderAccountId)**: `47340965`
- **Account Number (display)**: `5288664`
- **Balance**: $50,000 (demo)
- **Client ID**: `27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s`
- **Client Secret**: `loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty`

## Method 1: OAuth2 Flow (Recommended)

Since the cTrader OAuth portal (https://openapi.ctrader.com/) is not working, you need to use the **OAuth2 authorization flow** directly.

### Step 1: Configure Redirect URI

First, you need to add a redirect URI to your cTrader application settings. Common options:
- `http://localhost:8080` (if you can run a local server)
- `http://localhost:3000`
- Or any URL you control

**How to add redirect URI:**
1. Go to: https://id.ctrader.com/my/settings/openapi/applications
2. Sign in with your **DEMO** credentials
3. Find your application (Client ID: 27391_...)
4. Add a redirect URI (e.g., `http://localhost:8080`)
5. Save

### Step 2: Run OAuth Script

Once you have a redirect URI configured:

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_ctrader_token_oauth.ps1
```

The script will:
1. Ask for your redirect URI
2. Ask if you want DEMO or LIVE
3. Open a browser for you to authorize
4. Exchange the authorization code for an access token
5. Save the token to `ctrader_demo_token.json`
6. Optionally start the demo bridge

### Step 3: Authorize in Browser

When the browser opens:
1. **Sign in with DEMO credentials** (not live!)
2. Click "Allow access" to authorize the app
3. You'll be redirected to your redirect URI with a code
4. Copy the **full URL** from the browser address bar
5. Paste it back into the PowerShell script

Example redirect URL:
```
http://localhost:8080/?code=ABC123XYZ456...
```

### Step 4: Token Retrieved

The script will extract the code and exchange it for an access token. You'll see:
```
✅ Access token received!
Access Token: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Refresh Token: def50200...
Expires In: 86400 seconds (~1.0 days)
```

The token is saved to `ctrader_demo_token.json`.

---

## Method 2: Direct API Call (If OAuth Portal Works)

If the OAuth portal (https://openapi.ctrader.com/) starts working:

1. Go to: https://openapi.ctrader.com/
2. **Sign in with DEMO credentials**
3. Navigate to "Applications"
4. Select your application (Client ID: 27391_...)
5. Click "Generate Token" or "Refresh Token"
6. Copy the access token
7. Save it somewhere safe

---

## Method 3: Manual OAuth2 Flow (Advanced)

If you're comfortable with manual API calls:

### Step 1: Get Authorization Code

Open this URL in your browser (replace REDIRECT_URI):
```
https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s&redirect_uri=YOUR_REDIRECT_URI&scope=trading&product=web
```

Sign in with **DEMO credentials** and authorize. You'll be redirected to:
```
YOUR_REDIRECT_URI/?code=AUTHORIZATION_CODE
```

Copy the `AUTHORIZATION_CODE`.

### Step 2: Exchange for Access Token

Run this PowerShell command (replace CODE and REDIRECT_URI):
```powershell
$code = "YOUR_AUTHORIZATION_CODE"
$redirectUri = "YOUR_REDIRECT_URI"
$clientId = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$clientSecret = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"

$tokenUrl = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$code&redirect_uri=$([System.Uri]::EscapeDataString($redirectUri))&client_id=$clientId&client_secret=$clientSecret"

$response = Invoke-RestMethod -Uri $tokenUrl -Method GET
$response | ConvertTo-Json
```

You'll get:
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "def50200...",
  "expiresIn": 86400,
  "tokenType": "Bearer"
}
```

Copy the `accessToken`.

---

## After Getting Token

Once you have the access token, start the demo bridge:

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_demo_bridge.ps1
```

When prompted, paste your access token.

The bridge will:
1. Connect to `demo.ctraderapi.com`
2. Authenticate with account `47340965`
3. Load symbols
4. Start serving on port `8083`

---

## Troubleshooting

### "Invalid redirect URI"
- Make sure the redirect URI in the authorization URL matches exactly what's configured in your app settings
- Include the protocol (`http://` or `https://`)
- No trailing slash

### "Invalid authorization code"
- Authorization codes expire quickly (usually 10 minutes)
- Make sure you're using the code immediately after getting it
- Don't reuse codes - get a new one each time

### "Invalid client credentials"
- Double-check CLIENT_ID and CLIENT_SECRET
- Make sure there are no extra spaces or quotes

### "Account not found"
- Make sure you signed in with **DEMO** credentials when authorizing
- The token must match the account type (demo token for demo account)

---

## Quick Start (If You Already Have Token)

If you already have a demo access token:

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_demo_bridge.ps1
```

Paste your token when prompted, and you're done!

---

## Token Lifespan

- Access tokens typically expire after **24 hours**
- When expired, you'll see "Account auth timed out" errors
- Get a new token using any of the methods above
- Or implement refresh token logic (advanced)

---

## Next Steps

1. ✅ Choose a method above to get your demo access token
2. ✅ Run `.\start_demo_bridge.ps1` with the token
3. ✅ Verify authentication in logs: `docker logs ctrader-bridge-demo`
4. ✅ Connect your Android app to `ws://YOUR_PC_IP:8083`
5. ✅ Test with demo data before moving to live

---

**Status**: Waiting for demo access token
**Recommended**: Use Method 1 (OAuth2 Flow) with the provided script
