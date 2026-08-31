package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface HeimActionDto

@Serializable
@SerialName("navigate")
data class NavigateActionDto(
    @SerialName("screen_id") val screenId: String,
    val params: Map<String, String> = emptyMap()
) : HeimActionDto

@Serializable
@SerialName("submit_form")
data class SubmitFormActionDto(
    val endpoint: String,
    val method: String = "POST",
    val payload: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("show_snackbar")
data class ShowSnackbarActionDto(
    val message: String,
    val duration: String = "SHORT"
) : HeimActionDto

@Serializable
@SerialName("open_url")
data class OpenUrlActionDto(
    val url: String
) : HeimActionDto

@Serializable
@SerialName("custom")
data class CustomActionDto(
    val name: String,
    val payload: JsonObject? = null
) : HeimActionDto

@Serializable
@SerialName("dismiss")
data object DismissActionDto : HeimActionDto
