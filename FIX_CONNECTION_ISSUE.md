# Fix Connection Issue - Redis and cTrader Not Showing Live Data

## Problem Identified
Your Android app is configured to connect to **10.76.160.133**, but your PC's current IP address is **192.168.1.198**.

## Your Current Network Configuration
```
Wi-Fi IP Address: 192.168.1.198
Subnet: 255.255.255.0
Gateway: 192.168.1.1
```

## Services Running on Your PC
All services are running on `localhost` (127.0.0.1) but need to be accessible from your Android device:

- **Redis**: Port 6379
- **AI Backend**: Port 8000
- **cTrader Live Bridge**: Port 8082
- **cTrader Demo Bridge**: Port 8083

## Solution: Update App Configuration

### Option 1: Update via App Settings (Recommended)
1. Open your app on Android device
2. Navigate to **Settings** or **Configuration** screen
3. Update the following values:
   - **Backend URL**: `http://192.168.1.198:8000`
   - **cTrader Host**: `192.168.1.198`
   - **cTrader Port**: `8082`
   - **cTrader Demo Host**: `192.168.1.198`
   - **cTrader Demo Port**: `8083`
4. Save and restart the app

### Option 2: Update Default Configuration (For Development)
If you want to change the default IP in the code:

Edit `NetworkConfig.kt`:
```kotlin
const val DEFAULT_HOST = "192.168.1.198"  // Change from "10.76.160.133"
const val DEFAULT_BACKEND_URL = "http://192.168.1.198:8000"
```

Then rebuild the app.

## Verify Services Are Running

### 1. Check Redis
```powershell
podman exec asc-redis redis-cli ping
# Should return: PONG
```

### 2. Check AI Backend
```powershell
curl http://localhost:8000/health
# Or open in browser: http://localhost:8000/docs
```

### 3. Check cTrader Bridges
Make sure your cTrader bridge scripts are running:
```powershell
# Check if ctrader_bridge.py is running
Get-Process python | Where-Object { $_.CommandLine -like "*ctrader*" }
```

## Start cTrader Bridges (If Not Running)

### Start cTrader Live Bridge (Port 8082)
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
python ctrader_bridge.py
```

### Start cTrader Demo Bridge (Port 8083)
You'll need a separate bridge script for demo or configure it to run on port 8083.

## Firewall Configuration
Make sure Windows Firewall allows connections on these ports:

```powershell
# Allow Redis
New-NetFirewallRule -DisplayName "Redis" -Direction Inbound -LocalPort 6379 -Protocol TCP -Action Allow

# Allow AI Backend
New-NetFirewallRule -DisplayName "AI Backend" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow

# Allow cTrader Live
New-NetFirewallRule -DisplayName "cTrader Live" -Direction Inbound -LocalPort 8082 -Protocol TCP -Action Allow

# Allow cTrader Demo
New-NetFirewallRule -DisplayName "cTrader Demo" -Direction Inbound -LocalPort 8083 -Protocol TCP -Action Allow
```

## Test Connection from Android Device

### Test Backend Connection
Open browser on Android and navigate to:
```
http://192.168.1.198:8000/docs
```
You should see the FastAPI documentation page.

### Test Redis Connection
Use a Redis client app on Android or test from your PC:
```powershell
# Test from PC (should work)
redis-cli -h localhost ping

# Test from network (what Android will use)
redis-cli -h 192.168.1.198 ping
```

## Common Issues

### Issue 1: Services Only Listen on Localhost
**Problem**: Services are bound to 127.0.0.1 and not accessible from network.

**Solution**: Configure services to listen on 0.0.0.0 (all interfaces):

**Redis** (in podman-compose.yml):
```yaml
ports:
  - "0.0.0.0:6379:6379"
```

**AI Backend** (in ai_api.py):
```python
uvicorn.run(app, host="0.0.0.0", port=8000)
```

**cTrader Bridge** (in ctrader_bridge.py):
```python
app.run(host="0.0.0.0", port=8082)
```

### Issue 2: IP Address Changes
Your Wi-Fi IP (192.168.1.198) may change if your router assigns dynamic IPs.

**Solution**: Set a static IP in your router for your PC's MAC address, or use your PC's hostname instead of IP.

### Issue 3: Podman Network Isolation
Podman containers may not be accessible from the host network.

**Solution**: Use host network mode:
```powershell
podman run --network host redis
```

## Quick Diagnostic Commands

```powershell
# Check what's listening on ports
netstat -an | findstr "6379 8000 8082 8083"

# Check Podman containers
podman ps

# Check Python processes
Get-Process python

# Test local connectivity
Test-NetConnection -ComputerName localhost -Port 8000
Test-NetConnection -ComputerName 192.168.1.198 -Port 8000
```

## Next Steps

1. ✅ Verify all services are running
2. ✅ Update app configuration to use 192.168.1.198
3. ✅ Test connection from Android device
4. ✅ Check Market Data Bus page shows ACTIVE status
5. ✅ Verify prices are updating in real-time

## Need Help?
If services still don't connect:
1. Check Windows Firewall logs
2. Verify Android and PC are on same Wi-Fi network
3. Try pinging PC from Android: `ping 192.168.1.198`
4. Check if VPN or antivirus is blocking connections
