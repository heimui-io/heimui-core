package io.heimui.core.presentation.telemetry

import androidx.compose.runtime.staticCompositionLocalOf

fun interface HeimTelemetryObserver {
    fun onEvent(event: HeimTelemetryEvent)
}

sealed interface HeimTelemetryEvent {
    data class ScreenViewed(val screenId: String, val isStale: Boolean) : HeimTelemetryEvent
    data class TimeToRender(val screenId: String, val durationMs: Long) : HeimTelemetryEvent
    data class ActionExecuted(val actionType: String, val params: Map<String, String> = emptyMap()) : HeimTelemetryEvent
    data class FormSubmitted(val endpoint: String, val success: Boolean, val durationMs: Long) : HeimTelemetryEvent
    data class ScreenError(val screenId: String, val errorMessage: String, val throwable: Throwable? = null) : HeimTelemetryEvent
}

object NoOpHeimTelemetryObserver : HeimTelemetryObserver {
    override fun onEvent(event: HeimTelemetryEvent) {
        // No-op by default
    }
}

val LocalHeimTelemetryObserver = staticCompositionLocalOf<HeimTelemetryObserver> {
    NoOpHeimTelemetryObserver
}
