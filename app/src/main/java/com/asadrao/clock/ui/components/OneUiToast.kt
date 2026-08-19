package com.asadrao.clock.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockTheme
import kotlinx.coroutines.delay

/**
 * The confirmation that floats briefly above the tab pill — "Alarm set for 8 hr 43 min from now".
 *
 * A dark rounded pill rather than a Material `Snackbar`: no action button, no square corners, no
 * left-aligned full-width bar. It is purely a confirmation, so it is not interactive and does not
 * steal focus.
 */
class OneUiToastState {
    var message: String? by mutableStateOf(null)
        private set

    fun show(text: String) {
        message = text
    }

    internal fun clear() {
        message = null
    }
}

@Composable
fun rememberOneUiToastState(): OneUiToastState = remember { OneUiToastState() }

@Composable
fun OneUiToast(
    state: OneUiToastState,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2_200,
) {
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            delay(durationMillis)
            state.clear()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClockTheme.dimens.screenMargin),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .clip(ClockTheme.shapes.card)
                    .background(
                        if (ClockTheme.colors.isDark) Color(0xFF3A3A3D) else Color(0xFF2A2A2E)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message.orEmpty(),
                    style = ClockTheme.typography.alarmMeta,
                    color = Color.White,
                )
            }
        }
    }
}
