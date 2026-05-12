@echo off
REM cTrader Bridge Startup Script (Batch version)
REM This script starts the Pepperstone cTrader bridge with the correct credentials

echo ========================================
echo   Pepperstone cTrader Bridge Startup
echo ========================================
echo.

REM Set environment variables
set CTRADER_CLIENT_ID=27391_QneKEbL8qea5t4JDIT721RmcOd5i6nIO4hK6YaP0aBiY31q86s
set CTRADER_CLIENT_SECRET=loPssicrvxYshWozrxgGFL40yAxrZPPIBzUYBmC67cxHAKbjty
set CTRADER_ACCESS_TOKEN=sfV4Gls2KooxFKKpsqaUpboQswkPBz65DddPOZkLX-E
set CTRADER_ACCOUNT_ID=47223753
set CTRADER_HOST_TYPE=demo
set CTRADER_BRIDGE_PORT=8082

echo [OK] Environment variables configured
echo   - Host Type: demo
echo   - Account ID: 47223753
echo   - Bridge Port: 8082
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found. Please install Python 3.x
    echo   Download from: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo [OK] Python found
echo.
echo Starting cTrader bridge...
echo Press Ctrl+C to stop the bridge
echo.
echo ========================================
echo.

REM Start the bridge
python ctrader_bridge.py

pause
