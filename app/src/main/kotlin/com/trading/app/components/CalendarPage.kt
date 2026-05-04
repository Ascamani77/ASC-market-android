package com.trading.app.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.models.CalendarDayChip
import com.trading.app.models.EconomicCalendarDisplayEvent
import com.trading.app.models.EconomicCalendarDisplayPayload
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

@Composable
fun CalendarPage(
    payload: EconomicCalendarDisplayPayload?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectDate: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    BackHandler(onBack = onBack)

    val dayChips = payload?.dayChips ?: emptyList()
    val selectedDateIso = payload?.selectedDateIso.orEmpty()
    val events = payload?.events?.filter { it.isoDateTime.startsWith(selectedDateIso) } ?: emptyList()
    val monthLabel = payload?.rangeStartIso?.let(::formatMonthLabel).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        CalendarHeader(
            title = "Tradays",
            onBack = onBack,
            onRefresh = onRefresh
        )

        MonthStripHeader(
            monthLabel = monthLabel,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )

        DayStrip(
            dayChips = dayChips,
            onSelectDate = onSelectDate
        )

        when {
            isLoading && events.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }

            events.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No MT5 calendar events for this day",
                        color = Color(0xFF787B86),
                        fontSize = 15.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        CalendarEventRow(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthStripHeader(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = Color.White
            )
        }
        Text(
            text = monthLabel,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    title: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "Refresh calendar",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun DayStrip(
    dayChips: List<CalendarDayChip>,
    onSelectDate: (String) -> Unit
) {
    if (dayChips.isEmpty()) {
        return
    }

    val listState = rememberLazyListState()
    val selectedIndex = dayChips.indexOfFirst { it.isSelected }

    LaunchedEffect(selectedIndex, dayChips.size) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(max(selectedIndex - 1, 0))
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(dayChips, key = { _, chip -> chip.isoDate }) { _, chip ->
            Column(
                modifier = Modifier
                    .clickable { onSelectDate(chip.isoDate) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = chip.dayLabel,
                    color = if (chip.isSelected) Color.White else Color(0xFF7F8796),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = if (chip.isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip.dayNumber.toString(),
                            color = if (chip.isSelected) Color.Black else Color(0xFFD1D4DC),
                            fontSize = 18.sp,
                            fontWeight = if (chip.isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    Divider(
        color = Color.White.copy(alpha = 0.18f),
        thickness = 1.dp
    )
}

@Composable
private fun CalendarEventRow(event: EconomicCalendarDisplayEvent) {
    val actualColor = when (event.impactDirection) {
        1 -> Color(0xFF36C275)
        2 -> Color(0xFFF25F5C)
        else -> Color(0xFFE8EAED)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(106.dp)
                .background(importanceColor(event.importance))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = event.title,
                color = Color(0xFFE8EAED),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.width(56.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    FlagBadge(countryCode = event.countryCode)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = event.releaseTimeLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                MetricValueColumn(
                    modifier = Modifier.weight(1f),
                    value = event.actual,
                    label = "Actual",
                    valueColor = actualColor
                )
                MetricValueColumn(
                    modifier = Modifier.weight(1f),
                    value = event.forecast,
                    label = "Forecast",
                    valueColor = Color(0xFFE8EAED)
                )
                MetricValueColumn(
                    modifier = Modifier.weight(1f),
                    value = event.previous,
                    label = "Previous",
                    valueColor = Color(0xFFE8EAED)
                )
            }
        }
    }

    Divider(color = Color(0xFF121212), thickness = 0.5.dp)
}

@Composable
private fun MetricValueColumn(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.ifBlank { "--" },
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF8D95A5),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FlagBadge(countryCode: String) {
    val flag = countryFlagEmoji(countryCode)

    if (flag != null) {
        Text(
            text = flag,
            fontSize = 18.sp
        )
        return
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF263238))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countryCode.ifBlank { "GL" },
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun countryFlagEmoji(countryCode: String): String? {
    if (countryCode.length != 2) {
        return null
    }

    val upper = countryCode.uppercase()
    if (!upper.all { it in 'A'..'Z' }) {
        return null
    }

    val first = Character.codePointAt(upper, 0) - 'A'.code + 0x1F1E6
    val second = Character.codePointAt(upper, 1) - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

private fun importanceColor(importance: String): Color {
    return when (importance.lowercase()) {
        "high" -> Color(0xFFF23645)
        "medium" -> Color(0xFFFFC857)
        "low" -> Color(0xFF6B7280)
        else -> Color(0xFF434651)
    }
}

private fun formatMonthLabel(isoDate: String): String {
    return runCatching {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("MMMM yyyy", Locale.US)
        output.format(input.parse(isoDate)!!)
    }.getOrDefault("")
}
