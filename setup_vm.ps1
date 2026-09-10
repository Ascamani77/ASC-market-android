# setup_vm.ps1
# One-shot VM provisioner + background service launcher for the ASC stack.
#
# Run this ON the Azure VM (where MT5 lives), from the folder that contains
# the Python servers. It can be run repeatedly - it is idempotent.
#
#   .\setup_vm.ps1                          # start services + open firewall
#   .\setup_vm.ps1 -InstallDeps             # also pip-install deps
#   .\setup_vm.ps1 -InstallScheduledTask    # also auto-start at every logon
#   .\setup_vm.ps1 -StartOnly               # just (re)start services, no firewall/deps
#   .\setup_vm.ps1 -SkipFirewall            # start services, leave firewall alone
#
# Missing file policy: hybrid_ai_server.py / rss_news_sentiment_service.py are
# expected to be in this folder too (they come from the AI/EA folder). If any
# file is absent the script warns and simply skips that service.

param(
    [switch]$InstallDeps,
    [switch]$InstallScheduledTask,
    [switch]$StartOnly,
    [switch]$SkipFirewall
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$LogDir = Join-Path $ScriptDir "logs"
$Created = @()

$Services = @(
    [pscustomobject]@{
        Name = "MT5 Bridge"
        Script = "mt5_bridge.py"
        Ports = @(8001, 8081)
        Note = "Serves MQL5\Files on :8001 and the chart/price WebSocket on :8081. Requires MT5 running."
    },
    [pscustomobject]@{
        Name = "Hybrid AI Server"
        Script = "hybrid_ai_server.py"
        Ports = @(5000)
        Note = "Signals endpoint on :5000. Reread from the AI/EA folder."
    },
    [pscustomobject]@{
        Name = "RSS News Sentiment"
        Script = "rss_news_sentiment_service.py"
        Ports = @(8766)
        Note = "News sentiment on :8766. Reread from the AI/EA folder."
    }
)

function Write-Step($Text) {
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Test-Python {
    $py = Get-Command python -ErrorAction SilentlyContinue
    if (-not $py) {
        Write-Host "Python not found on PATH. Install Python 3.10+ from https://www.python.org/downloads/ " -ForegroundColor Red
        Write-Host "and tick 'Add python.exe to PATH' during setup, then re-run this script." -ForegroundColor Yellow
        exit 1
    }
    try {
        $ver = & python --version 2>&1
        Write-Step "Python: $($ver.Trim())"
    } catch {
        Write-Host "python --version failed ($_). " -ForegroundColor Red
        exit 1
    }
}

function Start-ServiceBackground($Svc) {
    $scriptPath = Join-Path $ScriptDir $Svc.Script
    if (-not (Test-Path -LiteralPath $scriptPath)) {
        Write-Host "[SKIP] $($Svc.Name): $($Svc.Script) not found next to this script. " -ForegroundColor Yellow
        return
    }
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    $stdout = Join-Path $LogDir ($Svc.Script -replace "\.py$", ".out.log")
    $stderr = Join-Path $LogDir ($Svc.Script -replace "\.py$", ".err.log")

    Get-Process python -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -and (Get-CimInstance Win32_Process -Filter "ProcessId=$($_.Id)").CommandLine -like "*$($Svc.Script)*" } |
        Stop-Process -Force -ErrorAction SilentlyContinue

    $p = Start-Process -FilePath "python" `
        -ArgumentList $scriptPath `
        -WorkingDirectory $ScriptDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru
    $script:Created += $Svc.Name
    Write-Host "[START] $($Svc.Name) (pid $($p.Id)) -> $($Svc.Note)" -ForegroundColor Green
}

function Set-FirewallRules($Svc) {
    foreach ($port in $Svc.Ports) {
        $ruleName = "ASC-in-{0}-{1}" -f ($Svc.Name -replace " ", "-"), $port
        if (-not (Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue)) {
            try {
                New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort $port -Action Allow | Out-Null
                Write-Host "[FW] Added rule $ruleName" -ForegroundColor Green
            } catch {
                Write-Host "[FW] Could not add rule for port $port $(if ($_.Exception.Message -match 'privileges') { '(needs admin - run PowerShell as Administrator)' })" -ForegroundColor Yellow
            }
        }
    }
}

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  ASC Stack - VM setup" -ForegroundColor Cyan
Write-Host "  Folder: $ScriptDir" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

Test-Python

if ($InstallDeps) {
    Write-Step "Installing Python dependencies"
    $requirements = Join-Path $ScriptDir "requirements.txt"
    if (Test-Path -LiteralPath $requirements) {
        & python -m pip install -r $requirements
    }
    & python -m pip install --upgrade MetaTrader5 websockets requests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "pip install had errors. Check the messages above." -ForegroundColor Yellow
    }
}

if (-not $SkipFirewall) {
    Write-Step "Opening inbound firewall ports (8001, 8081, 5000, 8766)"
    foreach ($Svc in $Services) { Set-FirewallRules $Svc }
}

Write-Step "Starting background services"
foreach ($Svc in $Services) { Start-ServiceBackground $Svc }

if ($InstallScheduledTask) {
    Write-Step "Registering scheduled task to (re)start services at logon"
    $taskCmd = "powershell -NoProfile -ExecutionPolicy Bypass -File `"$PSScriptRoot\setup_vm.ps1`" -StartOnly"
    schtasks /Create /F /TN "ASC Stack Services" /SC ONLOGON /RL HIGHEST /TR $taskCmd | Out-Null
    Write-Host "[TASK] ASC Stack Services scheduled at logon" -ForegroundColor Green
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
if ($Created.Count -eq 0) {
    Write-Host "  No services started (files missing?)." -ForegroundColor Yellow
} else {
    Write-Host "  Started: $($Created -join ', ')" -ForegroundColor Green
}
Write-Host "======================================" -ForegroundColor Green
Write-Host "Logs: $LogDir"
Write-Host ""
Write-Host "Still required on the Azure VM (one-time):" -ForegroundColor Yellow
Write-Host "  1. Keep MT5 open with the ASC EA attached and Algo Trading ON." -ForegroundColor White
Write-Host "  2. Azure NSG inbound rules: 8001, 5000, 8081, 8766 (and 3389 RDP)." -ForegroundColor White
Write-Host "  3. MT5  Tools -> Options -> Expert Advisors -> Allow WebRequest add:" -ForegroundColor White
Write-Host "       http://127.0.0.1:5000 / https://api.telegram.org / https://www.forexfactory.com" -ForegroundColor White
Write-Host "  4. In the ASC EA properties, set Backend endpoint to http://127.0.0.1:8001" -ForegroundColor White
Write-Host "     (it is currently 10.164.138.133:8000 - a stale LAN address)." -ForegroundColor White
Write-Host "  5. Enable autologon + keep the VM session logged in so RDP logoff" -ForegroundColor White
Write-Host "     does not stop MT5/services (RDP disconnect alone is fine)." -ForegroundColor White