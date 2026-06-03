# Podman Commands Cheatsheet

Quick reference for common Podman operations.

## 🚀 Quick Start

```powershell
# Start everything
.\start_podman_ctrader.ps1

# Check status
.\check_podman_status.ps1

# Stop everything
.\stop_podman_services.ps1
```

---

## 📦 Container Management

### List Containers
```powershell
# Running containers
podman ps

# All containers (including stopped)
podman ps -a

# With custom format
podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Start/Stop/Restart
```powershell
# Start
podman start ctrader-bridge-live
podman start asc-redis

# Stop
podman stop ctrader-bridge-live
podman stop asc-redis

# Restart
podman restart ctrader-bridge-live
podman restart asc-redis

# Stop all
podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis
```

### Remove Containers
```powershell
# Remove stopped container
podman rm ctrader-bridge-live

# Force remove running container
podman rm -f ctrader-bridge-live

# Remove all stopped containers
podman container prune
```

---

## 📊 Logs and Monitoring

### View Logs
```powershell
# Follow logs in real-time
podman logs -f ctrader-bridge-live

# Last 50 lines
podman logs --tail 50 ctrader-bridge-live

# Last 100 lines with timestamps
podman logs --tail 100 --timestamps ctrader-bridge-live

# Since specific time
podman logs --since 10m ctrader-bridge-live  # Last 10 minutes
podman logs --since 2024-01-01 ctrader-bridge-live
```

### Resource Usage
```powershell
# Real-time stats
podman stats

# One-time snapshot
podman stats --no-stream

# Specific container
podman stats ctrader-bridge-live
```

### Inspect Container
```powershell
# Full details
podman inspect ctrader-bridge-live

# Specific field (e.g., IP address)
podman inspect -f '{{.NetworkSettings.IPAddress}}' ctrader-bridge-live

# Container state
podman inspect -f '{{.State.Status}}' ctrader-bridge-live
```

---

## 🔧 Podman Compose

### Start Services
```powershell
# Start all services
podman compose -f podman-compose.ctrader-both.yml up -d

# Start specific service
podman compose -f podman-compose.ctrader-both.yml up -d redis

# Start with rebuild
podman compose -f podman-compose.ctrader-both.yml up -d --build
```

### Stop Services
```powershell
# Stop all services
podman compose -f podman-compose.ctrader-both.yml down

# Stop and remove volumes
podman compose -f podman-compose.ctrader-both.yml down -v
```

### View Service Status
```powershell
# List services
podman compose -f podman-compose.ctrader-both.yml ps

# View logs
podman compose -f podman-compose.ctrader-both.yml logs -f
```

---

## 🖼️ Image Management

### List Images
```powershell
# All images
podman images

# With digests
podman images --digests
```

### Build Images
```powershell
# Build from Dockerfile
podman build -t ctrader-bridge -f Dockerfile.ctrader .

# Build with no cache
podman build --no-cache -t ctrader-bridge -f Dockerfile.ctrader .
```

### Remove Images
```powershell
# Remove specific image
podman rmi ctrader-bridge

# Remove unused images
podman image prune

# Remove all unused images
podman image prune -a
```

---

## 💾 Volume Management

### List Volumes
```powershell
# All volumes
podman volume ls

# With filters
podman volume ls --filter name=redis
```

### Inspect Volume
```powershell
# Volume details
podman volume inspect redis-data
```

### Backup Volume
```powershell
# Export volume
podman volume export redis-data > redis-backup.tar

# Import volume
podman volume import redis-data < redis-backup.tar
```

### Remove Volumes
```powershell
# Remove specific volume
podman volume rm redis-data

# Remove unused volumes
podman volume prune
```

---

## 🔌 Network Management

### List Networks
```powershell
# All networks
podman network ls
```

### Inspect Network
```powershell
# Network details
podman network inspect trading-network
```

### Remove Networks
```powershell
# Remove specific network
podman network rm trading-network

# Remove unused networks
podman network prune
```

---

## 🐚 Execute Commands in Containers

### Interactive Shell
```powershell
# Bash shell
podman exec -it ctrader-bridge-live bash

# Python shell
podman exec -it ctrader-bridge-live python

# Redis CLI
podman exec -it asc-redis redis-cli
```

### Run Single Command
```powershell
# Redis ping
podman exec asc-redis redis-cli ping

# Check Python version
podman exec ctrader-bridge-live python --version

# List files
podman exec ctrader-bridge-live ls -la
```

---

## 📁 File Operations

### Copy Files
```powershell
# From container to host
podman cp ctrader-bridge-live:/app/logs/error.log ./error.log

# From host to container
podman cp ./config.json ctrader-bridge-live:/app/config.json
```

---

## 🔍 Troubleshooting

### Health Checks
```powershell
# Check container health
podman inspect --format='{{.State.Health.Status}}' ctrader-bridge-live

# View health check logs
podman inspect --format='{{json .State.Health}}' ctrader-bridge-live
```

### Port Mapping
```powershell
# List port mappings
podman port ctrader-bridge-live

# Check specific port
podman port ctrader-bridge-live 8082
```

### Process List
```powershell
# Processes in container
podman top ctrader-bridge-live

# With custom format
podman top ctrader-bridge-live user pid ppid
```

---

## 🧹 Cleanup

### Remove Everything
```powershell
# Stop all containers
podman stop -a

# Remove all containers
podman rm -a

# Remove all images
podman rmi -a

# Remove all volumes
podman volume rm -a

# Remove all networks
podman network rm -a
```

### System Prune
```powershell
# Remove unused data
podman system prune

# Remove all unused data (including volumes)
podman system prune -a --volumes
```

### Disk Usage
```powershell
# Show disk usage
podman system df

# Detailed view
podman system df -v
```

---

## 🔄 Podman Machine (Windows/Mac)

### Machine Management
```powershell
# List machines
podman machine list

# Start machine
podman machine start

# Stop machine
podman machine stop

# Restart machine
podman machine restart

# SSH into machine
podman machine ssh
```

### Machine Info
```powershell
# Machine details
podman machine inspect

# Machine status
podman machine info
```

---

## 🎯 Service-Specific Commands

### Redis
```powershell
# Ping Redis
podman exec asc-redis redis-cli ping

# Get all keys
podman exec asc-redis redis-cli KEYS '*'

# Get specific key
podman exec asc-redis redis-cli GET mykey

# Set key
podman exec asc-redis redis-cli SET mykey "myvalue"

# Monitor commands
podman exec asc-redis redis-cli MONITOR

# Get info
podman exec asc-redis redis-cli INFO

# Save data
podman exec asc-redis redis-cli SAVE
```

### cTrader Bridges
```powershell
# Check health endpoint
curl http://localhost:8082/health  # Live
curl http://localhost:8083/health  # Demo

# View Python logs
podman logs -f ctrader-bridge-live

# Restart bridge
podman restart ctrader-bridge-live
```

---

## 📝 Useful Aliases (Optional)

Add to your PowerShell profile (`$PROFILE`):

```powershell
# Podman shortcuts
function pd { podman ps }
function pda { podman ps -a }
function pl { podman logs -f $args }
function pe { podman exec -it $args }
function pst { podman stats --no-stream }

# Service shortcuts
function redis-cli { podman exec -it asc-redis redis-cli $args }
function redis-logs { podman logs -f asc-redis }
function ctrader-logs { podman logs -f ctrader-bridge-live }
```

---

## 🆘 Emergency Commands

### Container Not Responding
```powershell
# Force kill
podman kill ctrader-bridge-live

# Remove and recreate
podman rm -f ctrader-bridge-live
.\start_podman_ctrader.ps1
```

### Reset Everything
```powershell
# Nuclear option - removes everything
podman system reset

# Then recreate
.\start_podman_ctrader.ps1
```

### Check What's Using a Port
```powershell
# Windows
netstat -ano | findstr :8082

# Kill process by PID
taskkill /PID <pid> /F
```

---

## 📚 Additional Resources

- **Podman Docs**: https://docs.podman.io/
- **Podman Desktop**: https://podman-desktop.io/
- **Compose Spec**: https://compose-spec.io/

---

## 💡 Tips

1. **Use `-f` for follow**: `podman logs -f` to stream logs
2. **Use `--tail`**: Limit log output with `--tail 50`
3. **Use `--format`**: Customize output with `--format`
4. **Use tab completion**: Podman supports shell completion
5. **Check health**: Use `podman inspect` to check container health
6. **Monitor resources**: Use `podman stats` to watch resource usage
7. **Prune regularly**: Clean up unused resources with `prune` commands
