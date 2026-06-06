package com.techtower.meetingtranscriber.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    secondary = Color(0xFF0F766E),
    tertiary = Color(0xFFA16207),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E7EB),
    background = Color(0xFFF8FAFC),
)

@Composable
fun MeetingTranscriberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
