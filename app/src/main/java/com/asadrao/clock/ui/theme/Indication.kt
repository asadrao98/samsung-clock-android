package com.asadrao.clock.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * One UI's press feedback: the whole target quietly darkens (or lightens, in dark theme) and
 * fades back. There is no expanding circle.
 *
 * This replaces Material's ripple app-wide via `LocalIndication`, because a ripple is one of
 * the loudest tells that an app is Material underneath. Kept as a real [IndicationNodeFactory]
 * rather than per-component colour juggling so that every `clickable` gets it for free.
 *
 * The overlay is drawn across the node's full bounds, so a rounded component must apply its
 * `clip` *outside* the `clickable` or the tint will square off its corners.
 */
class ClockPressIndication(
    private val overlayColor: Color,
    private val pressedAlpha: Float,
    private val fadeInMillis: Int,
    private val fadeOutMillis: Int,
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressNode(interactionSource, overlayColor, pressedAlpha, fadeInMillis, fadeOutMillis)

    override fun equals(other: Any?): Boolean =
        other is ClockPressIndication &&
            other.overlayColor == overlayColor &&
            other.pressedAlpha == pressedAlpha &&
            other.fadeInMillis == fadeInMillis &&
            other.fadeOutMillis == fadeOutMillis

    override fun hashCode(): Int {
        var result = overlayColor.hashCode()
        result = 31 * result + pressedAlpha.hashCode()
        result = 31 * result + fadeInMillis
        result = 31 * result + fadeOutMillis
        return result
    }

    private class PressNode(
        private val interactionSource: InteractionSource,
        private val overlayColor: Color,
        private val pressedAlpha: Float,
        private val fadeInMillis: Int,
        private val fadeOutMillis: Int,
    ) : Modifier.Node(), DrawModifierNode {

        private val alpha = Animatable(0f)

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press ->
                            alpha.animateTo(pressedAlpha, tween(fadeInMillis))
                        is PressInteraction.Release, is PressInteraction.Cancel ->
                            alpha.animateTo(0f, tween(fadeOutMillis))
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            // Animatable's value is snapshot state, so reading it here re-invalidates draw
            // as the animation runs — no manual invalidateDraw needed.
            val current = alpha.value
            if (current > 0f) {
                drawRect(color = overlayColor.copy(alpha = current))
            }
        }
    }
}
