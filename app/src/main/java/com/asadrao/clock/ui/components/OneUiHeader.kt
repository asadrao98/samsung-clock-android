package com.asadrao.clock.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.snapTween
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The very large collapsing title that opens every One UI screen.
 *
 * Three behaviours make it read as One UI rather than as Material's `LargeTopAppBar`:
 *
 * 1. **It is enormous.** Samsung's published guideline puts the expanded header at 39.67% of
 *    screen height — around 360dp on a Pixel 8, more than double Material's large app bar.
 * 2. **It snaps.** There is no resting state in between. You may drag it to any height, but the
 *    instant your finger lifts it goes fully open or fully closed. Material happily rests
 *    halfway, and that single difference is very noticeable.
 * 3. **It re-expands from anywhere.** Scrolling down does not have to reach the top of the list
 *    first; the header comes back immediately, wherever you are.
 *
 * The two titles — huge and toolbar-sized — are laid out separately and crossfaded rather than
 * one being scaled, which avoids font-scaling artifacts and, since there is no resting mid-state,
 * loses nothing.
 */
@Composable
fun OneUiCollapsingHeaderLayout(
    title: String,
    modifier: Modifier = Modifier,
    gradient: Brush? = null,
    /**
     * Whether content scrolling collapses the header.
     *
     * False for screens whose body is a fixed control rather than a list — the Timer's duration
     * drums and the Stopwatch's dial. Two reasons. Samsung keeps those screens still, and more
     * importantly Compose consults the *outermost* nested-scroll connection first, so a header
     * attached above a drum picker steals the drag: turning the wheel would scroll the page
     * underneath it. There is no way for the wheel to pre-empt an ancestor, so the ancestor must
     * not be listening in the first place.
     */
    collapseOnContentScroll: Boolean = true,
    state: CollapsingHeaderState = rememberCollapsingHeaderState(),
    toolbarStart: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val dimens = ClockTheme.dimens
    val colors = ClockTheme.colors
    val motion = ClockTheme.motion
    val density = LocalDensity.current

    // A phone in landscape has no room for a 360dp header, so the expandable behaviour is switched
    // off entirely rather than squeezed into something that looks broken.
    //
    // Measured from the window's own size rather than Configuration.screenHeightDp, which reports
    // the whole screen and so is wrong in split-screen and in a freeform window.
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val screenHeight = with(density) { containerHeightPx.toDp() }
    val expandable = collapseOnContentScroll &&
        screenHeight >= dimens.headerExpandableMinScreenHeight

    val expandedHeight = if (expandable) {
        maxOf(screenHeight * dimens.headerExpandedFraction, dimens.headerExpandedMin)
    } else {
        // A screen that does not collapse gets the compact title instead of the hero band. The
        // hero is ~40% of the screen, and on the Timer and Stopwatch that left too little room:
        // the dial fitted but the controls beneath it ended up under the floating pill, reachable
        // only by scrolling a screen that should not scroll at all. Samsung likewise keeps those
        // two on a small title.
        dimens.headerCollapsedHeight
    }

    val expandedPx = with(density) { expandedHeight.toPx() }
    val collapsedPx = with(density) { dimens.headerCollapsedHeight.toPx() }
    state.maxOffset = (expandedPx - collapsedPx).coerceAtLeast(0f)

    val scope = rememberCoroutineScope()
    val connection = remember(state, scope, motion) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A new drag cancels any snap still in flight, or the two fight each other.
                state.cancelSnap()
                val dy = available.y
                return when {
                    // Dragging up: close the header before the list moves at all.
                    dy < 0f -> Offset(0f, state.consumeCollapse(dy))
                    // Dragging down: re-open it first, from anywhere in the list. Handled here
                    // rather than in onPostScroll precisely so it does not have to wait for the
                    // list to reach its top.
                    dy > 0f -> Offset(0f, state.consumeExpand(dy))
                    else -> Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                state.snap(available.y, motion.snapTween(), scope)
                // The fling itself is left for the list; only the header's position is claimed.
                return Velocity.Zero
            }
        }
    }

    val fraction = state.collapseFraction
    val currentHeight = with(density) { (expandedPx - state.offset).toDp() }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = if (collapseOnContentScroll) modifier.nestedScroll(connection) else modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.pageBackground)
                .then(
                    // The gradient hero paints behind the big title and compresses away with it.
                    if (gradient != null && expandable) {
                        Modifier.background(gradient, alpha = 1f - fraction)
                    } else {
                        Modifier
                    }
                )
                .padding(statusBarPadding)
                .height(currentHeight),
        ) {
            // The toolbar row stays pinned and visible in both states — it does not fade out
            // with the big title.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.headerCollapsedHeight)
                    .align(Alignment.TopStart)
                    .padding(horizontal = dimens.screenMargin),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (toolbarStart != null) toolbarStart()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions,
                )
            }

            // Collapsed title: centred in the toolbar row, sharing the expanded title's centre
            // so the crossfade has no lateral jump.
            Text(
                text = title,
                style = ClockTheme.typography.screenTitleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(dimens.headerCollapsedHeight)
                    .padding(top = 16.dp)
                    // When the header can expand, the big copy below is the screen's heading and
                    // this one is only the visual half of the crossfade — leaving both in the
                    // tree makes TalkBack read the title twice. On a short screen there is no
                    // expanded copy, so this one has to carry the heading instead.
                    .then(
                        if (expandable) Modifier.clearAndSetSemantics {}
                        else Modifier.semantics { heading() }
                    )
                    .graphicsLayer {
                        alpha = if (!expandable) {
                            // A fixed header has no crossfade to run and its collapse fraction is
                            // always 0, so the formula below would hold this title invisible — which
                            // is exactly what happened: the Timer and Stopwatch lost their titles.
                            1f
                        } else {
                            // Holds off until the header is most of the way closed, so the two
                            // titles are never both at full strength.
                            ((fraction - 0.55f) / 0.45f).coerceIn(0f, 1f)
                        }
                    },
            )

            if (expandable) {
                // Expanded title: centred, sitting low in the hero area.
                Text(
                    text = title,
                    style = ClockTheme.typography.screenTitleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = dimens.screenMargin, end = dimens.screenMargin, bottom = 28.dp)
                        .semantics { heading() }
                        .graphicsLayer {
                            alpha = (1f - fraction / 0.45f).coerceIn(0f, 1f)
                        },
                )
            }
        }

        // weight(1f) states the intent directly: take whatever the header left. (fillMaxSize()
        // behaves the same here, because Column already measures later children against the
        // remaining space — worth knowing, since it is easy to assume otherwise.)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            content = content,
        )
    }
}

/**
 * Remembers a header's collapse position.
 *
 * Saved, so a tab that was left collapsed comes back collapsed — including across process death.
 * Because each tab's screen is hosted in its own `SaveableStateProvider`, one of these per tab
 * happens automatically.
 */
@Composable
fun rememberCollapsingHeaderState(): CollapsingHeaderState =
    rememberSaveable(saver = CollapsingHeaderState.Saver) { CollapsingHeaderState() }

/**
 * The header's scroll position, in pixels of collapse.
 *
 * `0` is fully expanded and [maxOffset] fully collapsed. Kept free of Compose UI types so the
 * arithmetic — which is where the awkward cases live — is unit-testable.
 */
class CollapsingHeaderState(
    maxOffset: Float = 0f,
    initialOffset: Float = 0f,
) {
    var maxOffset: Float = maxOffset
        set(value) {
            field = value
            // A font-scale or rotation change can shrink the range under a header that is
            // already part-closed.
            if (offset > value) offset = value
        }

    var offset: Float by mutableFloatStateOf(initialOffset.coerceIn(0f, maxOffset))
        private set

    private var snapJob: Job? = null

    /** 0 fully expanded, 1 fully collapsed. */
    val collapseFraction: Float
        get() = if (maxOffset <= 0f) 0f else (offset / maxOffset).coerceIn(0f, 1f)

    /** Retained name for the tested arithmetic. */
    val progress: Float get() = collapseFraction

    /** [dy] is negative. Returns the (negative) amount actually taken. */
    fun consumeCollapse(dy: Float): Float {
        val room = maxOffset - offset
        if (room <= 0f) return 0f
        val take = minOf(room, abs(dy))
        offset += take
        return -take
    }

    /** [dy] is positive. Returns the (positive) amount actually taken. */
    fun consumeExpand(dy: Float): Float {
        if (offset <= 0f) return 0f
        val take = minOf(offset, dy)
        offset -= take
        return take
    }

    /**
     * Where the header should come to rest, given the fling velocity that released it.
     *
     * Position decides by default, but a decisive flick wins regardless of where the header
     * happens to be — flicking up hard should close it even from barely-moved.
     */
    fun targetOffsetFor(velocity: Float): Float = when {
        velocity < -VELOCITY_THRESHOLD -> maxOffset   // flung up: close
        velocity > VELOCITY_THRESHOLD -> 0f           // flung down: open
        collapseFraction > 0.5f -> maxOffset
        else -> 0f
    }

    /** Animates to whichever end [targetOffsetFor] chose. */
    fun snap(
        velocity: Float,
        spec: androidx.compose.animation.core.AnimationSpec<Float>,
        scope: kotlinx.coroutines.CoroutineScope,
    ) {
        if (maxOffset <= 0f) return
        val target = targetOffsetFor(velocity)
        if (target == offset) return
        snapJob?.cancel()
        snapJob = scope.launch {
            animate(initialValue = offset, targetValue = target, animationSpec = spec) { v, _ ->
                offset = v.coerceIn(0f, maxOffset)
            }
        }
    }

    fun cancelSnap() {
        snapJob?.cancel()
        snapJob = null
    }

    companion object {
        /** Pixels per second past which a fling overrides position. ~400dp/s at 3x density. */
        private const val VELOCITY_THRESHOLD = 1200f

        val Saver: Saver<CollapsingHeaderState, List<Float>> = Saver(
            save = { listOf(it.maxOffset, it.offset) },
            restore = { CollapsingHeaderState(maxOffset = it[0], initialOffset = it[1]) },
        )
    }
}
