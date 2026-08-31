package io.heimui.core.domain.evaluator

import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType

object HeimValidationEngine {

    fun validate(value: String, rules: List<ValidationRule>): String? {
        for (rule in rules) {
            val error = validateRule(value, rule)
            if (error != null) {
                return error
            }
        }
        return null
    }

    private fun validateRule(value: String, rule: ValidationRule): String? {
        return when (rule.type) {
            ValidationType.REQUIRED -> {
                if (value.trim().isEmpty()) rule.errorMessage else null
            }
            ValidationType.REGEX -> {
                val pattern = rule.value ?: return null
                if (!Regex(pattern).matches(value)) rule.errorMessage else null
            }
            ValidationType.MIN_LENGTH -> {
                val min = rule.value?.toIntOrNull() ?: 0
                if (value.length < min) rule.errorMessage else null
            }
            ValidationType.MAX_LENGTH -> {
                val max = rule.value?.toIntOrNull() ?: Int.MAX_VALUE
                if (value.length > max) rule.errorMessage else null
            }
            ValidationType.EMAIL -> {
                val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                if (!emailRegex.matches(value.trim())) rule.errorMessage else null
            }
            ValidationType.NUMERIC -> {
                if (value.isNotEmpty() && value.toDoubleOrNull() == null) rule.errorMessage else null
            }
        }
    }
}
