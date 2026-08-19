package com.asadrao.clock.ui.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.components.OneUiCollapsingHeaderLayout

/** The Timer tab. */
@Composable
fun TimerTab(
    gradient: Brush?,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: TimerViewModel = viewModel(
        key = "timer",
        factory = TimerViewModel.factory(container),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OneUiCollapsingHeaderLayout(
        title = "Timer",
        gradient = gradient,
        // Fixed: the body is a control to be manipulated, not a list to be scrolled. Without this
        // the header would consume the wheel's drag and scroll the page instead.
        collapseOnContentScroll = false,
        modifier = modifier,
    ) {
        TimerScreen(
            state = state,
            nowRealtime = viewModel::now,
            onStartNew = { h, m, s -> viewModel.addAndStart(h, m, s) },
            onStartQuick = viewModel::startQuickDuration,
            onSavePreset = { h, m, s, label -> viewModel.savePreset(h, m, s, label) },
            onStartPreset = viewModel::startPreset,
            onDeletePreset = viewModel::deletePreset,
            onStartOrPause = viewModel::startOrPause,
            onReset = viewModel::reset,
            onAddMinute = viewModel::addMinute,
            onDelete = viewModel::delete,
        )
    }
}
