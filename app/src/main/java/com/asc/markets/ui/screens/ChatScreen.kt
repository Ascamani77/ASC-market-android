package com.asc.markets.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.imePadding
import com.asc.markets.data.ChatMessage
import com.asc.markets.data.AppView
import com.asc.markets.data.toAiContextLabel
import com.asc.markets.logic.AscChatSession
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.ANALYST_MODELS
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: ForexViewModel) {
    var input by remember { mutableStateOf("") }
    var pasteBlocked by remember { mutableStateOf(false) }
    var showNewReplyNotice by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }
    var showChatSessionsSheet by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<AscChatSession?>(null) }
    var lastObservedMessageCount by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val contextSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val chatSessionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    fun isSuspectedApiKey(s: String): Boolean {
        val keyPattern = Regex("(?i)sk-[A-Za-z0-9_-]{20,}")
        val generic = Regex("(?i)(openai|api[_-]?key|secret|token)")
        return keyPattern.containsMatchIn(s) || generic.containsMatchIn(s)
    }
    val activePersonaId by viewModel.ascChatPersonaId.collectAsState()
    val selectedPersona = ANALYST_MODELS.firstOrNull { it.id == activePersonaId } ?: ANALYST_MODELS[0]
    val activeContextPageId by viewModel.ascChatContextPageId.collectAsState()
    val selectedContextPage = AppView.values().firstOrNull { it.name == activeContextPageId }
    val selectedContextLabel = selectedContextPage?.toAiContextLabel()
    val dedicatedContextPages = remember { viewModel.dedicatedChatContextPages() }
    val chatSessions by viewModel.ascChatSessions.collectAsState()
    val activeChatSessionId by viewModel.ascChatSessionId.collectAsState()
    val messages by viewModel.ascChatMessages.collectAsState()
    val isResponding by viewModel.ascChatResponding.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchLatestDeployments()
    }

    LaunchedEffect(messages.size, isResponding) {
        val currentCount = messages.size
        val lastMessage = messages.lastOrNull()
        if (currentCount > lastObservedMessageCount && lastMessage?.role == "model" && lastObservedMessageCount > 0) {
            showNewReplyNotice = true
            delay(1800)
            showNewReplyNotice = false
        }
        lastObservedMessageCount = currentCount
    }

    LaunchedEffect(activeChatSessionId) {
        lastObservedMessageCount = messages.size
        showNewReplyNotice = false
    }

    LaunchedEffect(messages.size, isResponding) {
        if (messages.isNotEmpty() || isResponding) {
            delay(80)
            val targetIndex = if (isResponding) messages.size else messages.lastIndex
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    fun sendCurrentMessage() {
        if (input.isNotBlank() && !isResponding) {
            val userQuery = input.trim()
            input = ""
            viewModel.sendAscChatMessage(
                userQuery = userQuery,
                personaName = selectedPersona.name,
                personaInstruction = selectedPersona.instruction
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                border = if (active) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)) else null,
                                modifier = Modifier.height(26.dp).clickable { viewModel.setAscChatPersona(persona.id) }
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

                if (selectedContextPage != null && selectedContextLabel != null) {
                    Box(modifier = Modifier.padding(top = 8.dp).zIndex(2f)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text("Context focus", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = selectedContextLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.clearAscChatContextPage() },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = 18.dp, y = (-10).dp)
                                .size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear page focus",
                                tint = Color(0xFFD8A23A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            ChatWelcomeCard(selectedPersona = selectedPersona)
                        }
                    }
                    items(messages) { msg -> ChatBubble(msg) }
                    if (isResponding) {
                        item {
                            ChatTypingBubble()
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showNewReplyNotice,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 14.dp),
                    enter = fadeIn() + slideInVertically { it / 3 },
                    exit = fadeOut() + slideOutVertically { it / 3 }
                ) {
                    Surface(
                        color = Color(0xFF17181C).copy(alpha = 0.96f),
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CC38A), CircleShape)
                            )
                            Text(
                                text = "New reply arrived",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Surface(
                color = Color(0xFF111113),
                modifier = Modifier.fillMaxWidth().padding(12.dp).imePadding(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Page context button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { showContextSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { showChatSessionsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Create, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
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
                            .heightIn(min = 48.dp, max = 120.dp),
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendCurrentMessage() })
                    )
                    if (pasteBlocked) {
                        Text("Pasting API keys is not allowed. Use build-time config.", color = Color(0xFFFFC107), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                    }

                    // Send button
                    Surface(
                        onClick = { sendCurrentMessage() },
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

        if (showContextSheet) {
            ChatContextSheet(
                sheetState = contextSheetState,
                currentFocus = selectedContextPage,
                contextPages = dedicatedContextPages,
                onClose = { showContextSheet = false },
                onSelectPage = { page ->
                    viewModel.setAscChatContextPage(page)
                    showContextSheet = false
                }
            )
        }

        if (showChatSessionsSheet) {
            ChatSessionsSheet(
                sheetState = chatSessionsSheetState,
                sessions = chatSessions,
                activeSessionId = activeChatSessionId,
                onClose = { showChatSessionsSheet = false },
                onStartNewChat = {
                    input = ""
                    pasteBlocked = false
                    viewModel.startNewAscChatSession()
                    showChatSessionsSheet = false
                },
                onSelectSession = { session ->
                    viewModel.setActiveAscChatSession(session.id)
                    showChatSessionsSheet = false
                },
                onDeleteSession = { sessionToDelete = it }
            )
        }

        if (sessionToDelete != null) {
            val targetSession = sessionToDelete
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text("Delete chat session") },
                text = { Text("Delete \"${targetSession?.title}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        targetSession?.let { viewModel.deleteAscChatSession(it.id) }
                        sessionToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatWelcomeCard(selectedPersona: com.asc.markets.logic.AnalystPersona) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                )
                Text(
                    text = "ASC Engine v1",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Ready to continue. Ask a follow-up, and I’ll keep the current conversation context.",
                color = SlateText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Active desk: ${selectedPersona.name} • ${selectedPersona.desc}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ChatTypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, delayMillis = 140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, delayMillis = 280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Surface(
            color = Color.White.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("ASC Engine is thinking", color = SlateText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color.White.copy(alpha = dot1), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(Color.White.copy(alpha = dot2), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(Color.White.copy(alpha = dot3), CircleShape))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContextSheet(
    sheetState: SheetState,
    currentFocus: AppView?,
    contextPages: List<AppView>,
    onClose: () -> Unit,
    onSelectPage: (AppView) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        windowInsets = WindowInsets(0),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF363A45), RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier
            .fillMaxHeight(0.93f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Chat page context",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose one page as the main lens. The assistant can still reference other pages when useful.",
                color = SlateText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(contextPages) { page ->
                    val active = page == currentFocus
                    Surface(
                        onClick = { onSelectPage(page) },
                        color = if (active) Color.White else Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Color.White else HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (active) Color.Black else Color.White.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(page.name.take(1), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.toAiContextLabel(),
                                    color = if (active) Color.Black else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (active) "Focused now" else "Tap to focus this page",
                                    color = if (active) Color.Black.copy(alpha = 0.7f) else SlateText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatSessionsSheet(
    sheetState: SheetState,
    sessions: List<AscChatSession>,
    activeSessionId: String,
    onClose: () -> Unit,
    onStartNewChat: () -> Unit,
    onSelectSession: (AscChatSession) -> Unit,
    onDeleteSession: (AscChatSession) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        windowInsets = WindowInsets(0),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(Color(0xFF363A45), RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier
            .fillMaxHeight(0.93f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "New Chat",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start a fresh conversation or switch to one of your saved chats.",
                color = SlateText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            Surface(
                onClick = onStartNewChat,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Create, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Chat",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to start a clean conversation",
                            color = SlateText,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = SlateText, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = "Your chats",
                color = SlateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.id }) { session ->
                    val active = session.id == activeSessionId
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = { onSelectSession(session) },
                            onLongClick = { onDeleteSession(session) }
                        )
                    ) {
                        Surface(
                            color = if (active) Color.White else Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) Color.White else HairlineBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (active) Color.Black else Color.White.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (active) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(session.title.take(1).uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        color = if (active) Color.Black else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (active) "Active chat" else "Tap to switch • Long press to delete",
                                        color = if (active) Color.Black.copy(alpha = 0.7f) else SlateText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
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
