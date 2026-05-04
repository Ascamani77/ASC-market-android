package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class SearchItem(
    val label: String,
    val category: String,
    val categoryKey: String,
    val icon: ImageVector
)

@Composable
fun SearchResultsPage(
    searchQuery: String,
    searchResults: List<SearchItem>,
    onResultSelect: (SearchItem) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with search query and controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Search Results",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "\"$searchQuery\"",
                            color = Color(0xFF787B86),
                            fontSize = 14.sp,
                            modifier = Modifier.paddingFromBaseline(top = 8.dp)
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF787B86),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Divider(color = Color(0xFF2A2E39))

                // Results count
                Text(
                    "${searchResults.size} result${if (searchResults.size != 1) "s" else ""} found",
                    color = Color(0xFF787B86),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp)
                )

                // Search results list
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentHeight(align = Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results found for \"$searchQuery\"",
                            color = Color(0xFF787B86),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        searchResults.forEachIndexed { index, result ->
                            SearchResultItem(
                                result = result,
                                onSelect = {
                                    onResultSelect(result)
                                    onClose()
                                }
                            )
                            if (index < searchResults.size - 1) {
                                Divider(color = Color(0xFF2A2E39))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: SearchItem,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Surface(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF121212), RoundedCornerShape(8.dp)),
            color = Color(0xFF121212),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = result.icon,
                contentDescription = null,
                tint = Color(0xFF2962FF),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = result.category,
                color = Color(0xFF787B86),
                fontSize = 12.sp,
                modifier = Modifier.paddingFromBaseline(top = 4.dp)
            )
        }

        // Chevron icon
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null,
            tint = Color(0xFF787B86),
            modifier = Modifier
                .size(20.dp)
                .rotate(180f)
        )
    }
}
