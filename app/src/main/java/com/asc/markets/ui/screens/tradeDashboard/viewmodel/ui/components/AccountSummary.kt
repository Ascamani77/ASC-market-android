package com.asc.markets.ui.screens.tradeDashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.AccountInfo
import com.asc.markets.ui.screens.tradeDashboard.ui.theme.DeepBlack
import java.util.Locale

@Composable
fun AccountSummary(account: AccountInfo?, modifier: Modifier = Modifier) {
    if (account == null) return
    
    var showPercentage by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepBlack)
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MT5 ACCOUNT OVERVIEW",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Column {
                        Text("ID:", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("55021944", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text("SERVER:", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("IC-Markets-Live03", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "TOTAL P&L",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                val profitPrefix = if (account.profit >= 0) "+" else ""
                Text(
                    text = "$profitPrefix$${String.format(Locale.US, "%,.2f", account.profit)}",
                    color = if (account.profit >= 0) Color(0xFF00C853) else Color(0xFFFF5252),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Grid of Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val balancePercent = if (account.balance != 0.0) (account.profit / account.balance) * 100 else 0.0
            AccountStatBox(
                label = "BALANCE",
                value = if (showPercentage) {
                    String.format(Locale.US, "%+.2f%%", balancePercent)
                } else {
                    "$${String.format(Locale.US, "%,.2f", account.balance)}"
                },
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showPercentage = !showPercentage }
            )
            
            val equityPercent = if (account.equity != 0.0) (account.profit / account.equity) * 100 else 0.0
            AccountStatBox(
                label = "EQUITY",
                value = if (showPercentage) {
                    String.format(Locale.US, "%+.2f%%", equityPercent)
                } else {
                    "$${String.format(Locale.US, "%,.2f", account.equity)}"
                },
                icon = Icons.Default.PieChart,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showPercentage = !showPercentage }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountStatBox(
                label = "MARGIN",
                value = "$${String.format(Locale.US, "%,.2f", account.margin)}",
                icon = Icons.Default.AccountBalance,
                modifier = Modifier.weight(1f)
            )
            AccountStatBox(
                label = "MARGIN LEVEL",
                value = "${String.format(Locale.US, "%.2f", account.marginLevel)}%",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f),
                iconColor = if (account.marginLevel < 100 && account.marginLevel > 0) Color(0xFFFF5252) else Color.Gray
            )
        }
    }
}

@Composable
private fun AccountStatBox(
    label: String, 
    value: String, 
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Gray
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
