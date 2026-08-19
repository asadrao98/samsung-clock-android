package com.asadrao.clock.ui.stopwatch

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asadrao.clock.domain.stopwatch.LapEntry
import com.asadrao.clock.domain.stopwatch.StopwatchState
import com.asadrao.clock.ui.components.OneUiCircleButton
import com.asadrao.clock.ui.components.OneUiRowDivider
import com.asadrao.clock.ui.format.DurationFormat
import com.asadrao.clock.ui.theme.ClockTheme

/**
 * The stopwatch.
 *
 * The elapsed value is recomputed from the monotonic clock **once per frame, and only while
 * running**. That keeps the hundredths smooth without a timer of our own, and costs nothing at all
 * when paused, because `withFrameNanos` simply stops being called.
 */
@Composable
fun StopwatchScreen(
    state: StopwatchState,
    nowRealtime: () -> Long,
    onStartOrPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val navInset = WindowInsets.navigationBars.asPaddingValues()

    // Re-read every frame while running; frozen at the banked value when not.
    val elapsed by produceState(initialValue = state.elapsedAt(nowRealtime()), state) {
        value = state.elapsedAt(nowRealtime())
        while (state.isRunning) {
            withFrameNanos { }
            value = state.elapsedAt(nowRealtime())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // With no laps recorded there is no scrollable here to carry contentPadding, so the
            // controls would sit under the floating pill. The lap list adds its own padding on top
            // of this once it appears.
            .padding(bottom = ClockTheme.dimens.contentBottomPadding(navInset.calculateBottomPadding())),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 56.dp)
                .fillMaxWidth()
                // Capped so the dial cannot crowd the controls off the bottom on a short screen.
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            StopwatchDial(
                elapsedMillis = elapsed,
                accentColor = colors.accent,
                tickColor = colors.textTertiary.copy(alpha = 0.35f),
                majorTickColor = colors.textSecondary,
                modifier = Modifier.fillMaxSize(),
            )
            Readout(elapsed = elapsed)
        }

        Spacer(Modifier.height(24.dp))
        Controls(
            state = state,
            onStartOrPause = onStartOrPause,
            onLap = onLap,
            onReset = onReset,
        )
        Spacer(Modifier.height(24.dp))

        LapList(
            laps = state.lapSplits(),
            modifier = Modifier.weight(1f),
            bottomPadding = ClockTheme.dimens.contentBottomPadding(
                navInset.calculateBottomPadding()
            ),
        )
    }
}

@Composable
private fun Readout(elapsed: Long) {
    val colors = ClockTheme.colors
    // The whole part and the hundredths are drawn separately so the hundredths can be smaller —
    // and, more importantly, so the digits that change fastest are not the largest thing moving.
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = DurationFormat.stopwatchWhole(elapsed),
            style = ClockTheme.typography.numericDisplay,
            color = colors.textPrimary,
        )
        Text(
            text = "." + DurationFormat.hundredths(elapsed),
            style = ClockTheme.typography.numericDisplaySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun Controls(
    state: StopwatchState,
    onStartOrPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
) {
    val colors = ClockTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Which controls exist depends on the state, rather than showing disabled buttons:
        // idle offers only Start; running offers Lap and Pause; paused offers Reset and Resume.
        if (state.isPaused) {
            OneUiCircleButton(
                label = "Reset",
                onClick = onReset,
                containerColor = colors.buttonSurface,
                contentColor = colors.textSecondary,
                icon = {
                    Text("↺", fontSize = 28.sp, color = colors.textPrimary)
                },
            )
        }
        if (state.isRunning) {
            OneUiCircleButton(
                label = "Lap",
                onClick = onLap,
                containerColor = colors.buttonSurface,
                contentColor = colors.textSecondary,
                icon = {
                    Text("+", fontSize = 32.sp, color = colors.textPrimary)
                },
            )
        }
        OneUiCircleButton(
            label = when {
                state.isRunning -> "Pause"
                state.isPaused -> "Resume"
                else -> "Start"
            },
            onClick = onStartOrPause,
            containerColor = if (state.isRunning) colors.buttonSurface else colors.accent,
            contentColor = colors.textSecondary,
            icon = {
                Text(
                    text = if (state.isRunning) "❙❙" else "▶",
                    fontSize = if (state.isRunning) 24.sp else 28.sp,
                    color = if (state.isRunning) colors.textPrimary else colors.onAccent,
                )
            },
        )
    }
}

@Composable
private fun LapList(
    laps: List<LapEntry>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    if (laps.isEmpty()) return
    val dimens = ClockTheme.dimens
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = dimens.navPillMargin,
            end = dimens.navPillMargin,
            bottom = bottomPadding,
        ),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ClockTheme.shapes.card)
                    .background(ClockTheme.colors.cardBackground),
            ) {
                laps.forEachIndexed { index, lap ->
                    if (index > 0) OneUiRowDivider()
                    LapRow(lap)
                }
            }
        }
    }
}

@Composable
private fun LapRow(lap: LapEntry) {
    val colors = ClockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ClockTheme.dimens.cardPadding,
                vertical = ClockTheme.dimens.rowPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%02d".format(lap.number),
            style = ClockTheme.typography.listTitle,
            color = colors.textSecondary,
            modifier = Modifier.size(width = 40.dp, height = 20.dp),
        )
        // The split is what the user cares about, so it is the emphasised value; the cumulative
        // total sits quieter on the right.
        Text(
            text = DurationFormat.stopwatch(lap.splitMillis),
            style = ClockTheme.typography.listTitle,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = DurationFormat.stopwatch(lap.totalMillis),
            style = ClockTheme.typography.alarmMeta,
            color = colors.textSecondary,
            textAlign = TextAlign.End,
        )
    }
}
