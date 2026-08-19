package com.asadrao.clock.domain.schedule

import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.repository.AlarmRepository
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Keeps what the *system* has scheduled in step with what the *database* says.
 *
 * All the policy lives here rather than in a BroadcastReceiver, so every rule below is
 * reachable from a unit test: what happens after an alarm rings, what a reboot has to redo,
 * and what a timezone change means for an already-scheduled alarm.
 */
class AlarmSchedulingCoordinator(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val clock: Clock,
) {

    /**
     * Re-arms everything from scratch.
     *
     * Needed after a reboot (the system forgets all alarms), after an app update (same), and
     * after the clock or timezone moves — in the last case the stored wall-clock time is still
     * correct but the instant it maps to has changed, so every alarm must be recomputed.
     */
    suspend fun syncAll() {
        val now = ZonedDateTime.now(clock)
        repository.getAllAlarms().forEach { alarm ->
            if (alarm.enabled) {
                scheduler.schedule(alarm, AlarmSchedule.nextTrigger(alarm, now).toEpochMillis())
            } else {
                // Cancel unconditionally: cheap, idempotent, and it cleans up anything left
                // over from a crash between a database write and its scheduling call.
                scheduler.cancel(alarm.id)
                scheduler.cancelSnooze(alarm.id)
            }
        }
    }

    /** Brings one alarm's schedule in line with its stored state. */
    suspend fun sync(alarmId: Long) {
        val alarm = repository.getAlarm(alarmId)
        if (alarm == null) {
            scheduler.cancel(alarmId)
            scheduler.cancelSnooze(alarmId)
            return
        }
        if (alarm.enabled) {
            scheduler.schedule(
                alarm,
                AlarmSchedule.nextTrigger(alarm, ZonedDateTime.now(clock)).toEpochMillis(),
            )
        } else {
            scheduler.cancel(alarm.id)
            scheduler.cancelSnooze(alarm.id)
        }
    }

    /**
     * Called the moment an alarm fires, before any ringing UI appears.
     *
     * Doing the bookkeeping now rather than when the user finally dismisses it means a
     * repeating alarm is already armed for tomorrow even if the ringing screen is killed, the
     * battery dies, or the user force-stops the app mid-ring.
     *
     * A one-shot alarm is switched off rather than deleted, which is what Samsung Clock does:
     * it stays in the list, ready to be re-enabled.
     */
    suspend fun onAlarmFired(alarmId: Long) {
        val alarm = repository.getAlarm(alarmId) ?: return
        if (alarm.repeats) {
            scheduler.schedule(
                alarm,
                AlarmSchedule.nextTrigger(alarm, ZonedDateTime.now(clock)).toEpochMillis(),
            )
        } else {
            repository.setEnabled(alarm.id, false)
            scheduler.cancel(alarm.id)
        }
    }

    /** Arms the snooze re-ring and returns when it will fire. */
    suspend fun snooze(alarmId: Long): ZonedDateTime? {
        val alarm = repository.getAlarm(alarmId) ?: return null
        if (!alarm.snoozeEnabled) return null
        val ringAt = AlarmSchedule.snoozeTrigger(alarm, ZonedDateTime.now(clock))
        scheduler.scheduleSnooze(alarm, ringAt.toEpochMillis())
        return ringAt
    }

    /**
     * Dismissing clears any pending snooze. The next scheduled occurrence is left alone —
     * [onAlarmFired] already set it up, and dismissing today's ring must not cancel tomorrow's.
     */
    fun dismiss(alarmId: Long) {
        scheduler.cancelSnooze(alarmId)
    }

    private fun ZonedDateTime.toEpochMillis(): Long = toInstant().toEpochMilli()
}
