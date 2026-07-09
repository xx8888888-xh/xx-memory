package com.xxmemory.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Primary,
    onSecondary = OnPrimary,
    secondaryContainer = PrimaryLight,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnPrimary,
    tertiary = PrimaryLight
)

/**
 * 墨水屏专用配色：仅使用黑、白、灰。永远白底黑字。
 */
private val EinkColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = Color.DarkGray,
    onSecondary = Color.White,
    secondaryContainer = Color.LightGray,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color.DarkGray,
    outline = Color(0xFFB0B0B0),
    outlineVariant = Color(0xFFD0D0D0),
    error = Color.Black,
    errorContainer = Color(0xFFE0E0E0),
    onError = Color.White,
    tertiary = Color.Gray
)

@Composable
fun XxMemoryTheme(
    einkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (einkMode) EinkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
