package io.heimui.core

import io.heimui.core.domain.evaluator.HeimValidationEngine
import io.heimui.core.domain.evaluator.HeimValidatorRegistry
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeimValidatorRegistryTest {

    @Test
    fun `test custom registered validator passes and fails accurately`() {
        val registry = HeimValidatorRegistry().apply {
            register("IBAN") { value, _ ->
                value.startsWith("ES") && value.length == 24
            }
        }

        val rules = listOf(
            ValidationRule(
                type = ValidationType.CUSTOM,
                value = "IBAN",
                errorMessage = "Invalid IBAN number"
            )
        )

        // Valid IBAN
        val validResult = HeimValidationEngine.validate("ES1234567890123456789012", rules, registry)
        assertNull(validResult)

        // Invalid IBAN
        val invalidResult = HeimValidationEngine.validate("FR123", rules, registry)
        assertEquals("Invalid IBAN number", invalidResult)
    }
}
