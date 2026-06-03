# Test Backend AI Scores
Write-Output "Testing AI Backend at http://10.164.138.133:8000/latest-ai"
Write-Output ""

try {
    $response = Invoke-RestMethod -Uri "http://10.164.138.133:8000/latest-ai" -Method Get -TimeoutSec 5
    
    if ($response.final_decision) {
        Write-Output "Backend is responding!"
        Write-Output ""
        Write-Output "Sample Scores:"
        Write-Output ("=" * 80)
        
        $response.final_decision | Select-Object -First 10 | ForEach-Object {
            $asset = $_.asset_1
            $preMove = if ($_.pre_move_ai_score) { ($_.pre_move_ai_score * 100).ToString("F2") + "%" } else { "N/A" }
            $finalTrade = if ($_.final_trade_score) { ($_.final_trade_score * 100).ToString("F2") + "%" } else { "N/A" }
            $journal = if ($_.journal_score) { $_.journal_score.ToString() + "%" } else { "N/A" }
            $state = if ($_.final_trade_state) { $_.final_trade_state } else { "N/A" }
            
            Write-Output ("{0,-15} | PreMove: {1,-8} | FinalTrade: {2,-8} | Journal: {3,-6} | State: {4}" -f $asset, $preMove, $finalTrade, $journal, $state)
        }
        
        Write-Output ""
        Write-Output "Statistics:"
        $totalAssets = $response.final_decision.Count
        $withPreMove = ($response.final_decision | Where-Object { $_.pre_move_ai_score -gt 0 }).Count
        $withFinalTrade = ($response.final_decision | Where-Object { $_.final_trade_score -gt 0 }).Count
        $tradeCandidates = ($response.final_decision | Where-Object { $_.final_trade_state -eq "TRADE_CANDIDATE" }).Count
        
        Write-Output "Total Assets: $totalAssets"
        Write-Output "With PreMove Score greater than 0: $withPreMove"
        Write-Output "With FinalTrade Score greater than 0: $withFinalTrade"
        Write-Output "Trade Candidates: $tradeCandidates"
        
    } else {
        Write-Output "Backend responded but no final_decision data"
    }
    
} catch {
    Write-Output "Backend is NOT accessible!"
    Write-Output "Error: $($_.Exception.Message)"
    Write-Output ""
    Write-Output "Possible issues:"
    Write-Output "1. Backend AI server is not running"
    Write-Output "2. Wrong IP address (current: 10.164.138.133:8000)"
    Write-Output "3. Firewall blocking the connection"
    Write-Output "4. Backend is on a different port"
    Write-Output ""
    Write-Output "To start the backend, run:"
    Write-Output "cd c:\Users\HP\Documents\NEW_ASC"
    Write-Output "python ai_api.py"
}
