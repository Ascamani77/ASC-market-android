# Alternative Methods to Get Demo Access Token

Since the cTrader OAuth portal (https://openapi.ctrader.com/) is not accessible, here are alternative methods to get your demo access token.

## Method 1: Direct OAuth Flow (Recommended)

This method uses a local HTTP server to automatically receive the OAuth callback.

**No portal access needed!**

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\get_demo_token_direct.ps1
```

The script will:
1. Start a local HTTP server on port 8080
2. Open your browser for authorization
3. Automatically receive the callback
4. Exchange the code for an access token
5. Save the token and optionally start the bridge

**Requirements:**
- Port 8080 must be available
- You must sign in with **DEMO credentials** in the browser

---

## Method 2: Contact Pepperstone Support

If the OAuth flow doesn't work, you can contact Pepperstone support to get a demo access token directly.

**Contact:**
- Email: support@pepperstone.com
- Live Chat: https://pepperstone.com/
- Phone: Check their website for your region

**What to say:**
> "I need a demo access token for my cTrader Open API application. My Client ID is 27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s and my demo account ID is 47340965. The OAuth portal is not accessible."

They should be able to generate a token for you.

---

## Method 3: Use cTrader Desktop App

If you have the cTrader desktop application installed:

1. Open cTrader desktop
2. Sign in with your **DEMO** account
3. Go to: **Settings** → **API** → **Applications**
4. Find your application (Client ID: 27391_...)
5. Click "Generate Token"
6. Copy the access token

**Note:** This method may not be available in all cTrader versions.

---

## Method 4: Manual Browser Method

If you're comfortable with manual steps:

### Step 1: Get Authorization Code

Open this URL in your browser:
```
https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s&redirect_uri=http://localhost:8080/callback&scope=trading&product=web
```

**Important:** Sign in with **DEMO credentials**!

After authorizing, you'll be redirected to:
```
http://localhost:8080/callback?code=AUTHORIZATION_CODE
```

The page won't load (that's OK). Copy the **AUTHORIZATION_CODE** from the URL.

### Step 2: Exchange for Token

Run this PowerShell command (replace `YOUR_CODE`):

```powershell
$code = "YOUR_CODE"
$clientId = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$clientSecret = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$redirectUri = "http://localhost:8080/callback"

$tokenUrl = "https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=$code&redirect_uri=$([System.Uri]::EscapeDataString($redirectUri))&client_id=$clientId&client_secret=$clientSecret"

$response = Invoke-RestMethod -Uri $tokenUrl -Method GET
$response | ConvertTo-Json

# Save the accessToken
$response.accessToken
```

Copy the `accessToken` from the output.

### Step 3: Start Bridge

```powershell
.\start_demo_bridge.ps1
```

Paste the access token when prompted.

---

## Method 5: Use Existing Live Token (Temporary)

If you have a **live** access token that's still valid, you can temporarily use it to test the bridge setup (but it will connect to your live account, not demo).

**Only do this if:**
- You understand it will use your live account
- You just want to test the bridge connection
- You'll get a proper demo token later

```powershell
.\start_live_bridge.ps1
```

Use your live token when prompted.

---

## Method 6: Check Old Token Files

If you've generated tokens before, check for old token files:

```powershell
# Check current directory
Get-ChildItem -Filter "*token*.json"

# Check common locations
Get-ChildItem -Path "$env:USERPROFILE\Downloads" -Filter "*token*.json"
Get-ChildItem -Path "$env:USERPROFILE\Documents" -Filter "*token*.json"
```

If you find an old demo token file, check if it's still valid:

```powershell
$tokenData = Get-Content "path\to\token.json" | ConvertFrom-Json
$tokenData.accessToken
```

Try using it with `.\start_demo_bridge.ps1`.

---

## Troubleshooting

### "Authorization code expired"
- Authorization codes expire in ~10 minutes
- Complete the process quickly
- Get a new code if it expires

### "Invalid client credentials"
- Double-check CLIENT_ID and CLIENT_SECRET
- Make sure there are no extra spaces

### "Account not found"
- Make sure you signed in with **DEMO** credentials
- Not live credentials!

### "Port 8080 in use"
- Close any apps using port 8080
- Or use a different port (edit the script)

### "Can't connect to cTrader API"
- Check your internet connection
- Try again later (API may be down)
- Contact Pepperstone support

---

## Recommended Approach

**Try in this order:**

1. ✅ **Method 1** (Direct OAuth Flow) - `.\get_demo_token_direct.ps1`
2. ✅ **Method 4** (Manual Browser Method) - Copy code from URL
3. ✅ **Method 2** (Contact Support) - Email Pepperstone
4. ✅ **Method 6** (Check Old Tokens) - Look for existing tokens

---

## After Getting Token

Once you have the access token:

```powershell
# Save it to a file (optional)
@{
    accessToken = "YOUR_TOKEN_HERE"
    generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    accountType = "DEMO"
    accountId = "47340965"
} | ConvertTo-Json | Out-File -FilePath "ctrader_demo_token.json" -Encoding UTF8

# Start the bridge
.\start_demo_bridge.ps1
```

When prompted, paste your access token.

---

## Need Help?

If none of these methods work:
1. Check if cTrader API is down: https://status.ctrader.com/
2. Contact Pepperstone support
3. Try again later (portal may be temporarily unavailable)

---

**Your Demo Account:**
- Account ID: `47340965`
- Account Number: `5288664`
- Balance: $50,000 (demo)
- Bridge Port: `8083`
