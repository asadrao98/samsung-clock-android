package com.asadrao.clock.di

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Hands the object graph to composables without a DI framework.
 *
 * Provided once, in `MainActivity`. Screens read it to build their view models, which keeps the
 * wiring visible at the call site instead of hidden behind annotations.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided — wrap the UI in a CompositionLocalProvider")
}
