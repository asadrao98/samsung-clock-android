package com.asadrao.clock

import com.asadrao.clock.domain.model.RepeatDays
import com.asadrao.clock.ui.format.AlarmFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class AlarmFormatTest {

    private val uk = Locale.UK
    private val us = Locale.US
    private val dubai = ZoneId.of("Asia/Dubai")

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 8, day, hour, minute, 0, 0, dubai)

    // ---- time --------------------------------------------------------------------------

    @Test
    fun twenty_four_hour_times_are_zero_padded() {
        assertEquals("07:30", AlarmFormat.time(7, 30, is24Hour = true))
        assertEquals("00:00", AlarmFormat.time(0, 0, is24Hour = true))
        assertEquals("23:59", AlarmFormat.time(23, 59, is24Hour = true))
    }

    @Test
    fun twelve_hour_times_drop_the_leading_zero_but_keep_it_on_minutes() {
        assertEquals("7:30", AlarmFormat.time(7, 30, is24Hour = false))
        assertEquals("7:05", AlarmFormat.time(7, 5, is24Hour = false))
    }

    @Test
    fun midnight_and_noon_are_twelve_not_zero() {
        // The classic 12-hour bug: `hour % 12` gives 0 at both ends.
        assertEquals("12:00", AlarmFormat.time(0, 0, is24Hour = false))
        assertEquals("12:00", AlarmFormat.time(12, 0, is24Hour = false))
        assertEquals("12:30", AlarmFormat.time(0, 30, is24Hour = false))
    }

    @Test
    fun afternoon_hours_wrap_correctly() {
        assertEquals("1:00", AlarmFormat.time(13, 0, is24Hour = false))
        assertEquals("11:45", AlarmFormat.time(23, 45, is24Hour = false))
    }

    @Test
    fun meridiem_is_am_before_noon_and_pm_from_noon() {
        assertEquals("AM", AlarmFormat.meridiem(0, us, is24Hour = false))
        assertEquals("AM", AlarmFormat.meridiem(11, us, is24Hour = false))
        assertEquals("PM", AlarmFormat.meridiem(12, us, is24Hour = false))
        assertEquals("PM", AlarmFormat.meridiem(23, us, is24Hour = false))
    }

    @Test
    fun meridiem_is_absent_in_twenty_four_hour_mode() {
        // Must be null, not empty: the layout should not reserve space for it at all.
        assertNull(AlarmFormat.meridiem(9, us, is24Hour = true))
    }

    // ---- week order --------------------------------------------------------------------

    @Test
    fun week_order_follows_the_locale() {
        assertEquals(DayOfWeek.MONDAY, AlarmFormat.weekOrder(uk).first())
        assertEquals(DayOfWeek.SUNDAY, AlarmFormat.weekOrder(us).first())
        assertEquals(7, AlarmFormat.weekOrder(us).size)
        assertEquals(7, AlarmFormat.weekOrder(us).toSet().size)
    }

    @Test
    fun week_order_wraps_without_repeating_or_dropping_a_day() {
        val order = AlarmFormat.weekOrder(us)
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            ),
            order,
        )
    }

    // ---- repeat summary ----------------------------------------------------------------

    private fun summary(days: RepeatDays, locale: Locale = uk) = AlarmFormat.repeatSummary(
        repeatDays = days,
        locale = locale,
        everyDayLabel = "Every day",
        weekdaysLabel = "Mon-Fri",
        weekendsLabel = "Sat-Sun",
    )

    @Test
    fun a_non_repeating_alarm_has_no_repeat_summary() {
        assertNull(summary(RepeatDays.None))
    }

    @Test
    fun the_common_patterns_get_their_own_wording() {
        assertEquals("Every day", summary(RepeatDays.EveryDay))
        assertEquals("Mon-Fri", summary(RepeatDays.WEEKDAYS))
        assertEquals("Sat-Sun", summary(RepeatDays.WEEKENDS))
    }

    @Test
    fun arbitrary_day_sets_are_listed_in_locale_week_order() {
        val days = RepeatDays.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        // UK starts on Monday, so Sunday lands last.
        assertEquals("Mon, Wed, Sun", summary(days, uk))
        // US starts on Sunday, so it leads.
        assertEquals("Sun, Mon, Wed", summary(days, us))
    }

    @Test
    fun a_single_repeat_day_reads_as_that_day() {
        assertEquals("Fri", summary(RepeatDays.of(DayOfWeek.FRIDAY)))
    }

    // ---- one-shot date -----------------------------------------------------------------

    private fun dateLabel(trigger: ZonedDateTime, now: ZonedDateTime) = AlarmFormat.oneShotDate(
        nextTrigger = trigger, now = now, locale = uk,
        todayLabel = "Today", tomorrowLabel = "Tomorrow",
    )

    @Test
    fun later_today_reads_as_today() {
        assertEquals("Today", dateLabel(at(19, 22, 0), at(19, 8, 0)))
    }

    @Test
    fun the_next_calendar_day_reads_as_tomorrow_even_if_only_hours_away() {
        // 23:30 today to 06:00 tomorrow is under seven hours, but it is still tomorrow.
        assertEquals("Tomorrow", dateLabel(at(20, 6, 0), at(19, 23, 30)))
    }

    @Test
    fun a_long_way_off_reads_as_a_date() {
        assertEquals("Wed, 26 Aug", dateLabel(at(26, 7, 0), at(19, 8, 0)))
    }

    // ---- time until --------------------------------------------------------------------

    private fun until(trigger: ZonedDateTime, now: ZonedDateTime) = AlarmFormat.timeUntil(
        nextTrigger = trigger, now = now,
        hourUnit = "hr", minuteUnit = "min", lessThanAMinute = "less than a minute",
    )

    @Test
    fun hours_and_minutes_are_both_shown_when_both_apply() {
        assertEquals("7 hr 30 min", until(at(19, 15, 30), at(19, 8, 0)))
    }

    @Test
    fun a_whole_number_of_hours_omits_the_minutes() {
        assertEquals("8 hr", until(at(19, 16, 0), at(19, 8, 0)))
    }

    @Test
    fun under_an_hour_shows_minutes_only() {
        assertEquals("45 min", until(at(19, 8, 45), at(19, 8, 0)))
    }

    @Test
    fun partial_minutes_round_up() {
        // 30 minutes and 30 seconds away. Saying "30 min" would be a minute optimistic.
        val now = at(19, 8, 0)
        val trigger = now.plusMinutes(30).plusSeconds(30)
        assertEquals("31 min", until(trigger, now))
    }

    @Test
    fun rounding_up_can_carry_into_the_hour() {
        val now = at(19, 8, 0)
        val trigger = now.plusMinutes(59).plusSeconds(30)
        assertEquals("1 hr", until(trigger, now))
    }

    @Test
    fun under_a_minute_gets_its_own_wording() {
        val now = at(19, 8, 0)
        assertEquals("less than a minute", until(now.plusSeconds(40), now))
    }

    @Test
    fun exactly_one_minute_is_not_treated_as_less_than_a_minute() {
        val now = at(19, 8, 0)
        assertEquals("1 min", until(now.plusSeconds(60), now))
    }
}
