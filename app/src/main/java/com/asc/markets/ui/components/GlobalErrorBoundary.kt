package com.asc.markets.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.markets.ui.theme.*

@Composable
fun GlobalErrorBoundary(content: @Composable () -> Unit) {
    // Provide a CompositionLocal reporter so child composables can safely report
    // errors to this boundary without throwing during composition.
    val LocalErrorReporter = compositionLocalOf<(Throwable) -> Unit> { { _ -> } }

    var hasError by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<Throwable?>(null) }

    val reportError: (Throwable) -> Unit = { t ->
        lastError = t
        hasError = true
        android.util.Log.e("GlobalErrorBoundary", "Captured error reported by child", t)
    }

    if (hasError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(RoseError, androidx.compose.foundation.shape.CircleShape))
                    Text(
                        "CRITICAL_NODE_FAILURE",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "DATA TEMPORARILY UNAVAILABLE",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))
                // Optional: show short error message when available
                lastError?.let { err ->
                    Text(err.message ?: "(no details)", color = SlateText, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Allow users/developers to dismiss and attempt recovery
                androidx.compose.material3.Button(onClick = {
                    hasError = false
                    lastError = null
                }) {
                    Text("Dismiss")
                }
            }
        }
    } else {
        CompositionLocalProvider(LocalErrorReporter provides reportError) {
            content()
        }
    }
}