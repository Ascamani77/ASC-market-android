#!/usr/bin/env pwsh
# Start cTrader bridges (Live + Demo) and Redis using Podman

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader Bridges + Redis (Podman)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Podman is installed
try {
    $podmanVersion = podman --version
    Write-Host "✅ Podman detected: $podmanVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Podman is not installed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Install Podman Desktop from: https://podman-desktop.io/downloads" -ForegroundColor Yellow
    Write-Host "Or use winget: winget install RedHat.Podman-Desktop" -ForegroundColor Yellow
    exit 1
}

# Check if podman-compose is available
$usePodmanCompose = $false
try {
    $composeVersion = podman-compose --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ podman-compose detected: $composeVersion" -ForegroundColor Green
        $usePodmanCompose = $true
    }
} catch {
    Write-Host "⚠️  podman-compose not found, using 'podman compose' instead" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Starting services..." -ForegroundColor Cyan

# Navigate to project directory
Set-Location "C:\Users\HP\AndroidStudioProjects\MyRealApp"

# Stop any existing containers
Write-Host "Stopping existing containers..." -ForegroundColor Yellow
podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis 2>$null | Out-Null

# Start services using Podman Compose
if ($usePodmanCompose) {
    Write-Host "Using podman-compose..." -ForegroundColor Gray
    podman-compose -f podman-compose.ctrader-both.yml up -d --build
} else {
    Write-Host "Using podman compose..." -ForegroundColor Gray
    podman compose -f podman-compose.ctrader-both.yml up -d --build
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start services!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Waiting for services to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 8

# Check container status
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Container Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$containers = @("ctrader-bridge-live", "ctrader-bridge-demo", "asc-redis")
$allRunning = $true

foreach ($container in $containers) {
    $status = podman ps --filter "name=$container" --format "{{.Status}}" 2>$null
    if ($status) {
        Write-Host "✅ $container : $status" -ForegroundColor Green
    } else {
        Write-Host "❌ $container : NOT RUNNING" -ForegroundColor Red
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
    Write-Host "✅ All services started successfully!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Some services failed to start. Check logs:" -ForegroundColor Yellow
    Write-Host "  podman logs ctrader-bridge-live" -ForegroundColor Gray
    Write-Host "  podman logs ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host "  podman logs asc-redis" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Useful Commands" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  View logs:    podman logs -f <container-name>" -ForegroundColor Gray
Write-Host "  Stop all:     podman stop ctrader-bridge-live ctrader-bridge-demo asc-redis" -ForegroundColor Gray
Write-Host "  Restart:      podman restart <container-name>" -ForegroundColor Gray
Write-Host "  Remove all:   podman rm -f ctrader-bridge-live ctrader-bridge-demo asc-redis" -ForegroundColor Gray
Write-Host ""
