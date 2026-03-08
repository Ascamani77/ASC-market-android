package com.researchcenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.researchcenter.data.models.NewsArticle
import com.researchcenter.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun NewsList(
    articles: List<NewsArticle>,
    onArticleClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit,
    bookmarkedIds: Set<String>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 0.dp)
    ) {
        itemsIndexed(articles) { index, article ->
            NewsCard(
                article = article,
                index = index,
                isBookmarked = bookmarkedIds.contains(article.id),
                onClick = { onArticleClick(article) },
                onBookmarkClick = { onBookmarkClick(article) }
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun NewsCard(
    article: NewsArticle,
    index: Int,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val articleTime = remember(article.publishedAt) { parseDateTime(article.publishedAt) }
    val isPast = remember(article, articleTime) { isPastArticle(article) }
    
    val isImminent = !isPast && articleTime != null && (
        article.category == "calendar" || 
        article.category == "macro_cal" || 
        article.intelligence?.asset_tags?.any { it.contains("SCHEDULE") } == true
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .alpha(if (isPast) 0.4f else 1f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Title Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isPast) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFDC2626))
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            article.title,
                            color = if (isPast) Gray400 else White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp,
                        )
                    }

                    Spacer(Modifier.height(2.dp))
                    
                    // Labels/Subtitles
                    Text(
                        "Time left Impact Previous Consensus Actual",
                        color = Gray400.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (isImminent) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SidebarBg)
                                .border(0.5.dp, White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "IMMINENT SCHEDULE",
                                color = Gray400,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Labels Column (moved up into the main block)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "PRE EVENT SCHEDULE",
                            color = Gray400.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(2.dp).clip(RoundedCornerShape(1.dp)).background(Gray400.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "MACRO",
                            color = Gray400.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(2.dp).clip(RoundedCornerShape(1.dp)).background(Gray400.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "LOW",
                            color = Color(0xFF3B82F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDate(articleTime),
                            color = Gray400.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (isImminent) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "IMMINENT",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else if (isPast) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PASSED",
                                color = Gray400.copy(alpha = 0.3f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SidebarBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                article.source.uppercase(),
                                color = Gray400.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Image and Countdown Column (properly aligned to top right)
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 4.dp)) {
                    val imageModel = if (!article.imageUrl.isNullOrEmpty()) {
                        article.imageUrl
                    } else {
                        "https://loremflickr.com/320/180/business,finance?lock=${article.id.hashCode()}"
                    }

                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        colorFilter = if (isPast) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                        modifier = Modifier
                            .size(width = 120.dp, height = 75.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SidebarBg),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (isImminent && articleTime != null) {
                        Spacer(Modifier.height(4.dp))
                        CountdownTimer(articleTime)
                    }
                }
            }
        }

        Icon(
            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) White else Gray400.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(18.dp)
                .clickable { onBookmarkClick() }
        )
    }
}

@Composable
fun CountdownTimer(targetTime: OffsetDateTime) {
    var timeLeft by remember { mutableStateOf("") }

    LaunchedEffect(targetTime) {
        while (true) {
            val now = Instant.now()
            val target = targetTime.toInstant()
            val duration = Duration.between(now, target)

            if (duration.isNegative || duration.isZero) {
                timeLeft = "00:00:00"
                break
            } else {
                val days = duration.toDays()
                val hours = duration.toHours() % 24
                val minutes = duration.toMinutes() % 60
                val seconds = duration.seconds % 60
                
                timeLeft = if (days > 0) {
                    String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
            }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF064E3B))
            .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.0.dp))
                    .background(Color(0xFF10B981))
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "T- $timeLeft",
                color = Color(0xFF10B981),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

private fun parseDateTime(dateStr: String): OffsetDateTime? {
    if (dateStr.isEmpty()) return null
    
    // Try ISO-8601 first
    try {
        return OffsetDateTime.parse(dateStr)
    } catch (e: Exception) { }

    // Try RFC 1123 (Common in RSS, e.g., "Wed, 11 Mar 2026 09:30:00 +0000")
    try {
        val rfc1123Formatter = DateTimeFormatter.RFC_1123_DATE_TIME
        return OffsetDateTime.parse(dateStr, rfc1123Formatter)
    } catch (e: Exception) { }

    // Custom fallback for other common formats
    val formats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd HH:mm:ss",
        "d MMM, HH:mm"
    )
    for (format in formats) {
        try {
            val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
            return OffsetDateTime.parse(dateStr, formatter)
        } catch (e: Exception) {
            try {
                val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
                val ldt = java.time.LocalDateTime.parse(dateStr, formatter)
                return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime()
            } catch (e2: Exception) { }
        }
    }

    return null
}

private fun formatDate(dateTime: OffsetDateTime?): String {
    if (dateTime == null) return "---"
    return try {
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
        dateTime.format(formatter)
    } catch (e: Exception) {
        "---"
    }
}

private fun isPastArticle(article: NewsArticle): Boolean {
    if (article.publishedAt.isEmpty()) return false
    
    val tags = article.intelligence?.asset_tags ?: emptyList()
    val isCalendar = article.category == "calendar" || article.category == "macro_cal" || tags.contains("PRE_EVENT_SCHEDULE")

    return try {
        val now = System.currentTimeMillis()
        val date = parseDateTime(article.publishedAt) ?: return false
        val timestamp = date.toInstant().toEpochMilli()

        // Logic for vague dates (e.g., "May 2025" defaults to May 1st)
        // If it's a calendar item but lacks a specific time (no ":"), 
        // don't mark it as past if the month hasn't ended.
        val hasSpecificTime = article.publishedAt.contains(":")
        if (isCalendar && !hasSpecificTime) {
            val currentCalendar = Calendar.getInstance()
            val articleCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            
            if (articleCalendar.get(Calendar.YEAR) > currentCalendar.get(Calendar.YEAR)) return false
            if (articleCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) && 
                articleCalendar.get(Calendar.MONTH) >= currentCalendar.get(Calendar.MONTH)) return false
        }

        timestamp < now
    } catch (e: Exception) {
        false
    }
}
