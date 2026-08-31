package io.heimui.core.domain.model.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface HeimAction

@Serializable
@SerialName("navigate")
data class NavigateAction(
    val screenId: String,
    val params: Map<String, String> = emptyMap()
) : HeimAction

@Serializable
@SerialName("submit_form")
data class SubmitFormAction(
    val endpoint: String,
    val method: String = "POST",
    val payload: JsonObject? = null
) : HeimAction

@Serializable
@SerialName("show_snackbar")
data class ShowSnackbarAction(
    val message: String,
    val duration: String = "SHORT"
) : HeimAction

@Serializable
@SerialName("open_url")
data class OpenUrlAction(
    val url: String
) : HeimAction

@Serializable
@SerialName("custom")
data class CustomAction(
    val name: String,
    val payload: JsonObject? = null
) : HeimAction

@Serializable
@SerialName("dismiss")
data object DismissAction : HeimAction
