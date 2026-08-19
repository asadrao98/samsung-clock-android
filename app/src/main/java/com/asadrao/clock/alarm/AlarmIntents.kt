package com.asadrao.clock.alarm

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.asadrao.clock.domain.model.Alarm

/**
 * Intents and PendingIntent identity for alarms.
 *
 * The important subtlety: **`PendingIntent` equality ignores extras.** Two alarms whose intents
 * differ only by an id extra are the same PendingIntent as far as the system is concerned, so
 * scheduling the second would silently replace the first. Every intent here therefore carries
 * the alarm id in its *data URI*, which does count towards equality, and a distinct request
 * code as well.
 */
object AlarmIntents {

    const val ACTION_FIRE = "com.asadrao.clock.action.ALARM_FIRE"
    const val ACTION_SNOOZE_FIRE = "com.asadrao.clock.action.SNOOZE_FIRE"

    const val EXTRA_ALARM_ID = "com.asadrao.clock.extra.ALARM_ID"

    private const val SCHEME = "samsungclock"

    /** Request codes are namespaced so an alarm and its snooze never collide. */
    private const val FIRE_BASE = 0
    private const val SNOOZE_BASE = 1

    fun fireIntent(context: Context, alarmId: Long): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = "$SCHEME://alarm/$alarmId".toUri()
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

    fun snoozeIntent(context: Context, alarmId: Long): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            data = "$SCHEME://snooze/$alarmId".toUri()
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

    fun fireRequestCode(alarmId: Long): Int = requestCode(FIRE_BASE, alarmId)

    fun snoozeRequestCode(alarmId: Long): Int = requestCode(SNOOZE_BASE, alarmId)

    /**
     * Room ids are `Long` and request codes are `Int`. Truncating would let two alarms share a
     * code after 2^31 inserts — unreachable in practice, but the data URI above is what
     * actually guarantees distinctness, so this only needs to be stable, not injective.
     */
    private fun requestCode(base: Int, alarmId: Long): Int =
        (base * 31 + alarmId.toInt()) * 2 + base

    fun alarmIdFrom(intent: Intent): Long =
        intent.getLongExtra(EXTRA_ALARM_ID, Alarm.NO_ID)
}
