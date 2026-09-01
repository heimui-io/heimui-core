package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Wire format of an action a component can dispatch.
 *
 * Discriminated by `type`: `"navigate"`, `"submit_form"`, `"open_url"`, and so on. An unknown
 * type degrades to [UnknownActionDto] so one unrecognised action does not take down the screen
 * that declares it.
 */
@Serializable
public sealed interface HeimActionDto {
    /**
     * Business analytics to report when this action runs, opaque to the SDK.
     *
     * Deliberately untyped: HeimUI must never know what Amplitude or Firebase are, or every new
     * provider becomes a new SDK release. The map travels verbatim to the host's
     * [io.heimui.core.presentation.tracking.HeimTrackingDispatcher], which decides where it goes.
     *
     * The point is who owns the names. Analytics teams rename events every sprint and are rarely
     * the mobile team; with the names in the payload that is a backend deploy instead of an app
     * release, which is the whole promise of server-driven UI applied to measurement.
     */
    public val tracking: JsonObject? get() = null
}

@Serializable
@SerialName("navigate")
public data class NavigateActionDto(
    @SerialName("screen_id") val screenId: String = "",
    val params: Map<String, String> = emptyMap(),
    override val tracking: JsonObject? = null
) : HeimActionDto

/**
 * Writes a value into the screen's form state without touching the network.
 *
 * This is what lets anything that is not an input drive `visible_if` — selecting a plan card,
 * switching a tab, expanding a section. Before it, state only changed when the user typed in a
 * field or moved a switch, so every other interaction needed a server round trip.
 */
@Serializable
@SerialName("set_state")
public data class SetStateActionDto(
    val key: String = "",
    val value: JsonElement? = null,
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("submit_form")
public data class SubmitFormActionDto(
    val endpoint: String = "",
    val method: String = "POST",
    val payload: JsonObject? = null,
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("show_snackbar")
public data class ShowSnackbarActionDto(
    val message: String = "",
    val duration: String = "SHORT",
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("open_url")
public data class OpenUrlActionDto(
    val url: String = "",
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("custom")
public data class CustomActionDto(
    val name: String = "",
    val payload: JsonObject? = null,
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("show_bottom_sheet")
public data class ShowBottomSheetActionDto(
    val title: String? = null,
    @SerialName("is_dismissible") val isDismissible: Boolean = true,
    val content: HeimComponentDto = UnknownComponentDto(id = "missing_content"),
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("show_dialog")
public data class ShowDialogActionDto(
    val title: String = "",
    val message: String = "",
    @SerialName("confirm_text") val confirmText: String = "OK",
    @SerialName("confirm_actions") val confirmActions: List<HeimActionDto> = emptyList(),
    @SerialName("dismiss_text") val dismissText: String? = null,
    @SerialName("dismiss_actions") val dismissActions: List<HeimActionDto> = emptyList(),
    override val tracking: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("dismiss_modal")
public data object DismissModalActionDto : HeimActionDto

@Serializable
@SerialName("dismiss")
public data object DismissActionDto : HeimActionDto

@Serializable
@SerialName("unknown")
public data class UnknownActionDto(
    val originalType: String? = null,
    override val tracking: JsonObject? = null
) : HeimActionDto
