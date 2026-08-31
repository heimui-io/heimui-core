package io.heimui.core.domain.model.validation

data class ValidationRule(
    val type: ValidationType,
    val value: String? = null,
    val errorMessage: String
)

enum class ValidationType {
    REQUIRED,
    REGEX,
    MIN_LENGTH,
    MAX_LENGTH,
    EMAIL,
    NUMERIC,
    CUSTOM
}
