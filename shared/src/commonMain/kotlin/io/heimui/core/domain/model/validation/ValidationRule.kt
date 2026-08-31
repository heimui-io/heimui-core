package io.heimui.core.domain.model.validation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidationRule(
    val type: ValidationType,
    val value: String? = null,
    val errorMessage: String
)

@Serializable
enum class ValidationType {
    @SerialName("REQUIRED") REQUIRED,
    @SerialName("REGEX") REGEX,
    @SerialName("MIN_LENGTH") MIN_LENGTH,
    @SerialName("MAX_LENGTH") MAX_LENGTH,
    @SerialName("EMAIL") EMAIL,
    @SerialName("NUMERIC") NUMERIC
}
