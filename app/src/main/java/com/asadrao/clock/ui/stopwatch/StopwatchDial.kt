package com.asadrao.clock.ui.stopwatch

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The analog half of One UI 8.5's hybrid stopwatch: a dial with tick marks and a sweeping hand
 * behind the digital readout.
 *
 * Drawn on a `Canvas` rather than assembled from composables — sixty tick marks and a hand that
 * moves every frame would otherwise mean sixty-one layout nodes being re-measured continuously.
 *
 * The hand sweeps **continuously**, not in one-second steps. A stepping hand is the detail that
 * makes a stopwatch look cheap, and since the elapsed value already has millisecond resolution
 * there is no reason to quantise it.
 */
@Composable
fun StopwatchDial(
    elapsedMillis: Long,
    accentColor: Color,
    tickColor: Color,
    majorTickColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)

        val majorTickLength = radius * 0.10f
        val minorTickLength = radius * 0.05f
        val tickInset = radius * 0.04f

        // Sixty ticks, one per second, with every fifth one longer and stronger.
        for (index in 0 until 60) {
            val major = index % 5 == 0
            val angle = (index / 60f) * TWO_PI - HALF_PI
            val outerR = radius - tickInset
            val innerR = outerR - if (major) majorTickLength else minorTickLength
            drawLine(
                color = if (major) majorTickColor else tickColor,
                start = Offset(
                    centre.x + cos(angle) * innerR,
                    centre.y + sin(angle) * innerR,
                ),
                end = Offset(
                    centre.x + cos(angle) * outerR,
                    centre.y + sin(angle) * outerR,
                ),
                strokeWidth = if (major) 3f else 1.5f,
                cap = StrokeCap.Round,
            )
        }

        // The sweep hand completes a revolution per minute, moving smoothly.
        val secondsFraction = (elapsedMillis % 60_000L) / 60_000f
        val handAngle = secondsFraction * TWO_PI - HALF_PI
        val handLength = radius * 0.80f
        // A short tail past the centre, as a real sweep hand has.
        val tailLength = radius * 0.12f
        drawLine(
            color = accentColor,
            start = Offset(
                centre.x - cos(handAngle) * tailLength,
                centre.y - sin(handAngle) * tailLength,
            ),
            end = Offset(
                centre.x + cos(handAngle) * handLength,
                centre.y + sin(handAngle) * handLength,
            ),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )

        // A progress arc for the current minute, so long runs still read at a glance.
        drawArc(
            color = accentColor.copy(alpha = 0.25f),
            startAngle = -90f,
            sweepAngle = secondsFraction * 360f,
            useCenter = false,
            topLeft = Offset(centre.x - radius + tickInset, centre.y - radius + tickInset),
            size = androidx.compose.ui.geometry.Size(
                (radius - tickInset) * 2f,
                (radius - tickInset) * 2f,
            ),
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )

        drawCircle(color = accentColor, radius = radius * 0.03f, center = centre)
    }
}

private const val TWO_PI = (2 * Math.PI).toFloat()
private const val HALF_PI = (Math.PI / 2).toFloat()
