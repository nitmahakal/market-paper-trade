package com.nsepapertrade.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F6FA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8F6),
    onPrimaryContainer = Color(0xFF14304F),
    secondary = Color(0xFF5B6B7C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E9F0),
    onSecondaryContainer = Color(0xFF1B2733),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFF5F6F8),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFE9ECF0),
    onSurfaceVariant = Color(0xFF565E68),
    outline = Color(0xFF8A929C),
    outlineVariant = Color(0xFFD3D8DE),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F4F7),
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceContainerHigh = Color(0xFFFAFBFC),
    surfaceContainerHighest = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB3E0),
    onPrimary = Color(0xFF0F2438),
    primaryContainer = Color(0xFF2A3F57),
    onPrimaryContainer = Color(0xFFD6E4F5),
    secondary = Color(0xFF9FB3C8),
    onSecondary = Color(0xFF14202B),
    secondaryContainer = Color(0xFF2B3744),
    onSecondaryContainer = Color(0xFFD5E0EC),
    background = Color(0xFF14171B),
    onBackground = Color(0xFFD9DCE1),
    surface = Color(0xFF14171B),
    onSurface = Color(0xFFD9DCE1),
    surfaceVariant = Color(0xFF22272E),
    onSurfaceVariant = Color(0xFFA9B0BA),
    outline = Color(0xFF6B737D),
    outlineVariant = Color(0xFF343A42),
    error = Color(0xFFE59A9A),
    onError = Color(0xFF3B1212),
    surfaceContainerLowest = Color(0xFF111417),
    surfaceContainerLow = Color(0xFF181C20),
    surfaceContainer = Color(0xFF1B1F24),
    surfaceContainerHigh = Color(0xFF1E2329),
    surfaceContainerHighest = Color(0xFF22272E)
)

private val LightProfit = Color(0xFF2E7D32)
private val LightLoss = Color(0xFFC62828)
private val DarkProfit = Color(0xFF6FBF8B)
private val DarkLoss = Color(0xFFE08A8A)

@Composable
fun profitColor(): Color {
    return if (isSystemInDarkTheme()) DarkProfit else LightProfit
}

@Composable
fun lossColor(): Color {
    return if (isSystemInDarkTheme()) DarkLoss else LightLoss
}

// Text colour that sits on top of profitColor() / lossColor()
@Composable
fun onTradeColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xFF10141A)
    } else {
        Color(0xFFFFFFFF)
    }
}

@Composable
fun NSEPaperTradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            applySystemBars(
                view = view,
                barColor = colorScheme.background.toArgb(),
                darkTheme = darkTheme
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Suppress("DEPRECATION")
private fun applySystemBars(
    view: android.view.View,
    barColor: Int,
    darkTheme: Boolean
) {
    val window = view.context.findActivity()?.window ?: return

    window.statusBarColor = barColor
    window.navigationBarColor = barColor

    val controller = WindowCompat.getInsetsController(window, view)
    controller.isAppearanceLightStatusBars = !darkTheme
    controller.isAppearanceLightNavigationBars = !darkTheme
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
