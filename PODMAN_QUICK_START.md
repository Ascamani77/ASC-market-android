# Podman Quick Start Guide

## 🚀 Quick Commands

### Start Everything
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_podman_ctrader.ps1
```

This starts:
- ✅ cTrader Live Bridge (port 8082)
- ✅ cTrader Demo Bridge (port 8083)  
- ✅ Redis (port 6379)

### Check Status
```powershell
.\check_podman_status.ps1
```

### Stop Everything
```powershell
.\stop_podman_services.ps1
```

---

## 📋 Individual Services

### Redis Only
```powershell
cd C:\Users\HP\Documents\NEW_ASC
.\start_podman_redis.ps1
```

### View Logs
```powershell
# Follow logs in real-time
podman logs -f ctrader-bridge-live
podman logs -f ctrader-bridge-demo
podman logs -f asc-redis

# Last 50 lines
podman logs --tail 50 ctrader-bridge-live
```

### Restart a Service
```powershell
podman restart ctrader-bridge-live
podman restart asc-redis
```

---

## 🔧 Common Tasks

### Update cTrader Credentials
1. Edit `podman-compose.ctrader-both.yml`
2. Update the environment variables
3. Restart: `podman restart ctrader-bridge-live`

### Rebuild After Code Changes
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
podman compose -f podman-compose.ctrader-both.yml up -d --build
```

### Access Redis CLI
```powershell
podman exec -it asc-redis redis-cli
```

### Clean Up Everything
```powershell
# Stop and remove containers (keeps volumes)
podman rm -f ctrader-bridge-live ctrader-bridge-demo asc-redis

# Remove volumes too (deletes data)
podman volume rm redis-data
```

---

## 🆘 Troubleshooting

### Container Won't Start
```powershell
# Check logs
podman logs ctrader-bridge-live

# Remove and recreate
podman rm -f ctrader-bridge-live
.\start_podman_ctrader.ps1
```

### Port Already in Use
```powershell
# Check what's using the port
netstat -ano | findstr :8082

# Stop Docker containers if still running
docker stop ctrader-bridge-live ctrader-bridge-demo asc-redis
```

### Redis Connection Issues
```powershell
# Test Redis
podman exec asc-redis redis-cli ping

# Should return: PONG
```

---

## 📁 Configuration Files

| File | Purpose |
|------|---------|
| `podman-compose.ctrader-both.yml` | Main config for all services |
| `start_podman_ctrader.ps1` | Easy startup script |
| `check_podman_status.ps1` | Status checker |
| `stop_podman_services.ps1` | Stop all services |

---

## 🔗 Service Endpoints

- **Live Bridge**: http://localhost:8082
- **Demo Bridge**: http://localhost:8083
- **Redis**: localhost:6379

---

## 💡 Tips

1. **Auto-start on boot**: Containers have `restart: unless-stopped` policy
2. **View all containers**: `podman ps -a`
3. **Resource usage**: `podman stats`
4. **Clean up unused images**: `podman image prune`

---

## 📚 Full Documentation

See `PODMAN_MIGRATION_GUIDE.md` for complete details.
