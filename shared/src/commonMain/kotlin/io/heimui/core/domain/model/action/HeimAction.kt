package io.heimui.core.domain.model.action

import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.component.HeimComponent

public sealed interface HeimAction {
    /**
     * Stable identifier for telemetry.
     *
     * `action::class.simpleName` is obfuscated by R8 in release builds, so action analytics
     * collected in production would be unreadable without this.
     */
    public val telemetryName: String
}

public data class NavigateAction(
    val screenId: String,
    val params: Map<String, String> = emptyMap()
) : HeimAction {
    override val telemetryName: String get() = "navigate"
}

public data class SubmitFormAction(
    val endpoint: String,
    val method: String = "POST",
    val payload: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "submit_form"
}

public data class ShowSnackbarAction(
    val message: String,
    val duration: String = "SHORT"
) : HeimAction {
    override val telemetryName: String get() = "show_snackbar"
}

public data class OpenUrlAction(
    val url: String
) : HeimAction {
    override val telemetryName: String get() = "open_url"
}

public data class CustomAction(
    val name: String,
    val payload: Map<String, HeimValue>? = null
) : HeimAction {
    override val telemetryName: String get() = "custom"
}

public data class ShowBottomSheetAction(
    val title: String? = null,
    val isDismissible: Boolean = true,
    val content: HeimComponent
) : HeimAction {
    override val telemetryName: String get() = "show_bottom_sheet"
}

public data class ShowDialogAction(
    val title: String,
    val message: String,
    val confirmText: String = "OK",
    val confirmActions: List<HeimAction> = emptyList(),
    val dismissText: String? = null,
    val dismissActions: List<HeimAction> = emptyList()
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
    val originalType: String? = null
) : HeimAction {
    override val telemetryName: String get() = "unknown"
}
