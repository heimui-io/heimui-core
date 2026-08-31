package io.heimui.core.domain.model.action

sealed interface HeimAction

data class NavigateAction(
    val screenId: String,
    val params: Map<String, String> = emptyMap()
) : HeimAction

data class SubmitFormAction(
    val endpoint: String,
    val method: String = "POST",
    val payload: Map<String, Any?>? = null
) : HeimAction

data class ShowSnackbarAction(
    val message: String,
    val duration: String = "SHORT"
) : HeimAction

data class OpenUrlAction(
    val url: String
) : HeimAction

data class CustomAction(
    val name: String,
    val payload: Map<String, Any?>? = null
) : HeimAction

data object DismissAction : HeimAction
