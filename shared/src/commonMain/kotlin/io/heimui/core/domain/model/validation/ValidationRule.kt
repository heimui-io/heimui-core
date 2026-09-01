package io.heimui.core.domain.model.validation

public data class ValidationRule(
    val type: ValidationType,
    val value: String? = null,
    val errorMessage: String
)

public enum class ValidationType {
    REQUIRED,
    REGEX,
    MIN_LENGTH,
    MAX_LENGTH,
    EMAIL,
    NUMERIC,
    CUSTOM
}
