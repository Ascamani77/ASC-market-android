package com.asc.markets.ui.screens.tradeDashboard.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.screens.tradeDashboard.model.RiskLevel
import com.asc.markets.ui.screens.tradeDashboard.ui.components.AISettingsPanel
import com.asc.markets.ui.screens.tradeDashboard.viewmodel.DashboardViewModel

/**
 * EXPLANATION TAB - "Why did the system decide this?"
 * Shows: Gemini/OpenAI narrative explanation only (NOT decision-making)
 * AI decision-making comes from internal ASC engine, this is presentation only
 */
@Composable
fun ExplanationTab(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val dividerColor = Color(0xFF151515)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 48.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM EXPLANATION",
                color = Color(0xFF6366F1),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // Important Disclaimer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6366F1).copy(alpha = 0.1f))
                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "💡 Explanation Tool: External AI (Gemini/OpenAI) explains ASC's internal decisions. Explanations are for transparency only and do NOT influence trade decisions.",
                color = Color(0xFF6366F1),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 12.sp
            )
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 1. Advisory Explanation
        viewModel.advisory?.let {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CURRENT SIGNAL RATIONALE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Bias: ${it.bias.name}",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Risk Level: ${it.riskLevel.name}",
                            color = when (it.riskLevel) {
                                RiskLevel.HIGH -> Color(0xFFFF6B6B)
                                RiskLevel.MEDIUM -> Color(0xFFFAA61A)
                                else -> Color(0xFF00C853)
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Confidence: ${it.confidence}%",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 2. Gemini/OpenAI Explanation
        ExplanationContent(viewModel)

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // 3. AI Settings (for selecting explanation model, temperature, etc)
        AISettingsPanel(
            settings = viewModel.aiSettings,
            onSettingsChanged = { viewModel.updateAISettings(it) },
            onOpenSettings = { /* open extended settings */ }
        )
    }
}

@Composable
private fun ExplanationContent(viewModel: DashboardViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "AI NARRATIVE EXPLANATION",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Placeholder - will be filled by Gemini/OpenAI response
                Text(
                    text = "Generating explanation...",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show loading or actual explanation
                if (viewModel.isLoading) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.dp,
                            color = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Building explanation...",
                            color = Color(0xFF6366F1),
                            fontSize = 9.sp
                        )
                    }
                } else {
                    // Actual explanation would go here
                    Text(
                        text = """
                            The current market setup shows a confluence of:
                            
                            1. Technical Confluence: Price is at the intersection of 200-MA and key S/R level
                            2. Risk/Reward: Entry offers 1:3 risk-reward setup with tight stops
                            3. Volatility Context: Current ATR supports sizing
                            4. Macro Alignment: USD bias supports entry direction
                            
                            Decision Rationale:
                            • Internal ASC engine identified this setup based on pattern recognition
                            • System confidence at 78% due to strong signal confluence
                            • Portfolio heat acceptable for new position
                            
                            This explanation is provided by external AI for transparency.
                            All decisions originate from ASC's internal logic.
                        """.trimIndent(),
                        color = Color.White,
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
