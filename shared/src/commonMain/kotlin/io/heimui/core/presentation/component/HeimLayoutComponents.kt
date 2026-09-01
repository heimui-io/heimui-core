package io.heimui.core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.heimui.core.presentation.state.HeimStateManager

@Composable
fun HeimContainerRenderer(
    component: ContainerComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = HeimTokenResolver.resolveColor(component.backgroundColor, colorScheme, Color.Transparent)

    val containerModifier = modifier
        .fillMaxWidth()
        .background(bgColor)
        .padding(component.padding.dp)
        .heimAccessibility(component.a11y)

    when (component.direction) {
        Direction.VERTICAL -> {
            Column(
                modifier = containerModifier,
                verticalArrangement = Arrangement.spacedBy(component.spacing.dp),
                horizontalAlignment = HeimTokenResolver.resolveHorizontalAlignment(component.alignment)
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
        Direction.HORIZONTAL -> {
            Row(
                modifier = containerModifier,
                horizontalArrangement = Arrangement.spacedBy(component.spacing.dp),
                verticalAlignment = HeimTokenResolver.resolveVerticalAlignment(component.alignment)
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
    }
}

@Composable
fun HeimBoxRenderer(
    component: BoxComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y),
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

@Composable
fun HeimSpacerRenderer(
    component: SpacerComponent,
    modifier: Modifier = Modifier
) {
    if (component.isFlexible) {
        Spacer(modifier = modifier.fillMaxWidth())
    } else {
        Spacer(modifier = modifier.size(component.size.dp))
    }
}

@Composable
fun HeimDividerRenderer(
    component: DividerComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val dividerColor = HeimTokenResolver.resolveColor(
        component.color,
        colorScheme,
        DividerDefaults.color
    )

    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y),
        thickness = component.thickness.dp,
        color = dividerColor
    )
}
