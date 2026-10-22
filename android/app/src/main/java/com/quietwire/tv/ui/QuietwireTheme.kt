package com.quietwire.tv.ui

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val QuietwireColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3DDC97),
    background = androidx.compose.ui.graphics.Color(0xFF0B0F14),
    surface = androidx.compose.ui.graphics.Color(0xFF151B23),
)

@Composable
fun QuietwireTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = QuietwireColorScheme, content = content)
}
