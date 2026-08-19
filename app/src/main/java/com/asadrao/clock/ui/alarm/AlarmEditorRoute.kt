package com.asadrao.clock.ui.alarm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.format.rememberIs24HourFormat
import com.asadrao.clock.ui.format.rememberLocale

/** Binds [AlarmEditorViewModel] to [AlarmEditorScreen]. */
@Composable
fun AlarmEditorRoute(
    alarmId: Long,
    onClose: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: AlarmEditorViewModel = viewModel(
        // Keyed by id so navigating from one alarm to another does not reuse the first one's
        // working copy.
        key = "alarm-editor-$alarmId",
        factory = AlarmEditorViewModel.factory(container, alarmId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Nothing is rendered until the alarm has been read, so the picker never briefly shows a
    // default time before jumping to the stored one.
    if (!state.loaded) return

    AlarmEditorScreen(
        state = state,
        is24Hour = rememberIs24HourFormat(),
        locale = rememberLocale(),
        onTimeChange = viewModel::setTime,
        onToggleDay = viewModel::toggleDay,
        onLabelChange = viewModel::setLabel,
        onSoundChange = viewModel::setSound,
        onVibrationChange = viewModel::setVibration,
        onSnoozeEnabledChange = viewModel::setSnoozeEnabled,
        onSnoozeChange = viewModel::setSnooze,
        onCancel = onClose,
        onSave = { viewModel.save { onClose() } },
        onDelete = { viewModel.delete { onClose() } },
    )
}
