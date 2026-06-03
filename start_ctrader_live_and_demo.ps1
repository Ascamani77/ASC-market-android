# Start Both cTrader Bridges (Live and Demo)
# This script starts two separate cTrader bridges:
# - Live bridge on port 8082
# - Demo bridge on port 8083

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader Bridges - Live & Demo" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker
Write-Host "Checking Docker Desktop..." -ForegroundColor Yellow
try {
    docker version 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Docker is not running" -ForegroundColor Red
        Write-Host "   Please start Docker Desktop" -ForegroundColor Yellow
        pause
        exit 1
    }
    Write-Host "✅ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker not found" -ForegroundColor Red
    pause
    exit 1
}
Write-Host ""

# LIVE CREDENTIALS
Write-Host "LIVE Bridge Configuration:" -ForegroundColor Cyan
$LIVE_CLIENT_ID = Read-Host "Enter LIVE Client ID (or press Enter to skip)"
if ($LIVE_CLIENT_ID) {
    $LIVE_CLIENT_SECRET = Read-Host "Enter LIVE Client Secret"
    $LIVE_ACCESS_TOKEN = Read-Host "Enter LIVE Access Token"
    $LIVE_ACCOUNT_ID = Read-Host "Enter LIVE Account ID"
    
    Write-Host ""
    Write-Host "Starting LIVE bridge on port 8082..." -ForegroundColor Cyan
    
    # Stop and remove if exists
    docker stop ctrader-bridge-live 2>$null
    docker rm ctrader-bridge-live 2>$null
    
    # Start live bridge
    docker run -d --name ctrader-bridge-live --restart unless-stopped -p 8082:8082 `
      -e CTRADER_CLIENT_ID="$LIVE_CLIENT_ID" `
      -e CTRADER_CLIENT_SECRET="$LIVE_CLIENT_SECRET" `
      -e CTRADER_ACCESS_TOKEN="$LIVE_ACCESS_TOKEN" `
      -e CTRADER_ACCOUNT_ID="$LIVE_ACCOUNT_ID" `
      -e CTRADER_HOST_TYPE="live" `
      -e CTRADER_BRIDGE_HOST="0.0.0.0" `
      -e CTRADER_BRIDGE_PORT="8082" `
      myrealapp-ctrader-bridge:latest
    
    Write-Host "✅ LIVE bridge started" -ForegroundColor Green
} else {
    Write-Host "⏭️  Skipping LIVE bridge" -ForegroundColor Yellow
}

Write-Host ""

# DEMO CREDENTIALS
Write-Host "DEMO Bridge Configuration:" -ForegroundColor Cyan
$DEMO_CLIENT_ID = Read-Host "Enter DEMO Client ID (or press Enter to skip)"
if ($DEMO_CLIENT_ID) {
    $DEMO_CLIENT_SECRET = Read-Host "Enter DEMO Client Secret"
    $DEMO_ACCESS_TOKEN = Read-Host "Enter DEMO Access Token"
    $DEMO_ACCOUNT_ID = Read-Host "Enter DEMO Account ID"
    
    Write-Host ""
    Write-Host "Starting DEMO bridge on port 8083..." -ForegroundColor Cyan
    
    # Stop and remove if exists
    docker stop ctrader-bridge-demo 2>$null
    docker rm ctrader-bridge-demo 2>$null
    
    # Start demo bridge
    docker run -d --name ctrader-bridge-demo --restart unless-stopped -p 8083:8083 `
      -e CTRADER_CLIENT_ID="$DEMO_CLIENT_ID" `
      -e CTRADER_CLIENT_SECRET="$DEMO_CLIENT_SECRET" `
      -e CTRADER_ACCESS_TOKEN="$DEMO_ACCESS_TOKEN" `
      -e CTRADER_ACCOUNT_ID="$DEMO_ACCOUNT_ID" `
      -e CTRADER_HOST_TYPE="demo" `
      -e CTRADER_BRIDGE_HOST="0.0.0.0" `
      -e CTRADER_BRIDGE_PORT="8083" `
      myrealapp-ctrader-bridge:latest
    
    Write-Host "✅ DEMO bridge started" -ForegroundColor Green
} else {
    Write-Host "⏭️  Skipping DEMO bridge" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Waiting for bridges to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  BRIDGES STARTED" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Check status
$containers = docker ps --filter "name=ctrader-bridge" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
Write-Host $containers
Write-Host ""

Write-Host "View logs:" -ForegroundColor Cyan
Write-Host "  docker logs -f ctrader-bridge-live" -ForegroundColor Gray
Write-Host "  docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
Write-Host ""

Write-Host "Stop bridges:" -ForegroundColor Cyan
Write-Host "  docker stop ctrader-bridge-live ctrader-bridge-demo" -ForegroundColor Gray
Write-Host ""

Write-Host "Restart if frozen:" -ForegroundColor Cyan
Write-Host "  docker restart ctrader-bridge-live" -ForegroundColor Gray
Write-Host "  docker restart ctrader-bridge-demo" -ForegroundColor Gray
Write-Host ""

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
