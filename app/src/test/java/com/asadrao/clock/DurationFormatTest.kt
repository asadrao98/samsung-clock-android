package com.asadrao.clock

import com.asadrao.clock.ui.format.DurationFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun stopwatch_shows_minutes_seconds_and_hundredths() {
        assertEquals("00:00.00", DurationFormat.stopwatch(0))
        assertEquals("00:05.43", DurationFormat.stopwatch(5_430))
        assertEquals("01:00.00", DurationFormat.stopwatch(60_000))
        assertEquals("59:59.99", DurationFormat.stopwatch(3_599_990))
    }

    @Test
    fun stopwatch_grows_an_hour_field_only_when_needed() {
        assertEquals("1:00:00.00", DurationFormat.stopwatch(3_600_000))
        assertEquals("2:03:04.05", DurationFormat.stopwatch(2 * 3_600_000 + 3 * 60_000 + 4_000 + 50))
    }

    @Test
    fun the_minute_field_is_always_two_digits_so_the_readout_does_not_shift() {
        // A readout that gains a character as it crosses ten minutes makes the whole block jump
        // sideways. Tabular figures do not help with a changing character count.
        assertEquals("09:59.00".length, DurationFormat.stopwatch(599_000).length)
        assertEquals("10:00.00".length, DurationFormat.stopwatch(600_000).length)
    }

    @Test
    fun negative_input_is_clamped_rather_than_rendered() {
        assertEquals("00:00.00", DurationFormat.stopwatch(-5_000))
        assertEquals("0:00", DurationFormat.timer(-1))
    }

    @Test
    fun whole_and_hundredths_split_matches_the_combined_form() {
        val millis = 754_321L
        assertEquals(
            DurationFormat.stopwatch(millis),
            DurationFormat.stopwatchWhole(millis) + "." + DurationFormat.hundredths(millis),
        )
    }

    @Test
    fun timer_rounds_up_so_a_countdown_starts_at_its_full_value() {
        // Starting a 1-minute timer must read 1:00, not flick straight to 0:59.
        assertEquals("1:00", DurationFormat.timer(60_000))
        assertEquals("1:00", DurationFormat.timer(59_999))
        assertEquals("0:59", DurationFormat.timer(59_000))
        assertEquals("0:01", DurationFormat.timer(1))
        assertEquals("0:00", DurationFormat.timer(0))
    }

    @Test
    fun timer_grows_an_hour_field_only_when_needed() {
        assertEquals("1:00:00", DurationFormat.timer(3_600_000))
        assertEquals("59:59", DurationFormat.timer(3_599_000))
    }

    @Test
    fun spoken_form_is_pluralised_for_screen_readers() {
        assertEquals("1 minute", DurationFormat.spoken(60_000))
        assertEquals("2 minutes", DurationFormat.spoken(120_000))
        assertEquals("1 hour 1 minute 1 second", DurationFormat.spoken(3_661_000))
        assertEquals("0 seconds", DurationFormat.spoken(0))
        assertEquals("1 second", DurationFormat.spoken(1_000))
    }
}
