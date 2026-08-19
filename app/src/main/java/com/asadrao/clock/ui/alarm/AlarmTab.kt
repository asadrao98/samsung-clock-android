package com.asadrao.clock.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asadrao.clock.R
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.schedule.AlarmSchedule
import com.asadrao.clock.ui.components.OneUiCollapsingHeaderLayout
import com.asadrao.clock.ui.components.OneUiPopupMenu
import com.asadrao.clock.ui.components.OneUiPopupMenuItem
import com.asadrao.clock.ui.components.OneUiTextButton
import com.asadrao.clock.ui.components.OneUiToastState
import com.asadrao.clock.ui.format.AlarmFormat
import com.asadrao.clock.ui.format.rememberIs24HourFormat
import com.asadrao.clock.ui.format.rememberLocale
import com.asadrao.clock.ui.theme.ClockTheme
import java.time.ZonedDateTime

/**
 * The Alarm tab: header, list, and the select-mode transformation.
 *
 * The header carries "+" then "⋮" at the top right, and no floating action button — One UI has
 * none anywhere. Settings lives inside the overflow menu rather than as a gear icon or a fifth
 * navigation destination.
 */
@Composable
fun AlarmTab(
    gradient: Brush?,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    toastState: OneUiToastState,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: AlarmListViewModel = viewModel(factory = AlarmListViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val is24Hour = rememberIs24HourFormat()
    val locale = rememberLocale()
    val colors = ClockTheme.colors
    var menuOpen by remember { mutableStateOf(false) }

    // Recomputed once per collection rather than per row, so every row's "Tomorrow" agrees.
    val now = remember(state.alarms) { ZonedDateTime.now() }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AlarmListEvent.AlarmEnabled -> toastState.show(
                    "Alarm set for " + AlarmFormat.timeUntil(
                        nextTrigger = event.ringsAt,
                        now = ZonedDateTime.now(),
                        hourUnit = "hr",
                        minuteUnit = "min",
                        lessThanAMinute = "less than a minute",
                    ) + " from now"
                )
                is AlarmListEvent.AlarmsDeleted -> toastState.show(
                    if (event.count == 1) "Alarm deleted" else "${event.count} alarms deleted"
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        OneUiCollapsingHeaderLayout(
            title = if (state.selectionMode) selectionTitle(state) else "Alarm",
            gradient = gradient,
            toolbarStart = if (state.selectionMode) {
                {
                    // Leading checkbox selects every alarm at once.
                    OneUiTextButton(
                        text = if (state.allSelected) "Deselect all" else "All",
                        onClick = { viewModel.selectAll(!state.allSelected) },
                    )
                }
            } else {
                null
            },
            actions = {
                if (state.selectionMode) {
                    OneUiTextButton(text = "Cancel", onClick = viewModel::exitSelection)
                } else {
                    IconButton(onClick = { onEditAlarm(Alarm.NO_ID) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add alarm",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(ClockTheme.dimens.iconSize),
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vertical),
                                contentDescription = "More options",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(ClockTheme.dimens.iconSize),
                            )
                        }
                        OneUiPopupMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            if (state.alarms.isNotEmpty()) {
                                OneUiPopupMenuItem(
                                    text = "Edit",
                                    onClick = {
                                        menuOpen = false
                                        viewModel.enterSelection()
                                    },
                                )
                            }
                            // Settings is always the last row.
                            OneUiPopupMenuItem(
                                text = "Settings",
                                onClick = {
                                    menuOpen = false
                                    onOpenSettings()
                                },
                            )
                        }
                    }
                }
            },
        ) {
            AlarmListContent(
                state = state,
                is24Hour = is24Hour,
                locale = locale,
                now = now,
                nextTriggerFor = { alarm ->
                    if (alarm.enabled) AlarmSchedule.nextTrigger(alarm, now)
                    else AlarmSchedule.nextTrigger(alarm.copy(enabled = true), now)
                },
                onAlarmClick = { alarm ->
                    if (state.selectionMode) viewModel.toggleSelected(alarm.id)
                    else onEditAlarm(alarm.id)
                },
                onAlarmLongClick = { alarm ->
                    // Long-press selects the alarm you pressed; the menu route selects nothing.
                    if (!state.selectionMode) viewModel.startSelectionWith(alarm.id)
                },
                onToggle = viewModel::setEnabled,
            )
        }

        if (state.selectionMode) {
            SelectionActionBar(
                selectedCount = state.selectedIds.size,
                onDelete = viewModel::deleteSelected,
                onTurnOn = { viewModel.setSelectedEnabled(true) },
                onTurnOff = { viewModel.setSelectedEnabled(false) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun selectionTitle(state: AlarmListUiState): String = when (state.selectedIds.size) {
    0 -> "Select alarms"
    else -> "${state.selectedIds.size} selected"
}

/**
 * Replaces the floating tab pill while selecting. Full-width and flush to the bottom, because
 * navigation is disabled in this mode — a floating pill would imply you could still switch tabs.
 *
 * Shows nothing at all when the selection is empty, rather than a row of disabled actions.
 */
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedCount == 0) return
    val colors = ClockTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.elevatedBackground)
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OneUiTextButton(text = "Turn on", onClick = onTurnOn, modifier = Modifier.weight(1f))
        OneUiTextButton(text = "Turn off", onClick = onTurnOff, modifier = Modifier.weight(1f))
        OneUiTextButton(
            text = "Delete",
            onClick = onDelete,
            color = colors.dangerText,
            modifier = Modifier.weight(1f),
        )
    }
}
