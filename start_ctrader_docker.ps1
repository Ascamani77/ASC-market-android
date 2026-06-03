# Start cTrader Bridge in Docker
# This runs the bridge as a Docker container with auto-restart

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader Bridge - Docker Setup" -ForegroundColor Cyan
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

# Check if containers already exist
Write-Host "Checking existing containers..." -ForegroundColor Yellow
$existingBridge = docker ps -a --filter "name=ctrader-bridge" --format "{{.Names}}" 2>$null
$existingRedis = docker ps -a --filter "name=asc-redis" --format "{{.Names}}" 2>$null

if ($existingBridge -eq "ctrader-bridge" -or $existingRedis -eq "asc-redis") {
    Write-Host "Found existing containers" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Cyan
    Write-Host "  1. Restart existing containers (fast)" -ForegroundColor White
    Write-Host "  2. Rebuild from scratch (slow, use if code changed)" -ForegroundColor White
    Write-Host "  3. Cancel" -ForegroundColor White
    Write-Host ""
    $choice = Read-Host "Choose option (1-3)"
    
    switch ($choice) {
        "1" {
            Write-Host ""
            Write-Host "Restarting existing containers..." -ForegroundColor Cyan
            docker-compose -f docker-compose.ctrader.yml up -d
        }
        "2" {
            Write-Host ""
            Write-Host "Rebuilding containers..." -ForegroundColor Cyan
            docker-compose -f docker-compose.ctrader.yml down
            docker-compose -f docker-compose.ctrader.yml build --no-cache
            docker-compose -f docker-compose.ctrader.yml up -d
        }
        "3" {
            Write-Host "Cancelled" -ForegroundColor Yellow
            exit 0
        }
        default {
            Write-Host "Invalid choice, using option 1" -ForegroundColor Yellow
            docker-compose -f docker-compose.ctrader.yml up -d
        }
    }
} else {
    Write-Host "Building and starting containers..." -ForegroundColor Cyan
    Write-Host "This will take 1-2 minutes on first run..." -ForegroundColor Yellow
    Write-Host ""
    docker-compose -f docker-compose.ctrader.yml up -d --build
}

Write-Host ""
Write-Host "Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check Redis
Write-Host ""
Write-Host "Checking Redis..." -ForegroundColor Yellow
$redisPing = docker exec asc-redis redis-cli ping 2>$null
if ($redisPing -eq "PONG") {
    Write-Host "✅ Redis is ready" -ForegroundColor Green
} else {
    Write-Host "⚠️  Redis not responding" -ForegroundColor Yellow
}

# Check cTrader Bridge
Write-Host ""
Write-Host "Checking cTrader Bridge..." -ForegroundColor Yellow
$bridgeStatus = docker ps --filter "name=ctrader-bridge" --format "{{.Status}}" 2>$null
if ($bridgeStatus -like "*Up*") {
    Write-Host "✅ cTrader Bridge is running" -ForegroundColor Green
} else {
    Write-Host "⚠️  cTrader Bridge not running" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  DOCKER CONTAINERS STARTED" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Services:" -ForegroundColor White
Write-Host "  • Redis:         localhost:6379" -ForegroundColor Gray
Write-Host "  • cTrader:       localhost:8082" -ForegroundColor Gray
Write-Host ""
Write-Host "View logs:" -ForegroundColor Cyan
Write-Host "  docker logs -f ctrader-bridge" -ForegroundColor Gray
Write-Host "  docker logs -f asc-redis" -ForegroundColor Gray
Write-Host ""
Write-Host "Check status:" -ForegroundColor Cyan
Write-Host "  docker ps" -ForegroundColor Gray
Write-Host ""
Write-Host "Stop containers:" -ForegroundColor Cyan
Write-Host "  docker-compose -f docker-compose.ctrader.yml down" -ForegroundColor Gray
Write-Host ""
Write-Host "Restart if frozen:" -ForegroundColor Cyan
Write-Host "  docker restart ctrader-bridge" -ForegroundColor Gray
Write-Host ""
Write-Host "Benefits of Docker:" -ForegroundColor Yellow
Write-Host "  ✅ Auto-restarts if crashes" -ForegroundColor Green
Write-Host "  ✅ Runs in background" -ForegroundColor Green
Write-Host "  ✅ Easy to monitor with 'docker logs'" -ForegroundColor Green
Write-Host "  ✅ Starts automatically with Docker Desktop" -ForegroundColor Green
Write-Host ""

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
