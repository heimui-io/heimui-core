package io.heimui.core

import io.heimui.core.presentation.state.HeimStateManager
import kotlin.test.Test
import kotlin.test.assertEquals

class HeimStateManagerTest {

    @Test
    fun testUpdateAndRetrieveState() {
        val stateManager = HeimStateManager(screenId = "test_screen")
        stateManager.updateValue("username", "julian")
        stateManager.updateValue("role", "admin")

        assertEquals("julian", stateManager.getValue("username"))
        assertEquals("admin", stateManager.getValue("role"))
        assertEquals("", stateManager.getValue("non_existing"))
    }

    @Test
    fun testInterpolatePayload() {
        val stateManager = HeimStateManager(screenId = "checkout_screen")
        stateManager.updateValue("coupon_code", "SUMMER2026")
        stateManager.updateValue("user_id", "usr_9981")

        val rawPayload = mapOf(
            "coupon" to "{{state.coupon_code}}",
            "userId" to "{{state.user_id}}",
            "staticField" to "fixed_value"
        )

        val interpolated = stateManager.interpolatePayload(rawPayload)
        assertEquals("SUMMER2026", interpolated?.get("coupon"))
        assertEquals("usr_9981", interpolated?.get("userId"))
        assertEquals("fixed_value", interpolated?.get("staticField"))
    }
}
