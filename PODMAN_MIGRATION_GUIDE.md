# Docker to Podman Migration Guide

## Overview

This guide covers the migration from Docker to Podman for:
- ✅ **Redis** - Data cache and message broker
- ✅ **cTrader Live Bridge** - Live trading account (port 8082)
- ✅ **cTrader Demo Bridge** - Demo trading account (port 8083)

## Why Podman?

- **Rootless by default** - Better security, no daemon required
- **Docker-compatible** - Same commands and Compose files work
- **Lightweight** - Lower resource usage
- **Open source** - No licensing concerns

---

## Installation

### Option 1: Podman Desktop (Recommended)
Download from: https://podman-desktop.io/downloads

### Option 2: Winget
```powershell
winget install RedHat.Podman-Desktop
```

### Option 3: Chocolatey
```powershell
choco install podman-desktop
```

### Verify Installation
```powershell
podman --version
podman-compose --version  # Optional but recommended
```

---

## Migration Steps

### 1. Stop Docker Containers

```powershell
# Stop cTrader bridges
docker stop ctrader-bridge-live ctrader-bridge-demo

# Stop Redis
docker stop asc-redis

# Optional: Remove Docker containers (keeps images)
docker rm ctrader-bridge-live ctrader-bridge-demo asc-redis
```

### 2. Start Services with Podman

#### Option A: All Services (Recommended)
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_podman_ctrader.ps1
```

This starts:
- cTrader Live Bridge (port 8082)
- cTrader Demo Bridge (port 8083)
- Redis (port 6379)

#### Option B: Redis Only
```powershell
cd C:\Users\HP\Documents\NEW_ASC
.\start_podman_redis.ps1
```

#### Option C: Manual Podman Compose
```powershell
# All services
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
podman compose -f podman-compose.ctrader-both.yml up -d

# Redis only
cd C:\Users\HP\Documents\NEW_ASC
podman compose -f podman-compose.yml up -d
```

---

## Configuration Files

### Created Files

| File | Purpose |
|------|---------|
| `podman-compose.ctrader-both.yml` | cTrader Live + Demo + Redis |
| `podman-compose.yml` (NEW_ASC) | Redis only |
| `start_podman_ctrader.ps1` | Easy startup script for all services |
| `start_podman_redis.ps1` | Easy startup script for Redis only |

### Credentials

Your live cTrader credentials are already configured in `podman-compose.ctrader-both.yml`:
- **Client ID**: `YOUR_CTRADER_CLIENT_ID`
- **Account ID**: `47341092`

**Demo credentials** need to be updated in the same file (replace `YOUR_DEMO_*` placeholders).

---

## Command Reference

### Podman vs Docker Commands

| Docker Command | Podman Equivalent |
|----------------|-------------------|
| `docker ps` | `podman ps` |
| `docker logs <name>` | `podman logs <name>` |
| `docker stop <name>` | `podman stop <name>` |
| `docker start <name>` | `podman start <name>` |
| `docker rm <name>` | `podman rm <name>` |
| `docker-compose up -d` | `podman compose up -d` |
| `docker-compose down` | `podman compose down` |

### Common Operations

#### View Running Containers
```powershell
podman ps
```

#### View All Containers (including stopped)
```powershell
podman ps -a
```

#### View Logs
```powershell
# Follow logs in real-time
podman logs -f ctrader-bridge-live
podman logs -f ctrader-bridge-demo
podman logs -f asc-redis

# Last 100 lines
podman logs --tail 100 ctrader-bridge-live
```

#### Stop Services
```powershell
# Stop all
podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis

# Stop individual
podman stop ctrader-bridge-live
```

#### Restart Services
```powershell
# Restart all
podman restart ctrader-bridge-live ctrader-bridge-demo asc-redis

# Restart individual
podman restart asc-redis
```

#### Remove Containers
```powershell
# Remove (keeps data volumes)
podman rm -f ctrader-bridge-live ctrader-bridge-demo asc-redis

# Remove with volumes (deletes data)
podman compose -f podman-compose.ctrader-both.yml down -v
```

#### Rebuild Images
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
podman compose -f podman-compose.ctrader-both.yml up -d --build
```

#### Access Redis CLI
```powershell
podman exec -it asc-redis redis-cli
```

---

## Service Endpoints

| Service | Endpoint | Purpose |
|---------|----------|---------|
| **Live Bridge** | `http://localhost:8082` | Pepperstone live trading |
| **Demo Bridge** | `http://localhost:8083` | Pepperstone demo trading |
| **Redis** | `localhost:6379` | Data cache |

---

## Health Checks

### Check All Services
```powershell
podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Test Redis
```powershell
podman exec asc-redis redis-cli ping
# Should return: PONG
```

### Test cTrader Bridges
```powershell
# Live bridge
curl http://localhost:8082/health

# Demo bridge
curl http://localhost:8083/health
```

---

## Troubleshooting

### Issue: "podman: command not found"
**Solution**: Install Podman Desktop or restart your terminal after installation.

### Issue: Containers won't start
**Solution**: Check logs for errors
```powershell
podman logs ctrader-bridge-live
podman logs asc-redis
```

### Issue: Port already in use
**Solution**: Stop Docker containers first
```powershell
docker stop ctrader-bridge-live ctrader-bridge-demo asc-redis
```

### Issue: Redis data not persisting
**Solution**: Check volume exists
```powershell
podman volume ls
podman volume inspect redis-data
```

### Issue: Can't connect to Redis from Python
**Solution**: Verify Redis is running and accessible
```powershell
podman exec asc-redis redis-cli ping
```

---

## Updating Credentials

### Update Live Credentials
Edit `podman-compose.ctrader-both.yml`:
```yaml
ctrader-bridge-live:
  environment:
    - CTRADER_CLIENT_ID=your_new_client_id
    - CTRADER_CLIENT_SECRET=your_new_secret
    - CTRADER_ACCESS_TOKEN=your_new_token
```

Then restart:
```powershell
podman compose -f podman-compose.ctrader-both.yml restart ctrader-bridge-live
```

### Update Demo Credentials
Same process, but edit the `ctrader-bridge-demo` section.

---

## Performance Tips

### 1. Allocate More Resources
In Podman Desktop:
- Settings → Resources
- Increase CPU/Memory if needed

### 2. Prune Unused Resources
```powershell
# Remove unused containers
podman container prune

# Remove unused images
podman image prune

# Remove unused volumes
podman volume prune
```

### 3. Monitor Resource Usage
```powershell
podman stats
```

---

## Backup and Restore

### Backup Redis Data
```powershell
# Create backup
podman exec asc-redis redis-cli SAVE
podman cp asc-redis:/data/dump.rdb ./redis-backup.rdb

# Or backup the volume
podman volume export redis-data > redis-data-backup.tar
```

### Restore Redis Data
```powershell
# Stop Redis
podman stop asc-redis

# Restore backup
podman cp ./redis-backup.rdb asc-redis:/data/dump.rdb

# Start Redis
podman start asc-redis
```

---

## Integration with Existing Scripts

Your existing Python scripts and AI system will work without changes. They connect to:
- `localhost:6379` (Redis)
- `localhost:8082` (Live Bridge)
- `localhost:8083` (Demo Bridge)

No code changes needed! 🎉

---

## Next Steps

1. ✅ Install Podman Desktop
2. ✅ Stop Docker containers
3. ✅ Run `.\start_podman_ctrader.ps1`
4. ✅ Verify services are running: `podman ps`
5. ✅ Test your AI system

---

## Support

If you encounter issues:
1. Check logs: `podman logs <container-name>`
2. Verify Podman version: `podman --version`
3. Check Podman Desktop is running
4. Restart Podman machine: `podman machine restart`

---

## Rollback to Docker

If you need to go back to Docker:
```powershell
# Stop Podman containers
podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis

# Start Docker containers
docker-compose -f docker-compose.ctrader-both.yml up -d
```

Your data volumes are separate, so no data loss.
