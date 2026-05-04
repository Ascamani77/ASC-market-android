package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.asc.markets.ui.components.AppBottomNavHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingsModal(
    onClose: () -> Unit,
    onToolSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(DrawingCategory.FAVORITES) }
    var showFavoritesOnChart by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363A45))
            )
        },
        windowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxHeight(0.93f)
            .padding(bottom = AppBottomNavHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Drawings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            DrawingsSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it }
            )

            DrawingsCategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            DrawingsCategoryPage(
                category = selectedCategory,
                searchQuery = searchQuery,
                onToolSelect = {
                    onToolSelect(it)
                    onClose()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            DrawingsBottomToggle(
                showFavoritesOnChart = showFavoritesOnChart,
                onShowFavoritesOnChartChange = { showFavoritesOnChart = it }
            )
        }
    }
}
