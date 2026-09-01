package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

public @Serializable
data class HeimScreenResponseDto(
    val id: String,
    val version: String = "1.0.0",
    val title: String? = null,
    @SerialName("apply_safe_insets") val applySafeInsets: Boolean = true,
    val root: HeimComponentDto,
    val metadata: JsonObject? = null,
    val signature: String? = null
)
