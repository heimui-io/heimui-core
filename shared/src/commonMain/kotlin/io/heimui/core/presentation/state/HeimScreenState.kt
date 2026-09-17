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
    /**
     * A screen is on display. [statusCode] is what the server answered with -- 200 for the screen
     * that was asked for, or the 4xx of a screen it sent instead. It is here because a host cannot
     * infer "the session expired" by reading a payload, and a 401 usually means it should navigate
     * somewhere the payload knows nothing about.
     */
    public data class Content(
        val screen: HeimScreenResponse,
        val isStale: Boolean = false,
        val statusCode: Int = 200
    ) : HeimScreenState
    public data class Error(val message: String, val throwable: Throwable? = null) : HeimScreenState
    public data class Empty(val message: String = "No content available") : HeimScreenState
}
