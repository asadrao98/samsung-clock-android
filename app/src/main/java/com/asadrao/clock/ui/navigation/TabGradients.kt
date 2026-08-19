package com.asadrao.clock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import com.asadrao.clock.ui.theme.AlarmGradientDark
import com.asadrao.clock.ui.theme.AlarmGradientLight
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.StopwatchGradientDark
import com.asadrao.clock.ui.theme.StopwatchGradientLight
import com.asadrao.clock.ui.theme.TimerGradientDark
import com.asadrao.clock.ui.theme.TimerGradientLight
import com.asadrao.clock.ui.theme.WorldClockGradientDark
import com.asadrao.clock.ui.theme.WorldClockGradientLight

/** The gradient hero for a tab's expanded header, in the current theme. */
@Composable
fun rememberTabGradient(tab: ClockTab): Brush {
    val colors = ClockTheme.colors
    val gradient = when (tab) {
        ClockTab.Alarm -> if (colors.isDark) AlarmGradientDark else AlarmGradientLight
        ClockTab.WorldClock ->
            if (colors.isDark) WorldClockGradientDark else WorldClockGradientLight
        ClockTab.Stopwatch ->
            if (colors.isDark) StopwatchGradientDark else StopwatchGradientLight
        ClockTab.Timer -> if (colors.isDark) TimerGradientDark else TimerGradientLight
    }
    return gradient.brush(colors.pageBackground)
}
