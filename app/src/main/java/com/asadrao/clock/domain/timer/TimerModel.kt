package com.asadrao.clock.domain.timer

/**
 * One countdown timer.
 *
 * Like the stopwatch, the remaining time is **derived**, never decremented. A timer stores the
 * monotonic instant it is due to finish; what is left is simply that minus now. Decrementing a
 * variable on a tick would drift whenever the process was not scheduled, and would be badly wrong
 * after any spell in the background — which is precisely when a kitchen timer is running.
 *
 * [endsAtRealtime] is a reading of `SystemClock.elapsedRealtime()`, so it is immune to the wall
 * clock being changed, to timezone changes and to daylight saving. A timer is a stretch of elapsed
 * time, so that is the right clock. (An alarm is the opposite case — see `AlarmSchedule`.)
 */
data class ClockTimer(
    val id: Long,
    /** What the user dialled in, so Restart can return to it. */
    val totalMillis: Long,
    /** Monotonic instant the countdown ends. Null while paused. */
    val endsAtRealtime: Long? = null,
    /** Time left, captured at the moment it was paused. */
    val remainingWhenPausedMillis: Long = totalMillis,
    val label: String = "",
    val position: Int = 0,
) {
    val isRunning: Boolean get() = endsAtRealtime != null

    /** Milliseconds left as of [nowRealtime], never negative. */
    fun remainingAt(nowRealtime: Long): Long {
        val endsAt = endsAtRealtime ?: return remainingWhenPausedMillis
        return (endsAt - nowRealtime).coerceAtLeast(0L)
    }

    fun isFinishedAt(nowRealtime: Long): Boolean = isRunning && remainingAt(nowRealtime) == 0L

    /** 0 at the start, 1 when it finishes. Drives the countdown ring. */
    fun progressAt(nowRealtime: Long): Float {
        if (totalMillis <= 0L) return 1f
        val elapsed = totalMillis - remainingAt(nowRealtime)
        return (elapsed.toFloat() / totalMillis).coerceIn(0f, 1f)
    }

    fun start(nowRealtime: Long): ClockTimer =
        if (isRunning) this
        else copy(endsAtRealtime = nowRealtime + remainingWhenPausedMillis)

    fun pause(nowRealtime: Long): ClockTimer =
        if (!isRunning) this
        else copy(
            remainingWhenPausedMillis = remainingAt(nowRealtime),
            endsAtRealtime = null,
        )

    fun reset(): ClockTimer =
        copy(endsAtRealtime = null, remainingWhenPausedMillis = totalMillis)

    /** Samsung's "+1 min" on a running timer, which extends rather than restarts. */
    fun addMinute(nowRealtime: Long): ClockTimer = if (isRunning) {
        copy(
            endsAtRealtime = (endsAtRealtime ?: nowRealtime) + ONE_MINUTE,
            totalMillis = totalMillis + ONE_MINUTE,
        )
    } else {
        copy(
            remainingWhenPausedMillis = remainingWhenPausedMillis + ONE_MINUTE,
            totalMillis = totalMillis + ONE_MINUTE,
        )
    }

    companion object {
        const val ONE_MINUTE = 60_000L
    }
}

/** A saved duration the user can start with one tap. */
data class TimerPreset(
    val id: Long,
    val totalMillis: Long,
    val label: String,
    val position: Int,
)

/** Converts a dialled hours/minutes/seconds into milliseconds. */
fun durationMillisOf(hours: Int, minutes: Int, seconds: Int): Long =
    (hours * 3_600L + minutes * 60L + seconds) * 1_000L
