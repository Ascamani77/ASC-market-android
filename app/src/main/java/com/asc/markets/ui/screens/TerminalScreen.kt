package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.data.ChatMessage
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.*
import com.asc.markets.backend.ExecutionStateManager

@Composable
fun TerminalScreen(viewModel: ForexViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    val isArmed by viewModel.isArmed.collectAsState()
    val activeAlgo by viewModel.activeAlgo.collectAsState()
    var input by remember { mutableStateOf("") }

    // Sync ViewModel with Backend State
    LaunchedEffect(isArmed) {
        ExecutionStateManager.setArmed(isArmed)
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Status Bar Parity
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val statusColor = if (isArmed) Color.White else Color.Gray
                    Box(modifier = Modifier.background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, androidx.compose.foundation.shape.CircleShape))
                            Text(
                                text = if (isArmed) "PIPELINE_ARMED" else "SAFETY_LOCK_ACTIVE",
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text("ALGO:", color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text(activeAlgo, color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                
                Surface(
                    color = if (isArmed) Color.White else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isArmed) Color.White else HairlineBorder),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { viewModel.toggleArm() }
                ) {
                    Text(
                        text = if (isArmed) "DISARM" else "ARM_PIPELINE",
                        color = if (isArmed) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true
        ) {
            items(logs) { log ->
                val isSystem = log.content.startsWith("[")
                Text(
                    text = if (log.role == "user") "> ${log.content}" else log.content,
                    color = if (isSystem) Color.White else if (log.role == "user") Color.White.copy(alpha = 0.6f) else SlateText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSystem) FontWeight.Black else FontWeight.Normal,
                    lineHeight = 18.sp
                )
            }
        }

        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("SYS_CMD >", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                IconButton(onClick = { if (input.isNotBlank()) { viewModel.sendCommand(input); input = "" } }) {
                    Text("↵", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}