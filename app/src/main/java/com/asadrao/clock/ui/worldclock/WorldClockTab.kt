package com.asadrao.clock.ui.worldclock

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asadrao.clock.R
import com.asadrao.clock.di.LocalAppContainer
import com.asadrao.clock.ui.components.OneUiCollapsingHeaderLayout
import com.asadrao.clock.ui.format.rememberIs24HourFormat
import com.asadrao.clock.ui.format.rememberLocale
import com.asadrao.clock.ui.theme.ClockTheme

/** The World clock tab. */
@Composable
fun WorldClockTab(
    gradient: Brush?,
    onAddCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: WorldClockViewModel = viewModel(
        key = "worldclock",
        factory = WorldClockViewModel.factory(container),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OneUiCollapsingHeaderLayout(
        title = "World clock",
        gradient = gradient,
        modifier = modifier,
        actions = {
            IconButton(onClick = onAddCity) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add city",
                    tint = ClockTheme.colors.textPrimary,
                    modifier = Modifier.size(ClockTheme.dimens.iconSize),
                )
            }
        },
    ) {
        WorldClockContent(
            state = state,
            is24Hour = rememberIs24HourFormat(),
            locale = rememberLocale(),
            onRemove = viewModel::removeCity,
            onAddCity = onAddCity,
        )
    }
}

/** The Add city destination, bound to the same view model instance as the tab. */
@Composable
fun CitySearchRoute(onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: WorldClockViewModel = viewModel(
        key = "worldclock",
        factory = WorldClockViewModel.factory(container),
    )
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    CitySearchScreen(
        state = searchState,
        is24Hour = rememberIs24HourFormat(),
        locale = rememberLocale(),
        onQueryChange = viewModel::setQuery,
        onAdd = viewModel::addCity,
        onClose = onClose,
    )
}
