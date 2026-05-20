# ✅ Live Account Configuration Complete

## What Was Updated

### 1. Token Generated
- **Live Account**: 1360716
- **API Account ID**: 47341092
- **Access Token**: bz5h7SrzyS_xb8udgcckRcGJU2D9FgIMCAkegWiRFeE
- **Refresh Token**: GVW380A7a2fk1-RiTO3HmzMI0Ri3pqM8HRsAOLoMkZY
- **Expires**: 30 days from now

### 2. Files Updated

#### local.properties
```properties
CTRADER_HOST_TYPE=live
CTRADER_ACCOUNT_ID=47341092
CTRADER_ACCESS_TOKEN=bz5h7SrzyS_xb8udgcckRcGJU2D9FgIMCAkegWiRFeE
CTRADER_REFRESH_TOKEN=GVW380A7a2fk1-RiTO3HmzMI0Ri3pqM8HRsAOLoMkZY
CTRADER_BRIDGE_HOST=10.164.138.133
```

#### start_ctrader_bridge.ps1
```powershell
$env:CTRADER_ACCOUNT_ID = "47341092"
$env:CTRADER_ACCESS_TOKEN = "bz5h7SrzyS_xb8udgcckRcGJU2D9FgIMCAkegWiRFeE"
$env:CTRADER_HOST_TYPE = "live"
```

#### ctrader_tokens_live.json
- Token data saved for future reference

## Next Steps

### 1. Start the Bridge
```powershell
.\start_ctrader_bridge.ps1
```

**Expected Output:**
```
✅ Connected to cTrader Open API
✅ Application authenticated
✅ Account authenticated (account 47341092)
✅ Symbols loaded
```

### 2. Test in Your App
1. Make sure bridge is running
2. Open your Android app
3. Navigate to Pepperstone chart
4. **Candles should now display!**

## Configuration Summary

| Setting | Value |
|---------|-------|
| Account Type | LIVE |
| Visible Account ID | 1360716 |
| API Account ID | 47341092 |
| Endpoint | live.ctraderapi.com:5035 |
| Bridge Host | 10.164.138.133 |
| Bridge Port | 8082 |
| Token Expiry | 30 days |

## Key Changes from Demo

| Setting | Demo (Old) | Live (New) |
|---------|-----------|-----------|
| HOST_TYPE | demo | live |
| Account ID | 47340965 | 47341092 |
| Visible ID | 5288664 | 1360716 |
| Token | Old demo token | New live token |

## Troubleshooting

### If Bridge Doesn't Start
1. Check Python is installed: `python --version`
2. Check port 8082 is available
3. Check internet connection

### If Authentication Fails
1. Verify token in local.properties matches ctrader_tokens_live.json
2. Verify account ID is 47341092 (not 1360716)
3. Verify HOST_TYPE is "live"

### If Candles Don't Display
1. Verify bridge is running (check PowerShell window)
2. Verify app is connecting to 10.164.138.133:8082
3. Check bridge logs for errors

## Success Indicators

### Bridge Console
```
[cTrader] connected: Connected to cTrader Open API
[cTrader] application_authenticated: cTrader application authenticated
[cTrader] account_authenticated: cTrader account 47341092 authenticated
[cTrader] symbols_loaded: Loaded 50+ broker symbols
[cTrader] subscribed: Subscribed to Pepperstone cTrader symbols
```

### Android App
- Candles display on chart ✅
- Price updates in real-time ✅
- Volume bars visible ✅
- No error messages ✅

## Important Notes

1. **Account ID Mapping**
   - Visible ID (1360716) ≠ API ID (47341092)
   - Always use API ID (47341092) in configuration

2. **Token Expiration**
   - Access token expires in 30 days
   - Use refresh token to get new access token
   - Or regenerate with `.\get_live_token_fixed.ps1`

3. **Live Account**
   - This is a LIVE account with real money
   - Be careful with trading operations
   - Test thoroughly before production use

4. **Bridge Must Stay Running**
   - Keep PowerShell window open
   - Bridge streams data to your app
   - Press Ctrl+C to stop

## Maintenance

### Daily
- Verify bridge is running
- Check for error messages

### Weekly
- Review bridge logs
- Test all symbols

### Monthly (Before Token Expires)
- Run `.\get_live_token_fixed.ps1`
- Update local.properties with new token
- Restart bridge

## Support

If you encounter issues:
1. Check bridge console for errors
2. Review this document
3. Check `QUICK_FIX_STEPS.md`
4. Contact Pepperstone support: support@pepperstone.com

## Status

- [x] Token generated for live account 1360716
- [x] API account ID discovered: 47341092
- [x] local.properties updated
- [x] start_ctrader_bridge.ps1 updated
- [ ] Bridge started and running
- [ ] Candles displaying in app

**Next**: Start the bridge with `.\start_ctrader_bridge.ps1`
