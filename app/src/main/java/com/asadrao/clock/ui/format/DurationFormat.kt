package com.asadrao.clock.ui.format

/**
 * Formats stopwatch and timer readouts.
 *
 * The hour field appears only once it is needed, which is what Samsung does, but the *minute* field
 * is always two digits so the string does not change width as it crosses ten minutes. A readout
 * that grows a character mid-run makes the whole block shift sideways, which is very visible at
 * this type size — tabular figures alone do not save you from that.
 */
object DurationFormat {

    /** `mm:ss.hh`, growing to `h:mm:ss.hh` past an hour. Used by the stopwatch. */
    fun stopwatch(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val hundredths = (safe / 10) % 100
        val seconds = (safe / 1_000) % 60
        val minutes = (safe / 60_000) % 60
        val hours = safe / 3_600_000
        return if (hours > 0) {
            "%d:%02d:%02d.%02d".format(hours, minutes, seconds, hundredths)
        } else {
            "%02d:%02d.%02d".format(minutes, seconds, hundredths)
        }
    }

    /** The main part of a stopwatch readout, without the hundredths. */
    fun stopwatchWhole(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val seconds = (safe / 1_000) % 60
        val minutes = (safe / 60_000) % 60
        val hours = safe / 3_600_000
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /** Just the hundredths, shown smaller and trailing the main readout. */
    fun hundredths(millis: Long): String = "%02d".format((millis.coerceAtLeast(0L) / 10) % 100)

    /**
     * `h:mm:ss` for a countdown, dropping the hour field below an hour.
     *
     * Rounds **up** to the next whole second, so a timer started at 60 seconds reads "1:00" rather
     * than flicking straight to "0:59". Counting down through zero should reach 0 exactly as the
     * alarm sounds, not a second early.
     */
    fun timer(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val totalSeconds = (safe + 999) / 1_000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3_600
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** Spoken form, for screen readers: "5 minutes 3 seconds". */
    fun spoken(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L) + 999) / 1_000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3_600
        return buildList {
            if (hours > 0) add("$hours ${plural(hours, "hour")}")
            if (minutes > 0) add("$minutes ${plural(minutes, "minute")}")
            if (seconds > 0 || isEmpty()) add("$seconds ${plural(seconds, "second")}")
        }.joinToString(" ")
    }

    private fun plural(value: Long, unit: String) = if (value == 1L) unit else "${unit}s"
}
