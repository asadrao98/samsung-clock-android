package com.asadrao.clock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The per-tab gradient hero that lives in the expanded header.
 *
 * This is where One UI 8.5's colour went. Earlier One UI Clock was a flat page; 8.5 gives each
 * tab a soft tinted gradient behind its big title, which compresses away as the header collapses.
 *
 * Each gradient fades into the page background at its bottom edge, so the hero dissolves into
 * the page rather than ending on a visible line. The hues are our reading of the published
 * screenshots — warm for Alarm, blue for World clock — and are a single edit each.
 */
data class TabGradient(
    val top: Color,
    val bottom: Color,
) {
    @Composable
    fun brush(pageBackground: Color): Brush = Brush.verticalGradient(
        0.0f to top,
        0.65f to bottom,
        // Landing exactly on the page colour is what makes the seam disappear.
        1.0f to pageBackground,
    )
}

/** Warm pink, per 8.5's Alarm tab. */
val AlarmGradientLight = TabGradient(Color(0xFFFFE3E8), Color(0xFFFDF0F1))
val AlarmGradientDark = TabGradient(Color(0xFF3A2028), Color(0xFF1E1519))

/** Blue, per 8.5's World clock tab. */
val WorldClockGradientLight = TabGradient(Color(0xFFDCE9FF), Color(0xFFEDF3FD))
val WorldClockGradientDark = TabGradient(Color(0xFF16283F), Color(0xFF141A22))

/** Green, reconstructed to sit alongside the two documented hues. */
val StopwatchGradientLight = TabGradient(Color(0xFFDDF3E6), Color(0xFFEFF7F1))
val StopwatchGradientDark = TabGradient(Color(0xFF15301F), Color(0xFF131C17))

/** Purple, likewise reconstructed. */
val TimerGradientLight = TabGradient(Color(0xFFE8E1FB), Color(0xFFF2EFFA))
val TimerGradientDark = TabGradient(Color(0xFF251E3B), Color(0xFF17151F))
