package com.example.jamuchat.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TelegramBluePrimary,
    onPrimary = Color.White,
    primaryContainer = TelegramSentBubbleDark,
    onPrimaryContainer = Color.White,
    secondary = TelegramHeaderBlue,
    background = JamuBgDark,
    surface = JamuSurfaceDark,
    surfaceVariant = JamuSurfaceVariantDark,
    onBackground = JamuTextPrimaryDark,
    onSurface = JamuTextPrimaryDark,
    onSurfaceVariant = JamuTextMutedDark,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun JamuchatTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
