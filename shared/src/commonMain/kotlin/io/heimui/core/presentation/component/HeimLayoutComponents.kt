package io.heimui.core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import io.heimui.core.domain.model.component.HeimComponent
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
    val fillsWidth = heimFillsWidth(component)

    val containerModifier = modifier
        .then(if (fillsWidth) Modifier.fillMaxWidth() else Modifier)
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

    CompositionLocalProvider(
        LocalInsideVerticalScroller provides (insideScroller || shouldScroll),
        // Set on the way into a row and cleared on the way into a column. Only setting it would
        // latch the flag on for the whole subtree, so everything below a row -- however deep, and
        // however many columns down -- would stop filling the width its own column offers it.
        LocalInsideHorizontalContainer provides (component.direction == Direction.HORIZONTAL),
    ) {
        when (component.direction) {
            Direction.VERTICAL -> HeimVerticalContainer(
                component = component,
                stateManager = stateManager,
                onAction = onAction,
                containerModifier = containerModifier,
                fillsWidth = fillsWidth,
                shouldScroll = shouldScroll,
            )

            Direction.HORIZONTAL -> {
                // A horizontal row that overflows used to clip its last children with no way to
                // reach them: `scrollable` was only ever honoured on the vertical axis. Horizontal
                // overflow is the normal case for a chip row or a category strip, so silently
                // cutting it off loses content rather than merely looking wrong.
                //
                // Note this scrolls the row *and its padding* — the padding is inside the
                // viewport, so the first child sits `start` dp in and scrolls away with everything
                // else. A chip strip that must keep its inset while scrolling edge to edge wants
                // `lazy_row`, whose padding becomes `contentPadding` and therefore stays outside
                // the scroll. `scrollable` defaults to true, but a weighted child says the row
                // divides a finite width — and the two cannot both hold, since a scrolling axis is
                // unbounded. An explicit weight in the payload outranks a flag nobody set, so
                // weight wins and the row does not scroll. Without this the default silently
                // defeated every weight.
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
                                // A scrolling row measures against infinite width, and `weight`
                                // divides a finite one — asking for both throws. The scroll wins,
                                // because a payload that scrolls and weights is contradictory and
                                // crashing the screen over it helps nobody.
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

/**
 * The vertical half of [HeimContainerRenderer], split out because a distributing arrangement needs
 * a second layout pass around the column.
 *
 * A column is only as tall as its children, so CENTER, END and the SPACE_* arrangements had no
 * height to distribute within and quietly did nothing -- the payload asked to centre, the JSON
 * carried the request all the way down, and the screen still rendered top-aligned.
 */
@Composable
private fun HeimVerticalContainer(
    component: ContainerComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    containerModifier: Modifier,
    fillsWidth: Boolean,
    shouldScroll: Boolean,
) {
    if (component.arrangement == HeimArrangement.PACKED) {
        // The default, and the one arrangement that needs no height of its own: packing children
        // at the start is what a column wrapping its content already does. Kept on the exact
        // layout path it has always had, so the common payload is untouched by any of this.
        Column(
            modifier = if (shouldScroll) {
                containerModifier.verticalScroll(rememberScrollState())
            } else {
                containerModifier
            },
            verticalArrangement = component.verticalArrangement(),
            horizontalAlignment = HeimTokenResolver.resolveHorizontalAlignment(component.alignment)
        ) {
            HeimContainerChildren(component, stateManager, onAction)
        }
        return
    }

    // The viewport height has to be read *outside* the scroller: inside one the incoming max
    // height is infinite, which is exactly why the arrangement had nothing to work with. The
    // column then takes at least that height, so short content is distributed across the screen
    // and long content still grows past it and scrolls.
    BoxWithConstraints(modifier = containerModifier) {
        val viewport = maxHeight
        var innerModifier: Modifier = Modifier
        if (shouldScroll) innerModifier = innerModifier.verticalScroll(rememberScrollState())
        if (fillsWidth) innerModifier = innerModifier.fillMaxWidth()
        // Infinite when an ancestor already scrolls vertically: there is no viewport to fill
        // there, and `heightIn(Dp.Infinity)` is not a size any layout can honour.
        if (viewport.value.isFinite()) innerModifier = innerModifier.heightIn(min = viewport)

        Column(
            modifier = innerModifier,
            verticalArrangement = component.verticalArrangement(),
            horizontalAlignment = HeimTokenResolver.resolveHorizontalAlignment(component.alignment)
        ) {
            HeimContainerChildren(component, stateManager, onAction)
        }
    }
}

@Composable
private fun ColumnScope.HeimContainerChildren(
    component: ContainerComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
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

/** True while composing inside a scrollable ancestor, so nested scrollers can opt out. */
internal val LocalInsideVerticalScroller = staticCompositionLocalOf { false }

/**
 * True while composing directly inside a row, so a child does not greedily fill the whole width.
 *
 * Every renderer that provides it has to provide it in both directions — see the note in
 * [HeimContainerRenderer].
 */
internal val LocalInsideHorizontalContainer = staticCompositionLocalOf { false }

/**
 * Whether a component should claim the full width offered to it.
 *
 * Filling is the right default for a block-level component: it is what makes a stack of cards line
 * up without every payload having to say so. Inside a row it is the wrong one — an unweighted
 * child that fills takes the entire row and starves its siblings — so there the component wraps
 * its content instead. An explicit width or weight in the payload outranks both.
 */
@Composable
internal fun heimFillsWidth(component: HeimComponent): Boolean =
    !LocalInsideHorizontalContainer.current &&
        component.frame.width == null &&
        component.frame.minWidth == null &&
        component.weight == null

/** [heimFillsWidth] applied, for the renderers that need nothing else from it. */
@Composable
internal fun Modifier.heimFillWidth(component: HeimComponent): Modifier =
    if (heimFillsWidth(component)) fillMaxWidth() else this

@Composable
internal fun HeimBoxRenderer(
    component: BoxComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heimFillWidth(component)
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
