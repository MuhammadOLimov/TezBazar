package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldMintContainer,
    onPrimaryContainer = EmeraldDeep,
    secondary = BukharaGold,
    onSecondary = Color.White,
    secondaryContainer = BukharaGoldContainer,
    onSecondaryContainer = BukharaGoldDeep,
    tertiary = EmeraldVibrant,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldSoftTint,
    onTertiaryContainer = EmeraldDeep,
    background = AlabasterBackground,
    onBackground = InkDark,
    surface = PureCardSurface,
    onSurface = InkDark,
    surfaceVariant = SoftMintSurface,
    onSurfaceVariant = InkMuted,
    outline = SoftMintBorder,
    outlineVariant = Color(0xFFE5ECE8),
    error = ErrorCrimson,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLuminous,
    onPrimary = NightEmeraldBg,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = EmeraldMintContainer,
    secondary = BukharaGoldGlow,
    onSecondary = NightEmeraldBg,
    secondaryContainer = Color(0xFF3E2D08),
    onSecondaryContainer = BukharaGoldContainer,
    tertiary = EmeraldVibrant,
    onTertiary = Color.White,
    tertiaryContainer = NightEmeraldSurfaceHigh,
    onTertiaryContainer = EmeraldMintContainer,
    background = NightEmeraldBg,
    onBackground = CreamText,
    surface = NightEmeraldSurface,
    onSurface = CreamText,
    surfaceVariant = NightEmeraldSurfaceHigh,
    onSurfaceVariant = SageMutedText,
    outline = NightEmeraldBorder,
    outlineVariant = Color(0xFF1A362A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5C1515),
    onErrorContainer = Color(0xFFFEE2E2)
)

val TezBazarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun TezBazarTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TezBazarTypography,
        shapes = TezBazarShapes,
        content = content
    )
}
