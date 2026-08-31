package io.heimui.core

import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.ShowSnackbarAction
import io.heimui.core.presentation.action.HeimActionDispatcher
import io.heimui.core.presentation.action.HeimActionInterceptor
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeimActionDispatcherTest {

    @Test
    fun `test action dispatcher executes interceptor chain`() = runTest {
        val executionLog = mutableListOf<String>()
        val stateManager = HeimStateManager(screenId = "test_screen")

        val loggingInterceptor = object : HeimActionInterceptor {
            override suspend fun intercept(
                action: HeimAction,
                stateManager: HeimStateManager,
                next: suspend (HeimAction) -> Unit
            ) {
                executionLog.add("logged: ${(action as ShowSnackbarAction).message}")
                next(action)
            }
        }

        val modifyingInterceptor = object : HeimActionInterceptor {
            override suspend fun intercept(
                action: HeimAction,
                stateManager: HeimStateManager,
                next: suspend (HeimAction) -> Unit
            ) {
                val snackbar = action as ShowSnackbarAction
                next(snackbar.copy(message = "${snackbar.message} [modified]"))
            }
        }

        val dispatcher = HeimActionDispatcher.build {
            addInterceptor(loggingInterceptor)
            addInterceptor(modifyingInterceptor)
        }

        var finalReceivedAction: HeimAction? = null
        dispatcher.dispatch(ShowSnackbarAction("Hello World"), stateManager) { action ->
            finalReceivedAction = action
        }

        assertEquals("logged: Hello World", executionLog.first())
        assertTrue(finalReceivedAction is ShowSnackbarAction)
        assertEquals("Hello World [modified]", (finalReceivedAction as ShowSnackbarAction).message)
    }
}
