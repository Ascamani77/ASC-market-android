package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.trading.app.models.UserAlert

@Composable
fun AlertModal(
    symbol: String,
    onAlertCreate: (UserAlert) -> Unit,
    onClose: () -> Unit
) {
    var price by remember { mutableStateOf("150.00") }
    var condition by remember { mutableStateOf("Crossing") }
    var message by remember { mutableStateOf("Price crossed 150.00") }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Alert on $symbol", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color.White,
                        modifier = Modifier.clickable { onClose() }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Condition
                Text("Condition", color = Color(0xFF787B86), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131722), RoundedCornerShape(4.dp))
                        .padding(12.dp)
                ) {
                    Text(condition, color = Color.White, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Price
                Text("Price", color = Color(0xFF787B86), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131722),
                        unfocusedContainerColor = Color(0xFF131722),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2962FF)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message
                Text("Message", color = Color(0xFF787B86), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131722),
                        unfocusedContainerColor = Color(0xFF131722),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF2962FF)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Create Button
                Button(
                    onClick = {
                        onAlertCreate(UserAlert(
                            id = System.currentTimeMillis().toString(),
                            symbol = symbol,
                            condition = condition,
                            price = price.toFloatOrNull() ?: 0f,
                            message = message,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        ))
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                ) {
                    Text("Create Alert")
                }
            }
        }
    }
}
