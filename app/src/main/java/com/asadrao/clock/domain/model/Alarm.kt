package com.asadrao.clock.domain.model

/**
 * A single alarm, as the rest of the app understands it. Free of Room and of Android types so
 * the scheduling arithmetic around it stays unit-testable.
 *
 * [soundUri] distinguishes three states on purpose:
 *  - `null` — no sound chosen yet, so use the system default alarm sound. Follows the device
 *    if the user later changes that default.
 *  - [SILENT_SOUND] — the user explicitly chose silence. Vibration may still apply.
 *  - anything else — a specific content URI. It may have become unreadable since it was
 *    picked (the file was deleted, or the permission lapsed), which the ringing code has to
 *    survive rather than fail silently.
 */
data class Alarm(
    val id: Long = NO_ID,
    /** 0..23, always stored in 24-hour form. 12-hour display is a presentation concern. */
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val repeatDays: RepeatDays = RepeatDays.None,
    val label: String = "",
    val soundUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    /** How many times snooze may be taken before the alarm gives up. */
    val snoozeRepeatLimit: Int = DEFAULT_SNOOZE_LIMIT,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    init {
        require(hour in 0..23) { "hour must be 0..23, got $hour" }
        require(minute in 0..59) { "minute must be 0..59, got $minute" }
        require(snoozeDurationMinutes > 0) {
            "snooze duration must be positive, got $snoozeDurationMinutes"
        }
        require(snoozeRepeatLimit >= 0) {
            "snooze limit cannot be negative, got $snoozeRepeatLimit"
        }
    }

    val repeats: Boolean get() = repeatDays.isNotEmpty
    val isSilent: Boolean get() = soundUri == SILENT_SOUND
    val usesDefaultSound: Boolean get() = soundUri == null

    companion object {
        const val NO_ID = 0L
        const val SILENT_SOUND = "silent"
        const val DEFAULT_SNOOZE_MINUTES = 5
        const val DEFAULT_SNOOZE_LIMIT = 3
    }
}
