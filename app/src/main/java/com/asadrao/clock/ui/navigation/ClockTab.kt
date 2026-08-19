package com.asadrao.clock.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.asadrao.clock.R

/**
 * The four bottom-navigation destinations, in Samsung Clock's order.
 *
 * Order is part of the resemblance — a Samsung user reaches for Timer on the right — so it is
 * fixed by the enum's declaration order and the UI iterates `entries` rather than listing tabs
 * by hand anywhere.
 */
enum class ClockTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    /** Stable key for saved UI state, so a tab's scroll position survives switching away. */
    val stateKey: String,
) {
    Alarm(R.string.tab_alarm, R.drawable.ic_tab_alarm, "tab_alarm"),
    WorldClock(R.string.tab_world_clock, R.drawable.ic_tab_world_clock, "tab_world_clock"),
    Stopwatch(R.string.tab_stopwatch, R.drawable.ic_tab_stopwatch, "tab_stopwatch"),
    Timer(R.string.tab_timer, R.drawable.ic_tab_timer, "tab_timer"),
    ;

    companion object {
        val Default = Alarm
    }
}
