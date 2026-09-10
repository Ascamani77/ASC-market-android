# Start Pepperstone cTrader LIVE Bridge
# Live Account ID: 47341092
# Uses same CLIENT_ID and CLIENT_SECRET as demo

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Pepperstone cTrader LIVE Bridge" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Shared credentials
$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"
$LIVE_ACCOUNT_ID = "47341092"

Write-Host "Live Account Configuration:" -ForegroundColor Yellow
Write-Host "  Account ID: $LIVE_ACCOUNT_ID" -ForegroundColor White
Write-Host "  Endpoint: LIVE (live.ctraderapi.com)" -ForegroundColor White
Write-Host "  Port: 8082" -ForegroundColor White
Write-Host ""

# Prompt for access token
Write-Host "⚠️  WARNING: This is for LIVE trading!" -ForegroundColor Red
Write-Host ""
Write-Host "You need a LIVE access token." -ForegroundColor Cyan
Write-Host ""
Write-Host "To get one:" -ForegroundColor Yellow
Write-Host "  1. Go to: https://openapi.ctrader.com/" -ForegroundColor Gray
Write-Host "  2. Sign in with LIVE credentials" -ForegroundColor Gray
Write-Host "  3. Applications → Your App → Generate Token" -ForegroundColor Gray
Write-Host ""

$ACCESS_TOKEN = Read-Host "Enter LIVE access token"

if (-not $ACCESS_TOKEN) {
    Write-Host "❌ No token provided" -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "Starting LIVE bridge..." -ForegroundColor Cyan

# Stop and remove if exists
docker stop ctrader-bridge-live 2>$null | Out-Null
docker rm ctrader-bridge-live 2>$null | Out-Null

# Start live bridge
docker run -d --name ctrader-bridge-live --restart unless-stopped -p 8082:8082 `
  -e CTRADER_CLIENT_ID="$CLIENT_ID" `
  -e CTRADER_CLIENT_SECRET="$CLIENT_SECRET" `
  -e CTRADER_ACCESS_TOKEN="$ACCESS_TOKEN" `
  -e CTRADER_ACCOUNT_ID="$LIVE_ACCOUNT_ID" `
  -e CTRADER_HOST_TYPE="live" `
  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
  -e CTRADER_BRIDGE_PORT="8082" `
  myrealapp-ctrader-bridge:latest

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start container" -ForegroundColor Red
    pause
    exit 1
}

Write-Host "✅ Container started" -ForegroundColor Green
Write-Host ""
Write-Host "Waiting for connection..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host ""
Write-Host "Checking authentication..." -ForegroundColor Yellow
$logs = docker logs ctrader-bridge-live --tail 30 2>&1

# Check for success
if ($logs -match "account_authenticated") {
    Write-Host "✅ LIVE bridge authenticated successfully!" -ForegroundColor Green
    Write-Host ""
    
    # Show symbols loaded
    $symbolsLine = $logs | Select-String "symbols_loaded"
    if ($symbolsLine) {
        Write-Host $symbolsLine -ForegroundColor Green
    }
} elseif ($logs -match "Account auth timed out|ACCESS_TOKEN") {
    Write-Host "❌ Authentication failed - token may be expired" -ForegroundColor Red
    Write-Host ""
    Write-Host "Recent logs:" -ForegroundColor Yellow
    $logs | Select-Object -Last 10 | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
} else {
    Write-Host "⏳ Still connecting..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Recent logs:" -ForegroundColor Yellow
    $logs | Select-Object -Last 10 | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  LIVE BRIDGE RUNNING" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Connection:" -ForegroundColor Cyan
Write-Host "  WebSocket: ws://localhost:8082" -ForegroundColor White
Write-Host "  Account: $LIVE_ACCOUNT_ID (LIVE)" -ForegroundColor White
Write-Host ""
Write-Host "Commands:" -ForegroundColor Cyan
Write-Host "  View logs:    docker logs -f ctrader-bridge-live" -ForegroundColor Gray
Write-Host "  Stop:         docker stop ctrader-bridge-live" -ForegroundColor Gray
Write-Host "  Restart:      docker restart ctrader-bridge-live" -ForegroundColor Gray
Write-Host "  Check status: docker ps | findstr live" -ForegroundColor Gray
Write-Host ""

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
