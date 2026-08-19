package com.asadrao.clock

import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.model.RepeatDays
import com.asadrao.clock.domain.schedule.AlarmSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The alarm arithmetic, including the cases that are painful to reach on a device: a minute
 * from now, across midnight, and both directions of a DST transition.
 */
class AlarmScheduleTest {

    private val dubai = ZoneId.of("Asia/Dubai")
    private val newYork = ZoneId.of("America/New_York")

    private fun alarm(
        hour: Int,
        minute: Int,
        repeat: RepeatDays = RepeatDays.None,
    ) = Alarm(id = 1, hour = hour, minute = minute, repeatDays = repeat)

    private fun at(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        zone: ZoneId = dubai,
    ) = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    // ---- one-shot alarms ------------------------------------------------------------------

    @Test
    fun a_one_shot_alarm_later_today_fires_today() {
        val next = AlarmSchedule.nextTrigger(alarm(7, 30), at(2026, 8, 19, 6, 0))
        assertEquals(at(2026, 8, 19, 7, 30), next)
    }

    @Test
    fun a_one_shot_alarm_already_past_today_rolls_to_tomorrow() {
        val next = AlarmSchedule.nextTrigger(alarm(7, 30), at(2026, 8, 19, 9, 0))
        assertEquals(at(2026, 8, 20, 7, 30), next)
    }

    @Test
    fun one_minute_from_now_fires_in_one_minute() {
        // Listed explicitly in the brief's edge cases: the classic off-by-one where an alarm
        // set moments ahead lands a whole day away.
        val next = AlarmSchedule.nextTrigger(alarm(6, 1), at(2026, 8, 19, 6, 0))
        assertEquals(at(2026, 8, 19, 6, 1), next)
    }

    @Test
    fun an_alarm_for_the_current_minute_goes_to_tomorrow_not_now() {
        // Strictly-future matters here: returning `from` itself would make a firing alarm
        // immediately re-arm onto its own instant.
        val next = AlarmSchedule.nextTrigger(alarm(6, 0), at(2026, 8, 19, 6, 0))
        assertEquals(at(2026, 8, 20, 6, 0), next)
    }

    @Test
    fun just_before_midnight_an_early_alarm_crosses_into_tomorrow() {
        val next = AlarmSchedule.nextTrigger(alarm(0, 5), at(2026, 8, 19, 23, 58))
        assertEquals(at(2026, 8, 20, 0, 5), next)
    }

    @Test
    fun just_after_midnight_a_late_alarm_is_still_the_same_day() {
        val next = AlarmSchedule.nextTrigger(alarm(23, 55), at(2026, 8, 20, 0, 2))
        assertEquals(at(2026, 8, 20, 23, 55), next)
    }

    // ---- repeating alarms ----------------------------------------------------------------

    @Test
    fun a_repeating_alarm_finds_the_next_matching_weekday() {
        // 19 Aug 2026 is a Wednesday.
        val start = at(2026, 8, 19, 12, 0)
        assertEquals(DayOfWeek.WEDNESDAY, start.dayOfWeek)
        val next = AlarmSchedule.nextTrigger(
            alarm(7, 0, RepeatDays.of(DayOfWeek.FRIDAY)),
            start,
        )
        assertEquals(at(2026, 8, 21, 7, 0), next)
    }

    @Test
    fun a_repeating_alarm_can_fire_later_the_same_day() {
        val next = AlarmSchedule.nextTrigger(
            alarm(18, 0, RepeatDays.of(DayOfWeek.WEDNESDAY)),
            at(2026, 8, 19, 12, 0),
        )
        assertEquals(at(2026, 8, 19, 18, 0), next)
    }

    @Test
    fun a_once_weekly_alarm_whose_time_has_passed_waits_a_full_week() {
        // The eighth day of the search exists for exactly this: today matches, but too late.
        val next = AlarmSchedule.nextTrigger(
            alarm(7, 0, RepeatDays.of(DayOfWeek.WEDNESDAY)),
            at(2026, 8, 19, 9, 0),
        )
        assertEquals(at(2026, 8, 26, 7, 0), next)
    }

    @Test
    fun every_day_repeats_daily() {
        val next = AlarmSchedule.nextTrigger(
            alarm(7, 0, RepeatDays.EveryDay),
            at(2026, 8, 19, 7, 30),
        )
        assertEquals(at(2026, 8, 20, 7, 0), next)
    }

    @Test
    fun weekdays_only_skips_the_weekend() {
        // Friday 21 Aug 2026, after the alarm time: next is Monday, not Saturday.
        val friday = at(2026, 8, 21, 9, 0)
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        val next = AlarmSchedule.nextTrigger(alarm(7, 0, RepeatDays.WEEKDAYS), friday)
        assertEquals(at(2026, 8, 24, 7, 0), next)
        assertEquals(DayOfWeek.MONDAY, next.dayOfWeek)
    }

    @Test
    fun weekends_only_skips_the_working_week() {
        val next = AlarmSchedule.nextTrigger(
            alarm(9, 0, RepeatDays.WEEKENDS),
            at(2026, 8, 19, 12, 0),
        )
        assertEquals(at(2026, 8, 22, 9, 0), next)
        assertEquals(DayOfWeek.SATURDAY, next.dayOfWeek)
    }

    // ---- daylight saving -----------------------------------------------------------------

    @Test
    fun an_alarm_inside_a_spring_forward_gap_still_rings() {
        // New York moves 02:00 to 03:00 on 8 Mar 2026, so 02:30 does not exist that morning.
        // The alarm must not be silently skipped for the year — it shifts past the gap.
        val next = AlarmSchedule.nextTrigger(
            alarm(2, 30),
            at(2026, 3, 8, 1, 0, newYork),
        )
        assertEquals(3, next.hour)
        assertEquals(30, next.minute)
        assertEquals(2026, next.year)
        assertEquals(3, next.monthValue)
        assertEquals(8, next.dayOfMonth)
    }

    @Test
    fun an_alarm_inside_a_fall_back_overlap_rings_once_at_the_first_occurrence() {
        // New York repeats 01:00-02:00 on 1 Nov 2026. 01:30 happens twice; the alarm should
        // take the earlier offset, not ring twice and not ring an hour late.
        val next = AlarmSchedule.nextTrigger(
            alarm(1, 30),
            at(2026, 11, 1, 0, 30, newYork),
        )
        assertEquals(1, next.hour)
        assertEquals(30, next.minute)
        // -04:00 is EDT, the first of the two 01:30s. -05:00 would be the repeat.
        assertEquals("-04:00", next.offset.id)
    }

    @Test
    fun a_daily_alarm_keeps_its_wall_clock_time_across_spring_forward() {
        // The point of doing the arithmetic in local time: on the morning the clocks go
        // forward, a 07:00 alarm is still 07:00 — so only 23 real hours separate it from
        // yesterday's. Measured between two consecutive occurrences, not from an arbitrary
        // "now", so the gap being tested is the day, not the offset into it.
        val a = alarm(7, 0, RepeatDays.EveryDay)
        val saturday = AlarmSchedule.nextTrigger(a, at(2026, 3, 7, 6, 0, newYork))
        val sunday = AlarmSchedule.nextTrigger(a, saturday)

        assertEquals(7, saturday.dayOfMonth)
        assertEquals(8, sunday.dayOfMonth)
        assertEquals(7, sunday.hour)
        assertEquals(0, sunday.minute)
        assertEquals(23L, java.time.Duration.between(saturday, sunday).toHours())
    }

    @Test
    fun a_daily_alarm_keeps_its_wall_clock_time_across_fall_back() {
        // The mirror case: 25 real hours, still 07:00 on the clock.
        val a = alarm(7, 0, RepeatDays.EveryDay)
        val saturday = AlarmSchedule.nextTrigger(a, at(2026, 10, 31, 6, 0, newYork))
        val sunday = AlarmSchedule.nextTrigger(a, saturday)

        assertEquals(1, sunday.dayOfMonth)
        assertEquals(11, sunday.monthValue)
        assertEquals(7, sunday.hour)
        assertEquals(25L, java.time.Duration.between(saturday, sunday).toHours())
    }

    @Test
    fun a_zone_without_dst_has_exactly_twenty_four_hours_between_occurrences() {
        val a = alarm(7, 0, RepeatDays.EveryDay)
        val first = AlarmSchedule.nextTrigger(a, at(2026, 3, 7, 6, 0, dubai))
        val second = AlarmSchedule.nextTrigger(a, first)
        assertEquals(24L, java.time.Duration.between(first, second).toHours())
    }

    // ---- snooze --------------------------------------------------------------------------

    @Test
    fun snooze_adds_elapsed_minutes_not_wall_clock_minutes() {
        // Snoozing at 01:59 into a spring-forward gap: the user asked for 5 more minutes of
        // sleep and must get 5, even though the wall clock jumps an hour.
        val snoozedAt = at(2026, 3, 8, 1, 59, newYork)
        val a = Alarm(id = 1, hour = 1, minute = 59, snoozeDurationMinutes = 5)
        val ring = AlarmSchedule.snoozeTrigger(a, snoozedAt)
        assertEquals(5L, java.time.Duration.between(snoozedAt, ring).toMinutes())
        // Wall clock reads 03:04, because 02:00-03:00 never happened.
        assertEquals(3, ring.hour)
        assertEquals(4, ring.minute)
    }

    @Test
    fun snooze_respects_a_custom_duration() {
        val snoozedAt = at(2026, 8, 19, 7, 0)
        val a = Alarm(id = 1, hour = 7, minute = 0, snoozeDurationMinutes = 30)
        assertEquals(at(2026, 8, 19, 7, 30), AlarmSchedule.snoozeTrigger(a, snoozedAt))
    }

    // ---- timezone travel -----------------------------------------------------------------

    @Test
    fun recomputing_in_a_new_zone_keeps_the_alarm_at_local_wall_clock_time() {
        // Fly Dubai to New York: a 07:00 alarm must ring at 07:00 New York time, which is a
        // different instant from the one that was scheduled in Dubai.
        val a = alarm(7, 0, RepeatDays.EveryDay)
        val inDubai = AlarmSchedule.nextTrigger(a, at(2026, 8, 19, 12, 0, dubai))
        val inNewYork = AlarmSchedule.nextTrigger(a, at(2026, 8, 19, 3, 0, newYork))
        assertEquals(7, inDubai.hour)
        assertEquals(7, inNewYork.hour)
        assertTrue(inDubai.toInstant() != inNewYork.toInstant())
    }
}
