package com.asadrao.clock.ui.format

import com.asadrao.clock.domain.model.RepeatDays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Turns alarm data into the strings the UI shows.
 *
 * Free of Android types — `is24Hour` and `locale` are passed in rather than read from a
 * `Context` — so all of it is unit-testable, including the fiddly cases: midnight and noon in
 * 12-hour mode, and a week that does not start on Monday.
 *
 * Times are formatted explicitly as `H:mm` rather than through
 * `DateTimeFormatter.ofLocalizedTime`. A localised short time can carry a narrow no-break space,
 * a trailing period, or a different separator, all of which would break the deliberate
 * arrangement of a large time with a small meridiem beside it. The one thing that genuinely
 * must follow the system is the 12-versus-24-hour choice, and that is honoured.
 */
object AlarmFormat {

    /** The time itself, with no AM/PM. In 24-hour mode the hour is zero-padded; in 12-hour it is not. */
    fun time(hour: Int, minute: Int, is24Hour: Boolean): String {
        require(hour in 0..23) { "hour must be 0..23, got $hour" }
        require(minute in 0..59) { "minute must be 0..59, got $minute" }
        return if (is24Hour) {
            "%02d:%02d".format(hour, minute)
        } else {
            "%d:%02d".format(twelveHour(hour), minute)
        }
    }

    /** AM/PM, localised. Null in 24-hour mode, where the UI must not reserve room for it. */
    fun meridiem(hour: Int, locale: Locale, is24Hour: Boolean): String? {
        if (is24Hour) return null
        return DateTimeFormatter.ofPattern("a", locale).format(LocalTime.of(hour, 0))
    }

    /** 0 -> 12, 13 -> 1. The two ends are where naive `% 12` arithmetic goes wrong. */
    private fun twelveHour(hour: Int): Int = when (val h = hour % 12) {
        0 -> 12
        else -> h
    }

    /**
     * The weekday order to lay chips out in, starting from the locale's first day of the week.
     * Monday in most of Europe, Sunday in the US, Saturday in much of the Gulf.
     */
    fun weekOrder(locale: Locale): List<DayOfWeek> {
        val first = WeekFields.of(locale).firstDayOfWeek
        return (0..6).map { first.plus(it.toLong()) }
    }

    /** A single day's short name, e.g. "Mon". */
    fun dayShortName(day: DayOfWeek, locale: Locale): String =
        day.getDisplayName(TextStyle.SHORT, locale)

    /** A single day's narrowest name, for the compact chip row, e.g. "M". */
    fun dayNarrowName(day: DayOfWeek, locale: Locale): String =
        day.getDisplayName(TextStyle.NARROW, locale)

    /**
     * The repeat line under an alarm's time, or null when the alarm does not repeat — in which
     * case the caller shows a date via [oneShotDate] instead.
     *
     * Days are listed in the locale's week order, not Monday-first, so a US user sees
     * "Sun, Mon" rather than "Mon, Sun".
     */
    fun repeatSummary(
        repeatDays: RepeatDays,
        locale: Locale,
        everyDayLabel: String,
        weekdaysLabel: String,
        weekendsLabel: String,
    ): String? = when {
        repeatDays.isEmpty -> null
        repeatDays.isEveryDay -> everyDayLabel
        repeatDays.isWeekdaysOnly -> weekdaysLabel
        repeatDays.isWeekendsOnly -> weekendsLabel
        else -> weekOrder(locale)
            .filter { it in repeatDays }
            .joinToString(", ") { dayShortName(it, locale) }
    }

    /**
     * The date a one-shot alarm will ring on: "Today", "Tomorrow", or a short date.
     *
     * Compared by calendar date rather than by hours between, so an alarm 20 hours away still
     * reads "Tomorrow" instead of "Today".
     */
    fun oneShotDate(
        nextTrigger: ZonedDateTime,
        now: ZonedDateTime,
        locale: Locale,
        todayLabel: String,
        tomorrowLabel: String,
    ): String {
        val today = now.toLocalDate()
        return when (nextTrigger.toLocalDate()) {
            today -> todayLabel
            today.plusDays(1) -> tomorrowLabel
            else -> DateTimeFormatter.ofPattern("EEE, d MMM", locale)
                .format(nextTrigger.toLocalDate())
        }
    }

    /**
     * How long until the alarm rings, for the confirmation shown when one is switched on.
     *
     * Rounds *up* to the next whole minute. Saying "in 7 hours and 29 minutes" when 7h29m30s
     * remain reads as wrong to anyone watching the clock; rounding up matches how people talk
     * about waiting.
     */
    fun timeUntil(
        nextTrigger: ZonedDateTime,
        now: ZonedDateTime,
        hourUnit: String,
        minuteUnit: String,
        lessThanAMinute: String,
    ): String {
        val seconds = ChronoUnit.SECONDS.between(now, nextTrigger)
        if (seconds < 60) return lessThanAMinute
        val totalMinutes = (seconds + 59) / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0L -> "$minutes $minuteUnit"
            minutes == 0L -> "$hours $hourUnit"
            else -> "$hours $hourUnit $minutes $minuteUnit"
        }
    }

    /** Used by tests and previews to build a date without pulling in Android. */
    internal fun dateOf(year: Int, month: Int, day: Int): LocalDate =
        LocalDate.of(year, month, day)
}
