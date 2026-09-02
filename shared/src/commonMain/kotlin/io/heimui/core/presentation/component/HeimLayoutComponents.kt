package io.heimui.core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.BoxComponent
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.model.component.HeimArrangement
import io.heimui.core.domain.model.component.Direction
import io.heimui.core.domain.model.component.DividerComponent
import io.heimui.core.domain.model.component.SpacerComponent
import io.heimui.core.presentation.HeimRenderer
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import io.heimui.core.presentation.designsystem.heimColor
import io.heimui.core.presentation.state.HeimStateManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
internal fun HeimContainerRenderer(
    component: ContainerComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = heimColor(component.backgroundColor, Color.Transparent)

    val containerModifier = modifier
        .fillMaxWidth()
        .heimSurface(
            backgroundColor = component.backgroundColor,
            cornerRadius = component.cornerRadius,
            borderColor = component.borderColor,
            borderWidth = component.borderWidth,
        )
        .heimPadding(component.padding)
        .heimAccessibility(component.a11y, componentId = component.id)

    // A vertical container taller than the viewport was previously clipped with no way to
    // scroll to the submit button. It scrolls now -- but never twice: nesting a scroller inside
    // a Lazy list or another scrolling column throws on infinite max-height constraints.
    val insideScroller = LocalInsideVerticalScroller.current
    val shouldScroll = component.direction == Direction.VERTICAL &&
        component.scrollable != false &&
        !insideScroller

    val scrolledModifier = if (shouldScroll) {
        containerModifier.verticalScroll(rememberScrollState())
    } else {
        containerModifier
    }

    CompositionLocalProvider(
        LocalInsideVerticalScroller provides (insideScroller || shouldScroll)
    ) {
    when (component.direction) {
        Direction.VERTICAL -> {
            Column(
                modifier = scrolledModifier,
                verticalArrangement = component.verticalArrangement(),
                horizontalAlignment = HeimTokenResolver.resolveHorizontalAlignment(component.alignment)
            ) {
                component.children.forEach { child ->
                    if (child is SpacerComponent) {
                        HeimSpacerRenderer(child)      // ColumnScope overload: weight works here
                    } else {
                        HeimRenderer(
                            component = child,
                            stateManager = stateManager,
                            onAction = onAction,
                            // Weight is only expressible from inside the scope, which is why the
                            // parent applies it rather than the child asking for it.
                            modifier = child.weight
                                ?.let { Modifier.weight(it) }
                                ?: Modifier
                        )
                    }
                }
            }
        }
        Direction.HORIZONTAL -> {
            // A horizontal row that overflows used to clip its last children with no way to
            // reach them: `scrollable` was only ever honoured on the vertical axis. Horizontal
            // overflow is the normal case for a chip row or a category strip, so silently
            // cutting it off loses content rather than merely looking wrong.
            //
            // Note this scrolls the row *and its padding* — the padding is inside the viewport,
            // so the first child sits `start` dp in and scrolls away with everything else. A
            // chip strip that must keep its inset while scrolling edge to edge wants `lazy_row`,
            // whose padding becomes `contentPadding` and therefore stays outside the scroll.
            // `scrollable` defaults to true, but a weighted child says the row divides a finite
            // width — and the two cannot both hold, since a scrolling axis is unbounded. An
            // explicit weight in the payload outranks a flag nobody set, so weight wins and the
            // row does not scroll. Without this the default silently defeated every weight.
            val hasWeightedChild = component.children.any { it.weight != null }
            // Opt-in, unlike the vertical axis: unbounded width stops text from wrapping, so a
            // row only scrolls when the payload actually asked for it.
            val horizontalModifier = if (component.scrollable == true && !hasWeightedChild) {
                containerModifier.horizontalScroll(rememberScrollState())
            } else {
                containerModifier
            }
            Row(
                modifier = horizontalModifier,
                horizontalArrangement = component.horizontalArrangement(),
                verticalAlignment = HeimTokenResolver.resolveVerticalAlignment(component.alignment)
            ) {
                component.children.forEach { child ->
                    if (child is SpacerComponent) {
                        HeimSpacerRenderer(child)      // RowScope overload
                    } else {
                        HeimRenderer(
                            component = child,
                            stateManager = stateManager,
                            onAction = onAction,
                            // A scrolling row measures against infinite width, and `weight` divides
                            // a finite one — asking for both throws. The scroll wins, because a
                            // payload that scrolls and weights is contradictory and crashing the
                            // screen over it helps nobody.
                            modifier = child.weight
                                ?.let { Modifier.weight(it) }
                                ?: Modifier
                        )
                    }
                }
            }
        }
    }
    }
}

/** True while composing inside a scrollable ancestor, so nested scrollers can opt out. */
internal val LocalInsideVerticalScroller = staticCompositionLocalOf { false }

@Composable
internal fun HeimBoxRenderer(
    component: BoxComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Surface before padding, so the colour and border frame the padded area rather than
            // being inset by it — the same order the container uses.
            .heimSurface(
                backgroundColor = component.backgroundColor,
                cornerRadius = component.cornerRadius,
                borderColor = component.borderColor,
                borderWidth = component.borderWidth,
            )
            .heimPadding(component.padding)
            .heimAccessibility(component.a11y, componentId = component.id),
        contentAlignment = HeimTokenResolver.resolveBoxAlignment(component.contentAlignment)
    ) {
        component.children.forEach { child ->
            HeimRenderer(
                component = child,
                stateManager = stateManager,
                onAction = onAction
            )
        }
    }
}

/**
 * A flexible spacer must consume the remaining space along the container's axis, which requires
 * `weight(1f)` from the enclosing scope. The previous `fillMaxWidth()` filled the cross axis in a
 * vertical container -- silently the opposite of what "flexible" declares.
 */
@Composable
internal fun ColumnScope.HeimSpacerRenderer(
    component: SpacerComponent,
    modifier: Modifier = Modifier
) {
    if (component.isFlexible) {
        Spacer(modifier = modifier.weight(1f))
    } else {
        Spacer(modifier = modifier.height(component.size.coerceAtLeast(0).dp))
    }
}

@Composable
internal fun RowScope.HeimSpacerRenderer(
    component: SpacerComponent,
    modifier: Modifier = Modifier
) {
    if (component.isFlexible) {
        Spacer(modifier = modifier.weight(1f))
    } else {
        Spacer(modifier = modifier.width(component.size.coerceAtLeast(0).dp))
    }
}

/** Fallback for spacers outside a Row/Column scope, where weight is not expressible. */
@Composable
internal fun HeimSpacerRenderer(
    component: SpacerComponent,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.size(component.size.coerceAtLeast(0).dp))
}

@Composable
internal fun HeimDividerRenderer(
    component: DividerComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val dividerColor = heimColor(component.color, DividerDefaults.color)

    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id),
        thickness = component.thickness.dp,
        color = dividerColor
    )
}

/**
 * Resolves a container's arrangement along its own axis.
 *
 * `spacedBy` is applied *with* the distributing arrangements, not instead of them: Compose treats
 * `spacing` as a minimum in that case, so a payload that sets both gets at least its gap and the
 * leftover space distributed on top. Dropping the spacing would silently ignore half the payload.
 */
private fun ContainerComponent.verticalArrangement(): Arrangement.Vertical =
    heimVerticalArrangement(arrangement, spacing)

private fun ContainerComponent.horizontalArrangement(): Arrangement.Horizontal =
    heimHorizontalArrangement(arrangement, spacing)

internal fun heimVerticalArrangement(arrangement: HeimArrangement, spacing: Int): Arrangement.Vertical {
    val gap = spacing.dp
    return when (arrangement) {
        HeimArrangement.PACKED -> Arrangement.spacedBy(gap)
        HeimArrangement.CENTER -> Arrangement.spacedBy(gap, Alignment.CenterVertically)
        HeimArrangement.END -> Arrangement.spacedBy(gap, Alignment.Bottom)
        HeimArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
        HeimArrangement.SPACE_AROUND -> Arrangement.SpaceAround
        HeimArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
}

internal fun heimHorizontalArrangement(arrangement: HeimArrangement, spacing: Int): Arrangement.Horizontal {
    val gap = spacing.dp
    return when (arrangement) {
        HeimArrangement.PACKED -> Arrangement.spacedBy(gap)
        HeimArrangement.CENTER -> Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
        HeimArrangement.END -> Arrangement.spacedBy(gap, Alignment.End)
        HeimArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
        HeimArrangement.SPACE_AROUND -> Arrangement.SpaceAround
        HeimArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
}

/**
 * Background, rounded corners and border, in the order they have to be applied.
 *
 * Clip before background, or a rounded container paints square corners underneath its own clip.
 * Border last, or it is drawn inside the clip and loses half its width to it.
 */
@Composable
private fun Modifier.heimSurface(
    backgroundColor: String?,
    cornerRadius: Int,
    borderColor: String?,
    borderWidth: Int,
): Modifier {
    val shape = if (cornerRadius > 0) RoundedCornerShape(cornerRadius.dp) else RectangleShape
    var m = this
    if (cornerRadius > 0) m = m.clip(shape)
    m = m.background(heimColor(backgroundColor, Color.Transparent), shape)
    if (borderColor != null && borderWidth > 0) {
        m = m.border(borderWidth.dp, heimColor(borderColor, Color.Transparent), shape)
    }
    return m
}
