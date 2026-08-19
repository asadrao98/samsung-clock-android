package com.asadrao.clock

import com.asadrao.clock.ui.components.CollapsingHeaderState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The collapsing header's scroll arithmetic. Pure logic, so it is testable without a device —
 * which matters, because the gesture itself cannot be verified here.
 */
class CollapsingHeaderStateTest {

    @Test
    fun starts_fully_expanded() {
        val state = CollapsingHeaderState(maxOffset = 100f)
        assertEquals(0f, state.offset, 0f)
        assertEquals(0f, state.progress, 0f)
    }

    @Test
    fun dragging_up_collapses_and_reports_what_it_took() {
        val state = CollapsingHeaderState(maxOffset = 100f)
        val consumed = state.consumeCollapse(-30f)
        // Returns a negative delta, matching the sign convention of the scroll it consumed.
        assertEquals(-30f, consumed, 0f)
        assertEquals(30f, state.offset, 0f)
        assertEquals(0.3f, state.progress, 1e-4f)
    }

    @Test
    fun collapse_stops_at_the_limit_and_leaves_the_rest_for_the_list() {
        val state = CollapsingHeaderState(maxOffset = 100f)
        state.consumeCollapse(-80f)
        val consumed = state.consumeCollapse(-50f)
        // Only the remaining 20 is taken; the other 30 must fall through to the list, or the
        // list would refuse to scroll once the header bottomed out.
        assertEquals(-20f, consumed, 0f)
        assertEquals(100f, state.offset, 0f)
        assertEquals(1f, state.progress, 0f)
    }

    @Test
    fun a_fully_collapsed_header_consumes_nothing_further() {
        val state = CollapsingHeaderState(maxOffset = 100f)
        state.consumeCollapse(-100f)
        assertEquals(0f, state.consumeCollapse(-40f), 0f)
    }

    @Test
    fun dragging_down_re_expands_only_as_far_as_it_had_collapsed() {
        val state = CollapsingHeaderState(maxOffset = 100f)
        state.consumeCollapse(-60f)
        assertEquals(40f, state.consumeExpand(40f), 0f)
        assertEquals(20f, state.offset, 0f)
        // The remaining 20 is all that is left to give back.
        assertEquals(20f, state.consumeExpand(90f), 0f)
        assertEquals(0f, state.offset, 0f)
        assertEquals(0f, state.consumeExpand(10f), 0f)
    }

    @Test
    fun wrong_direction_deltas_are_ignored_by_each_path() {
        val state = CollapsingHeaderState(maxOffset = 100f)
        // consumeExpand is only ever handed positive deltas by the caller; a zero must be inert.
        assertEquals(0f, state.consumeExpand(0f), 0f)
        state.consumeCollapse(-50f)
        assertEquals(0f, state.consumeExpand(0f), 0f)
        assertEquals(50f, state.offset, 0f)
    }

    @Test
    fun shrinking_the_range_clamps_an_already_collapsed_header() {
        // Happens on a font-scale or density change: the expanded height is recomputed while
        // the header is part-way closed. The offset must not survive outside the new range.
        val state = CollapsingHeaderState(maxOffset = 100f)
        state.consumeCollapse(-90f)
        state.maxOffset = 40f
        assertEquals(40f, state.offset, 0f)
        assertEquals(1f, state.progress, 0f)
    }

    @Test
    fun a_zero_range_header_never_divides_by_zero() {
        // Possible if expanded and collapsed heights coincide at a large font scale.
        val state = CollapsingHeaderState(maxOffset = 0f)
        assertEquals(0f, state.progress, 0f)
        assertEquals(0f, state.consumeCollapse(-10f), 0f)
        assertEquals(0f, state.consumeExpand(10f), 0f)
    }

    @Test
    fun saver_round_trips_offset_and_range() {
        val state = CollapsingHeaderState(maxOffset = 120f)
        state.consumeCollapse(-45f)
        val saved = CollapsingHeaderState.Saver.run {
            @Suppress("UNCHECKED_CAST")
            val scope = androidx.compose.runtime.saveable.SaverScope { true }
            with(scope) { save(state) } as List<Float>
        }
        val restored = CollapsingHeaderState.Saver.restore(saved)!!
        assertEquals(120f, restored.maxOffset, 0f)
        assertEquals(45f, restored.offset, 0f)
    }
}
