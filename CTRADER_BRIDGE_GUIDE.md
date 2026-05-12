# cTrader Bridge Setup Guide

## Quick Start

### Windows PowerShell (Recommended)
```powershell
.\start_ctrader_bridge.ps1
```

### Windows Command Prompt
```cmd
start_ctrader_bridge.bat
```

### Manual Start
```powershell
$env:CTRADER_CLIENT_ID="27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$env:CTRADER_CLIENT_SECRET="loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$env:CTRADER_ACCESS_TOKEN="sfV4Gls2KooxFKKpsqaUpboQswkPBz65DddPOZkLX-E"
$env:CTRADER_ACCOUNT_ID="47223753"
$env:CTRADER_HOST_TYPE="demo"
python .\ctrader_bridge.py
```

## Configuration

### Current Settings
- **Host Type**: Demo (sandbox environment)
- **Account ID**: 47223753 (auto-discovered from access token)
- **Bridge Port**: 8082
- **Symbols**: EURUSD, GBPUSD, USDJPY, USDCHF, AUDUSD, XAUUSD, XAGUSD, USOIL (Crude-F), DXY, NAS100, US30, SPX500

### Credentials Location
All credentials are stored in `local.properties` and loaded by the startup scripts.

## Testing the Bridge

### 1. Start the Bridge
Run one of the startup scripts above. You should see:
```
✓ Connected to cTrader
✓ Application authenticated
✓ Account authenticated
✓ Symbols loaded
✓ Subscribed to symbols
```

### 2. Test from Android App
- **Emulator**: Connect to `ws://10.0.2.2:8082`
- **Physical Device**: Connect to `ws://YOUR_COMPUTER_IP:8082`

To find your computer's IP:
```powershell
ipconfig | findstr IPv4
```

### 3. Monitor Bridge Activity
Watch the console for:
- Connection messages from your app
- Price tick updates being sent
- Any errors or warnings

## Troubleshooting

### Port Already in Use
If you see "port 8082 already in use":
```powershell
# Find the process using port 8082
netstat -ano | findstr :8082

# Stop the process (replace PID with actual process ID)
Stop-Process -Id PID -Force
```

### Authentication Errors
If you see "CH_CLIENT_AUTH_FAILURE":
- Verify credentials in `local.properties` are correct
- Check that you're using the demo environment
- Ensure access token hasn't expired

### Symbol Not Found
If a symbol can't be resolved:
- Check the symbol name in Pepperstone cTrader platform
- Update the `DEFAULT_SYMBOL_MAP` in `ctrader_bridge.py`
- Restart the bridge

### Connection Drops
If the bridge disconnects frequently:
- Check your internet connection
- Verify firewall isn't blocking the connection
- Try switching between demo/live endpoints

## Symbol Mapping

The bridge maps app symbols to Pepperstone broker symbols:

| App Symbol | Pepperstone Symbol |
|------------|-------------------|
| EURUSD     | EURUSD            |
| GBPUSD     | GBPUSD            |
| USDJPY     | USDJPY            |
| USDCHF     | USDCHF            |
| AUDUSD     | AUDUSD            |
| XAUUSD     | XAUUSD (Gold)     |
| XAGUSD     | XAGUSD (Silver)   |
| USOIL      | Crude-F           |
| DXY        | DXY               |
| NAS100     | NAS100            |
| US30       | US30              |
| SPX500     | SPX500            |

To add more symbols, edit the `DEFAULT_SYMBOL_MAP` in `ctrader_bridge.py`.

## Switching to Live Trading

⚠️ **WARNING**: Only switch to live when you're ready to trade with real money!

1. Update `local.properties`:
   ```properties
   CTRADER_HOST_TYPE=live
   ```

2. Get live credentials from [cTrader Open API](https://openapi.ctrader.com/)

3. Update access token for live account

4. Restart the bridge

## Access Token Refresh

Access tokens expire after a period. To refresh:

1. Use the refresh token:
   ```
   CTRADER_REFRESH_TOKEN=O0QYC0A8aH4SH5JLfliBnj7w2K_MWJet4v9ZEpHv0mM
   ```

2. The bridge will automatically request a new access token when needed

3. Update `local.properties` with the new token

## Support

- **cTrader API Docs**: https://help.ctrader.com/open-api/
- **Pepperstone Support**: https://pepperstone.com/support/
- **Bridge Issues**: Check console output for detailed error messages
