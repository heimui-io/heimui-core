package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire format of an action a component can dispatch.
 *
 * Discriminated by `type`: `"navigate"`, `"submit_form"`, `"open_url"`, and so on. An unknown
 * type degrades to [UnknownActionDto] so one unrecognised action does not take down the screen
 * that declares it.
 */
@Serializable
public sealed interface HeimActionDto

@Serializable
@SerialName("navigate")
public data class NavigateActionDto(
    @SerialName("screen_id") val screenId: String = "",
    val params: Map<String, String> = emptyMap()
) : HeimActionDto

@Serializable
@SerialName("submit_form")
public data class SubmitFormActionDto(
    val endpoint: String = "",
    val method: String = "POST",
    val payload: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("show_snackbar")
public data class ShowSnackbarActionDto(
    val message: String = "",
    val duration: String = "SHORT"
) : HeimActionDto

@Serializable
@SerialName("open_url")
public data class OpenUrlActionDto(
    val url: String = ""
) : HeimActionDto

@Serializable
@SerialName("custom")
public data class CustomActionDto(
    val name: String = "",
    val payload: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("show_bottom_sheet")
public data class ShowBottomSheetActionDto(
    val title: String? = null,
    @SerialName("is_dismissible") val isDismissible: Boolean = true,
    val content: HeimComponentDto = UnknownComponentDto(id = "missing_content")
) : HeimActionDto

@Serializable
@SerialName("show_dialog")
public data class ShowDialogActionDto(
    val title: String = "",
    val message: String = "",
    @SerialName("confirm_text") val confirmText: String = "OK",
    @SerialName("confirm_actions") val confirmActions: List<HeimActionDto> = emptyList(),
    @SerialName("dismiss_text") val dismissText: String? = null,
    @SerialName("dismiss_actions") val dismissActions: List<HeimActionDto> = emptyList()
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
    val originalType: String? = null
) : HeimActionDto
