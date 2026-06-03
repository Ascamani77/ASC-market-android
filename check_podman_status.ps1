#!/usr/bin/env pwsh
# Check status of all Podman containers for cTrader and Redis

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Podman Services Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Podman is installed
try {
    $podmanVersion = podman --version
    Write-Host "✅ Podman: $podmanVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Podman is not installed!" -ForegroundColor Red
    Write-Host "   Install from: https://podman-desktop.io/downloads" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Container Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$containers = @(
    @{Name="ctrader-bridge-live"; Port=8082; Type="cTrader Live"},
    @{Name="ctrader-bridge-demo"; Port=8083; Type="cTrader Demo"},
    @{Name="asc-redis"; Port=6379; Type="Redis"}
)

$allHealthy = $true

foreach ($container in $containers) {
    $name = $container.Name
    $port = $container.Port
    $type = $container.Type
    
    # Check if container exists
    $exists = podman ps -a --filter "name=$name" --format "{{.Names}}" 2>$null
    
    if ($exists -eq $name) {
        # Check if running
        $isRunning = podman ps --filter "name=$name" --format "{{.Names}}" 2>$null
        
        if ($isRunning -eq $name) {
            $status = podman ps --filter "name=$name" --format "{{.Status}}" 2>$null
            Write-Host "✅ $type ($name)" -ForegroundColor Green
            Write-Host "   Status: $status" -ForegroundColor Gray
            Write-Host "   Port:   $port" -ForegroundColor Gray
        } else {
            Write-Host "⚠️  $type ($name)" -ForegroundColor Yellow
            Write-Host "   Status: Stopped" -ForegroundColor Gray
            Write-Host "   Port:   $port" -ForegroundColor Gray
            $allHealthy = $false
        }
    } else {
        Write-Host "❌ $type ($name)" -ForegroundColor Red
        Write-Host "   Status: Not created" -ForegroundColor Gray
        Write-Host "   Port:   $port" -ForegroundColor Gray
        $allHealthy = $false
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Service Health Checks" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test Redis
$redisRunning = podman ps --filter "name=asc-redis" --format "{{.Names}}" 2>$null
if ($redisRunning -eq "asc-redis") {
    try {
        $pingResult = podman exec asc-redis redis-cli ping 2>$null
        if ($pingResult -eq "PONG") {
            Write-Host "✅ Redis: Responding (PONG)" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Redis: Not responding" -ForegroundColor Yellow
            $allHealthy = $false
        }
    } catch {
        Write-Host "⚠️  Redis: Cannot test connection" -ForegroundColor Yellow
        $allHealthy = $false
    }
} else {
    Write-Host "❌ Redis: Not running" -ForegroundColor Red
    $allHealthy = $false
}

# Test cTrader Live Bridge
$liveRunning = podman ps --filter "name=ctrader-bridge-live" --format "{{.Names}}" 2>$null
if ($liveRunning -eq "ctrader-bridge-live") {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8082/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ cTrader Live: Responding (HTTP 200)" -ForegroundColor Green
        } else {
            Write-Host "⚠️  cTrader Live: Unexpected response" -ForegroundColor Yellow
            $allHealthy = $false
        }
    } catch {
        Write-Host "⚠️  cTrader Live: Not responding on port 8082" -ForegroundColor Yellow
        $allHealthy = $false
    }
} else {
    Write-Host "❌ cTrader Live: Not running" -ForegroundColor Red
    $allHealthy = $false
}

# Test cTrader Demo Bridge
$demoRunning = podman ps --filter "name=ctrader-bridge-demo" --format "{{.Names}}" 2>$null
if ($demoRunning -eq "ctrader-bridge-demo") {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8083/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ cTrader Demo: Responding (HTTP 200)" -ForegroundColor Green
        } else {
            Write-Host "⚠️  cTrader Demo: Unexpected response" -ForegroundColor Yellow
            $allHealthy = $false
        }
    } catch {
        Write-Host "⚠️  cTrader Demo: Not responding on port 8083" -ForegroundColor Yellow
        $allHealthy = $false
    }
} else {
    Write-Host "❌ cTrader Demo: Not running" -ForegroundColor Red
    $allHealthy = $false
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Resource Usage" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

try {
    $stats = podman stats --no-stream --format "{{.Name}}: CPU {{.CPUPerc}} | MEM {{.MemUsage}}" 2>$null
    if ($stats) {
        $stats | ForEach-Object {
            Write-Host "  $_" -ForegroundColor Gray
        }
    } else {
        Write-Host "  No running containers" -ForegroundColor Gray
    }
} catch {
    Write-Host "  Unable to fetch stats" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($allHealthy) {
    Write-Host "✅ All services are healthy and running!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Some services need attention" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To start all services:" -ForegroundColor White
    Write-Host "  .\start_podman_ctrader.ps1" -ForegroundColor Gray
    Write-Host ""
    Write-Host "To view logs:" -ForegroundColor White
    Write-Host "  podman logs -f ctrader-bridge-live" -ForegroundColor Gray
    Write-Host "  podman logs -f ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host "  podman logs -f asc-redis" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Quick Commands" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Start all:    .\start_podman_ctrader.ps1" -ForegroundColor Gray
Write-Host "  Stop all:     podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis" -ForegroundColor Gray
Write-Host "  Restart all:  podman restart ctrader-bridge-live ctrader-bridge-demo asc-redis" -ForegroundColor Gray
Write-Host "  View logs:    podman logs -f <container-name>" -ForegroundColor Gray
Write-Host ""
