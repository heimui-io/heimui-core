package io.heimui.core

import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import io.heimui.core.presentation.state.HeimStateManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Several radios share one `state_key`, and each registers and unregisters with the composition.
 * That is new: every other component owns its key alone, so an unguarded unregister could only
 * ever remove what the same component had put there.
 */
class HeimRadioFieldTest {

    private val required = listOf(ValidationRule(ValidationType.REQUIRED, errorMessage = "Pick one"))

    @Test
    fun `a field with no rules registers nothing`() {
        val manager = HeimStateManager(screenId = "test")
        manager.registerField("doc", emptyList())

        // Nothing to validate, so nothing is reported as invalid.
        assertEquals(emptyMap(), manager.validateForm())
    }

    /**
     * The shape a radio and a radio group sharing a key produce: the group declares the rule, a
     * standalone radio declares none, and the radio scrolling out of a lazy list must not take
     * the group's rule with it.
     */
    @Test
    fun `unregistering a key that carried rules leaves them alone when nothing was registered`() {
        val manager = HeimStateManager(screenId = "test")
        manager.registerField("doc", required)
        manager.registerField("doc", emptyList())

        assertEquals(
            1,
            manager.validateForm().size,
            "an empty registration must not displace the rule already there",
        )
    }

    @Test
    fun `a real unregister still removes what it registered`() {
        val manager = HeimStateManager(screenId = "test")
        manager.registerField("doc", required)
        manager.unregisterField("doc")

        assertEquals(emptyMap(), manager.validateForm())
    }
}
