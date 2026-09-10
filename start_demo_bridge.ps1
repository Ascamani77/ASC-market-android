# Start Pepperstone cTrader DEMO Bridge
# Demo Account ID: 5288664
# Uses same CLIENT_ID and CLIENT_SECRET as live

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Pepperstone cTrader DEMO Bridge" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Shared credentials
$CLIENT_ID = "$env:CTRADER_CLIENT_ID"
$CLIENT_SECRET = "$env:CTRADER_CLIENT_SECRET"
$DEMO_ACCOUNT_ID = "47340965"  # ctidTraderAccountId (not accountNumber)

Write-Host "Demo Account Configuration:" -ForegroundColor Yellow
Write-Host "  Account ID: $DEMO_ACCOUNT_ID (ctidTraderAccountId)" -ForegroundColor White
Write-Host "  Account Number: 5288664 (display only)" -ForegroundColor White
Write-Host "  Balance: $50,000 (demo)" -ForegroundColor White
Write-Host "  Endpoint: DEMO (demo.ctraderapi.com)" -ForegroundColor White
Write-Host "  Port: 8083" -ForegroundColor White
Write-Host ""

# Prompt for access token
Write-Host "You need a DEMO access token." -ForegroundColor Cyan
Write-Host ""
Write-Host "To get one:" -ForegroundColor Yellow
Write-Host "  1. Go to: https://openapi.ctrader.com/" -ForegroundColor Gray
Write-Host "  2. Sign in with DEMO credentials" -ForegroundColor Gray
Write-Host "  3. Applications → Your App → Generate Token" -ForegroundColor Gray
Write-Host ""

$ACCESS_TOKEN = Read-Host "Enter DEMO access token"

if (-not $ACCESS_TOKEN) {
    Write-Host "❌ No token provided" -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "Starting DEMO bridge..." -ForegroundColor Cyan

# Stop and remove if exists
docker stop ctrader-bridge-demo 2>$null | Out-Null
docker rm ctrader-bridge-demo 2>$null | Out-Null

# Start demo bridge
docker run -d --name ctrader-bridge-demo --restart unless-stopped -p 8083:8083 `
  -e CTRADER_CLIENT_ID="$CLIENT_ID" `
  -e CTRADER_CLIENT_SECRET="$CLIENT_SECRET" `
  -e CTRADER_ACCESS_TOKEN="$ACCESS_TOKEN" `
  -e CTRADER_ACCOUNT_ID="$DEMO_ACCOUNT_ID" `
  -e CTRADER_HOST_TYPE="demo" `
  -e CTRADER_BRIDGE_HOST="0.0.0.0" `
  -e CTRADER_BRIDGE_PORT="8083" `
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
$logs = docker logs ctrader-bridge-demo --tail 30 2>&1

# Check for success
if ($logs -match "account_authenticated") {
    Write-Host "✅ DEMO bridge authenticated successfully!" -ForegroundColor Green
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
Write-Host "  DEMO BRIDGE RUNNING" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Connection:" -ForegroundColor Cyan
Write-Host "  WebSocket: ws://localhost:8083" -ForegroundColor White
Write-Host "  Account: $DEMO_ACCOUNT_ID (DEMO)" -ForegroundColor White
Write-Host ""
Write-Host "Commands:" -ForegroundColor Cyan
Write-Host "  View logs:    docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
Write-Host "  Stop:         docker stop ctrader-bridge-demo" -ForegroundColor Gray
Write-Host "  Restart:      docker restart ctrader-bridge-demo" -ForegroundColor Gray
Write-Host "  Check status: docker ps | findstr demo" -ForegroundColor Gray
Write-Host ""

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
