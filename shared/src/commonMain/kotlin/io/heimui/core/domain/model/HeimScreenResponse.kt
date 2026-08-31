package io.heimui.core.domain.model

import io.heimui.core.domain.model.component.HeimComponent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HeimScreenResponse(
    val id: String,
    val version: String = "1.0.0",
    val title: String? = null,
    @SerialName("apply_safe_insets") val applySafeInsets: Boolean = true,
    val root: HeimComponent,
    val metadata: JsonObject? = null,
    val signature: String? = null
)
