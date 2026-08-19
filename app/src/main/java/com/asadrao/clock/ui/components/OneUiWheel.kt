package com.asadrao.clock.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

/**
 * One UI's drum picker column.
 *
 * Not Material 3's `TimePicker`, and not a styled version of it. The selected value is identified
 * by being large and fully opaque while its neighbours shrink and fade — there is no ruled band
 * and no accent-coloured highlight.
 *
 * Several details here are load-bearing and easy to get subtly wrong:
 *
 * - **The falloff runs in the draw phase**, via `graphicsLayer`, reading the list's layout info.
 *   Scaling by animating `fontSize` instead would re-measure every item every frame, which drops
 *   frames and breaks the snapping arithmetic.
 * - **`contentPadding` is derived from the same `itemHeight` constant** used for the rows. Two
 *   independent constants is how the centred index ends up off by one.
 * - **Looping is faked with a large finite count**, anchored in the middle. `Int.MAX_VALUE` looks
 *   tempting but the offset arithmetic overflows and the halfway anchor lands badly.
 * - **The initial index is set in the state's constructor**, never by scrolling in a
 *   `LaunchedEffect`, which would show a visible jump on first frame.
 * - **Haptics come from a `snapshotFlow`**, not from the composition body, or the wheel ticks on
 *   every recomposition rather than once per value change.
 */
@Composable
fun OneUiWheel(
    itemCount: Int,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    label: (Int) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    width: Dp = 88.dp,
    itemHeight: Dp = 64.dp,
    visibleItems: Int = 3,
    loop: Boolean = true,
    textStyle: TextStyle,
    color: Color,
) {
    require(visibleItems % 2 == 1) { "visibleItems must be odd so one slot is exactly centred" }

    // A large finite repeat count stands in for infinite scrolling, anchored so the user can
    // travel a very long way in either direction before reaching an end they cannot see.
    val repeats = if (loop) LOOP_REPEATS else 1
    val totalItems = itemCount * repeats
    val anchor = if (loop) (repeats / 2) * itemCount else 0

    // Because the list is padded by exactly one slot at the top, the item at
    // firstVisibleItemIndex is the one sitting in the centre — not the one below it. Subtracting
    // slotsAboveCentre here (as the padding might suggest) puts every wheel one item out, which on
    // a 24-hour drum reads as 23 when the value is 0.
    val state = rememberLazyListState(initialFirstVisibleItemIndex = anchor + selectedIndex)

    // Derived from the measured layout rather than from index arithmetic. The arithmetic has to
    // agree exactly with contentPadding, and when it silently disagrees the whole wheel is off by
    // one; asking which item is actually nearest the middle cannot drift.
    val centredIndex by remember(state, itemCount) {
        derivedStateOf {
            val info = state.layoutInfo
            val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo
                .minByOrNull { kotlin.math.abs((it.offset + it.size / 2f) - viewportCentre) }
                ?.index
                ?.mod(itemCount)
                ?: selectedIndex
        }
    }

    val view = LocalView.current
    LaunchedEffect(state, itemCount) {
        snapshotFlow { centredIndex }
            .distinctUntilChanged()
            .collect { index ->
                // The platform's own picker tick, so it matches every other wheel on the device.
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onSelectedIndexChange(index)
            }
    }

    // Keeps the wheel in step when the value is changed from outside — e.g. the 12/24-hour
    // format flipping while the editor is open.
    LaunchedEffect(selectedIndex) {
        if (centredIndex != selectedIndex && !state.isScrollInProgress) {
            state.scrollToItem(anchor + selectedIndex)
        }
    }

    val density = LocalDensity.current
    val windowHeight = itemHeight * visibleItems
    val edgePadding = (windowHeight - itemHeight) / 2

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        modifier = modifier
            .width(width)
            .height(windowHeight)
            .clearAndSetSemantics {
                // The column as a whole is the control; the individual numbers are not
                // separately meaningful to a screen reader.
                this.contentDescription = contentDescription
                this.stateDescription = label(selectedIndex)
            },
        // Lets the first and last items reach the centre slot.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = edgePadding,
            bottom = edgePadding,
        ),
    ) {
        items(totalItems) { index ->
            val value = index % itemCount
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Fractional distance from the viewport centre, in item heights.
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            ?: return@graphicsLayer
                        val viewportCentre =
                            (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val itemCentre = item.offset + item.size / 2f
                        val d = ((itemCentre - viewportCentre) / item.size).absoluteValue

                        // Continuous, so values mid-drag fade smoothly instead of snapping
                        // between two fixed styles.
                        alpha = if (d <= 1f) lerp(1f, 0.28f, d) else lerp(0.28f, 0.10f, (d - 1f).coerceIn(0f, 1f))
                        val s = if (d <= 1f) lerp(1f, 0.62f, d) else lerp(0.62f, 0.5f, (d - 1f).coerceIn(0f, 1f))
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(value),
                    style = textStyle,
                    color = color,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Odd on purpose so the middle repeat is exactly the anchor, and small enough that
 * `index * itemHeight` never approaches overflow.
 */
private const val LOOP_REPEATS = 2001
