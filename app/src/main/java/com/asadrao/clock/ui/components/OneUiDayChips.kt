package com.asadrao.clock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.colorTween
import java.time.DayOfWeek
import java.util.Locale

/**
 * The seven day-of-week circles in the alarm editor.
 *
 * Unselected is a bare letter with no fill and no outline; selected is a solid accent circle. The
 * circle scales up from 0.6 as it fills, which is the small flourish that makes the control feel
 * One UI rather than Material.
 *
 * Days are laid out from the locale's first day of the week — Monday in most of Europe, Sunday in
 * the US, Saturday across much of the Gulf — so the row reads correctly wherever the user is.
 */
@Composable
fun OneUiDayChips(
    selectedDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlarmFormat.weekOrder(locale).forEach { day ->
            DayChip(
                letter = AlarmFormat.dayNarrowName(day, locale),
                dayName = AlarmFormat.dayShortName(day, locale),
                selected = day in selectedDays,
                onToggle = { onToggleDay(day) },
            )
        }
    }
}

@Composable
private fun DayChip(
    letter: String,
    dayName: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val haptics = LocalHapticFeedback.current

    val fillScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "dayChipFill",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.textSecondary,
        animationSpec = ClockTheme.motion.colorTween(),
        label = "dayChipText",
    )

    Box(
        modifier = Modifier
            // The visible circle is 46dp but the target is padded to the 48dp minimum.
            .defaultMinSize(minWidth = dimens.minTouchTarget, minHeight = dimens.minTouchTarget)
            .clip(CircleShape)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onToggle()
                },
            )
            .semanticsDayLabel(dayName),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = fillScale
                        scaleY = fillScale
                    }
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
        Text(
            text = letter,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
        )
    }
}

/**
 * A single letter is meaningless to a screen reader, so the chip announces the full day name.
 */
private fun Modifier.semanticsDayLabel(dayName: String): Modifier =
    this.semantics { contentDescription = dayName }
