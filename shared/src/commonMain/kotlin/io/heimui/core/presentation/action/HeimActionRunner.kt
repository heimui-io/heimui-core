package io.heimui.core.presentation.action

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.heimui.core.domain.model.action.HeimAction

/**
 * Runs the actions a component declares, **in order**, one after the previous finished.
 *
 * This exists because a list is not a set. A payload that says
 *
 * ```json
 * "actions": [ { "type": "submit_form", … }, { "type": "navigate", "screen_id": "success" } ]
 * ```
 *
 * means "submit, then go to success" — and before this ran, every action was dispatched on its own
 * coroutine, so the navigation fired immediately and fired *whether or not the submission
 * succeeded*. The user was shown a confirmation screen for something that had failed.
 *
 * A failing action stops the ones after it. That is the only reading of an ordered list that is
 * safe by default: continuing past a failure is occasionally what you want and catastrophic when
 * it is not, so it should have to be asked for rather than assumed.
 *
 * Components take this from the composition rather than receiving it, so the sequencing rule lives
 * in one place instead of being re-implemented by every renderer that has a list of actions.
 */
public fun interface HeimActionRunner {
    public fun run(actions: List<HeimAction>)
}

internal val LocalHeimActionRunner: ProvidableCompositionLocal<HeimActionRunner> =
    staticCompositionLocalOf { HeimActionRunner { } }

/**
 * The sequencing rule itself, separated from the composable that wires it.
 *
 * Extracted so the contract can be tested without a Compose harness: "in order, stop on failure"
 * is the kind of rule that is easy to state, easy to get subtly wrong, and impossible to notice
 * from a screenshot.
 *
 * @param dispatch runs the interceptor chain and hands back the action that survived it, or hands
 *   back nothing when an interceptor swallowed it.
 * @param execute performs one action and answers whether the sequence may continue.
 */
internal suspend fun runHeimActionSequence(
    actions: List<HeimAction>,
    dispatch: suspend (HeimAction, (HeimAction) -> Unit) -> Unit,
    execute: suspend (HeimAction) -> Boolean,
) {
    for (action in actions) {
        var resolved: HeimAction? = null
        dispatch(action) { resolved = it }

        // An interceptor that swallowed the action stops the sequence too: it decided this step
        // must not happen, and the steps after it were written assuming it did.
        val next = resolved ?: return
        if (!execute(next)) return
    }
}
