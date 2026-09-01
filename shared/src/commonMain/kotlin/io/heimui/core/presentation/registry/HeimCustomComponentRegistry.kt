package io.heimui.core.presentation.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.presentation.state.HeimStateManager

public typealias CustomComponentRenderer = @Composable (
    component: CustomComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier
) -> Unit

/**
 * Registry allowing host applications to register native Composables for custom SDUI components.
 */
public class HeimCustomComponentRegistry {
    private val renderers = mutableMapOf<String, CustomComponentRenderer>()

    public fun register(name: String, renderer: CustomComponentRenderer): HeimCustomComponentRegistry {
        renderers[name] = renderer
        return this
    }

    public fun getRenderer(name: String): CustomComponentRenderer? {
        return renderers[name]
    }

    public companion object {
        public fun build(builder: HeimCustomComponentRegistry.() -> Unit): HeimCustomComponentRegistry {
            return HeimCustomComponentRegistry().apply(builder)
        }
    }
}

public val LocalHeimCustomComponentRegistry: ProvidableCompositionLocal<HeimCustomComponentRegistry> =
    staticCompositionLocalOf {
    HeimCustomComponentRegistry()
}
