package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidationRuleDto(
    val type: ValidationTypeDto,
    val value: String? = null,
    @SerialName("error_message") val errorMessage: String
)

@Serializable
enum class ValidationTypeDto {
    @SerialName("REQUIRED") REQUIRED,
    @SerialName("REGEX") REGEX,
    @SerialName("MIN_LENGTH") MIN_LENGTH,
    @SerialName("MAX_LENGTH") MAX_LENGTH,
    @SerialName("EMAIL") EMAIL,
    @SerialName("NUMERIC") NUMERIC
}
