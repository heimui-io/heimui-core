package io.heimui.core.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.domain.model.component.UnknownComponent
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.registry.LocalHeimCustomComponentRegistry
import io.heimui.core.presentation.state.HeimStateManager

internal @Composable
fun HeimCustomRenderer(
    component: CustomComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    customRenderer: (@Composable (CustomComponent) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val registryRenderer = LocalHeimCustomComponentRegistry.current.getRenderer(component.name)

    // Registry first: it is keyed by component name, so it is the more specific match. Checking
    // the screen-level lambda first meant one custom renderer swallowed every custom component
    // on the screen and eclipsed the registry entirely.
    if (registryRenderer != null) {
        registryRenderer(component, stateManager, onAction, modifier)
    } else if (customRenderer != null) {
        customRenderer(component)
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heimAccessibility(component.a11y, componentId = component.id)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = "⚡ Custom Component: ${component.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal @Composable
fun HeimUnknownRenderer(
    component: UnknownComponent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id)
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "⚠️ Unknown Component (id: ${component.id})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
