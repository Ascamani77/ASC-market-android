# Generate cTrader Demo Access Token
# This script helps you get an access token for your demo account

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  cTrader Demo Access Token Generator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Credentials (same CLIENT_ID and SECRET for both live and demo)
$CLIENT_ID = "27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s"
$CLIENT_SECRET = "loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty"
$DEMO_ACCOUNT_ID = "47340965"  # ctidTraderAccountId

Write-Host "Using credentials:" -ForegroundColor Yellow
Write-Host "  Client ID: $($CLIENT_ID.Substring(0, 20))..." -ForegroundColor Gray
Write-Host "  Client Secret: $($CLIENT_SECRET.Substring(0, 20))..." -ForegroundColor Gray
Write-Host "  Demo Account ID: $DEMO_ACCOUNT_ID (ctidTraderAccountId)" -ForegroundColor Gray
Write-Host "  Demo Account Number: 5288664 (display only)" -ForegroundColor Gray
Write-Host ""

Write-Host "To get an access token, you have two options:" -ForegroundColor Cyan
Write-Host ""

Write-Host "Option 1: Via Pepperstone Portal (Recommended)" -ForegroundColor Yellow
Write-Host "  1. Go to: https://openapi.ctrader.com/" -ForegroundColor White
Write-Host "  2. Sign in with your Pepperstone DEMO credentials" -ForegroundColor White
Write-Host "  3. Navigate to 'Applications'" -ForegroundColor White
Write-Host "  4. Select your application" -ForegroundColor White
Write-Host "  5. Click 'Generate Token' or 'Refresh Token'" -ForegroundColor White
Write-Host "  6. Copy the access token" -ForegroundColor White
Write-Host ""

Write-Host "Option 2: Via OAuth2 Flow (Advanced)" -ForegroundColor Yellow
Write-Host "  This requires implementing OAuth2 authorization code flow" -ForegroundColor White
Write-Host "  See: https://help.ctrader.com/open-api/authentication/" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Quick Start Demo Bridge" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ACCESS_TOKEN = Read-Host "Paste your DEMO access token here (or press Enter to exit)"

if (-not $ACCESS_TOKEN) {
    Write-Host "No token provided. Exiting..." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Starting DEMO bridge with your token..." -ForegroundColor Cyan
Write-Host ""

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

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ DEMO bridge started successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Waiting for connection..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    Write-Host ""
    Write-Host "Checking logs..." -ForegroundColor Yellow
    docker logs ctrader-bridge-demo --tail 20
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  DEMO BRIDGE RUNNING" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Connection Details:" -ForegroundColor Cyan
    Write-Host "  WebSocket URL: ws://localhost:8083" -ForegroundColor White
    Write-Host "  Account ID: $DEMO_ACCOUNT_ID" -ForegroundColor White
    Write-Host "  Endpoint: DEMO (demo.ctraderapi.com)" -ForegroundColor White
    Write-Host ""
    Write-Host "View logs:" -ForegroundColor Cyan
    Write-Host "  docker logs -f ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Stop bridge:" -ForegroundColor Cyan
    Write-Host "  docker stop ctrader-bridge-demo" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host "❌ Failed to start DEMO bridge" -ForegroundColor Red
    Write-Host "Check Docker Desktop is running" -ForegroundColor Yellow
}

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
