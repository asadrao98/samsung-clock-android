package com.asadrao.clock.ui.theme

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Which palette to use. Mirrors the user's Settings choice; [ThemeMode.System] follows Android.
 */
enum class ThemeMode { Light, Dark, System }

private val LocalClockColors = staticCompositionLocalOf<ClockColors> {
    error("No ClockColors provided — wrap the UI in SamsungClockTheme")
}
private val LocalClockTypography = staticCompositionLocalOf<ClockTypography> {
    error("No ClockTypography provided — wrap the UI in SamsungClockTheme")
}
private val LocalClockShapes = staticCompositionLocalOf<ClockShapes> {
    error("No ClockShapes provided — wrap the UI in SamsungClockTheme")
}
private val LocalClockDimensions = staticCompositionLocalOf<ClockDimensions> {
    error("No ClockDimensions provided — wrap the UI in SamsungClockTheme")
}
private val LocalClockMotion = staticCompositionLocalOf<ClockMotion> {
    error("No ClockMotion provided — wrap the UI in SamsungClockTheme")
}

/**
 * The design system's access point. Every screen reads its colours, type, shapes, spacing and
 * motion from here — `ClockTheme.colors.textSecondary`, `ClockTheme.dimens.screenMargin` — and
 * never from `MaterialTheme` and never from a literal.
 */
object ClockTheme {
    val colors: ClockColors
        @Composable @ReadOnlyComposable get() = LocalClockColors.current
    val typography: ClockTypography
        @Composable @ReadOnlyComposable get() = LocalClockTypography.current
    val shapes: ClockShapes
        @Composable @ReadOnlyComposable get() = LocalClockShapes.current
    val dimens: ClockDimensions
        @Composable @ReadOnlyComposable get() = LocalClockDimensions.current
    val motion: ClockMotion
        @Composable @ReadOnlyComposable get() = LocalClockMotion.current
}

/**
 * Wraps the app in the One UI-derived design system.
 *
 * A [MaterialTheme] is still installed underneath, mapped onto the same palette. That is not
 * the source of truth for our own components — it is a safety net, so that any Material 3
 * component used for its behaviour (text selection handles, the text cursor) picks up the right
 * colours instead of Material purple.
 *
 * Dynamic colour (Material You) is deliberately **not** used. Samsung's colour-theme engine is
 * a different system, and letting the wallpaper repaint the app would pull it away from the
 * One UI look this project exists to reproduce.
 */
@Composable
fun SamsungClockTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (dark) ClockDarkColors else ClockLightColors

    // Status and navigation bar icon polarity has to follow the *app's* theme, not the
    // system's: with themeMode forced to Dark on a light-themed device the XML theme's
    // static value would leave dark icons on a black background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val indication = remember(dark) {
        ClockPressIndication(
            // The spec's ripple values, applied as a whole-target overlay rather than an
            // expanding circle: 20% white on dark, 10% black on light.
            overlayColor = if (dark) Color.White else Color.Black,
            pressedAlpha = if (dark) 0.20f else 0.10f,
            fadeInMillis = ClockMotionTokens.durationInstant,
            fadeOutMillis = ClockMotionTokens.durationPressOut,
        )
    }

    CompositionLocalProvider(
        LocalClockColors provides colors,
        LocalClockTypography provides ClockTypographyTokens,
        LocalClockShapes provides ClockShapeTokens,
        LocalClockDimensions provides ClockDimensionTokens,
        LocalClockMotion provides ClockMotionTokens,
        LocalIndication provides indication,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = Typography(),
            content = content,
        )
    }
}

private fun ClockColors.toMaterialColorScheme() =
    if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = pageBackground,
            onBackground = textPrimary,
            surface = cardBackground,
            onSurface = textPrimary,
            surfaceVariant = elevatedBackground,
            onSurfaceVariant = textSecondary,
            error = danger,
            outline = divider,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = pageBackground,
            onBackground = textPrimary,
            surface = cardBackground,
            onSurface = textPrimary,
            surfaceVariant = elevatedBackground,
            onSurfaceVariant = textSecondary,
            error = danger,
            outline = divider,
        )
    }
