# Podman Setup for cTrader & Redis

Quick reference for running your Pepperstone cTrader bridges and Redis with Podman.

---

## 🚀 Quick Start (3 Steps)

### 1. Install Podman
```powershell
winget install RedHat.Podman-Desktop
```

### 2. Start Services
```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
.\start_podman_ctrader.ps1
```

### 3. Verify
```powershell
.\check_podman_status.ps1
```

**Done!** Your services are running. ✅

---

## 📋 Services

| Service | Port | Status |
|---------|------|--------|
| **cTrader Live** | 8082 | http://localhost:8082 |
| **cTrader Demo** | 8083 | http://localhost:8083 |
| **Redis** | 6379 | localhost:6379 |

---

## 🎯 Common Commands

```powershell
# Start everything
.\start_podman_ctrader.ps1

# Check status
.\check_podman_status.ps1

# Stop everything
.\stop_podman_services.ps1

# View logs
podman logs -f ctrader-bridge-live
podman logs -f asc-redis

# Restart a service
podman restart ctrader-bridge-live
podman restart asc-redis
```

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| `podman-compose.ctrader-both.yml` | Service configuration |
| `start_podman_ctrader.ps1` | Start script |
| `check_podman_status.ps1` | Status checker |
| `stop_podman_services.ps1` | Stop script |

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| `PODMAN_QUICK_START.md` | Quick reference guide |
| `PODMAN_MIGRATION_GUIDE.md` | Complete migration guide |
| `DOCKER_TO_PODMAN_CHECKLIST.md` | Step-by-step checklist |
| `PODMAN_COMMANDS_CHEATSHEET.md` | Command reference |
| `DOCKER_VS_PODMAN.md` | Comparison guide |

---

## 🆘 Troubleshooting

### Services won't start?
```powershell
# Check logs
podman logs ctrader-bridge-live

# Rebuild
podman compose -f podman-compose.ctrader-both.yml up -d --build
```

### Port already in use?
```powershell
# Stop Docker first
docker stop ctrader-bridge-live ctrader-bridge-demo asc-redis
```

### Redis not responding?
```powershell
# Test Redis
podman exec asc-redis redis-cli ping

# Restart if needed
podman restart asc-redis
```

---

## 💡 Tips

- **Auto-restart**: Containers restart automatically on failure
- **Persistent data**: Redis data is saved in volumes
- **No code changes**: Your Python/Android code works as-is
- **Same endpoints**: All services use the same ports

---

## 🔗 Resources

- **Podman Desktop**: https://podman-desktop.io/
- **Documentation**: https://docs.podman.io/
- **Support**: Check the documentation files above

---

**Need help?** See `PODMAN_MIGRATION_GUIDE.md` for detailed instructions.
