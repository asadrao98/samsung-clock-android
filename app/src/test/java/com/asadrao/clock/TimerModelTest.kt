package com.asadrao.clock

import com.asadrao.clock.domain.timer.ClockTimer
import com.asadrao.clock.domain.timer.durationMillisOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Timer arithmetic. Like the stopwatch, the remaining time is derived from a monotonic deadline
 * rather than decremented, and these cover the cases where that difference shows.
 */
class TimerModelTest {

    private fun timer(totalMillis: Long = 60_000L) = ClockTimer(
        id = 1L,
        totalMillis = totalMillis,
        remainingWhenPausedMillis = totalMillis,
    )

    @Test
    fun a_new_timer_is_paused_at_its_full_duration() {
        val t = timer()
        assertFalse(t.isRunning)
        assertEquals(60_000L, t.remainingAt(0L))
        assertEquals(0f, t.progressAt(0L), 0.0001f)
    }

    @Test
    fun remaining_time_is_the_deadline_minus_now() {
        // Nothing counted the intervening 20 seconds — which is why it stays right while the
        // process is not scheduled at all.
        val running = timer().start(nowRealtime = 1_000L)
        assertEquals(60_000L, running.remainingAt(1_000L))
        assertEquals(40_000L, running.remainingAt(21_000L))
    }

    @Test
    fun a_long_background_spell_is_still_accurate() {
        val running = timer(totalMillis = 10 * 60_000L).start(nowRealtime = 0L)
        assertEquals(60_000L, running.remainingAt(9 * 60_000L))
    }

    @Test
    fun remaining_never_goes_negative_and_the_timer_reads_finished() {
        val running = timer().start(nowRealtime = 0L)
        assertEquals(0L, running.remainingAt(999_999L))
        assertTrue(running.isFinishedAt(999_999L))
        assertFalse(running.isFinishedAt(1_000L))
    }

    @Test
    fun progress_runs_from_zero_to_one() {
        val running = timer().start(nowRealtime = 0L)
        assertEquals(0f, running.progressAt(0L), 0.001f)
        assertEquals(0.5f, running.progressAt(30_000L), 0.001f)
        assertEquals(1f, running.progressAt(60_000L), 0.001f)
        assertEquals("clamped past the end", 1f, running.progressAt(120_000L), 0.001f)
    }

    @Test
    fun a_zero_duration_timer_reports_complete_rather_than_dividing_by_zero() {
        val t = ClockTimer(id = 1L, totalMillis = 0L, remainingWhenPausedMillis = 0L)
        assertEquals(1f, t.progressAt(0L), 0.0001f)
    }

    @Test
    fun pausing_freezes_the_remaining_time() {
        val paused = timer().start(nowRealtime = 0L).pause(nowRealtime = 20_000L)
        assertFalse(paused.isRunning)
        assertEquals(40_000L, paused.remainingAt(20_000L))
        // Time moving on must not change a paused timer.
        assertEquals(40_000L, paused.remainingAt(500_000L))
    }

    @Test
    fun resuming_continues_from_what_was_left() {
        val resumed = timer()
            .start(nowRealtime = 0L)
            .pause(nowRealtime = 20_000L)
            .start(nowRealtime = 100_000L)
        // The 80 seconds spent paused are not deducted.
        assertEquals(40_000L, resumed.remainingAt(100_000L))
        assertEquals(30_000L, resumed.remainingAt(110_000L))
    }

    @Test
    fun start_and_pause_are_both_idempotent() {
        val running = timer().start(nowRealtime = 1_000L)
        assertEquals(running, running.start(nowRealtime = 9_000L))
        val paused = running.pause(nowRealtime = 2_000L)
        assertEquals(paused, paused.pause(nowRealtime = 8_000L))
    }

    @Test
    fun reset_returns_to_the_dialled_duration() {
        val reset = timer()
            .start(nowRealtime = 0L)
            .pause(nowRealtime = 45_000L)
            .reset()
        assertFalse(reset.isRunning)
        assertEquals(60_000L, reset.remainingAt(0L))
    }

    @Test
    fun add_a_minute_to_a_running_timer_extends_the_deadline_and_the_total() {
        val running = timer().start(nowRealtime = 0L)
        val extended = running.addMinute(nowRealtime = 10_000L)

        assertTrue(extended.isRunning)
        // 50 seconds were left; a minute more makes 110.
        assertEquals(110_000L, extended.remainingAt(10_000L))
        // The total grows too, so the ring does not jump backwards.
        assertEquals(120_000L, extended.totalMillis)
    }

    @Test
    fun add_a_minute_to_a_paused_timer_extends_what_is_left() {
        val paused = timer().start(nowRealtime = 0L).pause(nowRealtime = 20_000L)
        val extended = paused.addMinute(nowRealtime = 20_000L)
        assertFalse(extended.isRunning)
        assertEquals(100_000L, extended.remainingAt(20_000L))
        assertEquals(120_000L, extended.totalMillis)
    }

    @Test
    fun adding_a_minute_to_a_finished_timer_gives_it_a_fresh_minute() {
        // The "+1 min" action on the finished notification: it should ring again in a minute,
        // not immediately.
        val finished = ClockTimer(id = 1L, totalMillis = 60_000L, remainingWhenPausedMillis = 0L)
        val extended = finished.addMinute(nowRealtime = 0L).start(nowRealtime = 0L)
        assertEquals(60_000L, extended.remainingAt(0L))
    }

    @Test
    fun duration_conversion_matches_hours_minutes_seconds() {
        assertEquals(0L, durationMillisOf(0, 0, 0))
        assertEquals(1_000L, durationMillisOf(0, 0, 1))
        assertEquals(60_000L, durationMillisOf(0, 1, 0))
        assertEquals(3_600_000L, durationMillisOf(1, 0, 0))
        assertEquals(3_661_000L, durationMillisOf(1, 1, 1))
        assertEquals(23 * 3_600_000L + 59 * 60_000L + 59_000L, durationMillisOf(23, 59, 59))
    }
}
