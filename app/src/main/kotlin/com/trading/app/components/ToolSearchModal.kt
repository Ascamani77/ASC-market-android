package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ToolSearchModal(
    onToolSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val tools = listOf("Trend Line", "Fib Retracement", "Brush", "Text", "Long Position", "Short Position", "Rectangle", "Circle", "Arrow")
    val filteredTools = tools.filter { it.contains(searchQuery, ignoreCase = true) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Search Input
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search tools...", color = Color(0xFF787B86)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF787B86)) },
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
                
                // Tool List
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTools) { tool ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToolSelect(tool); onClose() }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tool, color = Color.White, fontSize = 14.sp)
                        }
                        Divider(color = Color(0xFF363A45))
                    }
                }
            }
        }
    }
}
