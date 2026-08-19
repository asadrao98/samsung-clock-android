package com.asadrao.clock

import com.asadrao.clock.domain.model.RepeatDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class RepeatDaysTest {

    @Test
    fun none_is_empty_and_contains_nothing() {
        val none = RepeatDays.None
        assertTrue(none.isEmpty)
        assertEquals(0, none.count)
        DayOfWeek.entries.forEach { assertFalse(it in none) }
    }

    @Test
    fun every_day_contains_all_seven() {
        val all = RepeatDays.EveryDay
        assertTrue(all.isEveryDay)
        assertEquals(7, all.count)
        DayOfWeek.entries.forEach { assertTrue(it in all) }
    }

    @Test
    fun adding_and_removing_days_is_symmetric() {
        var days = RepeatDays.None.with(DayOfWeek.TUESDAY).with(DayOfWeek.SATURDAY)
        assertEquals(2, days.count)
        assertTrue(DayOfWeek.TUESDAY in days)
        days = days.without(DayOfWeek.TUESDAY)
        assertFalse(DayOfWeek.TUESDAY in days)
        assertTrue(DayOfWeek.SATURDAY in days)
        assertEquals(1, days.count)
    }

    @Test
    fun adding_a_day_twice_changes_nothing() {
        val once = RepeatDays.None.with(DayOfWeek.MONDAY)
        assertEquals(once, once.with(DayOfWeek.MONDAY))
    }

    @Test
    fun removing_an_absent_day_changes_nothing() {
        val days = RepeatDays.of(DayOfWeek.MONDAY)
        assertEquals(days, days.without(DayOfWeek.FRIDAY))
    }

    @Test
    fun toggle_flips_a_day_both_ways() {
        val on = RepeatDays.None.toggle(DayOfWeek.WEDNESDAY)
        assertTrue(DayOfWeek.WEDNESDAY in on)
        assertFalse(DayOfWeek.WEDNESDAY in on.toggle(DayOfWeek.WEDNESDAY))
    }

    @Test
    fun weekday_and_weekend_presets_partition_the_week() {
        assertEquals(5, RepeatDays.WEEKDAYS.count)
        assertEquals(2, RepeatDays.WEEKENDS.count)
        assertTrue(RepeatDays.WEEKDAYS.isWeekdaysOnly)
        assertTrue(RepeatDays.WEEKENDS.isWeekendsOnly)
        // Together they are every day, and they never overlap.
        assertEquals(
            RepeatDays.EveryDay,
            RepeatDays(RepeatDays.WEEKDAYS.bits or RepeatDays.WEEKENDS.bits),
        )
        assertEquals(0, RepeatDays.WEEKDAYS.bits and RepeatDays.WEEKENDS.bits)
        assertFalse(DayOfWeek.SATURDAY in RepeatDays.WEEKDAYS)
        assertTrue(DayOfWeek.SUNDAY in RepeatDays.WEEKENDS)
    }

    @Test
    fun bit_layout_is_monday_first_and_locale_independent() {
        // Pinned deliberately: these bits are persisted, so the mapping must never drift.
        assertEquals(1, RepeatDays.of(DayOfWeek.MONDAY).bits)
        assertEquals(1 shl 6, RepeatDays.of(DayOfWeek.SUNDAY).bits)
    }

    @Test
    fun to_set_is_ordered_monday_first() {
        val days = RepeatDays.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
            days.toSet().toList(),
        )
    }

    @Test
    fun bits_outside_the_seven_day_range_are_rejected() {
        // Guards against a corrupt or hand-edited database row becoming a silent eighth day.
        assertThrows(IllegalArgumentException::class.java) { RepeatDays(0b1000_0000) }
        assertThrows(IllegalArgumentException::class.java) { RepeatDays(-1) }
    }

    @Test
    fun round_trips_through_its_integer_form() {
        val original = RepeatDays.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)
        assertEquals(original, RepeatDays(original.bits))
    }
}
