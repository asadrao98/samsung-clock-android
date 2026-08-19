package com.asadrao.clock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.ui.theme.ClockFontFamily
import com.asadrao.clock.ui.theme.ClockTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The One UI alarm time picker: two or three drums side by side.
 *
 * Twelve-hour locales get three columns — hour, minute, AM/PM — and twenty-four-hour locales get
 * two, centred. Hours and minutes loop; the meridiem does not, because there are only two values.
 *
 * A consequence of the hour drum looping is that **rolling past 12 does not flip AM/PM**. That is
 * correct for a three-column picker: the period is an independent value the user sets themselves.
 * It is worth knowing, because it looks like a bug until you think about it.
 *
 * The value is kept as an hour/minute pair in 24-hour form throughout; twelve-hour display is
 * purely presentational.
 */
@Composable
fun OneUiTimePicker(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
) {
    val colors = ClockTheme.colors
    val digitStyle = remember24Or12Style(is24Hour)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (is24Hour) {
            OneUiWheel(
                itemCount = 24,
                selectedIndex = hour,
                onSelectedIndexChange = { onTimeChange(it, minute) },
                label = { "%02d".format(it) },
                contentDescription = "Hour",
                textStyle = digitStyle,
                color = colors.textPrimary,
            )
            MinuteWheel(minute, digitStyle) { onTimeChange(hour, it) }
        } else {
            // 1..12 displayed, but the model stays in 24-hour form.
            val displayHour = twelveHourOf(hour)
            val isPm = hour >= 12
            OneUiWheel(
                itemCount = 12,
                selectedIndex = displayHour - 1,
                onSelectedIndexChange = { index ->
                    onTimeChange(hourFrom(index + 1, isPm), minute)
                },
                label = { "${it + 1}" },
                contentDescription = "Hour",
                textStyle = digitStyle,
                color = colors.textPrimary,
            )
            MinuteWheel(minute, digitStyle) { onTimeChange(hour, it) }
            OneUiWheel(
                itemCount = 2,
                selectedIndex = if (isPm) 1 else 0,
                onSelectedIndexChange = { index ->
                    onTimeChange(hourFrom(displayHour, index == 1), minute)
                },
                label = { meridiemLabel(it == 1, locale) },
                contentDescription = "AM or PM",
                width = 72.dp,
                loop = false,
                textStyle = digitStyle.copy(fontSize = 24.sp),
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun MinuteWheel(
    minute: Int,
    style: androidx.compose.ui.text.TextStyle,
    onChange: (Int) -> Unit,
) {
    OneUiWheel(
        itemCount = 60,
        selectedIndex = minute,
        onSelectedIndexChange = onChange,
        label = { "%02d".format(it) },
        contentDescription = "Minute",
        textStyle = style,
        color = ClockTheme.colors.textPrimary,
    )
}

/**
 * In 24-hour mode the meridiem column's width is free, so the digits can be a little larger.
 * Font scale is clamped: at the system's largest setting an unclamped 48sp overflows the fixed
 * item height and the digits clip.
 */
@Composable
private fun remember24Or12Style(is24Hour: Boolean) = ClockTheme.typography.numericDisplay.copy(
    fontFamily = ClockFontFamily,
    fontSize = if (is24Hour) 52.sp else 48.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp,
)

private fun twelveHourOf(hour24: Int): Int = when (val h = hour24 % 12) {
    0 -> 12
    else -> h
}

private fun hourFrom(displayHour: Int, isPm: Boolean): Int {
    val base = if (displayHour == 12) 0 else displayHour
    return if (isPm) base + 12 else base
}

private fun meridiemLabel(isPm: Boolean, locale: Locale): String =
    DateTimeFormatter.ofPattern("a", locale)
        .format(LocalTime.of(if (isPm) 13 else 1, 0))
