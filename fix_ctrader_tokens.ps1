# cTrader Token Update Script
# This script helps you update expired cTrader access tokens

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "cTrader Token Update Helper" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
$dockerRunning = docker ps 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Docker is running" -ForegroundColor Green
Write-Host ""

# Show current container status
Write-Host "Current cTrader bridge status:" -ForegroundColor Yellow
docker ps --filter "name=ctrader-bridge" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
Write-Host ""

# Check logs for errors
Write-Host "Checking for authentication errors..." -ForegroundColor Yellow
$liveErrors = docker logs ctrader-bridge 2>&1 | Select-String "ACCESS_TOKEN" | Select-Object -Last 3
$demoErrors = docker logs ctrader-bridge-demo 2>&1 | Select-String "ACCESS_TOKEN" | Select-Object -Last 3

if ($liveErrors) {
    Write-Host "❌ Live bridge has authentication errors:" -ForegroundColor Red
    $liveErrors | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
}

if ($demoErrors) {
    Write-Host "❌ Demo bridge has authentication errors:" -ForegroundColor Red
    $demoErrors | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
}
Write-Host ""

# Instructions
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "How to Fix:" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Get New Access Tokens:" -ForegroundColor Yellow
Write-Host "   Live: https://openapi.ctrader.com/apps" -ForegroundColor White
Write-Host "   Demo: https://openapi.ctrader.com/apps (switch to Demo)" -ForegroundColor White
Write-Host ""
Write-Host "2. Update docker-compose.ctrader.yml with new tokens" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. Restart the bridges:" -ForegroundColor Yellow
Write-Host "   docker-compose -f docker-compose.ctrader.yml down" -ForegroundColor White
Write-Host "   docker-compose -f docker-compose.ctrader.yml up -d" -ForegroundColor White
Write-Host ""

# Offer to open the cTrader portal
Write-Host "Would you like to open the cTrader OpenAPI portal now? (Y/N)" -ForegroundColor Cyan
$response = Read-Host
if ($response -eq "Y" -or $response -eq "y") {
    Start-Process "https://openapi.ctrader.com/apps"
    Write-Host "✅ Opening cTrader portal in your browser..." -ForegroundColor Green
}
Write-Host ""

# Offer to stop containers
Write-Host "Would you like to stop the cTrader bridges now? (Y/N)" -ForegroundColor Cyan
$response = Read-Host
if ($response -eq "Y" -or $response -eq "y") {
    Write-Host "Stopping cTrader bridges..." -ForegroundColor Yellow
    docker stop ctrader-bridge ctrader-bridge-demo 2>&1 | Out-Null
    Write-Host "✅ Bridges stopped. Update the tokens and restart with:" -ForegroundColor Green
    Write-Host "   docker-compose -f docker-compose.ctrader.yml up -d" -ForegroundColor White
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Additional Info:" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Current Live Account ID: 47341092" -ForegroundColor White
Write-Host "Current Demo Account ID: (check your demo account)" -ForegroundColor White
Write-Host ""
Write-Host "After updating tokens, verify with:" -ForegroundColor Yellow
Write-Host "  docker logs ctrader-bridge --tail 50" -ForegroundColor White
Write-Host "  docker logs ctrader-bridge-demo --tail 50" -ForegroundColor White
Write-Host ""
