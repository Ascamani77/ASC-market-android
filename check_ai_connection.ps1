# AI Backend Connection Diagnostic Script
# Run this in PowerShell (as Administrator for firewall commands)

Write-Host "=== AI Backend Connection Diagnostics ===" -ForegroundColor Cyan
Write-Host ""

# 1. Check if port 8000 is listening
Write-Host "1. Checking if port 8000 is listening..." -ForegroundColor Yellow
$listening = netstat -ano | Select-String ":8000.*LISTENING"
if ($listening) {
    Write-Host "   ✓ Port 8000 is LISTENING" -ForegroundColor Green
    Write-Host "   $listening" -ForegroundColor Gray
} else {
    Write-Host "   ✗ Port 8000 is NOT listening" -ForegroundColor Red
    Write-Host "   → Start your AI backend first!" -ForegroundColor Red
}
Write-Host ""

# 2. Check PC's IP address
Write-Host "2. Checking PC's IP address..." -ForegroundColor Yellow
$ipAddress = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -like "10.*" -or $_.IPAddress -like "192.168.*"}).IPAddress
if ($ipAddress) {
    Write-Host "   ✓ PC IP: $ipAddress" -ForegroundColor Green
    Write-Host "   → Update local.properties if different from 10.164.138.133" -ForegroundColor Gray
} else {
    Write-Host "   ✗ No local network IP found" -ForegroundColor Red
}
Write-Host ""

# 3. Check firewall rule
Write-Host "3. Checking firewall rule for port 8000..." -ForegroundColor Yellow
$firewallRule = Get-NetFirewallRule -DisplayName "AI Backend Port 8000" -ErrorAction SilentlyContinue
if ($firewallRule) {
    Write-Host "   ✓ Firewall rule exists" -ForegroundColor Green
    $portFilter = Get-NetFirewallPortFilter -AssociatedNetFirewallRule $firewallRule
    Write-Host "   Port: $($portFilter.LocalPort), Enabled: $($firewallRule.Enabled)" -ForegroundColor Gray
} else {
    Write-Host "   ✗ Firewall rule NOT found" -ForegroundColor Red
    Write-Host "   → Run this command as Administrator:" -ForegroundColor Yellow
    Write-Host "   New-NetFirewallRule -DisplayName 'AI Backend Port 8000' -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow" -ForegroundColor Cyan
}
Write-Host ""

# 4. Test localhost connection
Write-Host "4. Testing localhost connection..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8000/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "   ✓ Localhost connection works" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Localhost connection failed" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 5. Test network connection (if IP found)
if ($ipAddress) {
    Write-Host "5. Testing network connection from PC..." -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri "http://${ipAddress}:8000/health" -TimeoutSec 5 -ErrorAction Stop
        Write-Host "   ✓ Network connection works" -ForegroundColor Green
        Write-Host "   Response: $($response.Content)" -ForegroundColor Gray
    } catch {
        Write-Host "   ✗ Network connection failed" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# 6. Check Python processes
Write-Host "6. Checking Python processes..." -ForegroundColor Yellow
$pythonProcesses = Get-Process | Where-Object {$_.ProcessName -like "*python*"}
if ($pythonProcesses) {
    Write-Host "   ✓ Python processes found:" -ForegroundColor Green
    $pythonProcesses | ForEach-Object { Write-Host "   - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Gray }
} else {
    Write-Host "   ✗ No Python processes found" -ForegroundColor Red
    Write-Host "   → AI backend may not be running" -ForegroundColor Red
}
Write-Host ""

# Summary
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. If port 8000 is not listening: Start your AI backend" -ForegroundColor White
Write-Host "2. If firewall rule is missing: Run the firewall command as Administrator" -ForegroundColor White
Write-Host "3. If localhost works but network fails: Check firewall settings" -ForegroundColor White
Write-Host "4. Test from phone browser: http://${ipAddress}:8000/health" -ForegroundColor White
Write-Host ""
