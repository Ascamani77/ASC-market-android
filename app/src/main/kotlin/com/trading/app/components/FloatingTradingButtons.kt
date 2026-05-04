package com.trading.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun FloatingTradingButtons(
    sellPrice: String,
    buyPrice: String,
    lotSize: String,
    onLotSizeChange: (String) -> Unit,
    onSellClick: () -> Unit,
    onBuyClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offset by remember { mutableStateOf(IntOffset(100, 300)) } // Initial position

    Box(
        modifier = modifier
            .offset { offset }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset = IntOffset(
                        (offset.x + dragAmount.x).roundToInt(),
                        (offset.y + dragAmount.y).roundToInt()
                    )
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF252525).copy(alpha = 0.95f))
            .padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(40.dp)
        ) {
            // 6 dots (Drag handle visual)
            Column(
                modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(2) {
                            Box(modifier = Modifier.size(2.5.dp).background(Color(0xFF434651), RoundedCornerShape(50)))
                        }
                    }
                }
            }

            // Sell Button (Red) - Fixed Width
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF23645).copy(alpha = 0.2f))
                    .clickable { onSellClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sellPrice,
                    color = Color(0xFFF23645),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // Center Lot Size Input - Fixed Width
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("0.0", color = Color(0xFF787B86), fontSize = 9.sp)
                BasicTextField(
                    value = lotSize,
                    onValueChange = onLotSizeChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Buy Button (Blue) - Fixed Width
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF2962FF).copy(alpha = 0.2f))
                    .clickable { onBuyClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buyPrice,
                    color = Color(0xFF2962FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // 3 dots
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, end = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onMoreClick() }
                    .padding(2.dp)
            ) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "More",
                    tint = Color(0xFFD1D4DC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
