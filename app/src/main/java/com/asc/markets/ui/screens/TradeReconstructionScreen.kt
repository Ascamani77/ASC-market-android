package com.asc.markets.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asc.markets.logic.ForexViewModel
import com.asc.markets.ui.theme.DeepBlack
import com.asc.markets.ui.theme.IndigoAccent
import com.asc.markets.ui.theme.PureBlack
import com.asc.markets.ui.theme.SlateText
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TradeReconstructionScreen(viewModel: ForexViewModel = viewModel()) {
    val selectedAudit = remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = PureBlack) {
        Column(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
            if (selectedAudit.value == null) {
                // List view
                ListAuditView(onAuditSelected = { selectedAudit.value = it })
            } else {
                // Detail view
                DetailAuditView(
                    pair = selectedAudit.value!!,
                    onClose = { selectedAudit.value = null }
                )
            }
        }
    }
}

@Composable
fun ListAuditView(onAuditSelected: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Document icon with magnifying glass
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color(0xFF0F3A7D),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "📋",
                            fontSize = 32.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "CLINICAL\nRECONSTRUCTION",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "FORENSIC MARKET AUDIT TRAIL",
                        color = SlateText,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Historical Audit Buffer Label
        item {
            Text(
                "HISTORICAL AUDIT BUFFER",
                color = SlateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Audit Trail Cards
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onAuditSelected("EUR/USD") },
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Fingerprint icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF4A4A7A),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("�", fontSize = 20.sp)
                        }

                        // Asset icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔗", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                "EUR/USD",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "WON AUDIT",
                                color = Color(0xFF2EE08A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("❯", color = SlateText, fontSize = 20.sp)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onAuditSelected("XAU/USDT") },
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF4A4A7A),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("�", fontSize = 20.sp)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                "XAU/USDT",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "LOST AUDIT",
                                color = Color(0xFFE53935),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("❯", color = SlateText, fontSize = 20.sp)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onAuditSelected("BTC/USDT") },
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF4A4A7A),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("�", fontSize = 20.sp)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₿", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                "BTC/USDT",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "WON AUDIT",
                                color = Color(0xFF2EE08A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("❯", color = SlateText, fontSize = 20.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DetailAuditView(pair: String, onClose: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClose() }
                    .padding(8.dp)
            ) {
                Text(
                    "<",
                    color = SlateText,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Back to Deep Audit",
                    color = SlateText,
                    fontSize = 12.sp
                )
            }
        }

        // Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                color = PureBlack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF0F3A7D),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("〰️", fontSize = 20.sp)
                        }

                        Column {
                            Text(
                                "CASE RECONSTRUCTION: T-842",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "AUDIT STATUS: IMMUTABLE HISTORICAL\nSNAPSHOT",
                                color = SlateText,
                                fontSize = 9.sp,
                                lineHeight = 11.sp
                            )
                        }
                    }

                    Text("✕", color = SlateText, fontSize = 18.sp, modifier = Modifier.clickable { onClose() })
                }
            }
        }

        // Institutional Bias @ Entry
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "INSTITUTIONAL BIAS @ ENTRY",
                    color = IndigoAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp)
                    ),
                color = PureBlack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "DETECTION\nSTATE",
                            color = SlateText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 11.sp
                        )

                        Text(
                            "CONFIRMED\nACCUMULATION",
                            color = Color(0xFF2EE08A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 13.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "\"THE SURVEILLANCE NODE IDENTIFIED INSTITUTIONAL BUY ORDERS AT 1.0842 FOLLOWING A RETAIL LIQUIDITY FLUSH. HTF ALIGNMENT WAS 92% BULLISH.\"",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Safety Gate @ Entry
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "SAFETY GATE @ ENTRY",
                    color = IndigoAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp)
                    ),
                color = PureBlack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "NEWS WINDOW",
                            color = SlateText,
                            fontSize = 10.sp
                        )
                        Text(
                            "CLEAR_WINDOW_UTC",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "NEXT HIGH IMPACT",
                            color = SlateText,
                            fontSize = 10.sp
                        )
                        Text(
                            "4H 12M",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Order Flow Reconstruction
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "ORDER FLOW RECONSTRUCTION",
                    color = IndigoAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp)
                    ),
                color = PureBlack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "VISUAL FRAGMENT CAPTURED",
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = Color(0xFF1A1A2E),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("OFI DELTA", color = SlateText, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("+14.2%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = Color(0xFF1A1A2E),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SUCTION", color = SlateText, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("UPWARD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = Color(0xFF1A1A2E),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = Color(0xFF1A1A2E),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SLIPPAGE", color = SlateText, fontSize = 9.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("0.0 pips", color = Color(0xFF2EE08A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Disclosure
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PureBlack,
                        shape = RoundedCornerShape(8.dp)
                    ),
                color = PureBlack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ℹ️", fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))

                    Text(
                        "RECONSTRUCTION DATA IS GENERATED FROM THE LOCAL NODE BUFFER AND VERIFIED AGAINST GLOBAL INSTITUTIONAL PRINTS. THIS RECORD IS IMMUTABLE AND SERVES AS PRIMARY EVIDENCE FOR MODEL PERFORMANCE AUDIT.",
                        color = SlateText,
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        // Export Button
        item {
            val context = LocalContext.current
            Button(
                onClick = {
                    exportForensicPdf(context, pair)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "EXPORT\nFORENSIC PDF",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

data class AuditTrail(
    val pair: String,
    val status: String,
    val statusColor: Color,
    val icon: String
)

fun exportForensicPdf(context: Context, pair: String) {
    try {
        val fileName = "ForensicAudit_${pair.replace("/", "_")}_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pdfFile = File(downloadsDir, fileName)

        val writer = PdfWriter(pdfFile)
        val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = Document(pdfDocument)

        // Set margins
        document.setMargins(20f, 20f, 20f, 20f)

        // Header
        val headerParagraph = Paragraph("CASE RECONSTRUCTION: T-842")
            .setFontSize(18f)
            .setFontColor(ColorConstants.BLACK)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(headerParagraph)

        val statusParagraph = Paragraph("AUDIT STATUS: IMMUTABLE HISTORICAL SNAPSHOT")
            .setFontSize(10f)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(statusParagraph)

        document.add(Paragraph(" "))

        // Institutional Bias Section
        val biasParagraph = Paragraph("INSTITUTIONAL BIAS @ ENTRY")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.BLUE)
        document.add(biasParagraph)

        val biasTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
        biasTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("DETECTION STATE").setFont(null).setFontSize(9f)
            )
        )
        biasTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("CONFIRMED ACCUMULATION").setFont(null).setFontSize(9f).setBold()
            )
        )
        document.add(biasTable)

        val biasDetail = Paragraph(
            "THE SURVEILLANCE NODE IDENTIFIED INSTITUTIONAL BUY ORDERS AT 1.0842 " +
            "FOLLOWING A RETAIL LIQUIDITY FLUSH. HTF ALIGNMENT WAS 92% BULLISH."
        ).setFontSize(10f).setItalic()
        document.add(biasDetail)

        document.add(Paragraph(" "))

        // Safety Gate Section
        val safetyParagraph = Paragraph("SAFETY GATE @ ENTRY")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.BLUE)
        document.add(safetyParagraph)

        val safetyTable = Table(UnitValue.createPercentArray(2)).useAllAvailableWidth()
        safetyTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("NEWS WINDOW").setFontSize(9f)
            )
        )
        safetyTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("CLEAR_WINDOW_UTC").setFontSize(9f).setBold()
            )
        )
        safetyTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("NEXT HIGH IMPACT").setFontSize(9f)
            )
        )
        safetyTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("4H 12M").setFontSize(9f).setBold()
            )
        )
        document.add(safetyTable)

        document.add(Paragraph(" "))

        // Order Flow Section
        val orderFlowParagraph = Paragraph("ORDER FLOW RECONSTRUCTION")
            .setFontSize(12f)
            .setBold()
            .setFontColor(ColorConstants.BLUE)
        document.add(orderFlowParagraph)

        val metricsTable = Table(UnitValue.createPercentArray(3)).useAllAvailableWidth()
        metricsTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("OFI DELTA").setFontSize(9f).setBold()
            )
        )
        metricsTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("SUCTION").setFontSize(9f).setBold()
            )
        )
        metricsTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("SLIPPAGE").setFontSize(9f).setBold()
            )
        )
        metricsTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("+14.2%").setFontSize(10f)
            )
        )
        metricsTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("UPWARD").setFontSize(10f)
            )
        )
        metricsTable.addCell(
            com.itextpdf.layout.element.Cell().add(
                Paragraph("0.0 pips").setFontSize(10f)
            )
        )
        document.add(metricsTable)

        document.add(Paragraph(" "))

        // Disclosure
        val disclosureParagraph = Paragraph(
            "RECONSTRUCTION DATA IS GENERATED FROM THE LOCAL NODE BUFFER AND VERIFIED " +
            "AGAINST GLOBAL INSTITUTIONAL PRINTS. THIS RECORD IS IMMUTABLE AND SERVES AS " +
            "PRIMARY EVIDENCE FOR MODEL PERFORMANCE AUDIT."
        ).setFontSize(9f).setFontColor(ColorConstants.DARK_GRAY)
        document.add(disclosureParagraph)

        document.add(Paragraph(" "))

        // Footer with timestamp
        val footer = Paragraph(
            "Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"
        ).setFontSize(8f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER)
        document.add(footer)

        document.close()

        // Share the PDF
        val uri = Uri.fromFile(pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Forensic PDF"))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
