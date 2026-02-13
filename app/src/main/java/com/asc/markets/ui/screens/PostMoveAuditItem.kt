package com.asc.markets.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.asc.markets.data.AuditRecord
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PostMoveAuditItem(
    entry: AuditRecord,
    expanded: SnapshotStateMap<String, Boolean>,
    viewModel: ForexViewModel,
    context: Context
) {
    val coroutineScope = rememberCoroutineScope()
    val isExpanded = expanded[entry.id] ?: false

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = PureBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.assets, color = SlateText, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                        val impactColor = when (entry.impact) {
                            "CRITICAL" -> RoseError
                            "INFO" -> SlateText.copy(alpha = 0.7f)
                            else -> IndigoAccent
                        }
                        Text(entry.impact, color = impactColor, modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry.status.uppercase(), color = if (entry.status.equals("UPCOMING", true)) Color(0xFFB06A00) else Color(0xFF0F6F52), modifier = Modifier.padding(end = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val confidenceInt = try { entry.confidence.toInt() } catch (e: Throwable) { null }
                    val confidenceColor = when {
                        confidenceInt != null && confidenceInt >= 60 -> EmeraldSuccess
                        confidenceInt != null && confidenceInt <= 40 -> RoseError
                        else -> Color(0xFFFFC107)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("[ ", color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily))
                        Text("${entry.confidence}%", color = confidenceColor, fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                        Text(" ] confidence", color = SlateText, style = TerminalTypography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily))
                    }
                    Spacer(modifier = Modifier.height(35.dp))

                    Text(entry.headline, color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, fontFamily = InterFontFamily), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                    Box(modifier = Modifier.wrapContentWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(fmt.format(Instant.ofEpochMilli(entry.timeUtc)), color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            if (entry.status.equals("ACTIVE", true)) {
                                val infiniteTransition = rememberInfiniteTransition()
                                val blinkAlpha by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 0.3f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 1000),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF3B30).copy(alpha = blinkAlpha), shape = CircleShape))
                            }
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!isExpanded) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { expanded[entry.id] = true }) {
                        Text("VIEW CONTEXT ›", color = IndigoAccent, style = TerminalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, letterSpacing = 0.08.em))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                        Text("ANALYTICAL REASONING", color = SlateText, style = TerminalTypography.labelSmall.copy(letterSpacing = 1.sp, fontFamily = InterFontFamily))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(entry.reasoning, color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, fontFamily = InterFontFamily))

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUDIT TRACE LOG", color = SlateText, style = TerminalTypography.labelSmall.copy(fontFamily = InterFontFamily))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("Insight Node", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Insight Node", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF070707))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("INTEGRITY CHECK", color = SlateText, fontSize = 10.sp, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("VERIFIED", color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        InfoBox(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ANALYTICAL CONTEXT", color = SlateText, fontSize = 12.sp, fontFamily = InterFontFamily)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(buildExpandedAnalyticalContext(entry, "Insight Node"), color = Color.White, style = Typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp, fontFamily = InterFontFamily))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val pdf = exportAuditPdf(context, entry)
                                    if (pdf != null) {
                                        withContext(Dispatchers.Main) { sharePdf(context, pdf) }
                                    } else {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                Icon(Icons.Default.FileCopy, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EXPORT AUDIT PDF", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            }

                            Button(onClick = { viewModel.markAuditRecordAudited(entry.id); Toast.makeText(context, "Marked audited", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("MARK AUDITED", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            }

                            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))) }, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0B0B))) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TS_MICRO: ${entry.timeUtc}", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                            Text("SEQ_ID: ${entry.id.take(3).uppercase()}", color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.White.copy(alpha = 0.04f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("LOG::${entry.id.take(3).uppercase()}", color = SlateText.copy(alpha = 0.6f), style = TerminalTypography.labelSmall.copy(fontSize = 11.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium))
                            TextButton(onClick = { expanded[entry.id] = false }) { Text("MINIMIZE CONTEXT", color = IndigoAccent, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}
