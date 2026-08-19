package com.asadrao.clock.domain.schedule

import com.asadrao.clock.domain.model.Alarm

/**
 * Hands an alarm to the platform so it fires while the app is closed.
 *
 * Abstracted for two reasons: the scheduling *policy* (which alarm, when, what happens after it
 * rings) becomes testable without a device, and the Android specifics stay in one file.
 */
interface AlarmScheduler {

    /**
     * Whether the system will honour an exact alarm right now.
     *
     * False only on Android 12 and 12L, where `SCHEDULE_EXACT_ALARM` is a user-revocable
     * permission. From Android 13 this app qualifies for `USE_EXACT_ALARM`, which is granted on
     * install because ringing at an exact time *is* the app's purpose.
     */
    fun canScheduleExact(): Boolean

    /** Schedules [alarm] for [triggerAtMillis], replacing any pending trigger for it. */
    fun schedule(alarm: Alarm, triggerAtMillis: Long)

    /** Schedules the snooze re-ring for [alarm]. Kept separate so it can be cancelled alone. */
    fun scheduleSnooze(alarm: Alarm, triggerAtMillis: Long)

    fun cancel(alarmId: Long)

    fun cancelSnooze(alarmId: Long)
}
