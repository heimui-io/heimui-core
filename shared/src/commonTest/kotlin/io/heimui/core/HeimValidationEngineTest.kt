package io.heimui.core

import io.heimui.core.domain.evaluator.HeimValidationEngine
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeimValidationEngineTest {

    @Test
    fun testRequiredRule() {
        val rules = listOf(
            ValidationRule(type = ValidationType.REQUIRED, errorMessage = "Field is required")
        )
        assertEquals("Field is required", HeimValidationEngine.validate("", rules))
        assertEquals("Field is required", HeimValidationEngine.validate("   ", rules))
        assertNull(HeimValidationEngine.validate("Hello", rules))
    }

    @Test
    fun testEmailRule() {
        val rules = listOf(
            ValidationRule(type = ValidationType.EMAIL, errorMessage = "Invalid email format")
        )
        assertEquals("Invalid email format", HeimValidationEngine.validate("invalid-email", rules))
        assertNull(HeimValidationEngine.validate("user@heimui.io", rules))
    }

    @Test
    fun testMinMaxLengthRule() {
        val minRule = listOf(ValidationRule(type = ValidationType.MIN_LENGTH, value = "5", errorMessage = "Too short"))
        val maxRule = listOf(ValidationRule(type = ValidationType.MAX_LENGTH, value = "10", errorMessage = "Too long"))

        assertEquals("Too short", HeimValidationEngine.validate("abc", minRule))
        assertNull(HeimValidationEngine.validate("abcde", minRule))

        assertEquals("Too long", HeimValidationEngine.validate("12345678901", maxRule))
        assertNull(HeimValidationEngine.validate("1234567890", maxRule))
    }
}
