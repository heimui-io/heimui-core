package io.heimui.core.presentation.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.presentation.state.HeimStateManager

typealias CustomComponentRenderer = @Composable (
    component: CustomComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier
) -> Unit

/**
 * Registry allowing host applications to register native Composables for custom SDUI components.
 */
class HeimCustomComponentRegistry {
    private val renderers = mutableMapOf<String, CustomComponentRenderer>()

    fun register(name: String, renderer: CustomComponentRenderer): HeimCustomComponentRegistry {
        renderers[name] = renderer
        return this
    }

    fun getRenderer(name: String): CustomComponentRenderer? {
        return renderers[name]
    }

    companion object {
        fun build(builder: HeimCustomComponentRegistry.() -> Unit): HeimCustomComponentRegistry {
            return HeimCustomComponentRegistry().apply(builder)
        }
    }
}

val LocalHeimCustomComponentRegistry = staticCompositionLocalOf<HeimCustomComponentRegistry> {
    HeimCustomComponentRegistry()
}
