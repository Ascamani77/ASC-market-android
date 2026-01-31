package com.asc.markets.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.api.ForexAnalysisEngine
import com.asc.markets.data.ChatMessage
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.ANALYST_MODELS
import com.asc.markets.ui.theme.*

@Composable
fun ChatScreen(viewModel: ForexViewModel) {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var selectedPersona by remember { mutableStateOf(ANALYST_MODELS[0]) }
    var isVoiceActive by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.height(44.dp)
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
                            modifier = Modifier.height(36.dp).clickable { selectedPersona = persona }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                                Text(
                                    text = persona.name.uppercase(), 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Black, 
                                    color = if (active) Color.Black else Color.Gray,
                                    fontFamily = InterFontFamily,
                                    letterSpacing = 1.sp
                                )
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
                    ChatBubble(ChatMessage(role = "model", content = "Local Analyst Online. Selected Module: ${selectedPersona.name}."))
                }
            }
            items(messages) { msg -> ChatBubble(msg) }
        }

        Surface(
            color = Color(0xFF111113),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("COMMAND INPUT...", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(if (isVoiceActive) pulseScale else 1f)
                                .background(if (isVoiceActive) RoseError.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                                .clickable { isVoiceActive = !isVoiceActive },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, null, tint = if (isVoiceActive) RoseError else Color.Gray, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Add, null, tint = Color.Gray)
                        }
                    }

                    Surface(
                        onClick = { 
                            if (input.isNotBlank()) { 
                                val userQuery = input
                                messages.add(ChatMessage(role = "user", content = userQuery)) 
                                input = "" 
                                val response = ForexAnalysisEngine.getAnalystResponse(userQuery, selectedPersona.name)
                                messages.add(ChatMessage(role = "model", content = response))
                            } 
                        },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (input.isNotBlank()) Color.White else Color.White.copy(alpha = 0.05f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ArrowForward, 
                                null, 
                                tint = if (input.isNotBlank()) Color.Black else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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