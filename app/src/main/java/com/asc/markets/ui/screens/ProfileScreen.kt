package com.asc.markets.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.components.InfoBox
import com.asc.markets.ui.theme.*

@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // 1. Profile Header Card Parity
        Surface(
            color = PureBlack,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                // Background Watermark Parity
                Icon(
                    androidx.compose.material.icons.autoMirrored.outlined.Person, null,
                    tint = Color.White.copy(alpha = 0.02f),
                    modifier = Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("JD", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                        Surface(
                            color = IndigoAccent,
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp).border(2.dp, PureBlack, CircleShape)
                        ) {
                            Icon(androidx.compose.material.icons.autoMirrored.outlined.Edit, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text("JOHN DOE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = (-0.5).sp)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(androidx.compose.material.icons.autoMirrored.outlined.Verified, null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INSTITUTIONAL ANALYST", color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Identity Management Card Parity
        InfoBox {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(androidx.compose.material.icons.autoMirrored.outlined.Shield, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("IDENTITY MANAGEMENT", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        onClick = { },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text("SAVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProfileInput("Legal Full Name", "John Doe", androidx.compose.material.icons.autoMirrored.outlined.Person)
                ProfileInput("Intelligence Email", "john.doe@forexpro.ai", androidx.compose.material.icons.autoMirrored.outlined.Mail)
                ProfileInput("Secure Phone Uplink", "+44 20 7946 0958", androidx.compose.material.icons.autoMirrored.outlined.Phone)
                ProfileInput("Institutional Firm", "Alpha Strategic Capital", androidx.compose.material.icons.autoMirrored.outlined.Business)
                ProfileInput("Operational Region", "London, UK", androidx.compose.material.icons.autoMirrored.outlined.Language)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Status & Logs Grid Parity
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBox(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NODE STATUS", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(16.dp))
                    StatusMiniRow("PLAN", "PRO", Color.White)
                    StatusMiniRow("VERIFY", "SECURE", EmeraldSuccess)
                    StatusMiniRow("EXP", "NOV 25", Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    // Moved here from sidebar footer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NODE:", color = SlateMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("L14-UK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("V0.9.0-BETA", color = Color(0xFF3EA6FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            InfoBox(modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("AUDIT LOG", color = IndigoAccent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(16.dp))
                    AuditMiniRow("BIAS GEN", "2h ago", EmeraldSuccess)
                    AuditMiniRow("RISK UPD", "5h ago", IndigoAccent)
                    AuditMiniRow("AUTH_SEC", "1d ago", Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 4. Terminate Button Parity
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoseError.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, RoseError.copy(alpha = 0.3f))
        ) {
            Icon(androidx.compose.material.icons.autoMirrored.outlined.Logout, null, tint = RoseError, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("SIGN OUT SESSION", color = RoseError, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = InterFontFamily)
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun ProfileInput(label: String, value: String, icon: ImageVector) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = IndigoAccent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label.uppercase(), color = SlateText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color.White.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun StatusMiniRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SlateMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
        Text(value, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun AuditMiniRow(label: String, time: String, indicator: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(4.dp).background(indicator, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), fontFamily = InterFontFamily)
        Text(time, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}