package io.heimui.core

import io.heimui.core.presentation.state.DriverBackedFormDraftStorage
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimFormDraftStorageTest {

    @Test
    fun `test save restore and clear form draft`() = runTest {
        val draftStorage = DriverBackedFormDraftStorage()

        // 1. Save draft
        draftStorage.saveDraft("checkout_screen", mapOf("email" to "user@example.com", "name" to "Alice"))

        // 2. Retrieve draft
        val draft = draftStorage.getDraft("checkout_screen")
        assertNotNull(draft)
        assertEquals("user@example.com", draft["email"])
        assertEquals("Alice", draft["name"])

        // 3. Restore into state manager
        val stateManager = HeimStateManager("checkout_screen")
        stateManager.restoreDraft(draft)
        assertEquals("user@example.com", stateManager.getValue("email"))
        assertEquals("Alice", stateManager.getValue("name"))

        // 4. Clear draft
        draftStorage.clearDraft("checkout_screen")
        assertNull(draftStorage.getDraft("checkout_screen"))
    }
}
