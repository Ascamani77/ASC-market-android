package com.asc.markets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*
import com.trading.app.components.AssetIcon
import com.trading.app.models.SymbolInfo

/** A pickable asset row entry — mirrors the AI Simulation picker's payload. */
data class PickableAsset(
    val symbol: String,
    val name: String,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAssetPickerSheet(
    assets: List<PickableAsset>,
    selected: Set<String>,
    onToggleAsset: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
    title: String = "Select assets",
    subtitle: String = "TARGETS FOR AUTO TRADE"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF363A45))
            )
        },
        contentWindowInsets = { WindowInsets(0) },
        modifier = modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        title.uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        subtitle,
                        color = SlateText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = InterFontFamily
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Select all / clear all + live count ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = IndigoAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, IndigoAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onSelectAll() }
                    ) {
                        Text(
                            "SELECT ALL",
                            color = IndigoAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = RoseError.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onClearAll() }
                    ) {
                        Text(
                            "CLEAR ALL",
                            color = RoseError,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    "${selected.size} / ${assets.size} selected",
                    color = if (selected.isNotEmpty()) EmeraldSuccess else SlateText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Asset list ──────────────────────────────────────────
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier.weight(1f).verticalScroll(scroll)
            ) {
                assets.forEach { asset ->
                    val symbol = asset.symbol
                    val isSelected = symbol in selected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) IndigoAccent.copy(alpha = 0.12f)
                                else Color.White.copy(alpha = 0.02f)
                            )
                            .clickable { onToggleAsset(symbol) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AssetIcon(
                                symbol = SymbolInfo(
                                    ticker = symbol.replace("/", "").removeSuffix("M").removeSuffix("m"),
                                    name = asset.name,
                                    type = asset.type
                                ),
                                size = 36
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    symbol,
                                    color = if (isSelected) IndigoAccent else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    asset.name,
                                    color = SlateText,
                                    fontSize = 11.sp,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Selection indicator
                        if (isSelected) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, EmeraldSuccess)
                            ) {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.padding(4.dp).size(16.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(24.dp)
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(50))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Confirm bar ─────────────────────────────────────────
            Button(
                onClick = onDone,
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected.isNotEmpty()) IndigoAccent else Color.White.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    if (selected.isNotEmpty()) "DONE \u2022 ${selected.size} SELECTED" else "SELECT AT LEAST ONE ASSET",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}