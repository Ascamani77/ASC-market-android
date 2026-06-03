# Start cTrader bridges and Redis with Docker

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting Services with Docker" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker..." -ForegroundColor Yellow
$dockerRunning = $false
$maxAttempts = 30
$attempt = 0

while (-not $dockerRunning -and $attempt -lt $maxAttempts) {
    try {
        docker ps 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $dockerRunning = $true
            Write-Host "OK Docker is running" -ForegroundColor Green
        }
    } catch {
        # Docker not ready yet
    }
    
    if (-not $dockerRunning) {
        $attempt++
        if ($attempt -eq 1) {
            Write-Host "Docker not running. Starting Docker Desktop..." -ForegroundColor Yellow
            Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
        }
        Write-Host "  Waiting for Docker to start... ($attempt/$maxAttempts)" -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}

if (-not $dockerRunning) {
    Write-Host "ERROR Docker failed to start after 60 seconds" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please:" -ForegroundColor Yellow
    Write-Host "1. Start Docker Desktop manually" -ForegroundColor White
    Write-Host "2. Wait for it to show 'Docker Desktop is running'" -ForegroundColor White
    Write-Host "3. Run this script again" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "Starting containers..." -ForegroundColor Yellow

# Navigate to project directory
Set-Location "C:\Users\HP\AndroidStudioProjects\MyRealApp"

# Start services with Docker Compose
docker-compose -f docker-compose.ctrader-both.yml up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR Failed to start services!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Waiting for services to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check container status
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Container Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$containers = @("ctrader-bridge-live", "ctrader-bridge-demo", "asc-redis")
$allRunning = $true

foreach ($container in $containers) {
    $status = docker ps --filter "name=$container" --format "{{.Status}}" 2>$null
    if ($status) {
        Write-Host "OK $container : $status" -ForegroundColor Green
    } else {
        Write-Host "ERROR $container : NOT RUNNING" -ForegroundColor Red
        $allRunning = $false
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Service Endpoints" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Live Bridge:  http://localhost:8082" -ForegroundColor White
Write-Host "  Demo Bridge:  http://localhost:8083" -ForegroundColor White
Write-Host "  Redis:        localhost:6379" -ForegroundColor White
Write-Host ""

if ($allRunning) {
    Write-Host "OK All services started successfully!" -ForegroundColor Green
} else {
    Write-Host "WARNING Some services failed to start. Check logs:" -ForegroundColor Yellow
    Write-Host "  docker logs ctrader-bridge-live" -ForegroundColor Gray
    Write-Host "  docker logs ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host "  docker logs asc-redis" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  View in UI" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Docker Desktop: Open Docker Desktop > Containers tab" -ForegroundColor White
Write-Host "  Podman Desktop: May show Docker containers if configured" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Useful Commands" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  View logs:    docker logs -f CONTAINER" -ForegroundColor Gray
Write-Host "  Stop all:     docker-compose -f docker-compose.ctrader-both.yml down" -ForegroundColor Gray
Write-Host "  Restart:      docker restart CONTAINER" -ForegroundColor Gray
Write-Host ""
