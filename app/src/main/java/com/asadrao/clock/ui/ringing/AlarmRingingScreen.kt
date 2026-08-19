package com.asadrao.clock.ui.ringing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.ui.theme.ClockFontFamily
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.hypot

/**
 * The full-screen alarm.
 *
 * Always dark with white text regardless of the app's theme — it is a display surface, not a page
 * of the app, and it has to be legible at 6am on a bedside table.
 *
 * The content sits in the upper 40% and the controls in the lower 60%, so everything you have to
 * touch is inside thumb reach. The dismiss target is a large circle with a slowly breathing halo
 * to signal that it is draggable; snooze is a deliberately smaller, lower pill, so the two are hard
 * to confuse when half-awake.
 *
 * [tapToDismiss] is false while the device is locked: only a drag dismisses, which is what stops
 * an alarm being cancelled by a pocket. Unlocked, a tap works too.
 */
@Composable
fun AlarmRingingScreen(
    label: String,
    now: ZonedDateTime,
    is24Hour: Boolean,
    locale: Locale,
    snoozeAvailable: Boolean,
    snoozeRemaining: Int?,
    tapToDismiss: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RingingGradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 96.dp, start = 24.dp, end = 24.dp),
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
                text = timeText(now, is24Hour),
                fontFamily = ClockFontFamily,
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                // Tabular figures: this readout updates every minute while it rings, and the
                // digits must not shuffle sideways as they change.
                color = Color.White,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            )
            meridiemText(now, is24Hour, locale)?.let {
                Text(
                    text = it,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = DateTimeFormatter.ofPattern("EEE, d MMMM", locale).format(now),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            DismissTarget(
                tapToDismiss = tapToDismiss,
                onDismiss = onDismiss,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (tapToDismiss) "Tap to dismiss" else "Swipe to dismiss",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(40.dp))

            // No greyed-out button when snooze is unavailable — the pill is simply absent, and
            // the dismiss target keeps its position.
            if (snoozeAvailable) {
                SnoozePill(remaining = snoozeRemaining, onSnooze = onSnooze)
            } else {
                Text(
                    text = "Last alarm — snooze unavailable",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun DismissTarget(
    tapToDismiss: Boolean,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    // A slow breathing halo, to say "this can be dragged" without any text.
    val transition = rememberInfiniteTransition(label = "dismissHalo")
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "halo",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    val scale = 1f + halo * 0.25f
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.25f * (1f - halo)
                }
                .clip(CircleShape)
                .background(Color.White),
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    translationX = dragX
                    translationY = dragY
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .then(
                    if (tapToDismiss) Modifier.clickable(onClick = onDismiss) else Modifier
                )
                .pointerInput(tapToDismiss) {
                    detectDragGestures(
                        onDragEnd = {
                            if (hypot(dragX, dragY) >= thresholdPx) {
                                onDismiss()
                            }
                            // Under the threshold it springs back rather than committing.
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragCancel = {
                            dragX = 0f
                            dragY = 0f
                        },
                    ) { _, delta ->
                        dragX += delta.x
                        dragY += delta.y
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", fontSize = 40.sp, color = Color.White)
        }
    }
}

@Composable
private fun SnoozePill(remaining: Int?, onSnooze: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 180.dp, height = 56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            // Snooze responds to a plain tap even on the lockscreen: it is the safe action, so
            // an accidental one costs the user nothing.
            .clickable(onClick = onSnooze),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Snooze",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            if (remaining != null) {
                Text(
                    text = if (remaining == 1) "1 left" else "$remaining left",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

private fun timeText(now: ZonedDateTime, is24Hour: Boolean): String =
    com.asadrao.clock.ui.format.AlarmFormat.time(now.hour, now.minute, is24Hour)

private fun meridiemText(now: ZonedDateTime, is24Hour: Boolean, locale: Locale): String? =
    com.asadrao.clock.ui.format.AlarmFormat.meridiem(now.hour, locale, is24Hour)

/**
 * The default ringing background: our own gradient, drawn in code.
 *
 * One UI 8.5 uses an animated live-weather scene here. That depends on Samsung's assets and a
 * weather feed, so this is the legitimate stand-in — and it keeps the app offline.
 */
private val RingingGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF1B1740),
        Color(0xFF43276B),
        Color(0xFF8E4A6B),
    )
)
