package com.asc.markets.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import com.asc.markets.backend.SurveillanceStateManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TerminalScreen(viewModel: ForexViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    val isArmed by viewModel.isArmed.collectAsState()
    val activeAlgo by viewModel.activeAlgo.collectAsState()
    var input by remember { mutableStateOf("") }
    var pasteBlocked by remember { mutableStateOf(false) }
    fun isSuspectedApiKey(s: String): Boolean {
        val keyPattern = Regex("(?i)sk-[A-Za-z0-9_-]{20,}")
        val generic = Regex("(?i)(openai|api[_-]?key|secret|token)")
        return keyPattern.containsMatchIn(s) || generic.containsMatchIn(s)
    }

    // Sync ViewModel with Backend State
    LaunchedEffect(isArmed) {
        SurveillanceStateManager.setArmed(isArmed)
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
                    // pulsing heartbeat glow
                    val pulse = rememberInfiniteTransition()
                    val pulseAlpha by pulse.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 0.6f,
                        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse)
                    )

                        Box(modifier = Modifier.background(statusColor.copy(alpha = 0.06f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(statusColor.copy(alpha = pulseAlpha), androidx.compose.foundation.shape.CircleShape))
                            Text(
                                text = if (isArmed) "SURVEILLANCE_ARMED" else "SURVEILLANCE_LOCKED",
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
                        text = if (isArmed) "DISARM" else "ARM_SURVEILLANCE",
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
            modifier = Modifier.weight(1f).padding(16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true
        ) {
            items(logs, key = { it.id }) { log ->
                val isSystem = log.content.startsWith("[")
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(300))) {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val ts = sdf.format(Date(log.timestamp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (log.role == "model") Arrangement.Start else Arrangement.End) {
                        // message bubble
                        Surface(
                            color = if (log.role == "model") Color(0xFF0D1113) else IndigoAccent,
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .fillMaxWidth(0.72f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // timestamp aligned to the same side as the bubble content
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (log.role == "model") Arrangement.Start else Arrangement.End) {
                                    Text(ts, color = SlateText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (log.role == "model") {
                                    TypewriterText(text = log.content)
                                } else {
                                    Text(
                                        text = log.content,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val bringRequester = remember { BringIntoViewRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        var inputFocused by remember { mutableStateOf(false) }

        Surface(
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(modifier = Modifier
                .padding(horizontal = 12.dp)
                .bringIntoViewRequester(bringRequester), verticalAlignment = Alignment.CenterVertically) {
                // SYS_CMD with blinking cursor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SYS_CMD >", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    val cursorTransition = rememberInfiniteTransition()
                    val cursorAlpha by cursorTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(animation = keyframes { durationMillis = 800 }, repeatMode = RepeatMode.Reverse)
                    )
                    Box(modifier = Modifier.padding(start = 6.dp)) {
                        Text("|", color = EmeraldSuccess.copy(alpha = cursorAlpha), fontFamily = FontFamily.Monospace)
                    }
                }

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
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state -> inputFocused = state.isFocused },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                if (pasteBlocked) {
                    Text("Pasting API keys is not allowed. Use build-time config.", color = Color(0xFFFFC107), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                LaunchedEffect(inputFocused) {
                    if (inputFocused) bringRequester.bringIntoView()
                }
                IconButton(onClick = { if (input.isNotBlank()) { viewModel.sendCommand(input); input = "" } }) {
                    Text("↵", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        
    }
}


@Composable
fun TypewriterText(text: String) {
    var displayed by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        displayed = ""
        for (c in text) {
            displayed += c
            delay(6)
        }
    }

    Text(
        text = displayed,
        color = Color.White,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp
    )
}
