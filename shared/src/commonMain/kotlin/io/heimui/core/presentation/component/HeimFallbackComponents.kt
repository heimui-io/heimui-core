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
import io.heimui.core.presentation.designsystem.LocalHeimShowDiagnostics
import io.heimui.core.presentation.state.HeimStateManager

@Composable
internal fun HeimCustomRenderer(
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
    } else if (LocalHeimShowDiagnostics.current) {
        // Same rule as an unknown type: the placeholder is a developer's tool, not a user's. A
        // custom component nobody registered is a wiring mistake in the app, and the app's
        // developers are who should see it.
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
                text = "Unregistered custom component: ${component.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Draws a component this client does not know.
 *
 * **Nothing, by default.** A user seeing `⚠️ Unknown Component (id: c_all)` learns nothing from
 * it, reads the screen as broken, and is shown an internal id that is none of their business. The
 * whole reason unknown types degrade instead of throwing is so the rest of the screen still
 * works — and a red box in the middle of it defeats that.
 *
 * Silence is not the same as ignoring it, though. Every unknown type is reported as a
 * `PayloadViolation`, so the team that shipped it finds out from telemetry rather than from a
 * screenshot. Set `showDiagnostics` on `HeimTheme` to put the box back while developing, which is
 * the one context where seeing it is what you want.
 */
@Composable
internal fun HeimUnknownRenderer(
    component: UnknownComponent,
    modifier: Modifier = Modifier
) {
    if (!LocalHeimShowDiagnostics.current) return

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
            text = "Unknown component '${component.originalType}' (id: ${component.id})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
