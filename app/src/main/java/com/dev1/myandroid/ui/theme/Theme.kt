package com.dev1.myandroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Google-vibe palette
private val GoogleBlue = Color(0xFF1A73E8)
private val GoogleBlueDark = Color(0xFF1558B0)
private val SurfaceGray = Color(0xFFF8F9FA)
private val OnSurface = Color(0xFF202124)
private val OutlineGray = Color(0xFFDADCE0)

private val LightColors = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    background = SurfaceGray,
    onBackground = OnSurface,
    surface = Color.White,
    onSurface = OnSurface,
    surfaceVariant = Color(0xFFF1F3F4),
    outline = OutlineGray,
    error = Color(0xFFD93025),
    onError = Color.White,
)

@Composable
fun MyAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
