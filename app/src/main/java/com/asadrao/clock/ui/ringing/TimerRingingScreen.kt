package com.asadrao.clock.ui.ringing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.ui.format.DurationFormat
import com.asadrao.clock.ui.theme.ClockFontFamily

/**
 * The full-screen timer alert.
 *
 * Built to the same shape as the alarm's so the two feel like one app, but with the differences a
 * finished timer actually needs: a plain **Stop** rather than Dismiss, "+1 min" in place of Snooze,
 * and no snooze budget to report. A timer has no schedule to defer to.
 *
 * Stop responds to a plain tap even on the lockscreen — unlike the alarm's dismiss, which requires
 * a drag. Cancelling a finished timer by accident costs nothing, whereas silently killing an alarm
 * from a pocket can make someone miss their morning.
 */
@Composable
fun TimerRingingScreen(
    label: String,
    totalMillis: Long,
    onStop: () -> Unit,
    onAddMinute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TimerRingingGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 112.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = "Timer finished",
                fontFamily = ClockFontFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = DurationFormat.timer(totalMillis),
                fontFamily = ClockFontFamily,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The big close target, matching the alarm's dismiss circle in size and placement so
            // the two screens are muscle-memory compatible.
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✕", fontSize = 44.sp, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tap to stop",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(width = 180.dp, height = 56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onAddMinute),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+1 min",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

/** Warmer than the alarm's, so the two alerts are distinguishable half-awake. */
private val TimerRingingGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF10233B),
        Color(0xFF1E4B63),
        Color(0xFF2F7A6B),
    )
)
