package io.heimui.core

import io.heimui.core.domain.model.action.DismissAction
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.NavigateAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.presentation.action.runHeimActionSequence
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The rule a payload relies on when it writes more than one action.
 *
 * Before this existed every action was dispatched on its own coroutine, so
 * `[submit_form, navigate]` navigated immediately and navigated whether or not the submission had
 * succeeded — the user was shown a confirmation for something that had failed.
 */
class HeimActionSequenceTest {

    private val submit = SubmitFormAction(endpoint = "https://api.example/kyc")
    private val navigate = NavigateAction(screenId = "success")

    private suspend fun run(
        actions: List<HeimAction>,
        swallow: Set<HeimAction> = emptySet(),
        failing: Set<HeimAction> = emptySet(),
    ): List<String> {
        val executed = mutableListOf<String>()
        runHeimActionSequence(
            actions = actions,
            dispatch = { action, onResolved -> if (action !in swallow) onResolved(action) },
            execute = { action ->
                executed += action.telemetryName
                action !in failing
            },
        )
        return executed
    }

    @Test
    fun `actions run in the order the payload declared them`() = runTest {
        assertEquals(
            listOf("submit_form", "navigate", "dismiss"),
            run(listOf(submit, navigate, DismissAction))
        )
    }

    @Test
    fun `a failed action stops the ones after it`() = runTest {
        // The case this whole mechanism exists for: no navigation to a success screen for a
        // submission that failed.
        assertEquals(
            listOf("submit_form"),
            run(listOf(submit, navigate), failing = setOf(submit))
        )
    }

    @Test
    fun `an action an interceptor swallowed stops the sequence too`() = runTest {
        // The interceptor decided this step must not happen, and the steps after it were written
        // assuming it did. Continuing would be the same bug wearing a different hat.
        assertEquals(
            emptyList(),
            run(listOf(submit, navigate), swallow = setOf(submit))
        )
    }

    @Test
    fun `an empty list is not an error`() = runTest {
        assertEquals(emptyList(), run(emptyList()))
    }
}
