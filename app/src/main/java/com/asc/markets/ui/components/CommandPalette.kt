package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asc.markets.data.AppView
import com.asc.markets.data.FOREX_PAIRS
import com.asc.markets.ui.theme.*

@Composable
fun CommandPalette(
    onDismiss: () -> Unit,
    onNavigate: (AppView) -> Unit,
    onSelectAsset: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var pasteBlocked by remember { mutableStateOf(false) }
    fun isSuspectedApiKey(s: String): Boolean {
        val keyPattern = Regex("(?i)sk-[A-Za-z0-9_-]{20,}")
        val generic = Regex("(?i)(openai|api[_-]?key|secret|token)")
        return keyPattern.containsMatchIn(s) || generic.containsMatchIn(s)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Search Input Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(PureBlack, RoundedCornerShape(12.dp))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SlateText)
                    TextField(
                        value = query,
                        onValueChange = {
                            if (isSuspectedApiKey(it)) {
                                pasteBlocked = true
                            } else {
                                pasteBlocked = false
                                query = it
                            }
                        },
                        placeholder = { Text("SEARCH ASSETS OR COMMANDS...", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Black) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pasteBlocked) {
                        Text("Pasting API keys is not allowed. Use build-time config.", color = Color(0xFFFFC107), fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, top = 6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filteredPairs = FOREX_PAIRS.filter { it.symbol.contains(query, ignoreCase = true) }
                    
                    item {
                        Text("SYSTEM NODES", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    
                    items(AppView.values().take(5)) { view ->
                        CommandItem(view.name.replace("_", " "), "NODE_UPLINK") { onNavigate(view) }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("INSTITUTIONAL ASSETS", color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    items(filteredPairs) { pair ->
                        CommandItem(pair.symbol, pair.name) { 
                            onSelectAsset(pair.symbol)
                            onDismiss()
                        }
                    }
                }
                
                // Footer
                Text("ESC TO EXIT • ↵ TO NAVIGATE", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun CommandItem(title: String, sub: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).background(GhostWhite, RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(sub.uppercase(), color = SlateText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}