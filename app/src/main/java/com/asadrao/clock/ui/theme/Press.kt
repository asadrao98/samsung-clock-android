package com.asadrao.clock.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * One UI's press recoil, as a scale factor to feed into a `graphicsLayer`.
 *
 * Every pressable surface — cards, rows, pill buttons, circular buttons, tab icons — dips
 * slightly under the finger and settles back on the `elastic50` curve, which is fast out of the
 * gate and very slow to arrive. It layers on top of the theme-wide press darken rather than
 * replacing it.
 *
 * Asymmetric on purpose: 100ms down, 350ms back. Matching the two would lose the recoil.
 */
@Composable
fun rememberPressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = ClockTheme.motion
    val active = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (active) motion.pressedScale else 1f,
        animationSpec = motion.pressTween(active),
        label = "pressScale",
    )
    return scale
}
