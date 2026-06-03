package com.asc.markets.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ChartAnalysisViewModel
import com.asc.markets.logic.ChartAnalysisUiState
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.logic.ANALYST_MODELS
import com.asc.markets.data.remote.RunAiResponse
import com.asc.markets.ui.theme.*

@Composable
fun ChartAnalysisScreen(
    forexViewModel: ForexViewModel = viewModel(),
    analysisViewModel: ChartAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by analysisViewModel.uiState.collectAsState()
    val selectedBitmap by analysisViewModel.selectedBitmap.collectAsState()
    val chartDescription by analysisViewModel.chartDescription.collectAsState()
    val activePersonaId by forexViewModel.ascChatPersonaId.collectAsState()
    val selectedPersona = ANALYST_MODELS.firstOrNull { it.id == activePersonaId } ?: ANALYST_MODELS[0]

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            analysisViewModel.onImageSelected(bitmap)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        Text(
            "CHART ANALYSIS NODE",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "ASC VISION INTELLIGENCE — DESCRIBE YOUR CHART",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chart Description Input (Primary)
            item {
                Surface(
                    color = DeepBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("YOUR CHART ANALYSIS", color = IndigoAccent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Describe what you see: Symbol, Timeframe, Pattern, Levels, Your Analysis",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = chartDescription,
                            onValueChange = { analysisViewModel.onChartDescriptionChanged(it) },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            placeholder = { 
                                Text(
                                    "Example:\nBTCUSD 1H chart showing bullish flag pattern.\nPrice consolidated after strong upward move.\nKey support at 73,500 held multiple tests.\nBreakout above 73,800 with volume.\nLooking for continuation to 75,000.",
                                    color = Color.Gray.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IndigoAccent,
                                unfocusedBorderColor = HairlineBorder
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily
                            )
                        )
                    }
                }
            }
            
            // Optional: Chart Image Upload (for future vision AI)
            item {
                Surface(
                    color = DeepBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHART IMAGE (OPTIONAL)", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("For future vision AI", color = Color.Gray.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (selectedBitmap == null) {
                            UploadPlaceholder(onClick = { launcher.launch("image/*") })
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                Image(
                                    bitmap = selectedBitmap!!.asImageBitmap(),
                                    contentDescription = "Uploaded Chart",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                        .background(Color.Black, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                IconButton(
                                    onClick = { analysisViewModel.clearImage() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (chartDescription.isNotBlank() && uiState is ChartAnalysisUiState.Idle) {
                item {
                    Button(
                        onClick = { 
                            analysisViewModel.analyzeChart(
                                personaName = selectedPersona.name,
                                personaInstruction = selectedPersona.instruction,
                                forexViewModel = forexViewModel
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUN ASC VISION ANALYSIS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (val state = uiState) {
                is ChartAnalysisUiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = IndigoAccent)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("ASC AI ANALYZING PIXELS...", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                is ChartAnalysisUiState.Success -> {
                    item {
                        AnalysisSummaryCard(state.summary)
                    }
                    item {
                        SectionHeader("RAW ASC AI PAYLOAD")
                        RawPayloadCard(state.rawPayload)
                    }
                }
                is ChartAnalysisUiState.Error -> {
                    item {
                        Surface(
                            color = Color(0xFF2C0B0B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                state.message,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun UploadPlaceholder(onClick: () -> Unit) {
    Surface(
        color = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("TAP TO UPLOAD", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("(Optional - for future vision AI)", color = Color.Gray.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
fun AnalysisSummaryCard(summary: String) {
    Surface(
        color = DeepBlack,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ASC ENGINE v1 VERDICT", color = IndigoAccent, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(summary, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
fun RawPayloadCard(response: RunAiResponse) {
    Surface(
        color = Color(0xFF0A0B0F),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            response.final_decision.forEach { decision ->
                Text("Asset: ${decision.asset_1 ?: "N/A"}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Bias: ${decision.journal_direction ?: "N/A"} (${decision.journal_label ?: "N/A"})", color = IndigoAccent)
                Text("Confidence: ${decision.direction_confidence ?: 0.0}", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
