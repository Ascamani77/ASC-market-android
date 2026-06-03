#!/usr/bin/env pwsh
# Stop all Podman services (cTrader bridges and Redis)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Stopping Podman Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Podman is installed
try {
    podman --version | Out-Null
} catch {
    Write-Host "❌ Podman is not installed!" -ForegroundColor Red
    exit 1
}

$containers = @("ctrader-bridge-live", "ctrader-bridge-demo", "asc-redis")

Write-Host "Stopping containers..." -ForegroundColor Yellow
Write-Host ""

foreach ($container in $containers) {
    $exists = podman ps -a --filter "name=$container" --format "{{.Names}}" 2>$null
    
    if ($exists -eq $container) {
        $isRunning = podman ps --filter "name=$container" --format "{{.Names}}" 2>$null
        
        if ($isRunning -eq $container) {
            Write-Host "  Stopping $container..." -ForegroundColor Gray
            podman stop $container 2>$null | Out-Null
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  ✅ $container stopped" -ForegroundColor Green
            } else {
                Write-Host "  ⚠️  Failed to stop $container" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  ⚠️  $container already stopped" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ⚠️  $container does not exist" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$runningContainers = podman ps --format "{{.Names}}" 2>$null

if ($runningContainers) {
    Write-Host "Still running:" -ForegroundColor Yellow
    $runningContainers | ForEach-Object {
        Write-Host "  - $_" -ForegroundColor Gray
    }
} else {
    Write-Host "✅ All containers stopped" -ForegroundColor Green
}

Write-Host ""
Write-Host "To start services again:" -ForegroundColor White
Write-Host "  .\start_podman_ctrader.ps1" -ForegroundColor Gray
Write-Host ""
