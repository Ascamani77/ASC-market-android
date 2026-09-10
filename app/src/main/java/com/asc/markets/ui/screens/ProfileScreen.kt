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
import com.asc.markets.data.UserProfileStore
import com.asc.markets.logic.ForexViewModel

@Composable
fun ProfileScreen(viewModel: ForexViewModel? = null) {
    val scrollState = rememberScrollState()
    val userProfile by UserProfileStore.profile.collectAsState()

    var firstName by remember(userProfile) { mutableStateOf(userProfile.firstName) }
    var surname by remember(userProfile) { mutableStateOf(userProfile.surname) }
    var email by remember(userProfile) { mutableStateOf(userProfile.email) }
    var phone by remember(userProfile) { mutableStateOf(userProfile.phone) }
    var firm by remember(userProfile) { mutableStateOf(userProfile.firm) }
    var region by remember(userProfile) { mutableStateOf(userProfile.region) }

    Surface(modifier = Modifier.fillMaxSize(), color = DeepBlack) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(color = PureBlack, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 36.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewModel != null) {
                            IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("PROFILE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            UserProfileStore.updateProfile(
                                userProfile.copy(
                                    firstName = firstName,
                                    surname = surname,
                                    email = email,
                                    phone = phone,
                                    firm = firm,
                                    region = region
                                )
                            )
                        },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text("SAVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Profile Header Card
                Surface(
                    color = PureBlack,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.Person, null,
                            tint = Color.White.copy(alpha = 0.02f),
                            modifier = Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(userProfile.initials, color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                                Surface(
                                    color = IndigoAccent,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp).border(2.dp, PureBlack, CircleShape)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Text(userProfile.fullName.uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, letterSpacing = (-0.5).sp)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.Verified, null, tint = IndigoAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(userProfile.role.uppercase(), color = IndigoAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(50)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Language, null, tint = SlateText, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(userProfile.region.ifBlank { "—" }, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        }
                                    }
                                    Surface(color = Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(50)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Business, null, tint = SlateText, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(userProfile.firm.ifBlank { "Independent" }, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 20.dp), color = Color.White.copy(alpha = 0.08f))

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Mail, null, tint = SlateText, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(userProfile.email.ifBlank { "—" }, color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Phone, null, tint = SlateText, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(userProfile.phone.ifBlank { "—" }, color = SlateText, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Identity Management Card
                InfoBox {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, null, tint = IndigoAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("IDENTITY MANAGEMENT", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = InterFontFamily)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        ProfileInput("First Name", firstName, Icons.Default.Person, onValueChange = { firstName = it })
                        ProfileInput("Surname", surname, Icons.Default.Person, onValueChange = { surname = it })
                        ProfileInput("Email", email, Icons.Default.Mail, onValueChange = { email = it })
                        ProfileInput("Phone", phone, Icons.Default.Phone, onValueChange = { phone = it })
                        ProfileInput("Firm", firm, Icons.Default.Business, onValueChange = { firm = it })
                        ProfileInput("Region", region, Icons.Default.Language, onValueChange = { region = it })
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun ProfileInput(label: String, value: String, icon: ImageVector, onValueChange: (String) -> Unit = {}) {
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
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            )
        }
    }
}
