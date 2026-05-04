package com.trading.app.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CalendarFilters(
    val selectedImportance: Set<String> = setOf("High", "Medium", "Low", "Holidays"),
    val selectedCountries: Set<String> = setOf(
        "Australia", "Brazil", "Canada", "China", "European Union", "France", "Germany",
        "Hong Kong", "India", "Italy", "Japan", "Mexico", "New Zealand", "Norway",
        "Singapore", "South Africa", "South Korea", "Spain", "Sweden", "Switzerland",
        "United Kingdom", "United States"
    ),
    val allWorld: Boolean = false
)

@Composable
fun CalendarFilterPage(
    filters: CalendarFilters,
    onFiltersChange: (CalendarFilters) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val importanceOptions = listOf("High", "Medium", "Low", "Holidays")
    val countryOptions = listOf(
        "Australia" to "🇦🇺",
        "Brazil" to "🇧🇷",
        "Canada" to "🇨🇦",
        "China" to "🇨🇳",
        "European Union" to "🇪🇺",
        "France" to "🇫🇷",
        "Germany" to "🇩🇪",
        "Hong Kong" to "🇭🇰",
        "India" to "🇮🇳",
        "Italy" to "🇮🇹",
        "Japan" to "🇯🇵",
        "Mexico" to "🇲🇽",
        "New Zealand" to "🇳🇿",
        "Norway" to "🇳🇴",
        "Singapore" to "🇸🇬",
        "South Africa" to "🇿🇦",
        "South Korea" to "🇰🇷",
        "Spain" to "🇪🇸",
        "Sweden" to "🇸🇪",
        "Switzerland" to "🇨🇭",
        "United Kingdom" to "🇬🇧",
        "United States" to "🇺🇸"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Filters",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Importance Section
            item {
                SectionHeader(
                    title = "IMPORTANCE",
                    onSelectAll = {
                        onFiltersChange(filters.copy(selectedImportance = importanceOptions.toSet()))
                    }
                )
            }

            items(importanceOptions) { importance ->
                FilterRow(
                    label = importance,
                    isSelected = filters.selectedImportance.contains(importance),
                    onToggle = {
                        val newImportance = if (filters.selectedImportance.contains(importance)) {
                            filters.selectedImportance - importance
                        } else {
                            filters.selectedImportance + importance
                        }
                        onFiltersChange(filters.copy(selectedImportance = newImportance))
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(importanceColor(importance))
                        )
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Country Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "COUNTRY",
                    onSelectAll = {
                        onFiltersChange(filters.copy(selectedCountries = countryOptions.map { it.first }.toSet()))
                    }
                )
            }

            items(countryOptions) { (name, flag) ->
                FilterRow(
                    label = name,
                    isSelected = filters.selectedCountries.contains(name),
                    onToggle = {
                        val newCountries = if (filters.selectedCountries.contains(name)) {
                            filters.selectedCountries - name
                        } else {
                            filters.selectedCountries + name
                        }
                        onFiltersChange(filters.copy(selectedCountries = newCountries))
                    },
                    icon = {
                        Text(text = flag, fontSize = 20.sp)
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                FilterRow(
                    label = "All world",
                    isSelected = filters.allWorld,
                    onToggle = {
                        onFiltersChange(filters.copy(allWorld = !filters.allWorld))
                    },
                    icon = {
                        Text(text = "🍱", fontSize = 20.sp) // Placeholder for "All world" icon
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.07f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSelectAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFF787B86),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "SELECT ALL",
            color = Color(0xFF2962FF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onSelectAll() }
        )
    }
}

@Composable
private fun FilterRow(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF2962FF),
                uncheckedColor = Color(0xFF363A45),
                checkmarkColor = Color.White
            )
        )
    }
}

private fun importanceColor(importance: String): Color {
    return when (importance.lowercase()) {
        "high" -> Color(0xFFF23645)
        "medium" -> Color(0xFFFFC857)
        "low" -> Color(0xFF6B7280)
        else -> Color.Transparent
    }
}
