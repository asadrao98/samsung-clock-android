package com.asadrao.clock.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Motion tokens: One UI's own sine-based curve family, plus the durations it pairs them with.
 *
 * **These curves do not overshoot.** No control point rises above 1. The springy quality One UI
 * is known for comes from how aggressively [ClockMotion.elastic50] decelerates — roughly
 * three-quarters of the distance in the first quarter of the time — not from bounce. So almost
 * everything here is a tween with one of these easings, and springs are reserved for
 * gesture-driven surfaces where physics is genuinely the right model (a sheet released mid-drag).
 *
 * Material 3's defaults are slower and flatter, so components must always take their spec from
 * here rather than relying on the default argument of `animate*AsState`.
 */
@Immutable
data class ClockMotion(
    /** Default state change. */
    val sineInOut70: Easing,
    /** A stronger settle — the toggle thumb. */
    val sineInOut80: Easing,
    /** Entrances. */
    val sineOut80: Easing,
    /** The signature One UI snap: popups, chips, press release. */
    val elastic50: Easing,
    /** Dialog and popup exits. */
    val exit: Easing,

    val durationInstant: Int,
    val durationShort: Int,
    val durationDefault: Int,
    val durationLong: Int,
    val durationPopup: Int,

    /** Press-down and press-release halves of the recoil. */
    val durationPressIn: Int,
    val durationPressOut: Int,

    /** For gesture releases only, where a spring is physically right. */
    val gestureSpringDamping: Float,
    val gestureSpringStiffness: Float,
) {
    /** One UI's tactile press: a small scale-down that recoils on release. */
    val pressedScale: Float get() = 0.96f
}

val ClockMotionTokens = ClockMotion(
    sineInOut70 = CubicBezierEasing(0.33f, 0.0f, 0.30f, 1.0f),
    sineInOut80 = CubicBezierEasing(0.33f, 0.0f, 0.20f, 1.0f),
    sineOut80 = CubicBezierEasing(0.17f, 0.17f, 0.20f, 1.0f),
    elastic50 = CubicBezierEasing(0.22f, 0.25f, 0.0f, 1.0f),
    exit = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f),

    durationInstant = 100,
    durationShort = 150,
    durationDefault = 220,
    durationLong = 300,
    durationPopup = 350,

    durationPressIn = 100,
    durationPressOut = 350,

    gestureSpringDamping = 0.9f,
    gestureSpringStiffness = Spring.StiffnessMediumLow,
)

// ---- Spec helpers -----------------------------------------------------------------------
// Typed per animated value, because Compose needs a visibility threshold suited to the unit:
// without one, Dp and IntOffset animations either stop visibly short or run on past the point
// anyone can see.

fun ClockMotion.shortTween(): FiniteAnimationSpec<Float> =
    tween(durationShort, easing = sineInOut70)

fun ClockMotion.defaultTween(): FiniteAnimationSpec<Float> =
    tween(durationDefault, easing = sineInOut70)

fun ClockMotion.enterTween(): FiniteAnimationSpec<Float> =
    tween(durationLong, easing = sineOut80)

fun ClockMotion.exitTween(): FiniteAnimationSpec<Float> =
    tween(durationLong, easing = exit)

fun ClockMotion.colorTween(): FiniteAnimationSpec<Color> =
    tween(durationShort, easing = sineInOut70)

/** The thumb of a toggle: a tween, which is closer to Samsung than a spring. */
fun ClockMotion.toggleTween(): FiniteAnimationSpec<Dp> =
    tween(durationShort, easing = sineInOut80)

/** Press release. Fast in, long elastic settle out. */
fun ClockMotion.pressTween(pressed: Boolean): FiniteAnimationSpec<Float> =
    if (pressed) tween(durationPressIn, easing = sineOut80)
    else tween(durationPressOut, easing = elastic50)

/** Header snap and other position settles driven by a released gesture. */
fun ClockMotion.snapTween(): FiniteAnimationSpec<Float> =
    tween(durationDefault, easing = sineInOut70)

fun ClockMotion.snapDpTween(): FiniteAnimationSpec<Dp> =
    tween(durationDefault, easing = sineInOut70)

fun ClockMotion.gestureFloatSpring(): AnimationSpec<Float> =
    spring(dampingRatio = gestureSpringDamping, stiffness = gestureSpringStiffness)

fun ClockMotion.gestureDpSpring(): AnimationSpec<Dp> =
    spring(
        dampingRatio = gestureSpringDamping,
        stiffness = gestureSpringStiffness,
        visibilityThreshold = Dp.VisibilityThreshold,
    )

fun ClockMotion.gestureIntOffsetSpring(): AnimationSpec<IntOffset> =
    spring(
        dampingRatio = gestureSpringDamping,
        stiffness = gestureSpringStiffness,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

fun ClockMotion.gestureIntSizeSpring(): AnimationSpec<IntSize> =
    spring(
        dampingRatio = gestureSpringDamping,
        stiffness = gestureSpringStiffness,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )
