package de.kreutzm.gemma4test.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme()

@Composable
fun Gemma4TestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
