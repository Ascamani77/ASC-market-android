package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun TradingAssistantScreen(viewModel: ForexViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    val isArmed by viewModel.isArmed.collectAsState()
    var input by remember { mutableStateOf("") }
    var pasteBlocked by remember { mutableStateOf(false) }
    fun isSuspectedApiKey(s: String): Boolean {
        val keyPattern = Regex("(?i)sk-[A-Za-z0-9_-]{20,}")
        val generic = Regex("(?i)(openai|api[_-]?key|secret|token)")
        return keyPattern.containsMatchIn(s) || generic.containsMatchIn(s)
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        // Status Bar Parity
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(if (isArmed) EmeraldSuccess else Color.Gray, androidx.compose.foundation.shape.CircleShape))
                    Text(
                        if (isArmed) "PIPELINE_ARMED" else "SAFETY_LOCK_ACTIVE",
                        color = if (isArmed) Color.White else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text("MODE: ASSISTANT_v4.2", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                Text(
                    text = if (log.role == "user") "> ${log.content}" else log.content,
                    color = if (log.role == "user") Color.White else SlateText,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    lineHeight = 18.sp
                )
            }
        }

        // Action Input Desk
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("SYS_CMD:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                TextField(
                    value = input,
                    onValueChange = {
                        if (isSuspectedApiKey(it)) {
                            pasteBlocked = true
                        } else {
                            pasteBlocked = false
                            input = it
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp)
                )
                if (pasteBlocked) {
                    Text("Pasting API keys is not allowed. Use build-time config.", color = Color(0xFFFFC107), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                IconButton(onClick = { if (input.isNotBlank()) { viewModel.sendCommand(input); input = "" } }) {
                    Text("↵", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}