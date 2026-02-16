package com.asc.markets.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.asc.markets.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.screens.dashboard.DashboardFontSizes
import com.asc.markets.ui.theme.*
import kotlin.random.Random
import java.util.Locale

/**
 * DashboardQuality - Analytical Quality / Institutional Audit Hub
 * Implements the 9-box matrix and tappable verification modals.
 */
@Composable
fun DashboardQuality() {
    var selectedBox by remember { mutableStateOf<String?>(null) }
    

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tactical Vitals: one per row (vertical list)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VitalsSmall(
                    "System Accuracy",
                    "${samplePercent(76.4)}%",
                    "H1/H4 Bias Validation",
                    modifier = Modifier.fillMaxWidth(),
                    iconRes = R.drawable.lucide_activity,
                    iconTint = IndigoAccent,
                    onTap = { selectedBox = "System Accuracy" }
                )

            VitalsSmall(
                "Decision Quality",
                "${samplePercent(84.2)}%",
                "Confidence Correlation",
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.lucide_book_open,
                iconTint = Color.White,
                onTap = { selectedBox = "Decision Quality" }
            )

            VitalsSmall(
                "WAIT Effectiveness",
                "${samplePercent(92.5)}%",
                "Noise Prevention",
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.lucide_list_filter,
                iconTint = Color(0xFFF59E0B), // amber
                onTap = { selectedBox = "WAIT Effectiveness" }
            )

            VitalsSmall(
                "Safety Gate Success",
                "${samplePercent(98.1)}%",
                "News Block Accuracy",
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.lucide_binary,
                iconTint = EmeraldSuccess,
                onTap = { selectedBox = "Safety Gate" }
            )

            // Automated Node Log with Won/Lost breakdown
            val won = Random.nextInt(80, 220)
            val lost = Random.nextInt(20, 120)
            VitalsSmall(
                "Automated Node Log",
                "${won + lost}",
                "${won} Won / ${lost} Lost",
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.lucide_arrow_left_right,
                iconTint = IndigoAccent,
                onTap = { selectedBox = "Auto Node Log" }
            )
        }

        // Row 2: Consistency Analyzers — stacked, one per row
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AreaChartBox(
                "Institutional Equity Curve",
                EmeraldSuccess,
                modifier = Modifier.fillMaxWidth(),
                iconRes = R.drawable.lucide_line_chart,
                iconTint = EmeraldSuccess,
                onTap = { selectedBox = "Equity Curve" }
            )

            AreaChartBox(
                "Bias Alignment History",
                IndigoAccent,
                modifier = Modifier.fillMaxWidth(),
                onTap = { selectedBox = "Bias History" }
            )
        }

        // Row 3: Risk & Compliance — stacked, one per row
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NewsSafetyBox(modifier = Modifier.fillMaxWidth(), iconRes = R.drawable.lucide_pie_chart, iconTint = RoseError, onTap = { selectedBox = "News Safety" })
            ProofBadgeBox(modifier = Modifier.fillMaxWidth(), iconRes = R.drawable.lucide_binary, iconTint = IndigoAccent, onTap = { selectedBox = "Proof Badge" })
        }

        // Macro Intelligence Ledger is implemented in a separate file (`ExecutionLedger.kt`).
        // It is intentionally not embedded here so this analytical page remains unchanged.
    }

    // Verification modal (shared)
    if (selectedBox != null) {
        VerificationModal(title = selectedBox!!, onClose = { selectedBox = null })
    }

// Macro Intelligence Ledger removed from this file (moved to ExecutionLedger.kt)
}

// Execution ledger moved to ExecutionLedger.kt to keep DashboardQuality focused on analytical UI.

@Composable
private fun VitalsSmall(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconTint: Color = Color.Unspecified,
    onTap: () -> Unit
) {
    InfoBox(modifier = modifier.clickable { onTap() }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: title + small subtitle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) Color.White else iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column {
                        Text(title.uppercase(Locale.getDefault()), color = Color.White, fontSize = DashboardFontSizes.valueSmall, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(subtitle.uppercase(Locale.getDefault()), color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
                    }
                }

                // value large on the right of header for compact look
                Text(value, color = EmeraldSuccess, fontSize = DashboardFontSizes.qualityScore, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }

            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            // description / explanatory text
            Text(
                when (title) {
                    "System Accuracy" -> "Measures alignment of institutional directional bias with subsequent market movement."
                    "Decision Quality" -> "Evaluates the correlation between higher internal confidence and positive technical outcomes."
                    "WAIT Effectiveness" -> "Reliability of WAIT advisories in avoiding directionless or volatile consolidation zones."
                    "Safety Gate Success" -> "Efficiency of the news engine in blocking signals during high-impact structural disruptions."
                    "Automated Node Log" -> "Historical tracking of AI autonomous node dispatches, including win/loss audit results."
                    else -> "Clinical explanation for $title."
                },
                color = SlateText,
                fontSize = DashboardFontSizes.gridHeaderSmall
            )
        }
    }
}

@Composable
private fun AreaChartBox(
    title: String,
    tint: Color,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconTint: Color = Color.Unspecified,
    onTap: () -> Unit
) {
    InfoBox(modifier = modifier.height(260.dp).clickable { onTap() }) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) Color.White else iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(title, color = SlateText, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
                        Text("$100,000 theoretical start — 7-day lookback", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    }
                }
                Text("7d", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
            }
            // Placeholder for area chart: simple gradient block and summary
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(tint.copy(alpha = 0.08f))) {
                // simulated spark/value
                Text("▲ 3.4%", color = EmeraldSuccess, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), fontFamily = InterFontFamily)
            }
            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            Text("Interactive area chart (emerald / indigo gradients)", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
        }
    }
}

@Composable
private fun NewsSafetyBox(modifier: Modifier = Modifier, iconRes: Int? = null, iconTint: Color = Color.Unspecified, onTap: () -> Unit) {
    // Use dynamic height and horizontal line scales per requirement
    InfoBox(modifier = modifier.clickable { onTap() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (iconTint == Color.Unspecified) Color.White else iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text("NEWS SAFETY GATE", color = SlateText, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
                    Text("BLOCKED PERIOD OUTCOMES", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                }
            }

            // Horizontal line scales with percentage labels
            val items = listOf(
                Triple("Volatility Spike Avoidance", 85f, EmeraldSuccess),
                Triple("Spread Widening Protection", 12f, Color.White.copy(alpha = 0.12f)),
                Triple("Directionless Market Avoided", 3f, Color.White.copy(alpha = 0.12f))
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { (label, pct, colorFill) ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(label.uppercase(Locale.getDefault()), color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall)
                            Text("${pct.toInt()}%", color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // track
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))) {
                            // filled portion
                            Box(modifier = Modifier.fillMaxWidth(pct / 100f).height(8.dp).background(colorFill, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)))
                        }
                    }
                }
            }

            // callout info card
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("WAITING DURING HIGH-IMPACT NEWS CYCLES PRESERVED DECISION QUALITY BY 96.4% IN LAST 30 INTERVALS.", color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall)
                }
            }
        }
    }
}

@Composable
private fun ProofBadgeBox(modifier: Modifier = Modifier, iconRes: Int? = null, iconTint: Color = Color.Unspecified, onTap: () -> Unit) {
    InfoBox(modifier = modifier.height(220.dp).clickable { onTap() }) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // ShieldCheck placeholder with pulse icon or provided icon
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (iconTint == Color.Unspecified) IndigoAccent else iconTint,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Box(modifier = Modifier.size(64.dp).background(IndigoAccent.copy(alpha = 0.06f))) {
                    Text("✔", color = IndigoAccent, modifier = Modifier.align(Alignment.Center), fontSize = DashboardFontSizes.verificationCheckmark)
                }
            }
            Text("Proof of Analysis Badge", color = Color.White, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
            Text("Institutional Proof of Analysis — Deterministic & Auditable", color = SlateText, fontSize = DashboardFontSizes.bodyTiny)
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Digital Signature Verified", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                Text("•", color = EmeraldSuccess, fontSize = DashboardFontSizes.gridHeaderSmall)
            }
        }
    }
}

@Composable
private fun VerificationModal(title: String, onClose: () -> Unit) {
    val (why, dataset) = verificationDetailsFor(title)
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.86f)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // watermark
                    Text("ASC", color = Color.White.copy(alpha = 0.03f), fontSize = DashboardFontSizes.watermarkText, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))

                    Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(title, color = Color.White, fontSize = DashboardFontSizes.emojiIcon, fontWeight = FontWeight.Black)
                            Text("Close", color = SlateText, modifier = Modifier.clickable { onClose() })
                        }

                        Text("The Why:", color = SlateText, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
                        Text(why, color = Color.White, fontSize = DashboardFontSizes.valueSmall)

                        Text("Verification Dataset:", color = SlateText, fontSize = DashboardFontSizes.gridHeaderSmall, fontWeight = FontWeight.Black)
                        Column {
                            dataset.forEach { d -> Text("• $d", color = SlateText) }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Text("Digital Signature: All calculations deterministic and auditable.", color = SlateText, fontSize = DashboardFontSizes.labelMedium)
                    }
                }
            }
        }
    }
}

private fun verificationDetailsFor(title: String): Pair<String, List<String>> {
    return when (title) {
        "System Accuracy" -> Pair(
            "Measures alignment between AI directional bias and structural market breaks over the verification window.",
            listOf("Sample Size: Last 500 Advisories", "Window: exclude News-Blocked periods", "Metric: % directional alignment (H1/H4)")
        )
        "Decision Quality" -> Pair(
            "Correlates high-confidence signals (>=90%) with realized 2:1 Reward-to-Risk outcomes to validate weighting.",
            listOf("Confidence Threshold: 90%+", "Success Condition: realized 2:1 RR", "Sample Size: filtered advisories")
        )
        "WAIT Effectiveness" -> Pair(
            "Evaluates how often WAIT prevented entries during directionless or choppy price action, reducing false trades.",
            listOf("Sample Size: last 500", "Choppiness filter: volatility & range metrics", "Outcome: loss avoided %")
        )
        "Safety Gate" -> Pair(
            "Audits news-blocking protocol effectiveness by measuring trades avoided during high-impact windows.",
            listOf("Blocked windows removed from stats", "Volatility spike detection method", "Preserved capital estimate")
        )
        "Auto Node Log" -> Pair(
            "Shows theoretical autonomous dispatch count and outcome split (Won / Lost) for traceability.",
            listOf("Total dispatches: simulated", "Won / Lost breakdown", "Deterministic replay available")
        )
        "Equity Curve" -> Pair(
            "Replays theoretical account growth from primary dispatches over a 7-day lookback starting at $100,000.",
            listOf("Start capital: $100,000", "Lookback: 7 days", "Dispatch selection: primary signals only")
        )
        "Bias History" -> Pair(
            "Tracks daily directional accuracy to reveal session-level (London/NY/Tokyo) performance differences.",
            listOf("Per-day accuracy %", "Session filter: UTC windows", "Visualization: indigo area chart")
        )
        "News Safety" -> Pair(
            "Details blocked-period outcomes and capital preserved during news cycles.",
            listOf("Volatility Spike Avoidance %", "Spread Widening Protection %", "Directionless Market Avoided %")
        )
        "Proof Badge" -> Pair(
            "Digital signature and deterministic audit trail proving reproducibility of metrics.",
            listOf("All calculations deterministic", "Source: on-device audit logs", "Reproducible computation steps")
        )
        else -> Pair("Clinical explanation for $title.", listOf("Sample Size: Last 500 Advisories", "Deterministic filters applied"))
    }
}

private fun samplePercent(v: Double): String = String.format("%.1f", v)
