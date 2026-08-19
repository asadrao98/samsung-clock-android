package com.asadrao.clock.domain.stopwatch

/**
 * The stopwatch, as a value.
 *
 * Elapsed time is **never** accumulated by counting ticks. It is derived from a monotonic clock —
 * `SystemClock.elapsedRealtime()` — so it stays correct while the app is backgrounded, while the
 * screen is off, and while the process is not scheduled at all. A per-second counter would drift
 * badly in exactly those conditions, and would be wrong by minutes after a night asleep.
 *
 * [bootMarker] exists to catch a reboot. `elapsedRealtime` counts from boot, so a value stored
 * before a restart is meaningless afterwards — it would read as an enormous or negative duration.
 * The marker is an estimate of the moment the device booted (`wall clock − elapsed realtime`),
 * which stays roughly constant across a session and jumps when the device restarts.
 */
data class StopwatchState(
    /** Time banked from previous runs, in milliseconds. */
    val accumulatedMillis: Long = 0L,
    /** The monotonic reading at which the current run began, or null when not running. */
    val runningSinceRealtime: Long? = null,
    /** Cumulative elapsed time at each recorded lap, oldest first. */
    val laps: List<Long> = emptyList(),
    /** Approximate boot instant, used to detect a restart. Null when nothing is stored. */
    val bootMarker: Long? = null,
) {
    val isRunning: Boolean get() = runningSinceRealtime != null
    val isIdle: Boolean get() = !isRunning && accumulatedMillis == 0L && laps.isEmpty()
    val isPaused: Boolean get() = !isRunning && !isIdle

    /** Elapsed time as of [nowRealtime]. */
    fun elapsedAt(nowRealtime: Long): Long {
        val since = runningSinceRealtime ?: return accumulatedMillis
        // Guards against a monotonic reading that has gone backwards, which should be impossible
        // but would otherwise show a negative duration.
        val delta = (nowRealtime - since).coerceAtLeast(0L)
        return accumulatedMillis + delta
    }

    fun start(nowRealtime: Long, bootMarker: Long): StopwatchState =
        if (isRunning) this
        else copy(runningSinceRealtime = nowRealtime, bootMarker = bootMarker)

    fun pause(nowRealtime: Long): StopwatchState =
        if (!isRunning) this
        else copy(accumulatedMillis = elapsedAt(nowRealtime), runningSinceRealtime = null)

    fun reset(): StopwatchState = StopwatchState()

    /**
     * Records a lap. Ignored when not running: Samsung's Lap button is only live while the
     * stopwatch is going, and a lap recorded while paused would duplicate the previous one.
     */
    fun lap(nowRealtime: Long): StopwatchState =
        if (!isRunning) this
        else copy(laps = laps + elapsedAt(nowRealtime))

    /**
     * The individual lap durations, newest first — which is the order the list is displayed in.
     *
     * Laps are stored cumulatively and differenced here, rather than stored as durations, so that
     * a lap's total can never drift out of step with the readout above it.
     */
    fun lapSplits(): List<LapEntry> {
        val result = ArrayList<LapEntry>(laps.size)
        laps.forEachIndexed { index, cumulative ->
            val previous = if (index == 0) 0L else laps[index - 1]
            result += LapEntry(
                number = index + 1,
                splitMillis = cumulative - previous,
                totalMillis = cumulative,
            )
        }
        return result.asReversed()
    }

    /**
     * Reconciles stored state with the current boot session.
     *
     * If the device has restarted while the stopwatch was running there is no way to know how long
     * it ran for, so the honest outcome is to keep the time banked before the restart and leave it
     * paused, rather than invent a duration or silently discard the user's timing.
     */
    fun afterRestore(currentBootMarker: Long, toleranceMillis: Long = BOOT_TOLERANCE): StopwatchState {
        val stored = bootMarker ?: return this
        val rebooted = kotlin.math.abs(currentBootMarker - stored) > toleranceMillis
        if (!rebooted) return this
        return copy(runningSinceRealtime = null, bootMarker = currentBootMarker)
    }

    companion object {
        /**
         * The boot marker wanders by a few milliseconds as the two clocks are read separately, and
         * more if the wall clock is corrected by NTP. A few seconds of slack distinguishes that
         * noise from a genuine restart.
         */
        const val BOOT_TOLERANCE = 5_000L
    }
}

/** One row of the lap list. */
data class LapEntry(
    val number: Int,
    val splitMillis: Long,
    val totalMillis: Long,
)
