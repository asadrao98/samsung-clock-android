package com.asadrao.clock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.asadrao.clock.MainActivity
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.schedule.AlarmScheduler

/**
 * Schedules alarms with [AlarmManager].
 *
 * Uses `setAlarmClock`, not `setExactAndAllowWhileIdle`, which matters on modern Android:
 * `setAlarmClock` is treated as a user-visible alarm clock, so it is exempt from Doze
 * batching, survives App Standby buckets, and puts the alarm icon in the status bar with a
 * working "next alarm" entry in Settings. It is the only API that behaves the way a person
 * expects an alarm clock to behave.
 *
 * If exact alarms are unavailable — only possible on Android 12/12L, where the permission is
 * user-revocable — it falls back to `setAndAllowWhileIdle`, which still wakes the device but
 * may fire minutes late. The fallback is logged rather than hidden, and the UI is expected to
 * tell the user their alarms are degraded rather than let them find out by oversleeping.
 */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    override fun schedule(alarm: Alarm, triggerAtMillis: Long) {
        set(
            triggerAtMillis = triggerAtMillis,
            operation = pendingIntent(
                AlarmIntents.fireIntent(context, alarm.id),
                AlarmIntents.fireRequestCode(alarm.id),
            ),
            what = "alarm ${alarm.id}",
        )
    }

    override fun scheduleSnooze(alarm: Alarm, triggerAtMillis: Long) {
        set(
            triggerAtMillis = triggerAtMillis,
            operation = pendingIntent(
                AlarmIntents.snoozeIntent(context, alarm.id),
                AlarmIntents.snoozeRequestCode(alarm.id),
            ),
            what = "snooze for alarm ${alarm.id}",
        )
    }

    override fun cancel(alarmId: Long) {
        alarmManager.cancel(
            pendingIntent(
                AlarmIntents.fireIntent(context, alarmId),
                AlarmIntents.fireRequestCode(alarmId),
            )
        )
    }

    override fun cancelSnooze(alarmId: Long) {
        alarmManager.cancel(
            pendingIntent(
                AlarmIntents.snoozeIntent(context, alarmId),
                AlarmIntents.snoozeRequestCode(alarmId),
            )
        )
    }

    private fun set(triggerAtMillis: Long, operation: PendingIntent, what: String) {
        if (canScheduleExact()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent()),
                operation,
            )
        } else {
            Log.w(
                TAG,
                "Exact alarms are not permitted; $what falls back to an inexact wake-up and " +
                    "may fire late.",
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
        }
    }

    /** What the system opens if the user taps the alarm icon or the "next alarm" entry. */
    private fun showIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            SHOW_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun pendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val TAG = "AlarmScheduler"
        const val SHOW_REQUEST_CODE = 1_000_000
    }
}
