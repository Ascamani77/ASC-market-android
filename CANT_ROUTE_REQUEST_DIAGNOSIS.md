# CANT_ROUTE_REQUEST Error Diagnosis

## Current Status

**Error**: `CANT_ROUTE_REQUEST: Cannot route request`

**What's Working**:
- ✅ Application authentication succeeds on live endpoint
- ✅ OAuth token generation works
- ✅ Client ID and Secret are valid

**What's Failing**:
- ❌ Account authentication fails with CANT_ROUTE_REQUEST
- ❌ Cannot connect to account 47340965

## Error Analysis

The `CANT_ROUTE_REQUEST` error occurs **after** successful application authentication but **during** account authentication. This indicates:

1. **Your application credentials are valid** (Client ID/Secret work)
2. **Your access token is valid** (OAuth succeeded)
3. **The routing to account 47340965 fails**

## Possible Causes

### 1. Account/Token Mismatch
The access token was authorized for account **5288664** (visible ID), but the API expects **47340965** (internal ID). However, there may be a mismatch in which endpoint the account exists on.

### 2. Endpoint Mismatch
- Demo accounts can exist on either `demo.ctraderapi.com` OR `live.ctraderapi.com`
- Your account might be on the demo endpoint, but you're connecting to live
- Or vice versa

### 3. Token Authorization Scope
The access token might not have been authorized for the specific account you're trying to access.

### 4. Application Configuration Issue
The application in the cTrader portal might have:
- Incomplete setup
- Missing permissions
- Incorrect redirect URI configuration
- Not fully activated/approved

## Diagnostic Steps

### Step 1: Verify Account/Token Relationship

Run the verification script:
```powershell
.\verify_account_token.ps1
```

This will:
- Query both demo and live endpoints
- Show which accounts are accessible with your token
- Confirm the correct endpoint to use

### Step 2: Check Token Authorization

The token was generated for account **5288664**. Verify:
1. When you authorized the token, which account did you select?
2. Does that account still exist and is active?
3. Is the account on demo or live endpoint?

### Step 3: Verify Application Settings

In the cTrader portal (https://id.ctrader.com/my/settings/openapi):
1. Check application status (should be "Active" or "Approved")
2. Verify redirect URI: `http://localhost:8888/callback`
3. Check permissions/scopes granted
4. Ensure the application is not in "Pending" status

### Step 4: Test with Fresh Token

If the account exists but routing fails, try:
1. Generate a new token using `.\get_token_curl.ps1`
2. When authorizing, carefully select account **5288664**
3. Immediately use the new token (they expire in 60 seconds)

## Known Issues

### Issue: Demo Account on Live Endpoint
According to cTrader docs, demo accounts CAN exist on the live endpoint. However, routing may fail if:
- The account was created on demo endpoint but you're connecting to live
- The account type doesn't match the endpoint

### Issue: Account ID Mapping
- Visible account ID: **5288664**
- API internal ID: **47340965**
- These are different, and the mapping might not be consistent across endpoints

## Recommended Actions

### Option 1: Verify Endpoint (Recommended)
1. Run `.\verify_account_token.ps1`
2. Check which endpoint returns account 47340965
3. Update `CTRADER_HOST_TYPE` in `start_ctrader_bridge.ps1` accordingly

### Option 2: Use Different Account
If account 47340965 is inaccessible:
1. Check if the token gives access to other accounts
2. Use a different account ID that's accessible
3. Update `CTRADER_ACCOUNT_ID` in the script

### Option 3: Contact Support
If routing continues to fail:
1. Contact Pepperstone support
2. Provide error details: "CANT_ROUTE_REQUEST when authenticating account 47340965"
3. Ask them to verify:
   - Account exists and is active
   - Application is properly configured
   - Account is accessible via API

### Option 4: Use Alternative Broker
As a temporary workaround:
- Binance charts are already working in your app
- Deriv integration is available
- Consider using these while resolving Pepperstone issues

## Technical Details

### Current Configuration
```
Client ID: YOUR_CTRADER_CLIENT_ID
Client Secret: YOUR_CTRADER_CLIENT_SECRET
Access Token: mW5eKJ0xZygSj4BnNvqlteOc_opHJ0EM9Qtcx8RvOfo
Account ID (visible): 5288664
Account ID (API): 47340965
Host Type: live
```

### Authentication Flow
1. ✅ Connect to `live.ctraderapi.com:5035`
2. ✅ Send `ProtoOAApplicationAuthReq` with Client ID/Secret
3. ✅ Receive `ProtoOAApplicationAuthRes` (success)
4. ❌ Send `ProtoOAAccountAuthReq` with Account ID + Access Token
5. ❌ Receive `ProtoOAErrorRes` with CANT_ROUTE_REQUEST

### Error Message
```
errorCode: "CANT_ROUTE_REQUEST"
description: "Cannot route request"
ctidTraderAccountId: 47340965
```

## Next Steps

1. **Run verification script** to identify correct endpoint
2. **Update configuration** based on verification results
3. **Test connection** with correct endpoint
4. **If still failing**, contact Pepperstone support with error details

## References

- cTrader Open API Docs: https://help.ctrader.com/open-api/
- Authentication Flow: https://help.ctrader.com/open-api/authentication-flow/
- Pepperstone Support: https://pepperstone.com/support/
