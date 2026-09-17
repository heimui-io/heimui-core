package io.heimui.core.presentation.telemetry

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public fun interface HeimTelemetryObserver {
    public fun onEvent(event: HeimTelemetryEvent)
}

public sealed interface HeimTelemetryEvent {
    public data class ScreenViewed(val screenId: String, val isStale: Boolean) : HeimTelemetryEvent

    public data class TimeToRender(val screenId: String, val durationMs: Long) : HeimTelemetryEvent

    public data class ActionExecuted(
        val screenId: String,
        val actionType: String,
        val params: Map<String, String> = emptyMap()
    ) : HeimTelemetryEvent

    public data class FormSubmitted(val endpoint: String, val success: Boolean, val durationMs: Long) : HeimTelemetryEvent

    public data class ScreenError(
        val screenId: String,
        val errorMessage: String,
        val throwable: Throwable? = null
    ) : HeimTelemetryEvent

    /**
     * Revalidation failed but cached content is still on screen. Distinct from [ScreenError]:
     * the user is not blocked, so this should not page anyone, but it is the signal that the
     * backend or the network is degraded.
     */
    public data class ScreenRefreshFailed(val screenId: String, val reason: String) : HeimTelemetryEvent

    /**
     * The server sent a payload the client had to repair (negative padding, duplicate ids,
     * excessive depth, unknown component type). This is how a backend team finds out it is
     * emitting invalid SDUI before users report a broken screen.
     */
    public data class PayloadViolation(val screenId: String, val violations: List<String>) : HeimTelemetryEvent

    /**
     * The server answered a screen request with a 4xx and a screen of its own, which is now on
     * display. Not an error for the reader -- they are looking at what the server meant them to
     * see -- but it is the signal a host acts on: a 401 is a dead session, a 403 a permission that
     * changed underneath someone.
     */
    public data class ScreenRefused(val screenId: String, val statusCode: Int) : HeimTelemetryEvent

    /** A CUSTOM validation rule referenced a validator the host never registered. */
    public data class ValidatorMissing(val validatorName: String) : HeimTelemetryEvent

    /** The payload referenced an icon name the provider cannot draw. */
    public data class IconMissing(val iconName: String) : HeimTelemetryEvent

    /** An OpenUrlAction was refused by the URL scheme policy. */
    public data class UrlBlocked(val url: String, val reason: String) : HeimTelemetryEvent

    /** A form submission was refused locally (cross-origin endpoint, disallowed method). */
    public data class SubmissionBlocked(val endpoint: String, val reason: String) : HeimTelemetryEvent
}

public object NoOpHeimTelemetryObserver : HeimTelemetryObserver {
    override fun onEvent(event: HeimTelemetryEvent) {
        // No-op by default
    }
}

public val LocalHeimTelemetryObserver: ProvidableCompositionLocal<HeimTelemetryObserver> =
    staticCompositionLocalOf {
    NoOpHeimTelemetryObserver
}
