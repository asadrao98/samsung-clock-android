package com.asadrao.clock

import com.asadrao.clock.domain.timer.QuickDurations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-tap durations on the Timer tab.
 *
 * Samsung orders these by what you have actually used, so the timer you set yesterday is the first
 * one offered today — that ordering is the whole point of the feature and is what these cover.
 */
class QuickDurationsTest {

    private val oneMinute = 60_000L
    private val fiveMinutes = 5 * 60_000L
    private val twelveMinutes = 12 * 60_000L
    private val twoHours = 2 * 60 * 60_000L

    @Test
    fun with_no_history_the_defaults_are_shown() {
        assertEquals(QuickDurations.DEFAULTS, QuickDurations.forDisplay(emptyList()))
    }

    @Test
    fun recent_durations_come_first() {
        val shown = QuickDurations.forDisplay(listOf(twelveMinutes, twoHours))
        assertEquals(twelveMinutes, shown[0])
        assertEquals(twoHours, shown[1])
    }

    @Test
    fun a_recent_duration_that_is_also_a_default_is_not_repeated() {
        val shown = QuickDurations.forDisplay(listOf(fiveMinutes))
        assertEquals(fiveMinutes, shown.first())
        assertEquals("no duplicates", shown.size, shown.distinct().size)
    }

    @Test
    fun the_row_is_capped_so_it_still_fits() {
        val many = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L).map { it * 60_000L }
        assertEquals(QuickDurations.MAX_SHOWN, QuickDurations.forDisplay(many).size)
    }

    @Test
    fun defaults_fill_the_row_when_history_is_short() {
        val shown = QuickDurations.forDisplay(listOf(twelveMinutes))
        assertEquals(QuickDurations.MAX_SHOWN, shown.size)
        assertEquals(twelveMinutes, shown.first())
    }

    @Test
    fun a_nonsense_history_entry_is_ignored() {
        // Guards against a zero or negative value reaching the row from a corrupted store.
        val shown = QuickDurations.forDisplay(listOf(0L, -5L, twelveMinutes))
        assertEquals(twelveMinutes, shown.first())
        assertTrue(shown.none { it <= 0L })
    }

    @Test
    fun labels_are_compact_and_read_naturally() {
        assertEquals("30s", QuickDurations.label(30_000))
        assertEquals("1m", QuickDurations.label(oneMinute))
        assertEquals("15m", QuickDurations.label(15 * 60_000L))
        assertEquals("1h", QuickDurations.label(60 * 60_000L))
        assertEquals("1h 30m", QuickDurations.label(90 * 60_000L))
        assertEquals("2m 30s", QuickDurations.label(150_000))
    }

    @Test
    fun every_default_has_a_sensible_label() {
        QuickDurations.DEFAULTS.forEach { millis ->
            val label = QuickDurations.label(millis)
            assertTrue("empty label for $millis", label.isNotBlank())
            assertTrue("unexpected label '$label'", label.last() in listOf('s', 'm', 'h'))
        }
    }
}
