package com.asc.markets.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            Text(
                "Upcoming Market Holidays",
                color = IndigoAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(upcomingHolidays) { holiday ->
            HolidayCard(holiday)
        }

        // Info Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard()
        }
    }
}

@Composable
fun MarketCard(market: MarketHours, currentTime: LocalDateTime) {
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
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

@Composable
fun InfoCard() {
    Surface(
        color = Color(0xFF1A1D2E),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = IndigoAccent,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Market Hours Information",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
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

fun getMarketHours(currentTime: LocalDateTime): List<MarketHours> {
    return listOf(
        MarketHours(
            name = "Cryptocurrency Markets",
            type = "Crypto (BTC, ETH, etc.)",
            status = resolveMarketSchedule(
                MarketHours("Cryptocurrency Markets", "Crypto (BTC, ETH, etc.)", MarketStatus.OPEN, "24/7", "Never", "UTC", ""),
                currentTime
            ).status,
            openTime = "24/7",
            closeTime = "Never",
            timezone = "UTC",
            description = "Cryptocurrency markets operate continuously without breaks or holidays. Trading is available around the clock."
        ),
        MarketHours(
            name = "Forex Session (New York Close)",
            type = "Foreign Exchange",
            status = resolveMarketSchedule(
                MarketHours("Forex Session (New York Close)", "Foreign Exchange", MarketStatus.CLOSED, "Sunday 5:00 PM", "Friday 5:00 PM", "America/New_York", ""),
                currentTime
            ).status,
            openTime = "Sunday 5:00 PM",
            closeTime = "Friday 5:00 PM",
            timezone = "America/New_York",
            description = "Forex is a decentralized global market, tracked here using the New York close convention (Sunday 5:00 PM to Friday 5:00 PM ET)."
        ),
        MarketHours(
            name = "US Stock Markets (NYSE / NASDAQ)",
            type = "Equities",
            status = resolveMarketSchedule(
                MarketHours("US Stock Markets (NYSE / NASDAQ)", "Equities", MarketStatus.CLOSED, "9:30 AM", "4:00 PM", "America/New_York", ""),
                currentTime
            ).status,
            openTime = "9:30 AM",
            closeTime = "4:00 PM",
            timezone = "America/New_York",
            description = "NYSE and NASDAQ regular trading runs 9:30 AM-4:00 PM ET, with pre-market from 4:00 AM and after-hours until 8:00 PM ET."
        ),
        MarketHours(
            name = "Commodities (CME Globex)",
            type = "Commodities",
            status = resolveMarketSchedule(
                MarketHours("Commodities (CME Globex)", "Commodities", MarketStatus.CLOSED, "Sunday 5:00 PM", "Friday 4:00 PM", "America/Chicago", ""),
                currentTime
            ).status,
            openTime = "Sunday 5:00 PM",
            closeTime = "Friday 4:00 PM",
            timezone = "America/Chicago",
            description = "Major commodity futures trade on CME Globex, generally Sunday 5:00 PM CT through Friday 4:00 PM CT with a daily maintenance break."
        ),
        MarketHours(
            name = "Index Futures (CME Globex)",
            type = "Futures",
            status = resolveMarketSchedule(
                MarketHours("Index Futures (CME Globex)", "Futures", MarketStatus.CLOSED, "Sunday 5:00 PM", "Friday 4:00 PM", "America/Chicago", ""),
                currentTime
            ).status,
            openTime = "Sunday 5:00 PM",
            closeTime = "Friday 4:00 PM",
            timezone = "America/Chicago",
            description = "Index futures on CME Globex follow the Chicago session: Sunday 5:00 PM CT open, Friday 4:00 PM CT close, with a daily 4:00-5:00 PM CT maintenance break."
        ),
        MarketHours(
            name = "US Treasury / Bond Market",
            type = "Fixed Income",
            status = resolveMarketSchedule(
                MarketHours("US Treasury / Bond Market", "Fixed Income", MarketStatus.CLOSED, "8:00 AM", "5:00 PM", "America/New_York", ""),
                currentTime
            ).status,
            openTime = "8:00 AM",
            closeTime = "5:00 PM",
            timezone = "America/New_York",
            description = "US Treasury trading is strongest during New York business hours, roughly 8:00 AM-5:00 PM ET on weekdays."
        )
    )
}

private data class MarketScheduleSnapshot(
    val status: MarketStatus,
    val detailText: String
)

private fun resolveMarketSchedule(market: MarketHours, currentTime: LocalDateTime): MarketScheduleSnapshot {
    val appZone = ZoneId.of(APP_TIMEZONE_ID)
    val exchangeZone = runCatching { ZoneId.of(market.timezone) }.getOrElse { appZone }
    val exchangeTime = currentTime.atZone(appZone).withZoneSameInstant(exchangeZone).toLocalDateTime()
    val time = exchangeTime.toLocalTime()
    val name = market.name.lowercase(Locale.US)
    val type = market.type.lowercase(Locale.US)

    return when {
        type.contains("crypto") || name.contains("crypto") -> MarketScheduleSnapshot(
            status = MarketStatus.OPEN,
            detailText = "Open 24/7 • No scheduled close"
        )

        type.contains("foreign exchange") || name.contains("forex") -> {
            val sundayOpen = nextWeeklyDateTime(exchangeTime, DayOfWeek.SUNDAY, 17, 0)
            val fridayClose = nextWeeklyDateTime(exchangeTime, DayOfWeek.FRIDAY, 17, 0)
            val isOpen = when (exchangeTime.dayOfWeek) {
                DayOfWeek.SATURDAY -> false
                DayOfWeek.SUNDAY -> time >= LocalTime.of(17, 0)
                DayOfWeek.FRIDAY -> time < LocalTime.of(17, 0)
                else -> true
            }

            MarketScheduleSnapshot(
                status = if (isOpen) MarketStatus.OPEN else MarketStatus.CLOSED,
                detailText = if (isOpen) {
                    "Closes in ${formatCountdown(exchangeTime, fridayClose)}"
                } else {
                    "Opens in ${formatCountdown(exchangeTime, sundayOpen)}"
                }
            )
        }

        type.contains("equities") || name.contains("stock") -> {
            val preMarketOpen = sameDayDateTime(exchangeTime, 4, 0)
            val regularOpen = sameDayDateTime(exchangeTime, 9, 30)
            val regularClose = sameDayDateTime(exchangeTime, 16, 0)
            val afterHoursClose = sameDayDateTime(exchangeTime, 20, 0)
            val nextPreMarketOpen = when (exchangeTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.FRIDAY -> nextWeeklyDateTime(exchangeTime, DayOfWeek.MONDAY, 4, 0)
                else -> if (time < LocalTime.of(4, 0)) preMarketOpen else exchangeTime.toLocalDate().plusDays(1).atTime(4, 0)
            }

            val status = when (exchangeTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> MarketStatus.CLOSED
                else -> when {
                    time < LocalTime.of(4, 0) -> MarketStatus.CLOSED
                    time < LocalTime.of(9, 30) -> MarketStatus.PRE_MARKET
                    time < LocalTime.of(16, 0) -> MarketStatus.OPEN
                    time < LocalTime.of(20, 0) -> MarketStatus.AFTER_HOURS
                    else -> MarketStatus.CLOSED
                }
            }

            val detailText = when (status) {
                MarketStatus.OPEN -> "Closes in ${formatCountdown(exchangeTime, regularClose)}"
                MarketStatus.PRE_MARKET -> "Regular session in ${formatCountdown(exchangeTime, regularOpen)}"
                MarketStatus.AFTER_HOURS -> "After-hours ends in ${formatCountdown(exchangeTime, afterHoursClose)}"
                else -> "Opens in ${formatCountdown(exchangeTime, nextPreMarketOpen)}"
            }

            MarketScheduleSnapshot(status = status, detailText = detailText)
        }

        type.contains("commodities") || type.contains("futures") || name.contains("cme") -> {
            val sundayOpen = nextWeeklyDateTime(exchangeTime, DayOfWeek.SUNDAY, 17, 0)
            val fridayClose = nextWeeklyDateTime(exchangeTime, DayOfWeek.FRIDAY, 16, 0)
            val maintenanceBreak = when {
                time < LocalTime.of(16, 0) -> sameDayDateTime(exchangeTime, 16, 0)
                time >= LocalTime.of(17, 0) -> exchangeTime.toLocalDate().plusDays(1).atTime(16, 0)
                else -> sameDayDateTime(exchangeTime, 17, 0)
            }

            val status = when (exchangeTime.dayOfWeek) {
                DayOfWeek.SATURDAY -> MarketStatus.CLOSED
                DayOfWeek.SUNDAY -> if (time >= LocalTime.of(17, 0)) MarketStatus.OPEN else MarketStatus.CLOSED
                DayOfWeek.FRIDAY -> if (time < LocalTime.of(16, 0)) MarketStatus.OPEN else MarketStatus.CLOSED
                else -> if (time in LocalTime.of(16, 0)..LocalTime.of(16, 59, 59)) MarketStatus.CLOSED else MarketStatus.OPEN
            }

            val detailText = when (status) {
                MarketStatus.OPEN -> when (exchangeTime.dayOfWeek) {
                    DayOfWeek.FRIDAY -> "Closes in ${formatCountdown(exchangeTime, fridayClose)}"
                    else -> "Maintenance break in ${formatCountdown(exchangeTime, maintenanceBreak)}"
                }
                else -> when (exchangeTime.dayOfWeek) {
                    DayOfWeek.SATURDAY -> "Opens in ${formatCountdown(exchangeTime, sundayOpen)}"
                    DayOfWeek.SUNDAY -> "Opens in ${formatCountdown(exchangeTime, sameDayDateTime(exchangeTime, 17, 0))}"
                    DayOfWeek.FRIDAY -> "Opens in ${formatCountdown(exchangeTime, sundayOpen)}"
                    else -> "Reopens in ${formatCountdown(exchangeTime, sameDayDateTime(exchangeTime, 17, 0))}"
                }
            }

            MarketScheduleSnapshot(status = status, detailText = detailText)
        }

        type.contains("fixed income") || name.contains("treasury") || name.contains("bond") -> {
            val openTime = sameDayDateTime(exchangeTime, 8, 0)
            val closeTime = sameDayDateTime(exchangeTime, 17, 0)
            val nextOpen = when (exchangeTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> nextWeeklyDateTime(exchangeTime, DayOfWeek.MONDAY, 8, 0)
                else -> if (time < LocalTime.of(8, 0)) openTime else exchangeTime.toLocalDate().plusDays(1).atTime(8, 0)
            }

            val status = when (exchangeTime.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> MarketStatus.CLOSED
                else -> if (time in LocalTime.of(8, 0)..LocalTime.of(16, 59, 59)) MarketStatus.OPEN else MarketStatus.CLOSED
            }

            MarketScheduleSnapshot(
                status = status,
                detailText = if (status == MarketStatus.OPEN) {
                    "Closes in ${formatCountdown(exchangeTime, closeTime)}"
                } else {
                    "Opens in ${formatCountdown(exchangeTime, nextOpen)}"
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
