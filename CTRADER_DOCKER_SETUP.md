# cTrader Bridge in Docker - No More Freezing!

## The Problem

The cTrader bridge running as a Python process:
- ❌ Freezes and stops sending data
- ❌ Requires manual restart
- ❌ Crashes without auto-recovery
- ❌ Hard to monitor

## The Solution: Docker

Running cTrader bridge in Docker provides:
- ✅ **Auto-restart** if it crashes or freezes
- ✅ **Runs in background** - no window to close accidentally
- ✅ **Easy monitoring** with `docker logs`
- ✅ **Starts with Docker Desktop** - set and forget
- ✅ **Isolated environment** - won't conflict with other processes

## Quick Start

### First Time Setup

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_ctrader_docker.ps1
```

This will:
1. Build the Docker image (1-2 minutes first time)
2. Start Redis container
3. Start cTrader bridge container
4. Configure auto-restart

### Daily Use

Just make sure Docker Desktop is running - the containers will start automatically!

Or manually start:
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
docker-compose -f docker-compose.ctrader.yml up -d
```

## Monitoring

### View live logs:
```powershell
docker logs -f ctrader-bridge
```

You should see:
```
✅ Connected to cTrader Open API
✅ Account authenticated
✅ Subscribed to symbols
Broadcasting tick: Crude-F @ 82.45
Broadcasting tick: Brent-F @ 85.12
Broadcasting tick: XAUUSD @ 2342.50
```

### Check if running:
```powershell
docker ps
```

Should show:
```
CONTAINER ID   IMAGE                  STATUS          PORTS                    NAMES
xxxxx          ctrader-bridge:latest  Up 5 minutes    0.0.0.0:8082->8082/tcp   ctrader-bridge
xxxxx          redis:7-alpine         Up 5 minutes    0.0.0.0:6379->6379/tcp   asc-redis
```

### Check health:
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

## If It Freezes

### Quick restart:
```powershell
docker restart ctrader-bridge
```

### Full restart:
```powershell
docker-compose -f docker-compose.ctrader.yml restart
```

### View recent logs to diagnose:
```powershell
docker logs --tail 100 ctrader-bridge
```

## Complete System Startup

Now your complete system is:

### 1. Start Docker Containers (once)
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_ctrader_docker.ps1
```

### 2. Start AI Production System
```powershell
cd C:\Users\HP\Documents\NEW_ASC
.\start_production_live.ps1
```

That's it! Only 2 commands instead of 3 windows.

## System Architecture

```
┌─────────────────────────────────────┐
│  Docker Container: ctrader-bridge   │
│  - Connects to Pepperstone          │
│  - Auto-restarts on crash           │
│  - Publishes to Redis               │
│  Port: 8082                         │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  Docker Container: Redis            │
│  - Stream: market.ticks.stream      │
│  - Persistent storage               │
│  Port: 6379                         │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  AI Production System (PowerShell)  │
│  - Reads from Redis                 │
│  - Processes live data              │
│  - Updates every 5-15 min           │
│  Port: 8000                         │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  Your Android App                   │
│  - Polls API every 5 seconds        │
│  - Shows live data                  │
└─────────────────────────────────────┘
```

## Configuration

Your Pepperstone credentials are in `docker-compose.ctrader.yml`:
- Client ID: `27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s`
- Account ID: `47341092`
- Host: `live` (production)

If you need to change them, edit the file and restart:
```powershell
docker-compose -f docker-compose.ctrader.yml down
docker-compose -f docker-compose.ctrader.yml up -d
```

## Troubleshooting

### Container won't start
```powershell
# Check logs
docker logs ctrader-bridge

# Rebuild
docker-compose -f docker-compose.ctrader.yml down
docker-compose -f docker-compose.ctrader.yml up -d --build
```

### No data flowing
```powershell
# Check Redis stream
docker exec asc-redis redis-cli XLEN market.ticks.stream

# Should show increasing number
# If 0, check cTrader logs
docker logs ctrader-bridge
```

### Port already in use
```powershell
# Stop old Python process
Get-Process python | Where-Object {$_.Path -like "*ctrader*"} | Stop-Process

# Or stop all on port 8082
netstat -ano | findstr :8082
# Then kill the PID
```

## Advantages Over Python Process

| Feature | Python Process | Docker Container |
|---------|---------------|------------------|
| Auto-restart | ❌ No | ✅ Yes |
| Background | ❌ Needs window | ✅ True background |
| Monitoring | ❌ Hard | ✅ Easy (`docker logs`) |
| Freezing | ❌ Common | ✅ Rare (auto-restart) |
| Startup | ❌ Manual | ✅ Automatic |
| Isolation | ❌ No | ✅ Yes |

## Files Created

1. **`Dockerfile.ctrader`** - Docker image definition
2. **`docker-compose.ctrader.yml`** - Container configuration
3. **`requirements.txt`** - Python dependencies
4. **`start_ctrader_docker.ps1`** - Easy startup script
5. **`CTRADER_DOCKER_SETUP.md`** - This guide

## Next Steps

1. **Stop old Python cTrader** (if running)
2. **Run**: `.\start_ctrader_docker.ps1`
3. **Wait 1 minute** for containers to start
4. **Check logs**: `docker logs -f ctrader-bridge`
5. **Start AI**: `cd C:\Users\HP\Documents\NEW_ASC; .\start_production_live.ps1`

## Summary

✅ **No more freezing** - Docker auto-restarts
✅ **No more manual restarts** - Runs in background
✅ **Easy monitoring** - `docker logs`
✅ **Reliable** - Starts with Docker Desktop

Your cTrader bridge will now be rock-solid!
