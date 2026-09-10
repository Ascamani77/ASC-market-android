package com.asc.markets.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

private const val APP_TIMEZONE_ID = "Africa/Lagos"

data class MarketHours(
    val name: String,
    val type: String, // "Forex", "Crypto", "Indices", "Commodities"
    val status: MarketStatus,
    val openTime: String,
    val closeTime: String,
    val timezone: String,
    val description: String
)

enum class MarketStatus {
    OPEN, CLOSED, PRE_MARKET, AFTER_HOURS, WEEKEND
}

data class MarketHoliday(
    val date: String,
    val name: String,
    val markets: List<String>,
    val type: String // "Full Day", "Half Day", "Early Close"
)

@Composable
fun MarketStatusScreen() {
    // Use Nigerian time (WAT - West Africa Time, UTC+1)
    val marketZone = remember { ZoneId.of(APP_TIMEZONE_ID) }
    var currentTime by remember { mutableStateOf(LocalDateTime.now(marketZone)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = LocalDateTime.now(marketZone)
        }
    }

    LaunchedEffect(refreshTick) {
        if (refreshTick == 0) return@LaunchedEffect
        currentTime = LocalDateTime.now(marketZone)
        delay(450)
        isRefreshing = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val markets = remember(currentTime) { getMarketHours(currentTime) }
    val upcomingHolidays = remember(currentTime) { getUpcomingHolidays(currentTime) }
    var holidaysExpanded by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Market Status",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        currentTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy • HH:mm:ss")) + " GMT+1",
                        color = SlateText,
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = {
                        isRefreshing = true
                        refreshTick += 1
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isRefreshing) rotation else 0f)
                    )
                }
            }
        }

        // Market Hours Section
        item {
            Text(
                "Live Market Hours",
                color = IndigoAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(markets) { market ->
            MarketCard(market, currentTime)
        }

        // Holidays Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { holidaysExpanded = !holidaysExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Upcoming Market Holidays",
                    color = IndigoAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (holidaysExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (holidaysExpanded) "Collapse" else "Expand",
                    tint = IndigoAccent
                )
            }
        }

        if (holidaysExpanded && upcomingHolidays.isNotEmpty()) {
            items(upcomingHolidays) { holiday ->
                HolidayCard(holiday)
            }
        }

        // Info Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                onClick = { infoExpanded = !infoExpanded },
                color = Color(0xFF1A1D2E),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = IndigoAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Market Hours Information",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            if (infoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (infoExpanded) "Collapse" else "Expand",
                            tint = IndigoAccent
                        )
                    }
                    
                    if (infoExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "• Crypto markets operate 24/7 with no holidays\n" +
                            "• Forex is normalized to the New York close (Sun 5 PM - Fri 5 PM ET)\n" +
                            "• US stocks use NYSE/NASDAQ hours in America/New_York\n" +
                            "• Futures and commodities use CME Globex hours in America/Chicago\n" +
                            "• Holidays may vary by exchange and region\n" +
                            "• Session status is based on exchange-local time",
                            color = SlateText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketCard(market: MarketHours, currentTime: LocalDateTime) {
    var isExpanded by remember { mutableStateOf(false) }
    val status = remember(market, currentTime) { resolveMarketSchedule(market, currentTime) }

    val statusColor = when (status.status) {
        MarketStatus.OPEN -> EmeraldSuccess
        MarketStatus.CLOSED -> RoseError
        MarketStatus.PRE_MARKET, MarketStatus.AFTER_HOURS -> Color(0xFFFFA500)
        MarketStatus.WEEKEND -> SlateText
    }

    val statusIcon = when (status.status) {
        MarketStatus.OPEN -> Icons.Default.CheckCircle
        MarketStatus.CLOSED -> Icons.Default.Cancel
        MarketStatus.PRE_MARKET, MarketStatus.AFTER_HOURS -> Icons.Default.Schedule
        MarketStatus.WEEKEND -> Icons.Default.EventBusy
    }

    Surface(
        onClick = { isExpanded = !isExpanded },
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Always visible header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        market.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        market.type,
                        color = SlateText,
                        fontSize = 13.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        status.status.name.replace("_", " "),
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Collapsible details
            if (isExpanded) {
                HorizontalDivider(color = HairlineBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Opens",
                            color = SlateText,
                            fontSize = 12.sp
                        )
                        Text(
                            market.openTime,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Closes",
                            color = SlateText,
                            fontSize = 12.sp
                        )
                        Text(
                            market.closeTime,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    market.description,
                    color = SlateText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    status.detailText,
                    color = IndigoAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    "Timezone: ${market.timezone}",
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HolidayCard(holiday: MarketHoliday) {
    Surface(
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    holiday.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    holiday.date,
                    color = SlateText,
                    fontSize = 13.sp
                )
                Text(
                    "Affected: ${holiday.markets.joinToString(", ")}",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }

            Surface(
                color = Color(0xFF2C0B0B),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    holiday.type,
                    color = RoseError,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

fun getMarketHours(currentTime: LocalDateTime): List<MarketHours> {
    // Convert market times to Nigerian time (GMT+1)
    // Forex: NY time (Sunday 5:00 PM ET = Monday 12:00 AM WAT, Friday 5:00 PM ET = Saturday 12:00 AM WAT)
    // ET is UTC-5 (standard) or UTC-4 (daylight), WAT is UTC+1
    // Difference: WAT is 6 hours ahead of ET (standard) or 5 hours (daylight)
    
    return listOf(
        MarketHours(
            name = "Cryptocurrency Markets",
            type = "Crypto (BTC, ETH, etc.)",
            status = resolveMarketSchedule(
                MarketHours("Cryptocurrency Markets", "Crypto (BTC, ETH, etc.)", MarketStatus.OPEN, "24/7", "Never", "Africa/Lagos", ""),
                currentTime
            ).status,
            openTime = "24/7",
            closeTime = "Never",
            timezone = "GMT+1 (Nigerian Time)",
            description = "Cryptocurrency markets operate continuously without breaks or holidays. Trading is available around the clock."
        ),
        MarketHours(
            name = "Forex Session (New York Close)",
            type = "Foreign Exchange",
            status = resolveMarketSchedule(
                MarketHours("Forex Session (New York Close)", "Foreign Exchange", MarketStatus.CLOSED, "Monday 12:00 AM", "Saturday 12:00 AM", "Africa/Lagos", ""),
                currentTime
            ).status,
            openTime = "Monday 12:00 AM",
            closeTime = "Saturday 12:00 AM",
            timezone = "GMT+1 (Nigerian Time)",
            description = "Forex is a decentralized global market. Opens Monday midnight (Sunday 5 PM ET) and closes Saturday midnight (Friday 5 PM ET) in Nigerian time."
        ),
        MarketHours(
            name = "US Stock Markets",
            type = "Equities",
            status = resolveMarketSchedule(
                MarketHours("US Stock Markets", "Equities", MarketStatus.CLOSED, "3:30 PM", "10:00 PM", "Africa/Lagos", ""),
                currentTime
            ).status,
            openTime = "3:30 PM",
            closeTime = "10:00 PM",
            timezone = "GMT+1 (Nigerian Time)",
            description = "NYSE and NASDAQ regular trading runs 3:30 PM-10:00 PM WAT, with pre-market from 10:00 AM and after-hours until 2:00 AM WAT."
        ),
        MarketHours(
            name = "Commodities (CME Globex)",
            type = "Commodities",
            status = resolveMarketSchedule(
                MarketHours("Commodities (CME Globex)", "Commodities", MarketStatus.CLOSED, "Monday 12:00 AM", "Saturday 11:00 PM", "Africa/Lagos", ""),
                currentTime
            ).status,
            openTime = "Monday 12:00 AM",
            closeTime = "Saturday 11:00 PM",
            timezone = "GMT+1 (Nigerian Time)",
            description = "Major commodity futures trade on CME Globex, generally Monday 12:00 AM WAT through Saturday 11:00 PM WAT with a daily maintenance break."
        ),
        MarketHours(
            name = "Index Futures (CME Globex)",
            type = "Futures",
            status = resolveMarketSchedule(
                MarketHours("Index Futures (CME Globex)", "Futures", MarketStatus.CLOSED, "Monday 12:00 AM", "Saturday 11:00 PM", "Africa/Lagos", ""),
                currentTime
            ).status,
            openTime = "Monday 12:00 AM",
            closeTime = "Saturday 11:00 PM",
            timezone = "GMT+1 (Nigerian Time)",
            description = "Index futures on CME Globex follow the Chicago session: Monday 12:00 AM WAT open, Saturday 11:00 PM WAT close, with a daily 11:00 PM-12:00 AM WAT maintenance break."
        ),
        MarketHours(
            name = "US Treasury / Bond Market",
            type = "Fixed Income",
            status = resolveMarketSchedule(
                MarketHours("US Treasury / Bond Market", "Fixed Income", MarketStatus.CLOSED, "2:00 PM", "11:00 PM", "Africa/Lagos", ""),
                currentTime
            ).status,
            openTime = "2:00 PM",
            closeTime = "11:00 PM",
            timezone = "GMT+1 (Nigerian Time)",
            description = "US Treasury trading is strongest during New York business hours, roughly 2:00 PM-11:00 PM WAT on weekdays."
        )
    )
}

private data class MarketScheduleSnapshot(
    val status: MarketStatus,
    val detailText: String
)

private fun resolveMarketSchedule(market: MarketHours, currentTime: LocalDateTime): MarketScheduleSnapshot {
    val appZone = ZoneId.of(APP_TIMEZONE_ID) // Nigerian time
    // All calculations are done in Nigerian time since that's what the user sees
    val localTime = currentTime
    val time = localTime.toLocalTime()
    val name = market.name.lowercase(Locale.US)
    val type = market.type.lowercase(Locale.US)

    return when {
        type.contains("crypto") || name.contains("crypto") -> MarketScheduleSnapshot(
            status = MarketStatus.OPEN,
            detailText = "Open 24/7 • No scheduled close"
        )

        type.contains("foreign exchange") || name.contains("forex") -> {
            // Forex: Opens Monday 00:00 WAT, Closes Saturday 00:00 WAT
            // Closed: All of Saturday and all of Sunday
            val isWeekend = localTime.dayOfWeek == DayOfWeek.SATURDAY || 
                           localTime.dayOfWeek == DayOfWeek.SUNDAY
            
            val isOpen = !isWeekend
            
            // Calculate next midnight or next market open
            val nextMidnight = localTime.toLocalDate().plusDays(1).atTime(0, 0)
            val mondayOpen = nextWeeklyDateTime(localTime, DayOfWeek.MONDAY, 0, 0)
            val saturdayClose = when (localTime.dayOfWeek) {
                DayOfWeek.SATURDAY -> sameDayDateTime(localTime, 0, 0)
                else -> nextWeeklyDateTime(localTime, DayOfWeek.SATURDAY, 0, 0)
            }

            MarketScheduleSnapshot(
                status = if (isOpen) MarketStatus.OPEN else MarketStatus.CLOSED,
                detailText = if (isOpen) {
                    // Show time until end of current trading day (midnight or Saturday close)
                    if (localTime.dayOfWeek == DayOfWeek.FRIDAY) {
                        "Closes in ${formatCountdown(localTime, saturdayClose)}"
                    } else {
                        "Resets in ${formatCountdown(localTime, nextMidnight)}"
                    }
                } else {
                    "Opens in ${formatCountdown(localTime, mondayOpen)}"
                }
            )
        }

        type.contains("equities") || name.contains("stock") -> {
            // US Stocks: 9:30 AM ET = 3:30 PM WAT, 4:00 PM ET = 10:00 PM WAT
            // Pre-market: 4:00 AM ET = 10:00 AM WAT, After hours: 8:00 PM ET = 2:00 AM WAT (next day)
            val preMarketOpen = sameDayDateTime(localTime, 10, 0)
            val regularOpen = sameDayDateTime(localTime, 15, 30)
            val regularClose = sameDayDateTime(localTime, 22, 0)
            val afterHoursClose = localTime.toLocalDate().plusDays(1).atTime(2, 0)
            val nextPreMarketOpen = when (localTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> nextWeeklyDateTime(localTime, DayOfWeek.MONDAY, 10, 0)
                DayOfWeek.FRIDAY -> if (time >= LocalTime.of(2, 0)) nextWeeklyDateTime(localTime, DayOfWeek.MONDAY, 10, 0) else preMarketOpen
                else -> if (time < LocalTime.of(10, 0)) preMarketOpen else localTime.toLocalDate().plusDays(1).atTime(10, 0)
            }

            val status = when (localTime.dayOfWeek) {
                DayOfWeek.SATURDAY -> MarketStatus.CLOSED
                DayOfWeek.SUNDAY -> MarketStatus.CLOSED
                DayOfWeek.FRIDAY -> when {
                    time < LocalTime.of(10, 0) -> MarketStatus.CLOSED
                    time < LocalTime.of(15, 30) -> MarketStatus.PRE_MARKET
                    time < LocalTime.of(22, 0) -> MarketStatus.OPEN
                    else -> MarketStatus.AFTER_HOURS
                }
                DayOfWeek.MONDAY -> when {
                    time < LocalTime.of(2, 0) -> MarketStatus.AFTER_HOURS // From Friday
                    time < LocalTime.of(10, 0) -> MarketStatus.CLOSED
                    time < LocalTime.of(15, 30) -> MarketStatus.PRE_MARKET
                    time < LocalTime.of(22, 0) -> MarketStatus.OPEN
                    else -> MarketStatus.AFTER_HOURS
                }
                else -> when {
                    time < LocalTime.of(2, 0) -> MarketStatus.AFTER_HOURS
                    time < LocalTime.of(10, 0) -> MarketStatus.CLOSED
                    time < LocalTime.of(15, 30) -> MarketStatus.PRE_MARKET
                    time < LocalTime.of(22, 0) -> MarketStatus.OPEN
                    else -> MarketStatus.AFTER_HOURS
                }
            }

            val detailText = when (status) {
                MarketStatus.OPEN -> "Closes in ${formatCountdown(localTime, regularClose)}"
                MarketStatus.PRE_MARKET -> "Regular session in ${formatCountdown(localTime, regularOpen)}"
                MarketStatus.AFTER_HOURS -> "After-hours ends in ${formatCountdown(localTime, afterHoursClose)}"
                else -> "Opens in ${formatCountdown(localTime, nextPreMarketOpen)}"
            }

            MarketScheduleSnapshot(status = status, detailText = detailText)
        }

        type.contains("commodities") || type.contains("futures") || name.contains("cme") -> {
            // CME: Sunday 5 PM CT = Monday 12 AM WAT, Friday 4 PM CT = Saturday 11 PM WAT
            // Maintenance: 4-5 PM CT = 11 PM-12 AM WAT
            val mondayOpen = nextWeeklyDateTime(localTime, DayOfWeek.MONDAY, 0, 0)
            val saturdayClose = nextWeeklyDateTime(localTime, DayOfWeek.SATURDAY, 23, 0)
            val maintenanceBreak = when {
                time < LocalTime.of(23, 0) -> sameDayDateTime(localTime, 23, 0)
                else -> localTime.toLocalDate().plusDays(1).atTime(0, 0)
            }

            val status = when (localTime.dayOfWeek) {
                DayOfWeek.SUNDAY -> MarketStatus.CLOSED
                DayOfWeek.MONDAY -> if (time >= LocalTime.of(0, 0)) MarketStatus.OPEN else MarketStatus.CLOSED
                DayOfWeek.SATURDAY -> if (time < LocalTime.of(23, 0)) MarketStatus.OPEN else MarketStatus.CLOSED
                else -> if (time in LocalTime.of(23, 0)..LocalTime.of(23, 59, 59)) MarketStatus.CLOSED else MarketStatus.OPEN
            }

            val detailText = when (status) {
                MarketStatus.OPEN -> when (localTime.dayOfWeek) {
                    DayOfWeek.SATURDAY -> "Closes in ${formatCountdown(localTime, saturdayClose)}"
                    else -> "Maintenance break in ${formatCountdown(localTime, maintenanceBreak)}"
                }
                else -> when (localTime.dayOfWeek) {
                    DayOfWeek.SUNDAY -> "Opens in ${formatCountdown(localTime, mondayOpen)}"
                    DayOfWeek.SATURDAY -> "Opens in ${formatCountdown(localTime, mondayOpen)}"
                    else -> "Reopens in ${formatCountdown(localTime, localTime.toLocalDate().plusDays(1).atTime(0, 0))}"
                }
            }

            MarketScheduleSnapshot(status = status, detailText = detailText)
        }

        type.contains("fixed income") || name.contains("treasury") || name.contains("bond") -> {
            // US Treasury: 8 AM ET = 2 PM WAT, 5 PM ET = 11 PM WAT
            val openTime = sameDayDateTime(localTime, 14, 0)
            val closeTime = sameDayDateTime(localTime, 23, 0)
            val nextOpen = when (localTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> nextWeeklyDateTime(localTime, DayOfWeek.MONDAY, 14, 0)
                else -> if (time < LocalTime.of(14, 0)) openTime else localTime.toLocalDate().plusDays(1).atTime(14, 0)
            }

            val status = when (localTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> MarketStatus.CLOSED
                else -> if (time in LocalTime.of(14, 0)..LocalTime.of(22, 59, 59)) MarketStatus.OPEN else MarketStatus.CLOSED
            }

            MarketScheduleSnapshot(
                status = status,
                detailText = if (status == MarketStatus.OPEN) {
                    "Closes in ${formatCountdown(localTime, closeTime)}"
                } else {
                    "Opens in ${formatCountdown(localTime, nextOpen)}"
                }
            )
        }

        else -> MarketScheduleSnapshot(
            status = market.status,
            detailText = "Schedule unavailable"
        )
    }
}

private fun getUpcomingHolidays(currentTime: LocalDateTime): List<MarketHoliday> {
    val currentYear = currentTime.year
    
    // Generate holidays for current year and next 2 years dynamically
    val allHolidays = mutableListOf<MarketHoliday>()
    
    for (year in currentYear..(currentYear + 2)) {
        allHolidays.addAll(generateHolidaysForYear(year))
    }
    
    // Filter to show only upcoming holidays (from current date forward)
    return allHolidays.filter { holiday ->
        val holidayDate = parseHolidayDate(holiday.date)
        val nowDate = currentTime.toLocalDate()
        holidayDate != null && !holidayDate.toLocalDate().isBefore(nowDate)
    }.take(10) // Show next 10 upcoming holidays
}

private fun formatCountdown(currentTime: LocalDateTime, targetTime: LocalDateTime): String {
    val duration = Duration.between(currentTime, targetTime)
    if (duration.isZero || duration.isNegative) return "now"

    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return buildString {
        if (hours > 0) {
            append(hours)
            append("h")
            if (minutes > 0) append(' ')
        }
        if (minutes > 0 || hours == 0L) {
            append(minutes)
            append("m")
        }
    }
}

private fun sameDayDateTime(currentTime: LocalDateTime, hour: Int, minute: Int): LocalDateTime {
    return currentTime.toLocalDate().atTime(hour, minute).withSecond(0).withNano(0)
}

private fun nextWeeklyDateTime(currentTime: LocalDateTime, dayOfWeek: DayOfWeek, hour: Int, minute: Int): LocalDateTime {
    var candidate = currentTime
        .with(TemporalAdjusters.nextOrSame(dayOfWeek))
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)

    if (!candidate.isAfter(currentTime)) {
        candidate = candidate.plusWeeks(1)
    }

    return candidate
}

private fun generateHolidaysForYear(year: Int): List<MarketHoliday> {
    val holidays = mutableListOf<MarketHoliday>()
    
    // New Year's Day - January 1 (or observed on Monday if falls on weekend)
    val newYear = java.time.LocalDate.of(year, 1, 1)
    val newYearObserved = when (newYear.dayOfWeek) {
        DayOfWeek.SATURDAY -> newYear.plusDays(2)
        DayOfWeek.SUNDAY -> newYear.plusDays(1)
        else -> newYear
    }
    holidays.add(MarketHoliday(
        date = newYearObserved.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = if (newYearObserved != newYear) "New Year's Day (Observed)" else "New Year's Day",
        markets = listOf("US Stocks", "Bonds", "Forex (Reduced Liquidity)"),
        type = "Full Day"
    ))
    
    // Martin Luther King Jr. Day - Third Monday in January
    val mlkDay = java.time.LocalDate.of(year, 1, 1)
        .with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY))
    holidays.add(MarketHoliday(
        date = mlkDay.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Martin Luther King Jr. Day",
        markets = listOf("US Stocks", "Bonds"),
        type = "Full Day"
    ))
    
    // Presidents' Day - Third Monday in February
    val presidentsDay = java.time.LocalDate.of(year, 2, 1)
        .with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY))
    holidays.add(MarketHoliday(
        date = presidentsDay.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Presidents' Day",
        markets = listOf("US Stocks", "Bonds"),
        type = "Full Day"
    ))
    
    // Good Friday - Friday before Easter (complex calculation)
    val goodFriday = calculateGoodFriday(year)
    holidays.add(MarketHoliday(
        date = goodFriday.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Good Friday",
        markets = listOf("US Stocks", "Bonds"),
        type = "Full Day"
    ))
    
    // Memorial Day - Last Monday in May
    val memorialDay = java.time.LocalDate.of(year, 5, 31)
        .with(java.time.temporal.TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))
    holidays.add(MarketHoliday(
        date = memorialDay.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Memorial Day",
        markets = listOf("US Stocks", "Bonds", "Commodities"),
        type = "Full Day"
    ))
    
    // Juneteenth - June 19 (or observed on Monday/Friday if falls on weekend)
    val juneteenth = java.time.LocalDate.of(year, 6, 19)
    val juneteenthObserved = when (juneteenth.dayOfWeek) {
        DayOfWeek.SATURDAY -> juneteenth.minusDays(1)
        DayOfWeek.SUNDAY -> juneteenth.plusDays(1)
        else -> juneteenth
    }
    holidays.add(MarketHoliday(
        date = juneteenthObserved.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = if (juneteenthObserved != juneteenth) "Juneteenth (Observed)" else "Juneteenth",
        markets = listOf("US Stocks", "Bonds"),
        type = "Full Day"
    ))
    
    // Independence Day - July 4 (or observed on Monday/Friday if falls on weekend)
    val july4 = java.time.LocalDate.of(year, 7, 4)
    val july4Observed = when (july4.dayOfWeek) {
        DayOfWeek.SATURDAY -> july4.minusDays(1)
        DayOfWeek.SUNDAY -> july4.plusDays(1)
        else -> july4
    }
    holidays.add(MarketHoliday(
        date = july4Observed.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = if (july4Observed != july4) "Independence Day (Observed)" else "Independence Day",
        markets = listOf("US Stocks", "Bonds", "Commodities"),
        type = "Full Day"
    ))
    
    // Labor Day - First Monday in September
    val laborDay = java.time.LocalDate.of(year, 9, 1)
        .with(java.time.temporal.TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
    holidays.add(MarketHoliday(
        date = laborDay.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Labor Day",
        markets = listOf("US Stocks", "Bonds", "Commodities"),
        type = "Full Day"
    ))
    
    // Thanksgiving Day - Fourth Thursday in November
    val thanksgiving = java.time.LocalDate.of(year, 11, 1)
        .with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
    holidays.add(MarketHoliday(
        date = thanksgiving.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Thanksgiving Day",
        markets = listOf("US Stocks", "Bonds", "Commodities"),
        type = "Full Day"
    ))
    
    // Day After Thanksgiving - Friday after Thanksgiving (Early Close)
    val dayAfterThanksgiving = thanksgiving.plusDays(1)
    holidays.add(MarketHoliday(
        date = dayAfterThanksgiving.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = "Day After Thanksgiving",
        markets = listOf("US Stocks", "Bonds"),
        type = "Early Close (6:00 PM GMT+1)"
    ))
    
    // Christmas Day - December 25 (or observed on Monday/Friday if falls on weekend)
    val christmas = java.time.LocalDate.of(year, 12, 25)
    val christmasObserved = when (christmas.dayOfWeek) {
        DayOfWeek.SATURDAY -> christmas.minusDays(1)
        DayOfWeek.SUNDAY -> christmas.plusDays(1)
        else -> christmas
    }
    holidays.add(MarketHoliday(
        date = christmasObserved.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
        name = if (christmasObserved != christmas) "Christmas Day (Observed)" else "Christmas Day",
        markets = listOf("US Stocks", "Bonds", "Commodities"),
        type = "Full Day"
    ))
    
    return holidays.sortedBy { parseHolidayDate(it.date) }
}

// Calculate Good Friday using the Computus algorithm (Easter calculation)
private fun calculateGoodFriday(year: Int): java.time.LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = ((h + l - 7 * m + 114) % 31) + 1
    
    // Easter Sunday
    val easter = java.time.LocalDate.of(year, month, day)
    // Good Friday is 2 days before Easter
    return easter.minusDays(2)
}

private fun parseHolidayDate(dateStr: String): LocalDateTime? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
        val date = java.time.LocalDate.parse(dateStr, formatter)
        date.atStartOfDay()
    } catch (e: Exception) {
        null
    }
}
