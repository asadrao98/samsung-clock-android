package com.asadrao.clock.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.asadrao.clock.domain.model.Alarm

/**
 * Plays an alarm's sound and drives the vibrator.
 *
 * Two things here are what make it behave like an alarm rather than like a notification:
 *
 * - **`USAGE_ALARM`** on the audio attributes, so the alarm stream's volume applies and Do Not
 *   Disturb lets it through. A media-usage player would be silenced by DND and would fight
 *   whatever the user was listening to.
 * - **A sound fallback chain.** The chosen URI may have become unplayable since it was picked —
 *   the file was deleted, the SD card was removed, the granted permission lapsed. Rather than fail
 *   silently, it falls back to the system alarm sound and then to the system default. An alarm
 *   that does not make a noise is the worst possible failure for this app.
 */
class AlarmAudio(private val context: Context) {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun start(alarm: Alarm) {
        if (!alarm.isSilent) startSound(alarm.soundUri)
        if (alarm.vibrationEnabled) startVibration()
    }

    private fun startSound(soundUri: String?) {
        // Ordered best-effort: the user's choice, then the system alarm sound, then whatever the
        // platform will give us.
        val candidates = buildList {
            soundUri?.takeIf { it != Alarm.SILENT_SOUND }?.let { add(it.toUri()) }
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { add(it) }
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { add(it) }
        }

        for (uri in candidates) {
            if (tryPlay(uri)) return
        }
        Log.e(TAG, "no playable alarm sound found; the alarm will vibrate only")
    }

    private fun tryPlay(uri: Uri): Boolean = try {
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            // If the user has the alarm stream muted there is nothing we can legitimately do
            // about it — raising it ourselves would override a deliberate choice.
            prepare()
            start()
        }
        true
    } catch (t: Throwable) {
        Log.w(TAG, "cannot play $uri, trying the next fallback", t)
        player?.release()
        player = null
        false
    }

    private fun startVibration() {
        val vib = resolveVibrator() ?: return
        vibrator = vib
        // A long-short-short pulse, repeating from index 0.
        val timings = longArrayOf(0, 900, 400, 300, 400)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0)
        val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Alarm usage so it survives DND, matching the audio.
            val attributes = android.os.VibrationAttributes.Builder()
                .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(effect, attributes)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }?.takeIf { it.hasVibrator() }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private companion object {
        const val TAG = "AlarmAudio"
    }
}
