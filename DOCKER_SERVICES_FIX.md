# Docker Services Fix Guide

## Issues Found

### 1. ❌ cTrader Bridges - Authentication Timeout
**Status:** Both live and demo bridges are failing
**Error:** `ACCESS_TOKEN is invalid or expired`
**Impact:** No Pepperstone live/demo chart data

### 2. ❌ AI Backend Not Running
**Status:** No AI backend container found
**Impact:** AI showing 0.0, no AI predictions

### 3. ⚠️ Network Configuration Mismatch
**Status:** App configured for `10.164.138.133`, Docker on `localhost`
**Impact:** Potential connectivity issues

---

## Quick Fix Steps

### Step 1: Regenerate cTrader Access Tokens

#### For Live Account (Port 8082):
1. Go to: https://openapi.ctrader.com/apps
2. Login with your Pepperstone credentials
3. Find your app and generate a new **ACCESS_TOKEN**
4. Copy the token

#### For Demo Account (Port 8083):
1. Go to: https://openapi.ctrader.com/apps
2. Switch to Demo environment
3. Generate a new **ACCESS_TOKEN** for demo
4. Copy the token

### Step 2: Update Docker Environment Files

#### Update `.env` file for live bridge:
```bash
# Edit the .env file in your project root
ACCESS_TOKEN=your_new_live_token_here
ACCOUNT_ID=47312778
CLIENT_ID=your_client_id
CLIENT_SECRET=your_client_secret
```

#### Update `.env.demo` file for demo bridge:
```bash
# Edit the .env.demo file
ACCESS_TOKEN=your_new_demo_token_here
ACCOUNT_ID=your_demo_account_id
CLIENT_ID=your_demo_client_id
CLIENT_SECRET=your_demo_client_secret
```

### Step 3: Restart cTrader Bridges

```powershell
# Stop the containers
docker stop ctrader-bridge ctrader-bridge-demo

# Remove the containers
docker rm ctrader-bridge ctrader-bridge-demo

# Rebuild and start (from project root)
docker-compose up -d ctrader-bridge
docker-compose up -d ctrader-bridge-demo
```

### Step 4: Start AI Backend

Check if you have a docker-compose file with AI backend service:

```powershell
# Look for AI backend service in docker-compose.yml
docker-compose ps

# If AI backend service exists, start it:
docker-compose up -d ai-backend

# Or if it's a separate container:
docker-compose up -d backend
```

### Step 5: Verify Services

```powershell
# Check all containers are running
docker ps

# Check cTrader live bridge logs
docker logs ctrader-bridge --tail 50

# Check cTrader demo bridge logs
docker logs ctrader-bridge-demo --tail 50

# Check AI backend logs (if running)
docker logs ai-backend --tail 50
```

---

## Alternative: Use Binance Instead

If you can't fix cTrader tokens immediately, switch to Binance which doesn't require Docker:

1. Open app Settings
2. Go to "Chart Feed Type"
3. Select "Binance" or "Binance Connect"
4. Binance Connect works without any credentials (view-only)

---

## Network Configuration Fix

If services are still not connecting, update the network config:

### Option A: Use localhost (Recommended for Docker Desktop)

In your app's Settings screen:
- Backend URL: `http://localhost:8000`
- cTrader Host: `localhost`
- cTrader Port: `8082`
- cTrader Demo Host: `localhost`
- cTrader Demo Port: `8083`

### Option B: Use Docker Host IP

Find your Docker host IP:
```powershell
ipconfig | findstr "IPv4"
```

Then update settings to use that IP instead of `10.164.138.133`.

---

## Verification Checklist

- [ ] cTrader live bridge shows "Connected" in logs
- [ ] cTrader demo bridge shows "Connected" in logs
- [ ] Redis is healthy (already ✅)
- [ ] AI backend is running and accessible
- [ ] App can connect to localhost:8082
- [ ] App can connect to localhost:8083
- [ ] Chart data is loading
- [ ] AI predictions are showing (not 0.0)

---

## Common Issues

### "Cannot connect to Docker daemon"
- Start Docker Desktop
- Wait for it to fully initialize

### "Port already in use"
- Check what's using the port: `netstat -ano | findstr :8082`
- Kill the process or use a different port

### "Container exits immediately"
- Check logs: `docker logs container-name`
- Usually means environment variables are missing

### "AI still showing 0.0"
- Check if AI backend is actually running
- Verify backend URL in app settings
- Check backend logs for errors
- Ensure Redis is accessible to backend

---

## Need Help?

1. Check container logs: `docker logs <container-name>`
2. Check container status: `docker ps -a`
3. Restart all services: `docker-compose restart`
4. Full reset: `docker-compose down && docker-compose up -d`
