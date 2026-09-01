package io.heimui.core.presentation.state

import io.heimui.core.domain.model.HeimScreenResponse

/**
 * Render state for a screen.
 *
 * `Error` deliberately carries no `onRetry` lambda. A function inside a state class makes
 * `equals` identity-based, so every new Error instance compares unequal and forces recomposition,
 * and the closure pins the composition scope it captured. The retry entry point lives on
 * [HeimScreenController] instead.
 */
public sealed interface HeimScreenState {
    public data object Loading : HeimScreenState
    public data class Content(val screen: HeimScreenResponse, val isStale: Boolean = false) : HeimScreenState
    public data class Error(val message: String, val throwable: Throwable? = null) : HeimScreenState
    public data class Empty(val message: String = "No content available") : HeimScreenState
}
