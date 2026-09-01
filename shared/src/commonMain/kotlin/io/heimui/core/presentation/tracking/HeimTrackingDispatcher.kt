package io.heimui.core.presentation.tracking

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.action.HeimAction

/**
 * Receives the business analytics a payload attached to an action.
 *
 * Separate from `HeimTelemetryObserver`, and the distinction matters. Telemetry is the SDK
 * reporting on itself — a screen rendered, a payload was repaired, a submission was blocked — and
 * its vocabulary is fixed by the SDK. Tracking is the *product* reporting on the user, and its
 * vocabulary belongs to whoever writes the payloads.
 *
 * The map is opaque here on purpose. HeimUI must never know what Amplitude, Firebase or Segment
 * are, because then every provider is an SDK release. It carries the names the server chose and
 * hands them over; the host decides where they go and how they are shaped.
 *
 * ```kotlin
 * HeimTheme(
 *     trackingDispatcher = { action, payload ->
 *         analytics.log(
 *             name = payload["event"]?.asString ?: return@HeimTheme,
 *             params = payload - "event",
 *         )
 *     },
 * ) { … }
 * ```
 *
 * Called on the main thread as part of handling the action, so an implementation that does real
 * work should hand it off rather than block the tap.
 */
public fun interface HeimTrackingDispatcher {
    public fun track(action: HeimAction, payload: Map<String, HeimValue>)
}

/** Drops everything. The default, so an app that has not wired analytics costs nothing. */
public object NoOpHeimTrackingDispatcher : HeimTrackingDispatcher {
    override fun track(action: HeimAction, payload: Map<String, HeimValue>): Unit = Unit
}

public val LocalHeimTrackingDispatcher: ProvidableCompositionLocal<HeimTrackingDispatcher> =
    staticCompositionLocalOf { NoOpHeimTrackingDispatcher }
