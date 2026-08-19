package com.asadrao.clock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.colorTween
import com.asadrao.clock.ui.theme.toggleTween

/**
 * The One UI toggle — the single most recognisable control in the system, and the one that most
 * betrays a Material app in disguise.
 *
 * Written from scratch rather than restyling `androidx.compose.material3.Switch`, because the
 * differences are structural, not cosmetic. Material 3's thumb *changes size* as it moves
 * (small when off, large when on, larger still while pressed) and its off-state track is drawn
 * as an outline. One UI's thumb is a single constant-size circle sliding inside a solid filled
 * capsule, and the track's colour is the only thing that changes. No amount of colour
 * overriding gets Material's switch there.
 *
 * The thumb travels on a slightly under-damped spring, so it arrives with a small overshoot.
 *
 * The visible capsule is 32dp tall but the touch target is padded out to 48dp, so the control
 * stays reachable without making the row look loose.
 */
@Composable
fun OneUiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val motion = ClockTheme.motion
    val haptics = LocalHapticFeedback.current

    val travel = dimens.switchWidth - dimens.switchThumbSize - dimens.switchThumbInset * 2

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) travel else 0.dp,
        animationSpec = motion.toggleTween(),
        label = "switchThumbOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabled(colors.switchTrackOff)
            checked -> colors.switchTrackOn
            else -> colors.switchTrackOff
        },
        animationSpec = motion.colorTween(),
        label = "switchTrackColor",
    )

    Box(
        modifier = modifier
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        // The row's own press tint already covers the touch; a second overlay
                        // on the switch itself reads as a smudge.
                        indication = null,
                        interactionSource = null,
                        onValueChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            onCheckedChange(it)
                        },
                    )
                } else {
                    Modifier
                }
            )
            // Grows the touch target to 48dp without changing how tall the capsule looks.
            .padding(vertical = (dimens.minTouchTarget - dimens.switchHeight) / 2),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(width = dimens.switchWidth, height = dimens.switchHeight)
                .clip(ClockTheme.shapes.pill)
                .background(trackColor),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = dimens.switchThumbInset)
                    // Lambda overload on purpose: the offset changes every frame while the
                    // thumb travels, and this defers it to layout instead of recomposing.
                    .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                    .size(dimens.switchThumbSize)
                    .clip(ClockTheme.shapes.circle)
                    .background(
                        if (enabled) colors.switchThumb else colors.disabled(colors.switchThumb)
                    )
                    // One UI rings the thumb with a hairline while off, and drops the ring when
                    // on. It is a small thing that carries a lot of the control's character.
                    .then(
                        if (!checked) {
                            Modifier.border(
                                width = dimens.switchThumbStroke,
                                color = if (enabled) colors.switchThumbStrokeOff
                                else colors.disabled(colors.switchThumbStrokeOff),
                                shape = ClockTheme.shapes.circle,
                            )
                        } else {
                            Modifier
                        }
                    ),
            )
        }
    }
}
