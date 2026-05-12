package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.imePadding
import com.asc.markets.data.ChatMessage
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.ANALYST_MODELS
import com.asc.markets.ui.theme.*

@Composable
fun ChatScreen(viewModel: ForexViewModel) {
    var input by remember { mutableStateOf("") }
    var pasteBlocked by remember { mutableStateOf(false) }
    fun isSuspectedApiKey(s: String): Boolean {
        val keyPattern = Regex("(?i)sk-[A-Za-z0-9_-]{20,}")
        val generic = Regex("(?i)(openai|api[_-]?key|secret|token)")
        return keyPattern.containsMatchIn(s) || generic.containsMatchIn(s)
    }
    var selectedPersona by remember { mutableStateOf(ANALYST_MODELS[0]) }
    val messages by viewModel.ascChatMessages.collectAsState()
    val isResponding by viewModel.ascChatResponding.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchLatestDeployments()
    }

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.height(32.dp)
            ) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(ANALYST_MODELS) { persona ->
                        val active = selectedPersona.id == persona.id
                        Surface(
                            color = if (active) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(26.dp).clickable { selectedPersona = persona }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = persona.icon,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = persona.name.uppercase(), 
                                        fontSize = 8.sp, 
                                        fontWeight = FontWeight.Black, 
                                        color = if (active) Color.Black else Color.Gray,
                                        fontFamily = InterFontFamily,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    ChatBubble(ChatMessage(role = "model", content = "This is ASC Engine v1. What can I do for you?"))
                }
            }
            items(messages) { msg -> ChatBubble(msg) }
            if (isResponding) {
                item {
                    ChatBubble(ChatMessage(role = "model", content = "[ASC_ENGINE] Reading app state..."))
                }
            }
        }

        Surface(
            color = Color(0xFF111113),
            modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ASC refresh button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Transparent, CircleShape)
                        .clickable { 
                            viewModel.fetchLatestDeployments()
                            viewModel.addAscChatSystemMessage("[ASC_AI] Refreshing latest ASC deployment output...")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Sync, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }

                // Input field (expanded)
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
                    placeholder = { Text("ASK ASC AI...", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp),
                    singleLine = true
                )
                if (pasteBlocked) {
                    Text("Pasting API keys is not allowed. Use build-time config.", color = Color(0xFFFFC107), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }

                // Add data button
                IconButton(onClick = {
                    viewModel.runAiPipelineNow()
                    viewModel.addAscChatSystemMessage("[ASC_AI] Pipeline run requested. Refresh deployments after it completes.")
                }) {
                    Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }

                // Send button
                Surface(
                    onClick = { 
                        if (input.isNotBlank() && !isResponding) { 
                            val userQuery = input.trim()
                            input = "" 
                            viewModel.sendAscChatMessage(
                                userQuery = userQuery,
                                personaName = selectedPersona.name,
                                personaInstruction = selectedPersona.instruction
                            )
                        } 
                    },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (input.isNotBlank() && !isResponding) Color.White else Color.White.copy(alpha = 0.05f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowUpward, 
                            null, 
                            tint = if (input.isNotBlank() && !isResponding) Color.Black else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isUser) Color(0xFF2A2A2A) else Color.White.copy(alpha = 0.03f),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 12.dp
            ),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content, 
                modifier = Modifier.padding(16.dp), 
                color = Color.White, 
                fontSize = 13.sp, 
                lineHeight = 20.sp,
                fontFamily = InterFontFamily,
                fontWeight = if (isUser) FontWeight.Normal else FontWeight.Medium
            )
        }
    }
}
