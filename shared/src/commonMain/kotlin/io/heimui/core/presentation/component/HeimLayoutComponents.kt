package io.heimui.core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import io.heimui.core.domain.model.component.Direction
import io.heimui.core.domain.model.component.DividerComponent
import io.heimui.core.domain.model.component.SpacerComponent
import io.heimui.core.presentation.HeimRenderer
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import io.heimui.core.presentation.designsystem.heimColor
import io.heimui.core.presentation.state.HeimStateManager
import androidx.compose.foundation.rememberScrollState
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
        .background(bgColor)
        .padding(component.padding.dp)
        .heimAccessibility(component.a11y, componentId = component.id)

    // A vertical container taller than the viewport was previously clipped with no way to
    // scroll to the submit button. It scrolls now -- but never twice: nesting a scroller inside
    // a Lazy list or another scrolling column throws on infinite max-height constraints.
    val insideScroller = LocalInsideVerticalScroller.current
    val shouldScroll = component.direction == Direction.VERTICAL &&
        component.scrollable &&
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
                verticalArrangement = Arrangement.spacedBy(component.spacing.dp),
                horizontalAlignment = HeimTokenResolver.resolveHorizontalAlignment(component.alignment)
            ) {
                component.children.forEach { child ->
                    if (child is SpacerComponent) {
                        HeimSpacerRenderer(child)      // ColumnScope overload: weight works here
                    } else {
                        HeimRenderer(
                            component = child,
                            stateManager = stateManager,
                            onAction = onAction
                        )
                    }
                }
            }
        }
        Direction.HORIZONTAL -> {
            Row(
                modifier = containerModifier,
                horizontalArrangement = Arrangement.spacedBy(component.spacing.dp),
                verticalAlignment = HeimTokenResolver.resolveVerticalAlignment(component.alignment)
            ) {
                component.children.forEach { child ->
                    if (child is SpacerComponent) {
                        HeimSpacerRenderer(child)      // RowScope overload
                    } else {
                        HeimRenderer(
                            component = child,
                            stateManager = stateManager,
                            onAction = onAction
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
