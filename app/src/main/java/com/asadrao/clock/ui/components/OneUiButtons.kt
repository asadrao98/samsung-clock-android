package com.asadrao.clock.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockColors
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.rememberPressScale

/**
 * One UI's buttons: capsules, not Material's 20dp-cornered rectangles.
 *
 * Press feedback is a small inward scale on a spring, layered on the theme-wide darken. Material
 * would draw an expanding ripple from the touch point, which is the wrong idiom here — and the
 * app-wide `LocalIndication` replacement means we never have to fight it per call site.
 */
@Composable
fun OneUiFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = OneUiButtonBase(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    container = { colors, on -> if (on) colors.accent else colors.disabled(colors.buttonSurface) },
    label = { colors, on -> if (on) colors.onAccent else colors.disabled(colors.textSecondary) },
)

/** The quieter partner to the filled button: accent text on a tinted capsule. */
@Composable
fun OneUiTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = OneUiButtonBase(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    container = { colors, _ -> colors.accentContainer },
    label = { colors, on -> if (on) colors.accent else colors.disabled(colors.textSecondary) },
)

/** No container at all. Used for Cancel, and for a dialog's secondary action. */
@Composable
fun OneUiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
) = OneUiButtonBase(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    container = { _, _ -> Color.Transparent },
    label = { colors, on ->
        when {
            !on -> colors.disabled(colors.textSecondary)
            color != null -> color
            else -> colors.accent
        }
    },
)

@Composable
private fun OneUiButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    container: (ClockColors, Boolean) -> Color,
    label: (ClockColors, Boolean) -> Color,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, enabled)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(ClockTheme.shapes.pill)
            .background(container(colors, enabled))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = dimens.buttonHeight)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClockTheme.typography.buttonLabel,
            color = label(colors, enabled),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The large circular control used by stopwatch and timer, with its caption underneath.
 *
 * Kept as a separate component rather than a variant of the capsule buttons: its geometry is
 * driven by a diameter, and the label sits outside the circle rather than inside it.
 */
@Composable
fun OneUiCircleButton(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, enabled)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(ClockTheme.dimens.circleButtonSize)
                .clip(ClockTheme.shapes.circle)
                .background(containerColor)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    // The visible caption is below the circle, so the click target needs its
                    // own accessible name or TalkBack announces an unlabelled button.
                    onClickLabel = label,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
            content = { icon() },
        )
        Text(
            text = label,
            style = ClockTheme.typography.listSummary,
            color = contentColor,
        )
    }
}
