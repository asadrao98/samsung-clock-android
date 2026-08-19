package com.asadrao.clock.domain.schedule

import com.asadrao.clock.domain.model.Alarm
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Works out when an alarm should next go off.
 *
 * Everything here is pure and takes its "now" as an argument, so the awkward cases — a minute
 * from now, across midnight, across a DST boundary — are all reachable from a unit test
 * instead of only from a device with its clock wound forward.
 *
 * Two rules underpin the whole file:
 *
 * 1. **Strictly in the future.** A trigger equal to `from` is never returned. This is what
 *    stops a repeating alarm from re-arming onto the instant it just fired and ringing forever.
 *
 * 2. **Wall-clock time wins over elapsed time.** An alarm set for 07:00 means 07:00 on the
 *    clock in the room, so the calculation is done in local dates and times and only then
 *    resolved to an instant. Doing the arithmetic on epoch millis would drift by an hour twice
 *    a year.
 */
object AlarmSchedule {

    /** How far ahead to look for a matching repeat day. Seven days plus today's wrap-around. */
    private const val SEARCH_DAYS = 8

    /**
     * The next instant [alarm] should ring, strictly after [from].
     *
     * Honours [from]'s time zone, so a timezone change is handled simply by recomputing with a
     * `from` in the new zone.
     */
    fun nextTrigger(alarm: Alarm, from: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(alarm.hour, alarm.minute)
        val zone = from.zone

        if (!alarm.repeats) {
            // Today if the time is still ahead of us, otherwise tomorrow.
            val today = resolve(from, time)
            return if (today.isAfter(from)) today else resolve(from.plusDays(1), time)
        }

        for (dayOffset in 0 until SEARCH_DAYS) {
            val date = from.toLocalDate().plusDays(dayOffset.toLong())
            if (date.dayOfWeek !in alarm.repeatDays) continue
            val candidate = date.atTime(time).atZone(zone)
            if (candidate.isAfter(from)) return candidate
        }

        // Unreachable while repeatDays is non-empty: any chosen weekday recurs within 7 days,
        // and the 8th iteration covers "today, but the time has already passed".
        error("no trigger found for repeating alarm ${alarm.id} within $SEARCH_DAYS days")
    }

    /** Convenience for production callers that just want "next, from now". */
    fun nextTrigger(alarm: Alarm, clock: Clock): ZonedDateTime =
        nextTrigger(alarm, ZonedDateTime.now(clock))

    /**
     * When a snooze taken at [snoozedAt] should ring.
     *
     * Deliberately computed by adding minutes to an instant rather than by manipulating local
     * time: a snooze is a stretch of elapsed time the user asked for, so "5 more minutes"
     * across a spring-forward boundary must still be five real minutes, not an hour and five.
     * This is the one place in the file where elapsed time, not wall-clock time, is correct.
     */
    fun snoozeTrigger(alarm: Alarm, snoozedAt: ZonedDateTime): ZonedDateTime =
        snoozedAt.plusMinutes(alarm.snoozeDurationMinutes.toLong())

    /**
     * Resolves [time] on the date of [onDate]'s local day, in that zone.
     *
     * Where a DST gap swallows the requested time — 02:30 on a morning that jumps 02:00 to
     * 03:00 — `java.time` moves the result forward past the gap, so the alarm still rings
     * rather than being skipped for the year. Where an overlap repeats it, the earlier of the
     * two offsets is chosen, so it rings once, at the first 01:30 rather than the second.
     */
    private fun resolve(onDate: ZonedDateTime, time: LocalTime): ZonedDateTime =
        onDate.toLocalDate().atTime(time).atZone(onDate.zone)
}
