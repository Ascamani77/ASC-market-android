# Docker Bridge Management Guide

## Current Setup

You have **one Docker Desktop** running multiple containers:

```
Docker Desktop
├── ctrader-bridge-demo (Port 8083) ✅ RUNNING
├── ctrader-bridge-live (Port 8082) ⚪ Not started yet
└── asc-redis (Port 6379) ✅ RUNNING
```

## Starting Bridges

### Demo Bridge (Already Running ✅)
```powershell
# Check status
docker ps | findstr ctrader-bridge-demo

# View logs
docker logs -f ctrader-bridge-demo

# Restart if needed
docker restart ctrader-bridge-demo

# Stop
docker stop ctrader-bridge-demo
```

### Live Bridge (Start When Needed)
```powershell
# Start live bridge
.\start_live_bridge.ps1

# Or manually:
docker run -d \
  --name ctrader-bridge-live \
  --network asc-network \
  -p 8082:8082 \
  -e CLIENT_ID="27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s" \
  -e CLIENT_SECRET="loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty" \
  -e ACCESS_TOKEN="YOUR_LIVE_TOKEN_HERE" \
  -e ACCOUNT_ID="47341092" \
  myrealapp-ctrader-bridge:latest
```

## Managing Both Bridges

### Check Status
```powershell
# See all running containers
docker ps

# See both bridges
docker ps | findstr ctrader-bridge
```

### View Logs
```powershell
# Demo logs
docker logs -f ctrader-bridge-demo

# Live logs
docker logs -f ctrader-bridge-live

# Both at once (in separate terminals)
# Terminal 1:
docker logs -f ctrader-bridge-demo

# Terminal 2:
docker logs -f ctrader-bridge-live
```

### Stop/Start
```powershell
# Stop demo
docker stop ctrader-bridge-demo

# Stop live
docker stop ctrader-bridge-live

# Start demo
docker start ctrader-bridge-demo

# Start live
docker start ctrader-bridge-live

# Restart both
docker restart ctrader-bridge-demo ctrader-bridge-live
```

### Remove Containers
```powershell
# Remove demo (stops and deletes)
docker rm -f ctrader-bridge-demo

# Remove live
docker rm -f ctrader-bridge-live

# Remove both
docker rm -f ctrader-bridge-demo ctrader-bridge-live
```

## Port Mapping

| Bridge | Container Port | Host Port | Account | Status |
|--------|---------------|-----------|---------|--------|
| Demo   | 8083          | 8083      | 47340965 | ✅ Running |
| Live   | 8082          | 8082      | 47341092 | ⚪ Not started |
| Redis  | 6379          | 6379      | N/A     | ✅ Running |

## App Connection

Your Android app connects to:
- **Demo trading**: `localhost:8083` (or `10.0.2.2:8083` from emulator)
- **Live trading**: `localhost:8082` (or `10.0.2.2:8082` from emulator)

## Docker Desktop Usage

### View in Docker Desktop GUI:
1. Open Docker Desktop
2. Click "Containers" tab
3. You'll see:
   - `ctrader-bridge-demo` (green = running)
   - `ctrader-bridge-live` (gray = stopped)
   - `asc-redis` (green = running)

### Actions in GUI:
- **Start**: Click ▶️ button
- **Stop**: Click ⏹️ button
- **Restart**: Click 🔄 button
- **View Logs**: Click container name → Logs tab
- **Delete**: Click 🗑️ button

## Resource Usage

Both bridges running together use approximately:
- **CPU**: ~5-10% total
- **RAM**: ~200-400 MB total
- **Network**: Minimal (WebSocket connections)

This is very light and won't impact your system.

## Common Scenarios

### Scenario 1: Testing Demo Only
```powershell
# Demo is already running ✅
# Just use the app with "Pepperstone Demo" selected
```

### Scenario 2: Using Live Trading
```powershell
# Start live bridge
.\start_live_bridge.ps1

# Use app with "Pepperstone cTrader" selected
```

### Scenario 3: Running Both (Recommended)
```powershell
# Demo is already running ✅
# Start live bridge
.\start_live_bridge.ps1

# Now you can switch between them in the app
```

### Scenario 4: Fresh Start
```powershell
# Stop everything
docker stop ctrader-bridge-demo ctrader-bridge-live asc-redis

# Start Redis first
docker start asc-redis

# Start demo
docker start ctrader-bridge-demo

# Start live
docker start ctrader-bridge-live
```

## Troubleshooting

### Port Already in Use
```powershell
# Check what's using port 8082
netstat -ano | findstr :8082

# Check what's using port 8083
netstat -ano | findstr :8083

# If another container is using it, stop it:
docker ps -a | findstr 8082
docker stop <container_name>
```

### Bridge Not Connecting
```powershell
# Check logs for errors
docker logs ctrader-bridge-demo
docker logs ctrader-bridge-live

# Common issues:
# 1. ACCESS_TOKEN expired → Get new token
# 2. Network issue → Restart container
# 3. Redis not running → Start Redis first
```

### Redis Connection Failed
```powershell
# Check Redis is running
docker ps | findstr redis

# Start Redis if stopped
docker start asc-redis

# Check Redis logs
docker logs asc-redis
```

## Best Practices

1. **Keep Demo Running**: Demo bridge can run 24/7 safely
2. **Start Live When Needed**: Only run live bridge when actively trading
3. **Monitor Logs**: Check logs occasionally for errors
4. **Update Tokens**: Live token expires, demo token lasts ~30 days
5. **Restart Weekly**: Restart bridges once a week for stability

## Quick Commands Cheat Sheet

```powershell
# Status check
docker ps

# Start live bridge
.\start_live_bridge.ps1

# View demo logs
docker logs -f ctrader-bridge-demo

# View live logs
docker logs -f ctrader-bridge-live

# Restart demo
docker restart ctrader-bridge-demo

# Restart live
docker restart ctrader-bridge-live

# Stop all bridges
docker stop ctrader-bridge-demo ctrader-bridge-live

# Start all bridges
docker start ctrader-bridge-demo ctrader-bridge-live
```

## Summary

✅ **Same Docker Desktop** - One installation runs everything
✅ **Separate Containers** - Demo and live are isolated
✅ **Different Ports** - No conflicts (8082 vs 8083)
✅ **Independent** - Can run one or both
✅ **Shared Redis** - Both use same Redis instance
✅ **Easy Management** - Simple start/stop commands

You're all set! The demo bridge is already running, and you can start the live bridge whenever you need it.
