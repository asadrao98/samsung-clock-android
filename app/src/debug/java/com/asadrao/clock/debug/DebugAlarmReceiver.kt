package com.asadrao.clock.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asadrao.clock.ClockApplication
import com.asadrao.clock.domain.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Schedules an alarm from adb, for testing the ringing path.
 *
 * ```
 * adb shell am broadcast -a com.asadrao.clock.DEBUG_SCHEDULE_ALARM \
 *     -n com.asadrao.clock/com.asadrao.clock.debug.DebugAlarmReceiver --ei minutes 2
 * ```
 *
 * Debug builds only — this file is in `src/debug`, so it is not compiled into a release APK.
 *
 * Needed because the thing most worth testing, an alarm going off over a locked screen, requires
 * the screen to be locked, and a locked screen cannot be driven over adb to set up the next test.
 * Everything it does goes through the ordinary repository and coordinator, so it exercises the real
 * code path rather than a shortcut around it.
 */
class DebugAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra("minutes", 2).coerceIn(1, 60)
        val app = context.applicationContext as? ClockApplication ?: return
        val container = app.container

        val pending: BroadcastReceiver.PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val target = ZonedDateTime.now().plusMinutes(minutes.toLong())
                val id = container.alarmRepository.addAlarm(
                    Alarm(
                        hour = target.hour,
                        minute = target.minute,
                        enabled = true,
                        label = "Debug test",
                    )
                )
                container.alarmSchedulingCoordinator.sync(id)
                Log.i(TAG, "scheduled debug alarm $id for ${target.hour}:${target.minute}")
            } catch (t: Throwable) {
                Log.e(TAG, "failed to schedule a debug alarm", t)
            } finally {
                pending?.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DebugAlarmReceiver"
    }
}
