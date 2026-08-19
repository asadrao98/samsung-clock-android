package com.asadrao.clock.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.asadrao.clock.ui.theme.ClockTheme
import com.asadrao.clock.ui.theme.colorTween
import com.asadrao.clock.ui.theme.rememberPressScale
import com.asadrao.clock.ui.theme.snapDpTween

/**
 * One UI 8.5's bottom navigation: a **detached floating pill**, not a bar.
 *
 * This is the single biggest break from every earlier One UI, which used a full-width row of
 * text-only tabs. 8.5 hovers a translucent capsule above the content with margins on all three
 * sides, containing four **icon-only** destinations. Content scrolls underneath it, so callers
 * must add [ClockTheme.dimens.contentBottomPadding] to their scrollable's `contentPadding`.
 *
 * Three things here are deliberately not Material 3:
 *
 * - **No labels.** The destination name lives in the header title instead. Samsung's own older
 *   guidance says tabs should be text-only — that describes One UI 1.x to 8.0, and 8.5 reverses
 *   it. Corroborated across several independent write-ups of the redesign.
 * - **A circular indicator, in neutral grey**, not Material's horizontal stadium pill and not
 *   accent blue. Reserving blue for controls and using high-contrast neutral for navigation is
 *   the One UI signature.
 * - **It floats.** This is the one place in the app where a shadow is allowed, because the pill
 *   genuinely sits above the content rather than being a surface the content rests on.
 *
 * The pill never hides or shrinks on scroll.
 */
@Composable
fun ClockBottomNav(
    selected: ClockTab,
    onSelect: (ClockTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimens.navPillMargin,
                end = dimens.navPillMargin,
                // Sits above the gesture-nav home indicator, never behind it.
                bottom = navInset + dimens.navPillBottomGap,
            )
            .height(dimens.navPillHeight)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(CircleShape)
            .background(
                if (colors.isDark) Color.White.copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.72f)
            )
            // A faint top-to-bottom lightening suggests the capsule's curvature.
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (colors.isDark) 0.06f else 0.10f), Color.Transparent)
                )
            )
            .border(
                width = 1.dp,
                color = if (colors.isDark) Color.White.copy(alpha = 0.08f)
                else Color.Black.copy(alpha = 0.05f),
                shape = CircleShape,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClockTab.entries.forEach { tab ->
                NavItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: ClockTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClockTheme.colors
    val dimens = ClockTheme.dimens
    val motion = ClockTheme.motion
    // Icon-only, so the accessible name has to come from somewhere: it becomes the click label
    // and the icon's content description.
    val label = stringResource(tab.labelRes)

    val tint by animateColorAsState(
        targetValue = when {
            selected && colors.isDark -> Color.White
            selected -> Color(0xFF101012)
            colors.isDark -> Color.White.copy(alpha = 0.55f)
            else -> Color.Black.copy(alpha = 0.45f)
        },
        animationSpec = motion.colorTween(),
        label = "navItemTint",
    )
    val circleSize by animateDpAsState(
        targetValue = if (selected) dimens.navSelectedCircleSize else 0.dp,
        animationSpec = motion.snapDpTween(),
        label = "navSelectedCircle",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    Box(
        modifier = modifier
            .height(dimens.navPillHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // The neutral emphasis circle. Grows in behind the selected icon.
        if (circleSize > 0.dp) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(
                        if (colors.isDark) Color.White.copy(alpha = 0.16f)
                        else Color.Black.copy(alpha = 0.07f)
                    )
            )
        }
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(dimens.iconSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
        )
    }
}
