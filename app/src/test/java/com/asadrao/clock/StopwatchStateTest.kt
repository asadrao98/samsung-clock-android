package com.asadrao.clock

import com.asadrao.clock.domain.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The stopwatch's timing arithmetic.
 *
 * All of it is driven by an injected monotonic reading, so the cases that matter — a long spell in
 * the background, a pause and resume, a reboot mid-run — are reachable here rather than only by
 * leaving a phone running for an hour.
 */
class StopwatchStateTest {

    private val boot = 1_000_000L

    @Test
    fun a_fresh_stopwatch_is_idle_at_zero() {
        val state = StopwatchState()
        assertTrue(state.isIdle)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(0L, state.elapsedAt(5_000L))
    }

    @Test
    fun elapsed_time_comes_from_the_monotonic_clock_not_from_ticks() {
        // The whole point: nothing counted the intervening 90 seconds, they are simply the
        // difference between two readings. This is what keeps it right in the background.
        val running = StopwatchState().start(nowRealtime = 10_000L, bootMarker = boot)
        assertEquals(90_000L, running.elapsedAt(100_000L))
    }

    @Test
    fun a_very_long_background_spell_is_still_accurate() {
        // Eight hours asleep. A per-second counter in a suspended process would be wildly short.
        val running = StopwatchState().start(nowRealtime = 0L, bootMarker = boot)
        val eightHours = 8 * 60 * 60 * 1_000L
        assertEquals(eightHours, running.elapsedAt(eightHours))
    }

    @Test
    fun pausing_banks_the_elapsed_time_and_freezes_the_readout() {
        val paused = StopwatchState()
            .start(nowRealtime = 1_000L, bootMarker = boot)
            .pause(nowRealtime = 6_000L)

        assertEquals(5_000L, paused.accumulatedMillis)
        assertFalse(paused.isRunning)
        assertTrue(paused.isPaused)
        // Time moving on must not change a paused readout.
        assertEquals(5_000L, paused.elapsedAt(60_000L))
    }

    @Test
    fun resuming_continues_from_the_banked_total() {
        val state = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .pause(nowRealtime = 5_000L)
            .start(nowRealtime = 100_000L, bootMarker = boot)

        // The 95 seconds spent paused are not counted.
        assertEquals(5_000L, state.elapsedAt(100_000L))
        assertEquals(7_000L, state.elapsedAt(102_000L))
    }

    @Test
    fun start_is_idempotent_and_does_not_restart_the_run() {
        val running = StopwatchState().start(nowRealtime = 1_000L, bootMarker = boot)
        val again = running.start(nowRealtime = 9_000L, bootMarker = boot)
        assertEquals(running, again)
        assertEquals(8_000L, again.elapsedAt(9_000L))
    }

    @Test
    fun pause_is_idempotent() {
        val paused = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .pause(nowRealtime = 3_000L)
        assertEquals(paused, paused.pause(nowRealtime = 90_000L))
    }

    @Test
    fun reset_clears_everything() {
        val state = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .lap(nowRealtime = 1_000L)
            .pause(nowRealtime = 2_000L)
            .reset()
        assertTrue(state.isIdle)
        assertEquals(0L, state.accumulatedMillis)
        assertTrue(state.laps.isEmpty())
    }

    @Test
    fun a_backwards_monotonic_reading_never_shows_a_negative_duration() {
        val running = StopwatchState().start(nowRealtime = 10_000L, bootMarker = boot)
        assertEquals(0L, running.elapsedAt(9_000L))
    }

    // ---- laps ----------------------------------------------------------------------------

    @Test
    fun laps_are_stored_cumulatively_and_reported_as_splits_newest_first() {
        val state = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .lap(nowRealtime = 5_000L)
            .lap(nowRealtime = 12_000L)
            .lap(nowRealtime = 20_000L)

        val splits = state.lapSplits()
        assertEquals(3, splits.size)

        // Newest first, which is the display order.
        assertEquals(3, splits[0].number)
        assertEquals(8_000L, splits[0].splitMillis)
        assertEquals(20_000L, splits[0].totalMillis)

        assertEquals(2, splits[1].number)
        assertEquals(7_000L, splits[1].splitMillis)
        assertEquals(12_000L, splits[1].totalMillis)

        assertEquals(1, splits[2].number)
        assertEquals(5_000L, splits[2].splitMillis)
        assertEquals(5_000L, splits[2].totalMillis)
    }

    @Test
    fun lap_splits_always_add_up_to_the_total() {
        // The reason laps are stored cumulatively rather than as durations: the splits can never
        // drift out of step with the readout above them.
        val state = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .lap(nowRealtime = 3_333L)
            .lap(nowRealtime = 7_777L)
            .lap(nowRealtime = 9_001L)

        val splits = state.lapSplits()
        assertEquals(9_001L, splits.sumOf { it.splitMillis })
        assertEquals(9_001L, splits.first().totalMillis)
    }

    @Test
    fun a_lap_taken_while_paused_is_ignored() {
        val paused = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .pause(nowRealtime = 5_000L)
        assertEquals(paused, paused.lap(nowRealtime = 6_000L))
    }

    @Test
    fun laps_survive_a_pause_and_resume() {
        val state = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .lap(nowRealtime = 2_000L)
            .pause(nowRealtime = 3_000L)
            .start(nowRealtime = 10_000L, bootMarker = boot)
            .lap(nowRealtime = 11_000L)

        val splits = state.lapSplits()
        assertEquals(2, splits.size)
        // Second lap: 4s total, so a 2s split — the paused time is excluded.
        assertEquals(4_000L, splits[0].totalMillis)
        assertEquals(2_000L, splits[0].splitMillis)
    }

    // ---- reboot -------------------------------------------------------------------------

    @Test
    fun a_reboot_while_running_freezes_the_banked_time_rather_than_inventing_one() {
        // elapsedRealtime counts from boot, so a stored reading is meaningless after a restart.
        // There is no honest way to know how long it ran, so the banked time stands and it stops.
        val running = StopwatchState()
            .start(nowRealtime = 1_000L, bootMarker = boot)
            .pause(nowRealtime = 4_000L)
            .start(nowRealtime = 5_000L, bootMarker = boot)

        val restored = running.afterRestore(currentBootMarker = boot + 600_000L)

        assertFalse("must not still be running after a reboot", restored.isRunning)
        assertEquals("the time banked before the restart is kept", 3_000L, restored.accumulatedMillis)
    }

    @Test
    fun a_restore_within_the_same_boot_session_is_left_alone() {
        val running = StopwatchState().start(nowRealtime = 1_000L, bootMarker = boot)
        // Small drift between the two clocks is normal and must not read as a reboot.
        val restored = running.afterRestore(currentBootMarker = boot + 40L)
        assertTrue(restored.isRunning)
        assertEquals(running, restored)
    }

    @Test
    fun a_paused_stopwatch_is_unaffected_by_a_reboot() {
        val paused = StopwatchState()
            .start(nowRealtime = 0L, bootMarker = boot)
            .pause(nowRealtime = 7_500L)
        val restored = paused.afterRestore(currentBootMarker = boot + 999_999L)
        assertEquals(7_500L, restored.accumulatedMillis)
        assertFalse(restored.isRunning)
    }

    @Test
    fun state_with_no_stored_boot_marker_is_left_alone() {
        val state = StopwatchState(accumulatedMillis = 1_234L)
        assertEquals(state, state.afterRestore(currentBootMarker = boot))
    }
}
