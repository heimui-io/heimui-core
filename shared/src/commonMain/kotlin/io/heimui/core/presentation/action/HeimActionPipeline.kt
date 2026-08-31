package io.heimui.core.presentation.action

import androidx.compose.runtime.staticCompositionLocalOf
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.presentation.state.HeimStateManager

/**
 * Interceptor for HeimUI actions. Allows logging, analytics, auth checks, or action transformation.
 */
interface HeimActionInterceptor {
    suspend fun intercept(
        action: HeimAction,
        stateManager: HeimStateManager,
        next: suspend (HeimAction) -> Unit
    )
}

/**
 * Pipeline dispatcher that executes registered interceptors in chain of responsibility.
 */
class HeimActionDispatcher(
    private val interceptors: List<HeimActionInterceptor> = emptyList(),
    private val defaultHandler: ((HeimAction) -> Unit)? = null
) {
    suspend fun dispatch(
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

    companion object {
        fun build(builder: Builder.() -> Unit): HeimActionDispatcher {
            return Builder().apply(builder).build()
        }
    }

    class Builder {
        private val interceptors = mutableListOf<HeimActionInterceptor>()
        private var defaultHandler: ((HeimAction) -> Unit)? = null

        fun addInterceptor(interceptor: HeimActionInterceptor) = apply {
            interceptors.add(interceptor)
        }

        fun setDefaultHandler(handler: (HeimAction) -> Unit) = apply {
            this.defaultHandler = handler
        }

        fun build(): HeimActionDispatcher = HeimActionDispatcher(interceptors.toList(), defaultHandler)
    }
}

val LocalHeimActionDispatcher = staticCompositionLocalOf<HeimActionDispatcher> {
    HeimActionDispatcher()
}
