package com.asadrao.clock.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.asadrao.clock.ClockApplication
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.ui.format.rememberIs24HourFormat
import com.asadrao.clock.ui.format.rememberLocale
import com.asadrao.clock.ui.ringing.AlarmRingingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * The screen that appears when an alarm goes off.
 *
 * Reached by the ringing notification's full-screen intent, which is what lets it come up over the
 * lockscreen. `setShowWhenLocked` and `setTurnScreenOn` are what actually wake the display; the
 * app cannot and does not attempt to unlock the device.
 *
 * If the user has revoked the full-screen-intent permission the system shows a heads-up
 * notification instead of this screen. That path still works, because the notification carries its
 * own Snooze and Dismiss actions.
 *
 * This activity does **not** own the alarm. The sound, the vibration and the schedule all belong to
 * [AlarmRingService], so killing this screen — by rotating, by the system, by a crash — cannot
 * silence an alarm that should still be ringing.
 */
class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Long = Alarm.NO_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        alarmId = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, Alarm.NO_ID)

        showOverLockscreen()

        // Back must not dismiss the alarm: leaving the screen without choosing would hide it
        // while it carried on ringing, which is the worst of both. Registered as a callback
        // rather than overriding the deprecated onBackPressed.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val container = (application as ClockApplication).container

        setContent {
            // remember is essential here, not decoration: state created bare in a composable body
            // is rebuilt on every recomposition, so the clock below would be reset to "now" each
            // time it ticked and the loaded label would be discarded.
            var label by remember { mutableStateOf("") }
            var snoozeAvailable by remember { mutableStateOf(false) }
            var snoozeRemaining by remember { mutableStateOf<Int?>(null) }
            var now by remember { mutableStateOf(ZonedDateTime.now()) }

            LaunchedEffect(alarmId) {
                val alarm = container.alarmRepository.getAlarm(alarmId)
                label = alarm?.label.orEmpty()
                if (alarm != null) {
                    val used = container.snoozeTracker.count(alarmId)
                    val unlimited = alarm.snoozeRepeatLimit == AlarmRingService.UNLIMITED_SNOOZE
                    snoozeAvailable = alarm.snoozeEnabled && (unlimited || used < alarm.snoozeRepeatLimit)
                    snoozeRemaining = if (unlimited) null else (alarm.snoozeRepeatLimit - used)
                }
            }

            // The clock on screen has to stay right while the alarm rings unanswered. Ticking to
            // the next minute boundary rather than every second keeps it accurate without waking
            // up sixty times a minute for a readout that only shows minutes.
            LaunchedEffect(Unit) {
                while (true) {
                    val current = ZonedDateTime.now()
                    now = current
                    val millisToNextMinute =
                        60_000L - (current.second * 1_000L + current.nano / 1_000_000L)
                    delay(millisToNextMinute.coerceAtLeast(1_000L))
                }
            }

            AlarmRingingScreen(
                label = label,
                now = now,
                is24Hour = rememberIs24HourFormat(),
                locale = rememberLocale(),
                snoozeAvailable = snoozeAvailable,
                snoozeRemaining = snoozeRemaining,
                // Only a drag dismisses while locked, so a pocket cannot cancel the alarm.
                tapToDismiss = !isDeviceLocked(),
                onDismiss = ::dismiss,
                onSnooze = ::snooze,
            )
        }
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        // Keeps the display on for as long as the alarm is showing.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun isDeviceLocked(): Boolean =
        getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked == true

    /**
     * Volume and side keys snooze, which is the Pixel-implementable half of Samsung's long list of
     * dismiss gestures. Consuming the event stops it also changing the alarm volume mid-ring.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_POWER,
        -> {
            snooze()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun snooze() {
        startService(AlarmRingService.snoozeIntent(this, alarmId))
        finish()
    }

    private fun dismiss() {
        startService(AlarmRingService.dismissIntent(this, alarmId))
        finish()
    }

    companion object {
        fun intent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmRingingActivity::class.java).apply {
                putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
                // NEW_TASK only. CLEAR_TASK alongside singleInstance and an empty taskAffinity
                // is an odd combination that AOSP's own alarm clock does not use, and it is one of
                // the few remaining differences from a launch that is known to work.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
    }
}
