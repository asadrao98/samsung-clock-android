package com.asadrao.clock.domain.timer

/**
 * The quick-duration buttons on the Timer tab.
 *
 * Samsung offers one-tap durations and orders them by what you have actually used, so the timer you
 * set yesterday is the first thing you see today. [forDisplay] does that: recents first, then the
 * common defaults to fill the row, de-duplicated, so the same value never appears twice.
 */
object QuickDurations {

    /** Sensible starting set, shown before the user has timed anything. */
    val DEFAULTS: List<Long> = listOf(
        1 * 60_000L,
        3 * 60_000L,
        5 * 60_000L,
        10 * 60_000L,
        15 * 60_000L,
        30 * 60_000L,
    )

    const val MAX_SHOWN = 6

    fun forDisplay(recents: List<Long>, limit: Int = MAX_SHOWN): List<Long> =
        (recents.filter { it > 0L } + DEFAULTS)
            .distinct()
            .take(limit)

    /** Compact label: "30s", "5m", "1h 30m". */
    fun label(millis: Long): String {
        val totalSeconds = millis / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            minutes > 0L && seconds > 0L -> "${minutes}m ${seconds}s"
            minutes > 0L -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}
