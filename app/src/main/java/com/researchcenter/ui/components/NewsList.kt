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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp)
                ) {
                    // Title Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            article.title,
                            color = White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDate(articleTime),
                            color = Gray400.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

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
    return try {
        OffsetDateTime.parse(dateStr)
    } catch (e: Exception) {
        null
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)

private fun formatDate(dateTime: OffsetDateTime?): String {
    if (dateTime == null) return "---"
    return try {
        dateTime.format(dateFormatter)
    } catch (e: Exception) {
        "---"
    }
}


