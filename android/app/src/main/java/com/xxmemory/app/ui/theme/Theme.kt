@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xxmemory.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
 * 墨水屏专用配色：白底黑字，图标用灰色，仅小面积边框用黑色。
 */
private val EinkColorScheme = lightColorScheme(
    primary = Color.DarkGray,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color.Black,
    secondary = Color.Gray,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color.Gray,
    outline = Color(0xFFB0B0B0),
    outlineVariant = Color(0xFFD0D0D0),
    error = Color.DarkGray,
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

/**
 * 墨水屏专用开关：无动画，直接切换，仅使用灰/黑/白。
 */
@Composable
fun EinkSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor = when {
        !enabled -> Color(0xFFE0E0E0)
        checked -> Color.DarkGray
        else -> Color(0xFFE0E0E0)
    }
    val thumbColor = Color.White
    val borderColor = if (checked) Color.Black else Color.Gray

    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(trackColor)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCheckedChange(!checked) }
                    )
                } else Modifier
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(thumbColor)
                .border(1.dp, Color.LightGray, CircleShape)
        )
    }
}

/**
 * 墨水屏专用对话框：无进入/退出动画，直接使用 Dialog + Surface。
 * 标题、正文、确认/取消按钮与 Material AlertDialog 保持一致。
 */
@Composable
fun EinkAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable (() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.0f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    title?.let {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface
                        ) {
                            it()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    text?.let {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            it()
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dismissButton?.let { it(); Spacer(modifier = Modifier.width(8.dp)) }
                        confirmButton()
                    }
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

/**
 * 墨水屏友好 Chip：无动画、无 ripple、仅使用灰色/黑色/白色。
 * 在非墨水屏模式下回退到普通 Text 样式，但调用处仍可用 Material FilterChip。
 */
@Composable
fun EinkFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isEinkMode = rememberEinkMode()
    if (isEinkMode) {
        val bgColor = when {
            !enabled -> Color(0xFFF5F5F5)
            selected -> Color.DarkGray
            else -> Color.White
        }
        val contentColor = when {
            !enabled -> Color.Gray
            selected -> Color.White
            else -> Color.Black
        }
        val borderModifier = if (selected) {
            Modifier.padding(1.dp).background(Color.Black)
        } else {
            Modifier
        }
        Box(
            modifier = modifier
                .then(borderModifier)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .then(
                    if (enabled) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    } else {
        androidx.compose.material3.FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled
        )
    }
}
