package com.xxmemory.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalContext

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

private object NoIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        return remember(interactionSource) {
            object : IndicationInstance {
                override fun ContentDrawScope.drawIndication() {
                    drawContent()
                }
            }
        }
    }
}

@Composable
fun XxMemoryTheme(
    einkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (einkMode) EinkColorScheme else LightColorScheme
    val themedContent: @Composable () -> Unit = {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
    if (einkMode) {
        CompositionLocalProvider(LocalIndication provides NoIndication) {
            themedContent()
        }
    } else {
        themedContent()
    }
}

/**
 * Observe the eink_mode SharedPreference as a reactive Compose state.
 * Any change (e.g. toggled from Settings) will trigger recomposition.
 */
@Composable
fun rememberEinkMode(): Boolean {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("xx_memory_settings", Context.MODE_PRIVATE)
    }
    val einkMode = remember { mutableStateOf(prefs.getBoolean("eink_mode", false)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "eink_mode") {
                einkMode.value = prefs.getBoolean("eink_mode", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return einkMode.value
}
