# cTrader Production Tokens Guide

## Quick Start: Use Sandbox/Playground (Recommended for Testing)

This is the **easiest way** to get tokens for your own account:

### Steps:

1. Go to https://openapi.ctrader.com/apps
2. Find your **"Asc market"** application
3. Click the **"Sandbox"** button (in the Actions column)
4. Select scope: **"trading"** (for full access) or **"accounts"** (read-only)
5. Click **"Get token"**
6. Copy the displayed tokens:
   - `accessToken`
   - `refreshToken`

### Update local.properties:

```properties
# Change from demo to live
CTRADER_HOST_TYPE=live

# Use your production app credentials
CTRADER_CLIENT_ID=<from Credentials button>
CTRADER_CLIENT_SECRET=<from Credentials button>

# Use tokens from Sandbox
CTRADER_ACCESS_TOKEN=<from Sandbox>
CTRADER_REFRESH_TOKEN=<from Sandbox>

# Account ID (optional - will be auto-discovered)
# CTRADER_ACCOUNT_ID=
```

---

## Full OAuth Flow (For Real Users)

If you need to allow **other users** to connect their accounts to your app, use the OAuth flow:

### Prerequisites:

1. Get your production credentials:
   - Go to https://openapi.ctrader.com/apps
   - Click **"Credentials"** on your "Asc market" app
   - Copy `Client ID` and `Client Secret`

2. Add redirect URI:
   - Click **"Edit"** on your app
   - Scroll to "Redirect URIs"
   - Add: `http://localhost:8080/callback` (for testing)
   - Click **"Save"**

### Using the Python Script:

1. **Install dependencies:**
   ```bash
   pip install requests
   ```

2. **Edit the script:**
   ```bash
   # Open: get_ctrader_production_tokens.py
   # Update these lines:
   CLIENT_ID = "YOUR_PRODUCTION_CLIENT_ID"
   CLIENT_SECRET = "YOUR_PRODUCTION_CLIENT_SECRET"
   ```

3. **Run the script:**
   ```bash
   python get_ctrader_production_tokens.py
   ```

4. **Follow the prompts:**
   - Browser will open automatically
   - Login with your cTID
   - Grant permissions
   - Tokens will be displayed and saved to `ctrader_production_tokens.json`

---

## Manual OAuth Flow (Without Script)

### Step 1: Get Authorization Code

Open this URL in your browser (replace `{CLIENT_ID}` and `{REDIRECT_URI}`):

```
https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id={CLIENT_ID}&redirect_uri={REDIRECT_URI}&scope=trading&product=web
```

Example:
```
https://id.ctrader.com/my/settings/openapi/grantingaccess/?client_id=27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s&redirect_uri=http://localhost:8080/callback&scope=trading&product=web
```

After login and granting permissions, you'll be redirected to:
```
http://localhost:8080/callback?code=AUTHORIZATION_CODE_HERE
```

Copy the `code` parameter value.

### Step 2: Exchange Code for Tokens

Use curl or any HTTP client:

```bash
curl -X GET 'https://openapi.ctrader.com/apps/token?grant_type=authorization_code&code=YOUR_CODE_HERE&redirect_uri=http://localhost:8080/callback&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json'
```

Response:
```json
{
  "accessToken": "mos8Bw3D4EG0fRPd4Eqq0JxaFT4zjd8e4YijNezh_ag",
  "tokenType": "bearer",
  "expiresIn": 2628000,
  "refreshToken": "VCuafFhy81AFZjsWkbuEzdOhhRj5YTWz8fWUwHam7KM",
  "errorCode": null,
  "description": null
}
```

---

## Refreshing Expired Tokens

Access tokens expire after ~30 days. Use the refresh token to get new ones:

### Using HTTP Request:

```bash
curl -X POST 'https://openapi.ctrader.com/apps/token?grant_type=refresh_token&refresh_token=YOUR_REFRESH_TOKEN&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json'
```

### Using Protobuf (in your bridge):

Send `ProtoOARefreshTokenReq` message with your refresh token.

---

## Important Notes

1. **Authorization code expires in 1 minute** - Exchange it quickly!
2. **Access token expires in ~30 days** - Use refresh token to renew
3. **Refresh token never expires** - Keep it secure!
4. **Sandbox tokens** are for your account only - Use OAuth for other users
5. **Keep tokens secure** - Never commit them to version control

---

## Troubleshooting

### "No playground" issue
- Look for **"Sandbox"** button instead of "Playground"
- It's in the Actions column next to Credentials and Edit

### "Access blocked" with Google login
- Set user-agent to: `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36`

### "Invalid redirect_uri"
- Make sure the redirect URI is added in your app settings
- URI must match exactly (including http/https and trailing slashes)

### "Invalid client credentials"
- Double-check CLIENT_ID and CLIENT_SECRET
- Make sure there are no extra spaces or quotes
