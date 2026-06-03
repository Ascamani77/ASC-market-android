# Contact Pepperstone Support for Demo Token

## The Problem

The OAuth portal (https://openapi.ctrader.com/) is not accessible, and we cannot register redirect URIs for the OAuth flow. This means we cannot get a demo access token through the normal OAuth2 process.

## The Solution

**Contact Pepperstone support directly** and ask them to provide a demo access token for your application.

---

## Contact Information

### Email Support
**Email**: support@pepperstone.com

**Subject**: Request for cTrader Demo API Access Token

**Message Template**:
```
Hello Pepperstone Support,

I need a demo access token for my cTrader Open API application.

Application Details:
- Client ID: 27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s
- Demo Account ID: 47340965
- Demo Account Number: 5288664

Issue:
The OAuth portal (https://openapi.ctrader.com/) is not accessible, so I cannot generate a token through the normal OAuth2 flow or manage my application's redirect URIs.

Request:
Could you please provide a demo access token for this application and account? Alternatively, if you could add the redirect URI "http://localhost:9999/callback" to my application settings, I can generate the token myself.

Thank you for your assistance.
```

### Live Chat
1. Go to: https://pepperstone.com/
2. Look for the live chat icon (usually bottom right)
3. Start a chat with support
4. Provide the same information as above

### Phone Support
Check https://pepperstone.com/contact-us for phone numbers in your region.

---

## Alternative: Check Existing Redirect URIs

You mentioned you saw "Redirect URLs" settings earlier. If you can access that page:

1. Go to where you saw the redirect URLs
2. Check what URIs are already registered
3. Tell me what you see, and I'll create a script using one of those URIs

Common redirect URIs that might already be registered:
- `http://localhost:8080`
- `http://localhost:3000`
- `http://localhost:5000`
- `http://127.0.0.1:8080`
- `https://localhost:8080`

---

## What to Do After Getting Token

Once Pepperstone provides you with a demo access token:

### Option 1: Use the start script
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_demo_bridge.ps1
```

When prompted, paste the access token.

### Option 2: Save to file first
```powershell
# Create token file
@{
    accessToken = "PASTE_YOUR_TOKEN_HERE"
    generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    accountType = "DEMO"
    accountId = "47340965"
} | ConvertTo-Json | Out-File -FilePath "ctrader_demo_token.json" -Encoding UTF8

# Then start bridge
.\start_demo_bridge.ps1
```

---

## Expected Response Time

- **Live Chat**: Immediate to a few minutes
- **Email**: Usually within 24 hours
- **Phone**: Immediate

---

## What Pepperstone Needs to Provide

Ask them for:
1. **Access Token** (required)
2. **Refresh Token** (optional, but useful)
3. **Token Expiration** (how long it's valid)

---

## Temporary Workaround: Use Live Token

If you have a **live** access token that's still valid, you can temporarily use it to test the bridge setup:

```powershell
.\start_live_bridge.ps1
```

**Warning**: This will connect to your **live** account, not demo. Only use this for testing the bridge connection, not for actual trading.

---

## Why This Happened

The cTrader OAuth portal appears to be having issues:
- The application management page returns 404
- Cannot register or modify redirect URIs
- Cannot generate tokens through the web interface

This is a temporary issue with cTrader's infrastructure, not your setup.

---

## Next Steps

1. ✅ **Contact Pepperstone support** (email or live chat)
2. ✅ Provide your application and account details
3. ✅ Request a demo access token
4. ✅ Once received, run `.\start_demo_bridge.ps1`
5. ✅ Paste the token when prompted
6. ✅ Verify the bridge connects successfully

---

## Your Account Details (For Reference)

**Demo Account**:
- Account ID (ctidTraderAccountId): `47340965`
- Account Number: `5288664`
- Balance: $50,000 (demo)

**Application**:
- Client ID: `27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s`
- Client Secret: `loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty`

**Bridge**:
- Port: `8083`
- Endpoint: `demo.ctraderapi.com`

---

## Questions?

If Pepperstone asks any technical questions, here are the answers:

**Q: What do you need the token for?**
A: I'm building a trading application that connects to cTrader's Open API to receive live market data and execute trades programmatically.

**Q: What scope do you need?**
A: `trading` scope (full access to trading and account data)

**Q: What grant type?**
A: OAuth2 authorization code flow (but the portal is not accessible)

**Q: Is this for live or demo?**
A: Demo account (Account ID: 47340965)

---

**Status**: Waiting for Pepperstone to provide demo access token
**Action**: Contact Pepperstone support using the template above
