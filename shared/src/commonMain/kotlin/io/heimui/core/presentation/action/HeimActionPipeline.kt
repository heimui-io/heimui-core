package io.heimui.core.presentation.action

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.presentation.state.HeimStateManager

/**
 * Interceptor for HeimUI actions. Allows logging, analytics, auth checks, or action transformation.
 */
public interface HeimActionInterceptor {
    public suspend fun intercept(
        action: HeimAction,
        stateManager: HeimStateManager,
        next: suspend (HeimAction) -> Unit
    )
}

/**
 * Pipeline dispatcher that executes registered interceptors in chain of responsibility.
 */
public class HeimActionDispatcher(
    private val interceptors: List<HeimActionInterceptor> = emptyList(),
    private val defaultHandler: ((HeimAction) -> Unit)? = null
) {
    public suspend fun dispatch(
        action: HeimAction,
        stateManager: HeimStateManager,
        finalHandler: (HeimAction) -> Unit
    ) {
        suspend fun executeChain(index: Int, currentAction: HeimAction) {
            if (index < interceptors.size) {
                interceptors[index].intercept(currentAction, stateManager) { nextAction ->
                    executeChain(index + 1, nextAction)
                }
            } else {
                defaultHandler?.invoke(currentAction)
                finalHandler(currentAction)
            }
        }
        executeChain(0, action)
    }

    public companion object {
        public fun build(builder: Builder.() -> Unit): HeimActionDispatcher {
            return Builder().apply(builder).build()
        }
    }

    public class Builder {
        private val interceptors = mutableListOf<HeimActionInterceptor>()
        private var defaultHandler: ((HeimAction) -> Unit)? = null

        public fun addInterceptor(interceptor: HeimActionInterceptor): Builder = apply {
            interceptors.add(interceptor)
        }

        public fun setDefaultHandler(handler: (HeimAction) -> Unit): Builder = apply {
            this.defaultHandler = handler
        }

        public fun build(): HeimActionDispatcher = HeimActionDispatcher(interceptors.toList(), defaultHandler)
    }
}

public val LocalHeimActionDispatcher: ProvidableCompositionLocal<HeimActionDispatcher> =
    staticCompositionLocalOf {
    HeimActionDispatcher()
}
