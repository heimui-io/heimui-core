package io.heimui.core

import io.heimui.core.domain.model.HeimValue
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.state.HeimStateScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A field's identity, once a list can repeat the template that declares it.
 *
 * `state_key` was the whole identity of a field, which held while a screen was a fixed set of
 * inputs. Three passengers on screen broke it: they carry the same key, so typing in the third row
 * filled the first, one validation rule covered all three, and the payload submitted one name.
 */
class HeimStateScopeTest {

    private val passenger1 = HeimStateScope("passengers/p1")
    private val passenger2 = HeimStateScope("passengers/p2")

    @Test
    fun `a screen without repeated forms stores exactly what it always did`() {
        // Every screen written before this exists in the root scope, and its stored keys have to
        // stay byte-identical: they are what a saved draft on a user's phone is keyed by.
        assertEquals("email", HeimStateScope.Root.resolve("email"))
        assertTrue(HeimStateScope.Root.isRoot)
    }

    @Test
    fun `two rows of the same list keep separate values`() {
        val manager = HeimStateManager(screenId = "booking")
        manager.updateValue(passenger1.resolve("full_name"), "Ada")
        manager.updateValue(passenger2.resolve("full_name"), "Grace")

        assertEquals("Ada", manager.resolveValue(passenger1, "full_name"))
        assertEquals("Grace", manager.resolveValue(passenger2, "full_name"))
    }

    @Test
    fun `a row falls back to the screen for a key it does not own`() {
        // A `visible_if` on something screen-wide has to keep working inside a row, without the
        // author knowing that rows have namespaces at all.
        val manager = HeimStateManager(screenId = "booking")
        manager.updateValue("is_editing", "true")
        assertEquals("true", manager.resolveValue(passenger1, "is_editing"))
    }

    @Test
    fun `a row shadows the screen when it has its own answer`() {
        val manager = HeimStateManager(screenId = "booking")
        manager.updateValue("full_name", "screen level")
        manager.updateValue(passenger1.resolve("full_name"), "row level")
        assertEquals("row level", manager.resolveValue(passenger1, "full_name"))
        assertEquals("screen level", manager.resolveValue(HeimStateScope.Root, "full_name"))
    }

    @Test
    fun `a submit inside a row carries that row's values`() {
        val manager = HeimStateManager(screenId = "booking")
        manager.updateValue(passenger1.resolve("full_name"), "Ada")
        manager.updateValue(passenger2.resolve("full_name"), "Grace")

        val payload = mapOf("name" to HeimValue.Str("{{state.full_name}}"))
        assertEquals(HeimValue.Str("Ada"), manager.interpolatePayload(payload, passenger1)?.get("name"))
        assertEquals(HeimValue.Str("Grace"), manager.interpolatePayload(payload, passenger2)?.get("name"))
    }

    @Test
    fun `an unresolved key inside a row is still reported`() {
        val manager = HeimStateManager(screenId = "booking")
        val missing = mutableListOf<String>()
        val out = manager.interpolatePayload(
            mapOf("name" to HeimValue.Str("{{state.full_name}}")),
            passenger1
        ) { missing += it }

        // Sending an empty string because a key was misspelled is not an acceptable failure mode,
        // and being inside a row does not change that.
        assertEquals(listOf("full_name"), missing)
        assertEquals(HeimValue.Null, out?.get("name"))
    }

    @Test
    fun `a host can collect what every row holds`() {
        val manager = HeimStateManager(screenId = "booking")
        manager.updateValue(passenger1.resolve("full_name"), "Ada")
        manager.updateValue(passenger1.resolve("seat"), "12A")
        manager.updateValue(passenger2.resolve("full_name"), "Grace")
        manager.updateValue("trip_code", "XY123")

        assertEquals(mapOf("full_name" to "Ada", "seat" to "12A"), manager.valuesIn(passenger1))
        // The screen's own values, without the rows leaking into them.
        assertEquals(mapOf("trip_code" to "XY123"), manager.valuesIn(HeimStateScope.Root))
        assertEquals(listOf(passenger1, passenger2), manager.scopesUnder("passengers"))
    }

    @Test
    fun `namespaces compose for a list inside a list`() {
        val bag = passenger1.child("bags", "b2")
        assertEquals("passengers/p1/bags/b2", bag.path)
        assertEquals("passengers/p1/bags/b2::weight", bag.resolve("weight"))
    }

    @Test
    fun `a stored key can be read back as the scope and the key the author wrote`() {
        // Draft storage holds the composed key, so restoring one has to be able to take it apart.
        val (scope, key) = HeimStateScope.split("passengers/p1::full_name")
        assertEquals(passenger1, scope)
        assertEquals("full_name", key)

        val (rootScope, plain) = HeimStateScope.split("email")
        assertTrue(rootScope.isRoot)
        assertEquals("email", plain)
    }

    @Test
    fun `a row that was never written has no value rather than someone else's`() {
        val manager = HeimStateManager(screenId = "booking")
        manager.updateValue(passenger1.resolve("full_name"), "Ada")
        assertNull(manager.resolveValue(passenger2, "full_name"))
    }
}
