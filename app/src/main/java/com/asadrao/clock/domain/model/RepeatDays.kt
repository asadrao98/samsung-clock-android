package com.asadrao.clock.domain.model

import java.time.DayOfWeek

/**
 * The set of weekdays an alarm repeats on, packed into the low seven bits of an [Int].
 *
 * Bit 0 is Monday through bit 6 Sunday, matching `DayOfWeek.value - 1`, so the mapping never
 * depends on the device locale's idea of which day starts the week. What the *UI* shows first
 * is a separate, locale-driven question and is decided at the presentation layer.
 *
 * An empty set means "does not repeat": the alarm fires once at its next occurrence and then
 * switches itself off.
 */
@JvmInline
value class RepeatDays(val bits: Int) {

    init {
        require(bits in 0..ALL_BITS) { "RepeatDays holds seven bits, got $bits" }
    }

    val isEmpty: Boolean get() = bits == 0
    val isNotEmpty: Boolean get() = bits != 0
    val isEveryDay: Boolean get() = bits == ALL_BITS
    val count: Int get() = Integer.bitCount(bits)

    operator fun contains(day: DayOfWeek): Boolean = bits and day.bit() != 0

    fun with(day: DayOfWeek): RepeatDays = RepeatDays(bits or day.bit())
    fun without(day: DayOfWeek): RepeatDays = RepeatDays(bits and day.bit().inv() and ALL_BITS)
    fun toggle(day: DayOfWeek): RepeatDays =
        if (day in this) without(day) else with(day)

    /** In Monday-first order, regardless of locale. */
    fun toSet(): Set<DayOfWeek> = DayOfWeek.entries.filter { it in this }.toSet()

    val isWeekdaysOnly: Boolean get() = bits == WEEKDAYS.bits
    val isWeekendsOnly: Boolean get() = bits == WEEKENDS.bits

    companion object {
        private const val ALL_BITS = 0b111_1111

        val None = RepeatDays(0)
        val EveryDay = RepeatDays(ALL_BITS)
        val WEEKDAYS = RepeatDays(
            DayOfWeek.MONDAY.bit() or DayOfWeek.TUESDAY.bit() or DayOfWeek.WEDNESDAY.bit() or
                DayOfWeek.THURSDAY.bit() or DayOfWeek.FRIDAY.bit()
        )
        val WEEKENDS = RepeatDays(DayOfWeek.SATURDAY.bit() or DayOfWeek.SUNDAY.bit())

        fun of(vararg days: DayOfWeek): RepeatDays =
            RepeatDays(days.fold(0) { acc, d -> acc or d.bit() })

        fun of(days: Iterable<DayOfWeek>): RepeatDays =
            RepeatDays(days.fold(0) { acc, d -> acc or d.bit() })
    }
}

private fun DayOfWeek.bit(): Int = 1 shl (value - 1)
