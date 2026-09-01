package io.heimui.core.domain.model.action

import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.component.HeimComponent

public sealed interface HeimAction {
    /**
     * Business analytics to report when this action runs, opaque to the SDK.
     *
     * Kept untyped on purpose — see the wire format for why the SDK must not know which analytics
     * provider the host uses. Delivered verbatim to [io.heimui.core.presentation.tracking.HeimTrackingDispatcher].
     */
    public val tracking: Map<String, HeimValue>? get() = null

    /**
     * Stable identifier for telemetry.
     *
     * `action::class.simpleName` is obfuscated by R8 in release builds, so action analytics
     * collected in production would be unreadable without this.
     */
    public val telemetryName: String
}

/**
 * Writes [value] into the screen's form state. Purely local: nothing leaves the device.
 *
 * Combine it with another action when the change must also reach the server — the two are
 * separate steps, and the runner executes them in the order the payload lists them.
 */
public data class SetStateAction(
    val key: String,
    val value: HeimValue,
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "set_state"
}

public data class NavigateAction(
    val screenId: String,
    val params: Map<String, String> = emptyMap(),
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "navigate"
}

public data class SubmitFormAction(
    val endpoint: String,
    val method: String = "POST",
    val payload: Map<String, HeimValue>? = null,
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "submit_form"
}

public data class ShowSnackbarAction(
    val message: String,
    val duration: String = "SHORT",
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "show_snackbar"
}

public data class OpenUrlAction(
    val url: String,
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "open_url"
}

public data class CustomAction(
    val name: String,
    val payload: Map<String, HeimValue>? = null,
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "custom"
}

public data class ShowBottomSheetAction(
    val title: String? = null,
    val isDismissible: Boolean = true,
    val content: HeimComponent,
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "show_bottom_sheet"
}

public data class ShowDialogAction(
    val title: String,
    val message: String,
    val confirmText: String = "OK",
    val confirmActions: List<HeimAction> = emptyList(),
    val dismissText: String? = null,
    val dismissActions: List<HeimAction> = emptyList(),
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "show_dialog"
}

public data object DismissModalAction : HeimAction {
    override val telemetryName: String get() = "dismiss_modal"
}

public data object DismissAction : HeimAction {
    override val telemetryName: String get() = "dismiss"
}

public data class UnknownAction(
    val originalType: String? = null,
    override val tracking: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "unknown"
}
