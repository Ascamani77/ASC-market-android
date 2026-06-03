# Docker to Podman Migration Checklist

## Pre-Migration

- [ ] **Backup Redis data** (if you have important data)
  ```powershell
  docker exec asc-redis redis-cli SAVE
  docker cp asc-redis:/data/dump.rdb ./redis-backup.rdb
  ```

- [ ] **Note your current Docker containers**
  ```powershell
  docker ps -a
  ```

- [ ] **Save current credentials** (already in config files, but double-check)
  - Live cTrader credentials in `docker-compose.ctrader.yml`
  - Demo cTrader credentials (if configured)

## Installation

- [ ] **Install Podman Desktop**
  - Download from: https://podman-desktop.io/downloads
  - Or use: `winget install RedHat.Podman-Desktop`

- [ ] **Verify Podman installation**
  ```powershell
  podman --version
  podman machine init
  podman machine start
  ```

- [ ] **Optional: Install podman-compose**
  ```powershell
  pip install podman-compose
  ```

## Migration Steps

### 1. Stop Docker Containers

- [ ] **Stop all Docker containers**
  ```powershell
  docker stop ctrader-bridge-live ctrader-bridge-demo asc-redis
  ```

- [ ] **Verify they're stopped**
  ```powershell
  docker ps
  # Should show no running containers
  ```

- [ ] **Optional: Remove Docker containers** (keeps images and volumes)
  ```powershell
  docker rm ctrader-bridge-live ctrader-bridge-demo asc-redis
  ```

### 2. Start Podman Services

- [ ] **Start all services with Podman**
  ```powershell
  cd C:\Users\HP\AndroidStudioProjects\MyRealApp
  .\start_podman_ctrader.ps1
  ```

- [ ] **Wait for services to initialize** (about 10 seconds)

### 3. Verify Services

- [ ] **Check container status**
  ```powershell
  .\check_podman_status.ps1
  ```

- [ ] **Verify Redis is working**
  ```powershell
  podman exec asc-redis redis-cli ping
  # Should return: PONG
  ```

- [ ] **Test cTrader Live Bridge**
  ```powershell
  curl http://localhost:8082/health
  # Or open in browser
  ```

- [ ] **Test cTrader Demo Bridge** (if configured)
  ```powershell
  curl http://localhost:8083/health
  ```

### 4. Test Your Application

- [ ] **Start your AI backend**
  ```powershell
  cd C:\Users\HP\Documents\NEW_ASC
  python ai_api.py
  ```

- [ ] **Verify AI can connect to Redis**
  - Check AI logs for Redis connection messages
  - Should see successful connection to `localhost:6379`

- [ ] **Test cTrader integration**
  - Send a test request to live bridge
  - Verify response from Pepperstone

- [ ] **Run your Android app**
  - Build and run the app
  - Verify it connects to all services

### 5. Update Scripts (Optional)

- [ ] **Update startup scripts to use Podman**
  - Replace `docker` commands with `podman`
  - Or use the new `start_podman_*.ps1` scripts

- [ ] **Update documentation**
  - Note that you're now using Podman
  - Update any team documentation

## Post-Migration

### Cleanup (Optional)

- [ ] **Remove Docker containers** (if not done earlier)
  ```powershell
  docker rm ctrader-bridge-live ctrader-bridge-demo asc-redis
  ```

- [ ] **Remove Docker images** (optional, saves disk space)
  ```powershell
  docker rmi $(docker images -q)
  ```

- [ ] **Uninstall Docker Desktop** (optional)
  - Only if you're fully committed to Podman
  - Keep it if you want both options

### Configuration

- [ ] **Set Podman to start on boot**
  - Podman Desktop → Settings → General → Start on login

- [ ] **Configure resource limits** (if needed)
  - Podman Desktop → Settings → Resources
  - Adjust CPU/Memory allocation

### Testing

- [ ] **Test full system restart**
  ```powershell
  # Stop everything
  .\stop_podman_services.ps1
  
  # Start everything
  .\start_podman_ctrader.ps1
  
  # Verify
  .\check_podman_status.ps1
  ```

- [ ] **Test after Windows reboot**
  - Restart your computer
  - Verify containers auto-start (they should with `restart: unless-stopped`)

- [ ] **Test Redis data persistence**
  ```powershell
  # Add test data
  podman exec asc-redis redis-cli SET test_key "test_value"
  
  # Restart Redis
  podman restart asc-redis
  
  # Verify data persists
  podman exec asc-redis redis-cli GET test_key
  # Should return: "test_value"
  ```

## Rollback Plan (If Needed)

If something goes wrong, you can easily roll back:

- [ ] **Stop Podman containers**
  ```powershell
  podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis
  ```

- [ ] **Start Docker containers**
  ```powershell
  cd C:\Users\HP\AndroidStudioProjects\MyRealApp
  docker-compose -f docker-compose.ctrader-both.yml up -d
  ```

- [ ] **Restore Redis backup** (if needed)
  ```powershell
  docker cp ./redis-backup.rdb asc-redis:/data/dump.rdb
  docker restart asc-redis
  ```

## Success Criteria

✅ **Migration is successful when:**

1. All Podman containers are running: `podman ps` shows 3 containers
2. Redis responds to ping: `podman exec asc-redis redis-cli ping` returns `PONG`
3. cTrader bridges respond: Both ports 8082 and 8083 are accessible
4. Your AI backend connects successfully to Redis
5. Your Android app works as before
6. Data persists across container restarts

## Troubleshooting

### Issue: Podman machine won't start
```powershell
podman machine stop
podman machine rm
podman machine init
podman machine start
```

### Issue: Containers won't start
```powershell
# Check logs
podman logs ctrader-bridge-live
podman logs asc-redis

# Try rebuilding
podman compose -f podman-compose.ctrader-both.yml up -d --build
```

### Issue: Can't connect to services
```powershell
# Check if ports are in use
netstat -ano | findstr :8082
netstat -ano | findstr :6379

# Make sure Docker isn't still running
docker ps
```

## Files Created

| File | Location | Purpose |
|------|----------|---------|
| `podman-compose.ctrader-both.yml` | MyRealApp | Main Podman Compose config |
| `podman-compose.yml` | NEW_ASC | Redis-only config |
| `start_podman_ctrader.ps1` | MyRealApp | Start all services |
| `start_podman_redis.ps1` | NEW_ASC | Start Redis only |
| `stop_podman_services.ps1` | MyRealApp | Stop all services |
| `check_podman_status.ps1` | MyRealApp | Status checker |
| `START_EVERYTHING_PODMAN.ps1` | NEW_ASC | Complete system startup |
| `PODMAN_MIGRATION_GUIDE.md` | MyRealApp | Full documentation |
| `PODMAN_QUICK_START.md` | MyRealApp | Quick reference |

## Next Steps After Migration

1. **Update your workflow**
   - Use `.\start_podman_ctrader.ps1` instead of Docker commands
   - Use `.\check_podman_status.ps1` to monitor services

2. **Monitor for a few days**
   - Watch for any issues
   - Check logs regularly: `podman logs -f <container>`

3. **Update team documentation**
   - If working with a team, update shared docs
   - Share the new Podman scripts

4. **Consider cleanup**
   - After confirming everything works, remove Docker
   - Free up disk space by removing old Docker images

## Support

- **Podman Documentation**: https://docs.podman.io/
- **Podman Desktop**: https://podman-desktop.io/docs
- **Community**: https://github.com/containers/podman/discussions

---

**Migration Date**: _________________

**Completed By**: _________________

**Notes**: _________________
