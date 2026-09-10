# Simple Start Guide - Use Docker (No Podman Needed)

## ✅ What We Decided

**Forget Podman** - it's taking too long to download.  
**Use Docker** - it already works on your system!

---

## 🚀 How to Start Your Services

### Step 1: Make Sure Docker Desktop is Running

Look for **Docker Desktop** in your system tray (bottom right).  
If you don't see it:
1. Press `Win` key
2. Type "Docker Desktop"
3. Click to open it
4. Wait until you see "Docker Desktop is running" (green icon)

### Step 2: Start Your Containers

Open PowerShell and run:

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
docker-compose -f docker-compose.ctrader.yml up -d
```

This starts:
- ✅ cTrader Live Bridge (port 8082)
- ✅ Redis (port 6379)

### Step 3: Check Status

```powershell
docker ps
```

You should see:
- `ctrader-bridge` - Running
- `asc-redis` - Running

---

## 📊 View Your Containers

### Option 1: Docker Desktop UI (Recommended)
1. Open **Docker Desktop**
2. Click **"Containers"** tab on the left
3. See all your containers with nice UI
4. Click any container to:
   - View logs
   - See stats
   - Open terminal
   - Stop/Start

### Option 2: Command Line
```powershell
# View logs
docker logs -f ctrader-bridge
docker logs -f asc-redis

# Stop containers
docker stop ctrader-bridge asc-redis

# Start containers
docker start ctrader-bridge asc-redis

# Restart
docker restart ctrader-bridge asc-redis
```

---

## 🔧 Service Endpoints

| Service | Port | URL |
|---------|------|-----|
| **cTrader Live** | 8082 | http://localhost:8082 |
| **Redis** | 6379 | localhost:6379 |

---

## 🛑 Stop Everything

```powershell
cd C:\Users\HP\AndroidStudioProjects\MyRealApp
docker-compose -f docker-compose.ctrader.yml down
```

---

## 💡 That's It!

No Podman needed. No long downloads. Just Docker working as it should.

Your credentials are already configured in `docker-compose.ctrader.yml`:
- Client ID: `YOUR_CTRADER_CLIENT_ID`
- Account ID: `47341092`

---

## 🆘 If Docker Desktop Won't Start

1. **Restart your computer** - Sometimes Docker needs a fresh start
2. **Check Windows Updates** - Docker needs WSL2 to be up to date
3. **Reinstall Docker Desktop** - Download from docker.com

---

## ✅ Summary

1. Open Docker Desktop (wait for green icon)
2. Run: `docker-compose -f docker-compose.ctrader.yml up -d`
3. View in Docker Desktop UI
4. Done! 🎉
