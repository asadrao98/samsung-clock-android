package com.asadrao.clock.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.components.OneUiBottomSheet
import com.asadrao.clock.ui.components.OneUiFilledButton
import com.asadrao.clock.ui.components.OneUiSectionHeader
import com.asadrao.clock.ui.theme.ClockTheme

/** Snooze intervals Samsung offers, in minutes. */
private val SNOOZE_INTERVALS = listOf(1, 3, 5, 10, 15, 30)

/** Repeat counts. Zero stands for "forever". */
private val SNOOZE_REPEATS = listOf(1, 2, 3, 5, 0)

/**
 * Snooze settings, as a bottom sheet.
 *
 * A repeat limit of `0` means unlimited — stored as a count rather than a flag so the two ideas
 * do not need separate fields.
 */
@Composable
fun SnoozeSheet(
    durationMinutes: Int,
    repeatLimit: Int,
    onConfirm: (durationMinutes: Int, repeatLimit: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var duration by remember { mutableIntStateOf(durationMinutes) }
    var repeats by remember { mutableIntStateOf(repeatLimit) }

    OneUiBottomSheet(onDismissRequest = onDismiss) {
        OneUiSectionHeader("Snooze interval")
        OptionRow(
            options = SNOOZE_INTERVALS,
            selected = duration,
            label = { "$it min" },
            onSelect = { duration = it },
        )
        OneUiSectionHeader("Repeat")
        OptionRow(
            options = SNOOZE_REPEATS,
            selected = repeats,
            label = { if (it == 0) "Forever" else "$it" },
            onSelect = { repeats = it },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClockTheme.dimens.screenMargin, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            OneUiFilledButton(
                text = "Done",
                onClick = { onConfirm(duration, repeats) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OptionRow(
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    val colors = ClockTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ClockTheme.dimens.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ClockTheme.shapes.pill)
                    .background(if (isSelected) colors.accent else colors.buttonSurface)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = ClockTheme.typography.buttonLabel,
                    color = if (isSelected) colors.onAccent else colors.textSecondary,
                )
            }
        }
    }
}
