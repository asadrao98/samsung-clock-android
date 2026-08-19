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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.asadrao.clock.ClockApplication
import com.asadrao.clock.ui.ringing.TimerRingingScreen

/**
 * The full-screen alert for a finished timer.
 *
 * Mirrors [AlarmRingingActivity]: shown over the lockscreen, turns the display on, and refuses Back
 * so the alert cannot be dismissed without a decision. The sound and the notification belong to
 * [TimerRingService], so this screen being killed cannot silence a timer that is still going off.
 */
class TimerRingingActivity : ComponentActivity() {

    private var timerId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        timerId = intent.getLongExtra(TimerScheduling.EXTRA_TIMER_ID, -1L)

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val container = (application as ClockApplication).container

        setContent {
            var label by remember { mutableStateOf("") }
            var total by remember { mutableLongStateOf(0L) }

            LaunchedEffect(timerId) {
                container.timerRepository.getTimer(timerId)?.let {
                    label = it.label
                    total = it.totalMillis
                }
            }

            TimerRingingScreen(
                label = label,
                totalMillis = total,
                onStop = ::stop,
                onAddMinute = ::addMinute,
            )
        }
    }

    /** Volume and power keys stop a finished timer, matching the alarm's key handling. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_POWER,
        -> {
            stop()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun stop() {
        startService(
            Intent(this, TimerRingService::class.java).apply {
                action = TimerRingService.ACTION_STOP
                putExtra(TimerScheduling.EXTRA_TIMER_ID, timerId)
            }
        )
        finish()
    }

    private fun addMinute() {
        startService(
            Intent(this, TimerRingService::class.java).apply {
                action = TimerRingService.ACTION_ADD_MINUTE
                putExtra(TimerScheduling.EXTRA_TIMER_ID, timerId)
            }
        )
        finish()
    }

    companion object {
        fun intent(context: Context, timerId: Long): Intent =
            Intent(context, TimerRingingActivity::class.java).apply {
                putExtra(TimerScheduling.EXTRA_TIMER_ID, timerId)
                // NEW_TASK only. CLEAR_TASK alongside singleInstance and an empty taskAffinity
                // is an odd combination that AOSP's own alarm clock does not use, and it is one of
                // the few remaining differences from a launch that is known to work.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
    }
}
