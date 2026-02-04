package com.asc.markets.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.asc.markets.ui.theme.PureBlack

/**
 * Reusable pattern: a TopAppBar that hides on scroll plus a pinned secondary bar directly beneath it.
 * The `content` lambda receives a Modifier that already applies the TopAppBar's nestedScroll connection
 * so any scrollable inside should use that Modifier to ensure the top bar hides correctly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideOnScrollTopWithPinnedSecondary(
    title: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = title,
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
            scrollBehavior = scrollBehavior
        )

        // Pinned secondary bar (remains visible while content scrolls)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PureBlack,
            tonalElevation = 0.dp
        ) {
            secondary()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Pass a modifier with nestedScroll applied so inner scrolling controls the top app bar
            content(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection))
        }
    }
}
