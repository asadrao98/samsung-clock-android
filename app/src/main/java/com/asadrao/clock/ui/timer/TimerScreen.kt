package com.asadrao.clock.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.domain.timer.ClockTimer
import com.asadrao.clock.domain.timer.QuickDurations
import com.asadrao.clock.domain.timer.TimerPreset
import com.asadrao.clock.ui.components.OneUiCircleButton
import com.asadrao.clock.ui.components.OneUiFilledButton
import com.asadrao.clock.ui.components.OneUiTextButton
import com.asadrao.clock.ui.components.OneUiWheel
import com.asadrao.clock.ui.format.DurationFormat
import com.asadrao.clock.ui.theme.ClockTheme

/**
 * The Timer tab.
 *
 * Two modes. With nothing running it shows the hours/minutes/seconds drums and any saved presets.
 * Once timers exist it becomes a stacked, scrollable list of them, which is how One UI 8.5 presents
 * multiple concurrent timers.
 *
 * Each running timer recomputes its remaining time per frame from the monotonic clock, exactly like
 * the stopwatch, so the ring animates smoothly instead of stepping once a second — and nothing
 * needs to tick while the screen is off.
 */
@Composable
fun TimerScreen(
    state: TimerUiState,
    nowRealtime: () -> Long,
    onStartNew: (Int, Int, Int) -> Unit,
    onStartQuick: (Long) -> Unit,
    onSavePreset: (Int, Int, Int, String) -> Unit,
    onStartPreset: (TimerPreset) -> Unit,
    onDeletePreset: (TimerPreset) -> Unit,
    onStartOrPause: (ClockTimer) -> Unit,
    onReset: (ClockTimer) -> Unit,
    onAddMinute: (ClockTimer) -> Unit,
    onDelete: (ClockTimer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navInset = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = ClockTheme.dimens.contentBottomPadding(navInset.calculateBottomPadding())

    if (state.showEntry) {
        TimerEntry(
            presets = state.presets,
            quickDurations = state.quickDurations,
            onStartQuick = onStartQuick,
            bottomPadding = bottomPadding,
            onStartNew = onStartNew,
            onSavePreset = onSavePreset,
            onStartPreset = onStartPreset,
            onDeletePreset = onDeletePreset,
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ClockTheme.dimens.navPillMargin,
                end = ClockTheme.dimens.navPillMargin,
                top = 8.dp,
                bottom = bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(ClockTheme.dimens.cardSpacing),
        ) {
            items(state.timers, key = { it.id }) { timer ->
                RunningTimerCard(
                    timer = timer,
                    nowRealtime = nowRealtime,
                    onStartOrPause = { onStartOrPause(timer) },
                    onReset = { onReset(timer) },
                    onAddMinute = { onAddMinute(timer) },
                    onDelete = { onDelete(timer) },
                )
            }
        }
    }
}

@Composable
private fun TimerEntry(
    presets: List<TimerPreset>,
    quickDurations: List<Long>,
    onStartQuick: (Long) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onStartNew: (Int, Int, Int) -> Unit,
    onSavePreset: (Int, Int, Int, String) -> Unit,
    onStartPreset: (TimerPreset) -> Unit,
    onDeletePreset: (TimerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(1) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelWithUnit("Hours", "h") {
                OneUiWheel(
                    itemCount = 24,
                    selectedIndex = hours,
                    onSelectedIndexChange = { hours = it },
                    label = { it.toString() },
                    contentDescription = "Hours",
                    width = 72.dp,
                    itemHeight = 56.dp,
                    textStyle = ClockTheme.typography.numericDisplaySmall,
                    color = colors.textPrimary,
                )
            }
            WheelWithUnit("Minutes", "m") {
                OneUiWheel(
                    itemCount = 60,
                    selectedIndex = minutes,
                    onSelectedIndexChange = { minutes = it },
                    label = { "%02d".format(it) },
                    contentDescription = "Minutes",
                    width = 72.dp,
                    itemHeight = 56.dp,
                    textStyle = ClockTheme.typography.numericDisplaySmall,
                    color = colors.textPrimary,
                )
            }
            WheelWithUnit("Seconds", "s") {
                OneUiWheel(
                    itemCount = 60,
                    selectedIndex = seconds,
                    onSelectedIndexChange = { seconds = it },
                    label = { "%02d".format(it) },
                    contentDescription = "Seconds",
                    width = 72.dp,
                    itemHeight = 56.dp,
                    textStyle = ClockTheme.typography.numericDisplaySmall,
                    color = colors.textPrimary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        QuickDurationRow(durations = quickDurations, onPick = onStartQuick)

        Spacer(Modifier.height(20.dp))

        OneUiCircleButton(
            label = "Start",
            onClick = { onStartNew(hours, minutes, seconds) },
            containerColor = colors.accent,
            contentColor = colors.textSecondary,
            icon = { Text("▶", fontSize = 28.sp, color = colors.onAccent) },
            // A zero duration is not a timer, so the control is genuinely unavailable rather
            // than starting something that finishes instantly.
            enabled = hours + minutes + seconds > 0,
        )

        Spacer(Modifier.height(16.dp))
        OneUiTextButton(
            text = "Save as preset",
            onClick = { onSavePreset(hours, minutes, seconds, "") },
            enabled = hours + minutes + seconds > 0,
        )

        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = ClockTheme.dimens.navPillMargin),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(presets, key = { it.id }) { preset ->
                    PresetRow(
                        preset = preset,
                        onStart = { onStartPreset(preset) },
                        onDelete = { onDeletePreset(preset) },
                    )
                }
            }
        }
    }
}

/**
 * One-tap durations.
 *
 * Ordered most-recently-used first, so the timer set yesterday is the first one offered today —
 * which is how Samsung's behave. They start a timer immediately rather than just loading the drums:
 * a quick duration exists precisely to avoid the dialling step.
 */
@Composable
private fun QuickDurationRow(
    durations: List<Long>,
    onPick: (Long) -> Unit,
) {
    if (durations.isEmpty()) return
    val colors = ClockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ClockTheme.dimens.navPillMargin),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        durations.forEach { millis ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ClockTheme.shapes.pill)
                    .background(colors.buttonSurface)
                    .clickable { onPick(millis) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = QuickDurations.label(millis),
                    style = ClockTheme.typography.buttonLabel,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun WheelWithUnit(
    label: String,
    unit: String,
    wheel: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        wheel()
        Text(
            text = unit,
            style = ClockTheme.typography.alarmMeta,
            color = ClockTheme.colors.textSecondary,
            modifier = Modifier.padding(start = 2.dp, end = 8.dp),
        )
    }
}

@Composable
private fun PresetRow(
    preset: TimerPreset,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = ClockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClockTheme.shapes.card)
            .background(colors.cardBackground)
            .clickable(onClick = onStart)
            .padding(horizontal = ClockTheme.dimens.cardPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DurationFormat.timer(preset.totalMillis),
            style = ClockTheme.typography.listTitle,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (preset.label.isNotBlank()) {
            Text(
                text = preset.label,
                style = ClockTheme.typography.alarmMeta,
                color = colors.textSecondary,
            )
        }
        OneUiTextButton(text = "Delete", onClick = onDelete, color = colors.dangerText)
    }
}

@Composable
private fun RunningTimerCard(
    timer: ClockTimer,
    nowRealtime: () -> Long,
    onStartOrPause: () -> Unit,
    onReset: () -> Unit,
    onAddMinute: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = ClockTheme.colors

    // Recomputed per frame while running, so the ring sweeps smoothly instead of stepping.
    val remaining by produceState(initialValue = timer.remainingAt(nowRealtime()), timer) {
        value = timer.remainingAt(nowRealtime())
        while (timer.isRunning) {
            withFrameNanos { }
            value = timer.remainingAt(nowRealtime())
        }
    }
    val progress = if (timer.totalMillis <= 0L) 1f
    else ((timer.totalMillis - remaining).toFloat() / timer.totalMillis).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClockTheme.shapes.card)
            .background(colors.cardBackground)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (timer.label.isNotBlank()) {
            Text(
                text = timer.label,
                style = ClockTheme.typography.alarmMeta,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            CountdownRing(
                progress = progress,
                trackColor = colors.divider,
                progressColor = if (remaining == 0L) colors.functionalOrange else colors.accent,
                modifier = Modifier.fillMaxSize(),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DurationFormat.timer(remaining),
                    style = ClockTheme.typography.numericDisplay,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (remaining == 0L) "Finished"
                    else "of " + DurationFormat.timer(timer.totalMillis),
                    style = ClockTheme.typography.alarmMeta,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OneUiCircleButton(
                label = "Cancel",
                onClick = onDelete,
                containerColor = colors.buttonSurface,
                contentColor = colors.textSecondary,
                icon = { Text("✕", fontSize = 24.sp, color = colors.textPrimary) },
            )
            OneUiCircleButton(
                label = if (timer.isRunning) "Pause" else "Resume",
                onClick = onStartOrPause,
                containerColor = if (timer.isRunning) colors.buttonSurface else colors.accent,
                contentColor = colors.textSecondary,
                icon = {
                    Text(
                        text = if (timer.isRunning) "❙❙" else "▶",
                        fontSize = if (timer.isRunning) 22.sp else 26.sp,
                        color = if (timer.isRunning) colors.textPrimary else colors.onAccent,
                    )
                },
            )
            OneUiCircleButton(
                label = "+1 min",
                onClick = onAddMinute,
                containerColor = colors.buttonSurface,
                contentColor = colors.textSecondary,
                icon = { Text("+", fontSize = 28.sp, color = colors.textPrimary) },
            )
        }
    }
}

/**
 * The countdown ring.
 *
 * Drawn on a Canvas and fed a continuous progress value, so it advances every frame rather than
 * jumping once a second — a stepping ring is the detail that makes a countdown look mechanical.
 */
@Composable
private fun CountdownRing(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 10f
        val inset = stroke / 2f
        val diameter = minOf(size.width, size.height) - stroke
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            // Counts down: the remaining arc shrinks clockwise from the top.
            sweepAngle = 360f * (1f - progress),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
