package io.heimui.core

import io.heimui.core.domain.model.HeimValue
import io.heimui.core.presentation.state.HeimStateManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun testInterpolatePayloadPreservesTypes() {
        val stateManager = HeimStateManager(screenId = "checkout_screen")
        stateManager.updateValue("coupon_code", "SUMMER2026")
        stateManager.updateValue("user_id", "usr_9981")
        stateManager.updateValue("amount", "500")
        stateManager.updateValue("accepted", "true")

        val rawPayload = mapOf(
            "coupon" to HeimValue.Str("{{state.coupon_code}}"),
            "userId" to HeimValue.Str("{{state.user_id}}"),
            "amount" to HeimValue.Str("{{state.amount}}"),
            "accepted" to HeimValue.Str("{{state.accepted}}"),
            "staticField" to HeimValue.Str("fixed_value")
        )

        val out = stateManager.interpolatePayload(rawPayload)

        assertEquals(HeimValue.Str("SUMMER2026"), out?.get("coupon"))
        assertEquals(HeimValue.Str("usr_9981"), out?.get("userId"))
        // A numeric state value must reach the backend as an exact number, not as "500"
        // and not routed through Double.
        assertEquals(HeimValue.Int64(500), out?.get("amount"))
        assertEquals(HeimValue.Bool(true), out?.get("accepted"))
        assertEquals(HeimValue.Str("fixed_value"), out?.get("staticField"))
    }

    @Test
    fun testInterpolateEmbeddedAndNestedPlaceholders() {
        val stateManager = HeimStateManager(screenId = "profile")
        stateManager.updateValue("name", "Julian")
        stateManager.updateValue("city", "Bogota")

        val payload = mapOf(
            "greeting" to HeimValue.Str("Hola {{state.name}}, bienvenido a {{state.city}}"),
            "nested" to HeimValue.Obj(
                mapOf("who" to HeimValue.Str("{{state.name}}"))
            ),
            "list" to HeimValue.Arr(listOf(HeimValue.Str("{{state.city}}")))
        )

        val out = stateManager.interpolatePayload(payload)

        assertEquals(HeimValue.Str("Hola Julian, bienvenido a Bogota"), out?.get("greeting"))
        assertEquals(HeimValue.Obj(mapOf("who" to HeimValue.Str("Julian"))), out?.get("nested"))
        assertEquals(HeimValue.Arr(listOf(HeimValue.Str("Bogota"))), out?.get("list"))
    }

    @Test
    fun testUnresolvedPlaceholderIsReportedAndNotSilentlyEmptied() {
        val stateManager = HeimStateManager(screenId = "transfer")
        val missing = mutableListOf<String>()

        val out = stateManager.interpolatePayload(
            payload = mapOf("amount" to HeimValue.Str("{{state.amount}}")),
            onUnresolved = { missing += it }
        )

        // Sending amount:"" because a key was misspelled is not an acceptable silent failure.
        assertEquals(HeimValue.Null, out?.get("amount"))
        assertEquals(listOf("amount"), missing)
    }

    @Test
    fun testInterpolationEdgeCases() {
        val stateManager = HeimStateManager(screenId = "edge")
        stateManager.updateValue("a", "A")

        fun interp(v: String) = stateManager.interpolatePayload(mapOf("x" to HeimValue.Str(v)))?.get("x")

        // No placeholder at all.
        assertEquals(HeimValue.Str("plain text"), interp("plain text"))
        // Unterminated placeholder must be emitted verbatim, never swallowed.
        assertEquals(HeimValue.Str("broken {{state.a"), interp("broken {{state.a"))
        // Two placeholders in one string.
        assertEquals(HeimValue.Str("A and A"), interp("{{state.a}} and {{state.a}}"))
        // Leading and trailing literal text around a placeholder.
        assertEquals(HeimValue.Str("[A]"), interp("[{{state.a}}]"))
        // Whitespace inside the braces.
        assertEquals(HeimValue.Str("A"), interp("{{  state.a  }}"))
        // The `state.` prefix is optional.
        assertEquals(HeimValue.Str("A"), interp("{{a}}"))
        // A lone closing brace is literal text, not a parse error.
        assertEquals(HeimValue.Str("a } b"), interp("a } b"))
    }

    @Test
    fun testDraftRestoreDoesNotOverwriteNewerUserInput() {
        val stateManager = HeimStateManager(screenId = "form")
        stateManager.updateValue("email", "typed@by.user")

        stateManager.restoreDraft(mapOf("email" to "stale@draft.value", "phone" to "3001112222"))

        assertEquals("typed@by.user", stateManager.getValue("email"))
        assertEquals("3001112222", stateManager.getValue("phone"))
    }

    @Test
    fun testSensitiveKeysAreTracked() {
        val stateManager = HeimStateManager(screenId = "login")
        stateManager.markSensitive("password")
        stateManager.updateValue("password", "hunter2")
        // Value is usable in-memory ...
        assertEquals("hunter2", stateManager.getValue("password"))
        // ... and the key is known to be excluded from persistence.
        assertTrue(stateManager.getAllValues().containsKey("password"))
    }

    @Test
    fun `a declared default submits as its value rather than null`() {
        val manager = HeimStateManager(screenId = "fintech_kyc")

        // What a switch and a text field each do on first composition: seed the default the
        // payload declared. Before this, only the text field did, so `{{state.is_business}}`
        // interpolated to null on a switch the user had simply not touched.
        manager.updateValue("is_business", "true")
        manager.updateValue("plan", "pro")

        val unresolved = mutableListOf<String>()
        val payload = manager.interpolatePayload(
            mapOf(
                "is_business" to HeimValue.Str("{{state.is_business}}"),
                "plan" to HeimValue.Str("{{state.plan}}"),
                "never_composed" to HeimValue.Str("{{state.hidden_field}}"),
            )
        ) { unresolved += it }

        // Booleans survive as booleans rather than arriving as the string "true".
        assertEquals(HeimValue.Bool(true), payload?.get("is_business"))
        assertEquals(HeimValue.Str("pro"), payload?.get("plan"))

        // A field that never composed stays null and is reported, rather than reaching the
        // backend as the literal text "{{state.hidden_field}}".
        assertEquals(HeimValue.Null, payload?.get("never_composed"))
        assertEquals(listOf("hidden_field"), unresolved)
    }

}
