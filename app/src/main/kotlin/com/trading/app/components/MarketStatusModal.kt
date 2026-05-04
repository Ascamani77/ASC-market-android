package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.AppBottomNavHeight
import java.util.*
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketStatusModal(
    symbol: String,
    selectedTimeZone: String,
    onDismiss: () -> Unit
) {
    val isCrypto = symbol.uppercase().contains("BTC") || symbol.uppercase().contains("ETH")
    
    // Simple logic for demo: Crypto is always open, Forex closed on weekends
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    
    val isOpen = isCrypto || (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY)
    
    val statusColor = if (isOpen) Color(0xFF089981) else Color(0xFF787B86)
    val statusText = if (isOpen) "Market open" else "Market closed"
    val description = if (isOpen) "All's well — market is open." else "Time for a walk — this market is closed."
    
    val nextEvent = if (isOpen) {
        if (isCrypto) "24/7 trading available"
        else "It'll close in 11 hours and 5 minutes."
    } else {
        "It'll open in 8 hours and 4 minutes."
    }

    val tzDisplay = if (selectedTimeZone.contains(")")) {
        selectedTimeZone.substringBefore(")") + ")"
    } else {
        selectedTimeZone
    }
    
    val tzId = if (selectedTimeZone.contains(")")) {
        selectedTimeZone.substringAfter(") ").trim()
    } else {
        selectedTimeZone
    }
    
    val tz = TimeZone.getTimeZone(tzId)
    val sdf = SimpleDateFormat("hh:mm a", Locale.US)
    sdf.timeZone = tz
    val currentTime = sdf.format(Date())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363A45))
            )
        },
        windowInsets = WindowInsets(0),
        modifier = Modifier.padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = symbol.uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = description,
                color = Color(0xFFD1D4DC),
                fontSize = 15.sp
            )
            
            Text(
                text = nextEvent,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Timeline view
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayName = SimpleDateFormat("EEE", Locale.US).format(Date()).uppercase()
                    Text(dayName, color = Color(0xFF787B86), fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF363A45))
                ) {
                    val progress = if (isOpen) 0.6f else 0.4f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(if (isOpen) Color(0xFF089981) else Color(0xFF363A45))
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("00:00", color = Color(0xFF787B86), fontSize = 12.sp)
                    Text(if (isOpen && isCrypto) "24:00" else "17:00", color = Color(0xFF787B86), fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Exchange timezone: $tzDisplay, $currentTime",
                color = Color(0xFF787B86),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
