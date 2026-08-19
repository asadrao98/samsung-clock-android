package com.asadrao.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asadrao.clock.ClockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-arms every alarm after an event that wipes or invalidates the system's alarm list.
 *
 *  - **Boot** — `AlarmManager` keeps nothing across a restart, so without this every alarm is
 *    silently lost on the first reboot.
 *  - **App replaced** — an update cancels the app's pending alarms in the same way.
 *  - **Time or timezone set** — the stored 07:00 is still 07:00, but the *instant* it refers to
 *    has moved, so every pending trigger is now wrong and has to be recomputed. This is the
 *    case that matters when travelling.
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) {
            Log.w(TAG, "ignoring unexpected action $action")
            return
        }

        val app = context.applicationContext as? ClockApplication ?: return
        val coordinator = app.container.alarmSchedulingCoordinator

        // goAsync() yields null when onReceive is invoked outside a real broadcast dispatch.
        // Calling finish() on that would throw on a background thread — in the alarm path, where a
        // crash is most costly — so the handle is treated as optional throughout.
        val pending: android.content.BroadcastReceiver.PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                coordinator.syncAll()
            } catch (t: Throwable) {
                Log.e(TAG, "failed to re-arm alarms after $action", t)
            } finally {
                pending?.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SystemEventReceiver"
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}
