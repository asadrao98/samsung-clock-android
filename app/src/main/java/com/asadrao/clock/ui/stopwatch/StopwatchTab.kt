package com.asadrao.clock.ui.stopwatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.components.OneUiCollapsingHeaderLayout

/**
 * The Stopwatch tab.
 *
 * The view model is scoped above the tab switch, so a running stopwatch keeps running — and keeps
 * its laps — while the user is on another tab.
 */
@Composable
fun StopwatchTab(
    gradient: Brush?,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: StopwatchViewModel = viewModel(
        key = "stopwatch",
        factory = StopwatchViewModel.factory(container),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    OneUiCollapsingHeaderLayout(
        title = "Stopwatch",
        gradient = gradient,
        // Fixed: the body is a control to be manipulated, not a list to be scrolled. Without this
        // the header would consume the wheel's drag and scroll the page instead.
        collapseOnContentScroll = false,
        modifier = modifier,
    ) {
        StopwatchScreen(
            state = state,
            nowRealtime = viewModel::now,
            onStartOrPause = viewModel::startOrPause,
            onLap = viewModel::lap,
            onReset = viewModel::reset,
        )
    }
}
