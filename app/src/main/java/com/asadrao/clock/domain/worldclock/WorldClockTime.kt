package com.asadrao.clock.domain.worldclock

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Time calculations for the World clock.
 *
 * Everything is derived from `java.time` at the current instant, never from a stored offset. That
 * matters: a stored "+4" would be wrong for half the year in any zone that observes daylight
 * saving, and would silently drift as governments change their rules. Asking the zone for its
 * offset *now* is always right.
 */
object WorldClockTime {

    /** The current time in [zoneId]. */
    fun nowIn(zoneId: String, instant: Instant): ZonedDateTime =
        instant.atZone(ZoneId.of(zoneId))

    /**
     * How far ahead or behind [zoneId] is from [homeZoneId], as text.
     *
     * Handles the half-hour and three-quarter-hour zones — India, Nepal, parts of Australia — which
     * a naive whole-hour difference would misreport.
     */
    fun offsetDescription(
        zoneId: String,
        homeZoneId: String,
        instant: Instant,
        aheadSuffix: String = "ahead",
        behindSuffix: String = "behind",
        sameLabel: String = "Same time",
    ): String {
        val target = instant.atZone(ZoneId.of(zoneId)).offset.totalSeconds
        val home = instant.atZone(ZoneId.of(homeZoneId)).offset.totalSeconds
        val deltaSeconds = target - home
        if (deltaSeconds == 0) return sameLabel

        // Computed by hand rather than with Duration.toMinutesPart(), which is API 31 and would
        // crash on Android 8 through 11 — this app supports API 26.
        val absoluteSeconds = kotlin.math.abs(deltaSeconds.toLong())
        val hours = absoluteSeconds / 3_600L
        val minutes = ((absoluteSeconds % 3_600L) / 60L).toInt()
        val magnitude = when {
            minutes == 0 -> "$hours ${if (hours == 1L) "hour" else "hours"}"
            hours == 0L -> "$minutes min"
            else -> "$hours ${if (hours == 1L) "hr" else "hr"} $minutes min"
        }
        return "$magnitude ${if (deltaSeconds > 0) aheadSuffix else behindSuffix}"
    }

    /**
     * Whether a city's date is yesterday, today or tomorrow relative to home — which is the part
     * of a world clock people actually use it for.
     */
    fun dayOffset(zoneId: String, homeZoneId: String, instant: Instant): Long {
        val there: LocalDate = instant.atZone(ZoneId.of(zoneId)).toLocalDate()
        val here: LocalDate = instant.atZone(ZoneId.of(homeZoneId)).toLocalDate()
        return there.toEpochDay() - here.toEpochDay()
    }

    /**
     * A rough day/night flag, from local wall-clock hour.
     *
     * This is deliberately a heuristic: the tz database carries no coordinates, so a real
     * sunrise/sunset calculation would need a latitude and longitude per city, which would mean
     * bundling exactly the dataset this app avoids. Treating 06:00–18:00 as daylight is right most
     * of the year for most of the populated world, and wrong near the poles and at the edges of
     * the seasons. It drives nothing but a sun-or-moon glyph, so the cost of being wrong is small.
     */
    fun isDaytime(zoneId: String, instant: Instant): Boolean {
        val localTime: LocalTime = instant.atZone(ZoneId.of(zoneId)).toLocalTime()
        return localTime.hour in DAY_START_HOUR until NIGHT_START_HOUR
    }

    private const val DAY_START_HOUR = 6
    private const val NIGHT_START_HOUR = 18
}
