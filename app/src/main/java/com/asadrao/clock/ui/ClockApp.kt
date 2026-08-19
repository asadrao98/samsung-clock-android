package com.asadrao.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.asadrao.clock.ui.alarm.AlarmEditorRoute
import com.asadrao.clock.ui.alarm.AlarmTab
import com.asadrao.clock.ui.settings.SettingsRoute
import com.asadrao.clock.ui.stopwatch.StopwatchTab
import com.asadrao.clock.ui.timer.TimerTab
import com.asadrao.clock.ui.worldclock.CitySearchRoute
import com.asadrao.clock.ui.worldclock.WorldClockTab
import com.asadrao.clock.ui.components.OneUiToast
import com.asadrao.clock.ui.components.rememberOneUiToastState
import com.asadrao.clock.ui.navigation.ClockBottomNav
import com.asadrao.clock.ui.navigation.ClockTab
import com.asadrao.clock.ui.navigation.rememberTabGradient
import com.asadrao.clock.ui.theme.ClockTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_ALARM_EDITOR = "alarm_editor"
private const val ROUTE_CITY_SEARCH = "city_search"
private const val ROUTE_SETTINGS = "settings"
private const val ARG_ALARM_ID = "alarmId"

/**
 * The app's navigation graph.
 *
 * Only second-depth screens are destinations. The four tabs are *not* — they live inside one
 * "home" destination and switch via the floating pill, which is what lets each tab keep its own
 * scroll position, header state and running timers. Routing them through the nav graph would
 * tear each one down on every switch.
 */
@Composable
fun ClockApp() {
    RequestNotificationPermission()
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onEditAlarm = { alarmId ->
                    navController.navigate("$ROUTE_ALARM_EDITOR/$alarmId")
                },
                onAddCity = { navController.navigate(ROUTE_CITY_SEARCH) },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsRoute(onClose = { navController.popBackStack() })
        }
        composable(ROUTE_CITY_SEARCH) {
            CitySearchRoute(onClose = { navController.popBackStack() })
        }
        composable("$ROUTE_ALARM_EDITOR/{$ARG_ALARM_ID}") { entry ->
            val alarmId = entry.arguments?.getString(ARG_ALARM_ID)?.toLongOrNull() ?: 0L
            AlarmEditorRoute(
                alarmId = alarmId,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

/**
 * The tab shell: content with a floating navigation pill on top of it.
 *
 * A `Box`, not a `Column` — the pill hovers over the content rather than sitting below it, and the
 * content scrolls underneath. Each tab supplies its own header so it can carry its own actions and
 * gradient.
 */
@Composable
private fun HomeScreen(
    onEditAlarm: (Long) -> Unit,
    onAddCity: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(ClockTab.Default) }
    val stateHolder = rememberSaveableStateHolder()
    val toastState = rememberOneUiToastState()
    val navInset = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClockTheme.colors.pageBackground),
    ) {
        // Keyed on the tab, so a tab's scroll position and collapsed header are saved as it
        // leaves composition and restored when the user comes back.
        stateHolder.SaveableStateProvider(selectedTab.stateKey) {
            val gradient = rememberTabGradient(selectedTab)
            when (selectedTab) {
                ClockTab.Alarm -> AlarmTab(
                    gradient = gradient,
                    onEditAlarm = onEditAlarm,
                    onOpenSettings = onOpenSettings,
                    toastState = toastState,
                )
                ClockTab.WorldClock -> WorldClockTab(
                    gradient = gradient,
                    onAddCity = onAddCity,
                )
                ClockTab.Stopwatch -> StopwatchTab(gradient = gradient)
                ClockTab.Timer -> TimerTab(gradient = gradient)
            }
        }

        OneUiToast(
            state = toastState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = navInset.calculateBottomPadding() +
                        ClockTheme.dimens.navPillHeight +
                        ClockTheme.dimens.navPillBottomGap +
                        ClockTheme.dimens.cardSpacing,
                ),
        )

        ClockBottomNav(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
