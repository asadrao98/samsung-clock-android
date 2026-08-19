package com.asadrao.clock.ui.alarm

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.ui.components.OneUiSwitch
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.snapDpTween
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.util.Locale

/**
 * One alarm in the list.
 *
 * How a disabled alarm looks is worth being precise about: **only alpha changes**. The text column
 * drops to 40%, and the day letters lose their accent so the blue disappears entirely. Height,
 * layout, weights, dividers and the card behind it all stay exactly as they were. Greying the
 * whole row or restyling it makes the list jump as alarms are toggled.
 *
 * The AM/PM marker is baseline-aligned beside the time at roughly half its size, in the same
 * colour. Never superscript, never tinted differently — that is one of the details that most
 * quickly reads as "not Samsung".
 */
@Composable
fun AlarmRow(
    alarm: Alarm,
    is24Hour: Boolean,
    locale: Locale,
    now: ZonedDateTime,
    nextTrigger: ZonedDateTime?,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens

    // In selection mode the row's content slides right to make room for the checkbox.
    val leadingSpace by animateDpAsState(
        targetValue = if (selectionMode) 40.dp else 0.dp,
        animationSpec = ClockTheme.motion.snapDpTween(),
        label = "alarmRowLeadingSpace",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
            )
            .defaultMinSize(minHeight = dimens.alarmRowMinHeight)
            .padding(horizontal = dimens.cardPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingSpace > 0.dp) {
            Box(
                modifier = Modifier.width(leadingSpace),
                contentAlignment = Alignment.CenterStart,
            ) {
                SelectionCheck(selected = selected)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                // The single alpha that carries the whole enabled/disabled distinction.
                .alpha(if (alarm.enabled) 1f else DISABLED_ALPHA),
        ) {
            // Baseline alignment is per-child in a Row, and it matters here: the AM/PM sits on
            // the same baseline as a time twice its size, which centre alignment would not give.
            Row {
                Text(
                    text = AlarmFormat.time(alarm.hour, alarm.minute, is24Hour),
                    style = ClockTheme.typography.alarmTime,
                    color = colors.textPrimary,
                    modifier = Modifier.alignByBaseline(),
                )
                AlarmFormat.meridiem(alarm.hour, locale, is24Hour)?.let { meridiem ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = meridiem,
                        style = ClockTheme.typography.alarmMeridiem,
                        color = colors.textPrimary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }

            Spacer(Modifier.size(2.dp))

            if (alarm.repeats) {
                DayLetters(
                    selectedDays = alarm.repeatDays.toSet(),
                    locale = locale,
                    enabled = alarm.enabled,
                )
            } else {
                Text(
                    text = nextTrigger?.let {
                        AlarmFormat.oneShotDate(it, now, locale, "Today", "Tomorrow")
                    } ?: "",
                    style = ClockTheme.typography.alarmMeta,
                    color = colors.textSecondary,
                )
            }

            if (alarm.label.isNotBlank()) {
                Text(
                    text = alarm.label,
                    style = ClockTheme.typography.alarmMeta,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        OneUiSwitch(
            checked = alarm.enabled,
            // Non-interactive in selection mode: tapping the row selects it instead.
            onCheckedChange = if (selectionMode) null else onToggle,
            enabled = !selectionMode,
        )
    }
}

/**
 * The seven repeat letters.
 *
 * Each sits in a fixed-width slot so the letters column-align down the whole list rather than
 * shifting with the width of each glyph. Chosen days are accent; the rest are faint. When the
 * alarm is off the accent is dropped completely, so an off alarm carries no blue at all.
 */
@Composable
private fun DayLetters(
    selectedDays: Set<DayOfWeek>,
    locale: Locale,
    enabled: Boolean,
) {
    val colors = ClockTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AlarmFormat.weekOrder(locale).forEach { day ->
            val chosen = day in selectedDays
            Text(
                text = AlarmFormat.dayNarrowName(day, locale),
                style = ClockTheme.typography.alarmMeta,
                color = when {
                    chosen && enabled -> colors.accentText
                    chosen -> colors.textPrimary
                    else -> colors.textPrimary.copy(alpha = 0.3f)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.width(15.dp),
            )
        }
    }
}

@Composable
private fun SelectionCheck(selected: Boolean) {
    val colors = ClockTheme.colors
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(text = "✓", color = colors.onAccent, style = ClockTheme.typography.caption)
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.textTertiary.copy(alpha = 0.25f)),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.4f
